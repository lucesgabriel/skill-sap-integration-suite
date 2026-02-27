# Adapters Reference

Complete adapter configuration guide for SAP Cloud Integration (80+ adapters).

## Adapter Selection Guide

| Integration Need | Recommended Adapter | Alternative |
|---|---|---|
| REST APIs | HTTP/HTTPS | OData V2/V4 |
| SOAP Services | SOAP 1.x/2.0 | HTTP with SOAP envelope |
| File Transfer | SFTP (secure) | FTP, SMB |
| Email | Mail (SMTP/IMAP/POP3) | - |
| Message Queuing | JMS, Kafka | AMQP |
| Databases | JDBC | OData (for HANA) |
| SAP Systems | RFC, IDoc, OData | SOAP (for PI) |
| SuccessFactors | SuccessFactors OData/SOAP | HTTP |
| Ariba | Ariba (cXML) | HTTP |
| Salesforce | Salesforce | Salesforce Pub/Sub |
| B2B/EDI | AS2, AS4 | SFTP + EDI Converter |
| Events | Kafka, AEM | JMS |
| Third-party SaaS | Open Connectors | HTTP/REST |

---

## Protocol Adapters

### HTTP/HTTPS Adapter

**Use For:** REST APIs, webhooks, HTTP-based services

**Sender (Inbound):**
- **Address:** `/endpoint/path` (added to tenant URL)
- **Method:** GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE
- **Authentication:** Basic, Client Certificate, OAuth 2.0
- **CSRF Protection:** On/Off

**Receiver (Outbound):**
- **Address:** Full URL (e.g., `https://api.example.com/orders`)
- **Method:** GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE
- **Authentication:** None, Basic, Client Certificate, OAuth 2.0, Principal Propagation
- **Request Headers:** Add custom headers (Content-Type, Authorization, etc.)
- **Query Parameters:** Dynamic or static
- **Timeout:** Default 60s, configurable
- **Proxy:** Type (Internet, On-Premise via Cloud Connector)

**Common Patterns:**
```
POST with JSON:
- Method: POST
- Content-Type: application/json
- Body: ${in.body}

GET with query params:
- Method: GET
- Address: https://api.example.com/users?id=${property.userId}
```

**Best Practices:**
- Use HTTPS for production
- Set appropriate timeouts
- Handle HTTP error codes in Exception Subprocess
- Use OAuth 2.0 for secure APIs
- For on-premise: Use Cloud Connector + Location ID

### SOAP Adapter

**Use For:** SOAP 1.x and SOAP 2.0 web services

**Sender:**
- **Address:** `/soap/endpoint`
- **Quality of Service:** Best Effort, At Least Once, Exactly Once
- **WS-Security:** Username Token, X.509, SAML
- **WS-Addressing:** Enable for routing headers

**Receiver:**
- **Address:** WSDL URL or endpoint URL
- **SOAP Version:** 1.1 or 1.2
- **Authentication:** None, Basic, Client Certificate, Principal Propagation
- **WS-Security:** Sign, Encrypt, Username Token
- **WS-Addressing:** Supported
- **Attachments:** SOAP with Attachments (SwA), MTOM

**Configuration Steps:**
1. Import WSDL
2. Select Service, Port, Operation
3. Configure authentication
4. Set WS-Security if required

**Example SOAP Call:**
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Header/>
  <soapenv:Body>
    <GetCustomer>
      <CustomerID>${property.customerId}</CustomerID>
    </GetCustomer>
  </soapenv:Body>
</soapenv:Envelope>
```

### OData Adapter

**Versions:** OData V2, OData V4

**Sender:**
- **Address:** `/odata/endpoint`
- **Service Definition:** Upload EDMX
- **Operations:** Create, Read, Update, Delete, Query
- **Authentication:** Basic, OAuth 2.0, Client Certificate

**Receiver:**
- **Address:** Service URL (e.g., `https://services.odata.org/V4/service`)
- **Operations:**
  - **Query (GET):** `$select`, `$filter`, `$expand`, `$top`, `$skip`, `$orderby`
  - **Create (POST):** Insert new entity
  - **Update (PUT/PATCH):** Modify entity
  - **Delete (DELETE):** Remove entity
- **Pagination:** Automatic or manual (`$skip`, `$top`)
- **Authentication:** Basic, OAuth 2.0, Client Certificate, Principal Propagation

