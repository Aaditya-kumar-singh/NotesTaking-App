#!/usr/bin/env python3
"""Cooperative three-chat APK task ledger: atomic sessions, task and CI claims.

This uses separate triad_* tables in the existing common Git SQLite database.
It does not replace legacy LANE-A/LANE-B/Gemini claims, run ChatGPT sessions,
merge code, build APKs, or grant permissions to edit another agent's files.
"""
from __future__ import annotations

import argparse
import re
import urllib.error
import urllib.request
from contextlib import contextmanager
from datetime import datetime, timezone
import json
from pathlib import Path
import sqlite3
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "docs/github/APK_BATCH_TASKS.json"
PROGRESS = ROOT / "docs/github/APK_BATCH_PROGRESS.md"
DONE = frozenset(("verified", "device_verified", "deferred"))
BRANCHES = {
    "A": ("chat-a", "agent/chat-a-b39-save-recovery"),
    "B": ("chat-b", "agent/chat-b-g08-toolbar-accessibility"),
    "C": ("chat-c", "agent/chat-c-core-canvas-reliability"),
}


class Blocked(Exception):
    pass


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def git(repo: Path, *args: str) -> str:
    p = subprocess.run(["git", "-C", str(repo), *args], capture_output=True,
                       text=True, check=False, timeout=25)
    if p.returncode:
        raise Blocked(f"Git {' '.join(args)} failed: {p.stderr.strip()}")
    return p.stdout.strip()


def database(root: Path = ROOT) -> Path:
    common = Path(git(root, "rev-parse", "--git-common-dir"))
    if not common.is_absolute():
        common = (root / common).resolve()
    return common / "agent-coordination/claims.sqlite3"


def catalog() -> dict:
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    tasks = data["tasks"]
    if data["version"] != 1 or len(tasks) != 130:
        raise Blocked("Unsupported or incomplete batch catalog")
    if len({t["id"] for t in tasks}) != 130:
        raise Blocked("Duplicate batch task ID")
    for ch in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
        if len([t for t in tasks if t["batch"] == ch]) != 5:
            raise Blocked(f"Batch {ch} must have five tasks")
    return data


