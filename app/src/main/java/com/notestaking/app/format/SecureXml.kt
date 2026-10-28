package com.notestaking.app.format

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.SAXException

/** Fail closed on DOCTYPE, reject every external entity, and disable XML inclusion. */
internal object SecureXml {
    fun builder(): DocumentBuilder = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        // This feature MUST succeed. If a parser implementation cannot reject DOCTYPE,
        // callers discard the imported XML rather than parse it insecurely.
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        isExpandEntityReferences = false
        // Android's bundled parser can differ from desktop JAXP. DTD prohibition is
        // mandatory; redundant feature toggles are best-effort for compatibility.
        runCatching { isXIncludeAware = false }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
        runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
    }.newDocumentBuilder().apply {
        setEntityResolver { _, _ -> throw SAXException("External XML entities are not allowed") }
    }
}
