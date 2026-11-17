package com.notestaking.app.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.charset.StandardCharsets
import org.xml.sax.SAXException

class SecureXmlTest {
    @Test fun parsesNormalSimpleXml() {
        val xml = """<note xmlns="urn:test"><page>Hello &amp; world</page></note>"""
        val parsed = SecureXml.parse(xml.toByteArray())
        assertEquals("Hello & world", parsed.documentElement.textContent)
        assertEquals("urn:test", parsed.documentElement.namespaceURI)
    }

    @Test fun parsesBomMarkedUtf16WithoutWeakeningDtdProtection() {
        val xml = """<?xml version="1.0" encoding="UTF-16"?><note>hello</note>"""
        assertEquals("hello", SecureXml.parse(xml.toByteArray(StandardCharsets.UTF_16)).documentElement.textContent)
        val hostile = """<?xml version="1.0" encoding="UTF-16"?>
            <!DOCTYPE note [<!ENTITY leak SYSTEM "file:///etc/passwd">]>
            <note>&leak;</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.parse(hostile.toByteArray(StandardCharsets.UTF_16))
        }
    }

    @Test fun rejectsAmbiguousEncodingAndMalformedUtf8BeforeDomParse() {
        val xml = "<!DOCTYPE note><note/>"
        assertThrows(SAXException::class.java) {
            SecureXml.parse(xml.toByteArray(StandardCharsets.UTF_16LE))
        }
        assertThrows(SAXException::class.java) {
            SecureXml.parse(byteArrayOf(0xC3.toByte(), 0x28))
        }
        assertThrows(SAXException::class.java) {
            SecureXml.parse("<note>\u0000</note>".toByteArray())
        }
    }

    @Test fun rejectsExternalEntityAndDoctype() {
        val hostile = """<?xml version="1.0"?>
            <!DOCTYPE note [<!ENTITY leak SYSTEM "file:///etc/passwd">]>
            <note>&leak;</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.parse(hostile.toByteArray())
        }
    }

    @Test fun rejectsExternalDtdWithoutEntityReference() {
        val hostile = """<!DOCTYPE note SYSTEM "https://example.invalid/external.dtd">
            <note>no entity used</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.parse(hostile.toByteArray())
        }
    }

    @Test fun rejectsParameterEntityDeclarations() {
        val hostile = """<!DOCTYPE note [
            <!ENTITY % payload SYSTEM "file:///etc/passwd">
            %payload;
            ]><note>unsafe</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.parse(hostile.toByteArray())
        }
    }

    @Test fun rejectsInternalEntityExpansion() {
        val hostile = """<!DOCTYPE note [<!ENTITY many "long long long">]>
            <note>&many;</note>""".trimIndent()
        assertThrows(SAXException::class.java) {
            SecureXml.parse(hostile.toByteArray())
        }
    }
}
