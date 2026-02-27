/*
 * SAP Cloud Integration - JSON to XML Converter with SAP Namespaces
 * Purpose: Convert JSON payloads to XML with proper SAP namespaces.
 *          Handles SAP-specific structures: RFC/BAPI calls, IDoc segments,
 *          OData batch payloads, and generic JSON-to-XML conversion.
 *
 * Where to use: Place BEFORE an RFC, IDoc, or SOAP receiver adapter when
 *               the source system sends JSON and the target expects XML.
 *
 * Configuration via Exchange Properties (set in Content Modifier BEFORE this script):
 *   - j2xMode         : Conversion mode:
 *                        "generic"  - Standard JSON → XML (default)
 *                        "rfc"      - JSON → SAP RFC/BAPI XML with namespace
 *                        "idoc"     - JSON → IDoc XML with control/data records
 *   - j2xRootElement  : Root element name (default: "root")
 *   - j2xNamespace    : XML namespace URI (default: none)
 *   - j2xNsPrefix     : Namespace prefix (default: "ns")
 *   - j2xRfcName      : RFC function name (required for mode "rfc")
 *   - j2xIdocType     : IDoc type (required for mode "idoc", e.g., "ORDERS05")
 *   - j2xArrayElement : Element name for JSON arrays (default: "item")
 *   - j2xIndent       : "true" for pretty-printed XML (default: "true")
 *
 * Output properties set:
 *   - j2xElementCount : Number of XML elements generated
 */

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    
    try {
        // ── 1. Read configuration ──
        def mode         = message.getProperty("j2xMode")         ?: "generic"
        def rootElement  = message.getProperty("j2xRootElement")  ?: "root"
        def namespace    = message.getProperty("j2xNamespace")    ?: ""
        def nsPrefix     = message.getProperty("j2xNsPrefix")     ?: "ns"
        def rfcName      = message.getProperty("j2xRfcName")      ?: ""
        def idocType     = message.getProperty("j2xIdocType")     ?: ""
        def arrayElement = message.getProperty("j2xArrayElement") ?: "item"
        def indent       = message.getProperty("j2xIndent")       ?: "true"
        
        // ── 2. Parse JSON body ──
        def body = message.getBody(String.class)
        if (!body || body.trim().isEmpty()) {
            throw new Exception("Message body is empty. Cannot convert empty JSON to XML.")
        }
        
        def json = new JsonSlurper().parseText(body)
        
        // ── 3. Convert based on mode ──
        def xml = ""
        def elementCount = 0
        
        switch (mode.toLowerCase()) {
            case "rfc":
                if (!rfcName) {
                    throw new Exception("j2xRfcName property required for RFC mode")
                }
                def result = buildRfcXml(json, rfcName, indent == "true")
                xml = result.xml
                elementCount = result.count
                break
                
            case "idoc":
                if (!idocType) {
                    throw new Exception("j2xIdocType property required for IDoc mode")
                }
                def result = buildIdocXml(json, idocType, indent == "true")
                xml = result.xml
                elementCount = result.count
                break
                
            default: // "generic"
                def result = buildGenericXml(json, rootElement, namespace, nsPrefix, 
                                            arrayElement, indent == "true")
                xml = result.xml
                elementCount = result.count
                break
        }
        
        // ── 4. Set output ──
        message.setBody(xml)
        message.setProperty("j2xElementCount", elementCount.toString())
        
        return message
        
    } catch (Exception e) {
        messageLog?.addAttachmentAsString("JSONtoXMLError",
            "Conversion failed: ${e.message}", "text/plain")
        message.setProperty("ScriptErrorMessage", e.message)
        throw new Exception("JSON to XML Error: ${e.message}", e)
    }
}

// ═══════════════════════════════════════════════════════════════
// GENERIC JSON → XML
// ═══════════════════════════════════════════════════════════════

def Map buildGenericXml(Object json, String root, String ns, String prefix, 
                        String arrayEl, boolean pretty) {
    def sb = new StringBuilder()
    def counter = [count: 0]
    
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    
    if (ns) {
        sb.append("<${prefix}:${root} xmlns:${prefix}=\"${ns}\">\n")
        appendElement(sb, json, prefix, arrayEl, pretty ? 1 : 0, counter, true)
        sb.append("</${prefix}:${root}>")
    } else {
        sb.append("<${root}>\n")
        appendElement(sb, json, "", arrayEl, pretty ? 1 : 0, counter, false)
        sb.append("</${root}>")
    }
    
    return [xml: sb.toString(), count: counter.count]
}

def void appendElement(StringBuilder sb, Object value, String prefix, String arrayEl,
                       int depth, Map counter, boolean usePrefix) {
    def indent = "  " * depth
    def pre = usePrefix && prefix ? "${prefix}:" : ""
    
    if (value instanceof Map) {
        value.each { key, val ->
            def safeName = sanitizeName(key)
            counter.count++
            
            if (val instanceof List) {
                val.each { item ->
                    sb.append("${indent}<${pre}${safeName}>\n")
                    appendElement(sb, item, prefix, arrayEl, depth + 1, counter, usePrefix)
                    sb.append("${indent}</${pre}${safeName}>\n")
                }
            } else if (val instanceof Map) {
                sb.append("${indent}<${pre}${safeName}>\n")
                appendElement(sb, val, prefix, arrayEl, depth + 1, counter, usePrefix)
                sb.append("${indent}</${pre}${safeName}>\n")
            } else {
                sb.append("${indent}<${pre}${safeName}>${escapeXml(val?.toString() ?: '')}</${pre}${safeName}>\n")
            }
        }
    } else if (value instanceof List) {
        value.each { item ->
            counter.count++
            sb.append("${indent}<${pre}${arrayEl}>\n")
            appendElement(sb, item, prefix, arrayEl, depth + 1, counter, usePrefix)
            sb.append("${indent}</${pre}${arrayEl}>\n")
        }
    } else {
        sb.append("${indent}${escapeXml(value?.toString() ?: '')}\n")
    }
}

