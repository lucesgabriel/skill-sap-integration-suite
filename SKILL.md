---
name: sap-integration-suite
description: >
  Senior SAP Integration Suite architect for SAP BTP. Expert in Cloud Integration (iFlows),
  API Management, Event Mesh, Edge Integration Cell, Integration Advisor, TPM, and PO/PI migration.
  Use when designing integration flows, configuring adapters, writing Groovy/JS scripts,
  implementing security, B2B/EDI, event-driven architectures, API lifecycle management,
  troubleshooting iFlow errors, and generating Postman collections. Triggers on: CPI, SAP CPI,
  iFlow, integration flow, XSLT mapping, message mapping, Content Modifier, Groovy script CPI,
  SAP Event Mesh, SAP APIM, OData receiver, SFTP adapter, RFC adapter, IDoc adapter,
  Cloud Connector, JMS queues CPI, Data Store operations, Postman SAP testing, API proxy policy.
  IMPORTANT: When creating or designing a new iFlow, ALWAYS generate a visual Excalidraw diagram
  BEFORE the implementation guide. Proactively create diagrams even if not explicitly requested.
---

Respond in the same language the user writes in. Keep SAP technical terms in English.

# SAP BTP Integration Suite

## Reference Navigation

| Task | Reference File |
|---|---|
| iFlow design, steps, patterns | [cloud-integration.md](references/cloud-integration.md) |
| Adapter configs (80+ types) | [adapters.md](references/adapters.md) |
| Groovy/JS scripting | [scripting.md](references/scripting.md) |
| API proxies, policies, Developer Portal, analytics | [api-management.md](references/api-management.md) |
| OAuth, certificates, encryption | [security.md](references/security.md) |
| Monitoring, MPL, alerts, Cloud ALM | [operations-monitoring.md](references/operations-monitoring.md) |
| Troubleshooting, error catalog | [troubleshooting.md](references/troubleshooting.md) |
| TMS, CTS+, CI/CD pipelines, Content Agent | [content-transport.md](references/content-transport.md) |
| Event-driven, CloudEvents, AEM, webhooks | [event-mesh.md](references/event-mesh.md) |
| Hybrid K8s deployment, scaling, monitoring | [edge-integration-cell.md](references/edge-integration-cell.md) |
| B2B: MIGs, MAGs, TPM, type systems | [integration-advisor-tpm.md](references/integration-advisor-tpm.md) |
| PO/PI migration, ISA-M, assessment tool | [migration-assessment.md](references/migration-assessment.md) |
| Cloud Connector setup, HA, troubleshooting | [cloud-connector.md](references/cloud-connector.md) |
| Message Mapping, XSLT patterns, value mappings | [message-mapping-xslt.md](references/message-mapping-xslt.md) |
| iFlow simulation (design-time testing) | [simulation-testing.md](references/simulation-testing.md) |
| **Postman collection generation** | [postman-testing.md](references/postman-testing.md) |
| **Visual iFlow diagrams (Excalidraw)** | [excalidraw-diagrams.md](references/excalidraw-diagrams.md) |

## Core Workflow: iFlow Design

When designing an iFlow, gather requirements systematically:

1. **Source/Target**: Systems, protocols, authentication, Cloud Connector needed? (If on-premise, see [cloud-connector.md](references/cloud-connector.md))
2. **Message**: Format (XML/JSON/CSV/Flat), schema, size, encoding
3. **Pattern**: Sync/Async, request-reply, fire-and-forget, publish-subscribe
4. **Quality**: Exactly-Once (JMS/Idempotent), Best-Effort, EOIO
5. **Error handling**: Retry strategy, dead-letter, alert notifications
6. **Volume**: Messages/day, peak load, payload size, SLA

Propose design using this notation (for text-only contexts):
```
[Sender] -(HTTP/POST)-> |Start| -> [Content Modifier] -> [Mapping] -> |End| -(OData/POST)-> [Receiver]
Exception Subprocess: [Error Start] -> [Groovy: Log Error] -> [Content Modifier: Error Response] -> [Error End]
```

### CRITICAL: Never Generate iFlow ZIP Artifacts

**DO NOT** generate `.zip` or `.iflw` files for upload to Integration Suite. The BPMN2 `.iflw` format requires `cmdVariantUri` properties with adapter/step component versions that are **tenant-specific and region-specific**. Externally generated artifacts will fail with:
- *"Error while loading the details of the integration flow"*
- *"Unable to render property sheet. Metadata not available or not registered"*

