#!/usr/bin/env python3
"""Opt-in A/B/C headless coding dispatcher. Dry-run is the default.

Never shares a chat session, publishes Git, builds Android locally, or approves CI.
Requires a separately registered auto-{a,b,c}-... triad session for each worker.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from contextlib import closing, contextmanager
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time

import check_lane
import triad

ROOT = triad.ROOT
# These COORD tasks can be audited without modifying their production UI files.
B_AUDIT_TASKS = ("NP-001", "NP-003", "NP-005", "NP-009", "NP-013",
                 "NP-002", "NP-011", "NP-014", "NP-015", "NP-016")
B_AUDIT_SCOPE = (
    "Read-only audit of coordinator-owned library/onboarding/create UI and "
    "accessibility behavior. Add useful, executable regression tests ONLY under "
    "app/src/test/java/com/notestaking/app/ui/ and update "
    "docs/agent-work/chat-b/STATUS.md. No production UI, shared resources, "
    "A/C source, Gemini Quick Scratch, or other files. Report cross-owner fixes "
    "as dependencies instead of editing them."
)


def emit(event: str, **values) -> None:
    print(json.dumps({"event": event, **values}, ensure_ascii=False), flush=True)


def git(repo: Path, *args: str) -> str:
    return triad.git(repo, *args)


def require_auto_session(db, slot: str, session: str) -> Path:
    if not session.startswith(f"auto-{slot.lower()}-"):
        raise triad.Blocked(
            f"{slot}: running headless workers requires an explicit auto-{slot.lower()}- "
            "replacement session; never reuse an active browser chat's identity"
        )
    triad.assert_session(db, slot, session)
    repo = ROOT / ".agent-worktrees" / triad.BRANCHES[slot][0]
    triad.assert_worktree(slot, repo)
    if git(repo, "status", "--porcelain"):
        raise triad.Blocked(f"{slot}: existing uncommitted worker changes must be preserved")
    if db.execute("SELECT 1 FROM triad_tasks WHERE slot=? AND status='active'",
                  (slot,)).fetchone():
        raise triad.Blocked(f"{slot}: previous active task needs a safe handoff before automation")
    return repo


@contextmanager
def slot_mutex(slot: str):
    """OS-level lock, released on process death; two dispatchers cannot write one slot."""
    path = triad.database().parent / f"auto-dispatch-{slot}.lock"
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a+b") as lock:
        lock.seek(0, 2)
        if lock.tell() == 0:
            lock.write(b"0")
            lock.flush()
        lock.seek(0)
        if os.name == "nt":
            import msvcrt
            try:
                msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
            except OSError as exc:
                raise triad.Blocked(f"{slot}: another dispatcher owns the OS slot lock") from exc
        else:
            import fcntl
            try:
                fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except OSError as exc:
                raise triad.Blocked(f"{slot}: another dispatcher owns the OS slot lock") from exc
        try:
            yield
        finally:
            if os.name == "nt":
                import msvcrt
                lock.seek(0)
                msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.flock(lock.fileno(), fcntl.LOCK_UN)


def gemini_executable() -> str:
    exe = (shutil.which("gemini.cmd") if os.name == "nt" else None) or shutil.which("gemini")
    if not exe:
        raise triad.Blocked(
            "No Gemini CLI on PATH; install/configure an authenticated headless coding runner"
        )
    return exe


def probe_runtime(exe: str, repo: Path, timeout: int = 50,
                  sandbox: bool = True) -> None:
    """Never claim tasks when CLI authentication/worktree trust is broken."""
    try:
        result = subprocess.run(
            [exe, *(["--sandbox"] if sandbox else []),
             "--approval-mode", "plan", "--output-format", "json",
             "--prompt", "Reply READY only. Do not call tools or change files."],
            cwd=repo, capture_output=True, text=True, timeout=timeout,
            stdin=subprocess.DEVNULL, check=False,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise triad.Blocked(f"Gemini runtime probe failed: {exc}") from exc
    if result.returncode:
        reason = (result.stderr or result.stdout).strip()[-1200:]
        raise triad.Blocked("Gemini authentication/worktree trust failed: " + reason)


def changed_paths(repo: Path) -> list[str]:
    names: set[str] = set()
    for args in (("diff", "--name-only", "-z"),
                 ("diff", "--cached", "--name-only", "-z"),
                 ("ls-files", "--others", "--exclude-standard", "-z")):
        output = git(repo, *args)
        # triad.git strips trailing NUL; interior separators remain.
        names.update(x for x in output.split("\0") if x)
    return sorted(names)


def check_task_scope(repo: Path, slot: str, before_head: str) -> list[str]:
    triad.assert_worktree(slot, repo)
    if git(repo, "rev-parse", "HEAD") != before_head:
        raise triad.Blocked(f"{slot}: model changed Git HEAD; preserve state for manual review")
    changes = changed_paths(repo)
    bad = [p for p in changes if not check_lane.permitted("chat-" + slot.lower(), p)]
    if bad:
        raise triad.Blocked(f"{slot}: unowned paths touched, not staging: {bad}")
    work = [p for p in changes if p != f"docs/agent-work/chat-{slot.lower()}/STATUS.md"]
    if not work:
        raise triad.Blocked(f"{slot}: no source/test change; do not invent a task completion")
    return changes


def select_task(db, slot: str, session: str, data: dict, auto_allocate: bool) -> dict | None:
    tasks = triad.catalog_tasks(data)
    pending = [t for t in triad.next_tasks(db, slot, session, data, include_optional=True)
               if tasks[t["task"]]["kind"] == "feature"]
    if pending:
        return pending[0]
    if slot == "B" and auto_allocate:
        states = {r["task_id"]: r for r in db.execute(
            "SELECT task_id,status,slot FROM triad_tasks")}
        for task_id in B_AUDIT_TASKS:
            r = states[task_id]
            if r["status"] == "pending" and r["slot"] is None:
                triad.allocate(db, task_id, slot, B_AUDIT_SCOPE, data, ROOT)
                return triad.next_tasks(db, slot, session, data, include_optional=True)[0]
    return None


def task_prompt(slot: str, task: dict, data: dict) -> str:
    spec = triad.catalog_tasks(data)[task["task"]]
    return (
        f"You are an isolated NotesTaking App headless coding agent, lane {slot}.\n"
        f"Task {task['task']} (Batch {task['batch']}): {spec['title']}.\n"
        f"Acceptance: {spec['acceptance']}\n"
        f"Next evidence: {spec['next_evidence']}\n"
        f"Allocated scope: {task.get('scope') or 'Only this lane owned files.'}\n\n"
        "Read AGENTS.md and the appropriate project source, callers and tests. "
        "Inspect other lanes read-only. Work ONLY inside paths permitted by "
        f"tools/agents/check_lane.py for chat-{slot.lower()}. "
        "Do not access private note data, credentials, secrets, .git internals, "
        "other worktrees, build folders or global settings. "
        "Never use a terminal/shell tool, Git commands, network actions, "
        "or edit AGENTS.md, coordinator scripts, CI, policies or release artifacts. "
        "Do not run Gradle, Kotlin/JUnit, lint, APK builds or device tests locally. "
        "Make the smallest correct production/test change, or only relevant "
        "owned regression tests for a coordinator-owned feature. "
        "Do not invent completion or edit outside your file ownership. "
        "If a required fix is unowned, explain that in your final response. "
        "The dispatcher checks source/privacy/ownership, commits locally and "
        "hands off; do not commit, publish, push, merge or approve anything yourself. "
        "At the end, explain what changed, remaining hardware/CI checks, "
        "and any unowned dependencies."
    )


def run_gemini(exe: str, repo: Path, prompt: str, db, slot: str, session: str,
               timeout: int, sandbox: bool = True) -> str:
    args = [exe, *(["--sandbox"] if sandbox else []),
            "--approval-mode", "auto_edit", "--output-format", "json",
            "--prompt", prompt]
    with tempfile.TemporaryFile() as out, tempfile.TemporaryFile() as err:
        try:
            proc = subprocess.Popen(args, cwd=repo, stdin=subprocess.DEVNULL,
                                    stdout=out, stderr=err,
                                    env={**os.environ, "GIT_TERMINAL_PROMPT": "0"})
        except OSError as exc:
            raise triad.Blocked(f"{slot}: could not start Gemini: {exc}") from exc
        started = time.monotonic()
        try:
            while True:
                try:
                    code = proc.wait(timeout=30)
                    break
                except subprocess.TimeoutExpired:
                    if time.monotonic() - started > timeout:
                        raise triad.Blocked(f"{slot}: model exceeded {timeout}s")
                    triad.touch(db, slot, session)
        except BaseException:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()
            raise
        out.seek(0)
        err.seek(0)
        message = out.read(65536).decode("utf-8", errors="replace")
        errors = err.read(4096).decode("utf-8", errors="replace")
    if code:
        raise triad.Blocked(
            f"{slot}: coding CLI exit {code}; preserved files and active claim. "
            + (errors or message)[-1300:])
    try:
        response = json.loads(message).get("response", "")
    except ValueError:
        response = message
    return str(response).strip()[:1000]


def source_checks(repo: Path, slot: str) -> None:
    commands = [
        [sys.executable, "tools/ci/check_project.py"],
        [sys.executable, "tools/ci/check_backup_privacy.py"],
        [sys.executable, "tools/agents/check_lane.py"],
        ["git", "diff", "--check"],
    ]
    for args in commands:
        proc = subprocess.run(args, cwd=repo, capture_output=True,
                              text=True, timeout=120, check=False)
        if proc.returncode:
            raise triad.Blocked(f"{slot}: check failed ({' '.join(args)}): "
                                + (proc.stdout + proc.stderr)[-1400:])


def commit_handoff(db, slot: str, session: str, task: dict,
                   repo: Path, prior_head: str, response: str) -> str:
    paths = check_task_scope(repo, slot, prior_head)
    source_checks(repo, slot)
    status_file = repo / f"docs/agent-work/chat-{slot.lower()}/STATUS.md"
    if not status_file.is_file():
        raise triad.Blocked(f"{slot}: missing owned status file")
    with status_file.open("a", encoding="utf-8") as f:
        f.write(
            f"\n### Auto-dispatch handoff {task['task']}\n"
            f"- Scope: {task['title']} (Batch {task['batch']})\n"
            "- Checks: Python source, backup privacy, ownership, Git diff passed\n"
            "- Kotlin/JUnit, GitHub CI, hardware QA: not run locally; pending review\n"
            "- Changes committed to isolated worker branch; no publication\n"
        )
    paths = sorted(set(paths + [f"docs/agent-work/chat-{slot.lower()}/STATUS.md"]))
    git(repo, "add", "--", *paths)
    git(repo, "diff", "--cached", "--check")
    title = triad.catalog_tasks(triad.catalog()) [task["task"]]["title"]
    git(repo, "commit", "-m", f"chore({slot.lower()}): {task['task']} {title[:55]}")
    sha = git(repo, "rev-parse", "HEAD")
    evidence = (
        f"Headless Gemini CLI task {task['task']} on isolated {slot} worktree. "
        "Permitted source/privacy/ownership/Git checks passed; no local Gradle, "
        "Kotlin execution, device test or CI acceptance claimed. "
        f"Before {prior_head}; exact handoff {sha}. "
        f"Model summary: {response[:330] if response else 'No extra summary provided'}"
    )
    triad.handoff(db, slot, session, task["task"], sha, evidence, repo)
    return sha


def work_slot(slot: str, session: str, max_tasks: int,
              watch: bool, poll: int, model_timeout: int,
              auto_allocate: bool, sandbox: bool = True) -> dict:
    completed = 0
    with slot_mutex(slot), closing(triad.connect(triad.database())) as db:
        data = triad.catalog()
        repo = require_auto_session(db, slot, session)
        exe = gemini_executable()
        probe_runtime(exe, repo, sandbox=sandbox)  # before ANY claims or changes
        while max_tasks == 0 or completed < max_tasks:
            require_auto_session(db, slot, session)
            task = select_task(db, slot, session, data, auto_allocate)
            if task is None:
                emit("idle", slot=slot, completed=completed)
                if not watch:
                    break
                time.sleep(poll)
                continue
            before = git(repo, "rev-parse", "HEAD")
            triad.claim(db, slot, session, task["task"], data, repo)
            emit("claimed", slot=slot, task=task["task"], batch=task["batch"])
            try:
                response = run_gemini(exe, repo, task_prompt(slot, task, data),
                                      db, slot, session, model_timeout, sandbox=sandbox)
                sha = commit_handoff(db, slot, session, task, repo, before, response)
            except Exception as exc:
                emit("blocked", slot=slot, task=task["task"], reason=str(exc))
                # Never reset, auto-approve or roll back another agent's work.
                return {"slot": slot, "handed_off": completed,
                        "blocked_task": task["task"], "reason": str(exc)}
            emit("handed_off", slot=slot, task=task["task"], sha=sha,
                 next="continue locally; coordinator reviews separately")
            completed += 1
    return {"slot": slot, "handed_off": completed, "status": "idle or limit reached"}


def read_plan(slots: list[str]) -> list[dict]:
    with closing(triad.connect(triad.database())) as db:
        data = triad.catalog()
        rows = []
        for slot in slots:
            record = db.execute("SELECT session FROM triad_slots WHERE slot=?",
                                (slot,)).fetchone()
            session = (record["session"] if record else None) or ""
            eligible = triad.next_tasks(db, slot, session, data,
                                        include_optional=True) if session else []
            current = db.execute(
                "SELECT task_id,status FROM triad_tasks "
                "WHERE slot=? AND status='active'", (slot,)).fetchall()
            rows.append({"slot": slot, "session": session,
                         "runner_session_ready": session.startswith(f"auto-{slot.lower()}-"),
                         "active": [dict(x) for x in current],
                         "next": eligible[:3],
                         "auto_allocatable_b_audits": (
                             [t for t in B_AUDIT_TASKS if db.execute(
                                 "SELECT 1 FROM triad_tasks WHERE task_id=? "
                                 "AND status='pending' AND slot IS NULL", (t,)).fetchone()]
                             if slot == "B" else []
                         )})
        return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("status", "doctor", "run"))
    parser.add_argument("--slots", nargs="+", choices=triad.BRANCHES,
                        default=list(triad.BRANCHES))
    parser.add_argument("--session", action="append", default=[],
                        help="A=auto-a-unique (repeat for each slot)")
    parser.add_argument("--execute", action="store_true",
                        help="Explicitly enable AI edits/commits; otherwise run only previews")
    parser.add_argument("--watch", action="store_true",
                        help="Poll for new pending tasks when a slot becomes idle")
    parser.add_argument("--poll-seconds", type=int, default=60)
    parser.add_argument("--max-tasks-per-slot", type=int, default=5,
                        help="0 means unlimited; each task is still reviewed separately")
    parser.add_argument("--model-timeout", type=int, default=1200)
    parser.add_argument("--no-auto-allocate", action="store_true")
    parser.add_argument("--probe-runtime", action="store_true",
                        help="Doctor: check Gemini auth, trust and sandbox (remote call)")
    parser.add_argument("--allow-unsandboxed", action="store_true",
                        help="Unsafe: explicitly waive Gemini CLI sandbox; other worktrees may be readable")
    args = parser.parse_args()
    slots = list(dict.fromkeys(args.slots))
    if args.command != "run" or not args.execute:
        emit("plan", workers=read_plan(slots))
        if args.command == "doctor":
            try:
                exe = gemini_executable()
                emit("runtime_found", executable=exe)
                if args.probe_runtime:
                    for slot in slots:
                        repo = ROOT / ".agent-worktrees" / triad.BRANCHES[slot][0]
                        probe_runtime(exe, repo, sandbox=not args.allow_unsandboxed)
                        emit("runtime_auth_ok", slot=slot)
            except triad.Blocked as exc:
                emit("runtime_blocked", reason=str(exc))
                return 2
        return 0
    if args.poll_seconds < 30 or args.model_timeout < 60 or args.max_tasks_per_slot < 0:
        parser.error("Poll >=30s, model timeout >=60s, max tasks >=0 required")
    sessions: dict[str, str] = {}
    for pair in args.session:
        if "=" not in pair:
            parser.error("--session must be SLOT=auto-slot-unique")
        slot, token = pair.split("=", 1)
        if slot in sessions:
            parser.error("Duplicate slot session")
        sessions[slot] = token
    if set(sessions) != set(slots):
        parser.error("Provide one distinct, separately registered auto-* session per slot")
    with closing(triad.connect(triad.database())) as db:
        for slot in slots:
            require_auto_session(db, slot, sessions[slot])
    emit("start", slots=slots, continuous=args.watch,
         note="Never publishes; CI and device QA remain coordinator-only")
    with ThreadPoolExecutor(max_workers=len(slots)) as pool:
        futures = {pool.submit(work_slot, slot, sessions[slot],
                               args.max_tasks_per_slot, args.watch,
                               args.poll_seconds, args.model_timeout,
                               not args.no_auto_allocate,
                               not args.allow_unsandboxed): slot for slot in slots}
        results = []
        for future in as_completed(futures):
            slot = futures[future]
            try:
                results.append(future.result())
            except Exception as exc:
                results.append({"slot": slot, "status": "blocked", "reason": str(exc)})
        emit("finished", results=results)
        return 2 if any(r.get("status") == "blocked" or "blocked_task" in r
                        for r in results) else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (triad.Blocked, subprocess.SubprocessError, OSError) as exc:
        emit("blocked", reason=str(exc))
        sys.exit(2)
