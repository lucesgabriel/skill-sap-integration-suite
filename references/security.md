# Security Reference

Authentication, encryption, certificates, and security best practices for SAP Cloud Integration.

## Authentication Methods

### 1. Basic Authentication
- **Use For:** Simple HTTP/SOAP endpoints, dev/test environments
- **Configuration:** Username + Password in User Credentials artifact
- **Security Level:** ⚠️ Low (base64 encoded, not encrypted)
- **Best Practice:** Only use over HTTPS

### 2. OAuth 2.0
- **Flows:** Client Credentials, Authorization Code, SAML Bearer
- **Use For:** Modern REST APIs, cloud services
- **Configuration:** OAuth2 Credentials artifact (Client ID, Secret, Token URL)
- **Security Level:** ✅ High (tokens expire, refresh mechanism)

#### Client Credentials Flow
```
iFlow → Token Endpoint (POST with client_id + client_secret)
      ← Access Token
iFlow → API (with Bearer token)
```

### 3. Certificate-Based (X.509)
- **Use For:** High-security integrations, client certificates
- **Configuration:** Deploy certificate to Keystore
- **Security Level:** ✅ Very High (mutual TLS)

### 4. API Key
- **Use For:** Third-party APIs
- **Configuration:** Store in Secure Parameter or User Credentials
- **Best Practice:** Don't expose in headers visible in trace

### 5. SAML Assertion
- **Use For:** SSO scenarios, principal propagation
- **Security Level:** ✅ High (identity federation)

---

## Keystore Management

### Upload Certificate to Keystore

**Steps:**
1. Navigate to **Monitor → Manage Security → Keystore**
2. Click **Add → Certificate**
3. Upload .cer or .crt file
4. Give it an alias (e.g., `partner-cert`)

**Use Cases:**
- Partner public keys (for encryption)
- CA certificates (for trust chain)
- Server certificates (for HTTPS)

### Upload Key Pair

**Steps:**
1. Generate key pair (e.g., using OpenSSL)
2. Create .p12 or .pfx file
3. Upload to Keystore with password
4. Give it an alias (e.g., `my-signing-key`)

**Use Cases:**
- Client certificates (mutual TLS)
- Signing messages (digital signatures)
- Decrypting incoming messages

---

## User Credentials Artifact

**Deploy Credentials:**
1. Navigate to **Monitor → Manage Security → User Credentials**
2. Click **Add**
3. Enter Name (e.g., `SAP_SYSTEM_USER`)
4. Enter Username and Password
5. Deploy

**Reference in Adapter:**
```
HTTP Receiver Adapter:
- Authentication: Basic
- Credential Name: SAP_SYSTEM_USER
```

---

## OAuth2 Credentials Artifact

**Deploy OAuth2 Credentials:**
1. Navigate to **Monitor → Manage Security → OAuth2 Credentials**
2. Click **Add**
3. Enter Name (e.g., `SALESFORCE_OAUTH`)
4. Enter Client ID, Client Secret, Token Service URL
5. Deploy

**Use in HTTP Adapter:**
```
HTTP Receiver:
- Authentication: OAuth2 Client Credentials
- Credential Name: SALESFORCE_OAUTH
```

---

## Encryption/Decryption

### PGP Encryption

**Encryptor Step:**
```
Encryptor:
- Algorithm: PGP Encryption
- Public Key Alias: partner-public-key (from Keystore)
- Compression: ZIP
```

**Decryptor Step:**
```
Decryptor:
- Algorithm: PGP Decryption
- Private Key Alias: my-private-key (from Keystore)
```

### PKCS#7/CMS Encryption

**Encryptor:**
```
- Algorithm: PKCS#7/CMS Enveloped Data
- Receiver Public Key: partner-cert
- Content Encryption Algorithm: AES256-CBC
```

**Decryptor:**
```
- Algorithm: PKCS#7/CMS Enveloped Data
- Private Key: my-private-key
```

---

## Digital Signatures

### Sign Message

**Signer Step:**
```
Signer:
- Signing Algorithm: PKCS#7/CMS Signed Data
- Private Key: my-signing-key
- Include Certificates: Yes (for verification)
```

### Verify Signature

**Verifier Step:**
```
Verifier:
- Verification Algorithm: PKCS#7/CMS Signed Data
- Public Key: partner-cert
- Fail on Invalid: Yes
```