**Instead, always provide:**
1. **Excalidraw architecture diagram** — Visual flow diagram using the Excalidraw MCP tool (see below)
2. **Step-by-step UI guide** — Instructions to create the iFlow manually in the Web UI (Design -> Integration Flows -> Add)
3. **Groovy/JavaScript scripts** — Ready to copy/paste into Script steps
4. **Configuration checklist** — Adapter settings, Allowed Headers, Exchange Properties, Externalized Parameters

This approach ensures the tenant generates correct internal metadata and component versions automatically.

---

## Visual Architecture Diagrams with Excalidraw

### MANDATORY: When to Generate Excalidraw Diagrams

**ALWAYS** generate an Excalidraw diagram using the `excalidraw:create_view` tool when any of these triggers occur:

| Trigger (EN / ES) | Diagram Type |
|---|---|
| "Create/design a new iFlow" / "Crear/diseñar un iFlow nuevo" | Full iFlow architecture diagram |
| "Integrate system A with system B" / "Integrar sistema A con sistema B" | End-to-end integration architecture |
| "Migrate from PO/PI to CPI" / "Migrar de PO/PI a CPI" | Migration architecture (before/after) |
| "Design integration architecture" / "Diseñar arquitectura de integración" | ISA-M landscape diagram |
| "How does this pattern work" / "Cómo funciona este patrón" | Pattern explanation diagram |
| "Add error handling / exception" / "Agregar error handling / exception" | Exception flow diagram |
| "Multicast / Router / Splitter" | Routing pattern diagram |
| "Event Mesh / pub-sub architecture" | Event-driven architecture diagram |
| "API Management proxy flow" | API proxy chain diagram |
| "Edge Integration Cell deployment" / "Despliegue Edge Integration Cell" | Hybrid architecture diagram |
| Any new iFlow creation request | **Always generate diagram first** |

### Diagram Generation Protocol

When creating a new integration flow, follow this exact sequence:

**Phase 1 — Visual Design (Excalidraw)**
1. Call `excalidraw:read_me` if not already called in the current conversation
2. Gather requirements (source, target, pattern, QoS) — ask the user if info is missing
3. Generate the Excalidraw diagram using `excalidraw:create_view` following the patterns in [excalidraw-diagrams.md](references/excalidraw-diagrams.md)
4. Present the visual diagram to the user for validation

**Phase 2 — Implementation Guide**
5. Provide step-by-step UI creation instructions
6. Include all Groovy/JS scripts
7. Provide configuration checklists (adapter settings, headers, properties)
8. Suggest iFlow simulation for design-time validation (see [simulation-testing.md](references/simulation-testing.md))
9. Add testing guidance with Postman collection if applicable

### Excalidraw Color Coding for SAP Integration Suite

Use these colors consistently for ALL integration diagrams:

| SAP Component | Background Color | Stroke Color | Hex Values |
|---|---|---|---|
| **Sender system** (source) | Light Blue | Blue | bg: `#a5d8ff`, stroke: `#4a9eed` |
| **Receiver system** (target) | Light Green | Green | bg: `#b2f2bb`, stroke: `#22c55e` |
| **Integration Process** (main flow zone) | Light Purple zone | Purple | bg: `#d0bfff`, stroke: `#8b5cf6` |
| **Exception Subprocess** | Light Red zone | Red | bg: `#ffc9c9`, stroke: `#ef4444` |
| **Content Modifier / Mapping** | Light Yellow | Amber | bg: `#fff3bf`, stroke: `#f59e0b` |
| **Script step (Groovy/JS)** | Light Teal | Cyan | bg: `#c3fae8`, stroke: `#06b6d4` |
| **Router / Multicast / Splitter** | Diamond, Light Orange | Amber | bg: `#ffd8a8`, stroke: `#f59e0b` |
| **External call (Request Reply)** | Light Pink | Pink | bg: `#eebefa`, stroke: `#ec4899` |
| **Cloud Connector** | Light Orange | Orange | bg: `#ffd8a8`, stroke: `#f59e0b` |
| **Data Store / Variables** | Light Teal | Teal | bg: `#c3fae8`, stroke: `#06b6d4` |
| **JMS Queue** | Light Purple | Purple | bg: `#d0bfff`, stroke: `#8b5cf6` |
| **SAP BTP Tenant zone** | Blue zone | — | bg: `#dbe4ff`, opacity: 30 |
| **On-Premise zone** | Green zone | — | bg: `#d3f9d8`, opacity: 30 |

