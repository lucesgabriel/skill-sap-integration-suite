# Cloud Connector Reference

Complete guide for SAP Cloud Connector: setup, configuration, high availability, and troubleshooting for on-premise connectivity.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Installation and Setup](#installation-and-setup)
- [Subaccount Connection](#subaccount-connection)
- [Access Control Configuration](#access-control-configuration)
- [Principal Propagation](#principal-propagation)
- [High Availability (Master/Shadow)](#high-availability-mastershadow)
- [Certificate Management](#certificate-management)
- [Monitoring and Health Check](#monitoring-and-health-check)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Overview

SAP Cloud Connector (CC) is an on-premise agent that creates a secure TLS tunnel between your on-premise network and SAP BTP services (including Cloud Integration). It acts as a reverse invoke proxy — the connection is initiated from inside the corporate firewall, eliminating the need to open inbound firewall ports.

```
SAP BTP (Cloud)                     Corporate Network (On-Premise)
┌──────────────────┐                ┌──────────────────────────┐
│                  │                │                          │
│  Cloud           │   TLS Tunnel   │  Cloud        SAP ECC    │
│  Integration  ←──│───────────────│──Connector → (RFC/HTTP)  │
│  (iFlow)         │  (Outbound     │   Agent       SAP S/4    │
│                  │   from CC)     │     ↓         SAP BW     │
│  Destination     │                │  Virtual      Database   │
│  Service         │                │  Host:Port    File Server│
└──────────────────┘                └──────────────────────────┘
```

**Key benefits:**
- No inbound firewall ports required (CC initiates outbound connection)
- Fine-grained access control (expose only specific resources)
- Audit logging of all access attempts
- TLS encryption for all data in transit
- Support for RFC, HTTP, LDAP, Mail, TCP protocols

---

## Architecture

### Components

| Component | Purpose |
|---|---|
| **Cloud Connector Agent** | Java-based process running on-premise |
| **TLS Tunnel** | Encrypted channel to SAP BTP (port 443 outbound) |
| **Virtual Host/Port** | Alias that hides real backend addresses |
| **Access Control** | Rules defining which resources are accessible |
| **Administration UI** | Web console at `https://localhost:8443` |
| **Audit Log** | Records all connection attempts and data flows |

### Communication Flow

```
1. CPI iFlow sends request to virtual host:port
2. BTP Connectivity Service routes to Cloud Connector via tunnel
3. Cloud Connector maps virtual host → real host
4. Cloud Connector checks access control rules
5. Cloud Connector forwards request to on-premise system
6. Response travels back through same tunnel
```

### Network Requirements

| Direction | Source | Destination | Port | Protocol |
|---|---|---|---|---|
| **Outbound** | Cloud Connector | SAP BTP (connectivitytunnel.*.hana.ondemand.com) | 443 | HTTPS/WSS |
| **Internal** | Cloud Connector | On-premise SAP system | 33xx (RFC), 80/443 (HTTP) | RFC/HTTP |
| **Admin** | Browser | Cloud Connector admin UI | 8443 | HTTPS |

---

## Installation and Setup

### System Requirements

| Requirement | Specification |
|---|---|
| **OS** | Windows Server 2016+, SLES 12+, RHEL 7+, Ubuntu 18.04+ |
| **Java** | SAP JVM 8 or OpenJDK 11+ (included in installer) |
| **RAM** | 2 GB minimum, 4 GB recommended |
| **Disk** | 1 GB for installation + logs |
| **Network** | Outbound HTTPS to *.hana.ondemand.com on port 443 |

### Installation Steps

**1. Download Cloud Connector**
- SAP Development Tools: https://tools.hana.ondemand.com/#cloud
- Choose portable or installer version for your OS

**2. Install (Linux example)**
```bash
# Extract portable version
tar -xzf sapcc-<version>-linux-x64.tar.gz
cd sapcc-<version>

# Start Cloud Connector
./go.sh

# Or install as system service
sudo ./daemon.sh install
sudo systemctl start scc_daemon
```

**3. Initial Configuration**
- Open `https://localhost:8443` in browser
- Default credentials: Administrator / manage
- **CRITICAL: Change default password immediately**

**4. Set instance number** (for master/shadow setup):
- Master: Instance 00 (default)
- Shadow: Instance 01

---

## Subaccount Connection

### Connect to SAP BTP Subaccount

**Navigation:** Cloud Connector Admin → Define Subaccount

**Configuration:**

| Field | Value | Notes |
|---|---|---|
| **Region** | cf.eu10.hana.ondemand.com | Your BTP region |
| **Subaccount ID** | GUID from BTP Cockpit | Find in subaccount overview |
| **Display Name** | DEV-CC-01 | Descriptive name |
| **Subaccount User** | cloud-connector@company.com | BTP user with CC admin role |
| **Password** | *** | BTP user password |
| **Location ID** | (optional) | Required if multiple CCs per subaccount |

### Location ID

Use **Location ID** when multiple Cloud Connectors connect to the same BTP subaccount (e.g., different data centers).

```
BTP Subaccount: "Production"
  ├── Cloud Connector 1 (Location ID: "DC_EUROPE")  → European SAP systems
  ├── Cloud Connector 2 (Location ID: "DC_AMERICAS") → American SAP systems
  └── Cloud Connector 3 (Location ID: "DC_ASIA")     → Asian SAP systems
```

**In CPI HTTP adapter:** Set `Location ID` field to route to specific Cloud Connector.

### Multiple Subaccount Connections

One Cloud Connector can connect to **multiple BTP subaccounts** simultaneously:

```
Cloud Connector
  ├── Subaccount: DEV    (Region: eu10)
  ├── Subaccount: QA     (Region: eu10)
  └── Subaccount: PROD   (Region: eu10)
```

---

## Access Control Configuration

### Concept

Access Control defines exactly which on-premise resources are accessible from BTP. This is a critical security feature — only explicitly configured resources can be reached.

### Configure System Mapping

**Navigation:** Cloud to On-Premise → Add System Mapping

| Field | Description | Example |
|---|---|---|
| **Back-End Type** | Protocol type | SAP System (RFC), Non-SAP System (HTTP) |
| **Protocol** | Communication protocol | RFC, HTTP, HTTPS, LDAP, Mail, TCP |
| **Internal Host** | Real hostname of backend | `sap-ecc.internal.company.com` |
| **Internal Port** | Real port | `8000` (HTTP), `3300` (RFC for instance 00) |
| **Virtual Host** | Alias exposed to cloud | `virtual-ecc` |
| **Virtual Port** | Alias port | `8000` |
| **Principal Type** | Authentication mode | None, X.509 Certificate |

### Configure Resource Access

After creating system mapping, define which specific resources (paths/RFCs) are accessible.

**For HTTP:**

| Field | Description | Example |
|---|---|---|
| **URL Path** | Allowed path prefix | `/sap/opu/odata/sap/API_SALES_ORDER_SRV` |
| **Access Policy** | Path or Path and Sub-Paths | Path and Sub-Paths |
| **Description** | What this resource is | Sales Order OData API |

**For RFC:**

| Field | Description | Example |
|---|---|---|
| **Function Name** | Allowed RFC function | `BAPI_SALESORDER_CREATEFROMDAT2` |
| **Access Policy** | Exact or Prefix | Prefix (allows all BAPI_SALESORDER_*) |

### Access Control Best Practices

1. **Expose minimum resources** — only paths/RFCs actually used by iFlows
2. **Use "Path and Sub-Paths"** carefully — don't over-expose
3. **Never expose root path `/`** — exposes all services on the system
4. **Document each resource** — use description field for traceability
5. **Review regularly** — remove unused mappings

### Example: Expose S/4HANA OData APIs

```
System Mapping:
  Type: Non-SAP System (HTTPS)
  Internal Host: s4hana.internal.company.com
  Internal Port: 443
  Virtual Host: s4hana-virtual
  Virtual Port: 443

Resources:
  1. /sap/opu/odata/sap/API_SALES_ORDER_SRV     [Path and Sub-Paths]
  2. /sap/opu/odata/sap/API_BUSINESS_PARTNER     [Path and Sub-Paths]
  3. /sap/opu/odata/sap/API_PRODUCT_SRV          [Path and Sub-Paths]
```

### Example: Expose RFC Functions

```
System Mapping:
  Type: SAP System (RFC)
  Internal Host: sap-ecc.internal.company.com
  Instance: 00
  Virtual Host: ecc-virtual
  Virtual Instance: 00

Resources:
  1. BAPI_SALESORDER_CREATEFROMDAT2    [Exact]
  2. BAPI_SALESORDER_GETLIST           [Exact]
  3. /BODS/*                           [Prefix] — All BODS functions
```

---

## Principal Propagation

### Overview

Principal Propagation forwards the cloud user identity to the on-premise system, enabling SSO and user-based authorization without separate credentials.

```
User "john@company.com" → CPI (SAML/OAuth) → Cloud Connector → SAP ECC (as JOHN)
```

### Setup Steps

**1. Configure Identity Provider in BTP**
- Set up trust between BTP and your corporate IdP (e.g., Azure AD, SAP IAS)

**2. Generate X.509 Certificate in Cloud Connector**
- Cloud Connector → Principal Propagation → Create System Certificate
- Download CA certificate

**3. Import CA Certificate in SAP Backend**
- Transaction `STRUST` → Import Cloud Connector CA cert
- Configure certificate-to-user mapping (transaction `CERTRULE`)

**4. Enable in Cloud Connector System Mapping**
- Set Principal Type to **X.509 Certificate (Strict/General Usage)**

**5. Configure CPI Adapter**
```
HTTP Receiver Adapter:
  Proxy Type: On-Premise
  Authentication: Principal Propagation
  Location ID: (if applicable)
```

---

## High Availability (Master/Shadow)

### Architecture

```
                SAP BTP
                  ↕
        ┌─────────┴─────────┐
        ↕                   ↕
   Cloud Connector      Cloud Connector
   (MASTER)             (SHADOW)
   Active               Standby
        ↕                   ↕
   On-Premise Systems   On-Premise Systems
```

### Setup Steps

**1. Install Master (first instance)**
- Install and configure normally
- Set HA Mode: Master

**2. Install Shadow (second instance)**
- Install Cloud Connector on separate server
- Set HA Mode: Shadow
- Configure Master host and port

**3. Shadow connects to Master**
- Shadow replicates all configuration from Master
- Shadow maintains own tunnel to BTP
- If Master fails, Shadow automatically takes over

### Failover Behavior

| Event | Behavior |
|---|---|
| Master stops | Shadow takes over within ~30 seconds |
| Master restarts | Master reclaims primary role |
| Shadow stops | Master continues serving (no HA) |
| Both stop | No connectivity until one restarts |
| Network between M/S lost | Both operate independently (split-brain risk) |

---

## Certificate Management

### Certificate Types

| Certificate | Purpose | Location |
|---|---|---|
| **System Certificate** | Cloud Connector identity to BTP | Auto-generated or custom |
| **CA Certificate** | Trust chain for principal propagation | Import from corporate CA |
| **Backend Certificates** | Trust on-premise server TLS certs | Import server certs |
| **BTP Certificates** | Trust BTP service endpoints | Auto-managed |

### Certificate Renewal

**Navigation:** Configuration → On Premise → System Certificate

**Steps:**
1. Generate new Certificate Signing Request (CSR)
2. Sign with your corporate CA
3. Import signed certificate
4. Restart Cloud Connector if needed

**Best Practice:** Set calendar reminder 30 days before expiry. Certificate expiry causes immediate connectivity loss.

---

## Monitoring and Health Check

### Cloud Connector Admin UI

**Navigation:** `https://<cc-host>:8443` → Monitoring

| Check | What to Monitor |
|---|---|
| **Tunnel Status** | Green = connected, Red = disconnected |
| **Subaccount Connection** | Status per connected subaccount |
| **Most Recent Requests** | Timestamp, duration, status of last calls |
| **Active Sessions** | Current RFC/HTTP connections |
| **Resource Usage** | Memory, threads, open connections |

### BTP Cockpit Monitoring

**Navigation:** BTP Cockpit → Connectivity → Cloud Connectors

- View all connected Cloud Connectors
- Check tunnel status and last heartbeat
- Verify Location IDs

### Health Check from CPI

Create a lightweight iFlow that regularly pings an on-premise health endpoint:

```
[Timer: Every 5 min] → [HTTP: GET /health on virtual-host]
    ↓
  [Router: Status Code]
    ├─ (200) → [Log: CC Healthy]
    └─ (Error) → [Alert: CC Connectivity Issue]
```

---

## Performance Tuning

### Connection Pool Settings

**Navigation:** Configuration → Advanced → Connection Pool

| Setting | Default | Recommendation |
|---|---|---|
| **Max Connections (HTTP)** | 100 | Increase for high-volume (200-500) |
| **Max Connections per Route** | 20 | Match expected concurrent iFlows |
| **Connection Timeout** | 30s | Decrease for fast-fail (10s) |
| **Socket Timeout** | 60s | Match backend SLA |
| **Idle Timeout** | 60s | Reduce if connection pool exhaustion |

### Thread Pool Settings

| Setting | Default | Recommendation |
|---|---|---|
| **Max Threads** | 200 | Increase for >100 concurrent connections |
| **Min Threads** | 10 | Increase for consistent load |

### RFC Connection Settings

| Setting | Recommendation |
|---|---|
| **Peak Limit** | Set based on SAP backend dialog process count |
| **Connection Limit** | Don't exceed SAP RFC server capacity |
| **Pool Size** | 10-50 depending on volume |

---

## Troubleshooting

### Common Errors

| Error | Cause | Resolution |
|---|---|---|
| **"Could not establish connection to Cloud Connector"** | Tunnel disconnected | Check CC status, network, certificates |
| **"Connection refused"** | Backend system down or wrong port | Verify internal host:port, check backend |
| **"403 Forbidden"** | Access control blocks request | Add resource to access control list |
| **"Certificate expired"** | System or backend cert expired | Renew certificate in CC admin |
| **"Host not reachable"** | DNS or network issue | Check DNS resolution from CC server |
| **"Connection reset"** | Firewall or proxy blocking | Check corporate firewall/proxy rules |
| **"Handshake failure"** | TLS version mismatch | Update TLS settings, check cipher suites |
| **"Location ID not found"** | Wrong Location ID in adapter | Verify Location ID in CC admin and iFlow |

### Diagnostic Steps

**1. Check Tunnel Status**
```
CC Admin → Subaccount Connection → Status should be "Connected"
If "Disconnected": Check outbound port 443, proxy settings, credentials
```

**2. Check Access Control**
```
CC Admin → Cloud to On-Premise → System Mapping → Resources
Verify the URL path or RFC function is listed and accessible
```

**3. Test Backend Connectivity**
```
CC Admin → Cloud to On-Premise → Select System → Check Availability
Should return "Reachable" with response time
```

**4. Review Audit Log**
```
CC Admin → Audit Log → Filter by time range
Look for: Denied access, failed connections, certificate errors
```

**5. Check CC Logs**
```
Linux: /opt/sap/scc/log/
Windows: C:\SAP\scc\log\

Key files:
  ljs_trace.log  — Main trace log
  ljs_tunnel.log — Tunnel connection log
```

---

## Best Practices

### Security

1. **Change default password immediately** after installation
2. **Use HTTPS for admin UI** (port 8443, never disable)
3. **Restrict admin access** to authorized personnel only
4. **Expose minimum resources** — only what iFlows need
5. **Rotate certificates** before expiry with 30-day buffer
6. **Enable audit logging** for compliance and troubleshooting
7. **Keep CC updated** — apply patches regularly

### Operations

1. **Deploy in HA mode** (master/shadow) for production
2. **Monitor tunnel status** continuously with alerts
3. **Set up health check iFlow** for proactive detection
4. **Document all system mappings and resources** with descriptions
5. **Review access control quarterly** — remove unused entries
6. **Plan maintenance windows** — CC restart causes brief connectivity loss

### Network

1. **Use corporate proxy** if required for outbound HTTPS
2. **Whitelist BTP endpoints** in firewall (`*.hana.ondemand.com:443`)
3. **Avoid NAT issues** — ensure stable outbound IP for tunnel
4. **Test failover** regularly — stop master, verify shadow takeover
5. **Monitor bandwidth** — CC handles all on-premise traffic

---

**Next Steps:**
- See [adapters.md](adapters.md) for adapter-specific Cloud Connector settings (RFC, HTTP, IDoc)
- See [security.md](security.md) for principal propagation and certificate details
- See [troubleshooting.md](troubleshooting.md) for integration error resolution
- See [edge-integration-cell.md](edge-integration-cell.md) for alternative hybrid deployment

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about Cloud Connector infrastructure, secure communication patterns, and security configuration.
