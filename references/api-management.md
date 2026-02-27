# API Management Reference

Complete guide for SAP API Management: proxy development, policies, Developer Portal, analytics, and API lifecycle management.

## Table of Contents

- [API Proxy Structure](#api-proxy-structure)
- [API Proxy Creation](#api-proxy-creation)
- [Policy Categories (34 Policies)](#policy-categories-34-policies)
- [Common API Proxy Patterns](#common-api-proxy-patterns)
- [Developer Portal / API Business Hub Enterprise](#developer-portal--api-business-hub-enterprise)
- [API Products](#api-products)
- [API Analytics and Monitoring](#api-analytics-and-monitoring)
- [Key Value Maps (KVM)](#key-value-maps-kvm)
- [API Proxy Debugging](#api-proxy-debugging)
- [OpenAPI / Swagger Integration](#openapi--swagger-integration)
- [API Versioning Strategies](#api-versioning-strategies)
- [Custom Policy Scripting](#custom-policy-scripting)
- [Cloud Integration to API Management Bridge](#cloud-integration-to-api-management-bridge)
- [Best Practices](#best-practices)

---

## API Proxy Structure

```
Client → Proxy Endpoint → [Policies] → Target Endpoint → Backend API
              ↓                              ↓
         PreFlow                        PreFlow
         Conditional Flows              Conditional Flows
         PostFlow                       PostFlow
              ↓                              ↓
         Fault Rules                    Fault Rules
```

### Proxy Components

| Component | Purpose |
|---|---|
| **Proxy Endpoint** | Public-facing URL that clients call |
| **Target Endpoint** | Backend API URL (Cloud Integration, S/4HANA, third-party) |
| **PreFlow** | Policies that execute BEFORE the main logic (always runs) |
| **PostFlow** | Policies that execute AFTER the main logic (always runs) |
| **Conditional Flow** | Policies that execute only when conditions are met |
| **Fault Rules** | Error handling policies (execute on error) |
| **Route Rules** | Dynamic backend routing based on conditions |

### Request/Response Flow

```
Client Request → Proxy Endpoint PreFlow → Proxy Conditional Flows → Proxy PostFlow
                                                    ↓
                 Target Endpoint PreFlow → Target Conditional Flows → Target PostFlow
                                                    ↓
                                              Backend API Call
                                                    ↓
                 Target Response PostFlow → Target Response PreFlow
                                                    ↓
                 Proxy Response PostFlow → Proxy Response PreFlow → Client Response
```

---

## API Proxy Creation

### Method 1: Create from Scratch

**Navigation:** Configure → APIs → Create

**Steps:**
1. Click **Create** in the API Management console
2. Enter API details:
   - **Name**: `SalesOrder_API_v1`
   - **Title**: `Sales Order API`
   - **API Base Path**: `/api/v1/salesorders`
   - **API State**: Active
3. Configure Target Endpoint:
   - **URL**: `https://<iflow-host>/http/salesorder`
   - **Proxy Type**: Internet / On-Premise (via Cloud Connector)
4. Add resources (paths):
   - `GET /` — List orders
   - `GET /{id}` — Get order by ID
   - `POST /` — Create order
   - `PUT /{id}` — Update order
   - `DELETE /{id}` — Delete order
5. Save and Deploy

### Method 2: Import from OpenAPI Specification

**Steps:**
1. Click **Create** → **Import API**
2. Upload OpenAPI 3.0 / Swagger 2.0 JSON or YAML file
3. Review auto-generated proxy structure
4. Adjust policies and endpoints as needed
5. Save and Deploy

### Method 3: Create from Discovered API

**Steps:**
1. Navigate to **Discover** → Select API provider (e.g., SAP S/4HANA Cloud)
2. Browse available APIs from SAP API Business Hub
3. Select API → Click **Create Proxy**
4. Auto-generates proxy with correct target endpoints
5. Add security and traffic policies
6. Deploy

---

## Policy Categories (34 Policies)

### Security Policies

| Policy | Purpose | Key Configuration |
|---|---|---|
| **Verify API Key** | Validate API keys from query param or header | `ref` attribute for key lookup |
| **OAuth 2.0 v2.0** | Validate OAuth 2.0 tokens | Token endpoint, scopes, grant types |
| **Basic Authentication** | Validate username/password | User credential store reference |
| **SAML Assertion** | Validate SAML 2.0 tokens | Identity provider, assertion attributes |
| **Access Control** | IP whitelisting/blacklisting | CIDR ranges, allow/deny rules |
| **JSON Web Token (JWT)** | Verify, decode, or generate JWTs | Public key, issuer, audience, algorithms |

#### Verify API Key Example
```xml
<VerifyAPIKey async="false" continueOnError="false" enabled="true">
    <APIKey ref="request.queryparam.apikey"/>
</VerifyAPIKey>
```

#### OAuth 2.0 Token Verification Example
```xml
<OAuthV2 async="false" continueOnError="false" enabled="true">
    <Operation>VerifyAccessToken</Operation>
    <Scope>read write</Scope>
</OAuthV2>
```

### Traffic Management Policies

| Policy | Purpose | Key Configuration |
|---|---|---|
| **Quota** | Limit API calls per time period per consumer | Count, interval, time unit, identifier |
| **Spike Arrest** | Rate limiting (requests per second/minute) | Rate (e.g., `30pm` = 30 per minute) |
| **Concurrent Rate Limit** | Limit concurrent connections | Max count, connection timeout |
| **Response Cache** | Cache API responses | Cache key, TTL, scope |

#### Quota Example
```xml
<Quota async="false" continueOnError="false" enabled="true" type="calendar">
    <Allow count="1000"/>
    <Interval>1</Interval>
    <TimeUnit>month</TimeUnit>
    <Identifier ref="request.header.apikey"/>
    <StartTime>2024-01-01 00:00:00</StartTime>
</Quota>
```

#### Spike Arrest Example
```xml
<SpikeArrest async="false" continueOnError="false" enabled="true">
    <Rate>30pm</Rate>
    <!-- 30 per minute = ~1 request every 2 seconds smoothed -->
    <Identifier ref="request.header.apikey"/>
</SpikeArrest>
```

#### Response Cache Example
```xml
<ResponseCache async="false" continueOnError="false" enabled="true">
    <CacheKey>
        <KeyFragment ref="request.uri"/>
    </CacheKey>
    <ExpirySettings>
        <TimeoutInSec>300</TimeoutInSec>
    </ExpirySettings>
    <ExcludeErrorResponse>true</ExcludeErrorResponse>
</ResponseCache>
```

### Mediation Policies

| Policy | Purpose | Key Configuration |
|---|---|---|
| **Assign Message** | Create/modify request or response messages | Set headers, query params, body, status code |
| **Extract Variables** | Extract data from payload, headers, URI | JSONPath, XPath, regex patterns |
| **JSON to XML** | Convert JSON payload to XML | Output variable, root element |
| **XML to JSON** | Convert XML payload to JSON | Output variable |
| **XSL Transform** | Apply XSLT transformations | XSLT resource file |
| **JSON Threat Protection** | Validate JSON structure and limits | Max depth, array size, string length |
| **XML Threat Protection** | Prevent XML attacks (XXE, billion laughs) | Max elements, attributes, depth |
| **Regular Expression Protection** | Validate against regex patterns | Patterns for headers, body, URI |
| **Raise Fault** | Generate custom error responses | Status code, reason phrase, payload |

#### Assign Message Example (Add Backend Auth Header)
```xml
<AssignMessage async="false" continueOnError="false" enabled="true">
    <Set>
        <Headers>
            <Header name="Authorization">Bearer {backend_token}</Header>
            <Header name="X-Forwarded-For">{client.ip}</Header>
        </Headers>
    </Set>
    <IgnoreUnresolvedVariables>false</IgnoreUnresolvedVariables>
    <AssignTo createNew="false" type="request"/>
</AssignMessage>
```

#### Extract Variables Example
```xml
<ExtractVariables async="false" continueOnError="false" enabled="true">
    <JSONPayload>
        <Variable name="orderType">
            <JSONPath>$.order.type</JSONPath>
        </Variable>
    </JSONPayload>
    <Header name="X-Request-ID">
        <Pattern ignoreCase="true">{request_id}</Pattern>
    </Header>
    <Source>request</Source>
</ExtractVariables>
```

### Extension Policies

| Policy | Purpose | Key Configuration |
|---|---|---|
| **JavaScript** | Custom logic via JavaScript | Script resource file |
| **Python Script** | Custom logic via Python | Script resource file |
| **Service Callout** | Call external services during policy execution | URL, method, headers, payload |
| **Statistics Collector** | Collect custom analytics data | Variable names, types |
| **Message Logging** | Log messages to external systems (syslog) | Syslog endpoint, format |

#### Service Callout Example
```xml
<ServiceCallout async="false" continueOnError="false" enabled="true">
    <Request variable="authRequest">
        <Set>
            <Headers>
                <Header name="Content-Type">application/x-www-form-urlencoded</Header>
            </Headers>
            <Payload contentType="application/x-www-form-urlencoded">
                grant_type=client_credentials&amp;client_id={client_id}
            </Payload>
            <Verb>POST</Verb>
        </Set>
    </Request>
    <Response>authResponse</Response>
    <HTTPTargetConnection>
        <URL>https://auth.example.com/oauth/token</URL>
    </HTTPTargetConnection>
</ServiceCallout>
```

---

## Common API Proxy Patterns

### Pattern 1: Secure API with OAuth + Quota

```
ProxyEndpoint PreFlow:
  1. Verify OAuth 2.0 Token
  2. Extract Variables (extract user ID from token)
  3. Quota (100 calls per minute per user)

TargetEndpoint PreFlow:
  4. Assign Message (add backend API key header)

ProxyEndpoint PostFlow (Response):
  5. Response Cache (cache for 5 minutes)
  6. Assign Message (add CORS headers)
```

### Pattern 2: API Aggregation (Mashup)

```
ProxyEndpoint:
  1. Extract Variables (get query parameters)
  2. Service Callout #1 (call Customer API)
  3. Service Callout #2 (call Order API)
  4. JavaScript (merge responses into single JSON)
  5. Assign Message (set combined response body)
```

### Pattern 3: Protocol Transformation

```
ProxyEndpoint PreFlow:
  1. Verify API Key
  2. JSON to XML (backend requires XML SOAP)
  3. XSL Transform (map REST structure to SOAP envelope)

TargetEndpoint → SOAP Backend

ProxyEndpoint PostFlow (Response):
  4. XML to JSON (client expects REST/JSON)
  5. Assign Message (set appropriate content-type headers)
```

### Pattern 4: Backend Routing by Client

```
ProxyEndpoint PreFlow:
  1. Verify API Key
  2. Extract Variables (extract client tier from API Product metadata)

Route Rules:
  - IF client_tier = "premium" → premium-backend.example.com
  - IF client_tier = "standard" → standard-backend.example.com
  - DEFAULT → standard-backend.example.com
```

### Pattern 5: Error Handling and Fault Management

```
Fault Rules:
  1. Raise Fault for 401 (invalid credentials):
     - Status: 401 Unauthorized
     - Body: {"error": "Invalid or expired credentials"}

  2. Raise Fault for 429 (quota exceeded):
     - Status: 429 Too Many Requests
     - Headers: Retry-After: {retry_seconds}
     - Body: {"error": "Rate limit exceeded", "retryAfter": {retry_seconds}}

  3. Default Fault:
     - Status: 500 Internal Server Error
     - Body: {"error": "An unexpected error occurred"}
```

### Pattern 6: CPI iFlow Exposure via API Management

```
ProxyEndpoint:
  1. Verify API Key or OAuth Token
  2. Spike Arrest (50 requests per second)
  3. JSON Threat Protection

Target Endpoint:
  URL: https://<tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/http/<iflow-endpoint>
  Authentication: OAuth 2.0 Client Credentials (to CPI Process Integration Runtime)

PostFlow (Response):
  4. Statistics Collector (track usage per consumer)
  5. Assign Message (sanitize internal headers)
```

---

## Developer Portal / API Business Hub Enterprise

### Overview

The **API Business Hub Enterprise** (Developer Portal) is the self-service portal where application developers discover, explore, and subscribe to APIs.

### Setup and Configuration

**Navigation:** Configure → APIs → Settings → Developer Portal

**Steps:**
1. Enable Developer Portal in Integration Suite settings
2. Configure custom domain (optional): `api-portal.yourcompany.com`
3. Set up Identity Provider (IdP) for developer authentication
4. Configure email notifications (registration, approval, key expiry)
5. Customize branding (logo, colors, footer text)

### Developer Workflow

```
Developer Registration → Browse API Catalog → Subscribe to API Product
                                                       ↓
                               Create Application → Get API Key/OAuth Credentials
                                                       ↓
                               Test in Portal Sandbox → Integrate in Application
```

### Portal Features

| Feature | Description |
|---|---|
| **API Catalog** | Browse all published API Products with documentation |
| **Try Out** | Test API calls directly from portal (Swagger UI) |
| **Application Management** | Register apps, get credentials, manage subscriptions |
| **Analytics** | View personal API usage statistics |
| **Documentation** | Auto-generated docs from OpenAPI specs + custom pages |
| **Rate Plan Visibility** | See quota limits and usage remaining |

### Publishing APIs to Portal

**Steps:**
1. Create API Proxy (see above)
2. Create API Product (bundle APIs + policies)
3. Publish API Product to Developer Portal
4. APIs appear in portal catalog automatically

---

## API Products

### Overview

API Products are bundles of APIs with associated access policies. They are the unit of subscription for developers.

### Product Structure

```
API Product: "Sales Order Management"
├── APIs Included:
│   ├── Sales Order API v1
│   ├── Customer API v1
│   └── Product Catalog API v2
├── Policies:
│   ├── Quota: 10,000 calls/month
│   ├── Spike Arrest: 100 calls/minute
│   └── OAuth 2.0 Required
├── Access Control:
│   ├── Approval Required: Yes
│   └── Allowed Developers: All registered
└── Metadata:
    ├── Description, Documentation URL
    └── Terms of Use, SLA
```

### Creating an API Product

**Navigation:** Configure → APIs → Products → Create

**Steps:**
1. Enter product details:
   - **Title**: `Sales Management APIs`
   - **Description**: Business description
   - **Quota**: 10,000 calls per month
   - **Scope**: Specific scopes for OAuth
2. Add APIs to the product (select from existing proxies)
3. Configure access:
   - **Auto-approve** subscriptions or require manual approval
   - **Restrict** to specific developer groups (optional)
4. Add documentation links and terms of use
5. Publish to Developer Portal

### Rate Plans

| Plan Type | Description | Use Case |
|---|---|---|
| **Free / Trial** | Limited calls, no cost | Developer onboarding |
| **Basic** | Standard quota, basic SLA | Small applications |
| **Premium** | High quota, priority support, SLA | Production apps |
| **Unlimited** | No quota limits | Internal/partner apps |

### Application and Subscription Management

**When a developer subscribes:**
1. Developer creates an **Application** in the portal
2. Selects API Products to subscribe to
3. If auto-approve: credentials generated immediately
4. If manual-approve: admin receives notification, reviews, approves/rejects
5. Developer receives **API Key** and/or **OAuth client credentials**
6. Credentials are tied to the Application (can have multiple products per app)

---

## API Analytics and Monitoring

### Analytics Dashboard

**Navigation:** Monitor → APIs → Analytics

### Key Metrics

| Metric | Description | Alert Threshold |
|---|---|---|
| **Total API Calls** | Number of requests in time period | Unusual spikes/drops |
| **Response Time (Avg/P95/P99)** | Latency percentiles | P95 > 2 seconds |
| **Error Rate** | Percentage of 4xx/5xx responses | > 5% error rate |
| **Traffic by API** | Calls per API proxy | Unbalanced load |
| **Traffic by Developer** | Calls per registered application | Abuse detection |
| **Top APIs** | Most-used APIs ranked | Capacity planning |
| **Top Errors** | Most frequent error codes | Root cause analysis |
| **Bandwidth** | Data transfer in/out | Cost monitoring |

### Custom Analytics with Statistics Collector

```xml
<StatisticsCollector async="false" continueOnError="false" enabled="true">
    <Statistics>
        <Statistic name="orderType" ref="orderType" type="string"/>
        <Statistic name="orderAmount" ref="orderAmount" type="float"/>
        <Statistic name="processingTime" ref="target.response.time" type="integer"/>
    </Statistics>
</StatisticsCollector>
```

### Monitoring Health

**Regular checks:**
1. Review error rates daily — investigate any spike above baseline
2. Monitor P95 latency — increasing trend indicates performance degradation
3. Track quota consumption per developer — proactive outreach before limits
4. Review API product subscription trends — plan capacity
5. Check certificate expiration for mTLS-protected APIs

---

## Key Value Maps (KVM)

### Overview

Key Value Maps (KVM) store key-value pairs accessible from policies at runtime. Use for configuration, feature flags, routing tables, and environment-specific data.

### KVM Scopes

| Scope | Visibility | Use Case |
|---|---|---|
| **Organization** | All API proxies across all environments | Global config, feature flags |
| **Environment** | All proxies in specific environment (dev/prod) | Environment-specific URLs, credentials |
| **API Proxy** | Single API proxy only | Proxy-specific routing, transformation rules |

### Create KVM

**Navigation:** Configure → APIs → Key Value Maps → Create

**Configuration:**
- **Name**: `BackendEndpoints`
- **Scope**: Environment
- **Encrypted**: Yes (for sensitive values)
- **Entries**:
  - `salesorder_url` → `https://s4hana.example.com/sap/opu/odata/sap/API_SALES_ORDER_SRV`
  - `customer_url` → `https://s4hana.example.com/sap/opu/odata/sap/API_BUSINESS_PARTNER`

### Access KVM from Policy

#### Read KVM in Assign Message
```xml
<AssignMessage>
    <AssignVariable>
        <Name>backend_url</Name>
        <Ref>private.BackendEndpoints.salesorder_url</Ref>
    </AssignVariable>
</AssignMessage>
```

#### Read KVM in JavaScript Policy
```javascript
var backendUrl = context.getVariable("private.BackendEndpoints.salesorder_url");
context.setVariable("target.url", backendUrl);
```

### KVM Patterns

**Pattern: Environment-Specific Backend Routing**
```
KVM "BackendEndpoints" (Environment scope):
  DEV:  salesorder_url = https://dev-s4.example.com/...
  QA:   salesorder_url = https://qa-s4.example.com/...
  PROD: salesorder_url = https://prod-s4.example.com/...

Proxy PreFlow:
  → KeyValueMapOperations: Read "salesorder_url" from KVM
  → Set target.url = {kvm_value}
```

**Pattern: Feature Flags**
```
KVM "FeatureFlags" (Organization scope):
  enable_caching = true
  enable_rate_limit = true
  maintenance_mode = false

Proxy PreFlow:
  → Read feature flags from KVM
  → Conditional Flow: IF maintenance_mode = true → Raise Fault 503
```

---

## API Proxy Debugging

### Trace / Debug Tool

**Navigation:** Configure → APIs → Select Proxy → Trace

**Steps:**
1. Open the API proxy in the editor
2. Click **Trace** tab
3. Click **Start Trace Session**
4. Send a request to the API (from browser, Postman, or portal)
5. View the complete request/response flow:
   - Each policy execution with timing
   - Request/response headers and body at each step
   - Variable values set/modified by policies
   - Error details for failed policies

### Reading Trace Results

```
Request Received (client → proxy)
  ↓ Headers, Body, Query Params visible
PreFlow: Verify API Key ✅ (2ms)
  ↓ Variables: apikey, developer.app.name
PreFlow: Quota ✅ (1ms)
  ↓ Variables: ratelimit.*.allowed.count
Conditional Flow: Extract Variables ✅ (1ms)
  ↓ Variables: orderType = "Premium"
Target Request Sent (proxy → backend)
  ↓ Modified headers visible
Target Response Received (backend → proxy)
  ↓ Status: 200, Response body visible
PostFlow: Assign Message ✅ (1ms)
  ↓ Added CORS headers
Response Sent (proxy → client)
  ↓ Final response headers and body
```

### Common Debug Scenarios

| Symptom | Check in Trace | Likely Cause |
|---|---|---|
| 401 Unauthorized | Verify API Key / OAuth policy | Invalid/expired key or token |
| 429 Too Many Requests | Quota / Spike Arrest policy | Rate limit exceeded |
| 500 from backend | Target request/response | Backend error, wrong URL, auth issue |
| Empty response body | PostFlow transformations | XML/JSON conversion error |
| Wrong backend called | Route Rules | Routing condition logic error |
| Slow response | Timing per policy step | Slow Service Callout or backend |

---

## OpenAPI / Swagger Integration

### Import OpenAPI Spec to Create Proxy

**Supported versions:** OpenAPI 3.0, Swagger 2.0

**Steps:**
1. Navigate to Configure → APIs → Create → Import
2. Upload file (JSON or YAML) or provide URL
3. Review imported configuration:
   - Base path extracted from `servers` / `basePath`
   - Resources auto-created from `paths`
   - Schemas documented in portal automatically
4. Configure target endpoint (not in OpenAPI spec)
5. Add security and traffic policies
6. Deploy

### Export API as OpenAPI Spec

**Steps:**
1. Select API proxy → Actions → Export API
2. Choose format: OpenAPI 3.0 JSON / Swagger 2.0
3. Generated spec includes:
   - All resource paths and operations
   - Request/response schemas (if defined)
   - Security schemes
   - Server URLs

### API Specification Best Practices

1. **Always include schemas** — enables portal documentation and validation
2. **Add examples** — helps developers understand expected payloads
3. **Document error responses** — 400, 401, 404, 500 with schemas
4. **Use tags** — organize operations by business domain
5. **Version in spec** — match API proxy version

---

## API Versioning Strategies

### Strategy 1: URL Path Versioning (Recommended)

```
/api/v1/salesorders → API Proxy: SalesOrder_v1 → Backend v1
/api/v2/salesorders → API Proxy: SalesOrder_v2 → Backend v2
```

**Pros:** Clear, cacheable, easy to route
**Cons:** URL changes between versions

### Strategy 2: Header Versioning

```
GET /api/salesorders
Header: X-API-Version: 2

Conditional Flow:
  IF header.X-API-Version = "2" → Route to v2 backend
  ELSE → Route to v1 backend (default)
```

**Pros:** Clean URLs
**Cons:** Hidden version, harder to debug

### Strategy 3: Query Parameter Versioning

```
GET /api/salesorders?version=2
```

**Pros:** Simple implementation
**Cons:** Pollutes query parameters, caching challenges

### Deprecation Strategy

1. **Announce deprecation** — add `Sunset` header with date
2. **Deprecation period** — 6-12 months minimum for production APIs
3. **Monitor v1 usage** — track remaining consumers via analytics
4. **Communicate** — email developers subscribed to v1 products
5. **Retire** — disable v1 proxy after deprecation period

```xml
<!-- Add Sunset header in PostFlow -->
<AssignMessage>
    <Set>
        <Headers>
            <Header name="Sunset">Sat, 01 Jun 2025 00:00:00 GMT</Header>
            <Header name="Deprecation">true</Header>
            <Header name="Link">&lt;/api/v2/salesorders&gt;; rel="successor-version"</Header>
        </Headers>
    </Set>
</AssignMessage>
```

---

## Custom Policy Scripting

### JavaScript Policy

```javascript
// js/transformResponse.js
var response = JSON.parse(context.getVariable("response.content"));

// Add metadata
response.metadata = {
    apiVersion: "2.0",
    timestamp: new Date().toISOString(),
    requestId: context.getVariable("messageid")
};

// Remove internal fields
delete response._internalId;
delete response._debugInfo;

context.setVariable("response.content", JSON.stringify(response));
context.setVariable("response.header.Content-Type", "application/json");
```

**Policy XML:**
```xml
<Javascript async="false" continueOnError="false" enabled="true" timeLimit="200">
    <ResourceURL>jsc://transformResponse.js</ResourceURL>
</Javascript>
```

### Python Script Policy

```python
# py/validate_payload.py
import json

request_body = flow.getVariable("request.content")
data = json.loads(request_body)

errors = []
if not data.get("orderId"):
    errors.append("orderId is required")
if not data.get("items") or len(data["items"]) == 0:
    errors.append("At least one item is required")

if errors:
    flow.setVariable("validation.errors", json.dumps(errors))
    flow.setVariable("validation.isValid", "false")
else:
    flow.setVariable("validation.isValid", "true")
```

---

## Cloud Integration to API Management Bridge

### Connecting CPI and API Management

When both Cloud Integration and API Management are activated in the same Integration Suite tenant, you can expose iFlow HTTP endpoints as managed APIs.

**Steps:**
1. Design and deploy iFlow with HTTP sender adapter in Cloud Integration
2. Navigate to API Management → Configure → APIs → Create
3. Set Target Endpoint to the iFlow HTTP endpoint URL:
   ```
   https://<tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/http/<endpoint-path>
   ```
4. Configure authentication to CPI (OAuth 2.0 Client Credentials with Process Integration Runtime)
5. Add consumer-facing policies (API key, quota, CORS)
6. Publish to Developer Portal

### Architecture Pattern

```
External Consumer → API Management (API Key + Quota + CORS)
                          ↓ (OAuth 2.0 to CPI)
                    Cloud Integration (iFlow processing)
                          ↓
                    Backend System (S/4HANA, DB, etc.)
```

---

## Best Practices

### Design

1. **One responsibility per proxy** — don't mix unrelated APIs in one proxy
2. **Use API Products** to bundle related APIs for consumers
3. **Version from day one** — start with `/v1/` in the base path
4. **Design API-first** — create OpenAPI spec before building proxy
5. **Use meaningful resource names** — `/salesorders` not `/data` or `/api`

### Security

1. **Always require authentication** — API Key minimum, OAuth 2.0 preferred
2. **Layer security** — API Key + OAuth + IP whitelisting for sensitive APIs
3. **Use Spike Arrest AND Quota** — Spike Arrest for burst protection, Quota for monthly limits
4. **Enable JSON/XML Threat Protection** — prevent payload attacks
5. **Don't expose internal details** — sanitize error messages and headers in PostFlow

### Performance

1. **Cache responses** — use Response Cache for read-heavy APIs
2. **Minimize Service Callouts** — each adds latency
3. **Use KVM over Service Callout** for static configuration
4. **Set appropriate timeouts** — prevent long-running requests from blocking
5. **Enable compression** — gzip for large payloads

### Operations

1. **Monitor analytics daily** — catch issues before users report them
2. **Set up alerts** for error rate spikes and latency degradation
3. **Track API product adoption** — understand which APIs are used
4. **Review and rotate API keys** periodically
5. **Document changes** — maintain changelog per API version

---

**Next Steps:**
- See [security.md](security.md) for OAuth setup and certificate management
- See [cloud-integration.md](cloud-integration.md) for iFlow design behind API proxies
- See [postman-testing.md](postman-testing.md) for testing API proxies
- See [troubleshooting.md](troubleshooting.md) for API Management error resolution

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about API Management policies, Developer Portal setup, API analytics, and API proxy debugging.