---

## WS-Security (for SOAP)

### Username Token

```
SOAP Receiver Adapter:
- WS-Security Configuration: Username Token
- Username: ${secure::username}
- Password: ${secure::password}
```

### Sign and Encrypt SOAP Message

```
SOAP Adapter:
- Sign Message: Yes
  - Private Key: my-signing-key
- Encrypt Message: Yes
  - Public Key: partner-cert
```

---

## Secure Parameter Store

**Access in Groovy Script:**

```groovy
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.ITApiFactory

def service = ITApiFactory.getService(SecureStoreService.class, null)
def credential = service.getUserCredential("API_KEY_CREDENTIAL")

if (credential != null) {
    def apiKey = new String(credential.getPassword())
    // Use apiKey in HTTP call (don't store in header)
}
```

**Never:**
```groovy
// ❌ Don't expose in headers (visible in trace)
message.setHeader("API-Key", apiKey)

// ✅ Use directly in HTTP call without header
```

---

## Best Practices

### 1. Credential Rotation
- Rotate passwords every 90 days
- Update OAuth2 client secrets regularly
- Renew certificates before expiration

### 2. Principle of Least Privilege
- Grant minimum permissions needed
- Use separate credentials per integration
- Restrict access to keystore/credentials

### 3. Secure Transport
- Always use HTTPS (not HTTP)
- Always use SFTP (not FTP)
- Enable TLS 1.2+ (disable older versions)

### 4. Data Protection
- Encrypt sensitive data at rest (Data Store)
- Encrypt sensitive payloads in transit
- Don't log sensitive data (passwords, API keys, PII)

### 5. Audit and Monitoring
- Review security material access logs
- Monitor failed authentication attempts
- Set up alerts for security events

---

## Common Security Scenarios

### Scenario 1: Call REST API with OAuth2

```
iFlow:
1. OAuth2 Token Call (automatic via adapter)
2. HTTP Call with Bearer token
3. Process response
```

Configuration:
```
HTTP Receiver:
- Authentication: OAuth2 Client Credentials
- Credential Name: API_OAUTH
```

### Scenario 2: AS2 with Signing and Encryption

```
AS2 Receiver:
- Sign Message: Yes (my-signing-key)
- Encrypt Message: Yes (partner-public-key)
- Request MDN: Yes
```

### Scenario 3: Connect to On-Premise SAP

```
RFC Adapter:
- Proxy: On-Premise (via Cloud Connector)
- Location ID: (if multiple connectors)
- Authentication: Basic (SAP user credentials)
```

---

## Role-Based Access Control (RBAC)

### Integration Suite Roles

| Role Collection | Permissions | Typical User |
|---|---|---|
| `PI_Administrator` | Full access: deploy, monitor, manage security materials, configure tenant | Operations lead |
| `PI_Business_Expert` | Design and deploy iFlows, view monitoring | Integration developer |
| `PI_Read_Only` | View-only access to monitoring and design | Support engineer |
| `PI_Integration_Developer` | Design iFlows, manage drafts, limited deployment | Junior developer |
| `AuthGroup.API.Admin` | Full API Management access | API admin |
| `AuthGroup.API.ApplicationDeveloper` | Subscribe to API products, create applications | API consumer |

### Assigning Roles

1. Navigate to **BTP Cockpit → Subaccount → Security → Role Collections**
2. Select the role collection (e.g., `PI_Business_Expert`)
3. Click **Edit → Add User**
4. Enter email/user ID from Identity Provider
5. Save

**Best Practice:** Create custom role collections combining standard roles for your organization's needs. Never assign `PI_Administrator` broadly.

---

## Principal Propagation

Principal Propagation forwards the identity of the original caller through the integration flow to the backend system, enabling user-level authorization.

### Flow: Browser → BTP → Cloud Connector → On-Premise SAP

```
1. User authenticates via IdP (e.g., SAP IAS, Azure AD)
2. SAML/JWT token arrives at CPI sender adapter
3. CPI extracts user principal from token
4. CPI generates short-lived X.509 certificate for the user
5. Cloud Connector maps X.509 certificate to SAP backend user (SNC or SSL)
6. Backend SAP system authorizes based on the propagated user identity
```

### Configuration Steps

