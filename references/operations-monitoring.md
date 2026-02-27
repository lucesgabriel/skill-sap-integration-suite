# Operations & Monitoring Reference

Message monitoring, stores management, and operational best practices.

## Message Monitoring

### Access Message Processing Logs (MPL)

**Navigation:** Monitor → Integrations and APIs → All Integration Flows

**Filter Options:**
- Time Range: Last hour, Last 24 hours, Custom
- Status: All, Completed, Failed, Processing, Retry
- Integration Flow: Filter by specific iFlow
- Correlation ID: Track specific message

**MPL Details View:**
- Message GUID (unique identifier)
- Status and Status Details
- Processing Start/End Time
- Log Level (INFO, WARNING, ERROR)
- Attachments (payloads, logs from scripts)
- Properties (Exchange properties set during processing)

### Message States

| State | Meaning | Action |
|---|---|---|
| Completed | Successful processing | None |
| Failed | Error occurred | Review error, fix, retry |
| Processing | Currently being processed | Wait or check if stuck |
| Retry | Scheduled for retry | Monitor retry attempts |
| Escalated | Escalation event triggered | Review escalation logic |

---

## Monitoring Hierarchy

```
Integration Suite → Monitor
    ├── Integrations and APIs
    │   ├── All Integration Flows (MPL)
    │   ├── Completed Messages
    │   ├── Failed Messages
    │   └── Processing Messages
    ├── Manage Integration Content
    │   ├── Deployed Artifacts (iFlows, Script Collections)
    │   ├── Design (edit iFlows)
    │   └── Discover (prepackaged content)
    ├── Manage Security
    │   ├── Keystore (certificates, keys)
    │   ├── User Credentials
    │   ├── OAuth2 Credentials
    │   ├── Secure Parameters
    │   └── Known Hosts (SSH)
    └── Manage Stores
        ├── Data Stores
        ├── Variables
        ├── Message Queues (JMS)
        └── Number Ranges
```

---

## Data Store Management

### View Data Store Entries

**Navigation:** Monitor → Manage Stores → Data Stores

**Operations:**
- View entries by Data Store name
- Search by Entry ID
- Download entry content
- Delete individual entries
- Monitor storage usage (100MB limit)

### Best Practices

1. **Naming Convention**
   - Use descriptive names: `OrderIdempotency`, `CustomerCache`
   - Avoid generic names: `DataStore1`, `temp`

2. **Entry ID Strategy**
   - Use business keys: Order ID, Customer ID
   - Include date for time-based cleanup: `ORDER-12345-20240115`

3. **Retention Management**
   - Set appropriate TTL (Time To Live)
   - Implement cleanup scripts for old entries
   - Monitor storage regularly (alert at 80%)

4. **Size Management**
   - Keep entries small (<1MB per entry)
   - Don't store large payloads
   - Use compression if needed

---

## Variables Management

### Global Variables
- Accessible across all iFlows
- Use for: Configuration, counters, cache

### Local Variables
- Accessible only within iFlow
- Use for: Temporary state, session data

**Navigation:** Monitor → Manage Stores → Variables

---

## JMS Queue Management

**Navigation:** Monitor → Manage Stores → Message Queues

**Monitoring:**
- Queue depth (number of messages)
- Messages in processing
- Dead Letter Queue (DLQ) messages

**Operations:**
- View queue entries
- Retry failed messages
- Delete stuck messages
- Monitor queue trends

**Alerts:**
- Set up alerts when queue depth exceeds threshold
- Monitor DLQ for failed messages
- Track message processing times

---

## Connectivity Tests

**Navigation:** Monitor → Manage Integration Content → Connectivity Tests

**Test Types:**
- **HTTP:** Test HTTP endpoint connectivity
- **SFTP:** Test SFTP connection and authentication
- **JDBC:** Test database connectivity
- **JMS:** Test message queue availability

**Example: Test HTTP Endpoint**
```
URL: https://api.example.com/health
Method: GET
Expected Response: 200 OK
```

---

## Alerts and Notifications

### SAP Alert Notification Service

**Setup:**
1. Subscribe to Alert Notification Service in BTP
2. Create Alert Rule in Integration Suite
3. Configure conditions (e.g., iFlow failure, queue depth)
4. Set notification channels (email, Slack, webhook)

**Alert Conditions:**
- Integration Flow fails
- Queue depth exceeds threshold
- Data Store exceeds 80% capacity
- Certificate expiration warning

---

## Cloud ALM Integration

**SAP Cloud ALM** provides centralized monitoring across SAP landscape.

**Features:**
- End-to-end integration monitoring
- Health monitoring dashboards
- Exception management
- Root cause analysis
- Automated alerting

