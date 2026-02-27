# Troubleshooting Reference

Common errors, diagnostics, and resolution strategies for SAP Cloud Integration.

## Diagnostic Approach

### Step-by-Step Troubleshooting

1. **Check Message Processing Log (MPL)**
   - Navigate to Monitor → Integrations and APIs → All Integration Flows
   - Find the failed message
   - Review error message and stack trace

2. **Enable Trace Mode**
   - Open iFlow in edit mode
   - Click Trace
   - Deploy and reprocess message
   - Review payload at each step

3. **Check Adapter Configuration**
   - Verify endpoint URLs
   - Check authentication credentials
   - Confirm adapter-specific settings

4. **Review System Logs**
   - Check for system-wide issues
   - Review tenant health

5. **Test Connectivity**
   - Use Connectivity Tests (Monitor → Manage Integration Content → Connectivity Tests)

---

## Common Errors and Solutions

### ⚠️ Silent Configuration Errors (No Error Shown)

These are the most dangerous errors because CPI does NOT throw any exception. The iFlow runs successfully with HTTP 200 but produces incorrect results.

#### Headers Silently Dropped (Allowed Headers Missing)

**Symptom:** iFlow returns 200 OK but filters/parameters are ignored. Groovy script receives `null` for header values.

**Cause:** The header name is NOT listed in **Allowed Header(s)** in the Integration Flow Runtime Configuration. CPI silently strips any headers not explicitly whitelisted.

**Resolution:**
1. Click the **white background** of the Integration Process (not any step)
2. Go to **Runtime Configuration** tab
3. In **Allowed Header(s)**, add the missing header name separated by `|`
4. Example: `FilterRoutingNumber|FilterOrderType|FilterOrderNumber|APIKey|Accept|Content-Type`
5. Save → **Deploy** (this requires redeployment, not just save)

**Prevention:** Always document the complete Allowed Headers list when designing an iFlow. When adding new filter/parameter headers, update Allowed Headers AND Content Modifier AND Script simultaneously.

#### Exchange Property Not Mapped in Content Modifier

**Symptom:** Header is in Allowed Headers, but the Groovy script still receives `null` for the property.

**Cause:** The Content Modifier does NOT have an Exchange Property row that reads the header into a property. The Groovy script reads from `message.getProperties()`, not directly from headers.

**Resolution:**
1. Open the Content Modifier step
2. Go to **Exchange Property** tab
3. Add row: Action=Create, Name=`propertyName`, Source Type=**Header**, Source Value=`HeaderName`
4. Save → Deploy

**Common mistake chain:**
```
✅ Allowed Headers:    FilterOrderNumber  ← header arrives at iFlow
❌ Content Modifier:   (no Exchange Property row for filterOrderNumber) ← NOT mapped to property
❌ Groovy Script:      properties.get("filterOrderNumber") → null ← script gets nothing
```

**Correct chain:**
```
✅ Allowed Headers:    FilterOrderNumber  ← header arrives at iFlow
✅ Content Modifier:   Exchange Property: filterOrderNumber ← Source: Header "FilterOrderNumber"
✅ Groovy Script:      properties.get("filterOrderNumber") → "4000003" ← works!
```

#### Request Headers Not Forwarded to Receiver

**Symptom:** iFlow calls receiver API successfully but the receiver returns 401/403 because required headers (like APIKey) are not forwarded.

**Cause:** HTTP adapter's **Request Headers** field does not list the headers to forward.

**Resolution:**
1. Open the HTTP receiver adapter configuration
2. In **Request Headers** field, add pipe-separated header names: `APIKey|Accept`
3. Save → Deploy

---

### HTTP Adapter Errors

#### 403 Forbidden on SAP API Business Hub Sandbox

**Cause:** APIKey expired, header name incorrect, or session invalidated.

