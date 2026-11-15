#!/usr/bin/env python3
"""Provision Chat C once, without deleting or resetting any existing worktree.

Run only from the top-level integration checkout AFTER this plan is published
to origin/developer. Does not start ChatGPT, claim a task, or build an APK.
"""
from __future__ import annotations

from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
WORKTREE = ROOT / ".agent-worktrees" / "chat-c"
BRANCH = "agent/chat-c-core-canvas-reliability"
BASE = "agent/chat-c-start-20261115"
PATTERN = "*/.agent-worktrees/chat-a|*/.agent-worktrees/chat-b"
NEW_PATTERN = PATTERN + "|*/.agent-worktrees/chat-c"


def git(*args: str) -> str:
    p = subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True,
                       text=True, timeout=30, check=False)
    if p.returncode:
        raise RuntimeError("git " + " ".join(args) + ": " + p.stderr.strip())
    return p.stdout.strip()


def safe_hooks() -> None:
    shared = Path(git("rev-parse", "--git-common-dir"))
    if not shared.is_absolute():
        shared = (ROOT / shared).resolve()
    for name in ("pre-commit", "pre-push"):
        path = shared / "hooks" / name
        if not path.is_file():
            raise RuntimeError(f"Missing existing {name} guard; do not silently install/overwrite hooks")
        data = path.read_text(encoding="utf-8")
        if NEW_PATTERN in data:
            continue
        if data.count(PATTERN) != 1:
            raise RuntimeError(f"Unknown {name} hook; manual coordinator review needed")
        path.write_text(data.replace(PATTERN, NEW_PATTERN, 1), encoding="utf-8", newline="\n")
        print(f"Updated common Git {name} to protect Chat C")


def main() -> None:
    if Path(git("rev-parse", "--show-toplevel")).resolve() != ROOT:
        raise RuntimeError("Must operate in administrative checkout")
    if git("branch", "--show-current").startswith("agent/chat-"):
        raise RuntimeError("Worker branch cannot provision a new worktree")
    remote = git("ls-remote", "origin", "refs/heads/developer").split()[0]
    local = git("rev-parse", "refs/remotes/origin/developer")
    if local != remote:
        raise RuntimeError("origin/developer tracking ref is stale; reconcile before provisioning")
    # Provision from the commit that contains the current policy and guard.
    if not (ROOT / "tools/agents/check_lane.py").is_file():
        raise RuntimeError("Ownership guard missing")
    if WORKTREE.exists():
        if Path(git("-C",str(WORKTREE),"rev-parse","--show-toplevel")).resolve() != WORKTREE.resolve():
            raise RuntimeError("Path occupied by a different checkout")
        if git("-C",str(WORKTREE),"branch","--show-current") != BRANCH:
            raise RuntimeError("Chat C worktree exists on unexpected branch")
        print("Chat C worktree exists; preserving all changes")
    else:
        for ref in (f"refs/heads/{BRANCH}",f"refs/heads/{BASE}"):
            if subprocess.run(["git","-C",str(ROOT),"show-ref","--verify","--quiet",ref],
                              check=False).returncode == 0:
                raise RuntimeError(f"Ref {ref} exists without expected worktree: stop for review")
        git("branch", BASE, remote)
        git("worktree", "add", "-b", BRANCH, str(WORKTREE), remote)
        print(f"Created isolated Chat C worktree at {remote[:12]}")
    safe_hooks()
    proc = subprocess.run(["python", "tools/agents/check_lane.py"],
                          cwd=str(WORKTREE), capture_output=True,text=True,check=False,timeout=30)
    if proc.returncode:
        raise RuntimeError("Chat C ownership guard failed: "+proc.stdout+proc.stderr)
    print(proc.stdout.strip())


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as e:
        print("BLOCKED:",e,file=sys.stderr)
        sys.exit(2)
