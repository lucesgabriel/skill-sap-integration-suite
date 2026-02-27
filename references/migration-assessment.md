# Migration Assessment Reference

Complete guide for migrating from SAP Process Orchestration (PO/PI) to SAP Cloud Integration, including the Migration Assessment Tool, ISA-M methodology, technology mapping, migration patterns, and parallel run strategies.

## Table of Contents

- [Overview](#overview)
- [Migration Assessment Tool](#migration-assessment-tool)
- [Reading the Assessment Report](#reading-the-assessment-report)
- [Scenario Complexity Classification](#scenario-complexity-classification)
- [Automated Migration Capabilities](#automated-migration-capabilities)
- [Technology Mapping](#technology-mapping)
- [Migration Patterns per Interface Type](#migration-patterns-per-interface-type)
- [Migration Strategies](#migration-strategies)
- [Parallel Run Strategy](#parallel-run-strategy)
- [ISA-M Methodology](#isa-m-methodology)
- [Post-Migration Validation](#post-migration-validation)
- [Best Practices](#best-practices)
- [Next Steps](#next-steps)

---

## Overview

SAP Process Orchestration (PO) and its predecessor SAP Process Integration (PI) are on-premise middleware platforms that have been the backbone of SAP integration landscapes for over two decades. With SAP's strategic direction toward cloud-first and the end of mainstream maintenance for SAP PO (aligned with SAP NetWeaver), organizations must migrate their integration scenarios to SAP Integration Suite's Cloud Integration capability.

**Migration context:**

```
 CURRENT STATE                          TARGET STATE
 +--------------------------+           +--------------------------+
 | SAP PO / PI              |           | SAP Integration Suite    |
 |                          |           |                          |
 | - Integration Directory  |    ====>  | - Cloud Integration      |
 | - ESR (Mappings)         |    ====>  | - Integration Packages   |
 | - Adapter Engine         |    ====>  | - Adapter Framework      |
 | - BPM / ccBPM            |    ====>  | - iFlows + JMS Queues    |
 | - Alert Framework        |    ====>  | - Alert Notification Svc |
 | - SLD / Directory        |    ====>  | - Integration Content    |
 +--------------------------+           +--------------------------+
        On-Premise                             Cloud (BTP)
```

The **Migration Assessment Tool (MAT)** is an SAP-provided capability within Integration Suite that connects to your SAP PO system, inventories all integration scenarios, classifies their complexity, and generates a detailed migration report with effort estimates.

The **ISA-M (Integration Solution Advisory Methodology)** is a complementary framework that helps organizations map their entire integration landscape across domains and styles, producing a technology recommendation for each scenario.

---

## Migration Assessment Tool

### Prerequisites

Before running the Migration Assessment Tool, ensure the following are in place:

| Prerequisite | Details |
|---|---|
| SAP PO system access | Administrator-level access (role `SAP_XI_API_DISPLAY_J2EE` minimum) |
| SAP PO version | SAP PO 7.31 SP17+ or SAP PO 7.4 SP10+ or SAP PO 7.5 any SP |
| Integration Suite entitlement | Active SAP Integration Suite subscription with Cloud Integration capability |
| Cloud Connector (if needed) | Required when PO system is not internet-accessible; configure RFC/HTTP destination |
| PO API endpoints enabled | `/CommunicationChannelInService`, `/IntegratedConfigurationInService`, `/ConfigurationScenarioInService` must be accessible |
| Network connectivity | HTTPS connectivity from BTP subaccount to PO system (direct or via Cloud Connector) |

### Connecting MAT to PO System

**Step 1: Configure the PO system connection in Integration Suite**

Navigate to: `Integration Suite` > `Settings` > `Integrations` > `Migration Assessment`

**Connection configuration:**

| Setting | Value | Notes |
|---|---|---|
| Name | Descriptive name (e.g., `PRD-PO-750`) | Unique identifier for this PO system |
| Description | Production PO 7.5 | Optional |
| Type | SAP Process Orchestration | Fixed |
| Address | `https://<po-host>:<port>` | PO Java stack HTTPS port (typically 5XX01) |
| Proxy Type | Internet / On-Premise | Use On-Premise if going through Cloud Connector |
| Location ID | Cloud Connector Location ID | Only needed if Proxy Type = On-Premise |
| Authentication | Basic Authentication | Use a technical user with read-only admin access |
| Credential Name | Deploy User Credentials artifact first | Name of deployed credential artifact |

**Step 2: If using Cloud Connector, configure the virtual mapping**

```
Cloud Connector Configuration:
  Back-end Type:       SAP Process Integration
  Protocol:            HTTPS
  Internal Host:       <po-internal-hostname>
  Internal Port:       <po-https-port>
  Virtual Host:        <virtual-hostname>
  Virtual Port:        <virtual-port>
  Principal Type:      None (pass-through)
  Resources:           /CommunicationChannelInService
                       /IntegratedConfigurationInService
                       /ConfigurationScenarioInService
                       /ValueMappingInService
                       /IntegratedConfiguration750InService
```

**Step 3: Test the connection**

Click `Test Connection` in the MAT configuration screen. A successful test returns the PO system version and the number of discoverable integration scenarios.

### Running the Assessment

**Step-by-step process:**

```
1. Select PO System
   Integration Suite > Assess > Migration Assessment > Select configured PO system

2. Trigger System Scan
   Click "Request" > System reads all ICOs, channels, mappings from PO APIs
   Duration: 5-30 minutes depending on number of interfaces (100-2000+)

3. Review Scan Progress
   Status bar shows: Extracting ICOs... Extracting Channels... Analyzing Mappings...
   Wait for status = "Completed"

4. Generate Assessment Report
   Click on completed assessment > "View Dashboard"
   Export options: PDF summary, CSV interface list, Excel detailed report

5. Refine if Needed
   Filter by: Communication Component, Software Component Version, Namespace
   Re-run for specific subsets if landscape is very large
```

**What the tool scans:**

| PO Artifact | What Is Analyzed | Impact on Assessment |
|---|---|---|
| Integrated Configuration Objects (ICOs) | Sender/receiver, interface, namespace, conditions | Determines interface count and routing complexity |
| Communication Channels | Adapter type, module chain, parameters | Maps to CPI adapter availability; modules raise complexity |
| Operation Mappings | Mapping type (graphical, XSLT, Java, ABAP) | Java/ABAP mappings classified as high complexity |
| Message Mappings (Graphical) | UDFs, standard functions, multi-source | Custom UDFs raise complexity |
| BPM / ccBPM Processes | Workflow steps, correlation, exception branches | BPM scenarios always classified as high complexity |
| Value Mappings | Groups, entries, bidirectional lookups | Generally low complexity to migrate |
| Adapter Modules | Custom Java modules in module chain | Custom modules raise complexity significantly |
| Alert Rules | Alert categories, rules, recipients | Require manual recreation in Alert Notification Service |

---

## Reading the Assessment Report

### Summary Dashboard

The report opens with a summary dashboard showing:

```
+------------------------------------------------------------------+
|  MIGRATION ASSESSMENT SUMMARY                                     |
|                                                                   |
|  Total Interfaces Discovered:  347                                |
|  ============================================================    |
|                                                                   |
|  [==========] Low Complexity (Green):    142  (41%)               |
|  [=======   ] Medium Complexity (Yellow): 128  (37%)              |
|  [====      ] High Complexity (Red):       62  (18%)              |
|  [=         ] Not Supported:               15  (4%)               |
|                                                                   |
|  Estimated Total Migration Effort:  2,340 - 4,680 person-days    |
|  Recommended Migration Waves:       4-6 waves over 12-18 months  |
+------------------------------------------------------------------+
```

### Interface List with Migration Readiness

Each interface is listed with its readiness status:

| Column | Description |
|---|---|
| Interface Name | Sender interface name from PO |
| Sender Component | Sending business system / business component |
| Receiver Component | Receiving business system / business component |
| Adapter (Sender) | Sender channel adapter type |
| Adapter (Receiver) | Receiver channel adapter type |
| Mapping Type | Graphical / XSLT / Java / ABAP / None |
| Complexity | Green / Yellow / Red |
| Effort Estimate | Low (1-2d), Medium (3-5d), High (5-15d) |
| Migration Rule | Specific rule ID that determined classification |
| Remarks | Additional notes (e.g., "Java mapping detected", "Custom adapter module") |

### Detailed Per-Interface Analysis

Clicking on any interface reveals:

- **Sender Channel Details**: Adapter type, endpoint configuration, module chain
- **Receiver Channel Details**: Same breakdown for receiver side
- **Mapping Details**: Mapping program names, types, UDF count, external lookups
- **Routing Rules**: Conditions, receiver determinations, interface determinations
- **Dependencies**: Value mapping groups referenced, lookup channels, referenced mappings
- **Migration Rule Explanation**: Why this specific complexity was assigned

---

## Scenario Complexity Classification

### Low Complexity (Green)

**Criteria:**
- Simple pass-through or point-to-point integration
- Standard adapters only: SOAP, HTTP, File/SFTP, RFC, IDoc, JDBC, Mail
- 1:1 message mapping (graphical) or no mapping (pass-through)
- No custom Java code (no UDFs, no Java mappings)
- No BPM/ccBPM orchestration
- No custom adapter modules in module chain
- No complex routing (single receiver, no conditions)

**Estimated effort:** 1-2 person-days per interface

**Migration approach:** Often candidates for automated migration. Many can be migrated using the MAT auto-migration feature with minimal manual adjustment.

**Example scenarios:**
- IDoc pass-through from SAP ECC to SAP S/4HANA
- Simple SOAP-to-SOAP proxy relay
- File pickup, 1:1 mapping, IDoc post to SAP

### Medium Complexity (Yellow)

**Criteria:**
- Multi-step processing with conditional routing
- XSLT mappings or graphical mappings with standard UDFs
- Value mapping lookups
- Multiple receivers (recipient list, interface determination)
- Content-based routing with simple conditions
- Standard adapter modules (e.g., `AF_Modules/PayloadSwapBean`)

**Estimated effort:** 3-5 person-days per interface

**Migration approach:** Manual migration recommended. Requires developer to recreate the iFlow, configure adapters, port mappings, and test thoroughly.

**Example scenarios:**
- Order processing with routing by plant or company code
- Multi-receiver distribution of master data IDocs
- File integration with XSLT transformation and value mapping

### High Complexity (Red)

**Criteria:**
- Java mappings (custom Java code for transformation)
- ABAP mappings (server-side ABAP programs)
- Custom adapter modules (proprietary Java modules in channel config)
- BPM / ccBPM processes (multi-step orchestration, correlation, timers)
- Complex multi-mapping with message splitting at mapping level
- Advanced alert rules with custom alert categories
- Custom headers or dynamic configuration with proprietary logic
- External lookup channels in mappings (RFC/JDBC lookups during mapping)
- Parameterized mappings with runtime-resolved parameters

**Estimated effort:** 5-15 person-days per interface

**Migration approach:** Redesign recommended using cloud-native patterns. Java mappings must be rewritten in Groovy. BPM scenarios must be decomposed into multiple iFlows with JMS queues. Custom adapter modules must be replaced with Groovy scripts or ProcessDirect sub-flows.

**Example scenarios:**
- Purchase order processing with BPM for approval workflow and exception handling
- Complex B2B integration with custom EDI parsing modules
- Multi-system orchestration with correlation and timeout handling

### Complexity Scoring Matrix

| Factor | Low (1 pt) | Medium (2 pts) | High (3 pts) |
|---|---|---|---|
| **Adapter Type** | Standard (SOAP, HTTP, File, IDoc, RFC) | Less common (JDBC, Mail, AS2) | Custom adapter or proprietary module |
| **Mapping Type** | None or simple graphical | XSLT or graphical with std UDFs | Java mapping or ABAP mapping |
| **Routing** | Single receiver, no conditions | Conditional routing, 2-3 receivers | Interface determination, 4+ receivers |
| **Orchestration** | None | Sequential multi-step | BPM/ccBPM with correlation |
| **Custom Code** | None | Standard UDFs or module beans | Custom Java classes or ABAP |
| **External Lookups** | None | Value mapping only | RFC/JDBC lookup during mapping |
| **Error Handling** | Standard | Alert rules | Custom alert categories + escalation |

**Scoring interpretation:**
- **7-10 points**: Low complexity (Green)
- **11-16 points**: Medium complexity (Yellow)
- **17-21 points**: High complexity (Red)

---

## Automated Migration Capabilities

### Scenarios Supporting Auto-Migration

The Migration Assessment Tool can automatically generate Cloud Integration iFlows for a subset of scenarios:

| Supported for Auto-Migration | Not Supported |
|---|---|
| Point-to-point ICOs with standard adapters | BPM/ccBPM processes |
| Graphical message mappings (without custom UDFs) | Java mappings |
| Simple XSLT mappings | ABAP mappings |
| IDoc sender/receiver channels | Custom adapter modules |
| SOAP sender/receiver channels | Parameterized interfaces |
| HTTP sender/receiver channels | Multi-mapping (N:1 or 1:N at mapping level) |
| RFC sender/receiver channels | Interfaces using SWC-level routing |
| File/SFTP sender/receiver channels | Scenarios with external lookup channels |
| Value mapping references | Interfaces with dynamic configuration (custom) |

### Using the Automated Migration Feature

```
Step 1: Select Interfaces
   In the assessment report, filter for Green (Low) complexity interfaces.
   Select one or more interfaces for automated migration.

Step 2: Choose Target Package
   Select an existing Integration Package or create a new one.
   Naming recommendation: <Source_System>_to_<Target_System>_Migrated

Step 3: Trigger Migration
   Click "Migrate" > Confirm target package and naming convention.
   Tool generates: iFlow, adapter configs, message mappings, value mapping refs.

Step 4: Review Generated Artifacts
   Open each generated iFlow in the Integration Flow Designer.
   Verify: adapter settings, mapping correctness, endpoint URLs.

Step 5: Adjust and Deploy
   Update credential artifacts, endpoint URLs for CPI-specific values.
   Configure error handling (add Exception Subprocess if not generated).
   Deploy to target runtime.
```

### Post-Migration Validation Checklist (Automated)

| Check | Action |
|---|---|
| Adapter endpoints | Update hostnames, ports, paths for CPI connectivity |
| Credential artifacts | Create and deploy User Credentials / OAuth / Certificate artifacts |
| Sender authentication | Configure sender authorization (Client Certificate, OAuth, Basic Auth) |
| Mapping accuracy | Send test message, compare output with PO output |
| Value mappings | Verify value mapping artifact is deployed with correct entries |
| Error handling | Confirm Exception Subprocess exists and logs correctly |
| Naming conventions | Rename iFlow and package to follow organizational standards |
| Channel security | Verify TLS/SSL settings, certificate trust chain |

### Limitations of Auto-Migration

What CANNOT be auto-migrated and requires manual work:

- **Java mappings**: Must be rewritten in Groovy. The tool flags these but cannot convert Java to Groovy automatically.
- **ABAP mappings**: Must be rewritten in Groovy, Message Mapping, or XSLT.
- **Custom adapter modules**: Module chain logic must be reimplemented as Groovy scripts or ProcessDirect sub-flows.
- **BPM/ccBPM**: Must be decomposed into multiple iFlows with JMS-based orchestration.
- **Alert configurations**: Must be manually recreated in SAP Alert Notification Service.
- **Dynamic configuration (custom)**: Custom dynamic attributes must be replaced with Exchange Properties and headers.
- **Adapter-specific parameters**: Some PO-specific adapter parameters have no direct CPI equivalent (e.g., certain XI 3.0 protocol settings).
- **External RFC/JDBC lookups in mappings**: Must be redesigned using Request Reply + Content Enricher pattern.

### Manual Adjustments Typically Needed

Even for auto-migrated scenarios, plan for these common adjustments:

1. **Endpoint URLs**: PO uses virtual hosts via SLD; CPI uses direct endpoints or Cloud Connector virtual hosts
2. **Authentication**: PO channel credentials do not transfer; create new credential artifacts
3. **IDoc metadata**: CPI IDoc adapter requires IDoc metadata deployment from ESR or direct upload
4. **Namespace handling**: Some namespace configurations differ between PO and CPI XML processing
5. **Character encoding**: Verify encoding settings match (PO defaults may differ from CPI defaults)

---

## Technology Mapping

| SAP PO Component | Cloud Integration Equivalent | Migration Notes |
|---|---|---|
| Integration Directory (ID) | Integration Flow (iFlow) | Each ICO becomes one or more iFlows |
| Enterprise Services Repository (ESR) | Integration Package + Artifacts | Mappings, schemas stored in package |
| Communication Channel | Adapter Configuration | Adapter types largely 1:1; some config differences |
| Interface Determination | Router Step (Content-Based) | Conditions ported to Router XPath/expression |
| Receiver Determination | Multicast / Recipient List | Multiple receivers become parallel branches |
| Operation Mapping | Message Mapping / XSLT Artifact | Graphical mappings transfer; operation mapping wrapper not needed |
| Graphical Mapping | Message Mapping | Closest 1:1 equivalent; UDFs become custom functions |
| Java Mapping | Groovy Script | Must rewrite; no automated conversion |
| XSLT Mapping | XSLT Mapping Artifact | Generally portable with minor namespace adjustments |
| ABAP Mapping | Groovy Script / Message Mapping | Must rewrite entirely |
| BPM / ccBPM | Multiple iFlows + JMS Queues | Decompose into event-driven iFlow chain |
| Alert Rule | SAP Alert Notification Service | Recreate rules; different configuration model |
| Alert Category | ANS Event Type | Map categories to ANS event types |
| Value Mapping | Value Mapping Artifact | Export from PO, import into CPI; format differs |
| Adapter Module (Standard) | Built-in Adapter Features / Groovy | PayloadSwapBean, XMLAnonymizerBean have CPI equivalents |
| Adapter Module (Custom) | Groovy Script / ProcessDirect Sub-flow | Must rewrite custom logic |
| RFC Lookup (in mapping) | Request Reply + Content Enricher | Two-step: call RFC adapter, merge result into payload |
| JDBC Lookup (in mapping) | Request Reply + Content Enricher | Same pattern as RFC lookup |
| Integrated Configuration (ICO) | iFlow + Adapter Config | One ICO maps to one iFlow (typically) |
| Sender Agreement | Sender Adapter Security Settings | Authentication/authorization at adapter level |
| Receiver Agreement | Receiver Adapter Security Settings | Security material and credentials per receiver |
| SLD Business System | Participant (Sender/Receiver) | No SLD equivalent; endpoints configured directly |

---

## Migration Patterns per Interface Type

### IDoc Interfaces

IDoc interfaces are among the most common in SAP PO landscapes and have well-defined migration patterns.

**PO Pattern:**
```
+----------+    IDoc (XI)     +--------+   IDoc (XI)    +----------+
| SAP ECC  | ───────────────> | SAP PO | ─────────────> | SAP S/4  |
| (Sender) |  Port: SAPPO1   | (ICO)  |  RFC Dest:     | (Recv)   |
|          |  Partner: LS    |  Map   |  S4HANA_100    |          |
+----------+                  +--------+                 +----------+
   ALE Config:                  ID:                       ALE Config:
   - SM59 RFC Dest              - ICO with IDoc adapter   - WE20 Partner
   - WE21 Port                  - Graphical mapping       - BD54 Logical Sys
   - WE20 Partner Profile       - Receiver determination
```

**CPI Pattern:**
```
+----------+    IDoc (SOAP)    +--------+   IDoc (RFC)   +----------+
| SAP ECC  | ────────────────> |  CPI   | ─────────────> | SAP S/4  |
| (Sender) |  SOAP Channel    | iFlow  |  RFC Adapter   | (Recv)   |
|          |  to CPI endpoint |  Map   |  via Cloud     |          |
+----------+                   +--------+  Connector     +----------+
   SAP Config:                   iFlow:                    SAP Config:
   - SM59 HTTP Dest to CPI      - IDoc Sender Adapter     - No changes
   - IDX1/IDX2 (optional)       - Message Mapping         - (receives IDoc
   - Transaction SXMB_IFC       - IDoc Receiver Adapter     as before)
     or direct IDoc send         - Error Subprocess
```

**Key differences:**
- Sender adapter changes from XI protocol to IDoc-SOAP or IDoc adapter
- SM59 destination on sender SAP system points to CPI endpoint (not PO)
- Cloud Connector required if receiver SAP system is on-premise
- IDoc metadata must be deployed to CPI tenant (import from ESR or upload `.xsd`)

### SOAP / HTTP Interfaces

```
PO Pattern:                              CPI Pattern:
[Consumer] --SOAP--> [PO] --SOAP--> [Provider]    [Consumer] --SOAP--> [CPI] --SOAP--> [Provider]
                      |                                                  |
                 WSDL from ESR                                    WSDL uploaded or
                 WS-Security                                      auto-generated
                 WS-RM (optional)                                 WS-Security via
                                                                  Security Material
```

**Migration notes:**
- WSDL artifacts: Export from ESR, upload to CPI package as resource
- WS-Security: Configure in CPI adapter security settings + Keystore
- Consumer endpoint URL changes from PO to CPI (update all consumers)
- WS-ReliableMessaging not available in CPI; use JMS for guaranteed delivery

### RFC Interfaces

- PO RFC sender adapter becomes CPI RFC sender adapter (via Cloud Connector)
- PO RFC receiver adapter becomes CPI RFC receiver adapter (via Cloud Connector)
- RFC metadata: Import from SAP system or upload `.xml` lookup definition
- tRFC/qRFC: Use JMS queue in CPI for guaranteed ordered delivery

### File / SFTP Interfaces

- PO File/FTP adapter maps directly to CPI SFTP or File adapter
- File processing parameters (polling interval, file filter, post-processing) are largely identical
- Content conversion (flat-to-XML) in PO module chain becomes Flat File converter or CSV-to-XML converter in CPI
- **Important**: PO `AF_Modules/MessageTransformBean` and `AF_Modules/PayloadSwapBean` must be replaced with CPI-native steps (Content Modifier, Encoder/Decoder)

### JDBC Interfaces

- PO JDBC adapter maps to CPI JDBC adapter
- SQL query configuration is similar but CPI uses XML-based request/response format
- Stored procedure calls supported in both
- Database driver deployment: CPI requires JDBC driver upload to tenant (Operations > JDBC Material)
- Connection pooling parameters differ; tune for CPI runtime

### REST API Interfaces

- PO REST adapter (available in PO 7.5) maps to CPI HTTP adapter with REST configuration
- OData provisioning in PO maps to CPI OData sender adapter
- JSON processing: CPI has native JSON support (no extra modules needed unlike PO)
- API Management layer recommended in CPI for REST APIs (rate limiting, authentication, analytics)

---

## Migration Strategies

### Lift and Shift (Minimal Changes)

**Approach:** Migrate existing PO scenarios to CPI with minimal redesign. Replicate the existing logic as closely as possible.

**When to use:**
- Timeline pressure (PO end-of-maintenance approaching)
- Simple interfaces that work well as-is
- Team has limited CPI experience (reduce risk)

**Pros:** Fastest migration, lowest risk per interface, predictable effort
**Cons:** Does not leverage cloud-native capabilities, may carry over PO anti-patterns

### Redesign (Cloud-Native)

**Approach:** Rethink the integration pattern using cloud-native capabilities: event-driven architecture, API-first design, modular iFlows, proper error handling with JMS.

**When to use:**
- Complex BPM scenarios that need decomposition
- Interfaces with known performance or reliability issues in PO
- Interfaces that will evolve significantly post-migration
- Greenfield integration requirements

**Pros:** Better long-term architecture, leverages CPI strengths, improved maintainability
**Cons:** Higher effort per interface, requires deeper CPI expertise

### Hybrid (Coexistence)

**Approach:** Keep some scenarios running in PO while migrating others to CPI. Typically used for phased migrations where PO remains operational during transition.

**When to use:**
- Very large landscapes (500+ interfaces) requiring multi-year migration
- Interfaces tightly coupled to on-premise components not yet cloud-ready
- Regulatory or compliance constraints on certain interfaces

```
Hybrid Architecture:
+----------+          +--------+          +----------+
| Sender   | -------> | SAP PO | -------> | Receiver |
| Systems  |          | (kept) |          | Systems  |
|          |          +--------+          |          |
|          |                              |          |
|          |          +--------+          |          |
|          | -------> |  CPI   | -------> |          |
|          |          |(migrtd)|          |          |
+----------+          +--------+          +----------+
                          |
                    Cloud Connector
                    (bidirectional)
```

**Important considerations:**
- Maintain PO patching and monitoring during transition
- Avoid splitting a single business process across PO and CPI
- Use Cloud Connector for CPI-to-on-premise connectivity
- Plan for PO decommissioning timeline

---

## Parallel Run Strategy

Running SAP PO and CPI simultaneously during migration is a critical risk mitigation strategy. It ensures migrated interfaces produce identical results before cutting over.

### Message Routing Approaches

**Option A: Dual Delivery (Splitter at Source)**
```
                    +--------+  (existing)  +----------+
               +--> | SAP PO | -----------> | Receiver |
               |    +--------+              +----------+
+---------+    |
| Sender  | ---+
| System  |    |
+---------+    |    +--------+  (migrated)  +----------+
               +--> |  CPI   | -----------> | Receiver |
                    +--------+    (shadow)   | (Test)   |
                                             +----------+
```
- Source system sends to both PO and CPI
- CPI delivers to a test/shadow receiver (not production)
- Compare outputs at shadow receiver

**Option B: CPI Pass-Through (Proxy Pattern)**
```
+---------+        +--------+        +--------+        +----------+
| Sender  | -----> |  CPI   | -----> | SAP PO | -----> | Receiver |
| System  |        | (proxy)|        | (orig) |        | System   |
+---------+        +--------+        +--------+        +----------+
                       |
                       +---> [Log/Compare output for validation]
```
- CPI sits in front of PO, forwarding messages through
- Allows CPI to capture and log all messages for comparison
- Gradually shift processing from PO to CPI

### Output Comparison and Validation

| Validation Aspect | Method |
|---|---|
| Message count | Compare daily/weekly message counts PO MPL vs CPI MPL |
| Payload content | Hash comparison of output payloads (MD5/SHA-256) |
| Field-level accuracy | Select sample messages, compare field-by-field |
| Error rates | Compare error counts and error types |
| Processing time | Benchmark latency: PO end-to-end vs CPI end-to-end |
| Edge cases | Test with: empty payloads, maximum size, special characters, missing optional fields |

### Cutover Checklist

Before switching an interface from PO to CPI in production:

- [ ] Parallel run completed for minimum 2 weeks (4 weeks recommended)
- [ ] Message count parity confirmed (CPI processes same volume as PO)
- [ ] Payload comparison shows 100% match on sampled messages
- [ ] Error handling tested: simulate failures, verify alerts and retries
- [ ] Performance benchmarks within acceptable range (CPI within 120% of PO latency)
- [ ] Monitoring dashboards configured in CPI (MPL, alert rules, Cloud ALM)
- [ ] Support team trained on CPI monitoring and troubleshooting
- [ ] Rollback procedure documented and tested
- [ ] Sender system reconfiguration planned (SM59 destinations, partner profiles)
- [ ] Communication sent to stakeholders with cutover date and rollback window
- [ ] Go/No-Go decision meeting held

### Rollback Procedures

If issues are detected after cutover:

```
Rollback Steps:
1. Re-enable PO channels (un-deactivate sender/receiver channels in PO ID)
2. Revert sender system configuration:
   - SM59: Point RFC destination back to PO
   - WE21: Revert IDoc port to PO
   - SXMB_IFC: Re-register PO as integration server
3. Undeploy or stop CPI iFlow (to prevent duplicate processing)
4. Verify message flow through PO is restored
5. Investigate root cause of CPI issues
6. Document lessons learned, adjust migration approach
```

### Recommended Parallel Run Pattern: 4-Week Cycle

```
Week 1: Shadow Mode
  - CPI processes messages in parallel
  - CPI output goes to test/shadow system only
  - Daily comparison of message counts

Week 2: Validation Mode
  - Field-level comparison on 10% sample
  - Error scenario testing (inject failures)
  - Performance benchmarking

Week 3: Pre-Production Mode
  - CPI output validated by business users
  - Edge case testing completed
  - Support team dry-run on CPI monitoring

Week 4: Controlled Cutover
  - Cut over during low-traffic window
  - Monitor first 24 hours intensively
  - Keep PO channels in standby for 48 hours
  - Confirm Go-Live after 48 hours stable operation
```

---

## ISA-M Methodology

The Integration Solution Advisory Methodology (ISA-M) provides a structured framework for assessing and planning your integration landscape beyond a simple 1:1 PO-to-CPI migration.

### Integration Domains

| Domain | Description | Typical Scenarios |
|---|---|---|
| **A2A** (Application to Application) | Integration between applications within the enterprise | ERP to CRM, ERP to Warehouse, Master Data distribution |
| **B2B** (Business to Business) | Integration with external business partners | EDI (X12, EDIFACT), AS2 communication, Partner onboarding |
| **B2G** (Business to Government) | Integration with government/regulatory bodies | Tax reporting, customs declarations, e-invoicing |
| **Cloud2Cloud** (C2C) | Integration between cloud applications | SuccessFactors to S/4HANA Cloud, Ariba to S/4HANA |
| **On-Premise2Cloud** (O2C) | Hybrid integration between on-prem and cloud | ECC to SuccessFactors, On-prem to cloud data lake |

### Integration Styles

| Style | Description | SAP Technology Recommendation |
|---|---|---|
| **Process Integration** | Orchestrated, mediated message exchange between applications | Cloud Integration (iFlows) |
| **Data Integration** | Bulk/batch data movement, replication, synchronization | SAP Data Intelligence, CPI with JDBC/File |
| **Analytics Integration** | Feeding data to analytical systems and dashboards | SAP BW, SAP Analytics Cloud connectors |
| **User Integration** | Embedding applications/data into user-facing experiences | SAP Build Work Zone, SAP Fiori Launchpad |
| **Thing Integration (IoT)** | Connecting physical devices and sensors | SAP IoT, Edge Integration Cell |

### ISA-M Assessment Template

**Step 1: Inventory all integration scenarios**

Create a catalog with the following attributes per scenario:

| Attribute | Example Value |
|---|---|
| Scenario ID | INT-001 |
| Scenario Name | Sales Order Replication ECC to CRM |
| Domain | A2A |
| Style | Process Integration |
| Source System | SAP ECC 6.0 |
| Target System | SAP CRM 7.0 |
| Current Technology | SAP PO 7.5 |
| Interface Type | IDoc (SALESORDER) |
| Volume | 5,000 messages/day |
| Criticality | High |
| Complexity (from MAT) | Medium (Yellow) |

**Step 2: Map to ISA-M grid**

```
                    Process    Data     Analytics   User    Thing
                    Integr.    Integr.  Integr.     Integr. Integr.
                 +---------+---------+---------+---------+---------+
  A2A            |  142    |   23    |    8    |    5    |    -    |
                 +---------+---------+---------+---------+---------+
  B2B            |   45    |   12    |    -    |    -    |    -    |
                 +---------+---------+---------+---------+---------+
  B2G            |   18    |    3    |    -    |    -    |    -    |
                 +---------+---------+---------+---------+---------+
  Cloud2Cloud    |   34    |   15    |   11    |    7    |    -    |
                 +---------+---------+---------+---------+---------+
  On-Prem2Cloud  |   28    |    9    |    4    |    3    |    2    |
                 +---------+---------+---------+---------+---------+
```

**Step 3: Technology recommendation per cell**

| Domain + Style | Recommended Technology |
|---|---|
| A2A + Process Integration | Cloud Integration (primary) |
| A2A + Data Integration | Cloud Integration + CPI JDBC/File adapters |
| B2B + Process Integration | Integration Advisor + Trading Partner Management |
| B2B + Data Integration | Cloud Integration + B2B adapters (AS2, SFTP) |
| B2G + Process Integration | Cloud Integration + country-specific e-invoicing packages |
| Cloud2Cloud + Process Integration | Cloud Integration (pre-built packages from Discover) |
| On-Prem2Cloud + Process Integration | Cloud Integration + Cloud Connector |
| Any + Thing Integration | Edge Integration Cell |
| Any + User Integration | SAP Build Work Zone + API Management |

### Pattern: ISA-M Assessment for Manufacturing Company

```
Company Profile:
- SAP ECC 6.0 (on-premise), migrating to S/4HANA Cloud
- 50 shop-floor PLCs connected via OPC-UA
- 30 external suppliers via EDI
- SuccessFactors for HR, Ariba for procurement
- 347 integration scenarios in SAP PO

ISA-M Results:
- A2A Process: 142 scenarios -> Cloud Integration
- B2B Process:  45 scenarios -> Cloud Integration + Trading Partner Mgmt
- B2G Process:  18 scenarios -> Cloud Integration + e-invoicing packages
- C2C Process:  34 scenarios -> Cloud Integration (pre-built content)
- IoT Thing:     2 scenarios -> Edge Integration Cell
- Data Integr.: 62 scenarios -> Cloud Integration + Data Intelligence
- User Integr.: 15 scenarios -> Build Work Zone + API Management
- Analytics:    23 scenarios -> SAP Analytics Cloud connectors

Migration Priority:
  Wave 1: Low-complexity A2A (quick wins, 60 interfaces)
  Wave 2: B2B + B2G (partner impact, plan early)
  Wave 3: Medium-complexity A2A + C2C
  Wave 4: High-complexity A2A (BPM redesign)
  Wave 5: IoT + Data Integration
  Wave 6: Remaining + decommission PO
```

---

## Post-Migration Validation

### Functional Testing Checklist

| # | Test | Pass Criteria |
|---|---|---|
| 1 | Send standard message through CPI | Message delivered to receiver, correct format |
| 2 | Validate output payload field-by-field | All fields match expected values from PO output |
| 3 | Test with minimum payload (required fields only) | No errors, defaults applied correctly |
| 4 | Test with maximum payload (all optional fields) | All fields processed, no truncation |
| 5 | Test with special characters (umlauts, CJK, emoji) | Encoding preserved end-to-end (UTF-8) |
| 6 | Test error handling (invalid payload) | Exception Subprocess triggers, error logged in MPL |
| 7 | Test receiver unavailability | Retry executes per config, alert generated |
| 8 | Test sender authentication failure | Proper HTTP 401/403 returned, logged |
| 9 | Test large message (near size limit) | Processes within timeout, no OOM error |
| 10 | Test concurrent load (expected peak volume) | All messages processed, no queue backup |
| 11 | Test value mapping resolution | Correct values returned for all mapping groups |
| 12 | Test idempotent processing (send duplicate) | Duplicate detected or handled per design |

### Performance Benchmark Comparison

| Metric | SAP PO Baseline | CPI Target | Acceptable Threshold |
|---|---|---|---|
| Average latency (ms) | Measure per interface | Measure per interface | CPI within 150% of PO |
| Throughput (msg/min) | Measure at peak | Measure at peak | CPI >= 80% of PO |
| Error rate (%) | Measure over 1 week | Measure over 1 week | CPI <= PO error rate |
| Retry success rate (%) | Measure over 1 week | Measure over 1 week | CPI >= PO rate |
| Memory consumption | N/A (on-prem) | Monitor via Operations | Below tenant limits |

### Monitoring Setup for Migrated Interfaces

Post-migration, configure the following in CPI:

1. **Message Processing Log (MPL)**: Verify all migrated iFlows show entries in Monitor > Messages
2. **Alert Rules**: Create alert rules in SAP Alert Notification Service for `FAILED` and `RETRY` statuses
3. **Custom Header Logging**: Add `SAP_MplCorrelationId` and business key headers for traceability
4. **Cloud ALM Integration**: Register CPI tenant in Cloud ALM for centralized monitoring
5. **Trace Activation**: Enable trace for first 24 hours post-go-live, then disable (performance impact)

### Go-Live Criteria

A migrated interface is cleared for go-live when:

- All 12 functional tests pass
- Performance within acceptable thresholds
- Parallel run completed (minimum 2 weeks, recommended 4 weeks)
- Monitoring and alerting configured and tested
- Support team trained and has access to CPI Operations views
- Rollback procedure documented and dry-run completed
- Business owner sign-off obtained

### Hypercare Period Activities (2-4 Weeks)

| Week | Activities |
|---|---|
| Week 1 | Daily message count comparison, hourly monitoring, immediate triage of any failures |
| Week 2 | Compare error rates PO vs CPI, performance trend analysis, address any edge cases found |
| Week 3 | Reduce monitoring frequency to twice-daily, document known issues and resolutions |
| Week 4 | Final validation report, transition to BAU support model, deactivate PO channels for migrated interfaces |

---

## Best Practices

### Planning

1. **Start with low-complexity interfaces (quick wins)**
   - Build team confidence and CPI skills with simple migrations first
   - Validate the end-to-end migration process (tooling, testing, cutover) on easy interfaces
   - Demonstrate early success to stakeholders

2. **Build team skills gradually**
   - Send at least 2 developers to SAP Cloud Integration training before migration starts
   - Practice with CPI trial tenant before touching production interfaces
   - Create a migration playbook documenting team-specific procedures and naming conventions

3. **Invest in ISA-M assessment early**
   - Map the full landscape before starting migration
   - Identify interfaces that should be retired (not migrated)
   - Identify interfaces that will change dramatically with S/4HANA (redesign, not lift-and-shift)

4. **Estimate effort realistically**
   - MAT estimates are starting points; add buffer for testing, cutover, and hypercare
   - Rule of thumb: multiply MAT estimates by 1.5x for total migration effort (including project management, testing, documentation)
   - High-complexity BPM scenarios can take 3-4 weeks each including redesign

### Execution

1. **Migrate in waves, not big-bang**
   - Group interfaces by business process, not by technical complexity
   - Each wave should be independently testable and deployable
   - Typical wave size: 15-30 interfaces over 4-6 weeks

2. **Validate each wave before starting the next**
   - Do not accumulate migration debt by rushing through waves
   - Each wave must complete hypercare before the next wave goes live
   - Exception: parallel development of next wave can start during current wave's hypercare

3. **Maintain a migration dashboard**
   - Track: interfaces migrated, in-progress, remaining, blocked
   - Track: effort actual vs estimate per interface
   - Share dashboard with project steering committee weekly

### Testing

1. **Test with production-like data**
   - Use anonymized production data (not synthetic test data) for migration testing
   - Include edge cases found in production message logs
   - Test with actual production message volumes during performance testing

2. **Validate message counts and content**
   - Automate comparison: script that pulls MPL counts from PO and CPI, flags discrepancies
   - For critical interfaces, implement payload comparison at field level (not just hash)
   - Maintain a test evidence log for audit purposes

3. **Automate regression testing**
   - Build a Postman collection or similar test suite for each migrated interface
   - Run regression tests after every CPI tenant update (monthly SAP updates)
   - Include negative tests (malformed input, missing mandatory fields, oversized payload)

### Governance

1. **Document all migrated interfaces**
   - Maintain a migration registry: PO interface name, CPI iFlow name, migration date, wave number
   - Document any deviations from the original PO logic (intentional improvements)
   - Update CMDB and IT service catalog with new CPI endpoints

2. **Train support team**
   - Operations team must know how to: monitor MPL, restart failed messages, read trace logs, escalate
   - Create runbooks for each critical interface: expected behavior, common errors, resolution steps
   - Conduct handover sessions per wave from migration team to support team

3. **Decommission PO interfaces systematically**
   - Do not decommission PO channels until CPI interface has passed hypercare
   - Deactivate PO channels first (keep configuration for rollback); delete only after 30 days stable
   - Track PO decommissioning progress; plan for PO system shutdown when all interfaces migrated

---

## Next Steps

- See [cloud-integration.md](cloud-integration.md) for iFlow design patterns and process steps reference
- See [adapters.md](adapters.md) for detailed adapter configuration (sender/receiver)
- See [content-transport.md](content-transport.md) for promoting migrated iFlows across landscapes (Dev/QA/Prod)
- See [troubleshooting.md](troubleshooting.md) for resolving common migration and runtime issues
- See [cloud-connector.md](cloud-connector.md) for configuring Cloud Connector for on-premise PO/SAP system connectivity
- See [operations-monitoring.md](operations-monitoring.md) for monitoring migrated interfaces in production
- See [security.md](security.md) for configuring authentication and certificates for migrated interfaces

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about PO/PI migration planning, Migration Assessment Tool, ISA-M methodology, and interface migration patterns.
