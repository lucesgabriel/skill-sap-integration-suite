# Postman Collection Generation for SAP Integration Suite

Guide for generating Postman Collection v2.1 JSON files to test iFlows, API proxies, OData services, and other SAP Integration Suite endpoints.

## Table of Contents

- [Collection Structure](#collection-structure)
- [Authentication Patterns](#authentication-patterns)
- [iFlow Testing](#iflow-testing)
- [API Management Testing](#api-management-testing)
- [OData Service Testing](#odata-service-testing)
- [Environment Template](#environment-template)
- [Generation Rules](#generation-rules)

---

## Collection Structure

Always generate Postman Collection v2.1 format. Structure:

```json
{
  "info": {
    "name": "{{collection_name}}",
    "_postman_id": "{{uuid}}",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    "description": "Auto-generated collection for SAP Integration Suite testing"
  },
  "auth": { ... },
  "variable": [ ... ],
  "item": [ ... ]
}
```

### Folder Organization

Organize requests in folders by function:
1. **Authentication** - Token requests (if OAuth)
2. **Health/Connectivity** - Endpoint availability checks
3. **Main Operations** - Core business requests (CRUD, trigger iFlow, etc.)
4. **Error Scenarios** - Negative tests (invalid payload, missing auth, wrong content type)

---

## Authentication Patterns

### OAuth 2.0 Client Credentials (Standard for CPI iFlows)

This is the most common pattern. The Process Integration Runtime service key provides the credentials.

**Environment variables needed:**
```json
{
  "key": "cpi_token_url",
  "value": "https://{{subaccount}}.authentication.{{region}}.hana.ondemand.com/oauth/token",
  "type": "default"
},
{
  "key": "cpi_client_id",
  "value": "",
  "type": "secret"
},
{
  "key": "cpi_client_secret",
  "value": "",
  "type": "secret"
},
{
  "key": "cpi_base_url",
  "value": "https://{{tenant_id}}.it-cpi-rt.cfapps.{{region}}.hana.ondemand.com",
  "type": "default"
}
```

**Token request:**
```json
{
  "name": "Get OAuth Token",
  "request": {
    "method": "POST",
    "header": [
      { "key": "Content-Type", "value": "application/x-www-form-urlencoded" }
    ],
    "body": {
      "mode": "urlencoded",
      "urlencoded": [
        { "key": "grant_type", "value": "client_credentials" },
        { "key": "client_id", "value": "{{cpi_client_id}}" },
        { "key": "client_secret", "value": "{{cpi_client_secret}}" }
      ]
    },
    "url": { "raw": "{{cpi_token_url}}", "host": ["{{cpi_token_url}}"] }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "var jsonData = pm.response.json();",
          "pm.environment.set('cpi_access_token', jsonData.access_token);",
          "pm.test('Token obtained successfully', function() {",
          "    pm.response.to.have.status(200);",
          "    pm.expect(jsonData.access_token).to.not.be.empty;",
          "});",
          "pm.test('Token type is Bearer', function() {",
          "    pm.expect(jsonData.token_type).to.eql('bearer');",
          "});"
        ]
      }
    }
  ]
}
```

**Auth header for subsequent requests (collection-level):**
```json
{
  "auth": {
    "type": "bearer",
    "bearer": [
      { "key": "token", "value": "{{cpi_access_token}}", "type": "string" }
    ]
  }
}
```

### Basic Authentication

For simple scenarios or Cloud Connector proxied endpoints:

```json
{
  "auth": {
    "type": "basic",
    "basic": [
      { "key": "username", "value": "{{username}}", "type": "string" },
      { "key": "password", "value": "{{password}}", "type": "string" }
    ]
  }
}
```

### API Key (API Management)

```json
{
  "auth": {
    "type": "apikey",
    "apikey": [
      { "key": "key", "value": "APIKey", "type": "string" },
      { "key": "value", "value": "{{api_key}}", "type": "string" },
      { "key": "in", "value": "header", "type": "string" }
    ]
  }
}
```

### CSRF Token (for SAP systems that require it)

Pre-request script to fetch CSRF token:

```json
{
  "listen": "prerequest",
  "script": {
    "exec": [
      "pm.sendRequest({",
      "    url: pm.variables.get('cpi_base_url') + '/api/v1/',",
      "    method: 'GET',",
      "    header: {",
      "        'X-CSRF-Token': 'Fetch',",
      "        'Authorization': 'Bearer ' + pm.variables.get('cpi_access_token')",
      "    }",
      "}, function(err, res) {",
      "    if (!err) {",
      "        pm.environment.set('csrf_token', res.headers.get('X-CSRF-Token'));",
      "    }",
      "});"
    ]
  }
}
```

---

## iFlow Testing

### HTTP Sender Endpoint

The most common scenario. iFlow exposes an HTTP endpoint.

**URL pattern:** `{{cpi_base_url}}/http/{{iflow_endpoint_path}}`

**Example: POST JSON to iFlow**
```json
{
  "name": "Trigger iFlow - JSON Payload",
  "request": {
    "method": "POST",
    "header": [
      { "key": "Content-Type", "value": "application/json" },
      { "key": "SAP_ApplicationID", "value": "{{test_correlation_id}}" }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n  \"orderId\": \"PO-2025-001\",\n  \"material\": \"MAT-100\",\n  \"quantity\": 10,\n  \"plant\": \"1000\"\n}"
    },
    "url": {
      "raw": "{{cpi_base_url}}/http/{{iflow_endpoint}}",
      "host": ["{{cpi_base_url}}"],
      "path": ["http", "{{iflow_endpoint}}"]
    }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('iFlow executed successfully', function() {",
          "    pm.response.to.have.status(200);",
          "});",
          "pm.test('Response time < 30s', function() {",
          "    pm.expect(pm.response.responseTime).to.be.below(30000);",
          "});",
          "pm.test('Response body is valid', function() {",
          "    var body = pm.response.text();",
          "    pm.expect(body).to.not.be.empty;",
          "});"
        ]
      }
    }
  ]
}
```

**Example: POST XML to iFlow**
```json
{
  "name": "Trigger iFlow - XML Payload",
  "request": {
    "method": "POST",
    "header": [
      { "key": "Content-Type", "value": "application/xml" },
      { "key": "SOAPAction", "value": "{{soap_action}}" }
    ],
    "body": {
      "mode": "raw",
      "raw": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Order>\n  <OrderId>PO-2025-001</OrderId>\n  <Material>MAT-100</Material>\n  <Quantity>10</Quantity>\n  <Plant>1000</Plant>\n</Order>"
    },
    "url": {
      "raw": "{{cpi_base_url}}/http/{{iflow_endpoint}}",
      "host": ["{{cpi_base_url}}"],
      "path": ["http", "{{iflow_endpoint}}"]
    }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('Status 200', function() { pm.response.to.have.status(200); });",
          "pm.test('XML response', function() {",
          "    pm.expect(pm.response.headers.get('Content-Type')).to.include('xml');",
          "});"
        ]
      }
    }
  ]
}
```

### iFlow with CSRF Token (OData/REST receivers)

When iFlow is triggered via OData API endpoint:
```json
{
  "name": "Trigger iFlow via OData API",
  "event": [
    {
      "listen": "prerequest",
      "script": {
        "exec": [
          "// Fetch CSRF token first",
          "pm.sendRequest({",
          "    url: pm.variables.get('cpi_base_url') + '/api/v1/',",
          "    method: 'GET',",
          "    header: {",
          "        'X-CSRF-Token': 'Fetch',",
          "        'Authorization': 'Bearer ' + pm.variables.get('cpi_access_token')",
          "    }",
          "}, function(err, res) {",
          "    pm.environment.set('csrf_token', res.headers.get('X-CSRF-Token'));",
          "});"
        ]
      }
    },
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('Accepted', function() {",
          "    pm.expect(pm.response.code).to.be.oneOf([200, 201, 202]);",
          "});"
        ]
      }
    }
  ],
  "request": {
    "method": "POST",
    "header": [
      { "key": "Content-Type", "value": "application/json" },
      { "key": "X-CSRF-Token", "value": "{{csrf_token}}" }
    ],
    "body": { "mode": "raw", "raw": "{}" },
    "url": { "raw": "{{cpi_base_url}}/api/v1/{{resource}}" }
  }
}
```

### Monitoring Requests (Integration Content API)

Add these to verify iFlow execution after triggering:

```json
{
  "name": "Check Message Processing Logs",
  "request": {
    "method": "GET",
    "header": [
      { "key": "Accept", "value": "application/json" }
    ],
    "url": {
      "raw": "{{cpi_base_url}}/api/v1/MessageProcessingLogs?$filter=IntegrationFlowName eq '{{iflow_name}}'&$top=5&$orderby=LogStart desc&$format=json",
      "host": ["{{cpi_base_url}}"],
      "path": ["api", "v1", "MessageProcessingLogs"]
    }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('Logs retrieved', function() {",
          "    pm.response.to.have.status(200);",
          "    var logs = pm.response.json().d.results;",
          "    pm.expect(logs.length).to.be.greaterThan(0);",
          "    console.log('Latest status: ' + logs[0].Status);",
          "    console.log('Message GUID: ' + logs[0].MessageGuid);",
          "});"
        ]
      }
    }
  ]
},
{
  "name": "Get MPL Attachments",
  "request": {
    "method": "GET",
    "header": [ { "key": "Accept", "value": "application/json" } ],
    "url": {
      "raw": "{{cpi_base_url}}/api/v1/MessageProcessingLogs('{{message_guid}}')/Attachments",
      "host": ["{{cpi_base_url}}"],
      "path": ["api", "v1", "MessageProcessingLogs('{{message_guid}}')", "Attachments"]
    }
  }
},
{
  "name": "Get iFlow Runtime Status",
  "request": {
    "method": "GET",
    "header": [ { "key": "Accept", "value": "application/json" } ],
    "url": {
      "raw": "{{cpi_base_url}}/api/v1/IntegrationRuntimeArtifacts('{{iflow_id}}')",
      "host": ["{{cpi_base_url}}"],
      "path": ["api", "v1", "IntegrationRuntimeArtifacts('{{iflow_id}}')"]
    }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('iFlow is deployed', function() {",
          "    var data = pm.response.json().d;",
          "    pm.expect(data.Status).to.eql('STARTED');",
          "});"
        ]
      }
    }
  ]
}
```

---

## API Management Testing

### API Proxy with API Key

```json
{
  "name": "Call API Proxy",
  "request": {
    "method": "GET",
    "header": [
      { "key": "APIKey", "value": "{{api_key}}" },
      { "key": "Accept", "value": "application/json" }
    ],
    "url": {
      "raw": "{{apim_proxy_url}}/{{resource_path}}?$top=10",
      "host": ["{{apim_proxy_url}}"],
      "path": ["{{resource_path}}"],
      "query": [ { "key": "$top", "value": "10" } ]
    }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('API Proxy responds', function() {",
          "    pm.response.to.have.status(200);",
          "});",
          "pm.test('Rate limit headers present', function() {",
          "    pm.expect(pm.response.headers.has('X-RateLimit-Remaining')).to.be.true;",
          "});"
        ]
      }
    }
  ]
}
```

### API Proxy with OAuth (API Management + CPI backend)

```json
{
  "name": "Call API Proxy (OAuth)",
  "request": {
    "auth": {
      "type": "bearer",
      "bearer": [ { "key": "token", "value": "{{apim_access_token}}" } ]
    },
    "method": "POST",
    "header": [
      { "key": "Content-Type", "value": "application/json" }
    ],
    "body": { "mode": "raw", "raw": "{{request_body}}" },
    "url": { "raw": "{{apim_proxy_url}}/{{resource_path}}" }
  }
}
```

---

## OData Service Testing

### Standard OData CRUD Pattern

Generate these requests for OData-based integrations:

**GET (Read):**
```json
{ "name": "GET EntitySet", "request": { "method": "GET", "url": { "raw": "{{base_url}}/{{entity_set}}?$top=10&$format=json" } } }
```

**GET by Key:**
```json
{ "name": "GET Entity by Key", "request": { "method": "GET", "url": { "raw": "{{base_url}}/{{entity_set}}('{{entity_key}}')?$format=json" } } }
```

**POST (Create) with CSRF:**
```json
{
  "name": "POST Create Entity",
  "event": [
    {
      "listen": "prerequest",
      "script": {
        "exec": [
          "pm.sendRequest({ url: pm.variables.get('base_url') + '/', method: 'GET',",
          "    header: { 'X-CSRF-Token': 'Fetch', 'Authorization': 'Bearer ' + pm.variables.get('access_token') }",
          "}, function(err, res) { pm.environment.set('csrf_token', res.headers.get('X-CSRF-Token')); });"
        ]
      }
    }
  ],
  "request": {
    "method": "POST",
    "header": [
      { "key": "Content-Type", "value": "application/json" },
      { "key": "X-CSRF-Token", "value": "{{csrf_token}}" }
    ],
    "body": { "mode": "raw", "raw": "{{create_payload}}" },
    "url": { "raw": "{{base_url}}/{{entity_set}}" }
  }
}
```

---

## Environment Template

Always generate a companion environment JSON:

```json
{
  "id": "{{uuid}}",
  "name": "SAP CPI - {{environment_name}}",
  "values": [
    { "key": "cpi_base_url", "value": "https://TENANT.it-cpi-rt.cfapps.REGION.hana.ondemand.com", "type": "default", "enabled": true },
    { "key": "cpi_token_url", "value": "https://SUBACCOUNT.authentication.REGION.hana.ondemand.com/oauth/token", "type": "default", "enabled": true },
    { "key": "cpi_client_id", "value": "", "type": "secret", "enabled": true },
    { "key": "cpi_client_secret", "value": "", "type": "secret", "enabled": true },
    { "key": "cpi_access_token", "value": "", "type": "secret", "enabled": true },
    { "key": "iflow_endpoint", "value": "", "type": "default", "enabled": true },
    { "key": "iflow_name", "value": "", "type": "default", "enabled": true },
    { "key": "iflow_id", "value": "", "type": "default", "enabled": true },
    { "key": "csrf_token", "value": "", "type": "default", "enabled": true },
    { "key": "message_guid", "value": "", "type": "default", "enabled": true },
    { "key": "apim_proxy_url", "value": "", "type": "default", "enabled": true },
    { "key": "api_key", "value": "", "type": "secret", "enabled": true },
    { "key": "test_correlation_id", "value": "TEST-{{$timestamp}}", "type": "default", "enabled": true }
  ],
  "_postman_variable_scope": "environment"
}
```

**Region values:** `eu10`, `eu20`, `us10`, `us20`, `ap10`, `ap11`, `jp10`, `br10`

**Where to find credentials:**
- BTP Cockpit → Instances and Subscriptions → Process Integration Runtime → Service Key
- Fields: `clientid`, `clientsecret`, `tokenurl`, `url`

---

## Generation Rules

When generating a Postman collection, follow these rules:

1. **Always produce two files**: Collection JSON + Environment JSON
2. **Use Postman Collection v2.1 schema** (`https://schema.getpostman.com/json/collection/v2.1.0/collection.json`)
3. **Auth at collection level** when all requests share the same auth; override at request level when mixed
4. **Include test scripts** in every request: status code check + basic body validation
5. **Use environment variables** for all URLs, credentials, and dynamic values (never hardcode)
6. **Add descriptive names** to requests (e.g., "Trigger Order Processing iFlow" not "POST Request 1")
7. **Include error test requests**: wrong content-type, empty body, invalid auth, to validate error handling
8. **Add pre-request script for OAuth** token refresh if token may expire during testing session:

```javascript
// Auto-refresh token if expired (add to collection pre-request)
var tokenExpiry = pm.environment.get('token_expiry');
if (!tokenExpiry || new Date() > new Date(tokenExpiry)) {
    pm.sendRequest({
        url: pm.environment.get('cpi_token_url'),
        method: 'POST',
        header: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: {
            mode: 'urlencoded',
            urlencoded: [
                { key: 'grant_type', value: 'client_credentials' },
                { key: 'client_id', value: pm.environment.get('cpi_client_id') },
                { key: 'client_secret', value: pm.environment.get('cpi_client_secret') }
            ]
        }
    }, function(err, res) {
        if (!err) {
            var json = res.json();
            pm.environment.set('cpi_access_token', json.access_token);
            var expiry = new Date();
            expiry.setSeconds(expiry.getSeconds() + json.expires_in - 60);
            pm.environment.set('token_expiry', expiry.toISOString());
        }
    });
}
```

9. **For file-based payloads** (CSV, flat file), use `formdata` body mode with file reference
10. **For SOAP iFlows**, include proper SOAP envelope in the raw body with `text/xml` content type

### Naming Convention

```
Collection: SAP CPI - {Project/iFlow Name} Tests
Folders:    01_Authentication, 02_Health_Check, 03_Main_Operations, 04_Error_Scenarios, 05_Monitoring
Requests:   {HTTP_METHOD} {Descriptive Name}
```

### Adapting to User Context

When generating, ask or infer:
- **Endpoint URL pattern**: Does the user know their tenant URL? If not, use placeholders.
- **Payload format**: JSON, XML, CSV, flat file? Generate matching sample payload.
- **Auth method**: Check service key type. Default to OAuth 2.0 Client Credentials for CPI.
- **Test assertions**: What does "success" look like? HTTP 200? Specific response field?
- **Monitoring needs**: Include MPL check requests if the user wants end-to-end validation.

---

## SAP API Business Hub Sandbox Testing

When testing against Sandbox APIs (api.sap.com), use this specific pattern:

### Sandbox Authentication

Sandbox APIs use a simple **APIKey header** (not OAuth). This is different from CPI authentication.

**Header format:**
```
APIKey: <your-api-key>
```

⚠️ The header name must be exactly `APIKey` (capital A and K). Common mistakes:
- ❌ `apikey` — will return 403
- ❌ `Api-Key` — will return 403
- ❌ `api_key` — will return 403
- ✅ `APIKey` — correct

**Where to get the key:**
1. Go to [api.sap.com](https://api.sap.com)
2. Login with SAP account
3. Click Settings (⚙️ icon, top right)
4. Click **Show API Key** → Copy
5. If expired, click **Regenerate**

### Sandbox Collection Variables

```json
{
  "variable": [
    { "key": "sandbox_base_url", "value": "https://sandbox.api.sap.com/s4hanacloud/sap/opu/odata/sap/{API_SERVICE_NAME}" },
    { "key": "api_key", "value": "<your-api-key>" }
  ]
}
```

### Sandbox Request Template

```json
{
  "name": "GET EntitySet - No Filter",
  "request": {
    "method": "GET",
    "header": [
      { "key": "APIKey", "value": "{{api_key}}" },
      { "key": "Accept", "value": "application/json" }
    ],
    "url": {
      "raw": "{{sandbox_base_url}}/{EntitySet}?$top=10&$format=json",
      "host": ["{{sandbox_base_url}}"],
      "path": ["{EntitySet}"],
      "query": [
        { "key": "$top", "value": "10" },
        { "key": "$format", "value": "json" }
      ]
    }
  }
}
```

### Dual-Mode Collection Pattern (Sandbox + iFlow)

When an iFlow proxies a Sandbox API, generate **both test paths** in the same collection:

```
📁 01_Direct_Sandbox_API        ← Tests the API directly (validates API works)
    GET $metadata
    GET EntitySet - No Filter
    GET EntitySet - With Filter
    GET EntitySet - JSON format
📁 02_CPI_Authentication        ← Gets OAuth token for CPI tenant
    POST Get OAuth Token
📁 03_iFlow_Proxy_Tests         ← Tests via the iFlow (validates iFlow works)
    GET via iFlow - No Filter
    GET via iFlow - Filter by Field1
    GET via iFlow - Filter by Field2
    GET via iFlow - Combined Filters
📁 04_Error_Scenarios           ← Validates error handling
    GET Invalid API Key (expect 401/403)
    GET Non-existent Entity (expect 404)
    GET iFlow without Token (expect 401)
📁 05_Monitoring                ← Checks iFlow runtime status
    GET IntegrationRuntimeArtifacts
    GET MessageProcessingLogs
```

This dual-mode approach helps isolate issues: if Sandbox works but iFlow doesn't, the problem is in the iFlow configuration (Allowed Headers, Content Modifier, Script, etc.).

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for iFlow design and endpoint configuration
- See [adapters.md](adapters.md) for adapter-specific authentication and URL formats
- See [security.md](security.md) for OAuth2, API Key, and certificate authentication setup
- See [api-management.md](api-management.md) for API proxy testing and Developer Portal
- See [troubleshooting.md](troubleshooting.md) for HTTP error codes and adapter error resolution
- See [simulation-testing.md](simulation-testing.md) for design-time iFlow testing before deployment
- See [operations-monitoring.md](operations-monitoring.md) for OData API endpoints to monitor iFlow execution

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about API testing strategies, Postman integration, and debugging techniques.
