# Universal Android APK — phone/tablet acceptance plan

**Prepared:** 2026-11-16 by integration coordinator. This is a **test assignment and evidence template**, not a device test result or release approval.

## Build first, test the same exact APK afterward

| Checkpoint | Commit / Actions | State at preparation |
|---|---|---|
| B97 all-ABI debug reference | `69e2eb39a67c334b6f522c531db5f90beaf5efbf`; [run #43853208944](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43853208944) | **PASSED** with `NotesTakingApp-debug-all-abis-198` artifact; lacks latest A/B/C UI fixes |
| B98 combined UI candidate | `b10a6930b7740955cc36b8bf33efc1b00dd9ad1e`; [run #43856500526](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43856500526) | **FAILED** Kotlin compilation (missing Compose `mutableStateOf` import); do not test as installed APK |
| **B99 universal APK target** | `b299cbb8972267eebe103493fa093f7e5cfa6f70`; [run #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156) | **PASSED** source, JUnit, lint, all three ABIs, APK-integrity checks; download `NotesTakingApp-debug-all-abis-200` from Actions Artifacts for manual installation |

The `qa/**` CI workflow sets `CI_ARM64_ONLY=false`, assembles **one** `app-debug.apk` containing `arm64-v8a`, `armeabi-v7a`, and `x86_64`, checks the debug package `com.notestaking.app.debug`, and uploads `NotesTakingApp-debug-all-abis-<run_number>` as a zipped artifact. This is a **debug/test APK**, **not** a production-signed APK.

Do not install a failing or still-running checkpoint. Once the B99 run is green, open its **Artifacts** section, obtain `NotesTakingApp-debug-all-abis-...`, extract `app-debug.apk`, record the SHA-256 of that exact APK, and manually install on a designated test phone. Use a test phone or disposable notes; never overwrite or delete valuable private notes. One APK can support different CPU ABIs, but this alone does not prove behavioral compatibility with every Android version.

**Local Windows safety rule:** ChatGPT/LocalMCP source agents may inspect `adb devices -l` and read test evidence, but do **not** run Gradle, Kotlin tests, lint, native builds, or device installs on the Windows host. GitHub Actions builds the APK; the phone owner performs installation and authorizes interactive testing. Updated coordinator ADB check on 2026-11-16: an authorized **Samsung SM-M135F, Android 14, API 34, 32-bit ARM (`armeabi-v7a`)** is connected. No app was installed or tested; no real-phone result is claimed.

## Assign all 79 existing NT acceptance cases without overlap

Official case details and expected results are in [TEST_PLAN.md](../notein-parity/TEST_PLAN.md). The ranges below partition every NT-001 through NT-079 exactly once. A case still runs in its specified environment—CI, real Android phone, or an actual compatible S Pen tablet; a phone without a pressure stylus cannot pass tablet-only cases.

| Lane | NT cases owned for evidence review | Count | Additional required card |
|---|---|---:|---|
| **Chat A** | NT-001–NT-018, NT-054–NT-066, NT-077, NT-079 | 33 | NP-004 Quick Scratch implementation with coordinator UI support; REL-13 offline startup, storage/SAF refusal and localization |
| **Chat B** | NT-029, NT-042, NT-047, NT-050, NT-051, NT-067–NT-076 | 15 | REL-12 TalkBack, keyboard, split panes, tooltip boundaries, 48dp targets and large fonts |
| **Chat C** | NT-019–NT-028, NT-030–NT-041, NT-043–NT-046, NT-048–NT-049, NT-052–NT-053, NT-078 | 31 | NP-102 handwriting recognition; REL-08 200-page PDF Perfetto; REL-09 pen/palm/pressure; REL-10 eraser/undo; REL-11 crop/rotation/split anchoring |
| **Coordinator** | Review every case and failure; consolidate fixes and repeat impacted cases on a new exact APK SHA | — | REL-14 full-ABI exact-candidate CI; REL-16 physical QA evidence/waivers; REL-19 production signature; REL-20 final release signoff |

The test assignment is **work division**, not an assertion that A/B/C chats can physically operate an unconnected phone or that test cases have passed.

### Phone testing — initial sequence

1. Confirm **successful B99** GitHub jobs and actual `NotesTakingApp-debug-all-abis-...` upload; record run URL, exact Git SHA, artifact name and SHA-256 of extracted APK.
2. Manually install `app-debug.apk` on an authorized Android test phone, separate from production. Record phone model, Android build, screen size, language, date, storage permissions and the installed package ID. If installation fails, capture the error and stop.
3. Use new disposable `.xnote`, `.xcanvas`, images and local 200-page scanned/vector PDF fixtures with known hashes. For personal data, use synthetic replacements, not uploads of private real notes.
4. **A:** onboarding/guest Home, create/open/search, native save/reopen, Quick Scratch (only after implemented on a later APK), backup, cancellation, SAF denied/revoked/slow provider, airplane mode, import/export/share and preservation of unrelated notes.
5. **B:** top/side/floating toolbar, split 180/240/320dp, toolbar keyboard/tooltip focus, English/Hindi, 1.0/1.3/2.0× text, TalkBack spoken names and actions, zoom lock, page/canvas selection, hover where supported, no false keyboard page turns.
6. **C:** finger/stylus draw, gesture ownership, eraser fragments and undo/redo, overlapping split gestures, native ink save/reopen, PDF zoom 25/100/200/400%, page rotation 0/90/180/270 and annotation anchoring, 200-page stress/performance. Physical S Pen pressure, hover, palm rejection and tilt must be repeated on an **S Pen-capable tablet**, not assumed from ordinary finger tests.
7. After every defect: record the smallest reproduction, assign the owning code lane, fix and review locally, publish **a new QA commit**, require its exact-SHA full-ABI CI, install the new APK manually and rerun affected phone/tablet cases. Never mix evidence from different APK hashes.
8. Coordinator may consider release only after all **required** P0/P1/device tests have grounded evidence and the approved signed production APK and rollback procedure have separately passed. Optional NP-102 may be deferred only by an explicit recorded scope decision, not silently treated as implemented.

### Evidence record — required for each NT / REL case

```text
NT/REL ID:
Owner lane: A / B / C
Result: NOT RUN / BLOCKED / PASS / FAIL
UTC/local date and time:
APK Git SHA:
GitHub Actions run + artifact name:
Extracted APK SHA-256:
Package / version:
Phone or tablet model + Android version:
Test fixture name + SHA-256:
Exact user actions:
Expected result:
Actual observed result:
Screenshots / logcat / Perfetto trace / video path:
Bug issue URL (if any):
Reviewer, waiver reason and approved date (if needed):
```

The tester must explicitly mark a case **BLOCKED** when a hardware feature is unavailable, and **NOT RUN** when no test was performed. A source-level test or a CI unit test is not a substitute for phone behavior; conversely CI-only NT cases must be reviewed against the GitHub logs, not marked as phone tests.

## Exclusive implementation ownership

- **A / NP-004:** On 2026-11-16 the user explicitly requested transfer of preferred task routing from the existing historical Gemini Quick Scratch worktree. The Gemini branch and files must remain untouched, and Gemini must not be a parallel writer. Chat A must `triad.py claim NP-004` in its own session after the coordinator allocates it. A may edit only A-owned `Editor.kt`, `platform/**`, matching tests/STATUS; the coordinator handles required shared `Backstage.kt`, `NewNoteScreen.kt`, `MainActivity.kt`, strings and the actual Quick Scratch UI. This user-authorized transfer does not expand A's file permissions.
- **B / issue #26:** Own the accessibility/device testing checklist and B-only regression test contracts, not shared Android code or A/C source.
- **C / NP-102, issue #27:** Already reserved in the coordinator; claim before writing C-owned core/canvas stroke planning. Do not add ML model dependencies/UI or claim recognition accuracy without actual integration and live tests.
- **Coordinator:** Sole QA publisher and release approver. Never push the temporary debug-APK branch to `developer` just to make an artifact downloadable.

## Current blockers

- One authorized Samsung SM-M135F phone is now connected through `adb devices -l`, but no approved APK is installed and no app UI/Perfetto/screenshots/TalkBack tests have been run. No compatible S Pen tablet has been verified.
- B99 has passed exact-SHA GitHub JUnit/lint/APK-integrity checks; the artifact must be downloaded, its extracted APK SHA-256 recorded, and **manually installed on the authorized phone** before actual tests can start.
- NP-004 Quick Scratch and NP-102 on-device recognition are not in B99 and require later implementation, another universal build, and acceptance.
- No production signing-certificate trust chain or signed release approval. Keep REL-08–REL-13, REL-14, REL-16, REL-19 and REL-20 open until verified.

## 2026-11-16 follow-up: B99 real-device QA and B100 requested redesign

- **B99 was installed and exercised on the authorized Galaxy M13**, contrary to the older preflight-only blocker above. Evidence: `docs/notein-parity/device-evidence/B99-GalaxyM13-smoke-2026-11-16.md` and `B99-GalaxyM13-followup-2026-11-16.md`. QA note/canvas, search, PDF export/import, annotations and save/reopen passed bounded smoke cases; this does not pass all 79 NT tests or REL-16.
- The user supplied two new screenshots and requested moving the visible **New Note** control away from the library header, placing the **document filename at the editor top**, and adding an **arrow-expandable tool palette**. This is a separately tracked **new UI requirement**, not a defect already fixed by B99.
- [GitHub issue #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28) and `docs/engineering/B100_LIBRARY_EDITOR_CHROME_REDESIGN_2026-11-16.md` specify layout, tool lists, ownership, 14 acceptance scenarios, data safety, and after-build phone QA. Existing A/B/C task claims and the fixed 130-card A-Z catalog remain unchanged. B99 should remain the comparison reference until B100 is source-reviewed, combined and passes fresh universal-APK CI.
- **Scheduled for the next APK update:** `docs/github/NEXT_UNIVERSAL_APK_UPDATE_PLAN_2026-11-17.md` explicitly includes the New Note/FAB correction, top filename and split action, arrow-expandable drawing palette, and pending Quick Scratch integration before a newly tested universal APK; no implementation or release approval is implied.
