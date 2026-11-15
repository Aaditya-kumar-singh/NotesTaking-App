#!/usr/bin/env python3
"""Coordinator-only: approve a reviewed Chat B checkpoint and resume its existing claim.

The worker's broker pilot lacks a review->active command. This one stays in
the integration-only tools/agents directory and uses the pilot's shared board,
SQLite transaction and event log without modifying any worker checkout.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKTREE = ROOT / ".agent-worktrees" / "chat-b"
BROKER_DIR = WORKTREE / "docs" / "agent-work" / "chat-b" / "coordination"
sys.path.insert(0, str(BROKER_DIR))
import coord  # noqa: E402


def reactivate(handoff: str, integration: str, run_id: int, dry_run: bool) -> dict:
    if len(handoff) != 40 or len(integration) != 40:
        raise coord.Blocked("Use full, exact 40-character commit SHAs")
    if coord.checkout_root(ROOT) != ROOT or coord.checkout_root(WORKTREE) != WORKTREE:
        raise coord.Blocked("Coordinator or Chat B worktree root mismatch")
    board = coord.config(BROKER_DIR / "board.json")
    agent = board["agents"]["chat-b"]
    if agent["read_only"] or (ROOT / agent["worktree"]).resolve() != WORKTREE:
        raise coord.Blocked("Chat B's approved worktree changed")
    if coord.git(WORKTREE, "branch", "--show-current") != agent["branch"]:
        raise coord.Blocked("Chat B branch does not match its reservation")
    if coord.git(WORKTREE, "rev-parse", "HEAD") != handoff:
        raise coord.Blocked("Chat B HEAD changed since the reviewed handoff")
    if coord.git(WORKTREE, "status", "--porcelain"):
        raise coord.Blocked("Chat B worktree is not clean")
    # The coordinator may have the next local batch staged atop the published integration.
    coord.git(ROOT, "merge-base", "--is-ancestor", integration, "HEAD")
    remote = coord.git(ROOT, "ls-remote", "origin", "refs/heads/developer").split()[0]
    if remote != integration:
        raise coord.Blocked("Remote developer is not at the reviewed integration SHA")

    db_path = coord.shared_dir(ROOT) / "agent-coordination" / "claims.sqlite3"
    if not db_path.is_file():
        raise coord.Blocked("Existing coordinator claims database not found")
    evidence = (
        f"Reviewed Chat B handoff {handoff}; developer integration {integration}; "
        f"GitHub Android CI https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/{run_id} "
        "completed successfully (source/privacy and Kotlin tests/lint/debug APK); "
        "physical keyboard, TalkBack and stylus acceptance remain pending"
    )
    with coord.connect(db_path) as db:
        db.execute("BEGIN IMMEDIATE")
        try:
            row = db.execute("SELECT * FROM claims WHERE task_id='LANE-B'").fetchone()
            if not row or row["phase"] != "review" or row["agent"] != "chat-b":
                raise coord.Blocked("LANE-B is not in Chat B's review state")
            if row["commit_sha"] != handoff or row["worktree"] != str(WORKTREE) or row["branch"] != agent["branch"]:
                raise coord.Blocked("Review claim differs from the approved handoff")
            blockers = coord.blocking_reasons(
                next(t for t in board["tasks"] if t["id"] == "LANE-B"),
                "chat-b", board, coord.claims_by_id(db),
            )
            if blockers:
                raise coord.Blocked("Cannot reactivate: " + "; ".join(blockers))
            if dry_run:
                db.rollback()
                return {"status": "eligible", "task": "LANE-B", "handoff": handoff,
                        "integration": integration, "ci_run": run_id, "changed": False}
            note = f"Reviewed handoff {handoff} on developer {integration}; resume owned G08 work; device QA pending"
            cur = db.execute(
                "UPDATE claims SET phase='active', note=?, evidence=NULL, heartbeat_at=? "
                "WHERE task_id='LANE-B' AND agent='chat-b' AND phase='review' AND commit_sha=?",
                (note, coord.utc_now(), handoff),
            )
            if cur.rowcount != 1:
                raise coord.Blocked("Concurrent coordinator update; no claim changed")
            coord.event(db, "LANE-B", "coordinator", "review-approved-reactivated", evidence)
            db.commit()
            return {"status": "active", "task": "LANE-B", "handoff": handoff,
                    "integration": integration, "ci_run": run_id, "changed": True}
        except Exception:
            db.rollback()
            raise


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--handoff", required=True, help="Full reviewed Chat B HEAD SHA")
    p.add_argument("--integration", required=True, help="Full exact-SHA developer integration")
    p.add_argument("--ci-run", required=True, type=int, help="Reviewed successful Android CI run ID")
    p.add_argument("--dry-run", action="store_true")
    a = p.parse_args()
    try:
        print(json.dumps(reactivate(a.handoff, a.integration, a.ci_run, a.dry_run), indent=2))
        return 0
    except (coord.Blocked, ValueError, OSError, StopIteration) as exc:
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
