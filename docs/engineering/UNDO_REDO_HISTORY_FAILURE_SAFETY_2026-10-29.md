# Undo/redo history failure safety — 2026-10-29

**Branch:** `fix/undo-redo-history-failure-safety`
**Baseline:** `main` at `ad7d90a`; pending import/PDF branches are independent.

## Problem and scope

`core/history/History.kt` previously called `removeLastOrNull()` before executing a command's `undo()` or `redo()`. If an in-memory edit failed with an exception, the command disappeared from the stack and could not be retried. On a note editor, this is a data-correctness risk: the user might lose access to the action that was meant to restore ink, text, or a page.

## Implemented

Execute the command while it remains at the top of its existing stack. Move it to the other stack **only if the command returns normally**. Empty-stack operations continue to be no-ops, existing history limits and redo-branch behavior remain unchanged, and no new dependency or state machine was added.

## Regression tests

Added focused tests to `HistoryTest.kt` for:
- Failed undo retaining the top undo command and an intact retry path.
- Failed redo retaining the top redo command and an intact retry path.
- A failed undo preserving other undo history and a pre-existing redo branch.

## Remaining limitations and verification

This change protects *history stack bookkeeping*. It cannot roll back a command that throws **after partially mutating the document**. Composite edits and other commands may require transactional behavior or deliberate compensation in a later phase. On Android, a thrown exception must still be surfaced safely in the UI. Follow-up device tests should include undo and redo during drawing, text editing and page manipulation.

**Gate:** full GitHub Android CI (Kotlin unit tests, lint, debug APK) must pass before this branch is merged into `main`.
