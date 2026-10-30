#!/usr/bin/env python3
"""Version and verify GitHub-built universal Android beta APKs."""
import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

TAG_RE = re.compile(r"v0\.1\.0-beta\.([1-9][0-9]*)\Z")
SHA_RE = re.compile(r"[0-9a-f]{40}\Z")
ABIS = {"arm64-v8a", "armeabi-v7a", "x86_64"}
LIBRARIES = {"libnotestakingts.so", "libnotestakingpdf.so"}


def beta_number(tag):
    match = TAG_RE.fullmatch(tag)
    return int(match.group(1)) if match else None


def next_tag(existing):
    numbers = [n for tag in existing if (n := beta_number(tag.strip())) is not None]
    return f"v0.1.0-beta.{max([3, *numbers]) + 1}"


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def metadata(tag, source_sha, apk):
    number = beta_number(tag)
    if number is None or number < 4:
        raise ValueError("Invalid or unsupported beta tag")
    if not SHA_RE.fullmatch(source_sha):
        raise ValueError("Invalid commit SHA")
    if not apk.is_file() or apk.stat().st_size == 0:
        raise ValueError("APK is missing or empty")
    return {
        "tag": tag,
        "version_name": tag[1:] + "-debug",
        "version_code": number,
        "source_sha": source_sha,
        "apk_sha256": sha256(apk),
    }


def verify(apk, data, source_sha):
    if not isinstance(data, dict):
        raise ValueError("Invalid metadata")
    tag = data.get("tag")
    expected = metadata(tag, source_sha, apk)
    if data != expected:
        raise ValueError("Release metadata, commit or APK digest mismatch")
    with zipfile.ZipFile(apk) as archive:
        if archive.testzip() is not None:
            raise ValueError("Corrupt APK ZIP")
        names = set(archive.namelist())
        if not {"AndroidManifest.xml", "classes.dex"} <= names:
            raise ValueError("APK manifest or classes.dex missing")
        actual_abis = {name.split("/")[1] for name in names
                       if name.startswith("lib/") and name.endswith(".so")}
        if actual_abis != ABIS:
            raise ValueError(f"Incorrect native architectures: {actual_abis}")
        for abi in ABIS:
            for lib in LIBRARIES:
                if f"lib/{abi}/{lib}" not in names:
                    raise ValueError(f"Missing {lib} for {abi}")
    return expected


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("next-tag")
    write = sub.add_parser("write")
    write.add_argument("--tag", required=True)
    write.add_argument("--source-sha", required=True)
    write.add_argument("--apk", type=Path, required=True)
    write.add_argument("--output", type=Path, required=True)
    check = sub.add_parser("verify")
    check.add_argument("--source-sha", required=True)
    check.add_argument("--apk", type=Path, required=True)
    check.add_argument("--metadata", type=Path, required=True)
    check.add_argument("--github-output", type=Path)
    args = parser.parse_args()
    if args.command == "next-tag":
        print(next_tag(sys.stdin))
    elif args.command == "write":
        data = metadata(args.tag, args.source_sha, args.apk)
        args.output.write_text(json.dumps(data, sort_keys=True) + "\n", encoding="utf-8")
        print(f"Wrote {data['tag']} metadata for {data['source_sha']}")
    else:
        data = verify(args.apk, json.loads(args.metadata.read_text("utf-8")), args.source_sha)
        if args.github_output:
            with args.github_output.open("a", encoding="utf-8") as out:
                for key in ("tag", "version_name", "version_code", "apk_sha256"):
                    out.write(f"{key}={data[key]}\n")
        print(f"Verified {data['tag']} for {data['source_sha']}")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile, json.JSONDecodeError) as error:
        print(f"Release validation failed: {error}", file=sys.stderr)
        sys.exit(1)
