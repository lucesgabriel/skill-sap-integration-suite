/*
 * SAP Cloud Integration - Dynamic Endpoint Routing
 * Purpose: Route messages to different receiver endpoints based on
 *          message content, headers, or lookup table configuration.
 *          Supports multi-criteria routing with fallback defaults.
 *
 * Where to use: Place BEFORE the receiver adapter. Configure the HTTP
 *               receiver adapter Address field as: ${property.targetEndpoint}
 *               and Method as: ${property.targetMethod}
 *
 * Configuration via Exchange Properties (set in Content Modifier BEFORE this script):
 *   - routingField     : JSON field path or XPath to evaluate (e.g., "orderType", "header.region")
 *   - routingSource    : "body" (parse from payload), "header", or "property" (default: "body")
 *   - routingFormat    : "json" or "xml" (default: "json")
 *   - routingDefault   : Fallback endpoint URL if no rule matches
 *   - routingRules     : JSON string with routing rules (see below)
 *
 * Routing rules format (set as externalized parameter for env-specific config):
 *   {
 *     "rules": [
 *       { "value": "ZOR",  "endpoint": "https://s4.example.com/api/sales-orders",  "method": "POST" },
 *       { "value": "ZRET", "endpoint": "https://s4.example.com/api/returns",        "method": "POST" },
 *       { "value": "ZSER", "endpoint": "https://crm.example.com/api/service-orders", "method": "PUT"  }
 *     ]
 *   }
 *
 * Output properties set:
 *   - targetEndpoint   : Resolved URL for the receiver adapter
 *   - targetMethod     : HTTP method (GET, POST, PUT, PATCH, DELETE)
 *   - routingMatchedRule : The value that matched (for traceability)
 *   - routingResolved  : "true" if a rule matched, "false" if using default
 */

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    
    try {
        // ── 1. Read routing configuration ──
        def routingField   = message.getProperty("routingField")   ?: "orderType"
        def routingSource  = message.getProperty("routingSource")  ?: "body"
        def routingFormat  = message.getProperty("routingFormat")  ?: "json"
        def routingDefault = message.getProperty("routingDefault") ?: ""
        def routingRulesJson = message.getProperty("routingRules") ?: '{"rules":[]}'
        
        // ── 2. Parse routing rules ──
        def rules = new JsonSlurper().parseText(routingRulesJson)
        
        if (!rules.rules || rules.rules.isEmpty()) {
            throw new Exception("No routing rules defined. Set 'routingRules' property with valid JSON.")
        }
        
        // ── 3. Extract routing value from message ──
        def routingValue = extractRoutingValue(message, routingField, routingSource, routingFormat)
        
        messageLog.addAttachmentAsString("RoutingValue", 
            "Field: ${routingField}, Source: ${routingSource}, Value: '${routingValue}'", "text/plain")
        
        // ── 4. Match against rules ──
        def matchedRule = rules.rules.find { rule ->
            matchesRule(routingValue, rule.value)
        }
        
        // ── 5. Set target endpoint ──
        if (matchedRule) {
            message.setProperty("targetEndpoint", matchedRule.endpoint)
            message.setProperty("targetMethod", matchedRule.method ?: "POST")
            message.setProperty("routingMatchedRule", matchedRule.value)
            message.setProperty("routingResolved", "true")
            
            // Set any additional properties defined in the rule
            matchedRule.each { key, value ->
                if (key != "value" && key != "endpoint" && key != "method") {
                    message.setProperty("routing_${key}", value?.toString())
                }
            }
            
            messageLog.addAttachmentAsString("RoutingResult",
                "Matched rule '${matchedRule.value}' → ${matchedRule.endpoint} [${matchedRule.method ?: 'POST'}]",
                "text/plain")
        } else if (routingDefault) {
            message.setProperty("targetEndpoint", routingDefault)
            message.setProperty("targetMethod", "POST")
            message.setProperty("routingMatchedRule", "__DEFAULT__")
            message.setProperty("routingResolved", "false")
            
            messageLog.addAttachmentAsString("RoutingResult",
                "No rule matched for '${routingValue}'. Using default: ${routingDefault}",
                "text/plain")
        } else {
            throw new Exception(
                "No routing rule matched for value '${routingValue}' " +
                "and no default endpoint configured. " +
                "Available rules: ${rules.rules.collect { it.value }.join(', ')}"
            )
        }
        
        return message
        
    } catch (Exception e) {
        messageLog?.addAttachmentAsString("RoutingError", 
            "Dynamic routing failed: ${e.message}", "text/plain")
        message.setProperty("ScriptErrorMessage", e.message)
        throw new Exception("Dynamic Routing Error: ${e.message}", e)
    }
}

/**
 * Extract the routing value from the message based on source type.
 */
def String extractRoutingValue(Message message, String field, String source, String format) {
    switch (source.toLowerCase()) {
        case "header":
            return message.getHeader(field, String.class) ?: ""
            
        case "property":
            return message.getProperty(field)?.toString() ?: ""
            
        case "body":
            def body = message.getBody(String.class)
            if (!body || body.trim().isEmpty()) {
                throw new Exception("Message body is empty, cannot extract routing value")
            }
            
            if (format.toLowerCase() == "xml") {
                return extractFromXml(body, field)
            } else {
                return extractFromJson(body, field)
            }
            
        default:
            throw new Exception("Invalid routingSource: '${source}'. Use 'body', 'header', or 'property'")
    }
}

/**
 * Extract a value from JSON body using dot-notation path.
 * Example: "order.header.type" → json.order.header.type
 */
def String extractFromJson(String body, String fieldPath) {
    def json = new JsonSlurper().parseText(body)
    def value = json
    
    fieldPath.split("\\.").each { segment ->
        if (value instanceof Map) {
            value = value[segment]
        } else if (value instanceof List && segment.isNumber()) {
            value = value[segment.toInteger()]
        } else {
            value = null
        }
    }
    
    return value?.toString() ?: ""
}

/**
 * Extract a value from XML body using a simple path.
 * Example: "Order.OrderType" → xml.Order.OrderType.text()
 */
def String extractFromXml(String body, String fieldPath) {
    def xml = new groovy.xml.XmlSlurper().parseText(body)
    def value = xml
    
    fieldPath.split("\\.").each { segment ->
        value = value."${segment}"
    }
    
    return value?.text() ?: ""
}

/**
 * Check if a routing value matches a rule value.
 * Supports exact match, wildcard (*), and prefix match (VALUE*).
 */
def boolean matchesRule(String actual, String ruleValue) {
    if (!ruleValue) return false
    
    // Wildcard: matches everything
    if (ruleValue == "*") return true
    
    // Prefix match: "ZOR*" matches "ZOR", "ZORP", "ZOR1"
    if (ruleValue.endsWith("*")) {
        def prefix = ruleValue[0..-2]
        return actual?.startsWith(prefix)
    }
    
    // Exact match (case-insensitive)
    return actual?.equalsIgnoreCase(ruleValue)
}
