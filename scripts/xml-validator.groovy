/*
 * SAP Cloud Integration - XML Schema Validator
 * Purpose: Validate incoming XML payloads against an XSD schema.
 *          Reports detailed validation errors with line numbers.
 *          Can operate in strict mode (throw on error) or lenient
 *          mode (log warnings and continue).
 *
 * Where to use: Place AFTER the sender adapter and BEFORE any mapping
 *               or processing steps, to reject malformed payloads early.
 *
 * XSD Schema source: Upload your .xsd file as a Resource in the iFlow
 *   (Resources tab → Add → Schema → XML Schema). Reference it by filename.
 *
 * Configuration via Exchange Properties (set in Content Modifier BEFORE this script):
 *   - xsdSchemaName    : Name of the XSD resource file (e.g., "PurchaseOrder.xsd")
 *   - xsdValidateMode  : "strict" = throw on first error, "lenient" = log and continue (default: "strict")
 *   - xsdLogPayload    : "true" to attach invalid payload to MPL (default: "false")
 *   - xsdMaxErrors     : Max errors to collect in lenient mode (default: "10")
 *
 * Output properties set:
 *   - xmlIsValid       : "true" or "false"
 *   - xmlErrorCount    : Number of validation errors found
 *   - xmlErrors        : Pipe-separated list of error messages
 */

import com.sap.gateway.ip.core.customdev.util.Message
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    
    try {
        // ── 1. Read configuration ──
        def schemaName   = message.getProperty("xsdSchemaName")   ?: ""
        def validateMode = message.getProperty("xsdValidateMode") ?: "strict"
        def logPayload   = message.getProperty("xsdLogPayload")  ?: "false"
        def maxErrors    = (message.getProperty("xsdMaxErrors")   ?: "10").toInteger()
        
        if (!schemaName) {
            throw new Exception("xsdSchemaName property not set. Specify the XSD resource filename.")
        }
        
        // ── 2. Load XSD schema from iFlow resources ──
        def schemaStream = this.class.classLoader.getResourceAsStream(schemaName)
        if (schemaStream == null) {
            throw new Exception(
                "XSD schema '${schemaName}' not found in iFlow resources. " +
                "Upload it via: Integration Flow → Resources → Add → Schema → XML Schema"
            )
        }
        
        // ── 3. Create schema validator ──
        def schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        def schema = schemaFactory.newSchema(new StreamSource(schemaStream))
        def validator = schema.newValidator()
        
        // ── 4. Set up error collection ──
        def errors = []
        def warnings = []
        
        validator.setErrorHandler(new ErrorHandler() {
            void warning(SAXParseException e) {
                if (warnings.size() < maxErrors) {
                    warnings.add("WARNING [line ${e.lineNumber}, col ${e.columnNumber}]: ${e.message}")
                }
            }
            void error(SAXParseException e) {
                if (errors.size() < maxErrors) {
                    errors.add("ERROR [line ${e.lineNumber}, col ${e.columnNumber}]: ${e.message}")
                }
                if (validateMode == "strict") {
                    throw e
                }
            }
            void fatalError(SAXParseException e) {
                errors.add("FATAL [line ${e.lineNumber}, col ${e.columnNumber}]: ${e.message}")
                throw e
            }
        })
        
        // ── 5. Validate ──
        def body = message.getBody(String.class)
        
        if (!body || body.trim().isEmpty()) {
            throw new Exception("Message body is empty. Cannot validate empty XML.")
        }
        
        def isValid = true
        try {
            validator.validate(new StreamSource(new java.io.StringReader(body)))
        } catch (SAXParseException e) {
            isValid = false
            // Error already captured by ErrorHandler
        } catch (org.xml.sax.SAXException e) {
            isValid = false
            if (errors.isEmpty()) {
                errors.add("VALIDATION ERROR: ${e.message}")
            }
        }
        
        if (!errors.isEmpty()) {
            isValid = false
        }
        
        // ── 6. Set output properties ──
        def allErrors = errors.join(" | ")
        message.setProperty("xmlIsValid", isValid.toString())
        message.setProperty("xmlErrorCount", errors.size().toString())
        message.setProperty("xmlErrors", allErrors)
        
        // ── 7. Log results ──
        if (isValid) {
            messageLog.addAttachmentAsString("XMLValidation", 
                "✅ XML is valid against schema '${schemaName}'", "text/plain")
            
            if (!warnings.isEmpty()) {
                messageLog.addAttachmentAsString("XMLValidationWarnings",
                    warnings.join("\n"), "text/plain")
            }
        } else {
            def errorReport = new StringBuilder()
            errorReport.append("❌ XML validation FAILED against schema '${schemaName}'\n")
            errorReport.append("Errors found: ${errors.size()}\n")
            errorReport.append("─".multiply(60)).append("\n")
            errors.eachWithIndex { err, idx ->
                errorReport.append("${idx + 1}. ${err}\n")
            }
            if (errors.size() >= maxErrors) {
                errorReport.append("... (showing first ${maxErrors} errors only)\n")
            }
            
            messageLog.addAttachmentAsString("XMLValidationErrors", 
                errorReport.toString(), "text/plain")
            
            // Optionally log the invalid payload
            if (logPayload == "true") {
                def truncated = body.length() > 10000 ? 
                    body.substring(0, 10000) + "\n... [TRUNCATED]" : body
                messageLog.addAttachmentAsString("InvalidPayload", truncated, "application/xml")
            }
            
            // In strict mode, throw to trigger Exception Subprocess
            if (validateMode == "strict") {
                throw new Exception(
                    "XML validation failed with ${errors.size()} error(s) against '${schemaName}'. " +
                    "First error: ${errors[0]}"
                )
            }
        }
        
        return message
        
    } catch (Exception e) {
        if (e.message?.startsWith("XML validation failed")) {
            // Re-throw validation errors as-is
            throw e
        }
        messageLog?.addAttachmentAsString("XMLValidatorError",
            "Validator script error: ${e.message}", "text/plain")
        message.setProperty("ScriptErrorMessage", e.message)
        throw new Exception("XML Validation Script Error: ${e.message}", e)
    }
}