1. **BTP Subaccount:** Configure trust with Identity Provider (SAML 2.0 or OIDC)
2. **Cloud Integration iFlow:** Use sender adapter with authentication = OAuth2 SAML Bearer or Client Certificate
3. **Cloud Connector:**
   - Enable Principal Propagation (Configuration → On Premise → Principal Propagation)
   - Configure system certificate for SNC
   - Map Subject Pattern to SAP user (e.g., `CN=${email}`)
4. **SAP Backend (ABAP):**
   - Configure SNC (transaction STRUST)
   - Map external certificates to SAP users (transaction CERTRULE or USREXTID)

See [cloud-connector.md](cloud-connector.md) for Cloud Connector principal propagation setup.

---

## Data Protection in Trace and Logs

### Masking Sensitive Data

When trace is enabled, full payloads are visible. Prevent exposure of sensitive fields:

```groovy
// Mask sensitive fields before logging
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def body = message.getBody(String.class)
def json = new JsonSlurper().parseText(body)

// Mask credit card, SSN, etc.
if (json.creditCard) json.creditCard = "****" + json.creditCard[-4..-1]
if (json.ssn) json.ssn = "***-**-" + json.ssn[-4..-1]
if (json.password) json.password = "********"

def maskedBody = JsonOutput.toJson(json)
messageLog.addAttachmentAsString("MaskedPayload", maskedBody, "application/json")
```

### Headers to Never Log

- `Authorization` (Bearer tokens)
- `APIKey` / `X-API-Key`
- `Cookie` / `Set-Cookie`
- `X-CSRF-Token`
- Any custom header containing credentials

**Best Practice:** Use Externalized Parameters (`{{param}}` syntax) for all credential references so they are not visible in the iFlow design view to unauthorized users.

---

## Externalized Parameters for Secrets

Externalized parameters allow credential references and endpoint URLs to be configured per environment without modifying the iFlow.

### Define in iFlow
```
HTTP Receiver Adapter:
- Address: {{endpoint_url}}
- Credential Name: {{credential_name}}
```

### Configure at Deployment
1. Deploy iFlow → **Configure** button appears
2. Set parameter values per environment:
   - DEV: `endpoint_url = https://sandbox.api.sap.com/...`
   - PRD: `endpoint_url = https://prod.api.sap.com/...`
3. Externalized parameters are stored securely and not visible in iFlow source

### Secure Parameter Store

For values that must be encrypted at rest (API keys, tokens):

1. Navigate to **Monitor → Manage Security → Secure Parameters**
2. Click **Add**
3. Enter Name and Value (encrypted at rest)
4. Reference in iFlow: `${secure:parameterName}`

---

## Certificate Chain of Trust

### How CPI Validates Server Certificates

```
Server presents:  [Server Cert] → [Intermediate CA] → [Root CA]

CPI Keystore must contain:
  - Root CA certificate (trusted anchor)
  - Intermediate CA certificate (if not included in server response)

CPI does NOT need the server's leaf certificate — only the CA chain.
```

### Adding CA Certificates

1. Obtain Root CA and Intermediate CA certificates (.cer/.crt/.pem)
2. Navigate to **Monitor → Manage Security → Keystore**
3. Click **Add → Certificate**
4. Upload each CA certificate with descriptive alias:
   - `digicert-root-ca`
   - `digicert-intermediate-g2`
5. CPI automatically trusts any server certificate signed by these CAs

### SAP-Managed vs Customer-Managed Keystore

| Keystore | Managed By | Contains | Modifiable |
|---|---|---|---|
| `sap_cloudintegrationcertificate` | SAP | SAP's own certificates, common CAs | No |
| `system` (tenant keystore) | Customer | Custom certs, partner keys, key pairs | Yes |

**Important:** SAP periodically updates the managed keystore. If a CA is missing from the SAP-managed store, add it to the tenant keystore.

---

**Next Steps:**
- See [adapters.md](adapters.md) for adapter-specific authentication
- See [cloud-integration.md](cloud-integration.md) for security steps in iFlows
- See [troubleshooting.md](troubleshooting.md) for authentication and SSL errors
- See [cloud-connector.md](cloud-connector.md) for principal propagation and SNC setup
- See [operations-monitoring.md](operations-monitoring.md) for certificate expiration monitoring

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about security configuration, certificate management, OAuth flows, and data protection patterns.