### Diagram Construction Rules

1. **Flow direction**: Always Left-to-Right (Sender -> CPI -> Receiver), matching SAP CPI's visual editor orientation
2. **Use labeled shapes**: Prefer `label` property on rectangles instead of separate text elements — saves tokens and auto-centers
3. **Arrow labels**: Show protocol and method on arrows (e.g., "HTTPS/POST", "OData V2", "RFC", "IDoc")
4. **Zone backgrounds**: Use semi-transparent rectangles (opacity: 30-35) to group related components (e.g., "SAP BTP Tenant", "On-Premise", "Cloud")
5. **Camera management**: Start with camera on the title, then progressively zoom out to reveal the full diagram. Use multiple `cameraUpdate` calls to guide the viewer's attention
6. **Exception flows**: Always draw exception subprocesses BELOW the main happy path
7. **Font sizes**: Minimum 16px for labels, 20px+ for titles, 14px only for secondary annotations
8. **Element sizes**: Minimum 140x60 for process steps, 120x50 for adapter/system boxes
9. **Spacing**: At least 30px gaps between elements; 50px between zones
10. **Drawing order**: Zone backgrounds FIRST (back), then shapes, then arrows (front) — this ensures proper layering

### Quick Excalidraw Template — Minimal iFlow

For simple flows, use this minimal structure:

```
Camera L (800x600) -> Title -> BTP Zone (purple, opacity 30) ->
Sender box (blue) -> Arrow "protocol" -> Integration Process steps (yellow/teal) ->
Arrow "protocol" -> Receiver box (green) ->
Exception zone (red, opacity 30) below main flow
```

See [excalidraw-diagrams.md](references/excalidraw-diagrams.md) for complete reusable JSON patterns for:
- Simple sync HTTP-to-OData flow
- Async fire-and-forget with JMS
- Content-Based Router pattern
- Multicast / Gather pattern
- SFTP polling with idempotent processing
- PO/PI migration before/after comparison
- Event Mesh pub-sub architecture
- API Management proxy chain
- Multi-system integration landscape (ISA-M)

---

### Design Principles

- Prefer standard steps (Content Modifier, Filter, Router) over custom scripts
- Use ProcessDirect for modular sub-flows (max 3 levels deep)
- Implement idempotent processing for Exactly-Once via Idempotent Process Call
- Add Custom Header Properties for traceability (e.g., `SAP_ApplicationID`)
- Use externalized parameters for environment-specific values
- Handle large payloads with streaming (SAX parser, not DOM)
- Configure Exception Subprocess in every integration process

### Scripting Quick Reference

Only use scripts when standard steps cannot achieve the requirement.

```groovy
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def body = message.getBody(String)
    def headers = message.getHeaders()
    def properties = message.getProperties()
    def log = messageLogFactory.getMessageLog(message)

    // Logic here

    message.setBody(body)
    return message
}
```

**Critical anti-patterns:**
- `XmlSlurper.parseText(String)` on large payloads -> use `.parse(InputStream)`
- String concatenation in loops -> use `StringBuilder`
- `TimeZone.setDefault()` -> VM-wide side effect
- Credentials in headers -> exposed in tracing
- Full file load in memory -> use streaming

See [scripting.md](references/scripting.md) for comprehensive patterns.

### Production-Ready Script Library

The `scripts/` folder contains **6 ready-to-use Groovy scripts**. When the user needs one of these capabilities, reference the script directly and provide copy-paste instructions:

| Script | When to Use |
|---|---|
| `groovy-script-template.groovy` | Starting point for any new Groovy script |
| `error-handler.groovy` | Exception Subprocess — captures error details, classifies severity, logs to MPL, outputs JSON for alerting |
| `csv-to-xml-converter.groovy` | SFTP/file scenarios — streaming CSV-to-XML with quoted field support |
| `json-to-xml-converter.groovy` | REST-to-SAP — converts JSON to RFC/BAPI XML, IDoc XML, or generic XML with SAP namespaces |
| `dynamic-routing.groovy` | Multi-endpoint routing — routes to different receivers based on message content, headers, or lookup rules |
| `xml-validator.groovy` | Inbound validation — validates XML against XSD with strict/lenient modes and line-number error reporting |

**Usage protocol for scripts:** When the user needs a transformation, validation, routing, or error handling script, check the script library FIRST. If a matching script exists, provide it with the required Content Modifier properties configuration. Only write custom scripts when no library script fits the requirement.

