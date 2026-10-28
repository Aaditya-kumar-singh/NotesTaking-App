#!/usr/bin/env python3
"""Validate APK package identity and expected bundled native libraries.

Designed to run after an Android build on GitHub Actions with Android build-tools.
"""
import argparse
import re
import subprocess
import sys
import zipfile
from pathlib import Path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--aapt", type=Path, required=True)
    args = parser.parse_args()

    if not args.apk.is_file():
        parser.error(f"APK not found: {args.apk}")
    if not args.aapt.is_file():
        parser.error(f"Android aapt not found: {args.aapt}")

    with zipfile.ZipFile(args.apk) as z:
        files = set(z.namelist())
        if z.testzip():
            raise SystemExit("APK ZIP CRC verification failed")
        expected_abis = ("arm64-v8a", "armeabi-v7a", "x86_64")
        native = ("libnotestakingts.so", "libnotestakingpdf.so")
        missing = [
            f"lib/{abi}/{library}"
            for abi in expected_abis for library in native
            if f"lib/{abi}/{library}" not in files
        ]
        if missing:
            raise SystemExit("Missing APK native libraries: " + ", ".join(missing))
        if "classes.dex" not in files:
            raise SystemExit("APK is missing classes.dex")

    result = subprocess.run(
        [str(args.aapt), "dump", "badging", str(args.apk)],
        check=True, text=True, capture_output=True
    )
    match = re.search(r"^package:\s+name='([^']+)'", result.stdout, re.MULTILINE)
    if not match or match.group(1) != args.package:
        raise SystemExit(
            f"APK package mismatch: expected {args.package}, "
            f"found {match.group(1) if match else 'unknown'}"
        )
    print(
        f"Verified APK: {args.apk.name} "
        f"({args.apk.stat().st_size:,} bytes), package {args.package}, "
        f"{len(expected_abis) * len(native)} JNI libraries, ZIP CRC OK."
    )

if __name__ == "__main__":
    try:
        main()
    except (OSError, zipfile.BadZipFile, subprocess.CalledProcessError) as exc:
        print("APK verification failed:", exc, file=sys.stderr)
        sys.exit(1)
