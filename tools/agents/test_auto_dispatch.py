#!/usr/bin/env python3
"""Source-only dispatcher tests; no Gemini calls and no live lane/CI mutations."""
import json
from pathlib import Path
import sqlite3
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auto_dispatch as auto
import triad


class DispatcherTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / "triad.sqlite3"
        self.db = triad.connect(self.path)
        triad.init(self.db)
        self.catalog = triad.catalog()

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def test_plan_never_claims_or_reallocates(self):
        before = self.db.execute("SELECT COUNT(*) FROM triad_tasks WHERE status='active'").fetchone()[0]
        with patch.object(triad, "database", return_value=self.path):
            plan = auto.read_plan(["B", "C"])
        self.assertEqual([item["slot"] for item in plan], ["B", "C"])
        self.assertFalse(plan[0]["runner_session_ready"])
        self.assertTrue(plan[0]["next"])
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM triad_tasks WHERE status='active'").fetchone()[0], before)
        self.assertFalse(self.db.execute("SELECT COUNT(*) FROM triad_tasks WHERE status='pending' AND slot IS NOT NULL").fetchone()[0])

    def test_existing_browser_session_never_becomes_headless_writer(self):
        with self.assertRaisesRegex(triad.Blocked, "explicit auto-b"):
            auto.require_auto_session(self.db, "B", "legacy-b")
        self.assertEqual(self.db.execute(
            "SELECT session FROM triad_slots WHERE slot='B'").fetchone()[0], "legacy-b")

    def test_replaced_clean_session_can_be_used(self):
        self.db.execute("UPDATE triad_slots SET session='auto-c-unique' WHERE slot='C'")
        with patch.object(triad, "assert_worktree"), patch.object(auto, "git", return_value=""):
            repo = auto.require_auto_session(self.db, "C", "auto-c-unique")
        self.assertTrue(str(repo).replace("\\", "/").endswith("/.agent-worktrees/chat-c"))
        with patch.object(triad, "assert_worktree"), patch.object(auto, "git", return_value=" M in-progress.kt"):
            with self.assertRaisesRegex(triad.Blocked, "uncommitted"):
                auto.require_auto_session(self.db, "C", "auto-c-unique")

    def test_live_active_claim_stops_new_writer(self):
        self.db.execute("UPDATE triad_slots SET session='auto-a-unique' WHERE slot='A'")
        task = next(t["id"] for t in self.catalog["tasks"] if t["owner"] == "A")
        self.db.execute("UPDATE triad_tasks SET slot='A',session='auto-a-unique',status='active' WHERE task_id=?", (task,))
        with patch.object(triad, "assert_worktree"), patch.object(auto, "git", return_value=""):
            with self.assertRaisesRegex(triad.Blocked, "active task"):
                auto.require_auto_session(self.db, "A", "auto-a-unique")

    def test_idle_b_auto_allocates_only_safe_coordinator_task(self):
        owned = [t["id"] for t in self.catalog["tasks"] if t["owner"] == "B"]
        self.db.executemany("UPDATE triad_tasks SET status='review',slot='B' WHERE task_id=?",
                            [(x,) for x in owned])
        with patch.object(triad, "assert_coordinator"):
            next_item = auto.select_task(self.db, "B", "legacy-b", self.catalog, True)
        self.assertEqual(next_item["task"], "NP-001")
        self.assertTrue(next_item["allocated"])
        self.assertIn("read-only", next_item["scope"].lower())
        reservation = self.db.execute("SELECT slot,status FROM triad_tasks WHERE task_id='NP-001'").fetchone()
        self.assertEqual((reservation["slot"], reservation["status"]), ("B", "pending"))
        self.assertEqual(self.db.execute("SELECT status FROM triad_tasks WHERE task_id='NP-004'").fetchone()[0], "pending")
        self.assertFalse(self.db.execute("SELECT COUNT(*) FROM triad_tasks WHERE status='active'").fetchone()[0])
        with patch.object(triad, "assert_coordinator"):
            self.assertEqual(auto.select_task(self.db, "B", "legacy-b", self.catalog, True)["task"], "NP-001")

    def test_auto_allocate_does_not_steal_from_c_or_device(self):
        self.db.execute("UPDATE triad_slots SET session='auto-c-unique' WHERE slot='C'")
        chosen = auto.select_task(self.db, "C", "auto-c-unique", self.catalog, True)
        self.assertEqual(chosen["task"], "NP-020")
        self.assertEqual(self.db.execute("SELECT status FROM triad_tasks WHERE task_id='REL-09'").fetchone()[0], "pending")
        self.assertFalse(self.db.execute("SELECT COUNT(*) FROM triad_tasks WHERE slot IS NOT NULL").fetchone()[0])

    def test_changed_paths_parses_file_list_without_shell(self):
        def fake_git(_repo, *args):
            if args[0] == "diff" and "--cached" in args:
                return "app/src/test/java/com/notestaking/app/ui/new.kt\0"
            if args[0] == "diff":
                return "app/src/main/java/com/notestaking/app/ui/Toolbar.kt\0"
            if args[0] == "ls-files":
                return "docs/agent-work/chat-b/STATUS.md\0"
            raise AssertionError(args)
        with patch.object(auto, "git", side_effect=fake_git):
            self.assertEqual(len(auto.changed_paths(Path("."))), 3)

    def test_scope_does_not_stage_out_of_lane_edits(self):
        repo = Path(".")
        def fake_git(_repo, *args):
            if args[:2] == ("rev-parse", "HEAD"):
                return "a" * 40
            if args[0] == "diff" and "--cached" in args:
                return ""
            if args[0] == "diff":
                return "app/src/main/java/com/notestaking/app/ui/Backstage.kt\0"
            if args[0] == "ls-files":
                return ""
            raise AssertionError(args)
        with patch.object(triad, "assert_worktree"), patch.object(auto, "git", side_effect=fake_git):
            with self.assertRaisesRegex(triad.Blocked, "unowned paths"):
                auto.check_task_scope(repo, "B", "a" * 40)

    def test_model_git_commit_is_blocked_before_staging(self):
        with patch.object(triad, "assert_worktree"), patch.object(auto, "git", return_value="b"*40):
            with self.assertRaisesRegex(triad.Blocked, "changed Git HEAD"):
                auto.check_task_scope(Path("."), "C", "a"*40)

    def test_os_mutex_excludes_duplicate_slot(self):
        with patch.object(triad, "database", return_value=self.path):
            with auto.slot_mutex("B"):
                with self.assertRaisesRegex(triad.Blocked, "another dispatcher"):
                    with auto.slot_mutex("B"):
                        pass
        with patch.object(triad, "database", return_value=self.path):
            with auto.slot_mutex("B"):
                pass

    def test_no_false_completion_in_prompt(self):
        task = {"task": "NP-036", "batch": "N", "title": "Shape recognition",
                "scope": "Only core/canvas tests"}
        prompt = auto.task_prompt("C", task, self.catalog)
        for phrase in ("Do not run Gradle", "do not commit", "or edit outside",
                       "Kotlin/JUnit", "tools/agents/check_lane.py"):
            self.assertIn(phrase.lower(), prompt.lower())
        self.assertIn("NT-030", str(self.catalog["tasks"]))

    def test_commit_handoff_uses_exact_local_sha_and_no_ci_claim(self):
        repo = Path(self.temp.name)
        status = repo / "docs/agent-work/chat-c/STATUS.md"
        status.parent.mkdir(parents=True)
        status.write_text("# Status\n", encoding="utf-8")
        task = {"task": "NP-036", "title": "Shape recognition", "batch": "N"}
        recorded = []
        def fake_git(_repo, *args):
            recorded.append(args)
            return "b" * 40 if args == ("rev-parse", "HEAD") else ""
        with patch.object(auto, "check_task_scope", return_value=[
            "app/src/test/java/com/notestaking/app/canvas/ShapeTest.kt"
        ]), patch.object(auto, "source_checks"), patch.object(auto, "git", side_effect=fake_git), patch.object(triad, "handoff") as handoff:
            sha = auto.commit_handoff(self.db, "C", "auto-c-session",
                                      task, repo, "a" * 40, "Documented shape tests")
        self.assertEqual(sha, "b" * 40)
        self.assertIn("Kotlin/JUnit, GitHub CI, hardware QA: not run locally",status.read_text(encoding="utf-8"))
        self.assertTrue(any(args[:2] == ("add", "--") for args in recorded))
        self.assertTrue(any(args[0] == "commit" for args in recorded))
        self.assertEqual(handoff.call_args.args[4], "b" * 40)
        self.assertIn("no local Gradle",handoff.call_args.args[5])

    def test_unavailable_provider_stops_before_claiming(self):
        self.db.execute("UPDATE triad_slots SET session='auto-c-session' WHERE slot='C'")
        with patch.object(triad, "database", return_value=self.path), patch.object(
                auto, "require_auto_session", return_value=Path(self.temp.name)), patch.object(
                auto, "gemini_executable", return_value="gemini"), patch.object(
                auto, "probe_runtime", side_effect=triad.Blocked("UNSUPPORTED_CLIENT")):
            with self.assertRaisesRegex(triad.Blocked, "UNSUPPORTED_CLIENT"):
                auto.work_slot("C", "auto-c-session", 1, False, 60, 100, False)
        self.assertEqual(self.db.execute(
            "SELECT COUNT(*) FROM triad_tasks WHERE status='active'").fetchone()[0], 0)

    def test_gemini_command_requests_sandbox_and_no_yolo(self):
        commands = []
        class FakeProcess:
            def wait(self, timeout=None):
                return 0
        def fake_start(command, **kwargs):
            commands.append(command)
            kwargs["stdout"].write(b'{"response":"Updated owned regression tests"}')
            kwargs["stdout"].flush()
            return FakeProcess()
        with patch.object(auto.subprocess, "Popen", side_effect=fake_start):
            result = auto.run_gemini("gemini", Path(self.temp.name), "Safe task",
                                     self.db, "C", "auto-c-session", 60)
        self.assertIn("Updated owned", result)
        self.assertIn("--sandbox", commands[0])
        self.assertIn("auto_edit", commands[0])
        self.assertNotIn("--yolo", commands[0])

    def test_gemini_probe_uses_sandbox_by_default(self):
        class FakeResponse:
            returncode = 0
            stdout = '{"response":"READY"}'
            stderr = ""
        with patch.object(auto.subprocess, "run", return_value=FakeResponse()) as proc:
            auto.probe_runtime("gemini", Path(self.temp.name))
        self.assertIn("--sandbox", proc.call_args.args[0])

    def test_probe_auth_failure_would_abort_before_task_claim(self):
        class Result:
            returncode = 1
            stdout = ""
            stderr = "IneligibleTierError UNSUPPORTED_CLIENT"
        with patch.object(auto.subprocess, "run", return_value=Result()):
            with self.assertRaisesRegex(triad.Blocked, "UNSUPPORTED_CLIENT"):
                auto.probe_runtime("gemini", Path("."), timeout=1)
        self.assertFalse(self.db.execute("SELECT COUNT(*) FROM triad_tasks WHERE status='active'").fetchone()[0])


if __name__ == "__main__":
    unittest.main()
