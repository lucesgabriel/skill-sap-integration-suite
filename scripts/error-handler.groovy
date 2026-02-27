/*
 * SAP Cloud Integration - Error Handler with MPL Logging
 * Purpose: Robust error handling script for Exception Subprocess.
 *          Captures error details, logs to MPL, and sets properties
 *          for downstream alerting (email, Slack, Teams webhook).
 *
 * Where to use: Place inside an Exception Subprocess as the FIRST step.
 *
 * Properties set by this script (use in subsequent Content Modifier / alert steps):
 *   - ErrorTimestamp     : ISO 8601 UTC timestamp
 *   - ErrorMessage       : Human-readable error description
 *   - ErrorClass         : Java exception class name
 *   - ErrorStackTrace    : First 10 lines of stack trace
 *   - ErroriFlowName     : Integration flow name (from SAP headers)
 *   - ErrorMessageId     : SAP Message Processing Log ID
 *   - ErrorCorrelationId : Correlation ID for distributed tracing
 *   - ErrorHttpCode      : HTTP status code (if HTTP-related error)
 *   - ErrorSeverity      : LOW / MEDIUM / HIGH / CRITICAL
 */

import com.sap.gateway.ip.core.customdev.util.Message
import java.text.SimpleDateFormat

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    
    try {
        // ── 1. Capture error details from Exchange Exception ──
        def ex = message.getProperties().get("CamelExceptionCaught")
        def errorMessage = ex?.message ?: "Unknown error"
        def errorClass = ex?.class?.name ?: "Unknown"
        def stackTrace = ex?.stackTrace?.take(10)?.collect { it.toString() }?.join("\n") ?: "No stack trace"
        
        // ── 2. Capture context from SAP headers ──
        def headers = message.getHeaders()
        def iFlowName = headers.get("SAP_IntegrationFlowName") ?: 
                        message.getProperty("SAP_IntegrationFlowName") ?: "Unknown"
        def messageId = headers.get("SAP_MessageProcessingLogID") ?: 
                        message.getProperty("SAP_MessageProcessingLogID") ?: "N/A"
        def correlationId = headers.get("SAP_ApplicationID") ?: 
                           message.getProperty("SAP_ApplicationID") ?: 
                           UUID.randomUUID().toString()
        
        // ── 3. Determine severity based on error type ──
        def severity = classifyError(errorClass, errorMessage)
        
        // ── 4. Extract HTTP status code if applicable ──
        def httpCode = extractHttpCode(errorMessage, ex)
        
        // ── 5. Build timestamp ──
        def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
        def timestamp = formatter.format(new Date())
        
        // ── 6. Set properties for downstream steps ──
        message.setProperty("ErrorTimestamp", timestamp)
        message.setProperty("ErrorMessage", errorMessage)
        message.setProperty("ErrorClass", errorClass)
        message.setProperty("ErrorStackTrace", stackTrace)
        message.setProperty("ErroriFlowName", iFlowName)
        message.setProperty("ErrorMessageId", messageId)
        message.setProperty("ErrorCorrelationId", correlationId)
        message.setProperty("ErrorHttpCode", httpCode)
        message.setProperty("ErrorSeverity", severity)
        
        // ── 7. Log structured error to MPL ──
        def errorReport = """
========================================
ERROR REPORT — ${timestamp}
========================================
iFlow:          ${iFlowName}
Message ID:     ${messageId}
Correlation ID: ${correlationId}
Severity:       ${severity}
HTTP Code:      ${httpCode}
----------------------------------------
Error Class:    ${errorClass}
Error Message:  ${errorMessage}
----------------------------------------
Stack Trace (first 10 lines):
${stackTrace}
========================================
""".stripIndent().trim()
        
        messageLog.addAttachmentAsString("ErrorReport", errorReport, "text/plain")
        
        // ── 8. Log original payload that caused the error (truncated for safety) ──
        def originalBody = message.getBody(String.class) ?: ""
        if (originalBody.length() > 5000) {
            originalBody = originalBody.substring(0, 5000) + "\n... [TRUNCATED at 5000 chars]"
        }
        messageLog.addAttachmentAsString("FailedPayload", originalBody, "text/plain")
        
        // ── 9. Build error response body (JSON) for alert endpoints ──
        def errorJson = groovy.json.JsonOutput.toJson([
            timestamp:     timestamp,
            severity:      severity,
            iflow:         iFlowName,
            messageId:     messageId,
            correlationId: correlationId,
            httpCode:      httpCode,
            error:         errorMessage,
            errorClass:    errorClass
        ])
        message.setBody(groovy.json.JsonOutput.prettyPrint(errorJson))
        
    } catch (Exception innerEx) {
        // Fallback: if error handler itself fails, log minimal info
        messageLog?.addAttachmentAsString("ErrorHandlerFailed", 
            "Error handler crashed: ${innerEx.message}", "text/plain")
        message.setProperty("ErrorMessage", "Error handler failed: ${innerEx.message}")
        message.setProperty("ErrorSeverity", "CRITICAL")
    }
    
    return message
}

/**
 * Classify error severity based on exception type and message.
 */
def String classifyError(String errorClass, String errorMessage) {
    def msg = errorMessage?.toLowerCase() ?: ""
    def cls = errorClass?.toLowerCase() ?: ""
    
    // CRITICAL: Authentication / certificate / security failures
    if (msg.contains("401") || msg.contains("403") || msg.contains("certificate") ||
        msg.contains("ssl") || msg.contains("unauthorized") || msg.contains("forbidden")) {
        return "CRITICAL"
    }
    
    // HIGH: Connection failures, timeouts, system unavailable
    if (msg.contains("connection refused") || msg.contains("timeout") || 
        msg.contains("503") || msg.contains("502") || msg.contains("unreachable") ||
        cls.contains("socketexception") || cls.contains("connectexception")) {
        return "HIGH"
    }
    
    // MEDIUM: Data/parsing errors, bad request
    if (msg.contains("400") || msg.contains("parsing") || msg.contains("invalid") ||
        msg.contains("404") || cls.contains("parseexception") || 
        cls.contains("jsonexception") || cls.contains("saxexception")) {
        return "MEDIUM"
    }
    
    // LOW: Everything else
    return "LOW"
}

/**
 * Extract HTTP status code from error message or exception chain.
 */
def String extractHttpCode(String errorMessage, Exception ex) {
    // Try to find HTTP code in error message
    def matcher = (errorMessage =~ /\b([4-5]\d{2})\b/)
    if (matcher.find()) {
        return matcher.group(1)
    }
    
    // Check for HttpOperationFailedException
    if (ex?.class?.name?.contains("HttpOperationFailedException")) {
        try {
            return ex.getStatusCode()?.toString() ?: "N/A"
        } catch (ignored) {}
    }
    
    return "N/A"
}
