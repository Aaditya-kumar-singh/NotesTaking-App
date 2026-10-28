#!/usr/bin/env python3
"""Require an existing release tag to match the Gradle app versionName."""
import re
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("Usage: check_release_tag.py v<version>")
tag = sys.argv[1]
if not re.fullmatch(r"v[0-9]+(?:\.[0-9]+){2}(?:[-._][A-Za-z0-9.-]+)?", tag):
    raise SystemExit("Release tag must look like v0.1.0")
gradle = (Path(__file__).resolve().parents[2] / "app/build.gradle.kts").read_text("utf-8")
match = re.search(r"\bversionName\s*=\s*\"([^\"]+)\"", gradle)
if not match:
    raise SystemExit("Cannot locate versionName in app/build.gradle.kts")
expected = "v" + match.group(1)
if tag != expected:
    raise SystemExit(f"Release tag mismatch: requested {tag}, app expects {expected}")
print(f"Release tag {tag} matches Gradle versionName {match.group(1)}.")
