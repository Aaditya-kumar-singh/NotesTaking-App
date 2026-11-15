#!/usr/bin/env python3
"""Local-first, cooperative multi-AI task broker for NotesTaking's shared Git worktrees.

Runtime state is kept in the common Git directory, not in versioned files.
This is a task/ownership guard, not a security sandbox or an autonomous agent runner.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import fnmatch
import json
import sqlite3
import subprocess
import sys
from datetime import datetime, timezone, timedelta
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
LIVE = ("reserved", "active", "review")
RISK = {"low": 0, "medium": 1, "high": 2}
PRIORITY = {"P0": 400, "P1": 300, "P2": 200, "P3": 100}


class Blocked(Exception):
    pass


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def git(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        capture_output=True, text=True, check=False, timeout=20,
    )
    if result.returncode:
        raise Blocked(f"Git failed ({' '.join(args)}): {result.stderr.strip()}")
    return result.stdout.strip()


def checkout_root(repo: Path) -> Path:
    return Path(git(repo, "rev-parse", "--show-toplevel")).resolve()


def shared_dir(repo: Path) -> Path:
    p = Path(git(repo, "rev-parse", "--git-common-dir"))
    return (p if p.is_absolute() else repo / p).resolve()


def source_root(repo: Path) -> Path:
    return shared_dir(repo).parent


def valid_scope(path: str) -> bool:
    # Fail closed on absolute paths, dot segments, backslash aliasing and .git.
    pp = PurePosixPath(path)
    return (
        bool(path) and not path.startswith("/") and "\\" not in path
        and ":" not in path and all(part not in ("..", ".") for part in path.split("/"))
        and not path.startswith(".git/") and path != ".git"
        and str(pp) == path
    )


def static_prefix(path: str) -> str:
    return path.split("*", 1)[0].split("?", 1)[0].split("[", 1)[0]


def overlaps(a: str, b: str) -> bool:
    """Conservative check: uncertain wildcard overlaps are denied, not guessed free."""
    if a == b or fnmatch.fnmatchcase(a, b) or fnmatch.fnmatchcase(b, a):
        return True
    a_prefix, b_prefix = static_prefix(a), static_prefix(b)
    return a_prefix.startswith(b_prefix) or b_prefix.startswith(a_prefix)


def within_permission(scope: str, permission: str) -> bool:
    if scope == permission:
        return True
    if permission.endswith("/**"):
        base = permission[:-3]
        return scope.startswith(base + "/")
    return not any(c in scope for c in "*?[") and fnmatch.fnmatchcase(scope, permission)


def config(path: Path) -> dict:
    board = json.loads(path.read_text(encoding="utf-8"))
    if board.get("version") != 1:
        raise Blocked("Unsupported board version; stop rather than migrating automatically")
    ids = [t["id"] for t in board["tasks"]]
    if len(ids) != len(set(ids)):
        raise Blocked("Duplicate task IDs")
    for t in board["tasks"]:
        if t.get("priority") not in PRIORITY or t.get("risk") not in RISK:
            raise Blocked(f"{t['id']}: invalid priority or risk")
        if not all(valid_scope(p) for p in t["paths"]):
            raise Blocked(f"{t['id']}: unsafe path scope")
        if any(d not in ids or d == t["id"] for d in t.get("deps", [])):
            raise Blocked(f"{t['id']}: invalid dependency")
    if not all(valid_scope(p) for p in board["protected_paths"]):
        raise Blocked("Unsafe protected-path rule")
    for a, data in board["agents"].items():
        if not all(valid_scope(p) for p in data["allowed_paths"]):
            raise Blocked(f"{a}: unsafe allowed path")
        if data.get("max_risk") not in RISK:
            raise Blocked(f"{a}: invalid risk ceiling")
    return board


@contextmanager
def connect(db_path: Path):
    db_path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(db_path), timeout=15, isolation_level=None)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA busy_timeout=15000")
    conn.execute("""
        CREATE TABLE IF NOT EXISTS claims (
            task_id TEXT PRIMARY KEY,
            agent TEXT NOT NULL,
            phase TEXT NOT NULL,
            worktree TEXT,
            branch TEXT,
            base_sha TEXT,
            commit_sha TEXT,
            note TEXT,
            started_at TEXT NOT NULL,
            heartbeat_at TEXT NOT NULL,
            evidence TEXT
        )""")
    conn.execute("""
        CREATE TABLE IF NOT EXISTS events (
            seq INTEGER PRIMARY KEY AUTOINCREMENT,
            stamp TEXT NOT NULL,
            task_id TEXT NOT NULL,
            actor TEXT NOT NULL,
            action TEXT NOT NULL,
            note TEXT
        )""")
    try:
        yield conn
    finally:
        conn.close()


def event(db: sqlite3.Connection, task: str, actor: str, action: str, note: str = "") -> None:
    db.execute(
        "INSERT INTO events(stamp,task_id,actor,action,note) VALUES(?,?,?,?,?)",
        (utc_now(), task, actor, action, note),
    )


def claims_by_id(db: sqlite3.Connection) -> dict:
    return {r["task_id"]: dict(r) for r in db.execute("SELECT * FROM claims")}


def task_by_id(board: dict) -> dict:
    return {t["id"]: t for t in board["tasks"]}


def scope_eligible(task: dict, agent: dict, board: dict) -> bool:
    paths = task["paths"]
    if agent["read_only"]:
        return not paths
    if not paths:
        return True  # writers may do read-only work within their own capacity
    for p in paths:
        if any(overlaps(p, protected) for protected in board["protected_paths"]):
            return False
        if not any(within_permission(p, allowed) for allowed in agent["allowed_paths"]):
            return False
    return True


def workspace(board: dict, agent_name: str, repo: Path) -> tuple[str | None, str | None]:
    agent = board["agents"][agent_name]
    if agent["read_only"]:
        return None, None
    assigned = agent.get("worktree")
    if not assigned:
        raise Blocked(f"{agent_name} has no approved coding worktree")
    expected = (source_root(repo) / assigned).resolve()
    actual = checkout_root(repo)
    if actual != expected:
        raise Blocked(f"{agent_name} must run from its assigned worktree: {expected}")
    branch = git(repo, "branch", "--show-current")
    if branch != agent["branch"]:
        raise Blocked(f"{agent_name}: wrong branch {branch}, expected {agent['branch']}")
    return str(actual), branch


def score(task: dict, agent: dict) -> int:
    ratings = [agent["skills"].get(skill, 0) for skill in task["skills"]]
    if ratings and min(ratings) < 2:
        return -1
    if task["difficulty"] > agent["max_difficulty"]:
        return -1
    if RISK[task["risk"]] > RISK[agent["max_risk"]]:
        return -1
    # Priority first; skills and difficulty fit distinguish tasks in a priority band.
    match = sum(ratings) / max(1, len(ratings))
    return PRIORITY[task["priority"]] + int(match * 14) - task["difficulty"] * 2


def blocking_reasons(task: dict, who: str, board: dict, claims: dict) -> list[str]:
    agent = board["agents"][who]
    reasons = []
    if not agent["enabled"]:
        reasons.append("profile not enabled")
    if not scope_eligible(task, agent, board):
        reasons.append("paths outside approved ownership or shared/protected paths")
    if score(task, agent) < 0:
        reasons.append("risk, difficulty or required-skill ceiling")
    current = claims.get(task["id"])
    if current and current["phase"] in LIVE and current["agent"] != who:
        reasons.append(f"already owned by {current['agent']}")
    if current and current["phase"] == "verified":
        reasons.append("already verified")
    if current and current["phase"] == "released":
        reasons.append("released; coordinator must re-plan explicitly")
    held = [
        c for c in claims.values()
        if c["agent"] == who and c["phase"] in LIVE and c["task_id"] != task["id"]
    ]
    if len(held) >= agent["capacity"]:
        reasons.append(f"{who} at capacity")
    for dep in task.get("deps", []):
        if claims.get(dep, {}).get("phase") != "verified":
            reasons.append(f"dependency {dep} not verified")
    tasks = task_by_id(board)
    for other in claims.values():
        if other["phase"] not in LIVE or other["task_id"] == task["id"]:
            continue
        owned_paths = tasks.get(other["task_id"], {}).get("paths", [])
        if any(overlaps(p, q) for p in task["paths"] for q in owned_paths):
            reasons.append(f"path claim overlaps {other['task_id']} ({other['agent']})")
    return reasons


def seed(db: sqlite3.Connection, board: dict) -> list[str]:
    """Reserve already-assigned lanes without changing their Git branches or claiming success."""
    added = []
    db.execute("BEGIN IMMEDIATE")
    try:
        for task in board["tasks"]:
            owner = task.get("reserve_for")
            if not owner:
                continue
            old = db.execute("SELECT task_id FROM claims WHERE task_id=?", (task["id"],)).fetchone()
            if old:
                continue
            if owner not in board["agents"]:
                raise Blocked(f"Unknown reserved owner: {owner}")
            agent = board["agents"][owner]
            if not scope_eligible(task, agent, board):
                raise Blocked(f"{task['id']}: reserved paths not allowed for {owner}")
            other_claims = claims_by_id(db)
            catalog = task_by_id(board)
            for other in other_claims.values():
                if other["phase"] not in LIVE:
                    continue
                if any(overlaps(p, q) for p in task["paths"]
                       for q in catalog[other["task_id"]]["paths"]):
                    raise Blocked(f"{task['id']}: reservation overlaps {other['task_id']}")
            now = utc_now()
            db.execute(
                "INSERT INTO claims(task_id,agent,phase,worktree,branch,started_at,heartbeat_at,note) "
                "VALUES(?,?,?,?,?,?,?,?)",
                (task["id"], owner, "reserved", agent["worktree"], agent["branch"],
                 now, now, task.get("note", "")),
            )
            event(db, task["id"], owner, "legacy-reservation", task.get("note", ""))
            added.append(task["id"])
        db.commit()
    except Exception:
        db.rollback()
        raise
    return added


def recommend(db: sqlite3.Connection, board: dict, who: str) -> tuple[list, list]:
    if who not in board["agents"]:
        raise Blocked(f"Unknown agent: {who}")
    claims = claims_by_id(db)
    choices, blocked = [], []
    for task in board["tasks"]:
        reasons = blocking_reasons(task, who, board, claims)
        if reasons:
            blocked.append((task["id"], reasons))
        else:
            owns = claims.get(task["id"], {}).get("agent") == who
            choices.append((score(task, board["agents"][who]) + (1000 if owns else 0), task))
    return sorted(choices, key=lambda x: (-x[0], x[1]["id"])), blocked


def claim(db: sqlite3.Connection, board: dict, who: str, repo: Path, wanted: str | None) -> dict:
    if who not in board["agents"]:
        raise Blocked(f"Unknown agent: {who}")
    worktree, branch = workspace(board, who, repo)
    db.execute("BEGIN IMMEDIATE")
    try:
        choices, blocked = recommend(db, board, who)
        selected = next((t for _, t in choices if t["id"] == wanted), None) if wanted else (
            choices[0][1] if choices else None
        )
        if not selected:
            if wanted:
                reason = next((r for tid, r in blocked if tid == wanted), ["unknown task"])
                raise Blocked(f"{wanted}: {'; '.join(reason)}")
            raise Blocked("No unblocked task in this agent's approved scope/capacity")
        prior = db.execute("SELECT phase,agent FROM claims WHERE task_id=?", (selected["id"],)).fetchone()
        now = utc_now()
        if prior:
            if prior["agent"] != who or prior["phase"] not in ("reserved", "active"):
                raise Blocked("Task changed owner or state while claiming")
            db.execute(
                "UPDATE claims SET phase='active', heartbeat_at=?, worktree=?,branch=? WHERE task_id=?",
                (now, worktree, branch, selected["id"]),
            )
        else:
            db.execute(
                "INSERT INTO claims(task_id,agent,phase,worktree,branch,base_sha,started_at,heartbeat_at) "
                "VALUES(?,?,?,?,?,?,?,?)",
                (selected["id"], who, "active", worktree, branch,
                 git(repo, "rev-parse", "HEAD") if worktree else None, now, now),
            )
        event(db, selected["id"], who, "claim")
        db.commit()
        return selected
    except Exception:
        db.rollback()
        raise


def update(db: sqlite3.Connection, board: dict, who: str, task_id: str, repo: Path,
           action: str, note: str = "", commit_sha: str = "") -> None:
    if who not in board["agents"]:
        raise Blocked(f"Unknown agent: {who}")
    worktree, branch = workspace(board, who, repo)
    db.execute("BEGIN IMMEDIATE")
    try:
        claim_row = db.execute("SELECT * FROM claims WHERE task_id=?", (task_id,)).fetchone()
        if not claim_row or claim_row["agent"] != who:
            raise Blocked("Task not owned by this agent")
        if claim_row["phase"] not in ("reserved", "active"):
            raise Blocked("Task is in review/closed; only coordinator can return it")
        if action == "handoff" and claim_row["phase"] != "active":
            raise Blocked("Claim the reserved task before handing off")
        if action == "handoff" and not note.strip():
            raise Blocked("Handoff needs a useful summary")
        if action == "handoff" and worktree:
            if git(repo, "status", "--porcelain"):
                raise Blocked("Commit or explicitly resolve worktree changes before handoff")
            if not commit_sha or git(repo, "rev-parse", "HEAD").lower() != commit_sha.lower():
                raise Blocked("Handoff must reference this worktree's exact HEAD commit")
        if action == "handoff":
            db.execute(
                "UPDATE claims SET phase='review',commit_sha=?,note=?,heartbeat_at=? WHERE task_id=?",
                (commit_sha or None, note, utc_now(), task_id),
            )
        else:
            db.execute("UPDATE claims SET heartbeat_at=? WHERE task_id=?", (utc_now(), task_id))
        event(db, task_id, who, action, note)
        db.commit()
    except Exception:
        db.rollback()
        raise


def close(db: sqlite3.Connection, board: dict, task_id: str, evidence: str) -> None:
    if not evidence.strip() or len(evidence.strip()) < 15:
        raise Blocked("Closing needs reviewed evidence including the exact integration/CI SHA")
    db.execute("BEGIN IMMEDIATE")
    try:
        row = db.execute("SELECT phase FROM claims WHERE task_id=?", (task_id,)).fetchone()
        if not row or row["phase"] != "review":
            raise Blocked("Only handed-off tasks can be verified by the coordinator")
        db.execute(
            "UPDATE claims SET phase='verified',evidence=?,heartbeat_at=? WHERE task_id=?",
            (evidence, utc_now(), task_id),
        )
        event(db, task_id, "coordinator", "verification-recorded", evidence)
        db.commit()
    except Exception:
        db.rollback()
        raise


def doctor(db: sqlite3.Connection, board: dict, repo: Path) -> list[str]:
    claims = claims_by_id(db)
    warnings = []
    for row in claims.values():
        if row["phase"] not in LIVE:
            continue
        age = datetime.now(timezone.utc) - datetime.fromisoformat(row["heartbeat_at"])
        if age > timedelta(hours=4):
            warnings.append(f"{row['task_id']}: heartbeat stale ({int(age.total_seconds() // 3600)}h); no automatic takeover")
    tasks = task_by_id(board)
    live = [row for row in claims.values() if row["phase"] in LIVE]
    for i, a in enumerate(live):
        for b in live[i + 1:]:
            if any(overlaps(p, q) for p in tasks[a["task_id"]]["paths"] for q in tasks[b["task_id"]]["paths"]):
                warnings.append(f"Unsafe overlapping claims: {a['task_id']} and {b['task_id']}")
    worktrees = {}
    for name, agent in board["agents"].items():
        p = agent.get("worktree")
        if not p:
            continue
        actual = (source_root(repo) / p).resolve()
        if str(actual) in worktrees:
            warnings.append(f"Agents {name} and {worktrees[str(actual)]} configured for same worktree")
        worktrees[str(actual)] = name
        if not actual.exists():
            warnings.append(f"{name}: missing expected worktree {actual}")
    return warnings


def run(args: argparse.Namespace) -> int:
    repo = Path(args.repo).resolve()
    board = config(Path(args.board))
    db_path = Path(args.db).resolve() if args.db else shared_dir(repo) / "agent-coordination" / "claims.sqlite3"
    with connect(db_path) as db:
        if args.command == "seed":
            print(json.dumps({"reserved": seed(db, board), "db": str(db_path)}, indent=2))
        elif args.command == "status":
            print(json.dumps({
                "db": str(db_path),
                "claims": list(claims_by_id(db).values()),
                "tasks": [{"id": t["id"], "priority": t["priority"], "title": t["title"]}
                          for t in board["tasks"]],
                "warnings": doctor(db, board, repo),
            }, indent=2))
        elif args.command == "recommend":
            options, blocked = recommend(db, board, args.agent)
            print(json.dumps({
                "agent": args.agent,
                "choices": [{"id": t["id"], "title": t["title"], "score": s} for s, t in options],
                "blocked": [{"id": id_, "reasons": reasons} for id_, reasons in blocked],
            }, indent=2))
        elif args.command in ("claim", "claim-next"):
            selected = claim(db, board, args.agent, repo, getattr(args, "task_id", None))
            print(json.dumps({"claimed": selected["id"], "title": selected["title"],
                              "paths": selected["paths"], "notes": selected.get("note", ""),
                              "branch": board["agents"][args.agent]["branch"]}, indent=2))
        elif args.command in ("heartbeat", "handoff"):
            update(db, board, args.agent, args.task_id, repo,
                   args.command, getattr(args, "summary", ""), getattr(args, "commit", ""))
            print(f"{args.command} recorded for {args.task_id}")
        elif args.command == "close":
            # Only the top-level coordinator checkout records external verification.
            if checkout_root(repo) != source_root(repo):
                raise Blocked("Only the administrative integration checkout may close tasks")
            close(db, board, args.task_id, args.evidence)
            print("Coordinator verification evidence recorded (not independently checked by broker)")
        elif args.command == "doctor":
            warnings = doctor(db, board, repo)
            for w in warnings:
                print("WARNING:", w)
            print(f"doctor: {len(warnings)} warning(s); no claims or branches modified")
            return int(bool(warnings))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".", help="The Git worktree this agent is working from")
    parser.add_argument("--board", default=str(HERE / "board.json"), help="Versioned task/role catalog")
    parser.add_argument("--db", default=None, help="Override shared Git SQLite board for isolated tests")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("seed", help="Once: reserve current Chat A, Chat B and Gemini scopes")
    sub.add_parser("status", help="Show live tasks and reserved files")
    sub.add_parser("doctor", help="Read-only stale/missing/overlap audit; never steal claims")
    r = sub.add_parser("recommend", help="Rank tasks by demonstrated capabilities, scope and dependencies")
    r.add_argument("--agent", required=True)
    for cmd in ("claim", "claim-next"):
        p = sub.add_parser(cmd)
        p.add_argument("--agent", required=True)
        if cmd == "claim":
            p.add_argument("task_id")
    p = sub.add_parser("heartbeat")
    p.add_argument("--agent", required=True)
    p.add_argument("task_id")
    p = sub.add_parser("handoff")
    p.add_argument("--agent", required=True)
    p.add_argument("task_id")
    p.add_argument("--commit", default="", help="For coding tasks, this must equal worktree HEAD")
    p.add_argument("--summary", required=True)
    p = sub.add_parser("close")
    p.add_argument("task_id")
    p.add_argument("--evidence", required=True, help="Human-checked exact SHA, CI URL and results")
    args = parser.parse_args()
    try:
        return run(args)
    except (Blocked, sqlite3.Error, ValueError, OSError) as exc:
        print("BLOCKED:", exc, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
