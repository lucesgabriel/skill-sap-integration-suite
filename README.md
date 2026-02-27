# SAP Integration Suite — Claude Skill

A comprehensive **custom skill** (system prompt) for [Claude](https://claude.ai) that transforms it into a senior SAP Integration Suite architect. With **12,000+ lines** of curated reference material, Claude can design integration flows, configure adapters, write Groovy scripts, troubleshoot errors, generate Postman collections, and create visual architecture diagrams — all following SAP best practices.

## What is a Claude Skill?

A **skill** is a structured knowledge base that Claude loads as context. It consists of a main prompt file (`SKILL.md`) and supporting reference files that give Claude deep, specialized expertise in a domain. Think of it as giving Claude a "brain transplant" for a specific technology.

**Without this skill**, Claude has general SAP knowledge but may hallucinate adapter configurations, miss CPI-specific patterns, or suggest outdated approaches.

**With this skill**, Claude consults 17 reference files covering every aspect of SAP Integration Suite, follows proven design patterns, and generates production-ready guidance.

## What's Included

```
skill-sap-integration-suite/
├── SKILL.md                          # Main skill prompt (340 lines)
├── references/
│   ├── cloud-integration.md          # iFlow design, steps, patterns (842 lines)
│   ├── adapters.md                   # 80+ adapter configurations (577 lines)
│   ├── api-management.md             # API proxies, policies, Developer Portal (847 lines)
│   ├── event-mesh.md                 # Event-driven, CloudEvents, AEM (828 lines)
│   ├── edge-integration-cell.md      # Hybrid K8s deployment (1,204 lines)
│   ├── integration-advisor-tpm.md    # B2B/EDI, Trading Partner Management (1,071 lines)
│   ├── content-transport.md          # CI/CD, TMS, Content Agent (1,067 lines)
│   ├── migration-assessment.md       # PO/PI migration, ISA-M (902 lines)
│   ├── scripting.md                  # Groovy/JS patterns and anti-patterns (630 lines)
│   ├── postman-testing.md            # Postman collection generation (700 lines)
│   ├── troubleshooting.md            # Error catalog and debugging (692 lines)
│   ├── message-mapping-xslt.md       # Mapping techniques and XSLT (568 lines)
│   ├── cloud-connector.md            # On-premise connectivity, HA (511 lines)
│   ├── operations-monitoring.md      # MPL, alerts, OData API (494 lines)
│   ├── security.md                   # OAuth, certificates, RBAC (447 lines)
│   ├── excalidraw-diagrams.md        # Visual iFlow diagram patterns (319 lines)
│   └── simulation-testing.md         # Design-time iFlow testing (280 lines)
└── scripts/
    └── groovy-script-template.groovy # Reusable Groovy template
```

**Total: ~12,300 lines of expert knowledge across 19 files.**

## Capabilities

| Area | What Claude Can Do |
|---|---|
| **iFlow Design** | Design integration flows with proper patterns (sync, async, pub-sub, EOIO), error handling, and idempotent processing |
| **Visual Diagrams** | Generate Excalidraw architecture diagrams with SAP color coding before implementation |
| **80+ Adapters** | Configure HTTP, SFTP, OData, RFC, IDoc, AMQP, Kafka, JDBC, AS2, SOAP, and 70+ more |
| **Groovy Scripting** | Write production-ready scripts with proper error handling, streaming, and performance patterns |
| **API Management** | Design API proxies with policies (quota, spike arrest, OAuth, caching), Developer Portal setup |
| **Event Mesh** | Implement event-driven architectures with CloudEvents, topics, queues, dead-letter handling |
| **B2B/EDI** | Configure EDIFACT, X12, IDoc type systems with Integration Advisor and Trading Partner Management |
| **Troubleshooting** | Diagnose errors across all adapters with a comprehensive error catalog (HTTP, SOAP, SFTP, JMS, OData, SSL, RFC) |
| **Postman Testing** | Generate ready-to-import Postman v2.1 collections with OAuth2, environment variables, and test assertions |
| **Security** | Configure OAuth2, X.509, SAML, PGP encryption, digital signatures, principal propagation |
| **Migration** | Plan PO/PI to CPI migrations with ISA-M methodology and Migration Assessment Tool |
| **Edge Deployment** | Deploy on private Kubernetes (EKS, AKS, GKE, OpenShift) with Edge Integration Cell |
| **CI/CD** | Set up transport pipelines with TMS, Content Agent, and GitHub Actions/Jenkins |
| **Monitoring** | Configure MPL monitoring, Cloud ALM, OData API queries, certificate expiration alerts |

## How to Install

### Option A: Claude Desktop App (recommended)

1. Download this repository as ZIP (or use the [Releases](../../releases) page)
2. Open **Claude Desktop App**
3. Go to **Settings > Skills** (or the equivalent in your version)
4. Click **Add Skill** and select the ZIP file
5. The skill appears as `sap-integration-suite` and activates automatically when you ask SAP-related questions

### Option B: Claude Code (CLI)

Clone this repo inside your project directory:

```bash
# From your project root
git clone https://github.com/lucesgabriel/skill-sap-integration-suite.git .claude/skills/sap-integration-suite
```

Claude Code will automatically detect and load skills from `.claude/skills/`.

### Option C: Manual (any Claude interface)

Copy the content of `SKILL.md` into Claude's **System Prompt** or **Custom Instructions**. For the full experience, also include the reference files as project knowledge.

## Usage Example

### Prompt

```
I need to integrate Salesforce with SAP S/4HANA on-premise. When a new
opportunity is marked as "Closed Won" in Salesforce, it should automatically
create a Sales Order in S/4HANA via BAPI_SALESORDER_CREATEFROMDAT2.

Requirements:
- Real-time (event-driven preferred)
- Exactly-once delivery
- Error notifications to the integration team
- Field mapping: Salesforce Opportunity fields to SAP Sales Order fields
```

### What Claude Does (step by step)

**1. Generates a visual Excalidraw diagram** showing the full architecture:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              SAP BTP Tenant                                     │
│                                                                                 │
│  ┌──────────┐    HTTPS/     ┌────────────────────────────────────┐    RFC/     ┌──────────┐
│  │Salesforce│──webhook──>│  Cloud Integration (iFlow)         │──Cloud──>│ SAP S/4  │
│  │  (CRM)   │   POST       │                                    │ Connector │  HANA    │
│  └──────────┘              │  [Content Modifier]                │          └──────────┘
│                             │       ↓                            │
│                             │  [Groovy: Transform]               │          ┌──────────┐
│                             │       ↓                            │          │  Cloud   │
│                             │  [RFC: BAPI_SALESORDER_CREATE]     │──────────│Connector │
│                             │                                    │          └──────────┘
│                             │  ┌─ Exception Subprocess ────────┐ │
│                             │  │ [Log Error] → [Send Alert]    │ │
│                             │  └───────────────────────────────┘ │
│                             └────────────────────────────────────┘
└─────────────────────────────────────────────────────────────────────────────────┘
```

> The actual output is an interactive Excalidraw diagram with SAP color coding (blue for source, green for target, purple for integration process, red for exceptions).

**2. Provides a step-by-step implementation guide:**

> **Step 1 — Create the Integration Flow**
>
> Navigate to *Design > Integrations and APIs > Create* and configure:
>
> **Sender Channel:**
> - Adapter: HTTPS
> - Address: `/salesforce/opportunity/won`
> - Authorization: OAuth2 (Salesforce webhook)
> - CSRF Protected: Enabled
>
> **Step 2 — Content Modifier (Extract Fields)**
> Set exchange properties from the Salesforce webhook payload:
>
> | Property | Source | Expression |
> |---|---|---|
> | `sfAccountId` | Body (XPath) | `/root/AccountId` |
> | `sfAmount` | Body (XPath) | `/root/Amount` |
> | `sfCloseDate` | Body (XPath) | `/root/CloseDate` |
> | `sfOpportunityName` | Body (XPath) | `/root/Name` |
>
> **Step 3 — Groovy Script (Transform to BAPI Structure)**

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {
    def body = message.getBody(String.class)
    def sf = new JsonSlurper().parseText(body)

    // Build BAPI_SALESORDER_CREATEFROMDAT2 input
    def order = """<?xml version="1.0" encoding="UTF-8"?>
    <rfc:BAPI_SALESORDER_CREATEFROMDAT2 xmlns:rfc="urn:sap-com:document:sap:rfc:functions">
      <ORDER_HEADER_IN>
        <DOC_TYPE>TA</DOC_TYPE>
        <SALES_ORG>1000</SALES_ORG>
        <DISTR_CHAN>10</DISTR_CHAN>
        <DIVISION>00</DIVISION>
        <PURCH_NO_C>${sf.Name}</PURCH_NO_C>
      </ORDER_HEADER_IN>
      <ORDER_ITEMS_IN>
        <item>
          <MATERIAL>${sf.Product_Code__c}</MATERIAL>
          <TARGET_QTY>${sf.Quantity__c}</TARGET_QTY>
        </item>
      </ORDER_ITEMS_IN>
      <ORDER_PARTNERS>
        <item>
          <PARTN_ROLE>AG</PARTN_ROLE>
          <PARTN_NUMB>${sf.SAP_Customer_Number__c}</PARTN_NUMB>
        </item>
      </ORDER_PARTNERS>
    </rfc:BAPI_SALESORDER_CREATEFROMDAT2>"""

    message.setBody(order)
    return message
}
```

> **Step 4 — RFC Receiver Adapter**
>
> | Setting | Value |
> |---|---|
> | Adapter Type | RFC |
> | Proxy Type | On-Premise |
> | Location ID | *(your Cloud Connector location)* |
> | Destination | `SAP_S4H_RFC` |
> | RFC Function | `BAPI_SALESORDER_CREATEFROMDAT2` |
>
> **Step 5 — Exception Subprocess**
> - Groovy script to log error details to MPL
> - Content Modifier to build alert payload
> - HTTP call to team's Slack/Teams webhook
>
> **Step 6 — Exactly-Once Delivery**
> - Wrap the main flow in an *Idempotent Process Call*
> - Use Salesforce `OpportunityId` as the unique message ID
> - Data Store: `SalesforceIdempotent`, expiration: 30 days

**3. Generates a Postman collection** for testing the endpoint with sample payloads and OAuth2 authentication.

**4. Provides a Cloud Connector checklist** for exposing the RFC destination to BTP.

## Companion MCP Tools

This skill works best when combined with these MCP (Model Context Protocol) tools:

| MCP Tool | Purpose |
|---|---|
| **Excalidraw MCP** | Generates interactive visual diagrams for iFlow architecture |
| **NotebookLM MCP** | Deep-reading queries against SAP Integration Suite books (2,800+ pages) |

The reference files automatically instruct Claude to use these tools when available.

## Topics Covered

| Category | Topics |
|---|---|
| **Cloud Integration** | iFlow design, process steps, Content Modifier, Router, Splitter, Aggregator, Filter, Persist, Gather, ProcessDirect, Looping, Idempotent processing, JMS queues, Data Stores |
| **Adapters** | HTTP, HTTPS, SOAP, SFTP, FTP, OData V2/V4, RFC, IDoc, JDBC, AMQP, Kafka, AS2, AS4, ELSTER, AEM, Mail, LDAP, SMB, Ariba, SuccessFactors, Salesforce, Slack, ServiceNow, Workday, Open Connectors |
| **API Management** | API Proxy creation, 34 policy types (traffic, security, mediation), Developer Portal, API Products, Analytics, Key Value Maps, custom scripting, versioning strategies |
| **Event Mesh** | Topics, queues, subscriptions, CloudEvents v1.0, dead-letter, webhooks, SAP S/4HANA Business Events, Advanced Event Mesh (AEM), event-driven patterns |
| **Edge Integration Cell** | Kubernetes deployment, Helm charts, scaling (HPA), monitoring (Prometheus/Grafana), multi-node HA, platform guides (EKS, AKS, GKE, OpenShift) |
| **B2B/EDI** | EDIFACT (D.96A-D.21A), X12 (004010-008020), IDoc, cXML, UBL 2.1, Integration Advisor MIGs/MAGs, Trading Partner Management, AS2/AS4, Peppol, XRechnung |
| **Security** | OAuth 2.0, X.509, SAML, API Keys, PGP encryption, PKCS#7/CMS, WS-Security, Principal Propagation, RBAC, Secure Parameter Store, certificate chain of trust |
| **Cloud Connector** | Architecture, installation, access control, virtual host mapping, Location ID, principal propagation, master/shadow HA, certificate management, performance tuning |
| **Migration** | ISA-M methodology, Migration Assessment Tool, complexity classification, pattern-by-pattern migration (IDoc, SOAP, RFC, File, JDBC, REST), parallel run strategy, validation |
| **Transport/CI/CD** | TMS, CTS+, Content Agent, Jenkins pipelines, GitHub Actions, Azure DevOps, semantic versioning, externalized parameters, environment promotion |
| **Monitoring** | MPL, trace levels, OData monitoring API, Cloud ALM, Alert Notification Service, certificate expiration monitoring, custom Groovy logging, Number Ranges |
| **Troubleshooting** | Error catalogs for HTTP (4xx/5xx), SOAP faults, SFTP, JMS, OData, IDoc/RFC, SSL/TLS, Event Mesh, Cloud Connector, Edge, API Management |

## FAQ

**Q: Does this skill generate iFlow ZIP files?**
No. The BPMN2 `.iflw` format requires tenant-specific metadata that cannot be generated externally. This skill provides visual diagrams + step-by-step UI instructions + copy-paste scripts. For ZIP generation, use the companion skill `sap-iflow-zip-generator`.

**Q: Does Claude need internet access to use this skill?**
No. All reference material is loaded locally. However, Claude can optionally search SAP Help Portal for the latest updates if web access is enabled.

**Q: Can I use this skill with Claude API (not Desktop)?**
Yes. Include `SKILL.md` as the system prompt and the reference files as document context. The skill is designed to work with any Claude interface.

**Q: What SAP Integration Suite version does this cover?**
The skill covers the Cloud Foundry-based Integration Suite (not Neo). Content is current as of 2026 and includes Edge Integration Cell, Advanced Event Mesh, and the latest adapter capabilities.

## Contributing

Contributions are welcome. To add or improve a reference file:

1. Fork this repository
2. Edit or add `.md` files in `references/`
3. Follow the existing format: Table of Contents, ASCII diagrams, configuration tables, code examples, best practices, and "Next Steps" cross-references
4. Ensure minimum 300 lines for expanded files
5. Submit a pull request

## License

This project is provided as-is for educational and professional use with Claude AI. SAP, SAP Integration Suite, SAP BTP, and all related product names are trademarks of SAP SE.