def connect(path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(str(path), timeout=15, isolation_level=None)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA busy_timeout=15000")
    db.execute("CREATE TABLE IF NOT EXISTS triad_slots("
               "slot TEXT PRIMARY KEY, session TEXT, generation INTEGER NOT NULL, "
               "joined_at TEXT, heartbeat_at TEXT)")
    db.execute("CREATE TABLE IF NOT EXISTS triad_tasks("
               "task_id TEXT PRIMARY KEY, status TEXT NOT NULL, slot TEXT, "
               "session TEXT, commit_sha TEXT, evidence TEXT, updated_at TEXT NOT NULL)")
    db.execute("CREATE TABLE IF NOT EXISTS triad_incidents("
               "run_id TEXT PRIMARY KEY, slot TEXT NOT NULL, session TEXT NOT NULL, "
               "status TEXT NOT NULL, description TEXT, evidence TEXT, updated_at TEXT NOT NULL)")
    db.execute("CREATE TABLE IF NOT EXISTS triad_publish("
               "id INTEGER PRIMARY KEY CHECK(id=1), slot TEXT NOT NULL, session TEXT NOT NULL, "
               "base_sha TEXT NOT NULL, evidence TEXT NOT NULL)")
    db.execute("CREATE TABLE IF NOT EXISTS triad_events("
               "seq INTEGER PRIMARY KEY AUTOINCREMENT, stamp TEXT NOT NULL, "
               "actor TEXT NOT NULL, action TEXT NOT NULL, detail TEXT NOT NULL)")
    return db


@contextmanager
def transaction(db):
    db.execute("BEGIN IMMEDIATE")
    try:
        yield
        db.commit()
    except Exception:
        db.rollback()
        raise


def emit(db, actor: str, action: str, detail: str):
    db.execute("INSERT INTO triad_events(stamp,actor,action,detail) VALUES(?,?,?,?)",
               (now(), actor, action, detail))


def assert_coordinator(repo: Path) -> None:
    if Path(git(repo, "rev-parse", "--show-toplevel")).resolve() != ROOT:
        raise Blocked("Coordinator mutations must run from top-level administrative checkout")


def assert_worktree(slot: str, repo: Path) -> None:
    if slot not in BRANCHES:
        raise Blocked("Unknown chat slot")
    name, branch = BRANCHES[slot]
    wanted = (ROOT / ".agent-worktrees" / name).resolve()
    if Path(git(repo, "rev-parse", "--show-toplevel")).resolve() != wanted:
        raise Blocked(f"Slot {slot} must use {wanted}, not the administrative checkout")
    if git(repo, "branch", "--show-current") != branch:
        raise Blocked("Wrong worktree branch for this slot")


def assert_session(db, slot: str, session: str) -> sqlite3.Row:
    if not session or slot not in BRANCHES:
        raise Blocked("Slot A/B/C and a nonempty unique session ID are required")
    row = db.execute("SELECT * FROM triad_slots WHERE slot=?", (slot,)).fetchone()
    if not row or row["session"] != session:
        raise Blocked("Session superseded or not registered. Stop writing; request explicit replacement.")
    return row


def init(db):
    tasks = catalog()["tasks"]
    with transaction(db):
        for slot, session in (("A", "legacy-a"), ("B", "legacy-b"), ("C", None)):
            db.execute("INSERT OR IGNORE INTO triad_slots(slot,session,generation,joined_at,heartbeat_at)"
                       " VALUES(?,?,?,?,?)", (slot, session, 1 if session else 0,
                       now() if session else None, now() if session else None))
        for task in tasks:
            db.execute("INSERT OR IGNORE INTO triad_tasks(task_id,status,updated_at) VALUES(?,?,?)",
                       (task["id"], "pending", now()))
        known = {r["task_id"] for r in db.execute("SELECT task_id FROM triad_tasks")}
        if known != {t["id"] for t in tasks}:
            raise Blocked("Database catalog mismatch: stop and migrate explicitly")
        emit(db, "coordinator", "init", "Seeded 130 tasks without overwriting prior states")


def replace_session(db, slot: str, old: str | None, new: str, reason: str, repo: Path):
    assert_coordinator(repo)
    if slot not in BRANCHES or not new or any(ch.isspace() for ch in new) or len(new) > 96:
        raise Blocked("Invalid slot/session")
    if len(reason.strip()) < 12:
        raise Blocked("Explicit replacement requires a meaningful reason")
    worktree = ROOT / ".agent-worktrees" / BRANCHES[slot][0]
    assert_worktree(slot, worktree)
    if git(worktree, "status", "--porcelain"):
        raise Blocked("Worktree has uncommitted changes; preserve and review before replacement")
    with transaction(db):
        row = db.execute("SELECT * FROM triad_slots WHERE slot=?", (slot,)).fetchone()
        if not row or row["session"] != old:
            raise Blocked("Compare-and-swap rejected: old session differs")
        if old == new:
            raise Blocked("Replacement session must differ from previous session")
        db.execute("UPDATE triad_slots SET session=?,generation=generation+1,"
                   "joined_at=?,heartbeat_at=? WHERE slot=?", (new, now(), now(), slot))
        db.execute("UPDATE triad_tasks SET session=? WHERE slot=? AND status IN ('active','review')",
                   (new, slot))
        db.execute("UPDATE triad_incidents SET session=? WHERE slot=? AND status='active'",
                   (new, slot))
        db.execute("UPDATE triad_publish SET session=? WHERE slot=?", (new, slot))
        emit(db, slot, "replace-session", f"{old} -> {new}: {reason}")
    return {"slot": slot, "session": new, "superseded": old}


def touch(db, slot: str, session: str):
    with transaction(db):
        assert_session(db, slot, session)
        db.execute("UPDATE triad_slots SET heartbeat_at=? WHERE slot=?", (now(), slot))
    return {"slot": slot, "session": session, "heartbeat": now()}


def catalog_tasks(data):
    return {t["id"]: t for t in data["tasks"]}


def next_tasks(db, slot: str, session: str, data: dict, include_optional=False):
    assert_session(db, slot, session)
    states = {r["task_id"]: r["status"] for r in db.execute("SELECT task_id,status FROM triad_tasks")}
    return [
        {"task": t["id"], "batch": t["batch"], "title": t["title"],
         "priority": t["priority"], "status": states[t["id"]]}
        for t in data["tasks"]
        if t["owner"] == slot and states[t["id"]] == "pending"
        and (include_optional or t["release_scope"] == "required")
    ][:10]


def claim(db, slot, session, task_id, data):
    t = catalog_tasks(data).get(task_id)
    if not t or t["owner"] != slot:
        raise Blocked("Task missing or not routed to this slot; request coordinator allocation")
    with transaction(db):
        assert_session(db, slot, session)
        if db.execute("SELECT 1 FROM triad_tasks WHERE slot=? AND status='active'", (slot,)).fetchone():
            raise Blocked("This slot already has an active task; hand it off first")
        r = db.execute("UPDATE triad_tasks SET status='active',slot=?,session=?,updated_at=? "
                       "WHERE task_id=? AND status='pending'",
                       (slot, session, now(), task_id))
        if r.rowcount != 1:
            raise Blocked("Task was concurrently claimed, reviewed or deferred")
        emit(db, slot, "task-claim", task_id)
    return {"task": task_id, "slot": slot, "status": "active"}


def handoff(db, slot, session, task_id, commit, evidence, repo):
    if len(evidence.strip()) < 20:
        raise Blocked("Task handoff requires detailed evidence")
    assert_worktree(slot, repo)
    if not commit or len(commit) != 40 or git(repo, "rev-parse", "HEAD") != commit:
        raise Blocked("Must hand off exact 40-character worktree HEAD")
    if git(repo, "status", "--porcelain"):
        raise Blocked("Worktree must be clean before task handoff")
    with transaction(db):
        assert_session(db, slot, session)
        r = db.execute("UPDATE triad_tasks SET status='review',commit_sha=?,evidence=?,"
                       "updated_at=? WHERE task_id=? AND status='active' AND slot=? AND session=?",
                       (commit, evidence, now(), task_id, slot, session))
        if r.rowcount != 1:
            raise Blocked("This session does not own an active task")
        emit(db, slot, "task-handoff", f"{task_id}: {commit}; {evidence}")
    return {"task": task_id, "status": "review", "commit": commit}


def approve(db, task_id, evidence, repo):
    assert_coordinator(repo)
    if len(evidence.strip()) < 25:
        raise Blocked("Approval requires reviewed, specific evidence")
    with transaction(db):
        r = db.execute("UPDATE triad_tasks SET status='local_ready',"
                       "evidence=COALESCE(evidence,'')||?,updated_at=? "
                       "WHERE task_id=? AND status='review'",
                       ("\nReviewer: " + evidence, now(), task_id))
        if r.rowcount != 1:
            raise Blocked("Only handed-off tasks may be marked local_ready")
        emit(db, "coordinator", "task-reviewed", f"{task_id}: {evidence}")
    return {"task": task_id, "status": "local_ready", "ci_passed": False}


def defer(db, task_id, reason, data, repo):
    assert_coordinator(repo)
    t = catalog_tasks(data).get(task_id)
    if not t or t["release_scope"] == "required":
        raise Blocked("Required tasks cannot be silently deferred")
    if len(reason.strip()) < 25:
        raise Blocked("Explicit scope decision and rationale required")
    with transaction(db):
        r = db.execute("UPDATE triad_tasks SET status='deferred',evidence=?,updated_at=? "
                       "WHERE task_id=? AND status='pending'", (reason, now(), task_id))
        if r.rowcount != 1:
            raise Blocked("Cannot defer a claimed or already-reviewed task")
        emit(db, "coordinator", "optional-deferred", f"{task_id}: {reason}")
    return {"task": task_id, "status": "deferred"}


def ci_claim(db, slot, session, run_id, reason):
    if not run_id.isdecimal() or len(reason.strip()) < 10:
        raise Blocked("CI incident needs numeric run ID and a reason")
    with transaction(db):
        assert_session(db, slot, session)
        try:
            db.execute("INSERT INTO triad_incidents(run_id,slot,session,status,description,updated_at)"
                       " VALUES(?,?,?,?,?,?)", (run_id,slot,session,"active",reason,now()))
        except sqlite3.IntegrityError as exc:
            raise Blocked("CI incident already claimed; do not double-fix") from exc
        emit(db, slot, "ci-claim", f"run {run_id}: {reason}")
    return {"run_id": run_id, "slot": slot, "status": "active"}


def ci_finish(db, slot, session, run_id, evidence):
    if len(evidence.strip()) < 20:
        raise Blocked("CI incident resolution needs evidence / classification")
    with transaction(db):
        assert_session(db, slot, session)
        r = db.execute("UPDATE triad_incidents SET status='review',evidence=?,updated_at=? "
                       "WHERE run_id=? AND slot=? AND session=? AND status='active'",
                       (evidence,now(),run_id,slot,session))
        if r.rowcount != 1:
            raise Blocked("Incident is not held by this active session")
        emit(db, slot, "ci-handoff", f"run {run_id}: {evidence}")
    return {"run_id": run_id, "status": "review", "note": "not independently verified"}


def publish_lock(db, slot, session, base, evidence, repo):
    assert_coordinator(repo)
    if len(base) != 40 or len(evidence.strip()) < 25:
        raise Blocked("Publish lock needs exact base SHA and reviewed CI evidence")
    remote = git(ROOT, "ls-remote", "origin", "refs/heads/developer").split()[0]
    if base != remote:
        raise Blocked("Remote developer advanced; rebase/review, never force-push")
    with transaction(db):
        assert_session(db, slot, session)
        if db.execute("SELECT 1 FROM triad_publish WHERE id=1").fetchone():
            raise Blocked("A different publish integration is already in progress")
        db.execute("INSERT INTO triad_publish(id,slot,session,base_sha,evidence)"
                   " VALUES(1,?,?,?,?)", (slot,session,base,evidence))
        emit(db,slot,"publish-lock",f"base={base}: {evidence}")
    return {"status": "locked", "owner": slot, "base_sha": base,
            "warning": "Coordinator must actually check GitHub CI and perform guarded push"}


def publish_release(db, slot, session, evidence, repo):
    assert_coordinator(repo)
    if len(evidence.strip()) < 20:
        raise Blocked("Publish release requires remote SHA / CI outcome note")
    with transaction(db):
        assert_session(db, slot, session)
        row = db.execute("SELECT * FROM triad_publish WHERE id=1").fetchone()
        if not row or row["slot"] != slot or row["session"] != session:
            raise Blocked("This session does not hold publish lock")
        db.execute("DELETE FROM triad_publish WHERE id=1")
        emit(db,slot,"publish-release",evidence)
    return {"status": "released", "release": "no claim of CI success"}


def github_ci_passed(run_id: str, sha: str) -> bool:
    """Require a successful GitHub Actions run on the exact reviewed SHA."""
    if not run_id.isdecimal() or not re.fullmatch(r"[a-f0-9]{40}", sha):
        raise Blocked("Use a numeric CI run ID and full 40-character lowercase Git SHA")
    url = ("https://api.github.com/repos/Aaditya-kumar-singh/"
           "NotesTaking-App/actions/runs/" + run_id)
    request = urllib.request.Request(
        url, headers={"Accept": "application/vnd.github+json",
                      "User-Agent": "NotesTaking-batch-acceptance"})
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            run = json.load(response)
    except (OSError, urllib.error.URLError, ValueError) as exc:
        raise Blocked(f"Cannot verify GitHub CI (network or API error): {exc}") from exc
    return (run.get("head_sha") == sha
            and run.get("status") == "completed"
            and run.get("conclusion") == "success")


def verify_task(db, task_id: str, sha: str, run_id: str, evidence: str, repo: Path) -> dict:
    """Coordinator attests acceptance after exact SHA CI and applicable device QA."""
    assert_coordinator(repo)
    if len(evidence.strip()) < 50:
        raise Blocked("Verification needs a specific evidence note, including any device QA")
    if not github_ci_passed(run_id, sha):
        raise Blocked("GitHub CI is not successful for this exact commit; task stays open")
    with transaction(db):
        row = db.execute("UPDATE triad_tasks SET status='verified',"
                         "evidence=COALESCE(evidence,'')||?,updated_at=? "
                         "WHERE task_id=? AND status='local_ready'",
                         ("\nAccepted: CI " + run_id + " @ " + sha + "; " + evidence,
                          now(), task_id))
        if row.rowcount != 1:
            raise Blocked("Task must be coordinator-reviewed local_ready before final verification")
        emit(db, "coordinator", "task-verified", f"{task_id}: CI #{run_id}, {sha}; {evidence}")
    return {"task": task_id, "status": "verified",
            "ci_run": run_id, "ci_sha": sha,
            "note": "Coordinator evidence still required for physical-device acceptance"}


def batch_progress(db, data: dict) -> dict:
    states = {r["task_id"]: r["status"]
              for r in db.execute("SELECT task_id,status FROM triad_tasks")}
    progress = {}
    for letter in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
        group = [states[t["id"]] for t in data["tasks"] if t["batch"] == letter]
        if len(group) != 5:
            raise Blocked(f"Invalid task count in Batch {letter}")
        counts = {state: group.count(state) for state in
                  ("pending", "active", "review", "local_ready",
                   "verified", "device_verified", "deferred")}
        resolved = sum(state in DONE for state in group)
        progress[letter] = {**counts, "resolved": resolved,
                            "total": 5, "checked": resolved == 5}
    return progress


def checklist(db, data: dict) -> str:
    batches = batch_progress(db, data)
    complete = sum(v["checked"] for v in batches.values())
    lines = [
        "# NotesTaking App - A-Z batch completion",
        "",
        "**Coordinator-verified task ledger snapshot.** Only the five-task batch "
        "with every item accepted or explicitly deferred receives a tick.",
        "Local commits, review handoffs and running/failed CI do **not** count as done. "
        "Approved optional deferrals are listed separately from verified completions.",
        "",
        f"**Completed batches: {complete}/26**",
        "",
    ]
    for letter, row in batches.items():
        mark = "x" if row["checked"] else " "
        verified = row["verified"] + row["device_verified"]
        lines.append(f"- [{mark}] **Batch {letter}** - "
                     f"{row['resolved']}/5 resolved "
                     f"({verified} verified, {row['deferred']} deferred)")
    lines.extend([
        "",
        "This Markdown is a snapshot from the local SQLite ledger; it is refreshed "
        "automatically by coordinator completion/deferral commands and may reach "
        "GitHub only with the next reviewed integration push. For live status run "
        "`python tools/agents/triad.py status`.",
        "",
    ])
    return "\n".join(lines)


def update_plan_checkboxes(text: str, progress: dict) -> str:
    """Rewrite only each batch's checkbox line; leave the task list untouched."""
    for letter, row in progress.items():
        mark = "x" if row["checked"] else " "
        verified = row["verified"] + row["device_verified"]
        label = (f"- [{mark}] **Batch {letter} resolved** - "
                 f"{row['resolved']}/5 ({verified} verified, {row['deferred']} deferred)")
        pattern = (r"(?m)^- \[[ x]\] \*\*Batch "
                   + letter + r" resolved\*\* - [^\r\n]*$")
        text, n = re.subn(pattern, lambda _: label, text)
        if n != 1:
            raise Blocked(f"Batch {letter} checkbox missing or ambiguous in A-Z task list")
    return text


def write_checklist(db, data: dict) -> None:
    progress = batch_progress(db,data)
    task_list = ROOT / "docs/github/APK_BATCH_A_TO_Z.md"
    updated = update_plan_checkboxes(task_list.read_text(encoding="utf-8"), progress)
    # Validate the checklist format before writing either visible artifact.
    summary = checklist(db,data)
    task_list.write_text(updated, encoding="utf-8")
    PROGRESS.write_text(summary, encoding="utf-8")


def batch_ready(db, letter, data):
    if letter not in "ABCDEFGHIJKLMNOPQRSTUVWXYZ" or len(letter) != 1:
        raise Blocked("Use a single batch letter A through Z")
    selected = [t for t in data["tasks"] if t["batch"] == letter]
    states = {r["task_id"]: r["status"] for r in db.execute("SELECT task_id,status FROM triad_tasks")}
    remaining = [{"task": t["id"], "state": states[t["id"]]} for t in selected
                 if states[t["id"]] not in ("local_ready", "verified", "deferred", "device_verified")]
    return {"batch": letter, "task_count": 5, "ready_for_integration_review": not remaining,
            "remaining": remaining, "note": "Readiness is not CI/device acceptance."}


def manual_complete(db, task_id, evidence, data, repo):
    assert_coordinator(repo)
    task = catalog_tasks(data).get(task_id)
    if not task or task["owner"] not in ("COORD", "DEVICE", "GEMINI"):
        raise Blocked("Manual evidence is only for externally-owned or coordinator tasks")
    if len(evidence.strip()) < 50:
        raise Blocked("Manual acceptance needs specific device/external review evidence")
    with transaction(db):
        r = db.execute("UPDATE triad_tasks SET status=?,evidence=?,updated_at=? "
                       "WHERE task_id=? AND status='pending'",
                       ("device_verified" if task["owner"] == "DEVICE" else "local_ready",
                        evidence,now(),task_id))
        if r.rowcount != 1:
            raise Blocked("Already claimed/deferred/completed")
        emit(db,"coordinator","manual-review",f"{task_id}: {evidence}")
    return {"task":task_id,"status":"device_verified" if task["owner"]=="DEVICE" else "local_ready",
            "warning":"Human-reviewed evidence; no test was run by this ledger"}


def ci_close(db, run_id, evidence, repo):
    assert_coordinator(repo)
    if len(evidence.strip()) < 30:
        raise Blocked("Incident review needs the GitHub SHA/status or verified root cause")
    with transaction(db):
        r=db.execute("UPDATE triad_incidents SET status='closed',evidence=COALESCE(evidence,'')||?,"
                     "updated_at=? WHERE run_id=? AND status='review'",
                     ("\nCoordinator: "+evidence,now(),run_id))
        if r.rowcount != 1:
            raise Blocked("Only handed-off CI incidents can be closed")
        emit(db,"coordinator","ci-incident-closed",f"{run_id}: {evidence}")
    return {"run_id":run_id,"status":"closed","note":"Verify GitHub checks separately"}


def status(db, data):
    states = {r["task_id"]: r["status"] for r in db.execute("SELECT task_id,status FROM triad_tasks")}
    by_batch = batch_progress(db, data)
    return {
        "slots": [dict(r) for r in db.execute("SELECT * FROM triad_slots ORDER BY slot")],
        "task_totals": {s: sum(v == s for v in states.values())
                        for s in ("pending","active","review","local_ready","verified","deferred","device_verified")},
        "batches": by_batch,
        "open_task_claims": [dict(r) for r in db.execute(
            "SELECT * FROM triad_tasks WHERE status IN ('active','review') ORDER BY task_id")],
        "ci_incidents": [dict(r) for r in db.execute(
            "SELECT * FROM triad_incidents WHERE status != 'closed' ORDER BY run_id")],
        "publish": [dict(r) for r in db.execute("SELECT * FROM triad_publish")],
        "completed_batches": sum(b["checked"] for b in by_batch.values()),
        "note": "A batch is checked only after all five tasks have verified completion or explicit optional deferral; a checkbox alone is not an APK release.",
    }


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--repo", default=".", help="Expected worker worktree for coding actions")
    p.add_argument("--db", default=None, help="Use a disposable database for tests")
    sub = p.add_subparsers(dest="command", required=True)
    sub.add_parser("init")
    sub.add_parser("status")
    k = sub.add_parser("checklist")
    k.add_argument("--write",action="store_true",help="Coordinator refreshes the checked-box Markdown snapshot")
    b = sub.add_parser("batch-ready")
    b.add_argument("letter")
    m = sub.add_parser("manual-complete")
    m.add_argument("task")
    m.add_argument("--evidence",required=True)
    c = sub.add_parser("ci-close")
    c.add_argument("run_id")
    c.add_argument("--evidence",required=True)
    n = sub.add_parser("next")
    n.add_argument("--slot", required=True, choices=BRANCHES)
    n.add_argument("--session", required=True)
    n.add_argument("--include-optional", action="store_true")
    r = sub.add_parser("replace")
    r.add_argument("--slot", required=True, choices=BRANCHES)
    r.add_argument("--previous", default=None)
    r.add_argument("--session", required=True)
    r.add_argument("--reason", required=True)
    for command in ("heartbeat","claim","handoff","ci-claim","ci-finish","publish-lock","publish-release"):
        x = sub.add_parser(command)
        x.add_argument("--slot", required=True, choices=BRANCHES)
        x.add_argument("--session", required=True)
        if command in ("claim","handoff"):
            x.add_argument("task")
        if command == "handoff":
            x.add_argument("--commit", required=True)
            x.add_argument("--evidence", required=True)
        if command == "ci-claim":
            x.add_argument("run_id")
            x.add_argument("--reason", required=True)
        if command == "ci-finish":
            x.add_argument("run_id")
            x.add_argument("--evidence", required=True)
        if command == "publish-lock":
            x.add_argument("--base", required=True)
            x.add_argument("--evidence", required=True)
        if command == "publish-release":
            x.add_argument("--evidence", required=True)
    a = sub.add_parser("approve")
    a.add_argument("task")
    a.add_argument("--evidence", required=True)
    v = sub.add_parser("verify")
    v.add_argument("task")
    v.add_argument("--ci-sha",required=True,help="Exact SHA of successful GitHub Actions run")
    v.add_argument("--ci-run",required=True,help="Numeric GitHub Actions run ID")
    v.add_argument("--evidence",required=True,help="Coordinator-reviewed acceptance, including applicable device QA")
    d = sub.add_parser("defer")
    d.add_argument("task")
    d.add_argument("--reason", required=True)
    args = p.parse_args()
    data = catalog()
    repo = Path(args.repo).resolve()
    with connect(Path(args.db).resolve() if args.db else database()) as db:
        if args.command == "init":
            assert_coordinator(repo)
            init(db)
            out = {"initialized": len(data["tasks"]), "slots": "A, B, C"}
        elif args.command == "status":
            out = status(db, data)
        elif args.command == "checklist":
            if args.write:
                assert_coordinator(repo)
                write_checklist(db,data)
            print(checklist(db,data))
            return 0
        elif args.command == "batch-ready":
            out = batch_ready(db,args.letter,data)
        elif args.command == "manual-complete":
            out = manual_complete(db,args.task,args.evidence,data,repo)
        elif args.command == "ci-close":
            out = ci_close(db,args.run_id,args.evidence,repo)
        elif args.command == "next":
            out = next_tasks(db, args.slot, args.session, data, args.include_optional)
        elif args.command == "replace":
            out = replace_session(db, args.slot, args.previous, args.session, args.reason, repo)
        elif args.command == "heartbeat":
            assert_worktree(args.slot, repo)
            out = touch(db, args.slot, args.session)
        elif args.command == "claim":
            assert_worktree(args.slot, repo)
            out = claim(db,args.slot,args.session,args.task,data)
        elif args.command == "handoff":
            out = handoff(db,args.slot,args.session,args.task,args.commit,args.evidence,repo)
        elif args.command == "approve":
            out = approve(db,args.task,args.evidence,repo)
        elif args.command == "verify":
            out = verify_task(db,args.task,args.ci_sha,args.ci_run,args.evidence,repo)
        elif args.command == "defer":
            out = defer(db,args.task,args.reason,data,repo)
        elif args.command == "ci-claim":
            assert_worktree(args.slot, repo)
            out = ci_claim(db,args.slot,args.session,args.run_id,args.reason)
        elif args.command == "ci-finish":
            assert_worktree(args.slot, repo)
            out = ci_finish(db,args.slot,args.session,args.run_id,args.evidence)
        elif args.command == "publish-lock":
            out = publish_lock(db,args.slot,args.session,args.base,args.evidence,repo)
        else:
            out = publish_release(db,args.slot,args.session,args.evidence,repo)
        if args.command in ("verify", "manual-complete", "defer") and not args.db:
            write_checklist(db,data)
        print(json.dumps(out,ensure_ascii=False,indent=2))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (Blocked,sqlite3.Error,ValueError,OSError,subprocess.TimeoutExpired) as exc:
        print(f"BLOCKED: {exc}",file=sys.stderr)
        sys.exit(2)
