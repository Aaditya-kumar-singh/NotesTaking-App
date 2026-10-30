package com.notestaking.app.format

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BoundedZipInputStreamTest {
    private fun archive(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                for ((name, data) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }.toByteArray()

    private fun compressedLarge(name: String, mib: Int): ByteArray =
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry(name))
                val chunk = ByteArray(64 * 1024) { 'x'.code.toByte() }
                repeat(mib * 16) { zip.write(chunk) }
                zip.closeEntry()
            }
        }.toByteArray()

    @Test fun readsOrdinaryNoteEntries() {
        val manifest = """{"format":"xnote"}""".toByteArray()
        val bytes = archive("assets/image-000.png" to byteArrayOf(1, 2, 3), "manifest.json" to manifest)
        BoundedZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            assertEquals("assets/image-000.png", zip.nextEntry!!.name)
            assertArrayEquals(byteArrayOf(1, 2, 3), zip.readBytes())
            assertEquals("manifest.json", zip.nextEntry!!.name)
            assertArrayEquals(manifest, zip.readBytes())
            assertEquals(null, zip.nextEntry)
        }
    }

    @Test fun rejectsTraversalAndInvalidNames() {
        for (name in listOf("../private", "/absolute", "a/../../b", "a/./b", "bad\\path", "a//b")) {
            val bytes = archive(name to byteArrayOf(42))
            assertThrows(IOException::class.java) {
                BoundedZipInputStream(ByteArrayInputStream(bytes)).use { it.nextEntry }
            }
        }
    }

    @Test fun rejectsInflatedXmlEvenWhenMetadataSizeIsUnknown() {
        val bytes = compressedLarge("text/flow.xml", 17)
        assertThrows(IOException::class.java) {
            BoundedZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                zip.nextEntry
                val chunk = ByteArray(64 * 1024)
                while (zip.read(chunk) != -1) Unit
            }
        }
    }

    @Test fun skippedEntriesMustStillStayInsideBudget() {
        val bytes = compressedLarge("text/flow.xml", 17)
        assertThrows(IOException::class.java) {
            BoundedZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                zip.nextEntry
                zip.closeEntry() // Closing/ignoring an entry is not an escape hatch.
            }
        }
    }

    @Test fun rejectsDuplicateEntryNames() {
        // ZipOutputStream rejects duplicates itself; construct a ZIP with two separately
        // encoded entries by changing the second entry's name in the local and central records.
        val bytes = archive("a.txt" to byteArrayOf(1), "b.txt" to byteArrayOf(2))
        val original = "b.txt".toByteArray()
        val replacement = "a.txt".toByteArray()
        var replacements = 0
        for (i in 0..bytes.size - original.size) {
            if (original.indices.all { bytes[i + it] == original[it] }) {
                replacement.copyInto(bytes, i)
                replacements++
            }
        }
        assertEquals(2, replacements)
        assertThrows(IOException::class.java) {
            BoundedZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (zip.nextEntry != null) zip.closeEntry()
            }
        }
    }

    @Test fun rejectsOversizedManifestEvenWhenHighlyCompressed() {
        val bytes = compressedLarge("manifest.json", 65)
        assertThrows(IOException::class.java) {
            BoundedZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                zip.nextEntry
                val chunk = ByteArray(64 * 1024)
                while (zip.read(chunk) != -1) Unit
            }
        }
    }

    @Test fun rejectsTooManySmallEntries() {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                for (i in 0..10_000) {
                    zip.putNextEntry(ZipEntry("items/$i"))
                    zip.closeEntry()
                }
            }
        }.toByteArray()
        assertThrows(IOException::class.java) {
            BoundedZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (zip.nextEntry != null) zip.closeEntry()
            }
        }
    }
}
