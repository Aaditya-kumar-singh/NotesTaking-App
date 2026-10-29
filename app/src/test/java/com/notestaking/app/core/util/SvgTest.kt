package com.notestaking.app.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

class SvgTest {
    @Test fun smallSvgIsAcceptedByParserBudget() {
        val file = Files.createTempFile("notes-svg", ".svg").toFile()
        try {
            file.writeText("""<svg xmlns="http://www.w3.org/2000/svg"></svg>""")
            assertTrue(Svg.isSvgFile(file))
            assertTrue(Svg.withinParseLimit(file))
        } finally {
            file.delete()
        }
    }

    @Test fun missingAndEmptyFilesCannotBeParsed() {
        val file = Files.createTempFile("notes-svg-empty", ".svg").toFile()
        try {
            assertFalse(Svg.withinParseLimit(file))
            file.delete()
            assertFalse(Svg.withinParseLimit(file))
        } finally {
            file.delete()
        }
    }

    @Test fun oversizedSvgIsRejectedWithoutLoadingItsContents() {
        val file = Files.createTempFile("notes-svg-large", ".svg").toFile()
        try {
            // Sparse file: test metadata budget without allocating 12 MiB in JVM.
            RandomAccessFile(file, "rw").use { it.setLength(Svg.MAX_PARSE_BYTES) }
            assertTrue(Svg.withinParseLimit(file))
            RandomAccessFile(file, "rw").use { it.setLength(Svg.MAX_PARSE_BYTES + 1) }
            assertFalse(Svg.withinParseLimit(file))
        } finally {
            file.delete()
        }
    }

    @Test fun rasterSignatureIsNotMistakenForSvg() {
        val file = Files.createTempFile("notes-raster", ".png").toFile()
        try {
            file.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47))
            assertFalse(Svg.isSvgFile(file))
        } finally {
            file.delete()
        }
    }
}