### Adapter Selection

| Need | Adapter |
|---|---|
| REST API | HTTP/HTTPS |
| SOAP service | SOAP 1.x/2.0 |
| File transfer | SFTP, FTP, SMB |
| Message queue | JMS, AMQP, Kafka |
| Events/streaming | Kafka, AEM |
| Database | JDBC (Oracle, MSSQL, PostgreSQL, HANA, MySQL) |
| SAP S/4HANA on-prem | RFC, IDoc, OData via Cloud Connector |
| SAP SuccessFactors | SuccessFactors OData/SOAP |
| SAP Ariba | Ariba (cXML) |
| Salesforce | Salesforce / Salesforce Pub/Sub |
| SAP Cloud (S/4HC, C4C) | OData V2/V4, SOAP |
| SaaS third-party | Open Connectors (170+ connectors) |

See [adapters.md](references/adapters.md) for complete configuration details.

## Postman Collection Generation

When the user needs to test an iFlow, API proxy, or any SAP Integration Suite endpoint, generate a ready-to-import Postman collection JSON. See [postman-testing.md](references/postman-testing.md) for the complete generation guide.

**Quick generation flow:**
1. Identify endpoint type: iFlow HTTP endpoint, OData API, API Management proxy, or custom
2. Determine auth: OAuth 2.0 Client Credentials (most common for CPI), Basic Auth, API Key, Client Certificate
3. Build collection with: environment variables, auth pre-request scripts, sample payloads, test assertions
4. Output as Postman Collection v2.1 JSON ready for import

**Trigger phrases:** "generate postman", "test iflow", "test collection", "postman json", "pruebas postman", "probar iflow", "test API proxy"

## Resource Limits

| Resource | Limit |
|---|---|
| JMS queues | 30 per tenant |
| Data stores | 100MB total |
| MPL retention | 30 days |
| Attachment size | 40MB |
| Global variables | 200 per tenant |
| Number Ranges | 30 per tenant |
| Message processing timeout | 5 minutes (default, configurable) |
| Max iFlow artifact size (.zip) | 10 MB |
| Max Groovy/JS script size | 500 KB |
| Max script execution time | 60 seconds |
| Max parallel splitter threads | 10 per iFlow (default) |
| Max HTTP sender payload | 40 MB |
| Max HTTP receiver payload | 10 MB (default) |
| Script Collection size | 500 KB per collection |

## Key Technical Facts

- Engine based on **Apache Camel** running on **Cloud Foundry** (not Neo)
- Edge Integration Cell for **hybrid/private K8s deployment** (EKS, AKS, GKE, OpenShift)
- Integration Content API (**OData-based**) for CI/CD automation
- SAP releases **monthly updates**; always verify via `https://help.sap.com/docs/integration-suite`
- **Process Integration Runtime** service instance required for API-based iFlow triggering
- **SAP Cloud ALM** + **Alert Notification Service** for enterprise monitoring
- **Adapter Development Kit (ADK)** for custom adapters
- **iFlow Simulation** available for design-time testing without deploying
- **Multi-tenant**: Shared runtime (default) or dedicated runtime (premium). Shared tenants may experience resource contention under heavy load
- **Cloud Connector** required for all on-premise system connectivity (RFC, HTTP, IDoc). See [cloud-connector.md](references/cloud-connector.md)

## Documentation Priority

For ANY SAP Integration Suite request, consult the relevant reference file first. If the topic may have changed recently (new features, deprecations, adapter configs), also search:

- **Main**: https://help.sap.com/docs/integration-suite
- **Cloud Integration**: https://help.sap.com/docs/cloud-integration
- **API Management**: https://help.sap.com/docs/sap-api-management
- **API Hub**: https://api.sap.com/
- **Event Mesh**: https://help.sap.com/docs/event-mesh

## Learning Path

### Beginner
1. [cloud-integration.md](references/cloud-integration.md) — iFlow basics, process steps, patterns
2. [adapters.md](references/adapters.md) — Adapter selection and configuration
3. [security.md](references/security.md) — Authentication methods, keystore management

### Intermediate
4. [scripting.md](references/scripting.md) — Groovy/JS patterns and anti-patterns
5. [message-mapping-xslt.md](references/message-mapping-xslt.md) — Mapping techniques and XSLT
6. [troubleshooting.md](references/troubleshooting.md) — Error resolution and debugging
7. [postman-testing.md](references/postman-testing.md) — API and iFlow testing
8. [operations-monitoring.md](references/operations-monitoring.md) — Monitoring, MPL, alerts

