# Modern Paper & Ink — foundation implementation checkpoint (2026-10-29)

**Branch:** `feature/modern-paper-ink-foundation`. Original branding baseline was `5a93bd7`; the batched Paper & Ink integration was replayed onto `main` commit `1ef9e1e` after CI policy PR #7 merged.
**Scope:** original brand mark, launcher assets, initial Material color alignment, system splash/loading, shared popup styling. No changes to saved notes, PDF rendering, `.xnote`, `.xcanvas`, or `.xtemplate` formats.

## Implemented

- Added the editable SVG brand master at `design/brand/notestaking-symbol.svg`: folded page, visible fold, restrained ruled lines, and blue flowing ink trail. The generated Android vector is `res/drawable/ic_brand_mark.xml`. Monochrome themed-icon vector is `ic_launcher_monochrome.xml`.
- Replaced legacy launcher PNGs at mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi, including round and foreground resources. Both adaptive launcher XML variants now use the vector foreground.
- Regenerated the 24 existing loading animation frames with progressive ink drawing. Preserved the old `AnimationDrawable` 24-frame contract and 67 ms/frame timing; loader background now comes from the same color token as the system splash.
- Android 12+ system splash uses the same mark instead of a transparent placeholder.
- The About screen and navigation headers display the same `ic_brand_mark` Android vector with a consistent background; this removes the About screen's runtime launcher bitmap conversion, and retains its existing version/credits content.
- Aligned the Android resource accent, launch/window surface and the **new default Material dual-color preset** with ink blue `#526DDF`, paper-toned neutral `#E5DED0` and navy `#17243B`.
- Shared `DropdownMenu` and `AlertDialog` now use the lifted palette panel surface and the existing rounded Material shape roles. Existing callbacks and all destructive-action semantics are unchanged.
- Preserved the original Morning preset (brown/blue) and added a separate selectable Paper & Ink preset. Added a settings regression case that loads historic brown/blue theme seed values and verifies serialization round-tripping.

## Reproduction

Run `python tools/branding/generate_assets.py` with Pillow available to regenerate all SVG/VectorDrawable/PNG assets from the same geometry. This tooling is **not required** to build or run the Android application; the output assets are committed.

## Verification gates

1. `python tools/ci/check_project.py`, `python tools/ci/check_backup_privacy.py`, resource XML parse, PNG dimensions/alpha and `git diff --check`.
2. **GitHub Actions only**: feature-branch `Android CI` runs `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` with the pinned Android SDK 36, NDK 27.0.12077973 and CMake 3.22.1. Local Windows is restricted to source-only dry runs.
3. Install APK on Android tablet and phone; inspect masked adaptive icon, themed icon, first-frame splash, loader loops and screen transitions. Check dialogs/menus on phone/tablet in system/light/dark/OLED/custom appearance and TalkBack.
4. Push only to the isolated feature branch after source-only dry run. Do not merge into `main` on failed GitHub checks; do not treat the first brand pass as final without device screenshots and acceptance.

## Remaining design work

This is **not** the complete 33-surface BR-0..BR-5 redesign: launcher branding is a first pass. Home, editor/canvas chrome, sidebar, pen/eraser control sizing, per-dialog destructive styling, typography, settings previews, accessibility, custom iconography and animation polish remain. Existing user theme options and note/paper content must stay unchanged; the brand default is a fallback/preset, not a forced palette override.

## Audit workbook

The historical `docs/audit/xnotes-problems-master.xlsx` still lists 45 findings as Open. This design-only change does not close any XN audit item. The audit workbook is local/ignored and was not rewritten. Safety fixes developed on earlier branches still require individual evidence-based reconciliation and Android device tests.
