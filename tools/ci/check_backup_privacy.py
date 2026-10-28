#!/usr/bin/env python3
"""Ensure private notes and working sessions are never silently extracted by Android backup."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res" / "xml"
MANIFEST = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
ANDROID = "{http://schemas.android.com/apk/res/android}"
DOMAINS = {"root", "file", "database", "sharedpref", "external",
           "device_root", "device_file", "device_database", "device_sharedpref"}


def excludes(node: ET.Element) -> set[str]:
    return {item.attrib["domain"] for item in node.findall("exclude")
            if item.attrib.get("path") in (".", "./")}


def main() -> None:
    app = ET.parse(MANIFEST).getroot().find("application")
    assert app is not None, "Missing application"
    assert app.get(ANDROID + "allowBackup") == "false", "Auto Backup must stay disabled"
    assert app.get(ANDROID + "fullBackupContent") == "@xml/backup_exclusions"
    assert app.get(ANDROID + "dataExtractionRules") == "@xml/data_extraction_rules"

    old = ET.parse(RES / "backup_exclusions.xml").getroot()
    assert old.tag == "full-backup-content"
    assert not list(old.iter("include")), "Do not opt data back into legacy backup"
    assert excludes(old) >= DOMAINS, "Legacy backup paths are not all excluded"

    new = ET.parse(RES / "data_extraction_rules.xml").getroot()
    assert new.tag == "data-extraction-rules"
    for name in ("cloud-backup", "device-transfer"):
        section = new.find(name)
        assert section is not None, f"Missing {name} rules (omission can enable transfer)"
        assert not list(section.iter("include")), f"Unexpected {name} include"
        assert excludes(section) >= DOMAINS, f"Incomplete {name} exclusions"
    print("PASS: private app data excluded from cloud and device-transfer backup")


if __name__ == "__main__":
    main()
