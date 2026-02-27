# Scripting Reference

Groovy and JavaScript patterns, APIs, and best practices for SAP Cloud Integration.

## Table of Contents

- [When to Use Scripts](#when-to-use-scripts)
- [Groovy Script Template](#groovy-script-template)
- [Common Script Patterns](#common-script-patterns)
- [Script Collections](#script-collections)
- [Performance Best Practices](#performance-best-practices)
- [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## When to Use Scripts

**✅ Use Scripts For:**
- Complex conditional logic not possible with routers
- Data transformations requiring loops or algorithms
- Dynamic endpoint configuration
- Custom parsing/validation logic
- Mathematical calculations
- Working with APIs not available in standard steps

**❌ Prefer Standard Steps For:**
- Simple header/property modifications → Use Content Modifier
- Basic routing → Use Router
- Standard transformations → Use Message Mapping or XSLT
- Format conversions → Use Converters (CSV to XML, JSON to XML, etc.)

**Rule of Thumb:** If a standard step can do it, use the standard step. Scripts are harder to debug and maintain.

---

## Groovy Script Template

### Basic Template

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import java.util.HashMap

def Message processData(Message message) {
    try {
        // 1. Access message components
        def body = message.getBody(String.class)
        def headers = message.getHeaders()
        def properties = message.getProperties()
        def messageLog = messageLogFactory.getMessageLog(message)
        
        // 2. Your processing logic here
        messageLog.addAttachmentAsString("OriginalBody", body, "text/plain")
        
        // Example: Parse JSON
        def slurper = new groovy.json.JsonSlurper()
        def json = slurper.parseText(body)
        
        // Example: Modify data
        json.processedDate = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
        
        // Example: Build new JSON
        def builder = new groovy.json.JsonBuilder(json)
        def newBody = builder.toPrettyString()
        
        // 3. Set modified body
        message.setBody(newBody)
        
        // 4. Set headers/properties
        message.setProperty("processedRecords", json.records.size().toString())
        
        messageLog.addAttachmentAsString("ProcessedBody", newBody, "text/plain")
        
        return message
        
    } catch (Exception e) {
        messageLog.addAttachmentAsString("Error", e.toString(), "text/plain")
        throw new Exception("Script Error: ${e.message}", e)
    }
}
```

---

## Common Script Patterns

### 1. XML Processing

#### Parse XML (XmlSlurper - Recommended)

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.*

def Message processData(Message message) {
    def body = message.getBody(String.class)
    
    // ✅ USE THIS for large payloads (streaming)
    def xml = new XmlSlurper().parseText(body)
    
    // Access elements
    def orderId = xml.Order.OrderID.text()
    def items = xml.Order.Items.Item
    
    // Iterate
    items.each { item ->
        println "Product: ${item.ProductID.text()}, Qty: ${item.Quantity.text()}"
    }
    
    return message
}
```

#### Build XML (MarkupBuilder)

```groovy
import groovy.xml.MarkupBuilder

def writer = new StringWriter()
def xml = new MarkupBuilder(writer)

xml.Orders {
    Order {
        OrderID('12345')
        Customer('ACME Corp')
        Items {
            Item {
                ProductID('P001')
                Quantity('10')
            }
        }
    }
}

def newBody = writer.toString()
message.setBody(newBody)
```

#### Modify XML

```groovy
def body = message.getBody(String.class)
def xml = new XmlSlurper().parseText(body)

// Modify value
xml.Order.Status = 'Processed'

// Add element
xml.Order.appendNode {
    ProcessedDate(new Date().format("yyyy-MM-dd"))
}

// Convert back to string
def builder = new StreamingMarkupBuilder()
def newBody = builder.bind { mkp.yield xml }
message.setBody(newBody.toString())
```

### 2. JSON Processing

#### Parse JSON

```groovy
import groovy.json.JsonSlurper

def body = message.getBody(String.class)
def json = new JsonSlurper().parseText(body)

// Access properties
def orderId = json.orderId
def items = json.items

// Iterate
items.each { item ->
    println "Product: ${item.productId}, Price: ${item.price}"
}
```

#### Build JSON

```groovy
import groovy.json.JsonBuilder

def data = [
    orderId: '12345',
    customer: 'ACME Corp',
    items: [
        [productId: 'P001', quantity: 10],
        [productId: 'P002', quantity: 5]
    ]
]

def json = new JsonBuilder(data)
message.setBody(json.toPrettyString())
```

#### Modify JSON

```groovy
import groovy.json.*

def body = message.getBody(String.class)
def json = new JsonSlurper().parseText(body)

// Modify
json.status = 'Processed'
json.processedDate = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")

// Add new property
json.processedBy = 'SAP CPI'

// Convert back
def newBody = JsonOutput.toJson(json)
message.setBody(newBody)
```

### 3. Working with Headers and Properties

```groovy
// Get header
def contentType = message.getHeader("Content-Type", String.class)

// Set header
message.setHeader("X-Correlation-ID", UUID.randomUUID().toString())

// Remove header
message.removeHeader("Authorization") // ⚠️ Be careful - don't remove needed headers

// Get property
def orderId = message.getProperty("orderId", String.class)

// Set property
message.setProperty("totalAmount", "1500.00")

// Get all headers (returns Map)
def headers = message.getHeaders()
headers.each { key, value ->
    println "$key: $value"
}

// Get all properties
def properties = message.getProperties()
```

### 4. Logging

```groovy
def messageLog = messageLogFactory.getMessageLog(message)

// Log text
messageLog.addAttachmentAsString("LogMessage", "Processing started", "text/plain")

// Log payload
messageLog.addAttachmentAsString("Payload", message.getBody(String.class), "application/json")

// Log headers
def headers = message.getHeaders()
messageLog.addAttachmentAsString("Headers", headers.toString(), "text/plain")

// ⚠️ WARNING: Logging increases message size and can impact performance
// Only log in development or for critical debugging
```

### 5. Data Store Operations

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.mapping.*

def Message processData(Message message) {
    def service = ITApiFactory.getService(DataStoreService.class, null)
    
    // Write to Data Store
    service.write(message, "OrderDataStore", "ORDER-12345", "order data here", "30d")
    
    // Read from Data Store
    def entry = service.get(message, "OrderDataStore", "ORDER-12345")
    if (entry != null) {
        def storedData = new String(entry)
        println "Found: $storedData"
    }
    
    // Delete from Data Store
    service.delete(message, "OrderDataStore", "ORDER-12345")
    
    return message
}
```

### 6. HTTP Calls from Script

```groovy
import groovy.json.*

def url = "https://api.example.com/customers/12345"
def connection = new URL(url).openConnection()
connection.setRequestMethod("GET")
connection.setRequestProperty("Authorization", "Bearer ${message.getProperty('accessToken')}")
connection.setRequestProperty("Content-Type", "application/json")

// Send request
connection.connect()

// Read response
def responseCode = connection.getResponseCode()
if (responseCode == 200) {
    def response = connection.getInputStream().getText("UTF-8")
    def json = new JsonSlurper().parseText(response)
    
    // Use response data
    message.setProperty("customerName", json.name)
} else {
    throw new Exception("API call failed: ${responseCode}")
}

connection.disconnect()
```

### 7. Base64 Encoding/Decoding

```groovy
// Encode
def text = "Hello World"
def encoded = text.bytes.encodeBase64().toString()
message.setBody(encoded)

// Decode
def encoded = message.getBody(String.class)
def decoded = new String(encoded.decodeBase64())
message.setBody(decoded)
```

### 8. Date/Time Formatting

```groovy
import java.text.SimpleDateFormat

// Current date/time
def now = new Date()

// Format date
def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
def formattedDate = formatter.format(now)

// Parse date string
def dateString = "2024-01-15T10:30:00Z"
def parsedDate = formatter.parse(dateString)

// Add days
use(groovy.time.TimeCategory) {
    def tomorrow = now + 1.day
    def nextWeek = now + 7.days
}

message.setProperty("processedDate", formattedDate)
```

### 9. Regular Expressions

```groovy
def body = message.getBody(String.class)

// Pattern matching
def pattern = ~/ORDER-\d{5}/
def matcher = body =~ pattern

if (matcher.find()) {
    def orderId = matcher.group()
    message.setProperty("orderId", orderId)
}

// Replace
def cleaned = body.replaceAll(/[^a-zA-Z0-9]/, '_')
message.setBody(cleaned)

// Extract all matches
def emails = []
(body =~ /[\w.-]+@[\w.-]+\.\w+/).each { match ->
    emails << match[0]
}
```

### 10. Dynamic Routing

```groovy
def body = message.getBody(String.class)
def json = new JsonSlurper().parseText(body)

// Determine endpoint based on data
def endpoint = ""
if (json.amount > 10000) {
    endpoint = "https://api.example.com/high-value-orders"
} else {
    endpoint = "https://api.example.com/standard-orders"
}

// Set as property (use in HTTP adapter Address with ${property.endpoint})
message.setProperty("endpoint", endpoint)
```

---

## Script Collections

### Creating Reusable Script Library

**Script Collection:** `CommonUtils`

```groovy
// File: CommonUtils.groovy
class CommonUtils {
    
    static String formatDate(Date date, String pattern = "yyyy-MM-dd") {
        def formatter = new java.text.SimpleDateFormat(pattern)
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
        return formatter.format(date)
    }
    
    static String generateUUID() {
        return UUID.randomUUID().toString()
    }
    
    static Map parseJSON(String json) {
        return new groovy.json.JsonSlurper().parseText(json)
    }
    
    static String toJSON(Object obj) {
        return groovy.json.JsonOutput.toJson(obj)
    }
    
    static boolean isValidEmail(String email) {
        return email ==~ /[\w.-]+@[\w.-]+\.\w+/
    }
}
```

### Using Script Collection

```groovy
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    // Import script collection
    def utils = CommonUtils.class
    
    // Use utility methods
    def uuid = utils.generateUUID()
    def formattedDate = utils.formatDate(new Date())
    
    message.setProperty("correlationId", uuid)
    message.setProperty("processedDate", formattedDate)
    
    return message
}
```

---

## Performance Best Practices

### 1. Use Appropriate Parsers

```groovy
// ❌ BAD: Creates entire DOM tree in memory
def xml = new XmlParser().parseText(largeXML)

// ✅ GOOD: Streaming parser
def xml = new XmlSlurper().parseText(largeXML)
```

### 2. String Concatenation

```groovy
// ❌ BAD: Creates many intermediate String objects
def result = ""
for (i in 1..1000) {
    result += "Line $i\n"
}

// ✅ GOOD: Uses StringBuilder
def sb = new StringBuilder()
for (i in 1..1000) {
    sb.append("Line ").append(i).append("\n")
}
def result = sb.toString()
```

### 3. Close Resources

```groovy
// ✅ GOOD: Close connections
def connection = null
try {
    connection = new URL(url).openConnection()
    // ... use connection
} finally {
    if (connection != null) {
        connection.disconnect()
    }
}
```

### 4. Limit Logging

```groovy
// ❌ BAD: Log everything
messageLog.addAttachmentAsString("Payload", message.getBody(String.class), "text/plain")

// ✅ GOOD: Log only when needed (e.g., error or debug mode)
if (message.getProperty("debug") == "true") {
    messageLog.addAttachmentAsString("Payload", message.getBody(String.class), "text/plain")
}
```

### 5. Avoid Unnecessary Parsing

```groovy
// ❌ BAD: Parse multiple times
def json1 = new JsonSlurper().parseText(body)
def json2 = new JsonSlurper().parseText(body)

// ✅ GOOD: Parse once, reuse
def json = new JsonSlurper().parseText(body)
// Use json multiple times
```

---

## Anti-Patterns to Avoid

### ❌ 1. Setting Global Timezone

```groovy
// ❌ NEVER DO THIS - Affects entire VM
TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

// ✅ DO THIS - Set timezone per formatter
def formatter = new SimpleDateFormat("yyyy-MM-dd")
formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
```

### ❌ 2. Exposing Credentials in Headers

```groovy
// ❌ BAD: Visible in trace logs
message.setHeader("API-Key", "secret_key_12345")

// ✅ GOOD: Use Secure Parameter Store
def service = ITApiFactory.getService(SecureStoreService.class, null)
def credential = service.getUserCredential("API_Key_Credential")
def apiKey = new String(credential.getPassword())
// Use apiKey but don't store in header
```

### ❌ 3. Loading Large Files into Memory

```groovy
// ❌ BAD: Loads entire file into memory
def body = message.getBody(String.class)

// ✅ GOOD: Use streaming for large files
def inputStream = message.getBody(InputStream.class)
// Process stream without loading all into memory
```

### ❌ 4. Not Handling Exceptions

```groovy
// ❌ BAD: No error handling
def json = new JsonSlurper().parseText(body)

// ✅ GOOD: Handle errors
try {
    def json = new JsonSlurper().parseText(body)
} catch (Exception e) {
    messageLog.addAttachmentAsString("Error", "Invalid JSON: ${e.message}", "text/plain")
    throw new Exception("JSON parsing failed", e)
}
```

### ❌ 5. Hardcoding Values

```groovy
// ❌ BAD: Hardcoded URL
def url = "https://api.production.example.com/orders"

// ✅ GOOD: Use externalized parameters or properties
def url = message.getProperty("apiEndpoint")
```

---

## JavaScript Alternative

While Groovy is recommended, JavaScript is also supported:

```javascript
importPackage(com.sap.gateway.ip.core.customdev.util);

function processData(message) {
    var body = message.getBody(java.lang.String);
    var properties = message.getProperties();
    
    // Parse JSON
    var json = JSON.parse(body);
    
    // Modify
    json.processedDate = new Date().toISOString();
    
    // Set body
    message.setBody(JSON.stringify(json));
    
    return message;
}
```

**Note:** Groovy is more feature-rich and commonly used in CPI.

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for when to use scripts vs standard steps in iFlows
- See [message-mapping-xslt.md](message-mapping-xslt.md) for mapping alternatives (Message Mapping, XSLT)
- See [troubleshooting.md](troubleshooting.md) for script debugging tips and Groovy error patterns
- See [security.md](security.md) for secure parameter access and data masking patterns
- See [operations-monitoring.md](operations-monitoring.md) for custom MPL logging with Groovy
- See [simulation-testing.md](simulation-testing.md) for testing scripts with iFlow simulation

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about Groovy scripting patterns, JavaScript in CPI, and advanced script collections.
