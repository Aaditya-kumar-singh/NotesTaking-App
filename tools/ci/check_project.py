#!/usr/bin/env python3
"""Lightweight, dependency-free source validation for GitHub Actions.

Does not replace Gradle compilation, native linking or device tests.
"""
import os
import re
import sys
import zlib
import struct
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "app"
SRC = APP / "src/main"
RES = SRC / "res"
KT = SRC / "java/com/notestaking/app"
TEST = APP / "src/test/java/com/notestaking/app"
problems = []


def require(ok, message):
    if not ok:
        problems.append(message)


def read(relative):
    return (ROOT / relative).read_text(encoding="utf-8")


def png_ok(path):
    raw = path.read_bytes()
    if not raw.startswith(b"\x89PNG\r\n\x1a\n"):
        return False
    offset = 8
    got_ihdr = False
    got_iend = False
    while offset + 12 <= len(raw):
        count = struct.unpack_from(">I", raw, offset)[0]
        if count > len(raw) - offset - 12:
            return False
        kind = raw[offset + 4:offset + 8]
        payload_end = offset + 8 + count
        crc = struct.unpack_from(">I", raw, payload_end)[0]
        if zlib.crc32(raw[offset + 4:payload_end]) != crc:
            return False
        if kind == b"IHDR":
            got_ihdr = count == 13
        if kind == b"IEND":
            got_iend = count == 0
            if payload_end + 4 != len(raw):
                return False
        offset = payload_end + 4
    return got_ihdr and got_iend


require((KT / "MainActivity.kt").is_file(), "Missing renamed Android MainActivity")
require(TEST.is_dir(), "Missing renamed test package")
require(not (SRC / "java/com/xnotes").exists(), "Obsolete source namespace still exists")
require((ROOT / "LICENSE").is_file(), "Original license must be preserved")
require((ROOT / "NOTICE.md").is_file(), "Derivative attribution is required")
require("Copyright (c) 2026 Shardul Vikram Singh" in read("LICENSE"), "Original MIT attribution was changed")
require("Aaditya Kumar" in read("README.md") and
        "https://github.com/Aaditya-kumar-singh" in read("README.md"),
        "Maintainer and official GitHub profile missing")
require("Aaditya Kumar" in read("NOTICE.md") and
        "https://github.com/Aaditya-kumar-singh/NotesTaking-App" in read("NOTICE.md"),
        "Current project credit missing from NOTICE")
require("R.string.project_maintainer" in read("app/src/main/java/com/notestaking/app/ui/AboutPane.kt") and
        "https://github.com/Aaditya-kumar-singh" in read("app/src/main/java/com/notestaking/app/ui/AboutPane.kt"),
        "About screen current maintainer link missing")
require((SRC / "cpp/pdfium/LICENSE").is_file(), "PDFium licensing must be preserved")
require((SRC / "cpp/tree-sitter/LICENSE").is_file(), "Tree-sitter licensing must be preserved")
require((SRC / "resources/com/notestaking/app/core/text/emoji.tsv").is_file(),
        "Renamed classpath emoji resource missing")
require("com.notestaking.app" in read("app/build.gradle.kts"),
        "Gradle package identity mismatch")
require('applicationId = "com.notestaking.app"' in read("app/build.gradle.kts"),
        "Android application ID mismatch")
require('rootProject.name = "NotesTakingApp"' in read("settings.gradle.kts"),
        "Gradle project name mismatch")
require('NOTE("xnote")' in read("app/src/main/java/com/notestaking/app/core/util/DocumentKind.kt"),
        "Legacy notebook format was modified")
require('CANVAS("ntcanvas", "xcanvas")' in
        read("app/src/main/java/com/notestaking/app/core/util/DocumentKind.kt"),
        "New Canvas suffix or legacy .xcanvas recognition is missing")
require('const val FORMAT = "xcanvas"' in
        read("app/src/main/java/com/notestaking/app/format/CanvasCodec.kt"),
        "Legacy Canvas ZIP manifest format marker was changed")
require('File(dir, "canvas.xcanvas")' in
        read("app/src/main/java/com/notestaking/app/platform/CanvasSessionStore.kt"),
        "Existing Canvas recovery slot was changed")
require('const val FORMAT = "xnotes-settings"' in
        read("app/src/main/java/com/notestaking/app/settings/SettingsBackup.kt"),
        "Legacy settings backup format was modified")
require("NOTESTAKING_LINK_OPTIONS" in read("app/src/main/cpp/CMakeLists.txt"),
        "Renamed CMake options missing")
for name in ("notestakingts", "notestakingpdf"):
    require("add_library(" + name + " SHARED" in read("app/src/main/cpp/CMakeLists.txt"),
            "Missing native library " + name)
for filename, symbol in (
    ("app/src/main/cpp/pdf_jni.cpp", "Java_com_notestaking_app_platform_PdfiumNative"),
    ("app/src/main/cpp/ts_jni.c", "Java_com_notestaking_app_platform_TreeSitterNative"),
):
    require(symbol in read(filename), "JNI binding mismatch: " + filename)

main_files = list(KT.rglob("*.kt")) if KT.exists() else []
test_files = list(TEST.rglob("*.kt")) if TEST.exists() else []
require(bool(main_files), "No Kotlin application sources")
require(bool(test_files), "No Kotlin unit tests")
for item in main_files + test_files:
    code = item.read_text(encoding="utf-8")
    require("package com.xnotes" not in code and "import com.xnotes" not in code,
            "Legacy Kotlin namespace found: " + str(item.relative_to(ROOT)))
    require("XnotesTheme" not in code and "XnotesIcons" not in code
            and "XnotesLoader" not in code,
            "Legacy UI symbol found: " + str(item.relative_to(ROOT)))

