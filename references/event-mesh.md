# Event Mesh Reference

Complete guide for event-driven architecture with SAP Event Mesh, Advanced Event Mesh (AEM), and Cloud Integration.

## Table of Contents

- [Overview](#overview)
- [Event Mesh vs Advanced Event Mesh](#event-mesh-vs-advanced-event-mesh)
- [Core Concepts](#core-concepts)
- [CloudEvents Specification](#cloudevents-specification)
- [Event-Driven Integration Patterns](#event-driven-integration-patterns)
- [SAP S/4HANA Business Events](#sap-s4hana-business-events)
- [Configuration in Cloud Integration](#configuration-in-cloud-integration)
- [Dead Letter Topics and Error Handling](#dead-letter-topics-and-error-handling)
- [Webhooks](#webhooks)
- [Event Enrichment and Filtering](#event-enrichment-and-filtering)
- [EOIO with Event Mesh](#eoio-with-event-mesh)
- [Event Mesh Administration](#event-mesh-administration)
- [Best Practices](#best-practices)

---

## Overview

SAP provides two event broker services:

**Event Mesh:** Lightweight event broker integrated into SAP BTP for cloud-to-cloud and SAP-to-SAP event routing. Included in Integration Suite entitlement.

**Advanced Event Mesh (AEM):** Enterprise-grade event streaming platform (based on Solace PubSub+) for high-throughput, multi-protocol, and hybrid event-driven scenarios.

---

## Event Mesh vs Advanced Event Mesh

| Feature | Event Mesh | Advanced Event Mesh (AEM) |
|---|---|---|
| **Protocol Support** | AMQP 1.0, REST, MQTT (limited) | AMQP, MQTT, JMS, REST, SMF, WebSocket |
| **Throughput** | Moderate (cloud scenarios) | High (enterprise-grade streaming) |
| **Message Persistence** | Durable queues | Durable + Non-durable, Replay |
| **Topic Hierarchy** | Up to 5 levels | Up to 10+ levels with advanced wildcards |
| **Mesh Networking** | Single broker | Multi-broker mesh across regions/clouds |
| **Event Management** | Basic | Full Event Portal with schema registry |
| **Dynamic Routing** | Topic wildcards | Content-based routing, topic dispatch |
| **Hybrid Support** | Cloud only | Cloud + On-Premise + Multi-Cloud |
| **Pricing** | Included in Integration Suite | Separate license (consumption-based) |
| **Best For** | SAP-centric event routing | Enterprise event backbone, multi-cloud |

### When to Use Which

| Scenario | Recommendation |
|---|---|
| S/4HANA events → CPI processing | Event Mesh |
| Simple pub-sub between SAP systems | Event Mesh |
| High-throughput IoT event streaming | AEM |
| Multi-cloud event backbone | AEM |
| Event replay and reprocessing needed | AEM |
| Complex event routing with content filtering | AEM |
| MQTT protocol required (IoT devices) | AEM |
| Budget-constrained, SAP-only landscape | Event Mesh |

---

## Core Concepts

### Topics

Channels for publishing/subscribing events using hierarchical naming.

**Naming Convention:**
```
{namespace}/{business-object}/{event-type}/{version}

Examples:
sap/s4hana/salesorder/created/v1
sap/s4hana/businesspartner/changed/v1
custom/erp/inventory/low-stock/v1
mycompany/logistics/shipment/dispatched/v1
```

**Wildcards (for subscriptions only):**
- `*` matches exactly one level: `sap/s4hana/*/created/v1` matches any business object creation
- `>` matches one or more levels: `sap/s4hana/>` matches all S/4HANA events

### Queues

Durable message stores that receive events from subscribed topics.

| Queue Type | Consumers | Ordering | Use Case |
|---|---|---|---|
| **Exclusive Queue** | Single consumer | Guaranteed FIFO | EOIO processing |
| **Non-Exclusive Queue** | Multiple consumers | Not guaranteed | Load balancing, scaling |

### Subscriptions

Link topics to queues. A queue can subscribe to multiple topics.

```
Topic: sap/s4hana/salesorder/created/v1 ─┐
Topic: sap/s4hana/salesorder/changed/v1 ─┤→ Queue: "salesorder-processing"
Topic: sap/s4hana/salesorder/deleted/v1 ─┘        ↓
                                            Consumer iFlow (CPI)
```

### Message Structure

```json
{
  "headers": {
    "Content-Type": "application/json",
    "SAP_ApplicationID": "SO_CREATED_001"
  },
  "body": {
    "specversion": "1.0",
    "type": "sap.s4.beh.salesorder.v1.SalesOrder.Created.v1",
    "source": "/default/sap.s4.beh/S4HANA_PROD",
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "time": "2024-01-15T10:30:00Z",
    "data": {
      "SalesOrder": "1000001234",
      "SalesOrderType": "OR",
      "SoldToParty": "CUST001"
    }
  }
}
```

---

## CloudEvents Specification

SAP S/4HANA Business Events follow the **CloudEvents v1.0** specification, a CNCF standard for describing events.

### Required Attributes

| Attribute | Type | Description | Example |
|---|---|---|---|
| `specversion` | String | CloudEvents spec version | `"1.0"` |
| `type` | String | Event type identifier | `"sap.s4.beh.salesorder.v1.SalesOrder.Created.v1"` |
| `source` | URI | Event source identifier | `"/default/sap.s4.beh/S4H_PROD"` |
| `id` | String | Unique event ID | `"a1b2c3d4-..."` |

### Optional Attributes

| Attribute | Type | Description | Example |
|---|---|---|---|
| `time` | Timestamp | Event timestamp (RFC 3339) | `"2024-01-15T10:30:00Z"` |
| `datacontenttype` | String | Content type of `data` | `"application/json"` |
| `subject` | String | Event subject | `"SalesOrder/1000001234"` |
| `dataschema` | URI | Schema for `data` | URL to JSON Schema |

### SAP Event Type Naming Convention

```
sap.{product}.beh.{object}.v{ver}.{Object}.{Action}.v{ver}

Examples:
sap.s4.beh.salesorder.v1.SalesOrder.Created.v1
sap.s4.beh.salesorder.v1.SalesOrder.Changed.v1
sap.s4.beh.businesspartner.v1.BusinessPartner.Created.v1
sap.s4.beh.purchaseorder.v1.PurchaseOrder.Released.v1
```

### Parse CloudEvents in Groovy

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {
    def body = message.getBody(String.class)
    def event = new JsonSlurper().parseText(body)

    // Extract CloudEvents metadata
    def eventType = event.type          // "sap.s4.beh.salesorder.v1.SalesOrder.Created.v1"
    def eventSource = event.source      // "/default/sap.s4.beh/S4H_PROD"
    def eventId = event.id
    def eventTime = event.time

    // Extract business data
    def salesOrder = event.data?.SalesOrder
    def orderType = event.data?.SalesOrderType

    // Set properties for routing
    message.setProperty("eventType", eventType)
    message.setProperty("salesOrder", salesOrder)
    message.setProperty("eventId", eventId)

    // Set body to data payload only for downstream processing
    def dataJson = new groovy.json.JsonBuilder(event.data).toPrettyString()
    message.setBody(dataJson)

    return message
}
```

---

## Event-Driven Integration Patterns

### Pattern 1: Choreography (Decentralized)

Each service reacts to events independently. No central orchestrator.

```
S/4HANA → Event: "SalesOrder.Created"
              ↓
    ┌─────────┼───────────────┐
    ↓         ↓               ↓
  CRM       Warehouse      Finance
 (update    (reserve        (create
  pipeline)  stock)          invoice)
    ↓         ↓               ↓
  Event:    Event:          Event:
  "Lead     "Stock          "Invoice
  Updated"  Reserved"       Created"
```

**Use when:** Services are independent, no complex coordination needed, eventual consistency is acceptable.

### Pattern 2: Orchestration (Centralized)

A central orchestrator (CPI iFlow) coordinates the process.

```
S/4HANA → Event: "SalesOrder.Created"
              ↓
         CPI Orchestrator iFlow
              ↓
    Step 1: Validate Order
    Step 2: Call CRM API (update pipeline)
    Step 3: Call Warehouse API (reserve stock)
    Step 4: Call Finance API (create invoice)
    Step 5: Publish "OrderProcessed" event
              ↓
    IF any step fails → Compensating actions
```

**Use when:** Steps must execute in order, rollback needed on failure, complex business logic.

### Pattern 3: Event Sourcing

Store all events as the source of truth. Derive current state from event history.

```
Events (immutable log):
  1. SalesOrder.Created  {id: 1234, amount: 5000}
  2. SalesOrder.Changed  {id: 1234, amount: 7500}
  3. SalesOrder.ItemAdded {id: 1234, item: "P002"}
  4. SalesOrder.Released  {id: 1234}

Current State (derived):
  SalesOrder 1234: amount=7500, items=[P001, P002], status=Released
```

**Use when:** Full audit trail required, need to replay events, complex business state.

### Pattern 4: Saga (Distributed Transactions)

Long-running business process with compensating actions for rollback.

```
Order Saga:
  1. Create Order       → IF FAIL → (no compensation needed)
  2. Reserve Inventory  → IF FAIL → Cancel Order
  3. Process Payment    → IF FAIL → Release Inventory, Cancel Order
  4. Ship Order         → IF FAIL → Refund Payment, Release Inventory, Cancel Order
  5. Order Complete     → Publish "OrderFulfilled" event

Each step publishes an event. Compensating steps listen for failure events.
```

### Pattern 5: Fan-Out (Event Distribution)

One event triggers processing in multiple independent consumers.

```
S/4HANA → Event: "BusinessPartner.Created"
              ↓
         Event Mesh Topic
              ↓
    ┌─────────┼──────────────────┐
    ↓         ↓                  ↓
  Queue 1   Queue 2            Queue 3
  CRM Sync  Data Lake Ingest   Compliance Check
  (iFlow 1) (iFlow 2)         (iFlow 3)
```

**Implementation:**
- Create 3 queues, each subscribing to the same topic
- Each queue feeds a separate CPI iFlow
- Consumers process independently (failure in one doesn't affect others)

---

## SAP S/4HANA Business Events

### Enabling Business Events in S/4HANA

**Prerequisites:**
- SAP S/4HANA 2020 or later (on-premise) or S/4HANA Cloud
- SAP Event Mesh or AEM service instance on BTP
- Channel binding configured in S/4HANA

**Configuration Steps (S/4HANA Cloud):**

1. **Create Communication Arrangement** in S/4HANA Cloud
   - Communication Scenario: `SAP_COM_0892` (Enterprise Event Enablement)
   - Communication System: Your BTP subaccount
   - Outbound Topic: Select business events to publish

2. **Available Event Categories:**

| Business Object | Event Types | Topic Pattern |
|---|---|---|
| Sales Order | Created, Changed, Deleted, Released | `sap/s4/beh/salesorder/*` |
| Purchase Order | Created, Changed, Released, Deleted | `sap/s4/beh/purchaseorder/*` |
| Business Partner | Created, Changed | `sap/s4/beh/businesspartner/*` |
| Material | Created, Changed | `sap/s4/beh/product/*` |
| Invoice | Created, Changed, Cancelled | `sap/s4/beh/billingdocument/*` |
| Delivery | Created, Changed | `sap/s4/beh/outbounddelivery/*` |
| Cost Center | Created, Changed | `sap/s4/beh/costcenter/*` |
| Workforce Person | Created, Changed | `sap/s4/beh/workforce/*` |

3. **Test Event Publishing:**
   - Create a sales order in S/4HANA
   - Check Event Mesh queue for received event
   - Verify CloudEvents structure and data payload

### S/4HANA On-Premise Configuration

1. Activate Enterprise Event Enablement (transaction `/n/IWXBE/CONFIG`)
2. Create channel binding to Event Mesh
3. Maintain topic bindings for desired business objects
4. Configure outbound connection via Cloud Connector (if needed)
5. Activate event channel

---

## Configuration in Cloud Integration

### Consume Events (AMQP Adapter)

**Sender Adapter Configuration:**

```
AMQP Sender Adapter:
  Connection:
    Host: <event-mesh-host>.messaging.solace.cloud
    Port: 5671 (AMQPS)
    Proxy Type: Internet
    Authentication: OAuth 2.0
    Credential Name: EVENT_MESH_OAUTH

  Processing:
    Queue Name: order-processing
    Max Messages per Poll: 20
    Consume Mode: Auto Acknowledge

  Advanced:
    Number of Concurrent Processes: 3
    Retry Interval: 60 seconds
```

### Publish Events (AMQP Adapter)

**Receiver Adapter Configuration:**

```
AMQP Receiver Adapter:
  Connection:
    Host: <event-mesh-host>.messaging.solace.cloud
    Port: 5671 (AMQPS)
    Authentication: OAuth 2.0
    Credential Name: EVENT_MESH_OAUTH

  Processing:
    Destination Type: Topic
    Destination Name: custom/myapp/order/processed/v1

  Headers:
    Content-Type: application/json
```

### AEM Adapter (Advanced Event Mesh)

**Sender (Subscribe):**
```
AEM Adapter (Sender):
  Connection:
    Host: <aem-broker>.messaging.solace.cloud
    Port: 5671
    Message VPN: <vpn-name>
    Authentication: OAuth 2.0

  Processing:
    Operation: Subscribe
    Queue Name: order-processing
    Acknowledgment: Auto / Manual
    Max Messages: 20
```

**Receiver (Publish):**
```
AEM Adapter (Receiver):
  Connection:
    Host: <aem-broker>.messaging.solace.cloud
    Port: 5671
    Message VPN: <vpn-name>

  Processing:
    Operation: Publish
    Topic: custom/myapp/order/processed/v1
    Delivery Mode: Persistent
```

### Event Consumer iFlow Pattern

```
[AEM/AMQP Sender: Subscribe Queue] → [Content Modifier: Extract CloudEvents Metadata]
         ↓
    [Groovy: Parse CloudEvents Envelope]
         ↓
    [Router: By Event Type]
         ├─ (SalesOrder.Created) → [Process New Order] → [HTTP: Call Target API]
         ├─ (SalesOrder.Changed) → [Process Change]    → [HTTP: Update Target]
         └─ (Default)            → [Log Unknown Event]  → [End]

    Exception Subprocess:
         [Log Error] → [Content Modifier: Error Details] → [AMQP: Publish to DLT]
```

---

## Dead Letter Topics and Error Handling

### Dead Letter Queue/Topic (DLQ/DLT) Pattern

When a message fails processing after retries, move it to a dead letter destination for manual investigation.

```
Main Queue: "order-processing"
  ↓ (consume)
CPI iFlow: Process Order
  ↓ (if fails after N retries)
Dead Letter Topic: "dlq/order-processing/failed"
  ↓ (subscribe)
Dead Letter Queue: "dlq-order-processing"
  ↓ (manual investigation)
Operations Team: Review, Fix, Reprocess
```

### Retry Strategy Configuration

```
iFlow Exception Subprocess:
  1. Increment retry counter (Exchange Property)
  2. IF retryCount < 3:
     - Wait (Timer: exponential backoff)
     - Re-publish to original topic (retry)
  3. IF retryCount >= 3:
     - Publish to Dead Letter Topic with error details
     - Log error with full context
     - Send alert notification
```

### Groovy: Implement Retry with Exponential Backoff

```groovy
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def retryCount = message.getProperty("retryCount")?.toInteger() ?: 0
    retryCount++

    // Exponential backoff: 2^retry * 1000ms (2s, 4s, 8s)
    def backoffMs = Math.pow(2, retryCount) * 1000
    def maxBackoff = 30000 // 30 seconds max
    backoffMs = Math.min(backoffMs, maxBackoff)

    message.setProperty("retryCount", retryCount.toString())
    message.setProperty("backoffMs", backoffMs.toString())
    message.setProperty("maxRetries", "3")

    def shouldRetry = retryCount <= 3
    message.setProperty("shouldRetry", shouldRetry.toString())

    return message
}
```

### Dead Letter Message Enrichment

When publishing to DLT, include diagnostic context:

```groovy
import groovy.json.JsonBuilder

def Message processData(Message message) {
    def originalBody = message.getBody(String.class)
    def errorMessage = message.getProperty("CamelExceptionCaught")?.toString()

    def dlqMessage = [
        originalPayload: originalBody,
        error: [
            message: errorMessage,
            timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            iFlowName: message.getProperty("SAP_MplCorrelationId"),
            retryCount: message.getProperty("retryCount") ?: "0"
        ],
        metadata: [
            originalTopic: message.getHeader("JMSDestination"),
            originalMessageId: message.getHeader("SAP_MessageProcessingLogID")
        ]
    ]

    message.setBody(new JsonBuilder(dlqMessage).toPrettyString())
    return message
}
```

---

## Webhooks

### Webhook Receiver in Cloud Integration

Expose an HTTP endpoint in CPI that receives webhook notifications from external systems.

**iFlow Pattern:**
```
[HTTP Sender: /webhook/github] → [Groovy: Verify HMAC Signature]
         ↓
    [Router: By Event Type Header]
         ├─ (push) → [Process Push Event]
         ├─ (pull_request) → [Process PR Event]
         └─ (Default) → [Log and Acknowledge]

    Response: HTTP 200 OK (acknowledge receipt)
```

### Webhook Signature Verification (HMAC-SHA256)

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

def Message processData(Message message) {
    def body = message.getBody(String.class)
    def signatureHeader = message.getHeader("X-Hub-Signature-256", String.class)

    if (!signatureHeader) {
        throw new Exception("Missing signature header")
    }

    // Get webhook secret from Secure Store
    def service = com.sap.it.api.ITApiFactory.getService(
        com.sap.it.api.securestore.SecureStoreService.class, null)
    def credential = service.getUserCredential("WEBHOOK_SECRET")
    def secret = new String(credential.getPassword())

    // Calculate HMAC-SHA256
    def mac = Mac.getInstance("HmacSHA256")
    mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"))
    def calculatedHash = mac.doFinal(body.getBytes("UTF-8"))
    def calculatedSignature = "sha256=" + calculatedHash.encodeHex().toString()

    // Constant-time comparison to prevent timing attacks
    if (!MessageDigest.isEqual(
        calculatedSignature.getBytes(), signatureHeader.getBytes())) {
        throw new Exception("Invalid webhook signature")
    }

    return message
}
```

### Webhook Sender from Cloud Integration

Publish events to external webhook endpoints.

```
[Event Source iFlow] → [Content Modifier: Set Webhook Headers]
         ↓
    [Groovy: Generate HMAC Signature]
         ↓
    [HTTP Receiver: POST to webhook URL]

    Headers:
      Content-Type: application/json
      X-Webhook-Signature: sha256={hmac}
      X-Event-Type: order.created
      X-Delivery-ID: {uuid}
```

---

## Event Enrichment and Filtering

### Event Enrichment Pattern

Enrich lightweight events with additional data before consumer processing.

```
[Event: "SalesOrder.Created" {SalesOrder: "1234"}]
         ↓
    [CPI Enrichment iFlow]
         ↓
    [Request Reply: GET /API_SALES_ORDER_SRV/A_SalesOrder('1234')]
         ↓
    [Groovy: Merge event + order details]
         ↓
    [Publish Enriched Event to "enriched/salesorder/created"]
         ↓
    Enriched payload: {SalesOrder: "1234", Customer: "ACME", Amount: 5000, Items: [...]}
```

### Content-Based Filtering

Filter events at subscription level to reduce unnecessary processing.

**Topic Wildcard Filtering:**
```
Queue A subscribes to: sap/s4hana/salesorder/created/v1
  → Only receives new sales orders

Queue B subscribes to: sap/s4hana/salesorder/*/v1
  → Receives all sales order events (created, changed, deleted)

Queue C subscribes to: sap/s4hana/*/created/v1
  → Receives all creation events across all business objects
```

**CPI Router-Based Filtering:**
```groovy
// Filter by business criteria after consuming from queue
def json = new JsonSlurper().parseText(body)
def amount = json.data?.TotalNetAmount?.toDouble() ?: 0

// Only process high-value orders
if (amount >= 10000) {
    message.setProperty("route", "process")
} else {
    message.setProperty("route", "skip")
}
```

---

## EOIO with Event Mesh

### Exactly-Once In Order Processing

Use **Exclusive Queues** for guaranteed ordering per business entity.

**Configuration:**
```
Queue Configuration:
  Queue Name: salesorder-eoio
  Access Type: Exclusive (single consumer)
  Max Redelivery Count: 3
```

### Partitioned Queues (AEM)

For parallel processing while maintaining order per partition key:

```
Partitioned Queue:
  Queue Name: salesorder-partitioned
  Partition Count: 4
  Partition Key: ${header.SalesOrder}

  Result:
    - Orders for SalesOrder 1001 → always Partition 0
    - Orders for SalesOrder 1002 → always Partition 1
    - Different orders processed in parallel
    - Same order always processed in sequence
```

### Idempotent Event Processing

```groovy
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def body = message.getBody(String.class)
    def event = new groovy.json.JsonSlurper().parseText(body)

    // Use CloudEvents 'id' for idempotency
    def eventId = event.id

    // Check Data Store for duplicate
    // (Use Data Store Get step before this script, result in property)
    def existingEntry = message.getProperty("existingEntry")

    if (existingEntry != null) {
        // Duplicate event - skip processing
        message.setProperty("isDuplicate", "true")
        def log = messageLogFactory.getMessageLog(message)
        log.addAttachmentAsString("DuplicateEvent",
            "Event ${eventId} already processed", "text/plain")
    } else {
        message.setProperty("isDuplicate", "false")
    }

    return message
}
```

---

## Event Mesh Administration

### Queue Management

**Navigation:** SAP BTP Cockpit → Event Mesh → Manage Queues

**Operations:**
| Operation | Description |
|---|---|
| Create Queue | Define name, access type, subscriptions |
| Delete Queue | Remove queue and all pending messages |
| Purge Queue | Delete all messages, keep queue |
| View Messages | Inspect pending messages in queue |
| Monitor Depth | Track queue size over time |

### Topic Subscription Management

```
Queue: "order-processing"
  Subscriptions:
    + sap/s4hana/salesorder/created/v1     ✅ Active
    + sap/s4hana/salesorder/changed/v1     ✅ Active
    + sap/s4hana/salesorder/deleted/v1     ❌ Inactive (optional)
    + custom/myapp/order/enriched/v1       ✅ Active
```

### Service Key Configuration

**Create service key for CPI connection:**

```json
{
  "management": [
    {
      "oa2": {
        "clientid": "<client-id>",
        "clientsecret": "<client-secret>",
        "tokenendpoint": "https://<auth-host>/oauth/token",
        "granttype": "client_credentials"
      },
      "uri": "https://<management-host>"
    }
  ],
  "messaging": [
    {
      "oa2": {
        "clientid": "<client-id>",
        "clientsecret": "<client-secret>",
        "tokenendpoint": "https://<auth-host>/oauth/token",
        "granttype": "client_credentials"
      },
      "protocol": ["amqp10ws"],
      "broker": {
        "type": "sapmgw"
      },
      "uri": "wss://<messaging-host>:443"
    }
  ]
}
```

### Monitoring Metrics

| Metric | Alert Threshold | Action |
|---|---|---|
| Queue Depth | > 1,000 messages | Scale consumers, check processing speed |
| Message Age | > 5 minutes | Consumer may be stuck, check iFlow |
| Dead Letter Count | > 0 | Investigate failed messages immediately |
| Connection Count | = 0 | Consumer disconnected, check iFlow deployment |
| Publish Rate | Unusual spike | Verify source system behavior |

---

## Best Practices

### Design

1. **Use CloudEvents format** for all custom events — ensures interoperability
2. **Design topic hierarchy carefully** — changing topics after go-live is disruptive
3. **Keep events small** — include keys/IDs, not full payloads; let consumers enrich
4. **One event type per topic** — don't mix different event types in same topic
5. **Version events** — include version in topic name and event type

### Reliability

1. **Always use durable queues** — prevent message loss during consumer downtime
2. **Implement idempotent consumers** — duplicate delivery is possible
3. **Configure dead letter topics** — never silently drop failed messages
4. **Set max redelivery count** — prevent infinite retry loops (3-5 retries typical)
5. **Monitor queue depth** — alert before queues grow too large

### Performance

1. **Use non-exclusive queues for parallel processing** — scale with multiple consumers
2. **Keep event payloads under 1MB** — broker performance degrades with large messages
3. **Batch where possible** — use splitter/aggregator patterns for bulk operations
4. **Set appropriate polling intervals** — balance latency vs resource consumption
5. **Use topic wildcards wisely** — overly broad subscriptions increase load

### Security

1. **Use OAuth 2.0** for all Event Mesh connections — never basic auth in production
2. **Encrypt in transit** — always use AMQPS (port 5671), never AMQP (5672)
3. **Verify webhook signatures** — prevent spoofed events
4. **Implement RBAC** — separate publish/subscribe permissions per service
5. **Don't include PII in topic names** — topics may be visible in monitoring

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for event consumer iFlow patterns
- See [security.md](security.md) for OAuth setup with Event Mesh
- See [troubleshooting.md](troubleshooting.md) for event processing error resolution
- See [adapters.md](adapters.md) for AMQP and AEM adapter configuration details

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about event-driven architectures, Event Mesh configuration, CloudEvents, and AMQP/Kafka integration patterns.