**Setup:**
1. Configure Cloud ALM connection
2. Map Integration Suite to monitored scope
3. Define KPIs and thresholds
4. Configure alert rules

---

## Performance Monitoring

### Key Metrics

| Metric | Target | Remediation |
|---|---|---|
| Message Processing Time | <5 seconds | Optimize mapping, use streaming |
| Error Rate | <1% | Review error patterns, fix root cause |
| Queue Depth | <100 messages | Increase consumers, optimize processing |
| Data Store Usage | <80MB | Cleanup old entries, reduce entry size |

### Monitoring Tools

1. **MPL Analysis**
   - Review processing times per step
   - Identify bottlenecks
   - Track error patterns

2. **Custom Dashboards**
   - Use SAP Analytics Cloud
   - Query Integration Content APIs
   - Build Grafana dashboards (via APIs)

---

## Operational Best Practices

### 1. Proactive Monitoring
- Review MPL daily (at minimum)
- Set up alerts for critical iFlows
- Monitor queue depths and Data Stores
- Track certificate expiration dates

### 2. Incident Management
- Document common errors and resolutions
- Create runbooks for critical scenarios
- Define escalation procedures
- Maintain on-call rotation

### 3. Change Management
- Test changes in dev/qa before production
- Deploy during maintenance windows
- Implement rollback strategy
- Document all changes

### 4. Capacity Planning
- Monitor message volumes (trend analysis)
- Plan for peak loads
- Review JMS queue limits
- Consider Edge Integration Cell for data locality

### 5. Security Reviews
- Rotate credentials quarterly
- Review keystore certificates
- Audit user access (who can deploy?)
- Monitor failed authentication attempts

---

## Troubleshooting with Monitoring Tools

### Scenario: High Error Rate

**Steps:**
1. Filter MPL by "Failed" status
2. Identify common error patterns
3. Review error messages and stack traces
4. Check if adapter configuration changed
5. Verify backend system availability
6. Fix root cause and deploy
7. Retry failed messages (if applicable)

### Scenario: Slow Processing

**Steps:**
1. Review MPL processing times
2. Enable Trace mode for detailed analysis
3. Identify slow steps (mapping, external calls)
4. Optimize bottleneck steps
5. Consider parallel processing (splitter)
6. Test improved iFlow

### Scenario: Queue Backlog

**Steps:**
1. Check JMS queue depth
2. Increase concurrent consumers (if possible)
3. Review message processing time
4. Check for stuck messages (delete if needed)
5. Investigate slow downstream system
6. Consider adding more processing nodes (if supported)

---

## Integration Content OData API

The Cloud Integration OData API enables programmatic monitoring and management. Base URL: `https://<tmn-host>/api/v1/`

### Key Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `MessageProcessingLogs` | GET | Query MPL entries with filters |
| `MessageProcessingLogs('{id}')/Attachments` | GET | Get MPL attachments (payloads, logs) |
| `MessageProcessingLogs('{id}')/ErrorInformation` | GET | Detailed error info for failed messages |
| `IntegrationRuntimeArtifacts` | GET | List deployed artifacts and their status |
| `IntegrationRuntimeArtifacts('{id}')` | DELETE | Undeploy an artifact |
| `LogFiles` | GET | System log files |
| `LogFiles('{name}')/$value` | GET | Download specific log file |
| `SecurityArtifactDescriptor` | GET | List keystore entries with expiration dates |

### MPL Query Examples

```
# Failed messages in last 24 hours
/api/v1/MessageProcessingLogs?$filter=Status eq 'FAILED'
  &$filter=LogStart gt datetime'2024-01-15T00:00:00'
  &$orderby=LogStart desc
  &$top=50

# Messages for specific iFlow
/api/v1/MessageProcessingLogs?$filter=IntegrationFlowName eq 'MyIFlow'
  &$select=MessageGuid,Status,LogStart,LogEnd

# Get error details for a specific message
/api/v1/MessageProcessingLogs('{messageGuid}')/ErrorInformation/$value
```

### Authentication for API Access

```
# Service Key approach (OAuth2 Client Credentials)
# 1. Create service instance: "Process Integration Runtime" plan "api"
# 2. Create service key
# 3. Use tokenurl, clientid, clientsecret from service key

curl -X POST <tokenurl>/oauth/token \
  -d "grant_type=client_credentials&client_id=<clientid>&client_secret=<clientsecret>"

curl -H "Authorization: Bearer <token>" \
  https://<tmn>/api/v1/MessageProcessingLogs?$top=10
```

---

## Custom Monitoring with Groovy

### Attach Custom Log Entries to MPL

