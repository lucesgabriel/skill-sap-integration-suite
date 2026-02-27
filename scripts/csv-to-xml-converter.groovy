/*
 * SAP Cloud Integration - CSV to XML Converter (Streaming)
 * Purpose: Convert CSV/flat file payloads to well-formed XML.
 *          Uses streaming (BufferedReader + StringBuilder) to handle
 *          large files without loading everything into memory at once.
 *
 * Where to use: Place as a Script step AFTER the sender adapter
 *               (e.g., SFTP polling) and BEFORE any XML-based processing.
 *
 * Configuration via Exchange Properties (set in Content Modifier BEFORE this script):
 *   - csvDelimiter    : Field separator (default: ",")
 *   - csvHasHeader    : "true" if first row is header (default: "true")
 *   - csvRootElement  : XML root element name (default: "Records")
 *   - csvRowElement   : XML row element name (default: "Record")
 *   - csvEncoding     : File encoding (default: "UTF-8")
 *   - csvSkipEmpty    : "true" to skip empty rows (default: "true")
 *   - csvMaxRows      : Max rows to process, 0=unlimited (default: "0")
 *
 * Output properties set:
 *   - csvRowCount     : Number of rows processed
 *   - csvColumnCount  : Number of columns detected
 */

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    
    try {
        // ── 1. Read configuration from Exchange Properties ──
        def delimiter    = message.getProperty("csvDelimiter")   ?: ","
        def hasHeader    = message.getProperty("csvHasHeader")   ?: "true"
        def rootElement  = message.getProperty("csvRootElement") ?: "Records"
        def rowElement   = message.getProperty("csvRowElement")  ?: "Record"
        def encoding     = message.getProperty("csvEncoding")    ?: "UTF-8"
        def skipEmpty    = message.getProperty("csvSkipEmpty")   ?: "true"
        def maxRows      = (message.getProperty("csvMaxRows")    ?: "0").toInteger()
        
        // ── 2. Read body as InputStream for streaming (memory-safe) ──
        def inputStream = message.getBody(java.io.InputStream.class)
        def reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, encoding))
        
        // ── 3. Parse header row (column names) ──
        def columnNames = []
        def firstLine = reader.readLine()
        
        if (firstLine == null || firstLine.trim().isEmpty()) {
            throw new Exception("CSV file is empty")
        }
        
        if (hasHeader == "true") {
            columnNames = parseCsvLine(firstLine, delimiter)
            // Sanitize column names for XML element names
            columnNames = columnNames.collect { sanitizeXmlName(it) }
        } else {
            // No header: generate generic column names (Column1, Column2, ...)
            def fields = parseCsvLine(firstLine, delimiter)
            columnNames = (1..fields.size()).collect { "Column${it}" }
            // Re-process first line as data (put it back)
            reader = prependLine(firstLine, reader, encoding)
        }
        
        message.setProperty("csvColumnCount", columnNames.size().toString())
        
        // ── 4. Build XML with streaming (StringBuilder, not DOM) ──
        def xml = new StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"${encoding}\"?>\n")
        xml.append("<${rootElement}>\n")
        
        def rowCount = 0
        String line
        
        while ((line = reader.readLine()) != null) {
            // Skip empty rows
            if (skipEmpty == "true" && line.trim().isEmpty()) {
                continue
            }
            
            // Check max rows limit
            if (maxRows > 0 && rowCount >= maxRows) {
                break
            }
            
            def fields = parseCsvLine(line, delimiter)
            
            xml.append("  <${rowElement}>\n")
            
            for (int i = 0; i < columnNames.size() && i < fields.size(); i++) {
                def value = escapeXml(fields[i])
                xml.append("    <${columnNames[i]}>${value}</${columnNames[i]}>\n")
            }
            
            xml.append("  </${rowElement}>\n")
            rowCount++
        }
        
        xml.append("</${rootElement}>")
        reader.close()
        
        // ── 5. Set output ──
        message.setBody(xml.toString())
        message.setProperty("csvRowCount", rowCount.toString())
        
        messageLog.addAttachmentAsString("CSVConversionInfo", 
            "Converted ${rowCount} rows with ${columnNames.size()} columns", "text/plain")
        
        return message
        
    } catch (Exception e) {
        messageLog?.addAttachmentAsString("CSVConversionError", 
            "Error: ${e.message}", "text/plain")
        message.setProperty("ScriptErrorMessage", e.message)
        throw new Exception("CSV to XML conversion failed: ${e.message}", e)
    }
}

/**
 * Parse a single CSV line respecting quoted fields.
 * Handles: "field with, comma", "field with ""escaped"" quotes"
 */
def List<String> parseCsvLine(String line, String delimiter) {
    def fields = []
    def current = new StringBuilder()
    def inQuotes = false
    def chars = line.toCharArray()
    def delimChar = delimiter.charAt(0)
    
    for (int i = 0; i < chars.length; i++) {
        def c = chars[i]
        
        if (inQuotes) {
            if (c == '"' as char) {
                // Check for escaped quote ""
                if (i + 1 < chars.length && chars[i + 1] == '"' as char) {
                    current.append('"')
                    i++ // skip next quote
                } else {
                    inQuotes = false
                }
            } else {
                current.append(c)
            }
        } else {
            if (c == '"' as char) {
                inQuotes = true
            } else if (c == delimChar) {
                fields.add(current.toString().trim())
                current = new StringBuilder()
            } else {
                current.append(c)
            }
        }
    }
    fields.add(current.toString().trim())
    
    return fields
}

/**
 * Sanitize a string to be a valid XML element name.
 * Removes invalid chars, ensures it starts with a letter or underscore.
 */
def String sanitizeXmlName(String name) {
    // Remove leading/trailing whitespace
    def sanitized = name.trim()
    // Replace spaces and special chars with underscore
    sanitized = sanitized.replaceAll(/[^a-zA-Z0-9_.-]/, '_')
    // Ensure starts with letter or underscore
    if (sanitized && !sanitized[0].matches(/[a-zA-Z_]/)) {
        sanitized = "_${sanitized}"
    }
    // Handle empty name
    if (!sanitized) {
        sanitized = "_field"
    }
    return sanitized
}

/**
 * Escape special XML characters in a value.
 */
def String escapeXml(String value) {
    if (!value) return ""
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

/**
 * Create a new reader that first returns the given line, then continues with the original reader.
 */
def java.io.BufferedReader prependLine(String line, java.io.BufferedReader originalReader, String encoding) {
    def combined = new java.io.SequenceInputStream(
        new java.io.ByteArrayInputStream("${line}\n".getBytes(encoding)),
        new java.io.ReaderInputStream(originalReader, encoding)
    )
    return new java.io.BufferedReader(new java.io.InputStreamReader(combined, encoding))
}
