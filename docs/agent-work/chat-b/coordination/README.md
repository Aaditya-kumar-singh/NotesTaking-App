# NotesTaking Multi-AI Coordinator — local pilot

**Status:** Source-tested Chat B pilot; NOT yet deployed as a repo-wide policy. It is
stored inside Chat B's permitted docs/agent-work/chat-b directory, leaving all
protected project files, existing agent worktrees and remote branches unchanged.

Inspired by AgentLane (https://github.com/thetrueshags/AgentLane) and Agent
Coordinator (https://github.com/mkalkere/agent-coordinator). Independently written
using Python's standard library. Neither third-party application was installed.
AgentLane ordinarily expects separate clones; Agent Coordinator needs a live
service. Both would complicate the existing single-writer Git integration policy.

## What it provides

- **Shared state:** SQLite at xnotes-android/.git/agent-coordination/claims.sqlite3.
  All worktrees sharing that Git common directory see the same ownership state.
- **Atomic task claims:** BEGIN IMMEDIATE and conservative path-overlap checks
  prevent two cooperating agents from claiming the same work.
- **Capability routing:** score priority, declared skill ratings, task difficulty,
  risk, dependency completion and per-agent capacity. A subscription is not proof
  of model performance or tool access.
- **Existing lanes:** initial reservations for Chat A, Chat B and Gemini; no claim
  that unfinished work is complete.
- **Review gate:** worktree handoffs require clean Git and an exact HEAD SHA;
  the task stays claimed until the coordinator explicitly records review evidence.
- **No silent takeover:** heartbeats older than four hours produce a warning,
  not permission to steal another agent's files.
- **Read-only lanes:** extra Plus and Go chats can accept research/checklists
  without code-edit authority. New coding chats require their own approved
  worktrees and ownership rules.
- **No background activity:** the program cannot start ChatGPT/Gemini sessions,
  push commits, merge, build APKs, or run Gradle.

This is a cooperative protocol, not an access-control sandbox. AIs must obey it
and the existing tools/agents/check_lane.py. The SQLite board is local to this
one Windows Git installation, NOT synchronized across computers. Cross-machine
claims need a remote atomic service/Git protocol before enabling remote writers.

## Identities and initial reservations

| Claim | Agent | Existing permission |
| --- | --- | --- |
| LANE-A | chat-a | Storage/recovery, assigned Chat A worktree |
| LANE-B | chat-b | Toolbar and G08, assigned Chat B worktree |
| NP-004 | gemini-pro | Quick Scratch's three assigned UI files |
| R-CI-TRACE | plus/go research | P0 test coverage study only |
| R-PDF-ANNOT | plus research | Editable PDF annotation export research |
| R-PERF | plus/go research | 200-page PDF performance plan |
| R-TAGS | plus/go research | Tags/favorites design brief |

**Ownership conflict requiring coordinator action:** Chat B's legacy allowlist
owns app/src/test/java/com/notestaking/app/ui/**, which could overlap Gemini's
creation-flow tests. Gemini is reserved only the three main UI files for now.
Do not create/modify creation-flow tests until specific test filenames are
transferred and the global ownership guard is updated.

## Run from the existing Chat B worktree

    cd C:\Users\WHITE-SNAKE\Desktop\xnotes-android\.agent-worktrees\chat-b
    python docs\agent-work\chat-b\coordination\coord.py seed
    python docs\agent-work\chat-b\coordination\coord.py status
    python docs\agent-work\chat-b\coordination\coord.py doctor
    python docs\agent-work\chat-b\coordination\coord.py recommend --agent chat-b

Seed is idempotent. It only creates local SQLite reservations in .git.
This does not re-create worktrees or change branches.

To resume Chat B's reserved G08 lane:

    python docs\agent-work\chat-b\coordination\coord.py claim LANE-B --agent chat-b
    python docs\agent-work\chat-b\coordination\coord.py heartbeat LANE-B --agent chat-b

Extra chats with read-only access can choose independent work:

    python docs\agent-work\chat-b\coordination\coord.py recommend --agent gpt-plus-research
    python docs\agent-work\chat-b\coordination\coord.py claim-next --agent gpt-plus-research
    python docs\agent-work\chat-b\coordination\coord.py claim-next --agent gpt-go-research
    python docs\agent-work\chat-b\coordination\coord.py handoff R-PERF --agent gpt-go-research --summary "Reviewed a bounded PDF performance checklist; no code was touched"

A writable agent must run with its exact assigned worktree and branch. From
Chat A or Gemini, call the Python script by its absolute Chat B path and pass
--repo pointing to THAT agent's checkout:

    python <absolute-path-to-coord.py> --repo "C:\Users\WHITE-SNAKE\Desktop\xnotes-android\.agent-worktrees\chat-a" claim LANE-A --agent chat-a
    python <absolute-path-to-coord.py> --repo "C:\Users\WHITE-SNAKE\Desktop\NotesTaking-QuickScratch" claim NP-004 --agent gemini-pro

Do not copy the script to unrelated worktrees while they have unreviewed code.

Coding handoff example after making local commits and running allowed checks:

    python docs\agent-work\chat-b\coordination\coord.py handoff LANE-B --agent chat-b --commit <full-40-character-HEAD-SHA> --summary "Local fixes source-checked, ownership checked, ready for coordinator review"

Only after the coordinator reviewed the exact integration SHA and applicable
GitHub CI should they record closure from the administrative checkout:

    python <absolute-path-to-coord.py> --repo "C:\Users\WHITE-SNAKE\Desktop\xnotes-android" close LANE-B --evidence "developer exact SHA and successful CI URL reviewed by coordinator"

This records a human's evidence statement; the broker does not independently
verify CI. No task should be reported CI PASSED merely because it was closed.

## Rules for every new AI chat

1. Inspect AGENTS.md, the assigned lane policy, branch/worktree, this board,
   and its pending claims. Use a distinct agent ID for each independent chat.
2. Claim the next eligible task BEFORE editing. Never edit outside claimed
   paths or the narrower legacy allowlist. If the task needs an owned/shared
   file, stop and request an allocation from the coordinator.
3. On each user turn, refresh the task heartbeat; do not presume the other
   ChatGPT/Gemini chats are running in the background.
4. Run permitted source/privacy checks, Git whitespace checks and unit
   source tests; commit locally, then hand off the exact SHA.
5. Only the coordinator integrates serially, checks dependency/overlap,
   and publishes to developer once the prior remote CI gate is resolved.
   Device tests happen only after agreed coding and pre-APK checks finish.

The plan labels Plus, Go and Gemini Pro as *examples* of available sessions.
A subscription alone does not provide an API key, a shared LocalMCP connection
or a writable worktree. For multiple chats of the same plan, the coordinator
must give every chat a new unique ID in board.json. Additional coding chats
also need unique branches/worktrees and a reviewed update to
docs/github/DUAL_GPT_LOCALMCP.md and tools/agents/check_lane.py. Do not
enable extra code writers by editing profiles alone.

## Validation and rollout

    python -m unittest discover -s docs\agent-work\chat-b\coordination -p test_coord.py -v
    python tools\agents\check_lane.py
    python tools\ci\check_project.py
    python tools\ci\check_backup_privacy.py
    git diff --check

The Python tests create disposable temporary Git repositories and SQLite
files; no NotesTaking data, Kotlin compilation, Android lint, APK build or
device install is involved.

This pilot touches only docs/agent-work/chat-b/coordination/**. It must be
reviewed by Chat A, integrated in order, and adopted in the global agent
instructions/ownership guard before it becomes mandatory for all coding AIs.
The original two-GPT lanes, Gemini, untracked .kotlin/ and user files remain
untouched. For a device-test APK, first run Kotlin tests/lint/build/validation
against one final integrated developer commit in GitHub Actions.