**Common $filter Examples:**
```
$filter=OrderDate gt 2024-01-01
$filter=Status eq 'Active'
$filter=Amount gt 1000 and Currency eq 'USD'
$filter=Country in ('US','CA','MX')
```

**Best Practices:**
- Use $select to retrieve only needed fields
- Implement paging for large result sets
- Use $expand wisely (performance impact)
- Handle OData errors (400, 404, 500)

### SFTP Adapter

**Use For:** Secure file transfer

**Sender (Poll Files):**
- **Directory:** `/inbound/orders`
- **File Name:** Wildcards supported (`*.csv`, `order_*.xml`)
- **Post-Processing:**
  - Delete file
  - Move to archive directory
  - Keep file
- **Polling Interval:** Default 60s, minimum 5s
- **Max Messages:** Limit files per poll
- **Sorting:** By name, by date

**Receiver (Write Files):**
- **Directory:** `/outbound/orders`
- **File Name:** Static or dynamic (`order_${date:now:yyyyMMdd}.xml`)
- **Append:** Create new or append to existing
- **Create Directory:** Auto-create if missing
- **File Existence:** Fail, Override, Append

**Authentication:**
- Public Key (recommended)
- User Credentials (username/password)

**Best Practices:**
- Use public key authentication
- Archive processed files
- Use file locks or unique names to avoid race conditions
- Handle large files (consider splitting)
- Set appropriate timeout for large transfers

### FTP Adapter

Similar to SFTP but without encryption. **Use SFTP instead for production.**

### SMB Adapter

**Use For:** Windows file shares (CIFS/SMB protocol)

**Configuration:**
- **Server:** `smb://server/share`
- **Directory:** Path within share
- **Authentication:** Username/Password, Domain

**Use Cases:**
- Legacy Windows file shares
- On-premise Windows servers

### JMS Adapter

**Use For:** Asynchronous messaging via queues

**Sender:**
- **Queue Name:** `order.queue`
- **Concurrent Consumers:** 1-99 (parallel processing)
- **Max Prefetch:** Messages to fetch per poll
- **Retry:** Configure retry attempts

**Receiver:**
- **Queue Name:** `order.queue`
- **Quality of Service:**
  - **Exactly Once:** Transactional, guaranteed delivery
  - **Exactly Once In Order (EOIO):** Use exclusive queues
- **Message Type:** Text, Bytes
- **Headers:** Custom JMS headers
- **Priority:** 0-9 (9 = highest)
- **Expiration:** TTL in milliseconds

**EOIO Configuration:**
```
Queue Name: order.queue
Quality of Service: Exactly Once In Order
Correlation ID: ${header.OrderID}
```

**Best Practices:**
- Use JMS for decoupling sender and receiver
- Implement EOIO for order-critical messages
- Monitor queue depth
- Set expiration to avoid queue bloat
- Use Dead Letter Queue (DLQ) for failed messages

### AMQP Adapter

**Use For:** RabbitMQ, Azure Service Bus, other AMQP 1.0 brokers

**Configuration:**
- **Broker URL:** `amqps://broker.example.com:5671`
- **Queue/Topic Name:** Target queue or topic
- **Authentication:** SASL Plain, External
- **Durable:** Persist messages to disk
- **Auto-acknowledge:** Acknowledge on receipt or processing

### Kafka Adapter

**Use For:** Apache Kafka event streaming

**Sender:**
- **Topics:** Comma-separated list
- **Consumer Group:** Unique group ID
- **Offset:** Earliest, Latest, Manual
- **Record Key:** Use as correlation ID
- **Max Poll Records:** Limit per poll

**Receiver:**
- **Topic:** Target topic name
- **Partition:** Auto or specify
- **Record Key:** For partitioning
- **Compression:** None, gzip, snappy, lz4, zstd

**Configuration Example:**
```
Bootstrap Servers: kafka1:9092,kafka2:9092,kafka3:9092
Topic: customer.events
Consumer Group: sap-cpi-consumer-01
Authentication: SASL_SSL (SCRAM-SHA-512)
```

### AS2 Adapter

**Use For:** B2B secure file transfer

**Sender:**
- **AS2 ID:** Partner AS2 identifier
- **MDN:** Request signed/unsigned MDN
- **Verify Signature:** Validate partner signature
- **Decrypt:** Decrypt incoming messages

