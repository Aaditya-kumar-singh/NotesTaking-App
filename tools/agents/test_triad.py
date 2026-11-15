#!/usr/bin/env python3
"""Fast source-only tests; never touch live Git state or build Android locally."""
import json
import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import triad


class TriadTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.db = triad.connect(Path(self.tmp.name) / "claims.sqlite3")
        triad.init(self.db)
        self.data = triad.catalog()

    def tearDown(self):
        self.db.close()
        self.tmp.cleanup()

    def ids(self, owner):
        return [t["id"] for t in self.data["tasks"] if t["owner"] == owner
                and t["release_scope"] == "required"]

    def test_catalog_complete_and_five_per_batch(self):
        tasks = self.data["tasks"]
        self.assertEqual(len(tasks), 130)
        self.assertEqual(sum(len(t["nt_ids"]) for t in tasks), 79)
        self.assertEqual({t["batch"] for t in tasks}, set("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
        self.assertTrue(all(sum(t["batch"] == c for t in tasks) == 5
                            for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))

    def test_coordinator_allocates_pending_ui_task_without_taking_b_session(self):
        item="NP-019"  # P1 task originally routed to COORD
        scope=("Read-only audit of Backstage/NewNoteScreen create and empty-library "
               "behaviour; add regression tests only under "
               "app/src/test/java/com/notestaking/app/ui/ and B-owned status; "
               "do not modify shared production UI, A/C files or Gemini Quick Scratch.")
        with patch.object(triad,"assert_coordinator"):
            result=triad.allocate(self.db,item,"B",scope,self.data,triad.ROOT)
        self.assertEqual(result["status"],"pending")
        self.assertEqual(result["slot"],"B")
        self.assertEqual(self.db.execute("SELECT session FROM triad_slots WHERE slot='B'").fetchone()[0],"legacy-b")
        first=triad.next_tasks(self.db,"B","legacy-b",self.data)[0]
        self.assertEqual(first["task"],item)
        self.assertTrue(first["allocated"])
        self.assertIn("Backstage",first["scope"])
        with self.assertRaises(triad.Blocked):
            triad.claim(self.db,"C","legacy-b",item,self.data)
        triad.claim(self.db,"B","legacy-b",item,self.data)
        with patch.object(triad,"assert_coordinator"):
            with self.assertRaises(triad.Blocked):
                triad.allocate(self.db,item,"B",scope,self.data,triad.ROOT)

    def test_pin_next_canvas_task_without_changing_source_catalog(self):
        item="NP-036"
        scope=("Audit shape recognition and hold-to-snap in C-owned canvas/core "
               "source with pure regression tests. Preserve freehand stroke data and "
               "undo/redo; no editor, toolbar, formats, shared resources or other files.")
        with patch.object(triad,"assert_coordinator"):
            triad.allocate(self.db,item,"C",scope,self.data,triad.ROOT)
        self.db.execute("UPDATE triad_slots SET session='test-c' WHERE slot='C'")
        next_items=triad.next_tasks(self.db,"C","test-c",self.data,include_optional=True)
        self.assertEqual(next_items[0]["task"],item)
        # Reserved optional work must appear without --include-optional too.
        default_next=triad.next_tasks(self.db,"C","test-c",self.data)
        self.assertEqual(default_next[0]["task"],item)
        self.assertNotIn("NP-024",[offered["task"] for offered in default_next])
        self.assertEqual(self.data["tasks"][0]["id"],"NP-020")  # original order unchanged
        triad.claim(self.db,"C","test-c",item,self.data)
        self.assertEqual(self.db.execute("SELECT status FROM triad_tasks WHERE task_id='NP-024'").fetchone()[0],"pending")

    def test_allocate_rejects_unowned_source_and_gemini_or_device_tasks(self):
        scope=("Read-only audit and tests within app/src/test/java/com/notestaking/app/ui/ "
               "only; no source edits outside B-owned files. Report cross-owner fixes.")
        with patch.object(triad,"assert_coordinator"):
            with self.assertRaises(triad.Blocked):
                triad.allocate(self.db,"NP-004","B",scope,self.data,triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.allocate(self.db,"REL-09","B",scope,self.data,triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.allocate(self.db,"NP-024","B",scope,self.data,triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.allocate(self.db,"NP-019","B","Test everything",self.data,triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.allocate(self.db,"NP-019","B",
                    "Implement inside app/src/main/java/com/notestaking/app/ui/Backstage.kt "
                    "despite nonowned file limitations; shared code rewrite.",self.data,triad.ROOT)

    def test_bootstrap_preserves_live_claims(self):
        self.db.execute("CREATE TABLE claims(task_id TEXT PRIMARY KEY, agent TEXT, phase TEXT)")
        self.db.execute("INSERT INTO claims VALUES ('LANE-B','chat-b','active')")
        triad.init(self.db)
        row = self.db.execute("SELECT * FROM claims WHERE task_id='LANE-B'").fetchone()
        self.assertEqual((row["agent"],row["phase"]), ("chat-b","active"))

    def test_atomic_claim_and_slot_capacity(self):
        a, b = self.ids("A")[:2]
        c = triad.claim(self.db,"A","legacy-a",a,self.data)
        self.assertEqual(c["status"],"active")
        with self.assertRaises(triad.Blocked):
            triad.claim(self.db,"A","legacy-a",b,self.data)
        with self.assertRaises(triad.Blocked):
            triad.claim(self.db,"B","legacy-b",a,self.data)

    def test_double_claim_other_slot_cannot_take(self):
        a = self.ids("A")[0]
        triad.claim(self.db,"A","legacy-a",a,self.data)
        with self.assertRaises(triad.Blocked):
            triad.claim(self.db,"B","legacy-b",a,self.data)

    def test_replacement_fences_old_and_transfers_lease(self):
        a = self.ids("A")[0]
        triad.claim(self.db,"A","legacy-a",a,self.data)
        triad.ci_claim(self.db,"A","legacy-a","12345","Timeout downloading Gradle dependencies")
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"assert_worktree"), patch.object(triad,"git",return_value=""):
            result = triad.replace_session(self.db,"A","legacy-a","new-chat-a","User closed previous chat; handoff transferred",triad.ROOT)
        self.assertEqual(result["session"],"new-chat-a")
        self.assertEqual(self.db.execute("SELECT session FROM triad_tasks WHERE task_id=?",(a,)).fetchone()[0],"new-chat-a")
        self.assertEqual(self.db.execute("SELECT session FROM triad_incidents WHERE run_id='12345'").fetchone()[0],"new-chat-a")
        with self.assertRaises(triad.Blocked):
            triad.touch(self.db,"A","legacy-a")
        triad.touch(self.db,"A","new-chat-a")
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"assert_worktree"), patch.object(triad,"git",return_value=""):
            with self.assertRaises(triad.Blocked):
                triad.replace_session(self.db,"A","legacy-a","intruder","Owner did not authorize stale takeover",triad.ROOT)

    def test_replacement_rejects_dirty_worktree(self):
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"assert_worktree"), patch.object(triad,"git",return_value=" M notes.txt"):
            with self.assertRaises(triad.Blocked):
                triad.replace_session(self.db,"A","legacy-a","new","Old session closed cleanly and transferred",triad.ROOT)

    def test_ci_claim_exclusive_but_does_not_block_feature(self):
        triad.ci_claim(self.db,"A","legacy-a","777","Network outage during dependency download")
        with self.assertRaises(triad.Blocked):
            triad.ci_claim(self.db,"B","legacy-b","777","Try to fix someone else's live incident")
        task = self.ids("B")[0]
        self.assertEqual(triad.claim(self.db,"B","legacy-b",task,self.data)["status"],"active")
        triad.ci_finish(self.db,"A","legacy-a","777","Transient network failure; retry required, no code change.")
        self.assertEqual(self.db.execute("SELECT status FROM triad_incidents WHERE run_id='777'").fetchone()[0],"review")

    def test_defer_only_nonrequired_with_reason(self):
        optional = next(t["id"] for t in self.data["tasks"] if t["release_scope"]!="required")
        required = next(t["id"] for t in self.data["tasks"] if t["release_scope"]=="required")
        with patch.object(triad,"assert_coordinator"):
            self.assertEqual(triad.defer(self.db,optional,"Explicit scope decision: postpone optional functionality until after beta",self.data,triad.ROOT)["status"],"deferred")
            with self.assertRaises(triad.Blocked):
                triad.defer(self.db,required,"No evidence; would silently drop critical acceptance",self.data,triad.ROOT)

    def test_handoff_and_approval_do_not_imply_ci(self):
        task=self.ids("C")[0]
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"assert_worktree"), patch.object(triad,"git",return_value=""):
            triad.replace_session(self.db,"C",None,"new-chat-c","Provisioned the third dedicated owner worktree",triad.ROOT)
        triad.claim(self.db,"C","new-chat-c",task,self.data)
        sha="a"*40
        def git_stub(repo,*args):
            return sha if args==("rev-parse","HEAD") else ""
        with patch.object(triad,"assert_worktree"), patch.object(triad,"git",side_effect=git_stub):
            triad.handoff(self.db,"C","new-chat-c",task,sha,"Source review complete, device QA not run.",triad.ROOT)
        with patch.object(triad,"assert_coordinator"):
            result=triad.approve(self.db,task,"Reviewed commit and source checks, awaiting publication/CI.",triad.ROOT)
        self.assertEqual(result["status"],"local_ready")
        self.assertFalse(result["ci_passed"])

    def test_publish_lock_serialized_and_ci_claim_independent(self):
        base="f"*40
        def remote_stub(repo,*args):
            return base + "\trefs/heads/developer"
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"git",side_effect=remote_stub):
            triad.publish_lock(self.db,"A","legacy-a",base,"GitHub exact-SHA CI verified by coordinator on this branch.",triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.publish_lock(self.db,"B","legacy-b",base,"Second contributor should not publish concurrently.",triad.ROOT)
            triad.ci_claim(self.db,"B","legacy-b","333","Runner connection timed out during action")
            triad.publish_release(self.db,"A","legacy-a","Publication complete, remote SHA separately verified.",triad.ROOT)
        self.assertFalse(self.db.execute("SELECT * FROM triad_publish").fetchone())


    def test_pending_legacy_review_does_not_block_next_owned_local_task(self):
        checkpoint="e"*40
        repo=triad.ROOT / ".agent-worktrees" / "chat-b"
        self.db.execute(
            "CREATE TABLE claims(task_id TEXT PRIMARY KEY,phase TEXT,agent TEXT,"
            "worktree TEXT,branch TEXT,commit_sha TEXT)")
        self.db.execute(
            "INSERT INTO claims VALUES(?,?,?,?,?,?)",
            ("LANE-B","review","chat-b",str(repo),
             "agent/chat-b-g08-toolbar-accessibility",checkpoint))
        calls=[]
        def simulated_git(_repo,*args):
            calls.append(args)
            if args == ("status","--porcelain"):
                return ""
            if args == ("merge-base","--is-ancestor",checkpoint,"HEAD"):
                return ""
            raise AssertionError(f"unexpected Git call: {args!r}")
        task = self.ids("B")[0]
        with patch.object(triad,"git",side_effect=simulated_git):
            result = triad.claim(self.db,"B","legacy-b",task,self.data,repo)
        self.assertEqual(result["legacy_handoff_unchanged"], checkpoint)
        self.assertIn(("merge-base","--is-ancestor",checkpoint,"HEAD"),calls)
        legacy=self.db.execute("SELECT phase,commit_sha FROM claims WHERE task_id='LANE-B'").fetchone()
        self.assertEqual((legacy["phase"],legacy["commit_sha"]),("review",checkpoint))

    def test_legacy_review_requires_clean_descendant_not_rewritten_head(self):
        checkpoint="f"*40
        repo=triad.ROOT / ".agent-worktrees" / "chat-b"
        self.db.execute(
            "CREATE TABLE claims(task_id TEXT PRIMARY KEY,phase TEXT,agent TEXT,"
            "worktree TEXT,branch TEXT,commit_sha TEXT)")
        self.db.execute(
            "INSERT INTO claims VALUES(?,?,?,?,?,?)",
            ("LANE-B","review","chat-b",str(repo),
             "agent/chat-b-g08-toolbar-accessibility",checkpoint))
        task=self.ids("B")[0]
        def rewritten_checkpoint(_repo,*args):
            if args == ("status","--porcelain"):
                return ""
            if args == ("merge-base","--is-ancestor",checkpoint,"HEAD"):
                raise triad.Blocked("checkpoint is not ancestor")
            raise AssertionError(f"unexpected Git call {args!r}")
        with patch.object(triad,"git",side_effect=rewritten_checkpoint):
            with self.assertRaises(triad.Blocked):
                triad.claim(self.db,"B","legacy-b",task,self.data,repo)
        self.assertEqual(
            self.db.execute("SELECT status FROM triad_tasks WHERE task_id=?",(task,)).fetchone()[0],
            "pending")

    def test_reviewed_task_does_not_block_unrelated_next_task_same_slot(self):
        tasks=self.ids("C")[:2]
        self.db.execute("UPDATE triad_slots SET session='new-c' WHERE slot='C'")
        triad.claim(self.db,"C","new-c",tasks[0],self.data)
        self.db.execute("UPDATE triad_tasks SET status='review' WHERE task_id=?", (tasks[0],))
        next_task=triad.claim(self.db,"C","new-c",tasks[1],self.data)
        self.assertEqual(next_task["status"],"active")
        self.assertEqual(
            self.db.execute("SELECT status FROM triad_tasks WHERE task_id=?",(tasks[0],)).fetchone()[0],
            "review")


    def test_dedicated_coordinator_publisher_is_exclusive_and_does_not_steal_worker_sessions(self):
        base="c"*40
        def remote_stub(repo,*args):
            return base + "\trefs/heads/developer"
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"git",side_effect=remote_stub):
            triad.register_coordinator(self.db,"coordinator-first",None,
                "Coordinator starts integration without borrowing A B or C sessions",triad.ROOT)
            self.assertEqual(
                self.db.execute("SELECT session FROM triad_slots WHERE slot='A'").fetchone()[0],
                "legacy-a")
            triad.publish_lock(self.db,"COORD","coordinator-first",base,
                "Exact predecessor CI and five local reviewed tasks checked.",triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.publish_lock(self.db,"B","legacy-b",base,
                    "Second contributor cannot publish while coordinator holds lock.",triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.register_coordinator(self.db,"coordinator-second","coordinator-first",
                    "Attempted takeover while a publish operation remains locked",triad.ROOT)
            triad.publish_release(self.db,"COORD","coordinator-first",
                "Verified integration outcome and recorded remote SHA state.",triad.ROOT)
            triad.register_coordinator(self.db,"coordinator-second","coordinator-first",
                "Explicit replacement after coordinator publication was released.",triad.ROOT)
            with self.assertRaises(triad.Blocked):
                triad.publish_lock(self.db,"COORD","coordinator-first",base,
                    "Old coordinator session should no longer be allowed to publish.",triad.ROOT)
            triad.publish_lock(self.db,"COORD","coordinator-second",base,
                "New coordinator holds publication rights after safe takeover.",triad.ROOT)
            triad.publish_release(self.db,"COORD","coordinator-second",
                "Released new exclusive lock after verification.",triad.ROOT)

    def test_batch_ready_requires_all_five_reviews_or_deferrals(self):
        self.assertFalse(triad.batch_ready(self.db,"A",self.data)["ready_for_integration_review"])
        self.assertEqual(len(triad.batch_ready(self.db,"A",self.data)["remaining"]),5)
        with self.assertRaises(triad.Blocked):
            triad.batch_ready(self.db,"AA",self.data)

    def test_manual_device_requires_external_evidence(self):
        device = next(t["id"] for t in self.data["tasks"] if t["owner"] == "DEVICE")
        with patch.object(triad,"assert_coordinator"):
            with self.assertRaises(triad.Blocked):
                triad.manual_complete(self.db,device,"No device",self.data,triad.ROOT)
            result=triad.manual_complete(self.db,device,
                "Observed actual physical device session, Android build SHA, test script and recorded outcome.",
                self.data,triad.ROOT)
        self.assertEqual(result["status"],"device_verified")

    def test_ci_incident_requires_review_before_close(self):
        triad.ci_claim(self.db,"A","legacy-a","505","Concrete CI runner error in test job")
        with patch.object(triad,"assert_coordinator"):
            with self.assertRaises(triad.Blocked):
                triad.ci_close(self.db,"505","Attempt to close before handoff",triad.ROOT)
        triad.ci_finish(self.db,"A","legacy-a","505","Root cause network, CI rerun on original SHA awaited.")
        with patch.object(triad,"assert_coordinator"):
            triad.ci_close(self.db,"505","Coordinator reviewed logs and correct retry for exact commit SHA.",triad.ROOT)
        self.assertFalse(triad.status(self.db,self.data)["ci_incidents"])


    def test_local_ready_does_not_tick_batch(self):
        cards = [t["id"] for t in self.data["tasks"] if t["batch"] == "A"]
        for task in cards:
            self.db.execute("UPDATE triad_tasks SET status='local_ready' WHERE task_id=?", (task,))
        self.assertTrue(triad.batch_ready(self.db,"A",self.data)["ready_for_integration_review"])
        self.assertFalse(triad.batch_progress(self.db,self.data)["A"]["checked"])
        self.assertIn("- [ ] **Batch A** - 0/5 resolved",triad.checklist(self.db,self.data))
        self.assertEqual(triad.status(self.db,self.data)["completed_batches"],0)

    def test_fifth_verified_task_automatically_ticks_batch(self):
        cards = [t["id"] for t in self.data["tasks"] if t["batch"] == "A"]
        for task in cards:
            self.db.execute("UPDATE triad_tasks SET status='verified' WHERE task_id=?", (task,))
        self.db.execute("UPDATE triad_tasks SET status='local_ready' WHERE task_id=?", (cards[-1],))
        self.assertFalse(triad.batch_progress(self.db,self.data)["A"]["checked"])
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"github_ci_passed",return_value=True):
            out=triad.verify_task(self.db,cards[-1],"a"*40,"43679116911",
                "Exact SHA CI passed, regression checks reviewed and applicable device QA signed off.",triad.ROOT)
        self.assertEqual(out["status"],"verified")
        self.assertTrue(triad.batch_progress(self.db,self.data)["A"]["checked"])
        self.assertIn("- [x] **Batch A** - 5/5 resolved (5 verified, 0 deferred)",triad.checklist(self.db,self.data))
        self.assertEqual(triad.status(self.db,self.data)["completed_batches"],1)
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"github_ci_passed",return_value=True):
            with self.assertRaises(triad.Blocked):
                triad.verify_task(self.db,cards[-1],"a"*40,"43679116911",
                    "Duplicate verification must not rewrite an already accepted task status.",triad.ROOT)

    def test_failed_ci_never_checks_batch(self):
        t=next(t["id"] for t in self.data["tasks"] if t["batch"] == "A")
        self.db.execute("UPDATE triad_tasks SET status='local_ready' WHERE task_id=?", (t,))
        with patch.object(triad,"assert_coordinator"), patch.object(triad,"github_ci_passed",return_value=False):
            with self.assertRaises(triad.Blocked):
                triad.verify_task(self.db,t,"b"*40,"43682160649",
                    "CI is still running; claiming complete would conceal the actual build status.",triad.ROOT)
        self.assertEqual(self.db.execute("SELECT status FROM triad_tasks WHERE task_id=?", (t,)).fetchone()[0],"local_ready")
        self.assertFalse(triad.batch_progress(self.db,self.data)["A"]["checked"])

    def test_optional_deferral_explicit_in_batch_tick(self):
        tasks=[t["id"] for t in self.data["tasks"] if t["batch"]=="V"]
        self.assertEqual(len(tasks),5)
        self.assertTrue(all(t["release_scope"]=="defer_by_decision"
                            for t in self.data["tasks"] if t["batch"]=="V"))
        with patch.object(triad,"assert_coordinator"):
            for t in tasks:
                triad.defer(self.db,t,
                    "Coordinator explicitly postponed this P2/P3 feature until a future post-release scope.",self.data,triad.ROOT)
        self.assertTrue(triad.batch_progress(self.db,self.data)["V"]["checked"])
        self.assertIn("- [x] **Batch V** - 5/5 resolved (0 verified, 5 deferred)",triad.checklist(self.db,self.data))

    def test_plan_tick_updates_only_matching_batch_line(self):
        path=triad.ROOT / "docs/github/APK_BATCH_A_TO_Z.md"
        original=path.read_text(encoding="utf-8")
        cards=[t["id"] for t in self.data["tasks"] if t["batch"]=="A"]
        self.db.executemany("UPDATE triad_tasks SET status='verified' WHERE task_id=?",
                            [(id_,) for id_ in cards])
        patched=triad.update_plan_checkboxes(original,triad.batch_progress(self.db,self.data))
        self.assertIn("- [x] **Batch A resolved** - 5/5 (5 verified, 0 deferred)",patched)
        self.assertIn("- [ ] **Batch B resolved** - 0/5 (0 verified, 0 deferred)",patched)
        with self.assertRaises(triad.Blocked):
            triad.update_plan_checkboxes("## Batch A only",triad.batch_progress(self.db,self.data))

    def test_exact_sha_ci_verification(self):
        from io import BytesIO
        class FakeResponse(BytesIO):
            def __enter__(self): return self
            def __exit__(self,*_): self.close()
        payload=json.dumps({"head_sha":"a"*40,"status":"completed","conclusion":"success"}).encode()
        with patch.object(triad.urllib.request,"urlopen",return_value=FakeResponse(payload)):
            self.assertTrue(triad.github_ci_passed("43679116911","a"*40))
        with patch.object(triad.urllib.request,"urlopen",return_value=FakeResponse(payload)):
            self.assertFalse(triad.github_ci_passed("43679116911","b"*40))
        with self.assertRaises(triad.Blocked):
            triad.github_ci_passed("invalid","a"*40)


if __name__ == "__main__":
    unittest.main()
