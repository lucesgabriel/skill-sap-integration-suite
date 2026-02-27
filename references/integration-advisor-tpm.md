# Integration Advisor & Trading Partner Management

Comprehensive B2B integration reference: Integration Advisor (MIGs, MAGs), Trading Partner Management (TPM), B2B/B2G scenarios, and PO migration.

## Table of Contents

- [Integration Advisor Overview](#integration-advisor-overview)
- [Type Systems](#type-systems)
- [Message Implementation Guideline (MIG)](#message-implementation-guideline-mig)
- [Mapping Guideline (MAG)](#mapping-guideline-mag)
- [Trading Partner Management (TPM)](#trading-partner-management-tpm)
- [Agreement Templates and Lifecycle](#agreement-templates-and-lifecycle)
- [B2G Scenarios](#b2g-scenarios)
- [PO B2B Migration to CPI TPM](#po-b2b-migration-to-cpi-tpm)
- [AS2 Adapter Configuration for B2B](#as2-adapter-configuration-for-b2b)
- [B2B iFlow Patterns](#b2b-iflow-patterns)

---

## Integration Advisor Overview

Integration Advisor is an AI-powered capability within SAP Integration Suite for developing B2B/A2A integration content. It reduces the effort of creating mappings for industry-standard messages by leveraging machine learning proposals.

**Key capabilities:**
- Define message structures based on industry standards (MIGs)
- Create mappings between different message formats (MAGs)
- Generate runtime XSLT artifacts deployable to Cloud Integration
- AI-assisted mapping proposals based on learned patterns
- Library of pre-built content from SAP and community

**Access:** Integration Suite cockpit --> Design --> Integration Advisor

**Required Roles:**
- `AuthGroup_IntegrationDeveloper` - Create and edit MIGs/MAGs
- `AuthGroup_Administrator` - Manage type systems and libraries
- `AuthGroup_ReadOnly` - View-only access

---

## Type Systems

A Type System defines the standard message structure that serves as the foundation for MIGs. Integration Advisor supports these industry standards:

### UN/EDIFACT (United Nations Electronic Data Interchange for Administration, Commerce and Transport)

**Supported Versions:**
- D.93A through D.22A (annual and bi-annual releases)
- Common versions: D.96A, D.01B, D.04A, D.10B, D.16A, D.20A
- Each version contains hundreds of message types

**Key Message Types:**

| Message Type | Purpose | Common Segments |
|---|---|---|
| ORDERS | Purchase Order | BGM, DTM, NAD, LIN, QTY, MOA, UNS |
| ORDRSP | Order Response | BGM, DTM, RFF, NAD, LIN, QTY |
| DESADV | Dispatch Advice | BGM, DTM, NAD, CPS, LIN, QTY, PCI |
| INVOIC | Invoice | BGM, DTM, RFF, NAD, CUX, LIN, MOA, TAX |
| CONTRL | Syntax Acknowledgment | UCI, UCM, UCS, UCD |
| APERAK | Application Error/Ack | BGM, DTM, RFF, ERC |
| IFTMIN | Transport Instruction | BGM, DTM, TSR, NAD, GID, FTX |
| PAYMUL | Multiple Payment | BGM, DTM, RFF, FII, NAD, DOC |
| PRICAT | Price Catalogue | BGM, DTM, NAD, PGI, LIN, PRI |
| RECADV | Receiving Advice | BGM, DTM, NAD, LIN, QTY |

**EDIFACT Structure:**
```
UNA:+.? '           (Service String Advice - delimiters)
UNB+UNOC:3+SENDER+RECEIVER+240115:1030+REF001'  (Interchange Header)
UNH+1+ORDERS:D:96A:UN'   (Message Header)
BGM+220+PO-12345+9'       (Beginning of Message)
DTM+137:20240115:102'     (Date/Time)
...message segments...
UNT+15+1'                 (Message Trailer)
UNZ+1+REF001'             (Interchange Trailer)
```

### ASC X12 (Accredited Standards Committee X12)

**Supported Versions:**
- 003010 through 008030
- Common versions: 004010, 004030, 005010, 005030, 006020, 007060, 008010

**Key Transaction Sets:**

| Transaction Set | Purpose | Equivalent EDIFACT |
|---|---|---|
| 850 | Purchase Order | ORDERS |
| 855 | Purchase Order Acknowledgment | ORDRSP |
| 856 | Advance Ship Notice (ASN) | DESADV |
| 810 | Invoice | INVOIC |
| 997 | Functional Acknowledgment | CONTRL |
| 999 | Implementation Acknowledgment | CONTRL (enhanced) |
| 820 | Payment Order/Remittance Advice | PAYMUL |
| 846 | Inventory Inquiry/Advice | INVRPT |
| 860 | Purchase Order Change | ORDCHG |
| 870 | Order Status Report | - |
| 204 | Motor Carrier Load Tender | IFTMIN |
| 214 | Transportation Carrier Shipment Status | IFTSTA |
| 270/271 | Health Care Eligibility | - |
| 835 | Health Care Claim Payment | - |
| 837 | Health Care Claim | - |

**X12 Structure:**
```
ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *240115*1030*U*00401*000000001*0*P*:~
GS*PO*SENDER*RECEIVER*20240115*1030*1*X*004010~
ST*850*0001~
BEG*00*NE*PO-12345**20240115~
...transaction segments...
SE*15*0001~
GE*1*1~
IEA*1*000000001~
```

### SAP IDoc (Intermediate Document)

**Supported IDoc Types:**

| IDoc Type | Purpose | Message Type |
|---|---|---|
| ORDERS05 | Purchase Order | ORDERS |
| ORDRSP05 | Order Response | ORDRSP |
| DESADV05 | Delivery Notification | DESADV |
| INVOIC02 | Invoice | INVOIC |
| MATMAS05 | Material Master | MATMAS |
| DEBMAS07 | Customer Master | DEBMAS |
| CREMAS05 | Vendor Master | CREMAS |
| HRMD_A07 | HR Master Data | HRMD_A |
| WMMBID02 | Goods Movement | WMMBXY |
| PORDCR05 | Purchase Order Create | PORDCR |

**IDoc Structure:**
```xml
<ORDERS05>
  <IDOC BEGIN="1">
    <EDI_DC40 SEGMENT="1">      <!-- Control Record -->
      <TABNAM>EDI_DC40</TABNAM>
      <MANDT>100</MANDT>
      <DOCNUM>0000001234</DOCNUM>
      <IDOCTYP>ORDERS05</IDOCTYP>
      <MESTYP>ORDERS</MESTYP>
      <SNDPRT>LS</SNDPRT>
      <SNDPRN>PARTNER01</SNDPRN>
      <RCVPRT>LS</RCVPRT>
      <RCVPRN>SAPERP</RCVPRN>
    </EDI_DC40>
    <E1EDK01 SEGMENT="1">       <!-- Header Data -->
      <ACTION>000</ACTION>
      <CURCY>USD</CURCY>
    </E1EDK01>
    <E1EDP01 SEGMENT="1">       <!-- Item Data -->
      <POSEX>000010</POSEX>
      <MENGE>100.000</MENGE>
    </E1EDP01>
  </IDOC>
</ORDERS05>
```

### cXML (Commerce XML)

Used primarily for Ariba Network integration.

**Supported cXML Document Types:**
- OrderRequest / OrderConfirmation
- InvoiceDetailRequest
- ShipNoticeRequest
- PaymentRemittanceRequest / StatusUpdateRequest
- PunchOutSetupRequest / PunchOutOrderMessage
- CatalogUploadRequest

**cXML Structure:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE cXML SYSTEM "http://xml.cxml.org/schemas/cXML/1.2.060/cXML.dtd">
<cXML payloadID="1234567890@sender.com" timestamp="2024-01-15T10:30:00+00:00">
  <Header>
    <From><Credential domain="NetworkId"><Identity>AN01000000001</Identity></Credential></From>
    <To><Credential domain="NetworkId"><Identity>AN01000000002</Identity></Credential></To>
    <Sender>
      <Credential domain="NetworkId"><Identity>AN01000000001</Identity>
        <SharedSecret>s3cr3t</SharedSecret></Credential>
    </Sender>
  </Header>
  <Request deploymentMode="production">
    <OrderRequest>
      <OrderRequestHeader orderID="PO-12345" orderDate="2024-01-15" type="new">
        ...
      </OrderRequestHeader>
    </OrderRequest>
  </Request>
</cXML>
```

### GS1 XML

Used for retail/supply chain (GDSN, EPC, etc.):
- Order, DespatchAdvice, Invoice
- Based on GS1 Business Message Standard (BMS)

### UN/CEFACT (Cross-Industry Invoice, Supply Chain)

Supports UN/CEFACT XML schemas including:
- Cross-Industry Invoice (CII) - used in European e-invoicing
- Supply Chain Reference Data Model (SCRDM)

### Custom Type Systems

You can also create custom type systems:
1. Upload XSD schema files
2. Define custom message structures
3. Use as source/target in MIGs
4. Useful for proprietary formats or legacy systems

### Type System Versioning

Integration Advisor manages type system versions. When SAP updates a type system:
- Existing MIGs continue to work with their original version
- You can create new MIGs with updated versions
- Version comparison tools help identify changes

---

## Message Implementation Guideline (MIG)

A MIG defines how a specific message interface is implemented - which fields from the standard are used, which are mandatory/optional, and any value restrictions.

### MIG Creation Workflow (Detailed Steps)

**Step 1: Navigate and Create**
1. Open Integration Advisor: Integration Suite --> Design --> Integration Advisor
2. Click "Message Implementation Guidelines" in the left navigation
3. Click "Create"
4. Enter:
   - **Name:** e.g., `MIG_ORDERS_D96A_CustomerA`
   - **Description:** Purpose and scope of this MIG
   - **Type System:** Select from dropdown (e.g., UN/EDIFACT)
   - **Version:** Select version (e.g., D.96A)
   - **Message Type:** Select message (e.g., ORDERS)

**Step 2: Structure Customization**
After creation, the full message structure loads in the tree editor.

Operations available:
- **Mark as Mandatory:** Set cardinality to 1..1 or 1..n (red asterisk)
- **Mark as Optional:** Set cardinality to 0..1 or 0..n
- **Mark as Not Used:** Remove segments/elements from the implementation (grayed out, excluded from runtime)
- **Set Cardinality:** Define min/max occurrences (e.g., 1..99 for line items)

**For each node, you can configure:**

| Property | Description | Example |
|---|---|---|
| Min Occurs | Minimum occurrences | 0 (optional), 1 (mandatory) |
| Max Occurs | Maximum occurrences | 1, 99, unbounded |
| Data Type | Expected data type | AN (alphanumeric), N (numeric), DT (date) |
| Min Length | Minimum character length | 1 |
| Max Length | Maximum character length | 35 |
| Fixed Value | Hardcoded value | "220" for order type |
| Code List | Restricted value set | Qualifier codes |
| Notes | Implementation notes | Free text documentation |

**Step 3: Qualifier and Code Value Configuration**
For EDIFACT segments with qualifiers:
1. Select the qualifier element (e.g., DTM Segment -> Date/Time qualifier 2005)
2. Click "Maintain Code Values"
3. Select which codes are valid for this implementation:
   - `137` = Document/message date
   - `2` = Delivery date requested
   - `63` = Delivery date latest
   - `64` = Delivery date earliest
4. Each code value filters the segment structure accordingly

**Step 4: Documentation and Notes**
- Add implementation notes at any level (segment, group, element)
- Document business rules and constraints
- Add example values for clarity
- Notes appear in generated documentation

**Step 5: Validation**
1. Click "Validate" in the toolbar
2. Review any errors or warnings:
   - Missing mandatory elements
   - Invalid cardinality combinations
   - Conflicting qualifier settings
3. Fix issues before proceeding

**Step 6: Simulation and Testing**
1. Click "Simulate" to test with sample data
2. Upload a sample EDI/XML message
3. The simulator validates the message against the MIG definition
4. Shows pass/fail for each constraint

**Step 7: Activate**
1. Click "Activate" to make the MIG available for MAGs
2. Only activated MIGs can be used in Mapping Guidelines
3. Activation creates a versioned snapshot

### MIG Versioning
- Each MIG maintains version history
- Draft -> Active lifecycle
- New versions can be created from active versions
- Dependent MAGs reference specific MIG versions

### AI-Assisted MIG Creation
Integration Advisor uses machine learning to:
- Suggest which fields to include based on industry patterns
- Propose cardinality based on learned usage
- Recommend code value restrictions
- Auto-populate common qualifier codes

**Proposal confidence levels:**
- High (green) - Strong recommendation based on many samples
- Medium (yellow) - Moderate confidence
- Low (red) - Suggestion only, review carefully

---

## Mapping Guideline (MAG)

A MAG defines the mapping between a source MIG and a target MIG, and generates an XSLT runtime artifact.

### MAG Creation Workflow (Detailed Steps)

**Step 1: Create MAG**
1. Navigate to Integration Advisor -> Mapping Guidelines
2. Click "Create"
3. Enter:
   - **Name:** e.g., `MAG_X12_850_to_ORDERS05`
   - **Description:** Mapping purpose
   - **Source MIG:** Select source (e.g., X12 850 MIG)
   - **Target MIG:** Select target (e.g., IDoc ORDERS05 MIG)

**Step 2: Field-Level Mapping**
The mapping editor shows source on the left, target on the right.

**Mapping operations:**

| Operation | Description | Example |
|---|---|---|
| Direct mapping | 1:1 field copy | BEG03 (PO Number) -> BELNR (Document Number) |
| Concatenation | Join multiple source fields | FirstName + " " + LastName -> NAME1 |
| Substring | Extract part of field | First 10 chars of description |
| Code value mapping | Translate codes | X12 code "NE" -> IDoc code "000" |
| Constant | Set fixed value | "SAP" -> SNDPRT |
| Conditional | Map based on condition | If qualifier=137 then map to IDDAT |
| Lookup | Reference external table | Country code lookup |
| Mathematical | Calculate value | Quantity * Price = Amount |
| Date conversion | Format transformation | YYYYMMDD -> CCYYMMDD |

**Step 3: AI-Assisted Mapping Proposals**
1. Click "Propose Mappings" in the toolbar
2. Integration Advisor analyzes the source and target MIGs
3. AI proposes field-level mappings based on:
   - Semantic similarity of field names
   - Data type compatibility
   - Learned patterns from other mappings
   - Industry-standard mapping conventions
4. Review proposals:
   - Accept (green checkmark)
   - Reject (red X)
   - Modify (edit the proposed mapping)
5. Unmatched fields require manual mapping

**Step 4: Code Value Mapping**
For qualifier and code translations:
1. Select a mapped field with code lists
2. Click "Maintain Code Value Mapping"
3. Map source codes to target codes:

```
Source (X12 850)          Target (IDoc ORDERS05)
-------------------       ---------------------
NE (New Order)        ->  000 (Standard Order)
DS (Drop Ship)        ->  001 (Rush Order)
BK (Blanket Order)    ->  002 (Schedule Line)
```

**Step 5: Functions and Transformations**
Available mapping functions:
- **String:** concat, substring, trim, toUpper, toLower, replace, length, padLeft, padRight
- **Numeric:** add, subtract, multiply, divide, round, abs, floor, ceiling
- **Date/Time:** formatDate, parseDate, currentDate, addDays, dateDiff
- **Boolean:** if-then-else, exists, equals, and, or, not
- **Node:** count, sum, min, max, position, sort
- **Type conversion:** toString, toNumber, toDate
- **Custom:** Groovy-based functions for complex logic

**Step 6: Validation and Testing**
1. Click "Validate" to check mapping completeness
2. Verify:
   - All mandatory target fields have mappings
   - Data type compatibility
   - Code value coverage
3. Click "Simulate" with a sample source message
4. Review the generated target output
5. Compare expected vs. actual results

**Step 7: Generate Runtime Artifact**
1. Click "Generate Runtime Artifact"
2. Integration Advisor generates an **XSLT stylesheet**
3. The XSLT implements all defined mappings including:
   - Field transformations
   - Code value translations
   - Conditional logic
   - Structural reorganization
4. Download or directly deploy to Cloud Integration

### XSLT Runtime Artifact Details

The generated XSLT:
- Uses **XSLT 2.0** (Saxon processor)
- Includes all mapping logic as templates
- Handles namespace transformations
- Implements code value lookups as xsl:choose blocks
- Supports multi-occurrence segment handling with xsl:for-each
- Can be customized post-generation (not recommended - regeneration overwrites)

**Deployment to Cloud Integration:**
1. Export the XSLT artifact from Integration Advisor
2. In Cloud Integration, open target iFlow
3. Add XSLT Mapping step
4. Import the generated XSLT resource
5. Configure input/output message types

**Or use direct deployment (recommended):**
1. In the MAG, click "Deploy to Cloud Integration"
2. Select target integration package
3. The artifact is uploaded as a resource
4. Reference it in XSLT Mapping steps

---

## Trading Partner Management (TPM)

TPM streamlines B2B partner onboarding, agreement management, and runtime artifact generation.

### TPM Architecture

```
Partner Profiles ─┐
                  ├─> Agreement Templates ─> Agreements ─> Runtime Artifacts
Company Profile ──┘                                              │
                                                                 ↓
                                                    Auto-generated iFlows
                                                    (deployed to Cloud Integration)
```

### Company Profile Setup

Before onboarding partners, configure your own company profile:

**Step 1: Navigate to TPM**
Integration Suite --> Design --> B2B Scenarios (or Trading Partner Management)

**Step 2: Configure Company Profile**
1. Click "Company Profile"
2. Enter company details:
   - **Company Name:** Your organization name
   - **Identifier:** Primary business identifier
   - **Address:** Headquarters address
   - **Contact persons:** Technical and business contacts

**Step 3: Add Identifiers**
Add one or more business identifiers:

| Identifier Type | Description | Example |
|---|---|---|
| AS2 ID | AS2 message identifier | `MYCOMPANY-AS2-001` |
| DUNS Number | Dun & Bradstreet number | `123456789` |
| GLN | Global Location Number | `1234567890123` |
| EAN/UPC | Product identifier | `0123456789012` |
| Mutually Defined | Custom partner ID | `COMP-001` |
| VAT Number | Tax identifier | `DE123456789` |

**Step 4: Add Systems**
Define your integration systems:

| System Property | Description | Example |
|---|---|---|
| Name | Descriptive name | `SAP_S4HANA_PROD` |
| Type | System type | SAP S/4HANA, ECC, Third-Party |
| Adapter | Communication protocol | AS2, SFTP, HTTPS, AS4 |
| Address | System endpoint | `https://mycompany.as2server.com/as2` |
| Authentication | Auth method | Client Certificate, Basic Auth |

**Step 5: Add Certificates**
Upload security material:
- **Signing Certificate:** Your private key for outbound signing
- **Encryption Certificate:** Partner's public key for outbound encryption
- **Verification Certificate:** Partner's public key for inbound signature verification
- **Decryption Certificate:** Your private key for inbound decryption

### Partner Profile Onboarding

**Step 1: Create Partner Profile**
1. Navigate to TPM -> Trading Partners
2. Click "Create"
3. Enter:
   - **Name:** Partner company name
   - **Short Name:** Abbreviated identifier (used in system references)
   - **Contact person:** Name, email, phone

**Step 2: Add Partner Identifiers**
Same identifier types as company profile:
- AS2 ID (critical for AS2 communication)
- DUNS Number, GLN, etc.
- Custom identifiers as needed

**Step 3: Add Partner Systems**
Define partner's integration endpoints:
```
System Name: Partner_AS2_System
Type: Third-Party
Adapter: AS2
  - AS2 Partner URL: https://partner.com/as2/receive
  - AS2 ID: PARTNER-AS2-001
  - MDN URL: (same or different for async MDN)
```

**Step 4: Upload Partner Certificates**
- Partner's public signing certificate (for signature verification)
- Partner's public encryption certificate (for outbound encryption)

**Step 5: Define Supported Message Types**
Specify which messages this partner can send/receive:
- EDIFACT ORDERS, INVOIC, DESADV
- X12 850, 810, 856
- cXML OrderRequest, InvoiceDetailRequest
- Custom XML schemas

**Step 6: Configure Communication Parameters**

For AS2:

| Parameter | Description | Example |
|---|---|---|
| AS2 From ID | Sender AS2 ID | `MYCOMPANY-AS2-001` |
| AS2 To ID | Receiver AS2 ID | `PARTNER-AS2-001` |
| URL | Partner AS2 endpoint | `https://partner.com/as2` |
| MDN Type | Sync or Async | Synchronous |
| MDN Signing | Sign MDN? | SHA-256 |
| Encryption Algorithm | Message encryption | 3DES, AES128, AES256 |
| Signing Algorithm | Message signing | SHA-1, SHA-256, SHA-512 |
| Compression | Compress payload? | Yes (zlib) |
| Content Type | MIME type | application/edi-x12, application/edifact |

For SFTP:

| Parameter | Description | Example |
|---|---|---|
| Host | SFTP server | `sftp.partner.com` |
| Port | SFTP port | 22 |
| Directory | File location | `/inbound/edi` |
| Authentication | Auth method | Public Key, Password |
| File Pattern | Naming convention | `PO_*.edi` |
| Post-Processing | After pickup | Archive, Delete |

---

## Agreement Templates and Lifecycle

### Agreement Template Structure

An agreement template predefines the integration pattern for a specific B2B scenario.

**Template components:**

1. **Sender/Receiver Configuration:**
   - Which party sends, which receives
   - Applicable for one-way or request-response

2. **Message Flow:**
   - Source message type (e.g., X12 850)
   - Target message type (e.g., IDoc ORDERS05)
   - Mapping reference (MAG)
   - Pre/post processing steps

3. **Communication Channel:**
   - Protocol (AS2, SFTP, AS4, HTTPS)
   - Security settings (signing, encryption)
   - Acknowledgment settings (MDN, 997, CONTRL)

4. **Processing Rules:**
   - Validation requirements
   - Error handling behavior
   - Duplicate check settings
   - Retry configuration

### Creating Agreement Templates

**Step 1: Create Template**
1. Navigate to TPM -> Agreement Templates
2. Click "Create"
3. Enter template name and description
4. Select direction: Inbound (partner to you) or Outbound (you to partner)

**Step 2: Define Activity Parameters**

**Sender side:**
```
Adapter Type: AS2
Security:
  - Verify Signature: Yes
  - Decrypt: Yes (using company private key)
  - Send MDN: Yes (synchronous, signed)
Message Type: UN/EDIFACT INVOIC D.96A
```

**Receiver side:**
```
Adapter Type: IDoc
System: SAP_S4HANA_PROD
IDoc Type: INVOIC02
Message Type: INVOIC
Process Code: INVO
```

**Step 3: Define Interchange Processing**
```
Interchange:
  - EDI Splitter: Yes (split batched interchanges)
  - Syntax Validation: Yes (EDIFACT syntax check)
  - Duplicate Check: Yes (check UNB reference number)

Acknowledgment:
  - Generate CONTRL: Yes (functional acknowledgment)
  - Send to: Original sender system
```

**Step 4: Add Mapping**
- Reference the MAG created in Integration Advisor
- Or specify a custom XSLT mapping
- Or use pass-through (no transformation)

**Step 5: Configure Error Handling**
```
On Error:
  - Retry: 3 times with exponential backoff
  - Alert: Send email to B2B admin
  - Store: Keep failed message in error queue
  - Negative Ack: Send CONTRL with error code
```

### Agreement Lifecycle

```
Draft --> Active --> Suspended --> Renewed/Terminated
  │         │           │
  │         │           └── Reactivate
  │         └── Suspend (temporary pause)
  └── Edit and finalize
```

**Lifecycle states:**

| State | Description | Actions Available |
|---|---|---|
| Draft | Being configured | Edit, Validate, Delete |
| Active | In production use | Suspend, Update, Deploy |
| Suspended | Temporarily paused | Reactivate, Terminate |
| Terminated | Permanently ended | Archive, Delete |

**Creating an Agreement from Template:**
1. Select an agreement template
2. Choose the trading partner
3. Override template defaults if needed (e.g., different protocol for this partner)
4. Partner-specific parameters are populated from the partner profile
5. Validate the agreement
6. Activate to generate and deploy runtime artifacts

### Automated Runtime Artifact Generation

When an agreement is activated, TPM **automatically generates**:

1. **Sender iFlow:** Receives messages from the trading partner
   - Adapter configuration (AS2/SFTP/HTTPS)
   - Decryption and signature verification
   - EDI syntax validation
   - Interchange splitting
   - Acknowledgment generation

2. **Receiver iFlow:** Sends messages to the trading partner
   - Message transformation (MIG/MAG)
   - Signing and encryption
   - Partner endpoint routing
   - MDN handling

3. **Mapping Artifacts:** XSLT resources from referenced MAGs

4. **Security Artifacts:** Certificate assignments and key references

**These iFlows are deployed automatically to Cloud Integration runtime.**

### Agreement Monitoring

Monitor active agreements in:
- TPM Dashboard: Agreement status, message counts, errors
- Cloud Integration MPL: Individual message traces
- Alert rules: Automatic notifications for failures

---

## B2G Scenarios

Business-to-Government scenarios for electronic invoicing compliance.

### European E-Invoicing

**Peppol (Pan-European Public Procurement OnLine)**

Peppol uses AS4 protocol with SMP (Service Metadata Publisher) for dynamic endpoint discovery.

**Peppol Integration Architecture:**
```
Supplier ERP --> CPI iFlow --> Peppol Access Point (AS4) --> Peppol Network --> Government SMP --> Government Access Point
```

**Key components:**
- **Peppol ID:** Participant identifier (e.g., `0088:1234567890123`)
- **Document Type:** PINT (Peppol International), BIS Billing 3.0
- **Process ID:** `urn:fdc:peppol.eu:2017:poacc:billing:01:1.0`
- **Transport Profile:** AS4 (busdox-transport-as4-v1p0)

**CPI Configuration for Peppol:**
1. Register as Peppol Access Point (or use certified provider)
2. Configure AS4 adapter with Peppol security profile
3. Implement SMP lookup for dynamic receiver discovery
4. Map business document to UBL 2.1 or CII format
5. Add Peppol SBDH (Standard Business Document Header) envelope

**Peppol BIS Billing 3.0 Invoice (UBL 2.1):**
```xml
<Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
  <cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0</cbc:CustomizationID>
  <cbc:ProfileID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</cbc:ProfileID>
  <cbc:ID>INV-2024-0001</cbc:ID>
  <cbc:IssueDate>2024-01-15</cbc:IssueDate>
  <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>
  <cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode>
  <cac:AccountingSupplierParty>...</cac:AccountingSupplierParty>
  <cac:AccountingCustomerParty>...</cac:AccountingCustomerParty>
  <cac:TaxTotal>...</cac:TaxTotal>
  <cac:LegalMonetaryTotal>...</cac:LegalMonetaryTotal>
  <cac:InvoiceLine>...</cac:InvoiceLine>
</Invoice>
```

### UBL (Universal Business Language) 2.1

UBL is the standard format for many e-invoicing mandates:

**Supported UBL Document Types:**
- Invoice (UBL 2.1)
- CreditNote
- Order
- OrderResponse
- DespatchAdvice
- ReceiptAdvice

**UBL 2.1 Key Namespaces:**
```
urn:oasis:names:specification:ubl:schema:xsd:Invoice-2
urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2
urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2
urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2
```

### EN 16931 (European Norm)

The EU standard for electronic invoicing:
- **Semantic model:** Defines required business terms
- **Syntax bindings:** UBL 2.1 and UN/CEFACT CII
- **CIUS:** Core Invoice Usage Specification (country-specific rules)

**Country-specific implementations:**

| Country | Standard | Format | Notes |
|---|---|---|---|
| Germany | XRechnung | UBL 2.1 / CII | Mandatory for B2G since 2020 |
| Italy | FatturaPA / SDI | Italian XML (FatturaPA) | Mandatory B2B since 2019 |
| France | Factur-X | Hybrid PDF/A-3 + CII XML | B2B mandate phased 2024-2026 |
| Spain | TicketBAI / SII | Spanish XML | Real-time reporting |
| Poland | KSeF | Polish structured e-invoice | National system |
| India | GST e-Invoice | JSON (India-specific) | IRN-based system |
| Saudi Arabia | ZATCA FATOORA | UBL 2.1 + Arabic extensions | Phased rollout |

### SAP Document Compliance

SAP provides pre-built integration content for e-invoicing compliance:

**SAP Document Compliance (formerly SAP DRC):**
- Pre-built iFlow packages in Integration Suite
- Country-specific mapping and validation
- Government portal connectivity
- Digital signature and QR code generation
- Archival and audit trail

**Integration pattern:**
```
SAP S/4HANA --> CPI (SAP Document Compliance iFlow) --> Country-specific API/Portal
     │                        │
     │                  [Validation]
     │                  [Mapping to local format]
     │                  [Digital signing]
     │                  [QR code generation]
     └──── Response <── [Status callback]
```

---

## PO B2B Migration to CPI TPM

Migrating B2B scenarios from SAP Process Orchestration (PO/PI) to Cloud Integration TPM.

### Assessment Phase

**Step 1: Inventory Current PO B2B Scenarios**
1. Export PO Integration Directory configuration
2. Catalog all B2B interfaces:
   - Partner agreements (sender/receiver)
   - Communication channels (AS2, SFTP, FTP)
   - Interface mappings (operation mappings)
   - Conversion rules (EDI -> XML -> IDoc)
3. Document partner-specific configurations:
   - Certificates and security settings
   - Retry/error handling rules
   - Scheduling and polling intervals

**Step 2: Analyze Complexity**

| PO B2B Component | CPI TPM Equivalent | Migration Effort |
|---|---|---|
| Partner Agreement | TPM Agreement | Medium (recreate) |
| B2B Communication Channel (AS2) | TPM + AS2 Adapter | Medium |
| EDI Converter Module | Integration Advisor MIG | Medium-High |
| Mapping Program (Java/XSLT) | MAG (XSLT generated) | High (recreate) |
| BPM Process | iFlow with TPM runtime artifacts | High |
| EDIINT AS2 Adapter | CPI AS2 Adapter | Low-Medium |
| Party/Service/Channel | TPM Partner Profile + Systems | Medium |
| Monitoring (PIMON) | MPL + TPM Dashboard | Low |

### Migration Strategy

**Recommended approach: Phased Partner Migration**

**Phase 1: Foundation (2-4 weeks)**
1. Set up Integration Advisor type systems
2. Create MIGs for all used message types
3. Create MAGs for all mapping scenarios
4. Test generated XSLT artifacts with sample data

**Phase 2: TPM Configuration (2-4 weeks)**
1. Create company profile in TPM
2. Create agreement templates for each B2B pattern
3. Import/upload certificates
4. Configure communication parameters

**Phase 3: Partner Migration (per partner, 1-2 weeks each)**
1. Create partner profile in TPM
2. Upload partner certificates
3. Create agreement from template
4. Test with partner (parallel run with PO)
5. Cut over to CPI
6. Decommission PO scenario for this partner

**Phase 4: Decommission PO (after all partners migrated)**
1. Verify all partners are running on CPI TPM
2. Disable PO B2B scenarios
3. Archive PO configuration
4. Decommission B2B modules

### Mapping Migration

**PO operation mappings to Integration Advisor MAGs:**

| PO Artifact | CPI Equivalent | Notes |
|---|---|---|
| Message Mapping (graphical) | Integration Advisor MAG | Recreate; IA has AI proposals |
| XSLT Mapping | XSLT resource in iFlow | Can often reuse with namespace adjustments |
| Java Mapping | Groovy Script or XSLT | Must rewrite; no Java mapping in CPI |
| ABAP Mapping | Groovy Script or Message Mapping | Must rewrite |
| Value Mapping (ID lookup) | CPI Value Mapping artifact | Recreate; similar concept |
| Parameterized Mapping | Externalized Parameters + XSLT | Different mechanism |

### Testing Strategy

1. **Unit testing:** Test each MIG/MAG with sample EDI/XML messages
2. **Integration testing:** End-to-end with test partner endpoints
3. **Parallel run:** Run both PO and CPI simultaneously, compare results
4. **Partner UAT:** Partner validates message exchange
5. **Performance testing:** Verify throughput matches PO levels

---

## AS2 Adapter Configuration for B2B

### AS2 Sender Adapter (Inbound - Receive from Partner)

**Connection Tab:**

| Parameter | Description | Example |
|---|---|---|
| Address | Endpoint path | `/as2/receive` |
| Authorization | Client cert or user role | `ESBMessaging.send` |
| User Role | Required role | `ESBMessaging.send` |

**Processing Tab:**

| Parameter | Description | Options |
|---|---|---|
| Message ID Left Part | AS2 message ID prefix | `${header.AS2-From}` |
| Message ID Right Part | AS2 message ID suffix | `mycompany.com` |
| Partner AS2 ID | Expected sender AS2 ID | `PARTNER-AS2-001` |
| Own AS2 ID | Your AS2 ID | `MYCOMPANY-AS2-001` |
| Message Security | Verify/Decrypt | Verify Signature, Decrypt Message |
| Signature Algorithm | Signature verification | SHA-256, SHA-512 |

**MDN Tab:**

| Parameter | Description | Options |
|---|---|---|
| MDN Type | Synchronous or Asynchronous | Synchronous (recommended) |
| MDN Signing | Sign MDN | SHA-256 |
| Private Key Alias | Key for MDN signing | `my-signing-key` |
| MDN Response | Success/Error response | Automatic |

**Security Tab:**

| Parameter | Description | Value |
|---|---|---|
| Verify Signature | Validate partner signature | Enabled |
| Public Key Alias | Partner's public key | `partner-signing-cert` |
| Decrypt Message | Decrypt incoming message | Enabled |
| Private Key Alias | Your private key | `my-encryption-key` |

### AS2 Receiver Adapter (Outbound - Send to Partner)

**Connection Tab:**

| Parameter | Description | Example |
|---|---|---|
| Recipient URL | Partner AS2 endpoint | `https://partner.com/as2/receive` |
| Proxy Type | Internet or On-Premise | Internet |
| Authentication | Auth method | Client Certificate |
| Private Key Alias | Client cert key | `my-client-cert` |
| Timeout | Connection timeout (ms) | 60000 |

**Processing Tab:**

| Parameter | Description | Example |
|---|---|---|
| Own AS2 ID | Your AS2 ID (From) | `MYCOMPANY-AS2-001` |
| Partner AS2 ID | Partner AS2 ID (To) | `PARTNER-AS2-001` |
| Message Subject | AS2 subject line | `EDI Order - ${date:now:yyyyMMdd}` |
| Content Type | MIME type | `application/edi-x12` |
| Content Transfer Encoding | Encoding | `binary` |
| File Name | Attachment name | `order_${property.orderId}.edi` |

**Security Tab:**

| Parameter | Description | Options |
|---|---|---|
| Sign Message | Sign outbound message | Enabled |
| Signing Algorithm | Algorithm | SHA-256 |
| Private Key Alias | Your signing key | `my-signing-key` |
| Encrypt Message | Encrypt outbound | Enabled |
| Encryption Algorithm | Algorithm | AES128, AES256, 3DES |
| Public Key Alias | Partner's encryption cert | `partner-encryption-cert` |
| Compress Message | ZLIB compression | Yes/No |

**MDN Tab:**

| Parameter | Description | Options |
|---|---|---|
| Request MDN | Request receipt | Yes |
| MDN Type | Synchronous or Async | Synchronous |
| Request Signed MDN | MDN must be signed | Yes |
| MDN Signing Algorithm | MDN signature algo | SHA-256 |

### AS2 Certificate Management

**Certificate deployment steps:**
1. Navigate to Monitor -> Manage Security -> Keystore
2. Upload partner certificates:
   - `partner_signing_cert.cer` -> alias: `partner-signing-cert`
   - `partner_encryption_cert.cer` -> alias: `partner-encryption-cert`
3. Upload your key pairs:
   - `my_signing_key.p12` -> alias: `my-signing-key`
   - `my_encryption_key.p12` -> alias: `my-encryption-key`
   - `my_client_cert.p12` -> alias: `my-client-cert`

**Certificate renewal process:**
1. Receive new certificate from partner
2. Upload to Keystore with new alias (e.g., `partner-signing-cert-2025`)
3. Update adapter configuration to reference new alias
4. Test with partner
5. Remove old certificate after successful transition

---

## B2B iFlow Patterns

### Pattern 1: Inbound EDI Processing (Partner to SAP)

```
[AS2 Sender] --> [MIME Decode] --> [EDI Splitter] --> [EDI to XML Converter]
                                                           |
                                                     [XSLT Mapping (MAG)]
                                                           |
                                                     [XML Validation]
                                                           |
                                                     [IDoc Adapter] --> [SAP S/4HANA]

Exception: [Generate Negative Ack] --> [AS2: Send to Partner]
```

**Steps:**
1. AS2 Sender receives EDI from partner (auto-decrypts, verifies signature)
2. MIME decoder extracts payload
3. EDI Splitter splits batched interchanges/transactions
4. EDI to XML converts EDI syntax to XML
5. XSLT mapping (from MAG) transforms to IDoc XML
6. XML validation against IDoc schema
7. IDoc adapter sends to SAP S/4HANA
8. Generate functional acknowledgment (997/CONTRL)

### Pattern 2: Outbound EDI Processing (SAP to Partner)

```
[IDoc Sender] --> [IDoc to XML] --> [XSLT Mapping (MAG)] --> [XML to EDI Converter]
                                                                    |
                                                              [EDI Envelope]
                                                                    |
                                                              [AS2 Receiver] --> [Partner]

Exception: [Error Queue] --> [Alert Notification]
```

### Pattern 3: Acknowledgment Flow

```
[AS2 Sender: Receive 997/CONTRL] --> [Parse Ack] --> [Router]
    |-- Accepted --> [Update Status: Success] --> [Log]
    |-- Rejected --> [Update Status: Error] --> [Alert] --> [Retry Queue]
    |-- Partial --> [Update Status: Partial] --> [Alert]
```

### Pattern 4: AS2 with MDN Correlation

```
Outbound:
[Send EDI via AS2] --> [Store Message ID in DataStore]
                       [Receive Sync MDN] --> [Verify MDN Signature]
                                                |
                                          [Match with DataStore]
                                                |
                                          [Update Processing Status]
```

---

**Next Steps:**
- See [adapters.md](adapters.md) for AS2/AS4 adapter configuration details
- See [security.md](security.md) for certificate and encryption setup
- See [cloud-integration.md](cloud-integration.md) for iFlow design patterns
- See [migration-assessment.md](migration-assessment.md) for PO migration methodology

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about Integration Advisor, Trading Partner Management, B2B/EDI patterns, and type systems (EDIFACT, X12, IDoc).