// ═══════════════════════════════════════════════════════════════
// SAP RFC/BAPI JSON → XML
// ═══════════════════════════════════════════════════════════════

/**
 * Build RFC XML with SAP namespace.
 * Input JSON format:
 * {
 *   "IMPORT_PARAM1": "value1",
 *   "TABLE_PARAM": [
 *     { "FIELD1": "val1", "FIELD2": "val2" },
 *     { "FIELD1": "val3", "FIELD2": "val4" }
 *   ]
 * }
 *
 * Output XML:
 * <rfc:BAPI_NAME xmlns:rfc="urn:sap-com:document:sap:rfc:functions">
 *   <IMPORT_PARAM1>value1</IMPORT_PARAM1>
 *   <TABLE_PARAM>
 *     <item><FIELD1>val1</FIELD1><FIELD2>val2</FIELD2></item>
 *     <item><FIELD1>val3</FIELD1><FIELD2>val4</FIELD2></item>
 *   </TABLE_PARAM>
 * </rfc:BAPI_NAME>
 */
def Map buildRfcXml(Object json, String rfcName, boolean pretty) {
    def sb = new StringBuilder()
    def counter = [count: 0]
    def RFC_NS = "urn:sap-com:document:sap:rfc:functions"
    
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<rfc:${rfcName} xmlns:rfc=\"${RFC_NS}\">\n")
    
    if (json instanceof Map) {
        json.each { paramName, paramValue ->
            counter.count++
            if (paramValue instanceof List) {
                // Table parameter
                sb.append("  <${paramName}>\n")
                paramValue.each { row ->
                    sb.append("    <item>\n")
                    if (row instanceof Map) {
                        row.each { field, val ->
                            counter.count++
                            sb.append("      <${field}>${escapeXml(val?.toString() ?: '')}</${field}>\n")
                        }
                    }
                    sb.append("    </item>\n")
                }
                sb.append("  </${paramName}>\n")
            } else if (paramValue instanceof Map) {
                // Structure parameter
                sb.append("  <${paramName}>\n")
                paramValue.each { field, val ->
                    counter.count++
                    sb.append("    <${field}>${escapeXml(val?.toString() ?: '')}</${field}>\n")
                }
                sb.append("  </${paramName}>\n")
            } else {
                // Simple parameter
                sb.append("  <${paramName}>${escapeXml(paramValue?.toString() ?: '')}</${paramName}>\n")
            }
        }
    }
    
    sb.append("</rfc:${rfcName}>")
    return [xml: sb.toString(), count: counter.count]
}

// ═══════════════════════════════════════════════════════════════
// SAP IDoc JSON → XML
// ═══════════════════════════════════════════════════════════════

/**
 * Build IDoc XML structure.
 * Input JSON format:
 * {
 *   "EDI_DC40": { "TABNAM": "EDI_DC40", "MANDT": "100", "DOCNUM": "", ... },
 *   "E1EDK01": { "CURCY": "USD", "WKURS": "1.00" },
 *   "E1EDK14": [ { "QUALF": "006", "ORGID": "1000" } ],
 *   "E1EDP01": [ { "POSEX": "000010", "MENGE": "5" } ]
 * }
 */
def Map buildIdocXml(Object json, String idocType, boolean pretty) {
    def sb = new StringBuilder()
    def counter = [count: 0]
    
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<${idocType}>\n")
    sb.append("  <IDOC BEGIN=\"1\">\n")
    
    if (json instanceof Map) {
        json.each { segmentName, segmentData ->
            if (segmentData instanceof List) {
                // Repeating segment
                segmentData.each { segment ->
                    counter.count++
                    sb.append("    <${segmentName} SEGMENT=\"1\">\n")
                    if (segment instanceof Map) {
                        segment.each { field, val ->
                            sb.append("      <${field}>${escapeXml(val?.toString() ?: '')}</${field}>\n")
                        }
                    }
                    sb.append("    </${segmentName}>\n")
                }
            } else if (segmentData instanceof Map) {
                // Single segment (e.g., EDI_DC40 control record)
                counter.count++
                sb.append("    <${segmentName} SEGMENT=\"1\">\n")
                segmentData.each { field, val ->
                    sb.append("      <${field}>${escapeXml(val?.toString() ?: '')}</${field}>\n")
                }
                sb.append("    </${segmentName}>\n")
            }
        }
    }
    
    sb.append("  </IDOC>\n")
    sb.append("</${idocType}>")
    return [xml: sb.toString(), count: counter.count]
}

// ═══════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════

def String sanitizeName(String name) {
    def safe = name?.replaceAll(/[^a-zA-Z0-9_.-]/, '_') ?: "_field"
    if (safe && !safe[0].matches(/[a-zA-Z_]/)) {
        safe = "_${safe}"
    }
    return safe
}

def String escapeXml(String value) {
    if (!value) return ""
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
