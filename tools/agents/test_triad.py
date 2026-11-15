#!/usr/bin/env python3
"""Fast source-only tests; never touch live Git state or build Android locally."""
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


if __name__ == "__main__":
    unittest.main()
