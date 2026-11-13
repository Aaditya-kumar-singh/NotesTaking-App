#!/usr/bin/env python3
"""Refuse commits that cross ownership boundaries between two LocalMCP GPT worktrees.

This is a local source-level guard. It does not replace code review or Android CI.
Run from the intended worktree: python tools/agents/check_lane.py
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

COORDINATOR_REF = "refs/heads/chore/localmcp-two-gpt-lanes-20261113"
LANES = {
    "chat-a": {
        "branch": "agent/chat-a-b39-save-recovery",
        "files": (
            "app/src/main/java/com/notestaking/app/ui/Editor.kt",
            "app/src/main/java/com/notestaking/app/ui/SidePanel.kt",
        ),
        "prefixes": (
            "app/src/main/java/com/notestaking/app/format/",
            "app/src/main/java/com/notestaking/app/platform/",
            "app/src/test/java/com/notestaking/app/format/",
            "app/src/test/java/com/notestaking/app/platform/",
            "docs/agent-work/chat-a/",
        ),
    },
    "chat-b": {
        "branch": "agent/chat-b-g08-toolbar-accessibility",
        "files": (
            "app/src/main/java/com/notestaking/app/ui/Toolbar.kt",
            "app/src/main/java/com/notestaking/app/ui/ToolbarFrame.kt",
            "app/src/main/java/com/notestaking/app/ui/ToolbarCustomizer.kt",
            "app/src/main/java/com/notestaking/app/ui/FloatingToolbar.kt",
            "app/src/main/java/com/notestaking/app/ui/InfiniteToolbar.kt",
            "app/src/main/java/com/notestaking/app/ui/CanvasQuickAccess.kt",
            "app/src/main/java/com/notestaking/app/ui/Tooltip.kt",
        ),
        "prefixes": (
            "app/src/test/java/com/notestaking/app/ui/",
            "docs/agent-work/chat-b/",
        ),
    },
}


def permitted(lane: str, name: str) -> bool:
    path = name.replace("\\", "/")
    rules = LANES[lane]
    return path in rules["files"] or any(path.startswith(p) for p in rules["prefixes"])


def git(root: Path, *args: str) -> bytes:
    proc = subprocess.run(
        ["git", "-C", str(root), *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if proc.returncode:
        message = proc.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"git {' '.join(args)}: {message or proc.returncode}")
    return proc.stdout


def changed_paths(root: Path) -> set[str]:
    commands = (
        ("diff", "--name-only", "--no-renames", "-z", f"{COORDINATOR_REF}..HEAD"),
        ("diff", "--name-only", "--no-renames", "--cached", "-z"),
        ("diff", "--name-only", "--no-renames", "-z"),
        ("ls-files", "--others", "--exclude-standard", "-z"),
    )
    names: set[str] = set()
    for cmd in commands:
        for name in git(root, *cmd).decode("utf-8", errors="replace").split("\0"):
            if name:
                names.add(name)
    return names


def self_test() -> None:
    assert permitted("chat-a", "app/src/main/java/com/notestaking/app/ui/Editor.kt")
    assert permitted("chat-a", "app/src/test/java/com/notestaking/app/format/DocumentCodecTest.kt")
    assert not permitted("chat-a", "app/src/main/java/com/notestaking/app/ui/Toolbar.kt")
    assert permitted("chat-b", "app/src/main/java/com/notestaking/app/ui/Toolbar.kt")
    assert permitted("chat-b", "app/src/test/java/com/notestaking/app/ui/ToolbarMetricsTest.kt")
    assert not permitted("chat-b", "app/src/main/java/com/notestaking/app/ui/Editor.kt")
    for lane in LANES:
        assert not permitted(lane, "app/src/main/java/com/notestaking/app/ui/Backstage.kt")
        assert not permitted(lane, "app/src/main/res/values/strings.xml")
        assert not permitted(lane, "docs/notein-parity/PROGRESS.md")
        assert not permitted(lane, "tools/agents/check_lane.py")
    print("PASS: dual-chat file ownership self-test")


def main() -> int:
    if sys.argv[1:] == ["--self-test"]:
        self_test()
        return 0
    if len(sys.argv) != 1:
        print("Usage: python tools/agents/check_lane.py [--self-test]", file=sys.stderr)
        return 2

    root = Path(__file__).resolve().parents[2]
    lane = root.name
    if root.parent.name != ".agent-worktrees" or lane not in LANES:
        print("BLOCKED: run from .agent-worktrees/chat-a or .agent-worktrees/chat-b", file=sys.stderr)
        return 2
    try:
        true_root = Path(git(root, "rev-parse", "--show-toplevel").decode().strip()).resolve()
        if true_root != root:
            raise RuntimeError("worktree root mismatch")
        actual_branch = git(root, "branch", "--show-current").decode().strip()
        if actual_branch != LANES[lane]["branch"]:
            raise RuntimeError(f"branch {actual_branch!r} is not {LANES[lane]['branch']!r}")
        git(root, "merge-base", "--is-ancestor", COORDINATOR_REF, "HEAD")
        changes = changed_paths(root)
        blocked = sorted(path for path in changes if not permitted(lane, path))
        if blocked:
            print(f"BLOCKED: {lane} changed {len(blocked)} unowned file(s):", file=sys.stderr)
            for path in blocked:
                print(f"  {path}", file=sys.stderr)
            print("Stop and ask Chat A/integrator to reassign ownership; do not bypass the hook.", file=sys.stderr)
            return 2
        print(f"PASS: {lane} branch and file ownership ({len(changes)} changed path(s))")
        return 0
    except (RuntimeError, OSError) as exc:
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