**Resolution:**
1. Verify the header is exactly `APIKey` (capital A and K) — NOT `apikey`, `Api-Key`, or `api_key`
2. Go to [api.sap.com](https://api.sap.com) → Login → Settings (⚙️ icon) → **Show API Key**
3. If expired, click **Regenerate** and copy the new key
4. Verify the header value has no leading/trailing whitespace
5. Testing directly in browser without headers will always return:
   ```json
   {"fault":{"faultstring":"Failed to resolve API Key variable request.header.apikey"}}
   ```
   This is expected — the browser doesn't send the `APIKey` header. Always test from Postman/curl.

### iFlow Artifact Upload Errors

#### "Error while loading the details of the integration flow"

**Cause:** The uploaded `.zip` has an incorrect BPMN2 structure, missing mandatory XML elements, or wrong META-INF/MANIFEST.MF format.

**Resolution:** Do NOT attempt to fix the ZIP. Delete the corrupted artifact and **recreate the iFlow manually in the Web UI**. See the [iFlow Artifact Generation Policy](cloud-integration.md#️-iflow-artifact-generation-policy).

#### "Unable to render property sheet. Metadata not available or not registered"

**Cause:** The `.iflw` BPMN2 XML contains `cmdVariantUri` properties with adapter/step component versions that don't match the versions registered on the tenant.

**Example of a cmdVariantUri:**
```
ctype::AdapterVariant/cname::sap:HTTPS/tp::HTTPS/mp::None/direction::Sender/version::1.3.0
```
The `version::1.3.0` part must match the exact version deployed on the tenant, which varies by region and update cycle.

**Resolution:** Delete the artifact and recreate manually. There is no way to fix `cmdVariantUri` versions without knowing the tenant's registered component catalog.

#### 401 Unauthorized
**Cause:** Invalid credentials or expired token

**Resolution:**
- Verify User Credentials artifact
- Check OAuth2 token expiration
- Confirm correct authentication type in adapter
- Test credentials manually (Postman/curl)

#### 404 Not Found
**Cause:** Incorrect URL or endpoint

**Resolution:**
- Verify receiver URL (check for typos)
- Confirm API endpoint exists
- Check if endpoint requires specific path parameters

#### 500 Internal Server Error
**Cause:** Backend system error

**Resolution:**
- Review backend system logs
- Check payload format (JSON/XML validity)
- Verify required headers (Content-Type, Accept)
- Contact backend system administrator

#### Connection Timeout
**Cause:** Slow response or network issue

**Resolution:**
- Increase adapter timeout (default 60s)
- Check network connectivity
- Verify firewall rules
- Consider asynchronous pattern for long-running operations

### SOAP Adapter Errors

#### SOAP Fault: Invalid XML
**Cause:** Malformed SOAP envelope

**Resolution:**
- Validate XML against WSDL
- Check namespace declarations
- Ensure proper SOAP structure (Envelope > Header > Body)

#### WS-Security Error
**Cause:** Invalid signature or certificate issue

**Resolution:**
- Verify certificate in Keystore
- Check certificate validity period
- Confirm correct signing/encryption algorithm
- Ensure partner's public key is imported

### SFTP Adapter Errors

#### Authentication Failed
**Cause:** Invalid SSH credentials or key

**Resolution:**
- Verify username/password in User Credentials
- Check SSH public key (if using key-based auth)
- Confirm user has access to target directory

#### Permission Denied
**Cause:** Insufficient file system permissions

**Resolution:**
- Verify user has read/write permissions on directory
- Check file ownership
- Confirm directory exists

#### File Already Exists
**Cause:** File name conflict

**Resolution:**
- Enable "Override" option in adapter
- Use dynamic file names with timestamp: `file_${date:now:yyyyMMddHHmmss}.xml`
- Implement file archival strategy

### JMS Adapter Errors

#### Queue Full
**Cause:** Message backlog exceeds queue capacity

**Resolution:**
- Increase consumer instances (Concurrent Consumers)
- Reduce message size
- Implement message expiration (TTL)
- Monitor queue depth regularly

#### Message Not Found (for EOIO)
**Cause:** Message already processed or expired

**Resolution:**
- Check Data Store for duplicate entry
- Verify message expiration settings
- Review correlation ID

### JDBC Adapter Errors

#### SQL Exception: Connection Failed
**Cause:** Database connectivity issue

**Resolution:**
- Verify JDBC URL format
- Check database credentials
- Confirm database is reachable (firewall, Cloud Connector)
- Test connection from Cloud Connector (if on-premise)

#### SQL Exception: Invalid Query
**Cause:** Syntax error in SQL

**Resolution:**
- Validate SQL syntax
- Check table/column names (case-sensitive?)
- Use parameterized queries (avoid SQL injection)
- Test query in database client

### Scripting Errors

#### GroovyScriptException: Cannot get property
**Cause:** Accessing null or non-existent property

**Resolution:**
```groovy
// ❌ Bad
def value = json.order.items.item[0].price

// ✅ Good - Check null
def value = json?.order?.items?.item?.getAt(0)?.price ?: "0.00"
```

#### OutOfMemoryError
**Cause:** Large payload loaded into memory

**Resolution:**
- Use streaming parsers (XmlSlurper, not XmlParser)
- Avoid loading full payload in script
- Split large files before processing

#### ClassCastException
**Cause:** Incorrect type conversion

**Resolution:**
```groovy
// ❌ Bad
def body = message.getBody()

// ✅ Good - Specify type
def body = message.getBody(String.class)
```

### Mapping Errors

#### XPath Error: Invalid expression
**Cause:** Incorrect XPath syntax or namespace

**Resolution:**
- Validate XPath expression
- Include namespace prefixes
- Test XPath in online validator

#### JSON Path Error
**Cause:** Invalid JSONPath expression

**Resolution:**
- Use correct JSONPath syntax: `$.order.items[0].price`
- Test in JSONPath evaluator
- Check for typos in property names

### Data Store Errors

#### Entry Not Found
**Cause:** Entry ID doesn't exist in Data Store

**Resolution:**
- Check Entry ID value (exact match required)
- Verify Data Store name (case-sensitive)
- Check if entry expired (TTL)

#### Data Store Full
**Cause:** 100MB limit exceeded

**Resolution:**
- Delete old entries
- Reduce entry size
- Implement cleanup strategy (automated deletion)

---

## HTTP Error Code Reference

| Code | Meaning | Common Causes |
|---|---|---|
| 400 | Bad Request | Invalid JSON/XML, missing required field |
| 401 | Unauthorized | Invalid credentials, expired token |
| 403 | Forbidden | Valid credentials but insufficient permissions |
| 404 | Not Found | Wrong URL, resource doesn't exist |
| 405 | Method Not Allowed | Using POST when GET required (or vice versa) |
| 408 | Request Timeout | Slow backend, network latency |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Backend application error |
| 502 | Bad Gateway | Proxy/gateway error (Cloud Connector?) |
| 503 | Service Unavailable | Backend system down or overloaded |
| 504 | Gateway Timeout | Backend didn't respond in time |

---

## Performance Issues

### Symptom: Slow Message Processing
**Possible Causes:**
- Large payload (>10MB)
- Complex mappings
- Multiple external calls
- Inefficient scripts

**Resolution:**
- Use streaming for large files
- Optimize mappings (reduce unnecessary transformations)
- Implement parallel processing (Splitter)
- Profile script performance

### Symptom: High Memory Usage
**Possible Causes:**
- Loading large files in memory
- Memory leaks in scripts
- Too many concurrent messages

**Resolution:**
- Use SAX parser for XML (not DOM)
- Release resources in scripts (close connections)
- Adjust thread pool size
- Split large files before processing

---

## Debugging Techniques

### 1. Use Trace Mode
**Steps:**
1. Edit iFlow
2. Click Trace
3. Deploy
4. Trigger iFlow
5. Review trace (see payload at each step)

**Best Practices:**
- Disable trace in production (performance impact)
- Use for specific troubleshooting only
- Clear trace data after debugging

### 2. Log Payload in Script
```groovy
def messageLog = messageLogFactory.getMessageLog(message)
def body = message.getBody(String.class)
messageLog.addAttachmentAsString("Payload", body, "application/json")
```

### 3. Add Content Modifier for Debugging
```
Content Modifier:
- Add Property: debug_checkpoint = "Step 3 completed"
```
View in MPL properties.

### 4. Use Postman/curl for Adapter Testing
```bash
# Test HTTP endpoint
curl -X POST https://your-tenant.it-cpitrial.cfapps.us10.hana.ondemand.com/http/test \
  -H "Content-Type: application/json" \
  -d '{"orderId": "12345"}'
```

---

## Preventive Measures

### 1. Implement Proper Error Handling
- Always add Exception Subprocess
- Log errors with context
- Return meaningful error messages
- Set up alerts for critical errors

### 2. Validate Input
- Validate JSON/XML structure
- Check required fields
- Validate data types
- Use Schema Validation step

### 3. Monitor Proactively
- Set up alerts (SAP Alert Notification Service)
- Review Message Processing Logs daily
- Monitor queue depths (JMS)
- Track Data Store usage

### 4. Test Thoroughly
- Test with real payload samples
- Test error scenarios (invalid data, timeouts)
- Load testing for high-volume scenarios
- Test at expected message frequency

---

## When to Contact SAP Support

**Create Incident When:**
- Tenant performance degradation
- Adapter malfunction (not configuration error)
- Unexpected behavior after SAP update
- Critical production issue

**Before Contacting Support:**
1. Gather MPL logs
2. Collect error messages and stack traces
3. Document steps to reproduce
4. Check SAP Community for similar issues
5. Review SAP Notes (if you have S-user access)

---

## Cloud Connector Errors

| Error | Cause | Resolution |
|---|---|---|
| `"Could not establish connection to Cloud Connector"` | Tunnel disconnected, CC not running | Check CC admin UI → Subaccount status; verify outbound port 443 |
| `"Connection refused (backend)"` | Backend system down or wrong host:port | Check internal host:port in CC system mapping; verify backend is running |
| `"403 Forbidden (access control)"` | Resource path not in CC access control list | Add URL path or RFC function to CC access control → Resources |
| `"Host not reachable via Cloud Connector"` | DNS issue or network block between CC and backend | Check DNS resolution from CC server; verify internal firewall rules |
| `"Certificate expired (Cloud Connector)"` | CC system certificate or backend certificate expired | Renew certificate in CC admin → Configuration → Certificates |
| `"Location ID not found"` | Wrong Location ID in CPI adapter config | Verify Location ID matches CC admin → Subaccount → Location ID |
| `"Connection reset by peer"` | Firewall/proxy between CC and BTP drops idle connections | Configure keep-alive; check corporate proxy settings |
| `"Handshake failure (TLS)"` | TLS version mismatch between CC and backend | Update TLS settings; ensure both support TLS 1.2+ |

**Diagnostic sequence:** CC Admin → Check tunnel status → Check system mapping → Check access control → Check backend availability → Review audit log

See [cloud-connector.md](cloud-connector.md) for detailed Cloud Connector configuration and troubleshooting.

---

## Edge Integration Cell Errors

| Error | Cause | Resolution |
|---|---|---|
| `Pod CrashLoopBackOff` | Insufficient memory, config error, image pull failure | `kubectl describe pod <name>` → check events; increase resource limits |
| `Helm install failure` | RBAC insufficient, wrong namespace, image not accessible | Check Helm output; verify K8s admin access; check image registry connectivity |
| `"Edge LM Bridge connection failure"` | Tunnel to BTP cannot be established | Verify outbound HTTPS to `*.hana.ondemand.com:443`; check proxy settings |
| `"Certificate synchronization failed"` | Cloud keystore not syncing to edge | Check Edge LM Bridge logs; verify BTP connectivity; re-trigger sync |
| `"iFlow deployment to edge failed"` | Adapter not supported on edge or resource exhaustion | Check adapter compatibility matrix; verify cluster resource availability |
| `ImagePullBackOff` | Container registry not accessible | Check network policy; verify image pull secrets; check proxy/firewall |
| `Ingress 503 Service Unavailable` | Backend pod not ready or service misconfigured | `kubectl get pods` → check readiness; verify ingress controller config |
| `PersistentVolumeClaim pending` | Storage class not available or insufficient capacity | Check storage class; verify PV availability; check disk quota |

**Diagnostic commands:**
```bash
kubectl get pods -n edge-integration-cell          # Check pod status
kubectl logs <pod-name> -n edge-integration-cell    # View pod logs
kubectl describe pod <pod-name> -n edge-integration-cell  # Detailed status
kubectl get events -n edge-integration-cell --sort-by='.lastTimestamp'  # Recent events
```

See [edge-integration-cell.md](edge-integration-cell.md) for Edge deployment configuration.

---

## API Management Errors

| Error | Cause | Resolution |
|---|---|---|
| `401 Unauthorized (API proxy)` | Invalid API key, expired OAuth token | Check API key validity; verify OAuth token endpoint; check credential name |
| `429 Too Many Requests` | Quota or Spike Arrest limit exceeded | Review quota policy settings; check Spike Arrest rate; increase limits if needed |
| `500 from target endpoint` | Backend (CPI iFlow or system) error | Check CPI MPL for iFlow errors; verify target endpoint URL; check backend health |
| `"Policy execution failed"` | Error in JavaScript/Python policy or Service Callout | Use Trace tool → identify failing policy → check policy configuration |
| `"Key not found"` | API key not registered or product subscription inactive | Verify key in Developer Portal → Applications; check product subscription status |
| `"API proxy deployment failed"` | Configuration error in proxy or policy XML | Review proxy configuration; validate policy XML; check for syntax errors |
| `"Service Callout timeout"` | External service too slow to respond | Increase timeout in Service Callout policy; check external service health |
| `"CORS error"` | Missing CORS headers in response | Add AssignMessage policy in PostFlow with Access-Control-Allow-Origin headers |

**Debug workflow:** API proxy → Trace tab → Start session → Send request → Inspect each policy execution step

See [api-management.md](api-management.md) for API proxy debugging and policy configuration.

---

## Quick Diagnostic Decision Tree

```
Message failed in MPL
  ├─ Status: FAILED
  │   ├─ Error in adapter? (connection, auth, timeout)
  │   │   ├─ HTTP: Check URL, credentials, SSL certificate
  │   │   ├─ RFC: Check Cloud Connector, SAP user, function module
  │   │   ├─ SFTP: Check host, port, SSH key, file permissions
  │   │   └─ JDBC: Check connection string, DB user, SQL syntax
  │   ├─ Error in mapping? (XSLT, message mapping)
  │   │   ├─ Check input payload format matches schema
  │   │   ├─ Check for null/missing fields
  │   │   └─ Test mapping with Simulation
  │   ├─ Error in script? (Groovy, JavaScript)
  │   │   ├─ Check script logs (attachments in MPL)
  │   │   ├─ Look for NullPointerException
  │   │   └─ Test with Simulation or Trace
  │   └─ Error in external call? (Request Reply, Send)
  │       ├─ Check target system availability
  │       ├─ Verify response format matches expectation
  │       └─ Check timeout settings
  ├─ Status: RETRY
  │   ├─ Transient error (backend temporarily unavailable)
  │   ├─ Monitor retry attempts
  │   └─ If stuck in retry: check backend, consider manual retry
  └─ Status: ESCALATED
      └─ Review escalation logic and notification configuration
```

---

## OData Adapter Errors

| Error | Cause | Resolution |
|---|---|---|
| `"Entity not found"` | Incorrect entity set name or missing key fields | Verify entity set name matches service metadata (`$metadata`); check key field values |
| `"CSRF token validation failed"` | Token expired or not fetched | Enable **Fetch CSRF Token** in OData adapter; check if `x-csrf-token` header is returned by HEAD request |
| `"Unsupported media type"` | Content-Type mismatch with OData version | Use `application/json` for OData V4; `application/json;odata=verbose` for V2 |
| `"Batch request failed"` | One or more operations in changeset failed | Review individual operation responses in batch response body; fix failing operation |
| `"$filter not supported on field"` | Backend does not support filtering on that property | Check service metadata for `Filterable` annotation; use a different filter field |
| `"Navigation property not found"` | Expand path incorrect | Verify navigation property name in `$metadata`; check casing (OData is case-sensitive) |

---

## IDoc / RFC Adapter Errors

| Error | Cause | Resolution |
|---|---|---|
| `"RFC_ERROR_LOGON_FAILURE"` | SAP user locked, expired password, or wrong client | Check user in SU01; verify client number; reset password if locked |
| `"RFC_ERROR_SYSTEM_FAILURE"` | ABAP short dump in target function module | Check ST22 in SAP backend for ABAP dump details |
| `"IDoc status 51 (Application Error)"` | Business rule validation failed in IDoc processing | Check WE02/WE05 in SAP for IDoc status details; review BD87 for reprocessing |
| `"IDoc status 64 (IDoc ready to be transferred)"` | IDoc stuck, partner profile or port misconfigured | Check WE20 (partner profile) and WE21 (port configuration) |
| `"No RFC authorization for function module"` | Missing S_RFC authorization | Add function module group to S_RFC authorization object in role (PFCG) |
| `"Cloud Connector: virtual host not mapped"` | Virtual host in adapter not configured in CC | Add virtual host mapping in Cloud Connector access control; see [cloud-connector.md](cloud-connector.md) |

---

## SSL / Certificate Errors

| Error | Cause | Resolution |
|---|---|---|
| `"PKIX path building failed"` | Server certificate not trusted (CA cert missing from keystore) | Import CA root/intermediate certificate to tenant keystore |
| `"Certificate expired"` | Server or client certificate past validity | Renew certificate; upload new cert to keystore; update adapter alias |
| `"Handshake failure"` | TLS version or cipher suite mismatch | Check TLS version support (CPI requires TLS 1.2+); verify cipher compatibility |
| `"Certificate alias not found"` | Alias referenced in adapter does not exist in keystore | Verify alias spelling in adapter config matches keystore entry exactly |
| `"Peer not authenticated"` | Mutual TLS required but client cert not sent | Configure client certificate (Private Key Alias) in adapter; ensure key pair is in keystore |

**Certificate expiration check (Groovy):**
```groovy
import com.sap.it.api.keystore.KeystoreService
import com.sap.it.api.ITApiFactory
import java.security.cert.X509Certificate

def keystoreService = ITApiFactory.getService(KeystoreService.class, null)
def cert = keystoreService.getCertificate("partner-cert")

if (cert instanceof X509Certificate) {
    def expiryDate = cert.getNotAfter()
    def daysRemaining = (expiryDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
    if (daysRemaining < 30) {
        // Alert: certificate expiring soon
    }
}
```

See [security.md](security.md) for keystore management and certificate deployment.

---

## Event Mesh / AMQP Errors

| Error | Cause | Resolution |
|---|---|---|
| `"Queue not found"` | Queue name incorrect or queue not provisioned | Verify queue name in Event Mesh dashboard; check queue naming format (`<namespace>/<queue>`) |
| `"AMQP connection refused"` | Wrong host, port, or credentials for AMQP endpoint | Verify AMQP endpoint URL from service key; check OAuth credentials |
| `"Message rejected (quota exceeded)"` | Queue storage quota full | Consume pending messages; increase queue size in Event Mesh admin; check for blocked consumers |
| `"Topic subscription not found"` | Topic not subscribed to queue | Create topic subscription in Event Mesh cockpit → Queues → Subscriptions |
| `"Webhook delivery failed"` | Webhook endpoint unreachable or returns non-2xx | Check webhook URL accessibility; verify endpoint returns 200 OK within timeout |

See [event-mesh.md](event-mesh.md) for Event Mesh configuration and architecture.

---

## Groovy Error Handling Patterns

### Robust Exception Subprocess Script
```groovy
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    def ex = message.getProperties().get("CamelExceptionCaught")

    def errorInfo = [
        timestamp : new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
        iFlowName : message.getProperties().get("SAP_IntegrationFlowID") ?: "unknown",
        messageId : message.getHeaders().get("SAP_MessageProcessingLogID") ?: "unknown",
        correlationId : message.getHeaders().get("SAP_ApplicationID") ?: "none",
        errorClass : ex?.getClass()?.name ?: "unknown",
        errorMessage : ex?.getMessage() ?: "No error message available",
        rootCause : getRootCause(ex)?.getMessage() ?: "none"
    ]

    // Log as attachment for MPL visibility
    def errorJson = groovy.json.JsonOutput.prettyPrint(
        groovy.json.JsonOutput.toJson(errorInfo)
    )
    messageLog.addAttachmentAsString("ErrorDetails", errorJson, "application/json")

    // Set error response body
    message.setBody(errorJson)
    message.setHeader("Content-Type", "application/json")
    message.setHeader("CamelHttpResponseCode", 500)

    return message
}

private Throwable getRootCause(Throwable t) {
    while (t?.cause != null && t.cause != t) {
        t = t.cause
    }
    return t
}
```

### Retry Logic with Exponential Backoff (Groovy)
```groovy
// Use in a Looping Process Call with max iterations
def retryCount = message.getProperties().get("retryCount")?.toInteger() ?: 0
def maxRetries = 3

if (retryCount >= maxRetries) {
    throw new Exception("Max retries ($maxRetries) exceeded")
}

// Exponential backoff: 1s, 2s, 4s
def waitMs = (long) Math.pow(2, retryCount) * 1000
Thread.sleep(waitMs)

message.setProperty("retryCount", retryCount + 1)
```

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for exception subprocess design
- See [operations-monitoring.md](operations-monitoring.md) for monitoring setup
- See [adapters.md](adapters.md) for adapter-specific configurations
- See [cloud-connector.md](cloud-connector.md) for on-premise connectivity troubleshooting
- See [edge-integration-cell.md](edge-integration-cell.md) for Edge deployment troubleshooting
- See [api-management.md](api-management.md) for API proxy debugging
- See [simulation-testing.md](simulation-testing.md) for design-time testing and Trace mode
- See [security.md](security.md) for SSL/certificate configuration
- See [event-mesh.md](event-mesh.md) for Event Mesh troubleshooting

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about error resolution, Cloud Integration operations, logging patterns, and monitoring strategies.