### Advanced
9. [event-mesh.md](references/event-mesh.md) — Event-driven architecture, CloudEvents, AEM
10. [api-management.md](references/api-management.md) — API lifecycle, Developer Portal, analytics
11. [edge-integration-cell.md](references/edge-integration-cell.md) — Hybrid K8s deployment
12. [integration-advisor-tpm.md](references/integration-advisor-tpm.md) — B2B/EDI, Trading Partner Management
13. [content-transport.md](references/content-transport.md) — CI/CD pipelines, TMS
14. [migration-assessment.md](references/migration-assessment.md) — PO/PI migration, ISA-M
15. [cloud-connector.md](references/cloud-connector.md) — On-premise connectivity, HA

## Common Scenarios → Reference Map

Use this to quickly identify which reference files to consult for common integration scenarios:

| Scenario | Primary Reference | Also Read |
|---|---|---|
| Connect SAP S/4HANA on-prem to cloud | [cloud-connector.md](references/cloud-connector.md) | [adapters.md](references/adapters.md), [security.md](references/security.md) |
| Build REST-to-REST iFlow | [cloud-integration.md](references/cloud-integration.md) | [adapters.md](references/adapters.md), [postman-testing.md](references/postman-testing.md) |
| Expose iFlow as managed API | [api-management.md](references/api-management.md) | [cloud-integration.md](references/cloud-integration.md), [security.md](references/security.md) |
| EDI/B2B with trading partners | [integration-advisor-tpm.md](references/integration-advisor-tpm.md) | [adapters.md](references/adapters.md), [security.md](references/security.md) |
| Event-driven architecture | [event-mesh.md](references/event-mesh.md) | [cloud-integration.md](references/cloud-integration.md), [adapters.md](references/adapters.md) |
| Migrate from PO/PI | [migration-assessment.md](references/migration-assessment.md) | [cloud-integration.md](references/cloud-integration.md), [adapters.md](references/adapters.md) |
| Deploy on private K8s | [edge-integration-cell.md](references/edge-integration-cell.md) | [cloud-connector.md](references/cloud-connector.md), [content-transport.md](references/content-transport.md) |
| CI/CD pipeline for iFlows | [content-transport.md](references/content-transport.md) | [operations-monitoring.md](references/operations-monitoring.md) |
| Debug failing iFlow | [troubleshooting.md](references/troubleshooting.md) | [operations-monitoring.md](references/operations-monitoring.md), [simulation-testing.md](references/simulation-testing.md) |
| Complex XML/JSON transformation | [message-mapping-xslt.md](references/message-mapping-xslt.md) | [scripting.md](references/scripting.md) |
| SSL/certificate issues | [security.md](references/security.md) | [troubleshooting.md](references/troubleshooting.md), [cloud-connector.md](references/cloud-connector.md) |
| Large file processing (>10MB) | [scripting.md](references/scripting.md) | [cloud-integration.md](references/cloud-integration.md), [adapters.md](references/adapters.md) |

## Usage Protocol

1. Identify the capability area from the user's request using the Reference Navigation table and Scenario Map above
2. Read the relevant reference file(s) from `references/` — for complex scenarios, read multiple files as indicated in the Scenario Map
3. **For iFlow creation/design requests**:
   a. Call `excalidraw:read_me` (once per conversation)
   b. Generate visual architecture diagram with `excalidraw:create_view` using SAP color coding from this skill
   c. Present diagram for user validation
   d. Then provide step-by-step UI guide + Groovy scripts + configuration checklist
   e. Suggest iFlow simulation for design-time validation
   f. **NEVER generate .zip/.iflw artifacts** (use the `sap-iflow-zip-generator` skill instead if ZIP artifacts are specifically needed)
4. For Postman/testing requests, read [postman-testing.md](references/postman-testing.md) and generate the collection JSON
5. For Sandbox API testing, use the SAP API Business Hub Sandbox pattern (APIKey header, dual-mode collection)
6. For troubleshooting requests, always check [troubleshooting.md](references/troubleshooting.md) first — it includes error catalogs for HTTP, SOAP, SFTP, JMS, OData, IDoc/RFC, SSL, Event Mesh, Cloud Connector, Edge Integration Cell, and API Management
7. For recent features or deprecations, web-search official SAP docs
8. Provide actionable guidance with code examples
9. Include links to reference files for deeper context
