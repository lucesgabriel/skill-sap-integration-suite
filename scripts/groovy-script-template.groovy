/*
 * SAP Cloud Integration - Groovy Script Template
 * Purpose: [Describe what this script does]
 */

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    try {
        def messageLog = messageLogFactory.getMessageLog(message)
        def body = message.getBody(String.class)
        
        // YOUR LOGIC HERE
        
        message.setBody(body)
        return message
        
    } catch (Exception e) {
        def messageLog = messageLogFactory.getMessageLog(message)
        messageLog.addAttachmentAsString("Error", e.toString(), "text/plain")
        throw new Exception("Script Error: ${e.message}", e)
    }
}
