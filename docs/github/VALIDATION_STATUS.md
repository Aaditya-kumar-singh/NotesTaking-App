# GitHub workflow preparation — validation status

Date: 2026-10-28

## Local checks performed

- GitHub Actions workflow YAML parsed: 4 / 4
- Dependabot and issue-template YAML parsed: 3 / 3
- Standalone source check: PASS (360 Kotlin source files, 197 test files, 19 XML files, 39 PNGs)
- Release tag/version check: PASS for `v0.1.0`
- Python helper scripts: syntax compilation passed
- Gradle Wrapper JAR: present
- Original XNotes Git history: absent, as requested
- Current GitHub remote: none (no local `.git` repository)
- Original MIT and third-party license notices: preserved

## What has not been tested

- GitHub-hosted action execution: not run, because the app has not been uploaded to a repository
- Android Gradle build, JVM tests or lint: not run in this environment because Java/JDK is unavailable
- Signed release build: requires an actual GitHub repository, tag, release environment, and signing secrets
- CodeQL scanning: requires GitHub code-scanning support and a successful Android build
- Physical Android device, stylus, PDF and save/recovery workflows: not run

If first CI fails, do not mark the build successful or bypass mandatory checks. Inspect its logs, fix the actual issue, and rerun.

## Source documents

- `.github/workflows/android-ci.yml`
- `.github/workflows/android-release.yml`
- `.github/workflows/dependency-graph.yml`
- `.github/workflows/codeql.yml`
- `docs/github/WORKFLOWS.md`

The release intentionally remains manual/draft until the Android app passes device testing and its outstanding historical audit findings are addressed.