**Receiver:**
- **Partner URL:** AS2 endpoint
- **AS2 ID (From/To):** Own and partner AS2 IDs
- **Sign/Encrypt:** Configure message security
- **MDN:** Request sync or async MDN

**Certificates:**
- Partner public key (for encryption & signature verification)
- Own private key (for decryption & signing)

### AS4 Adapter

**Use For:** Advanced B2B messaging (successor to AS2)

**Features:**
- ebMS 3.0 compliant
- Pull/Push modes
- Message partitioning
- Receipt acknowledgments

**Configuration:**
- **Service/Action:** ebMS service and action
- **Party ID:** Sender/Receiver identities
- **Security:** Sign, Encrypt, both
- **Reliability:** At-Most-Once, At-Least-Once, Exactly-Once

---

## Application Adapters

### SuccessFactors Adapter

**Protocols:** OData V2, SOAP

**OData Receiver:**
- **Address:** `https://apiXX.successfactors.com/odata/v2`
- **Entity:** Employee, User, Position, etc.
- **Operations:** Query, Upsert, Insert, Update, Delete
- **Authentication:** Basic, OAuth 2.0

**SOAP Receiver:**
- **Operations:** Query, Upsert, Insert, Update, Delete
- **API Version:** Specify SF API version

**Common Use Cases:**
- Employee onboarding/offboarding
- Organizational chart sync
- Payroll data extraction
- Time tracking integration

### Ariba Network Adapter

**Use For:** Ariba procurement integration

**Protocol:** cXML

**Configuration:**
- **Ariba Network ID:** Your Ariba ID
- **Shared Secret:** Authentication credential
- **Message Type:** PO, Invoice, OrderRequest, etc.

**Common Scenarios:**
- Purchase Order transmission
- Invoice submission
- Catalog updates

### Salesforce Adapter

**Use For:** Salesforce CRM integration

**Receiver:**
- **Operation:** Query, Create, Update, Upsert, Delete
- **Object:** Account, Contact, Lead, Opportunity, Custom Objects
- **SOQL Query:** Salesforce Object Query Language
- **Authentication:** OAuth 2.0, Username-Password

**SOQL Examples:**
```sql
SELECT Id, Name, Email FROM Contact WHERE LastModifiedDate > YESTERDAY
SELECT Id, Amount FROM Opportunity WHERE StageName = 'Closed Won'
```

### Salesforce Pub/Sub Adapter

**Use For:** Real-time Salesforce event streaming

**Sender:**
- **Topic:** Platform Event or Change Data Capture topic
- **Replay ID:** Start from specific event

**Receiver:**
- **Publish Event:** Send Platform Events to Salesforce

---

## Cloud Platform Adapters

### AWS Adapters

**AWS S3:**
- **Operations:** Get, Put, Delete, List
- **Bucket:** S3 bucket name
- **Authentication:** Access Key/Secret Key, IAM Role

**AWS SQS:**
- **Operations:** Send, Receive, Delete
- **Queue URL:** Full SQS queue URL
- **Max Messages:** Batch size

**AWS SNS:**
- **Operation:** Publish
- **Topic ARN:** Target SNS topic

### Microsoft 365 Adapters

**Teams:**
- Send channel messages
- Post adaptive cards

**OneDrive:**
- Upload/download files
- List folders

**Outlook Mail:**
- Send emails
- Read emails (with filters)

### Google Cloud Adapters

**Google Cloud Storage:**
- Read/write objects
- List buckets

**Google Pub/Sub:**
- Publish/subscribe messages

**Google BigQuery:**
- Run queries
- Stream inserts

---

## Database Adapters

### JDBC Adapter

**Supported Databases:**
- Oracle
- Microsoft SQL Server
- MySQL
- PostgreSQL
- SAP HANA
- IBM DB2

**Receiver Operations:**
- **Select:** Read data (returns XML)
- **Insert:** Add records
- **Update:** Modify records
- **Delete:** Remove records
- **Call:** Execute stored procedures

**Configuration:**
- **JDBC URL:** `jdbc:sqlserver://host:1433;databaseName=mydb`
- **Driver:** Auto-selected based on URL
- **Authentication:** Database credentials
- **Connection Timeout:** Default 30s

