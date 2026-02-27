# Simulation & Testing Reference

Design-time iFlow simulation, Trace mode debugging, and testing strategies for SAP Cloud Integration.

## Table of Contents

- [Overview](#overview)
- [iFlow Simulation](#iflow-simulation)
- [Trace Mode](#trace-mode)
- [Simulation vs Trace vs Postman](#simulation-vs-trace-vs-postman)
- [Testing Strategies](#testing-strategies)
- [Best Practices](#best-practices)

---

## Overview

SAP Cloud Integration provides multiple testing approaches at different stages of development:

| Stage | Tool | Requires Deployment? | Real Backend Calls? |
|---|---|---|---|
| **Design-time** | iFlow Simulation | ❌ No | ❌ No (mocked) |
| **Runtime debug** | Trace Mode | ✅ Yes | ✅ Yes |
| **External test** | Postman / API Client | ✅ Yes | ✅ Yes |
| **Automated** | CI/CD + OData API | ✅ Yes | ✅ Yes |

---

## iFlow Simulation

### What is Simulation?

Simulation allows you to test message processing logic **without deploying** the iFlow. You provide a sample input message and see how it flows through each step, viewing the message body, headers, and properties at every stage.

### Starting Simulation

**Navigation:** Design → Select iFlow → Edit → Click **Simulate** button (play icon in toolbar)

**Steps:**
1. Open iFlow in Edit mode
2. Click the **Simulate** button in the top toolbar
3. The simulation panel opens on the right side
4. Click on the **Start Event** or **Sender** to set the simulation input point
5. Click on any step to set the **End Point** (where to stop simulation)
6. Provide input:

### Providing Test Input

**Input options at the Start Point:**

| Input Type | How to Provide |
|---|---|
| **Message Body** | Paste XML/JSON/text content or upload a file |
| **Message Headers** | Add key-value pairs (e.g., `Content-Type: application/json`) |
| **Exchange Properties** | Add key-value pairs (e.g., `orderId: 12345`) |

**Example input for a JSON iFlow:**
```json
{
  "orderId": "1000001234",
  "orderType": "OR",
  "customer": "ACME Corp",
  "items": [
    {"productId": "P001", "quantity": 10, "price": 25.50},
    {"productId": "P002", "quantity": 5, "price": 100.00}
  ]
}
```

**Headers to set:**
```
Content-Type: application/json
SAP_ApplicationID: SIM_TEST_001
```

### Viewing Simulation Results

After running simulation, click on any step to view the message state at that point:

```
Simulation Flow:
  [Start] → Message body = original input
       ↓
  [Content Modifier] → View: body modified, new headers added
       ↓
  [Groovy Script] → View: body transformed, properties set
       ↓
  [Router] → View: which route was taken and why
       ↓
  [Mapping] → View: mapped output structure
       ↓
  [End] → View: final message body, headers, properties
```

**At each step you can inspect:**
- **Message Body** — full content with syntax highlighting
- **Headers** — all headers with values
- **Properties** — all exchange properties
- **Errors** — exception details if step fails

### Supported Steps in Simulation

| Step Type | Simulated? | Notes |
|---|---|---|
| Content Modifier | ✅ Yes | Full support |
| Groovy/JS Script | ✅ Yes | Executes script logic |
| Message Mapping | ✅ Yes | Full mapping execution |
| XSLT Mapping | ✅ Yes | Full transformation |
| Router (CBR) | ✅ Yes | Evaluates conditions |
| Filter | ✅ Yes | Applies filter logic |
| Splitter | ✅ Yes | Splits message |
| Converter (CSV↔XML, JSON↔XML) | ✅ Yes | Format conversion |
| Encryptor / Decryptor | ⚠️ Partial | Requires keystore access |
| **Request Reply** | ❌ No | Cannot call external systems |
| **Send** | ❌ No | Cannot call external systems |
| **HTTP/OData/SOAP Adapter** | ❌ No | No real network calls |
| **Data Store Operations** | ❌ No | No runtime persistence |
| **JMS Send/Receive** | ❌ No | No queue access |
| **Poll Enrich** | ❌ No | Cannot poll external sources |

### Simulation Limitations

1. **No external calls** — Request Reply, Send, and adapter calls are not executed
2. **No Data Store** — Write/Read/Delete operations are skipped
3. **No JMS queues** — Queue operations are not available
4. **No timer-based triggers** — Timer start events cannot be simulated (use message start)
5. **No Secure Store access** — `SecureStoreService` calls in scripts will fail
6. **Script limitations** — Scripts that depend on runtime-only APIs may fail
7. **No ProcessDirect** — Cross-iFlow calls are not executed

### Workaround for External Calls

For steps that require external data, use **mock data** in the simulation:

1. **Before the external call step:** Add a Content Modifier that sets the message body to a sample response
2. **Set the simulation end point** before the external call
3. **Start a new simulation segment** after the call, providing the expected response as input

---

## Trace Mode

### What is Trace Mode?

Trace mode captures **full message processing details** at runtime on a deployed iFlow. Unlike simulation, it executes all steps including external calls, but it requires the iFlow to be deployed and a real message to be sent.

### Enabling Trace

**Navigation:** Monitor → Manage Integration Content → Select deployed iFlow → Log Configuration

**Steps:**
1. Navigate to Monitor → Integrations and APIs
2. Find the deployed iFlow under **Manage Integration Content**
3. Click on the iFlow → **Log Configuration**
4. Set Log Level to **Trace**
5. Click **Save**
6. Send a real test message
7. View trace results in Message Processing Log (MPL)

### Viewing Trace Results

**Navigation:** Monitor → Integrations and APIs → All → Filter by iFlow name → Click message → **Trace** tab

**Trace shows:**
- Complete message flow through all steps
- Message body before/after each step
- Headers and properties at each step
- Processing time per step
- External call request/response payloads
- Error details with stack traces

### Trace Limitations

| Limitation | Details |
|---|---|
| **Auto-disable** | Trace mode auto-disables after **10 minutes** |
| **Performance impact** | Increases processing time and storage (captures all payloads) |
| **Security risk** | Captures full payloads including sensitive data |
| **Storage** | Trace data counts against MPL retention (30 days) |
| **Not for production** | Never leave Trace enabled in production |

---

## Simulation vs Trace vs Postman

| Aspect | Simulation | Trace | Postman |
|---|---|---|---|
| **Deployment needed** | ❌ No | ✅ Yes | ✅ Yes |
| **External calls** | ❌ Mocked | ✅ Real | ✅ Real |
| **Speed** | ⚡ Instant | 🕐 Real-time | 🕐 Real-time |
| **Data Store** | ❌ Not available | ✅ Available | ✅ Available |
| **See step-by-step** | ✅ Every step | ✅ Every step | ❌ Only final response |
| **Sensitive data risk** | 🟢 Low (local) | 🔴 High (logged) | 🟡 Medium |
| **Best for** | Logic validation | Full debugging | End-to-end testing |
| **Use phase** | Development | Testing/Debug | QA/Integration test |

### Recommended Testing Flow

```
1. DEVELOP → Use Simulation to validate mapping/routing logic
2. DEPLOY to DEV → Use Trace for full end-to-end debugging
3. TEST → Use Postman for structured API testing with assertions
4. PROMOTE → Use CI/CD for automated regression testing
```

---

## Testing Strategies

### Unit Testing (Design-Time)

Use simulation to test individual transformation logic:
1. Create sample input messages for happy path
2. Create sample input messages for edge cases (empty fields, special characters, large payloads)
3. Run simulation for each test case
4. Verify output matches expected results

### Integration Testing (Runtime)

Use Trace mode + Postman to test full flows:
1. Deploy to DEV/QA environment
2. Enable Trace mode
3. Send test messages via Postman
4. Verify end-to-end processing
5. Check target system received correct data
6. Disable Trace mode when done

### Regression Testing (Automated)

Use CI/CD + OData API for automated testing after each deployment:
1. Deploy iFlow via CI/CD pipeline
2. Send predefined test payloads via API
3. Query MPL via OData API to verify completion status
4. Compare responses against expected baselines
5. Alert on failures

### Error Scenario Testing

Always test these error scenarios:
- Invalid input format (malformed JSON/XML)
- Missing required fields
- Authentication failures (wrong credentials)
- Backend system unavailable (timeout)
- Payload exceeding size limits
- Duplicate message detection (idempotency)

---

## Best Practices

### Simulation

1. **Simulate before deploying** — catch logic errors early, save deployment time
2. **Create reusable test data** — maintain a library of sample payloads for common scenarios
3. **Test boundary conditions** — empty arrays, null values, special characters, maximum lengths
4. **Use simulation segments** — break complex flows into testable segments around external calls

### Trace Mode

1. **Never leave Trace on in production** — auto-disables after 10 min, but always verify
2. **Use sparingly** — Trace significantly increases message size and processing time
3. **Capture traces quickly** — 10-minute window means you need to send test messages promptly
4. **Sanitize before sharing** — Trace captures may contain passwords, tokens, or PII

### General Testing

1. **Test with realistic data** — use production-like payloads (anonymized)
2. **Document test cases** — maintain test catalog with expected inputs/outputs
3. **Automate regression tests** — prevent regressions when iFlows are updated
4. **Test at expected volume** — single-message testing doesn't catch concurrency issues

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for iFlow design patterns to test
- See [postman-testing.md](postman-testing.md) for Postman collection generation
- See [troubleshooting.md](troubleshooting.md) for debugging failed messages
- See [operations-monitoring.md](operations-monitoring.md) for MPL monitoring

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about iFlow simulation, design-time testing, and integration flow debugging techniques.
