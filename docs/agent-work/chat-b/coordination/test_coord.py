"""Source-only tests for the cooperative task broker (no Android tooling involved)."""
from __future__ import annotations

import copy
import sqlite3
import subprocess
import tempfile
import threading
import unittest
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from pathlib import Path

import coord


class BrokerTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = Path(self.tmp.name) / "repo"
        self.repo.mkdir()
        self._git("init", "-q", "-b", "dev")
        (self.repo / "README.md").write_text("temporary test repository\n")
        self._git("add", ".")
        self._git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                  "commit", "-qm", "test baseline")
        self.db = self.repo / ".git" / "test-coordinator.sqlite3"
        self.board = {
            "version": 1,
            "protected_paths": ["src/protected/**", "AGENTS.md"],
            "agents": {
                "a": {
                    "enabled": True, "read_only": False,
                    "worktree": ".", "branch": "dev",
                    "capacity": 1, "max_risk": "high", "max_difficulty": 5,
                    "skills": {"code": 5, "research": 4},
                    "allowed_paths": ["src/**"],
                },
                "b": {
                    "enabled": True, "read_only": False,
                    "worktree": ".", "branch": "dev",
                    "capacity": 1, "max_risk": "medium", "max_difficulty": 4,
                    "skills": {"code": 4, "research": 4},
                    "allowed_paths": ["src/**"],
                },
                "go": {
                    "enabled": True, "read_only": True,
                    "worktree": None, "branch": None,
                    "capacity": 1, "max_risk": "low", "max_difficulty": 3,
                    "skills": {"research": 4}, "allowed_paths": [],
                },
            },
            "tasks": [
                {"id": "A", "title": "One", "paths": ["src/one.py"], "priority": "P1",
                 "risk": "low", "difficulty": 2, "skills": ["code"], "deps": []},
                {"id": "B", "title": "Collision", "paths": ["src/one.py"], "priority": "P1",
                 "risk": "low", "difficulty": 2, "skills": ["code"], "deps": []},
                {"id": "C", "title": "Separate", "paths": ["src/other.py"], "priority": "P2",
                 "risk": "low", "difficulty": 2, "skills": ["code"], "deps": []},
                {"id": "D", "title": "Research", "paths": [], "priority": "P2",
                 "risk": "low", "difficulty": 2, "skills": ["research"], "deps": []},
                {"id": "E", "title": "Dependent", "paths": ["src/dependent.py"], "priority": "P0",
                 "risk": "low", "difficulty": 2, "skills": ["code"], "deps": ["A"]},
                {"id": "F", "title": "Protected", "paths": ["src/protected/token.py"],
                 "priority": "P0", "risk": "low", "difficulty": 2,
                 "skills": ["code"], "deps": []},
                {"id": "G", "title": "High-risk", "paths": ["src/unsafe.py"],
                 "priority": "P0", "risk": "high", "difficulty": 4,
                 "skills": ["code"], "deps": []},
            ],
        }

    def _git(self, *args):
        subprocess.run(["git", "-C", str(self.repo), *args], check=True,
                       capture_output=True, text=True)

    def claim(self, agent, task):
        with coord.connect(self.db) as db:
            return coord.claim(db, self.board, agent, self.repo, task)

    def state(self):
        with coord.connect(self.db) as db:
            return coord.claims_by_id(db)

    def test_existing_assignments_seed_idempotently(self):
        self.board["tasks"][0]["reserve_for"] = "a"
        with coord.connect(self.db) as db:
            self.assertEqual(["A"], coord.seed(db, self.board))
            self.assertEqual([], coord.seed(db, self.board))
        self.assertEqual("reserved", self.state()["A"]["phase"])
        self.assertEqual("a", self.state()["A"]["agent"])
        self.assertEqual("A", self.claim("a", "A")["id"])
        self.assertEqual("active", self.state()["A"]["phase"])

    def test_conflicting_paths_block_other_agent_but_disjoint_paths_work(self):
        self.claim("a", "A")
        with self.assertRaisesRegex(coord.Blocked, "already|overlap"):
            self.claim("b", "B")
        self.claim("b", "C")
        self.assertEqual({"A", "C"}, set(self.state()))

    def test_readonly_analyst_can_only_claim_readonly_task(self):
        self.claim("go", "D")
        with self.assertRaisesRegex(coord.Blocked, "ownership|risk|capacity"):
            self.claim("go", "A")
        self.assertIsNone(self.state()["D"]["branch"])

    def test_dependencies_require_verified_handoff_not_just_commit(self):
        with self.assertRaisesRegex(coord.Blocked, "dependency"):
            self.claim("b", "E")
        self.claim("a", "A")
        head = coord.git(self.repo, "rev-parse", "HEAD")
        with coord.connect(self.db) as db:
            coord.update(db, self.board, "a", "A", self.repo,
                         "handoff", "Source tests reviewed, commit ready", head)
        with self.assertRaisesRegex(coord.Blocked, "dependency"):
            self.claim("b", "E")
        with coord.connect(self.db) as db:
            coord.close(db, self.board, "A",
                        "Manual external CI evidence: exact integration SHA, test pass")
        self.claim("b", "E")

    def test_protected_and_out_of_scope_paths_are_never_assigned(self):
        with self.assertRaisesRegex(coord.Blocked, "ownership"):
            self.claim("a", "F")
        with self.assertRaisesRegex(coord.Blocked, "risk"):
            self.claim("b", "G")
        self.assertEqual({}, self.state())

    def test_two_simultaneous_claims_only_one_wins(self):
        gate = threading.Barrier(2)
        def attempt(agent):
            gate.wait(timeout=5)
            try:
                self.claim(agent, "A")
                return "ok"
            except coord.Blocked:
                return "blocked"
        with ThreadPoolExecutor(max_workers=2) as ex:
            results = list(ex.map(attempt, ("a", "b")))
        self.assertEqual(["blocked", "ok"], sorted(results))
        self.assertEqual(1, len(self.state()))

    def test_expired_heartbeat_never_stolen_automatically(self):
        self.claim("a", "A")
        old = (datetime.now(timezone.utc) - timedelta(hours=8)).isoformat()
        with coord.connect(self.db) as db:
            db.execute("UPDATE claims SET heartbeat_at=? WHERE task_id='A'", (old,))
            warnings = coord.doctor(db, self.board, self.repo)
        self.assertTrue(any("stale" in w for w in warnings))
        with self.assertRaises(coord.Blocked):
            self.claim("b", "B")
        self.assertEqual("a", self.state()["A"]["agent"])

    def test_wrong_branch_fails_without_mutation(self):
        mismatch = copy.deepcopy(self.board)
        mismatch["agents"]["a"]["branch"] = "different"
        with coord.connect(self.db) as db:
            with self.assertRaisesRegex(coord.Blocked, "wrong branch"):
                coord.claim(db, mismatch, "a", self.repo, "A")
        self.assertEqual({}, self.state())

    def test_handoff_requires_clean_worktree_and_matching_head(self):
        self.claim("a", "A")
        (self.repo / "README.md").write_text("uncommitted data")
        head = coord.git(self.repo, "rev-parse", "HEAD")
        with coord.connect(self.db) as db:
            with self.assertRaisesRegex(coord.Blocked, "Commit or"):
                coord.update(db, self.board, "a", "A", self.repo,
                             "handoff", "Ready for external review", head)
        self.assertEqual("active", self.state()["A"]["phase"])

    def test_scope_matching_is_conservative(self):
        self.assertTrue(coord.overlaps("src/file.py", "src/**"))
        self.assertTrue(coord.overlaps("src/ToolA.kt", "src/Tool*.kt"))
        self.assertFalse(coord.overlaps("src/foo.py", "src/bar.py"))
        self.assertTrue(coord.within_permission("src/foo.py", "src/**"))
        self.assertFalse(coord.within_permission("src/**", "src/foo.py"))
        self.assertFalse(coord.valid_scope("../escape"))
        self.assertFalse(coord.valid_scope("C:/users/secret"))

    def test_current_repository_manifest_seeds_without_overlap(self):
        real_board = coord.config(coord.HERE / "board.json")
        with coord.connect(self.db) as db:
            self.assertEqual(3, len(coord.seed(db, real_board)))
            self.assertFalse(any("overlap" in w for w in coord.doctor(db, real_board, self.repo)))


if __name__ == "__main__":
    unittest.main()