**SQL Examples:**
```sql
-- Select
SELECT * FROM Orders WHERE OrderDate > '2024-01-01'

-- Insert (use parameterized)
INSERT INTO Orders (OrderID, Customer, Amount) VALUES (?, ?, ?)

-- Update
UPDATE Orders SET Status = 'Processed' WHERE OrderID = ?

-- Call Stored Procedure
CALL sp_ProcessOrder(?)
```

**Best Practices:**
- Use parameterized queries (prevent SQL injection)
- Limit result set size (use WHERE, TOP, LIMIT)
- Use connection pooling (reuse connections)
- Index frequently queried columns
- For on-premise: Use Cloud Connector

---

## On-Premise Connectivity

### RFC Adapter

**Use For:** Call SAP function modules in ECC/S/4HANA

**Receiver:**
- **Connection:** Via Cloud Connector
- **Function Module:** e.g., `BAPI_SALESORDER_CREATEFROMDAT2`
- **Authentication:** SAP user credentials

**Configuration:**
1. Set up Cloud Connector
2. Expose RFC destination
3. Configure RFC adapter with destination name
4. Map input parameters

**Example:**
```
Function Module: BAPI_MATERIAL_GETLIST
Import Parameters:
  MATNRSELECTION: Material range
Export Parameters:
  MATNRLIST: Material list
```

### IDoc Adapter

**Use For:** SAP IDoc integration

**Sender:**
- Receive IDocs from SAP via Cloud Connector

**Receiver:**
- Send IDocs to SAP
- IDoc Types: ORDERS05, MATMAS05, DEBMAS06, etc.

**Configuration:**
- **IDoc Type:** e.g., ORDERS05
- **Partner Number:** Sender/Receiver partner
- **Port:** Logical port in SAP
- **Client:** SAP client (e.g., 100)

### Cloud Connector Setup

**Steps:**
1. Install Cloud Connector on-premise
2. Connect to SAP BTP subaccount
3. Add access control for backend systems
4. Define resources (RFC destination, IDoc port, HTTP backend)
5. Configure adapter with Cloud Connector location ID

**Adapter Configuration:**
- **Proxy Type:** On-Premise
- **Location ID:** (if multiple connectors)
- **Authentication:** Basic (credentials for backend)

---

## Open Connectors

**Use For:** 170+ third-party SaaS connectors

**Supported Categories:**
- CRM (Salesforce, HubSpot, Dynamics 365)
- ERP (NetSuite, QuickBooks)
- Marketing (Marketo, Mailchimp)
- Collaboration (Slack, Microsoft Teams)
- Storage (Dropbox, Box, Google Drive)
- Ecommerce (Shopify, WooCommerce)

**Usage:**
1. Create Open Connector instance in Integration Suite
2. Authenticate to third-party service
3. Use HTTP adapter to call Open Connector APIs
4. Unified API across different services

**Benefits:**
- Standardized API interface
- Built-in authentication
- Automatic retries and error handling

---

## Adapter Best Practices

### Performance
- Use polling intervals wisely (avoid very short intervals)
- Implement throttling for rate-limited APIs
- Use batch operations where possible
- Monitor adapter performance in MPL

### Security
- Always use secure protocols (HTTPS, SFTP, not HTTP/FTP)
- Use certificate-based auth when available
- Rotate credentials regularly
- Don't log credentials in trace mode

### Error Handling
- Configure retry logic for transient errors
- Use Exception Subprocess for adapter errors
- Monitor adapter connection health
- Set up alerts for failed messages

### Monitoring
- Track adapter message counts
- Monitor connection pool usage (JDBC)
- Check queue depths (JMS, Kafka)
- Review timeout settings

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for iFlow design patterns and process steps
- See [security.md](security.md) for authentication setup (OAuth2, certificates, API keys)
- See [troubleshooting.md](troubleshooting.md) for adapter-specific error catalogs (HTTP, SFTP, JMS, OData, RFC, AMQP)
- See [cloud-connector.md](cloud-connector.md) for on-premise adapter connectivity (RFC, HTTP proxy)
- See [event-mesh.md](event-mesh.md) for AMQP and Kafka adapter patterns with Event Mesh
- See [postman-testing.md](postman-testing.md) for testing adapter endpoints with Postman
- See [simulation-testing.md](simulation-testing.md) for design-time testing of adapter configurations

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about adapters, connectivity patterns, and advanced adapter configuration.
