# Cloud Integration Reference

Complete guide for SAP Cloud Integration development including iFlow design, process steps, patterns, and best practices.

## Table of Contents

- [Integration Flow Structure](#integration-flow-structure)
- [Process Steps Reference](#process-steps-reference)
- [Message Processing Patterns](#message-processing-patterns)
- [Modular Design Patterns](#modular-design-patterns)
- [Quality of Service](#quality-of-service)
- [Performance Optimization](#performance-optimization)
- [Best Practices](#best-practices)

---

## ⚠️ iFlow Artifact Generation Policy

**NEVER generate .iflw or .zip artifacts for upload.** The BPMN2 XML in `.iflw` files contains `cmdVariantUri` properties tied to specific adapter/step component versions registered on each tenant. These versions vary by:
- SAP region (us10, eu10, ap10, etc.)
- Tenant type (trial vs production)
- Monthly SAP update cycle

Externally generated artifacts cause:
- *"Error while loading the details of the integration flow"* → ZIP structure or BPMN2 schema mismatch
- *"Unable to render property sheet. Metadata not available or not registered"* → `cmdVariantUri` version mismatch

**Always provide instead:**
1. Step-by-step UI creation guide (Design → Add Integration Flow)
2. Groovy/JS scripts ready to copy/paste
3. Configuration table: adapter settings, Allowed Headers, Exchange Properties
4. Architecture diagram in ASCII notation

### Step-by-Step UI Guide Template

When creating an iFlow guide, follow this structure:

```
Paso 1: Crear Integration Flow
  → Design → Create → Integration Flow → Name, Package

Paso 2: Configurar Runtime Configuration (click fondo blanco)
  → Allowed Header(s): Header1|Header2|Header3
  
Paso 3: Configurar Sender Adapter
  → Sender → Connector → Adapter Type → Settings

Paso 4: Agregar pasos del Integration Process
  → Content Modifier: Headers, Properties
  → Script: [provide code separately]
  → Request Reply / Send / etc.

Paso 5: Configurar Receiver Adapter
  → Receiver → Connector → Adapter Type → Settings

Paso 6: Agregar Exception Subprocess
  → Content Modifier con mensaje de error

Paso 7: Save → Deploy
```

### Content Modifier Configuration Table Template

When documenting Content Modifier settings, use this table format for clarity:

**Message Header tab:**

| Action | Name | Source Type | Source Value |
|--------|------|------------|--------------|
| Create | HeaderName | Constant/Header/Property/XPath | value |

**Exchange Property tab:**

| Action | Name | Source Type | Source Value |
|--------|------|------------|--------------|
| Create | propertyName | Header | OriginalHeaderName |

⚠️ **Common pitfall:** If a header is listed in Allowed Headers but there is NO Exchange Property row reading it into a property, the Groovy script receives `null` for that value. Always verify that each header has a corresponding Exchange Property row in the Content Modifier.

---

## Integration Flow Structure

### Basic iFlow Architecture

```
Sender System → [Sender Adapter] → Integration Process → [Receiver Adapter] → Receiver System
                                           ↓
                           ┌───────────────┴───────────────┐
                           │  Message Processing Steps     │
                           │  - Content Modifier            │
                           │  - Router/Filter               │
                           │  - Mapping                     │
                           │  - Splitter/Aggregator         │
                           │  - Script                      │
                           │  - External Call               │
                           │  - Security                    │
                           └────────────────────────────────┘
                                           ↓
                              Exception Subprocess (Error Handling)
```

### iFlow Components

**Start Event:**
- Timer (scheduled/cron-based)
- Message (event-driven, triggered by sender)

**End Events:**
- End Event (normal termination)
- End Message (terminate with custom response)
- Error End Event (propagate error)
- Escalation End Event (trigger escalation)

**Intermediate Events:**
- Timer (wait for duration)
- Message (wait for external trigger)
- Error (catch specific error)
- Escalation (catch escalation)

---

## Process Steps Reference

### Routing Steps

#### Router (Content-Based)
Routes messages to different paths based on conditions.

**Use Cases:**
- Route by message content (e.g., order type, region, priority)
- Route by header values
- Route by property values

**Configuration:**
- XPath for XML: `/Orders/Order/Type = 'Premium'`
- JSONPath for JSON: `$.order.type == 'Premium'`
- Simple expressions: `${header.Country} = 'US'`

**Best Practices:**
- Always include a default route
- Use meaningful route names for debugging
- Keep conditions simple and readable
- Consider using Content Modifier before Router for complex logic

#### Multicast
Sends copies of the message to multiple receivers in parallel.

**Use Cases:**
- Notify multiple systems simultaneously
- Send to primary and backup systems
- Distribute data to multiple targets

**Configuration:**
- Parallel processing (all branches execute simultaneously)
- Sequential processing (branches execute in order)
- Stop on exception (optional)

**Pattern:**
```
[Start] → [Multicast] ─┬→ [Branch 1] → [Receiver 1]
                        ├→ [Branch 2] → [Receiver 2]
                        └→ [Branch 3] → [Receiver 3]
```

#### Recipient List
Dynamically determines receivers at runtime based on message content.

**Use Cases:**
- Send to recipients defined in message payload
- Dynamic partner routing in B2B scenarios
- Multi-tenant message distribution

**Configuration:**
- Recipient list from header: `${header.RecipientList}`
- Delimiter: comma, semicolon, or custom
- Can use ProcessDirect addresses for internal routing

### Transformation Steps

#### Content Modifier
Adds, modifies, or deletes headers, properties, and message body.

**Use Cases:**
- Set headers for downstream adapters
- Store temporary data in properties
- Add static content to body
- Extract data for logging

**Configuration:**
- Message Header: Add/Modify/Delete headers
- Exchange Property: Add/Modify properties
- Message Body: Set static or dynamic content

**Expression Types:**
- Simple: `${property.myValue}`
- XPath: `//Order/OrderNumber`
- JSONPath: `$.order.number`
- Script: Groovy/JavaScript for complex logic

**Best Practices:**
- Use Content Modifier for simple transformations instead of scripts
- Set properties early for use throughout the flow
- Clear sensitive data from headers after use
- Use meaningful property/header names

#### Message Mapping
Graphical mapping tool for structure transformations.

**Use Cases:**
- XML to XML transformations
- Flat file to XML
- Complex structure mapping with multiple sources

**Features:**
- Drag-and-drop field mapping
- Built-in functions (string, date, arithmetic, etc.)
- Custom functions (Groovy/JavaScript)
- Value mapping (lookup tables)
- 1:1, 1:n, n:1 cardinality support

**Best Practices:**
- Use Message Mapping for complex structure changes
- Prefer XSLT for transformation logic with extensive conditionals
- Test with real payload samples
- Document complex mapping logic in function descriptions

#### XSLT Mapping
Template-based XML transformations using XSLT 1.0/2.0/3.0.

**Use Cases:**
- Complex conditional transformations
- Restructuring with template matching
- Namespace handling
- Transformations requiring XSLT-specific functions

**Example:**
```xml
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <Output>
      <xsl:for-each select="//Order">
        <xsl:if test="Status = 'Active'">
          <Order>
            <ID><xsl:value-of select="OrderID"/></ID>
          </Order>
        </xsl:if>
      </xsl:for-each>
    </Output>
  </xsl:template>
</xsl:stylesheet>
```

#### Converters
Built-in converters for format transformations.

**Available Converters:**
- CSV to XML
- XML to CSV
- JSON to XML
- XML to JSON
- EDI to XML (X12, EDIFACT)
- XML to EDI
- Base64 Encoder/Decoder
- MIME Multipart Encoder/Decoder
- Zip/Unzip
- Gzip/Gunzip

**Best Practices:**
- Use built-in converters instead of scripts when possible
- Validate source format before conversion
- Handle encoding properly (UTF-8 recommended)
- Consider message size when converting

### Splitting and Aggregating

#### General Splitter
Splits a message into multiple sub-messages.

**Split Strategies:**
- XPath (for XML): `/Orders/Order`
- JSONPath (for JSON): `$.orders[*]`
- Token (delimiter-based): Split by newline, comma, etc.
- IDoc (for SAP IDoc messages)

**Configuration:**
- Parallel Processing: Process splits concurrently
- Number of Messages: Limit splits (e.g., batch size)
- Grouping: Group splits before processing
- Streaming: For large messages (avoid loading all in memory)

**Pattern with Aggregator:**
```
[Splitter] → [Process Each] → [Aggregator]
```

#### Iterating Splitter
Splits and processes each sub-message sequentially.

**Use Cases:**
- Sequential processing required (order matters)
- Processing with side effects (file creation, DB updates)
- Rate-limited external calls

**Configuration:**
- Expression Type: XPath, JSONPath, Line Break
- Stop on Exception: Halt on first error or continue
- Timeout: Maximum time for all iterations

#### Gather
Collects all split messages without merging them.

**Use Cases:**
- Count processed messages
- Wait for all splits before next step
- Error handling after parallel processing

#### Aggregator
Combines multiple messages into one.

**Aggregation Strategies:**
- Combine: Merge all messages
- Concatenate: Join message bodies with delimiter
- Define: Custom aggregation logic (Groovy script)

**Completion Conditions:**
- All messages received
- Timeout reached
- Custom condition (script)

**Use Cases:**
- Combine split messages after processing
- Batch multiple incoming messages
- Create summary reports

### External Calls

#### Request Reply
Synchronous call to external system.

**Use Cases:**
- Enrich message with external data
- Validate data against external system
- Synchronous query-response patterns

**Pattern:**
```
[Request Reply] -(HTTP/OData/SOAP)-> [External System]
     ↓ (wait for response)
[Continue Processing]
```

**Best Practices:**
- Set appropriate timeout
- Handle errors with Exception Subprocess
- Use asynchronous patterns for long-running calls
- Consider circuit breaker patterns for external dependencies

#### Send
Asynchronous call to external system (fire-and-forget).

**Use Cases:**
- Send notification without waiting for response
- Trigger downstream process
- Write to queue or topic

**Configuration:**
- No response expected
- Can continue processing immediately
- Errors handled via Exception Subprocess

#### Content Enricher
Enriches message with data from external source.

**Use Cases:**
- Look up master data (customer, product)
- Add reference data from database
- Enrich with data from REST API

**Configuration:**
- Original Message: Preserve in property
- Enrichment Resource: Configure call to external system
- Aggregation Algorithm: Combine original + enriched data

#### Poll Enrich
Periodically retrieves data from external source.

**Use Cases:**
- Regular data synchronization
- Polling for status updates
- Scheduled data retrieval

### Persistence Steps

#### Data Store Write
Stores message in Data Store for later retrieval.

**Use Cases:**
- Implement idempotent processing
- Store intermediate results
- Cross-iFlow data sharing
- Duplicate detection

**Configuration:**
- Data Store Name: Global or Local
- Entry ID: Unique identifier (from header/property/XPath)
- Retention Period: How long to keep (up to 90 days)
- Overwrite: Replace existing entry or fail

**Best Practices:**
- Use meaningful Entry IDs
- Set appropriate retention period
- Monitor Data Store size (100MB limit)
- Clean up old entries regularly

#### Data Store Read/Get/Delete
Retrieves or removes entries from Data Store.

**Operations:**
- Get: Read single entry by ID
- Select: Query multiple entries
- Delete: Remove entry by ID

#### Write Variables
Stores data in global or local variables.

**Use Cases:**
- Share configuration across iFlows
- Store counters or state
- Cache reference data

**Scope:**
- Global: Accessible by all iFlows
- Local: Accessible within current iFlow

#### Persist
Persists entire message for reprocessing.

**Use Cases:**
- Persist before risky operations
- Enable manual reprocessing
- Audit trail

### Security Steps

#### Encryptor
Encrypts message content.

**Encryption Types:**
- PGP Encryption
- PKCS#7/CMS Encryption
- XML Encryption

**Configuration:**
- Public Key: From keystore
- Algorithm: AES, 3DES, etc.
- Compression: Optional (before encryption)

#### Decryptor
Decrypts message content.

**Decryption Types:**
- PGP Decryption (requires private key)
- PKCS#7/CMS Decryption
- XML Decryption

#### Signer
Creates digital signature.

**Signature Types:**
- PKCS#7/CMS Signer
- XML Signer
- Simple Signer (message hash)

#### Verifier
Verifies digital signature.

**Use Cases:**
- Ensure message integrity
- Verify sender authenticity
- B2B non-repudiation

---

## Message Processing Patterns

### Pattern 1: Content-Based Routing

**Scenario:** Route orders to different systems based on order type.

```
[Sender] → [Content Modifier: Extract Type] → [Router]
              ├─(Type=Premium)─→ [Map Premium] → [Premium System]
              ├─(Type=Standard)─→ [Map Standard] → [Standard System]
              └─(Default)────────→ [Log Error] → [Error Handler]
```

### Pattern 2: Split-Process-Aggregate

**Scenario:** Process large file with multiple records.

```
[Sender: File] → [Splitter: By Record] → [Parallel Processing]
                                              ↓
                                         [Validate]
                                              ↓
                                         [Transform]
                                              ↓
                                         [Aggregator]
                                              ↓
                                         [Send Result]
```

### Pattern 3: Synchronous Request-Reply

**Scenario:** Look up customer details before processing order.

```
[Sender: Order] → [Extract Customer ID] → [Request Reply: GET Customer]
                                              ↓
                                         [Content Enricher: Add Customer Data]
                                              ↓
                                         [Process Order]
                                              ↓
                                         [Send Confirmation]
```

### Pattern 4: Asynchronous with Callback

**Scenario:** Long-running process with callback notification.

```
[Sender: Request] → [Store Request ID] → [Send: Async Processing]
                                              ↓
                                         [Return 202 Accepted]

[Separate iFlow for Callback]
[Timer] → [Poll Status] → [If Complete] → [Retrieve Result] → [Notify Sender]
```

### Pattern 5: Idempotent Processing

**Scenario:** Prevent duplicate order processing.

```
[Sender: Order] → [Extract Order ID]
                        ↓
                  [Data Store Get: Check if exists]
                        ↓
                  [Router: Is Duplicate?]
                   ├─(Yes)─→ [Return Cached Response]
                   └─(No)──→ [Process Order]
                                ↓
                           [Data Store Write: Mark as Processed]
                                ↓
                           [Send Response]
```

### Pattern 6: Event-Driven with JMS

**Scenario:** Decouple sender and receiver via message queue.

```
[Sender] → [JMS Send: Queue] ─┐
[Sender] → [JMS Send: Queue] ─┤
[Sender] → [JMS Send: Queue] ─┤ → [JMS Queue: order.queue]
                               ↓
                          [Consumer iFlow]
                          [JMS Receiver] → [Process] → [Target System]
```

---

## Modular Design Patterns

### ProcessDirect Communication

**Use Case:** Break complex iFlow into modular, reusable sub-flows.

**Parent iFlow:**
```
[Start] → [Validate] → [ProcessDirect Call: /validation/customer]
                              ↓
                         [Response from Sub-flow]
                              ↓
                         [Continue Processing]
```

**Sub-flow (Reusable):**
```
[ProcessDirect: /validation/customer] → [Validate Customer] → [Return Result]
```

**Benefits:**
- Modular design
- Reusability across iFlows
- Easier testing and maintenance
- Clear separation of concerns

**Best Practices:**
- Use meaningful ProcessDirect addresses (e.g., `/validation/customer`)
- Document input/output contract (expected headers, properties)
- Keep sub-flows focused on single responsibility
- Version sub-flows carefully (changes affect all consumers)

### Local Integration Process

**Use Case:** Modularize within a single iFlow without creating separate iFlow.

```
[Main Integration Process]
    ↓
[Call: Local Integration Process A]
    ↓
[Call: Local Integration Process B]
    ↓
[End]
```

**Benefits:**
- Keep related logic together
- Avoid creating many small iFlows
- Easier to understand flow hierarchy
- Encapsulate error handling per section

### Looping Process Call

**Use Case:** Retry logic, polling, or iterative processing.

```
[Start Loop] → [Process] → [Check Condition]
                  ↑              ↓ (Not Met)
                  └──────(Loop Back)
                                 ↓ (Met)
                            [Exit Loop]
```

**Configuration:**
- Loop Condition: Expression to evaluate
- Maximum Iterations: Prevent infinite loops
- Break on Exception: Exit on error

---

## Quality of Service

### At-Least-Once (Default)
- Messages guaranteed to be delivered
- May result in duplicates
- Suitable for idempotent operations

### Exactly-Once (EOIO)
- No duplicates, maintains order
- Uses JMS exclusive queues
- Performance impact (sequential processing)

**Configuration:**
```
JMS Receiver Adapter:
- Queue Name: order.queue
- Quality of Service: Exactly Once In Order
```

### Best-Effort
- No delivery guarantee
- Highest performance
- Suitable for non-critical notifications

---

## Performance Optimization

### Large File Handling

**DO:**
- ✅ Use streaming for large files
- ✅ Split files before processing
- ✅ Use SAX parser for XML (not DOM)
- ✅ Process in chunks
- ✅ Use SFTP/FTP instead of HTTP for large files

**DON'T:**
- ❌ Load entire file into memory
- ❌ Use DOM parser for >10MB XML files
- ❌ Convert large files multiple times
- ❌ Log full payload in scripts

### Parallel Processing

**Use Case:** Process multiple records concurrently.

**Configuration:**
```
[Splitter]
- ✅ Enable Parallel Processing
- ✅ Set appropriate batch size
- ✅ Configure timeout
```

**Thread Pool Considerations:**
- Default: 10 threads per iFlow
- Can be increased for high-volume scenarios
- Monitor thread usage in MPL

### Memory Management

**Best Practices:**
- Use Content Modifier instead of Script for simple operations
- Release resources in scripts (close connections, streams)
- Avoid creating unnecessary copies of large objects
- Use StringBuilder for string concatenation in loops
- Clear unused headers/properties

---

## Best Practices

### iFlow Design

1. **Keep It Simple**
   - Start with simplest solution that meets requirements
   - Add complexity only when necessary
   - Document complex logic

2. **Modular Design**
   - Break complex flows into smaller, reusable sub-flows
   - Use ProcessDirect for inter-iFlow communication
   - Create Local Integration Processes for logical grouping

3. **Error Handling**
   - Always add Exception Subprocess
   - Log errors with context (headers, payload sample)
   - Set appropriate error responses
   - Consider retry strategies

4. **Testing**
   - Test with real payload samples
   - Test error scenarios
   - Use Trace mode for debugging
   - Test at expected volume

5. **Documentation**
   - Add descriptions to iFlow and steps
   - Document expected inputs/outputs
   - Note dependencies and prerequisites
   - Version iFlows appropriately

### Naming Conventions

**iFlows:**
- Use descriptive names: `Order_Processing_SAP_to_Salesforce`
- Include direction: `Sender_to_Receiver`
- Add version: `_v1`, `_v2`

**Variables/Properties:**
- Use camelCase: `orderNumber`, `customerEmail`
- Prefix scope: `global_`, `local_`
- Be descriptive: Avoid `temp`, `data`, `value`

**Headers:**
- Follow standard conventions: `Content-Type`, `Authorization`
- Custom headers: `X-Custom-OrderID`

### Security

1. **Credentials**
   - Never hardcode credentials
   - Use User Credentials artifact
   - Rotate credentials regularly

2. **Sensitive Data**
   - Don't log sensitive data
   - Don't expose in headers (visible in trace)
   - Encrypt sensitive payloads
   - Use secure protocols (HTTPS, SFTP, TLS)

3. **Access Control**
   - Limit who can deploy iFlows
   - Use separate tenants for dev/qa/prod
   - Review security settings regularly

### Monitoring

1. **Logging**
   - Add meaningful log messages
   - Log at key decision points
   - Include correlation IDs
   - Don't over-log (performance impact)

2. **Message Processing Logs**
   - Review MPL regularly
   - Set up alerts for errors
   - Archive logs if needed beyond 30 days

3. **Alerts**
   - Configure alerts for critical errors
   - Use SAP Alert Notification Service
   - Integrate with monitoring tools (Cloud ALM, Splunk)

---

## Common iFlow Scenarios

### Scenario 1: File to API
```
[SFTP: Poll Files] → [Convert CSV to XML] → [Splitter] → [Map] → [HTTP: POST to API]
Exception: [Log Error] → [Move File to Error Folder]
```

### Scenario 2: API to Database
```
[HTTP: Webhook] → [Validate JSON] → [Map to SQL] → [JDBC: INSERT]
Exception: [Return HTTP 500] → [Log Error]
```

### Scenario 3: Real-time Event Processing
```
[Kafka: Subscribe Topic] → [Filter Events] → [Enrich Data] → [ProcessDirect: Multiple Targets]
```

### Scenario 4: B2B EDI Processing
```
[AS2: Receive] → [EDI to XML] → [Validate] → [Map to SAP IDoc] → [RFC: Call SAP]
Exception: [Send Negative Acknowledgment] → [Log Error]
```

---

**Next Steps:**
- See [adapters.md](adapters.md) for adapter configurations (80+ types)
- See [scripting.md](scripting.md) for Groovy/JavaScript patterns and anti-patterns
- See [message-mapping-xslt.md](message-mapping-xslt.md) for mapping techniques and XSLT patterns
- See [security.md](security.md) for authentication, encryption, and keystore management
- See [operations-monitoring.md](operations-monitoring.md) for MPL monitoring and OData API
- See [troubleshooting.md](troubleshooting.md) for error catalog and debugging techniques
- See [simulation-testing.md](simulation-testing.md) for design-time iFlow testing
- See [postman-testing.md](postman-testing.md) for Postman collection generation
- See [content-transport.md](content-transport.md) for CI/CD and TMS deployment
- See [cloud-connector.md](cloud-connector.md) for on-premise system connectivity
- See [excalidraw-diagrams.md](excalidraw-diagrams.md) for visual iFlow diagram patterns

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about iFlow design fundamentals, integration patterns, and Cloud Integration architecture.
