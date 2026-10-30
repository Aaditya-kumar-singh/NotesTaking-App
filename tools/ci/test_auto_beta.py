"""Fast, offline regression checks for the automated beta release contract."""
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from auto_beta import ABIS, LIBRARIES, metadata, next_tag, verify


class AutoBetaTest(unittest.TestCase):
    SHA = "a" * 40

    def write_apk(self, path):
        with zipfile.ZipFile(path, "w") as z:
            z.writestr("AndroidManifest.xml", b"manifest")
            z.writestr("classes.dex", b"dex")
            for abi in ABIS:
                for library in LIBRARIES:
                    z.writestr(f"lib/{abi}/{library}", b"ELF")

    def test_next_version_is_numeric_and_does_not_reuse_beta_three(self):
        self.assertEqual(next_tag(["v0.1.0-beta.1\n", "v0.1.0-beta.3\n"]), "v0.1.0-beta.4")
        self.assertEqual(next_tag(["v0.1.0-beta.9", "v0.1.0-beta.10",
                                   "v0.1.0", "invalid", "v2.0.0-beta.99"]), "v0.1.0-beta.11")

    def test_artifact_metadata_is_tied_to_exact_source_and_apk(self):
        with tempfile.TemporaryDirectory() as temp:
            apk = Path(temp) / "app-debug.apk"
            self.write_apk(apk)
            data = metadata("v0.1.0-beta.4", self.SHA, apk)
            self.assertEqual(data["version_code"], 4)
            self.assertEqual(data["version_name"], "0.1.0-beta.4-debug")
            self.assertEqual(verify(apk, data, self.SHA), data)
            with self.assertRaises(ValueError):
                verify(apk, data, "b" * 40)
            with self.assertRaises(ValueError):
                verify(apk, {**data, "tag": "v0.1.0-beta.5"}, self.SHA)
            apk.write_bytes(apk.read_bytes() + b"changed")
            with self.assertRaises(ValueError):
                verify(apk, data, self.SHA)

    def test_missing_native_library_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            apk = Path(temp) / "app-debug.apk"
            with zipfile.ZipFile(apk, "w") as z:
                z.writestr("AndroidManifest.xml", b"manifest")
                z.writestr("classes.dex", b"dex")
                z.writestr("lib/arm64-v8a/libnotestakingpdf.so", b"ELF")
            data = metadata("v0.1.0-beta.4", self.SHA, apk)
            with self.assertRaises(ValueError):
                verify(apk, data, self.SHA)


if __name__ == "__main__":
    unittest.main()