```groovy
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)

    // Add custom header for MPL visibility
    messageLog.setStringProperty("OrderId", message.getProperty("orderId"))
    messageLog.setStringProperty("CustomerName", message.getProperty("customerName"))

    // Attach payload snapshot at this processing step
    def body = message.getBody(String.class)
    messageLog.addAttachmentAsString(
        "Step3_AfterMapping",
        body,
        "application/xml"
    )

    return message
}
```

### Custom Counter with Variables Store

```groovy
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi

// Increment a global counter (e.g., for daily message count)
def varService = ITApiFactory.getService(
    com.sap.it.api.msglog.adapter.api.VariableStoreService.class, null
)
def currentCount = varService.get("DailyMessageCount", "global")?.toInteger() ?: 0
varService.put("DailyMessageCount", "global", (currentCount + 1).toString())
```

---

## Trace Levels and Best Practices

### Log Level Configuration

| Level | What is Captured | Performance Impact | Use Case |
|---|---|---|---|
| **None** | Nothing (MPL entry only) | Minimal | Production (high volume) |
| **Info** | Basic step execution info | Low | Production (normal) |
| **Debug** | Step details, properties, headers | Medium | Staging/QA troubleshooting |
| **Trace** | Full payload at every step | **High** | Active debugging only |

### Trace Mode Workflow

1. Navigate to **Monitor → Manage Integration Content**
2. Find the deployed iFlow
3. Click **Log Configuration** (or set via iFlow properties)
4. Set to **Trace** level
5. **Trigger the message** (trace only captures new messages)
6. Go to **Monitor → All Integration Flows** → find message
7. Click message → **Trace** tab → step through each node
8. **Revert to Info** after debugging (trace expires after 10 minutes automatically)

**Important:** Trace captures full payloads. If messages contain PII or sensitive data, ensure compliance with data protection policies before enabling trace in production.

---

## Certificate Expiration Monitoring

### Manual Check
**Navigation:** Monitor → Manage Security → Keystore

The keystore view shows:
- Alias name
- Certificate type (X.509, Key Pair)
- Valid From / Valid To dates
- Issuer and Subject

### Automated Monitoring via API

```bash
# List all keystore entries with expiration info
curl -H "Authorization: Bearer <token>" \
  "https://<tmn>/api/v1/KeystoreEntries" \
  | jq '.d.results[] | {alias: .Hexalias, validUntil: .ValidNotAfter}'
```

**Alert Strategy:**
- 90 days before expiry → Informational alert
- 30 days before expiry → Warning alert
- 7 days before expiry → Critical alert
- Use SAP Alert Notification Service with custom condition on certificate validity

---

## Number Range Management

**Navigation:** Monitor → Manage Stores → Number Ranges

Number Ranges provide unique sequential identifiers across iFlow executions.

| Field | Description |
|---|---|
| Name | Identifier for the number range |
| Current Value | Last assigned number |
| Min / Max | Boundaries of the range |
| Rotate | Whether to restart at Min when Max is reached |
| Field Length | Number of digits (zero-padded) |

**Use Cases:**
- Sequential document numbers (Invoice 0001, 0002...)
- Unique file naming for SFTP outputs
- Batch identifiers for EDI transmissions

**Access in Groovy:**
```groovy
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.NumberRangeService

def nrService = ITApiFactory.getService(NumberRangeService.class, null)
def nextNumber = nrService.getNextValuefromNumberRange("InvoiceRange", "global")
message.setProperty("invoiceNumber", nextNumber)
```

---

## Deployed Artifact Management

### Deployment States

| State | Meaning | Action |
|---|---|---|
| **Started** | Running and processing messages | Normal |
| **Error** | Deployment failed | Review error; check adapter config; redeploy |
| **Starting** | Deploying in progress | Wait |
| **Stopping** | Undeployment in progress | Wait |

### Bulk Redeployment (after tenant update)

After SAP applies a monthly update to the tenant, some iFlows may need redeployment:

1. Monitor → Manage Integration Content
2. Filter by Status: **Error**
3. Review each artifact's error message
4. Redeploy affected artifacts (click Restart or redeploy from Design workspace)

---

**Next Steps:**
- See [troubleshooting.md](troubleshooting.md) for error resolution
- See [cloud-integration.md](cloud-integration.md) for iFlow error handling design
- See [content-transport.md](content-transport.md) for deployment strategies
- See [security.md](security.md) for keystore and credential management
- See [scripting.md](scripting.md) for Groovy logging patterns
- See [api-management.md](api-management.md) for API analytics and monitoring

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about Cloud Integration operations, OData monitoring API, logging patterns, and performance monitoring.
