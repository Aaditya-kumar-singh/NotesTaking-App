package com.notestaking.app.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import org.xml.sax.SAXException

class SecureXmlTest {
    @Test fun parsesNormalSimpleXml() {
        val xml = """<note xmlns="urn:test"><page>Hello &amp; world</page></note>"""
        val parsed = SecureXml.builder().parse(ByteArrayInputStream(xml.toByteArray()))
        assertEquals("Hello & world", parsed.documentElement.textContent)
    }

    @Test fun rejectsExternalEntityAndDoctype() {
        val hostile = """<?xml version="1.0"?>
            <!DOCTYPE note [<!ENTITY leak SYSTEM "file:///etc/passwd">]>
            <note>&leak;</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.builder().parse(ByteArrayInputStream(hostile.toByteArray()))
        }
    }

    @Test fun rejectsInternalEntityExpansion() {
        val hostile = """<!DOCTYPE note [<!ENTITY many "long long long">]>
            <note>&many;</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.builder().parse(ByteArrayInputStream(hostile.toByteArray()))
        }
    }
}
