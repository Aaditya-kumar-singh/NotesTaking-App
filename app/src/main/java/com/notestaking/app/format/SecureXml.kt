package com.notestaking.app.format

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.xml.sax.SAXException

/** Parse UTF-8 or BOM-marked UTF-16 XML without DTDs on Android and the JVM. */
internal object SecureXml {
    fun parse(bytes: ByteArray): Document {
        // Android's DOM factory does not support JAXP security features. Enforce the
        // mandatory DTD ban on the original input before using its parser.
        val utf16 = bytes.size >= 2 &&
            ((bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) ||
                (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()))
        val xml = try {
            (if (utf16) Charsets.UTF_16 else Charsets.UTF_8).newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: java.nio.charset.CharacterCodingException) {
            throw SAXException("Unsupported XML encoding", e)
        }
        if ('\u0000' in xml || xml.contains("<!DOCTYPE", ignoreCase = true) ||
            xml.contains("<!ENTITY", ignoreCase = true)) {
            throw SAXException("XML DTDs and entities are not allowed")
        }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // Still enable available defense-in-depth features on JVM providers.
            // Unsupported Android features are safe to ignore after the preflight above.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            isExpandEntityReferences = false
            runCatching { isXIncludeAware = false }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        return factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> throw SAXException("External XML entities are not allowed") }
        }.parse(ByteArrayInputStream(bytes))
    }
}
