# Message Mapping & XSLT Reference

Comprehensive guide for graphical message mappings, XSLT transformations, value mappings, and custom mapping functions in SAP Cloud Integration.

## Table of Contents

- [Mapping Types Overview](#mapping-types-overview)
- [Message Mapping (Graphical)](#message-mapping-graphical)
- [Built-in Mapping Functions](#built-in-mapping-functions)
- [Custom Functions (Groovy)](#custom-functions-groovy)
- [Value Mapping](#value-mapping)
- [Multi-Source Mapping](#multi-source-mapping)
- [XSLT Mapping](#xslt-mapping)
- [XSLT Patterns for CPI](#xslt-patterns-for-cpi)
- [Namespace Handling](#namespace-handling)
- [Large Payload Mapping](#large-payload-mapping)
- [Testing Mappings](#testing-mappings)
- [Common Errors and Fixes](#common-errors-and-fixes)
- [Best Practices](#best-practices)

---

## Mapping Types Overview

| Type | Best For | Complexity | Performance |
|---|---|---|---|
| **Message Mapping** | Structure-to-structure, visual drag-and-drop | Low-Medium | Good |
| **XSLT Mapping** | Complex conditionals, namespace-heavy XML, templates | Medium-High | Very Good |
| **Groovy Script** | Non-XML formats, API calls during mapping, dynamic logic | High | Varies |
| **Content Modifier** | Simple header/property extraction, static assignments | Very Low | Excellent |

### Decision Guide

```
Is the transformation XML-to-XML?
  ├─ Yes → Is it a simple field-to-field mapping?
  │         ├─ Yes → Use Message Mapping (graphical)
  │         └─ No  → Does it require complex templates or recursion?
  │                   ├─ Yes → Use XSLT Mapping
  │                   └─ No  → Use Message Mapping with custom functions
  └─ No  → Is it JSON processing or non-XML?
            ├─ Yes → Use Groovy Script
            └─ No  → Use Content Modifier (for simple assignments)
```

---

## Message Mapping (Graphical)

### Overview

The graphical Message Mapping editor provides a visual drag-and-drop interface for mapping source fields to target fields. It supports XML schemas (XSD), WSDL, and IDoc structures.

### Creating a Message Mapping

**Navigation:** Design → Select Package → Add → Message Mapping

**Steps:**
1. Add source schema (upload XSD, WSDL, or select from ESR)
2. Add target schema
3. Connect source fields to target fields by dragging lines
4. Add functions between fields for transformations
5. Test with sample data
6. Save and reference in iFlow

### Schema Import Options

| Source | How to Import |
|---|---|
| **XSD file** | Upload → Browse → Select .xsd file |
| **WSDL file** | Upload → Browse → Select .wsdl file |
| **IDoc structure** | From ESR or manual XSD from IDoc documentation |
| **JSON schema** | Convert to XSD first (JSON not natively supported in graphical mapper) |
| **OData metadata** | Download $metadata → Convert to XSD |

### Field Mapping Cardinalities

| Cardinality | Description | Example |
|---|---|---|
| **1:1** | One source → One target | `SourceOrderID → TargetOrderID` |
| **1:N** | One source → Multiple targets | `FullName → FirstName + LastName` |
| **N:1** | Multiple sources → One target | `Street + City + ZIP → FullAddress` |
| **N:M** | Multiple sources → Multiple targets | Complex restructuring with context changes |

---

## Built-in Mapping Functions

### String Functions

| Function | Description | Example |
|---|---|---|
| `Concat` | Concatenate strings | `"Mr." + " " + Name` → `"Mr. Smith"` |
| `Substring` | Extract part of string | `Substring("SAP CPI", 0, 3)` → `"SAP"` |
| `Length` | String length | `Length("SAP")` → `3` |
| `ToUpperCase` | Convert to uppercase | `"hello"` → `"HELLO"` |
| `ToLowerCase` | Convert to lowercase | `"HELLO"` → `"hello"` |
| `Trim` | Remove leading/trailing spaces | `" SAP "` → `"SAP"` |
| `Replace` | Replace substring | `Replace("2024-01-15", "-", "")` → `"20240115"` |
| `Split` | Split by delimiter | `Split("A;B;C", ";")` → `["A","B","C"]` |
| `Contains` | Check if contains substring | `Contains("SAP CPI", "CPI")` → `true` |

### Date Functions

| Function | Description | Example |
|---|---|---|
| `DateTrans` | Transform date format | `"20240115"` (yyyyMMdd) → `"2024-01-15"` (yyyy-MM-dd) |
| `CurrentDate` | Get current date | Returns `"2024-01-15"` |
| `DateAdd` | Add days/months/years | `DateAdd("2024-01-15", 30, "days")` |

**Date Format Patterns:**

| Pattern | Example | Description |
|---|---|---|
| `yyyy-MM-dd` | 2024-01-15 | ISO date |
| `yyyyMMdd` | 20240115 | SAP internal date |
| `dd.MM.yyyy` | 15.01.2024 | European format |
| `MM/dd/yyyy` | 01/15/2024 | US format |
| `yyyy-MM-dd'T'HH:mm:ss'Z'` | 2024-01-15T10:30:00Z | ISO 8601 |

### Arithmetic Functions

| Function | Description |
|---|---|
| `Add` | Addition |
| `Subtract` | Subtraction |
| `Multiply` | Multiplication |
| `Divide` | Division |
| `Round` | Round to N decimal places |
| `Floor` / `Ceil` | Round down / up |

### Boolean / Conditional Functions

| Function | Description | Use Case |
|---|---|---|
| `If` | Conditional mapping | Map field only if condition is true |
| `IfWithoutElse` | Conditional without else | Suppress output when false |
| `Equals` | String equality check | Route based on value |
| `Greater` / `Less` | Numeric comparison | Amount thresholds |
| `And` / `Or` / `Not` | Logical operators | Combine conditions |
| `Exists` | Check if source field exists | Handle optional fields |

### Node Functions

| Function | Description | Use Case |
|---|---|---|
| `CreateIf` | Create target node conditionally | Only create `<Address>` if fields exist |
| `Collapse` | Remove context (flatten) | Flatten nested repeating structures |
| `SplitByValue` | Create new context per value change | Group items by category |
| `Sort` | Sort by value | Alphabetical/numerical ordering |
| `RemoveContext` | Reset context to root | Cross-context mapping |
| `FormatByExample` | Format value by pattern | Number formatting (e.g., `"1234.50"` → `"1,234.50"`) |

---

## Custom Functions (Groovy)

### Creating a Custom Function

In the Message Mapping editor, you can create User-Defined Functions (UDFs) using Groovy.

**Steps:**
1. In the mapping editor, click **Create** → **Function** → **User-Defined Function**
2. Choose function type:
   - **Simple**: Takes single values as input, returns single value
   - **Queue**: Takes arrays of values, returns arrays
   - **Cache**: Executes once, caches result

### Simple Function Example

```groovy
// Function: formatAmount
// Input: amount (String)
// Output: formatted amount (String)
def formatAmount(String amount) {
    if (amount == null || amount.isEmpty()) return "0.00"
    def value = new BigDecimal(amount)
    return String.format("%.2f", value)
}
```

### Queue Function Example (for repeating elements)

```groovy
// Function: deduplicateItems
// Input: items[] (String array)
// Output: unique items[] (String array)
import java.util.LinkedHashSet

def deduplicateItems(String[] items, Output output, MappingContext context) {
    def seen = new LinkedHashSet<String>()
    items.each { item ->
        if (item != null && !item.isEmpty() && seen.add(item)) {
            output.addValue(item)
        }
    }
}
```

### Cache Function Example

```groovy
// Function: getExchangeRate (called once, cached for all occurrences)
import com.sap.it.api.mapping.MappingContext

def getExchangeRate(String currency, MappingContext context) {
    // Access exchange rate from external source or property
    def rates = ["USD": "1.0", "EUR": "0.85", "GBP": "0.73"]
    return rates.get(currency, "1.0")
}
```

---

## Value Mapping

### Overview

Value Mapping provides lookup tables for code/value translations between systems. For example, mapping SAP country codes to ISO country codes.

### Creating Value Mappings

**Navigation:** Design → Select Package → Add → Value Mapping

**Configuration:**
1. Define identifiers (source agency + schema, target agency + schema)
2. Add value pairs:

| Source (SAP) | Target (ISO) |
|---|---|
| `DE` | `DEU` |
| `US` | `USA` |
| `GB` | `GBR` |
| `FR` | `FRA` |

### Using Value Mapping in Message Mapping

In the graphical mapping editor:
1. Drag source field to the `ValueMapping` function
2. Configure:
   - Source Agency: `SAP`
   - Source Identifier: `CountryCode`
   - Target Agency: `ISO`
   - Target Identifier: `CountryCode3`
3. Connect output to target field

### Using Value Mapping in Groovy Script

```groovy
import com.sap.it.api.mapping.ValueMappingApi
import com.sap.it.api.ITApiFactory

def valueMappingApi = ITApiFactory.getService(ValueMappingApi.class, null)

// Lookup: SAP country code → ISO 3-letter code
def isoCode = valueMappingApi.getMappedValue(
    "SAP", "CountryCode", "DE",    // Source: agency, schema, value
    "ISO", "CountryCode3"           // Target: agency, schema
)
// Result: isoCode = "DEU"
```

---

## Multi-Source Mapping

### Overview

Multi-source mapping combines data from multiple source messages into a single target structure. Useful for enrichment scenarios where data comes from different sources.

### Setup

1. Add multiple source schemas to the mapping
2. Add target schema
3. Map fields from different sources to the target
4. Handle context alignment between sources

### Pattern: Combine Order + Customer Data

```
Source 1 (Order XML):           Source 2 (Customer XML):
<Order>                         <Customer>
  <OrderID>1234</OrderID>        <CustomerID>C001</CustomerID>
  <CustomerRef>C001</CustomerRef> <Name>ACME Corp</Name>
  <Amount>5000</Amount>          <City>Berlin</City>
</Order>                        </Customer>

Target (Combined):
<EnrichedOrder>
  <OrderID>1234</OrderID>       ← from Source 1
  <Amount>5000</Amount>          ← from Source 1
  <CustomerName>ACME Corp</CustomerName> ← from Source 2
  <CustomerCity>Berlin</CustomerCity>     ← from Source 2
</EnrichedOrder>
```

**In iFlow:** Use Content Enricher step before the mapping to load Source 2 into a property, then reference both in the mapping.

---

## XSLT Mapping

### Overview

XSLT (Extensible Stylesheet Language Transformations) provides template-based XML transformations. SAP Cloud Integration supports XSLT 1.0, 2.0, and 3.0.

### Adding XSLT Mapping to iFlow

**Steps:**
1. Create XSLT resource file (`.xsl`)
2. Upload to iFlow resources or Script Collection
3. Add **XSLT Mapping** step to iFlow
4. Reference the XSLT resource file

---

## XSLT Patterns for CPI

### Pattern 1: Simple Field Mapping

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <TargetOrder>
            <OrderNumber><xsl:value-of select="//SourceOrder/OrderID"/></OrderNumber>
            <CustomerName><xsl:value-of select="//SourceOrder/Customer/Name"/></CustomerName>
            <TotalAmount><xsl:value-of select="format-number(//SourceOrder/Amount, '#,##0.00')"/></TotalAmount>
        </TargetOrder>
    </xsl:template>
</xsl:stylesheet>
```

### Pattern 2: Conditional Transformation

```xml
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <Orders>
            <xsl:for-each select="//Order">
                <xsl:if test="Status = 'Active' and Amount > 1000">
                    <HighValueOrder>
                        <ID><xsl:value-of select="OrderID"/></ID>
                        <Amount><xsl:value-of select="Amount"/></Amount>
                        <Priority>
                            <xsl:choose>
                                <xsl:when test="Amount > 10000">HIGH</xsl:when>
                                <xsl:when test="Amount > 5000">MEDIUM</xsl:when>
                                <xsl:otherwise>STANDARD</xsl:otherwise>
                            </xsl:choose>
                        </Priority>
                    </HighValueOrder>
                </xsl:if>
            </xsl:for-each>
        </Orders>
    </xsl:template>
</xsl:stylesheet>
```

### Pattern 3: Grouping (Muenchian Method / XSLT 2.0 for-each-group)

```xml
<!-- XSLT 2.0: Group orders by customer -->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <CustomerOrders>
            <xsl:for-each-group select="//Order" group-by="CustomerID">
                <Customer id="{current-grouping-key()}">
                    <OrderCount><xsl:value-of select="count(current-group())"/></OrderCount>
                    <TotalAmount><xsl:value-of select="sum(current-group()/Amount)"/></TotalAmount>
                    <xsl:for-each select="current-group()">
                        <Order>
                            <ID><xsl:value-of select="OrderID"/></ID>
                            <Amount><xsl:value-of select="Amount"/></Amount>
                        </Order>
                    </xsl:for-each>
                </Customer>
            </xsl:for-each-group>
        </CustomerOrders>
    </xsl:template>
</xsl:stylesheet>
```

### Pattern 4: Access CPI Headers/Properties in XSLT

```xml
<!-- Access exchange properties passed as XSLT parameters -->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="exchange_property_orderId"/>
    <xsl:param name="exchange_property_timestamp"/>

    <xsl:template match="/">
        <ProcessedOrder>
            <ID><xsl:value-of select="$exchange_property_orderId"/></ID>
            <ProcessedAt><xsl:value-of select="$exchange_property_timestamp"/></ProcessedAt>
            <Data><xsl:copy-of select="//OrderData/*"/></Data>
        </ProcessedOrder>
    </xsl:template>
</xsl:stylesheet>
```

**iFlow Configuration:** Set exchange properties before the XSLT step. They are auto-available as `$exchange_property_{name}` parameters.

### Pattern 5: Remove/Rename Namespaces

```xml
<!-- Remove all namespaces from input XML -->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:attribute name="{local-name()}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="text() | comment() | processing-instruction()">
        <xsl:copy/>
    </xsl:template>
</xsl:stylesheet>
```

---

## Namespace Handling

### Common SAP Namespaces

| Namespace | URI | Used In |
|---|---|---|
| SAP IDoc | `urn:sap-com:document:sap:idoc:messages` | IDoc XML |
| SAP OData | `http://schemas.microsoft.com/ado/2007/08/dataservices` | OData responses |
| SOAP 1.1 | `http://schemas.xmlsoap.org/soap/envelope/` | SOAP messages |
| SOAP 1.2 | `http://www.w3.org/2003/05/soap-envelope` | SOAP 1.2 |

### Declaring Namespaces in XSLT

```xml
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:sap="urn:sap-com:document:sap:idoc:messages"
    xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices"
    exclude-result-prefixes="sap d">

    <xsl:template match="/">
        <Output>
            <xsl:value-of select="//sap:ORDERS05/IDOC/E1EDK01/BELNR"/>
        </Output>
    </xsl:template>
</xsl:stylesheet>
```

---

## Large Payload Mapping

### Streaming Considerations

| Approach | Memory Usage | Speed | Best For |
|---|---|---|---|
| Message Mapping (graphical) | High (DOM) | Moderate | < 10 MB payloads |
| XSLT 2.0 streaming | Medium | Good | 10-100 MB payloads |
| Groovy SAX parser | Low | Fast | > 100 MB payloads |
| Splitter → Map → Aggregator | Low per chunk | Varies | Very large files |

### XSLT Streaming Mode

```xml
<!-- XSLT 3.0 streaming for large files -->
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:mode streamable="yes"/>

    <xsl:template match="/">
        <Output>
            <xsl:for-each select="//Record">
                <xsl:copy-of select="."/>
            </xsl:for-each>
        </Output>
    </xsl:template>
</xsl:stylesheet>
```

### Recommendation for Large Files

```
< 10 MB  → Use graphical Message Mapping (DOM is fine)
10-50 MB → Use XSLT 2.0/3.0 mapping
> 50 MB  → Use Splitter first, then map each chunk
> 100 MB → Use Groovy with SAX/StAX streaming parser
```

---

## Testing Mappings

### Test in Message Mapping Editor

1. Open Message Mapping → Click **Test** tab
2. Provide sample XML input (paste or upload)
3. Click **Execute**
4. Review output XML
5. Check for warnings/errors in the test log

### Test with iFlow Simulation

1. Open iFlow in Edit mode
2. Click **Simulate**
3. Provide input at the start
4. Set end point after mapping step
5. Inspect mapped output

---

## Common Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `"Source field not found"` | Schema mismatch or namespace issue | Verify source schema matches actual payload |
| `"Null pointer in mapping"` | Source field is empty/missing | Add `Exists` check or `IfWithoutElse` guard |
| `"Context mismatch"` | Cardinality conflict between source contexts | Use `RemoveContext` or `Collapse` functions |
| `"XSLT compilation error"` | Syntax error in XSLT | Validate XSLT externally before uploading |
| `"Namespace not declared"` | Missing namespace in XSLT | Add `xmlns:` declaration for all used prefixes |
| `"OutOfMemoryError"` | Payload too large for DOM parser | Switch to XSLT streaming or Groovy SAX |
| `"Value mapping not found"` | Value mapping artifact not deployed | Deploy value mapping to same tenant |

---

## Best Practices

### Message Mapping

1. **Use graphical mapping for standard structure transformations** — easier to maintain than scripts
2. **Document complex mappings** — add comments to custom functions
3. **Test with real data** — synthetic test data may miss edge cases
4. **Handle optional fields** — use `Exists` / `IfWithoutElse` to avoid null errors
5. **Keep UDFs simple** — complex logic belongs in Groovy scripts, not UDFs

### XSLT

1. **Use XSLT 2.0 minimum** — better functions, grouping, date handling vs 1.0
2. **Always declare namespaces** — SAP payloads almost always have namespaces
3. **Use `exclude-result-prefixes`** — keep output XML clean
4. **Test with namespace-aware tools** — many editors strip namespaces silently
5. **Use parameters for dynamic values** — access CPI properties via `$exchange_property_*`

### General

1. **Choose the right mapping type** — don't use scripts when graphical mapping suffices
2. **Consider payload size** — switch to streaming for large files
3. **Version your mappings** — include version in mapping artifact name
4. **Reuse mappings** — create reusable mapping artifacts in shared packages
5. **Profile performance** — use Trace mode to identify slow mapping steps

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for iFlow design with mapping steps
- See [scripting.md](scripting.md) for Groovy XML/JSON processing patterns
- See [simulation-testing.md](simulation-testing.md) for testing mappings
- See [troubleshooting.md](troubleshooting.md) for mapping error resolution

**For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about message mappings, XSLT transformations, value mappings, and Groovy mapping functions.
