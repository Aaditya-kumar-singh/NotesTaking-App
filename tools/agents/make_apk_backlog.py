#!/usr/bin/env python3
"""Generate the one-time A-Z APK task catalog from the existing NP/NT trackers.

--write is a deliberate planning migration: it does not update runtime claims.
--check verifies the committed snapshot, without rewriting statuses or sessions.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs/notein-parity"
OUT = ROOT / "docs/github"
PRIORITY = {"P0": 0, "P1": 1, "P2": 2, "P3": 3}

# An owner is a routing suggestion, never permission to modify an unowned path.
A = set(range(53, 89)) | {6, 7, 8, 10, 12, 17, 18, 71, 72, 73, 74, 100, 101, 106}
B = {32, 33, 34, 35, 37, 52, 66, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100}
C = set(range(20, 32)) | set(range(36, 52)) | {56, 67, 68, 69, 70, 75, 76, 77, 78, 79, 80, 96, 98, 102}
# Specific UI ownership wins over generic PDF/core suggestions.
GEMINI = {4}

RELEASE = [
    ("Audit exact developer SHA and required CI jobs", "COORD", "P0"),
    ("Reconcile three agent sessions and active file claims", "COORD", "P0"),
    ("Review all P0/P1 feature evidence and open exceptions", "COORD", "P0"),
    ("Reopen old xnote/xcanvas/xtemplate fixtures without migration loss", "A", "P0"),
    ("Verify hostile ZIP, encrypted PDF and limit regression coverage", "A", "P0"),
    ("Test PDF import/export and source preservation with SAF", "A", "P0"),
    ("Test force-stop, zero-byte and low-storage save recovery", "A", "P0"),
    ("Measure 200-page PDFs and memory/jank with Perfetto", "DEVICE", "P0"),
    ("Validate palm rejection, mixed touch and stylus pressure", "DEVICE", "P0"),
    ("Validate eraser segments, dense ink and undo/redo", "DEVICE", "P0"),
    ("Validate zoom, crop, rotation, split pane and page anchoring", "DEVICE", "P0"),
    ("Check TalkBack, keyboard, touch targets and large fonts", "DEVICE", "P0"),
    ("Check offline startup, localization and permission-denial cases", "DEVICE", "P0"),
    ("Run and verify full-ABI GitHub CI on exact release candidate SHA", "COORD", "P0"),
    ("Review privacy, backup, dependencies and secret hygiene", "COORD", "P0"),
    ("Record device QA evidence, outstanding defects and waivers", "COORD", "P0"),
    ("Reconcile unique main/master changes; create rollback refs", "COORD", "P0"),
    ("Verify release version, tag, keystore secrets and key continuity", "COORD", "P0"),
    ("Run signed release workflow; verify signature, ABI and SHA256", "COORD", "P0"),
    ("Review release notes, package distribution and final signoff", "COORD", "P0"),
]


def refs(text: str) -> list[int]:
    result = []
    for m in re.finditer(r"NP-(\d{3})(?:\s*[\u2013-]\s*NP-(\d{3}))?", text):
        start = int(m.group(1))
        end = int(m.group(2)) if m.group(2) else start
        if end < start or end > 110:
            raise ValueError("Invalid NP range " + m.group(0))
        result.extend(range(start, end + 1))
    return list(dict.fromkeys(result))


def rows(filename: str, prefix: str) -> list[list[str]]:
    out = []
    for line in (SOURCE / filename).read_text(encoding="utf-8").splitlines():
        if line.startswith("| " + prefix + "-"):
            cols = [s.strip() for s in line.split("|")[1:-1]]
            if len(cols) >= 6:
                out.append(cols)
    return out


def owner(n: int) -> str:
    if n in GEMINI:
        return "GEMINI"
    if n in B:
        return "B"
    if n in C:
        return "C"
    if n in A:
        return "A"
    return "COORD"


def generate() -> dict:
    features = rows("FEATURE_MATRIX.md", "NP")
    nt = rows("TEST_PLAN.md", "NT")
    if len(features) != 110 or {x[0] for x in features} != {f"NP-{n:03d}" for n in range(1, 111)}:
        raise ValueError("Feature matrix must contain NP-001 through NP-110 exactly once")
    if len(nt) != 79 or {x[0] for x in nt} != {f"NT-{n:03d}" for n in range(1, 80)}:
        raise ValueError("Test plan must contain NT-001 through NT-079 exactly once")
    features.sort(key=lambda r: (PRIORITY[r[1]], int(r[0][3:])))
    cards = []
    for row in features:
        num = int(row[0][3:])
        pri = row[1]
        cards.append({
            "id": row[0], "kind": "feature", "priority": pri,
            "title": row[2], "source_state": row[3],
            "next_evidence": row[4], "acceptance": row[5],
            "owner": owner(num), "release_scope": "required" if pri in ("P0", "P1") else "defer_by_decision",
            "nt_ids": [],
        })
    for index, (title, slot, pri) in enumerate(RELEASE, start=1):
        cards.append({
            "id": f"REL-{index:02d}", "kind": "release", "priority": pri,
            "title": title, "source_state": "NOT_VERIFIED",
            "next_evidence": "Record exact reproducible evidence and pass/fail, never assume acceptance.",
            "acceptance": "Evidence signed off by coordinator; missing physical tests recorded as blockers.",
            "owner": slot, "release_scope": "required", "nt_ids": [],
        })
    # Every numbered acceptance test has one accountable task; tests may cover
    # additional NP IDs, which remain visible in the original TEST_PLAN.md.
    by_id = {task["id"]: task for task in cards}
    for row in nt:
        number = int(row[0][3:])
        targets = [f"NP-{n:03d}" for n in refs(row[3])]
        if number <= 7:  # CI gate: map broad NT-001..NT-007 to release tasks.
            task = by_id[f"REL-{number:02d}"]
        else:
            targets = [t for t in targets if t in by_id]
            if not targets:
                raise ValueError(row[0] + ": no NP assignment")
            task = by_id[targets[0]]
        task["nt_ids"].append(row[0])
    assert len(cards) == 130 and sum(len(c["nt_ids"]) for c in cards) == 79
    for index, task in enumerate(cards):
        task["batch"] = chr(ord("A") + index // 5)
        task["order"] = index % 5 + 1
    return {
        "version": 1, "project": "NotesTaking-App",
        "source": ["docs/notein-parity/FEATURE_MATRIX.md", "docs/notein-parity/TEST_PLAN.md"],
        "description": "Snapshot of 110 NP feature checks plus 20 release gates, exactly five tasks per A-Z batch.",
        "status_authority": "SQLite in common Git directory, not this snapshot",
        "tasks": cards,
    }


def markdown(data: dict) -> str:
    lines = [
        "# NotesTaking App - A-Z pre-APK task list",
        "",
        "This is the complete **planning queue**, not evidence that tasks have failed or passed. "
        "Many SOURCE/PARTIAL rows already have CI-tested code. See the original NP matrix, "
        "NT plan, and the live coordinator before making changes.",
        "",
        "**26 batches A-Z, exactly five tasks each (130 tasks):** 110 NP feature checks "
        "and 20 release-readiness checks. Priority is P0 -> P1 -> P2 -> P3, with "
        "release gates W-Z. Beta builds can continue before all tasks are accepted; "
        "P2/P3 scope requires explicit deferral decisions, never silent removal.",
        "",
        "Owner is **preferred routing**, not file ownership; COORD/DEVICE/GEMINI tasks "
        "are not automatically assigned to GPT A/B/C. Any change touching unowned "
        "files needs coordinator-approved allocation. ChatGPT sessions do not run "
        "without a user turn. Review [the three-chat protocol](THREE_GPT_APK_WORKFLOW.md).",
        "",
    ]
    for i in range(26):
        letter = chr(ord("A") + i)
        lines.extend([f"## Batch {letter}", "",
                      "| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |",
                      "|---|---|---|---|---|---|---|"])
        for t in data["tasks"][i * 5:(i + 1) * 5]:
            title = (t["title"] + " - " + t["acceptance"]).replace("|", "/")
            lines.append(f"| {t['order']} | {t['id']} | {t['priority']} | {t['owner']} "
                         f"| {t['source_state']} | {title} | {', '.join(t['nt_ids']) or '-'} |")
        lines.append("")
    lines.extend([
        "## Acceptance rules",
        "",
        "- Never equate a SOURCE, LOCAL, PARTIAL or PUBLISHED tag with an accepted task.",
        "- Every task needs scope/caller review, source checks, a local commit or "
        "evidence-only report, coordinator approval and applicable exact-SHA CI.",
        "- Hardware-dependent acceptance requires actual device logs and screenshots; "
        "an unavailable device is a blocker to claiming public-release acceptance.",
        "- Independent tasks in later batches may start while another batch is in CI. "
        "Only publication, integration and release are serialized.",
        "- Network/CI failures are a separate exclusive incident claim; any slot "
        "may investigate and fix within its owned file paths, otherwise request transfer.",
        "- The SQLite ledger controls live claims, reviewer decisions, replacements "
        "and incident ownership. Do not edit this Markdown to impersonate progress.",
        "",
    ])
    return "\n".join(lines)


def verify(data: dict) -> None:
    tasks = data["tasks"]
    assert len(tasks) == 130, "expected 130 tasks"
    assert len({t["id"] for t in tasks}) == 130
    assert {t["id"] for t in tasks if t["kind"] == "feature"} == {f"NP-{i:03d}" for i in range(1,111)}
    assert {t["id"] for t in tasks if t["kind"] == "release"} == {f"REL-{i:02d}" for i in range(1,21)}
    assert {v for t in tasks for v in t["nt_ids"]} == {f"NT-{i:03d}" for i in range(1,80)}
    assert Counter(v for t in tasks for v in t["nt_ids"]).most_common(1)[0][1] == 1
    assert {t["batch"] for t in tasks} == set("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    assert all(len([t for t in tasks if t["batch"] == c]) == 5 for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    assert all(t["owner"] in ("A", "B", "C", "COORD", "DEVICE", "GEMINI") for t in tasks)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--write", action="store_true")
    group.add_argument("--check", action="store_true")
    args = parser.parse_args()
    path = OUT / "APK_BATCH_TASKS.json"
    md = OUT / "APK_BATCH_A_TO_Z.md"
    if args.write:
        data = generate()
        verify(data)
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        md.write_text(markdown(data), encoding="utf-8")
        print(f"Wrote {len(data['tasks'])} tasks across 26 five-task batches, with 79 NT tests.")
    else:
        data = json.loads(path.read_text(encoding="utf-8"))
        verify(data)
        print("PASS: all 110 NP, 79 NT, 20 release tasks; 26 five-task batches.")
        # Do not regenerate: source evidence changes as reviewers update FEATURE_MATRIX.


if __name__ == "__main__":
    main()