xmls = list(RES.rglob("*.xml")) + [SRC / "AndroidManifest.xml"]
for item in xmls:
    try:
        ET.parse(item)
    except ET.ParseError as exc:
        problems.append("Bad XML " + str(item.relative_to(ROOT)) + ": " + str(exc))

strings = ET.parse(RES / "values/strings.xml").getroot()
lookup = {node.get("name"): node.text for node in strings if node.tag == "string"}
require(lookup.get("app_name") == "NotesTaking App", "Incorrect launcher display name")

# English is the default; Hindi must cover every translatable key without changing
# Android format placeholders, plural forms or string-array lengths.
hi_files = sorted((RES / "values-hi").glob("*.xml"))
require(bool(hi_files), "Hindi UI locale resources are missing")
require(not list(RES.glob("values-zh*")) and not list(RES.glob("values-b+zh*")),
        "Chinese UI locale must not be packaged")
require("generateLocaleConfig = true" in read("app/build.gradle.kts"),
        "Android per-app language choices must be generated")
require('localeFilters += listOf("en", "hi")' in read("app/build.gradle.kts"),
        "Only English and Hindi should be packaged, including library locales")
require("unqualifiedResLocale=en-US" in read("app/src/main/res/resources.properties"),
        "Default English locale must remain explicit")

if hi_files:
    hi_resources = [item for file in hi_files for item in ET.parse(file).getroot()]
    defaults = {(item.tag, item.get("name")): item for item in strings}
    seen_hindi = set()
    critical = {"create_note", "library_title", "tool_pen", "tool_eraser",
                "tool_highlighter", "caption_size", "undo", "redo", "cancel",
                "rename_note", "save_as", "toolbar_tab_notes", "toolbar_tab_canvas",
                "canvas_all_tools", "pref_toolbar", "kind_canvas", "editor_header_canvas"}
    translated = set()

    def format_tokens(value):
        # Match Android positional and plain printf tokens without translating them.
        return sorted(re.findall(r"%(?:\d+\$)?[-+#0, .]*\d*(?:\.\d+)?[a-zA-Z%]",
                                 "".join(value.itertext())))

    for item in hi_resources:
        key = (item.tag, item.get("name"))
        require(key not in seen_hindi, "Duplicate Hindi resource " + str(key))
        seen_hindi.add(key)
        source = defaults.get(key)
        require(source is not None, "Unknown Hindi resource " + str(key))
        if source is None:
            continue
        require(source.get("translatable") != "false",
                "Non-translatable resource overridden in Hindi: " + str(key))
        if item.tag == "string":
            translated.add(item.get("name"))
            require(format_tokens(item) == format_tokens(source),
                    "Hindi formatting tokens differ: " + str(key))
        elif item.tag == "plurals":
            variants = {child.get("quantity"): child for child in source}
            require(any(child.get("quantity") == "other" for child in item),
                    "Hindi plural missing 'other': " + str(key))
            for child in item:
                original = variants.get(child.get("quantity"))
                require(original is not None and format_tokens(child) == format_tokens(original),
                        "Hindi plural format mismatch: " + str(key) + "/" +
                        str(child.get("quantity")))
        elif item.tag == "string-array":
            require(len(item) == len(source), "Hindi array length differs: " + str(key))
            for translated_item, english_item in zip(item, source):
                require(format_tokens(translated_item) == format_tokens(english_item),
                        "Hindi array format mismatch: " + str(key))
    require(critical.issubset(translated),
            "Missing essential Hindi controls: " + str(sorted(critical - translated)))
    expected_hindi = {(item.tag, item.get("name")) for item in strings
                      if item.get("translatable") != "false"}
    require(seen_hindi == expected_hindi,
            "Missing Hindi resources: " + str(sorted(expected_hindi - seen_hindi)))

renderer = read("app/src/main/java/com/notestaking/app/platform/AndroidRenderer.kt")
require("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {\n            nativeBlend(blend)?.let" in renderer,
        "Android 26 raster blend API guard missing")
for name in ("app/src/main/java/com/notestaking/app/MainActivity.kt",
             "app/src/main/java/com/notestaking/app/ui/Backstage.kt",
             "app/src/main/java/com/notestaking/app/ui/TrashPane.kt"):
    text = read(name)
    require("LocalResources.current" in text, "Compose locale-aware resource local missing: " + name)

drawables = {p.stem for p in RES.rglob("*.png")}
drawables.update(p.stem for p in RES.rglob("*.xml"))
for item in RES.rglob("*.xml"):
    for name in re.findall(r"@drawable/([a-zA-Z_0-9]+)", item.read_text(encoding="utf-8")):
        require(name in drawables, "Missing drawable " + name + " in " + str(item))

pngs = list(RES.rglob("ic_launcher*.png")) + sorted(
    (RES / "drawable-nodpi").glob("notestaking_frame_*.png"))
require(len(pngs) >= 39, "Missing original branded launcher or animation frames")
for item in pngs:
    require(png_ok(item), "PNG checksum or chunk boundary failure: " + str(item))

# Never commit signing material into the GitHub repository.
if os.environ.get("GITHUB_ACTIONS") == "true":
    # On a clean GitHub checkout these files should never be committed. Local
    # developers may legitimately have ignored local.properties or a keystore.
    for candidate in ("keystore.properties", "local.properties",
                      "app/release-upload.jks", "app/release-upload.keystore"):
        require(not (ROOT / candidate).exists(), "Private credential committed to repository: " + candidate)

print(f"NotesTaking App source checks: {len(main_files)} Kotlin sources, "
      f"{len(test_files)} test sources, {len(xmls)} XML files, "
      f"{len(pngs)} PNGs; {len(problems)} problems.")
for error in problems:
    print("ERROR:", error, file=sys.stderr)
sys.exit(1 if problems else 0)
