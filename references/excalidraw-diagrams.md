# Excalidraw Diagram Patterns for SAP Integration Suite

Complete reference for generating professional SAP integration architecture diagrams using the `excalidraw:create_view` MCP tool. These patterns follow real SAP Cloud Integration visual conventions.

---

## Prerequisites

Before generating any diagram, call `excalidraw:read_me` **once** per conversation to load the Excalidraw format reference. Do NOT call it again after the first time.

---

## SAP Color Palette (Mandatory)

Always use these colors consistently across ALL SAP integration diagrams:

### System/Participant Colors
| Component | Background | Stroke | Usage |
|---|---|---|---|
| Sender (source system) | `#a5d8ff` | `#4a9eed` | SAP ECC, S/4HANA, third-party source |
| Receiver (target system) | `#b2f2bb` | `#22c55e` | Target system, API endpoint |
| Cloud Connector | `#ffd8a8` | `#f59e0b` | On-premise bridge |

### Integration Process Step Colors
| Step Type | Background | Stroke | Shape |
|---|---|---|---|
| Content Modifier | `#fff3bf` | `#f59e0b` | Rectangle rounded |
| Message Mapping / XSLT | `#fff3bf` | `#f59e0b` | Rectangle rounded |
| Groovy / JS Script | `#c3fae8` | `#06b6d4` | Rectangle rounded |
| Request Reply / Send | `#eebefa` | `#ec4899` | Rectangle rounded |
| Router / Multicast | `#ffd8a8` | `#f59e0b` | Diamond |
| Filter | `#ffd8a8` | `#f59e0b` | Diamond |
| Splitter (General/Iterating) | `#ffd8a8` | `#f59e0b` | Diamond |
| Gather | `#d0bfff` | `#8b5cf6` | Diamond |
| Data Store Operations | `#c3fae8` | `#06b6d4` | Rectangle rounded |
| Persist / Write Variables | `#c3fae8` | `#06b6d4` | Rectangle rounded |
| Idempotent Process Call | `#d0bfff` | `#8b5cf6` | Rectangle rounded |
| ProcessDirect call | `#d0bfff` | `#8b5cf6` | Rectangle rounded |

### Zone Colors (Background Areas)
| Zone | Background | Opacity | Usage |
|---|---|---|---|
| SAP BTP Tenant | `#dbe4ff` | 30 | Cloud Integration tenant boundary |
| Integration Process | `#e5dbff` | 30 | Main processing flow area |
| Exception Subprocess | `#ffc9c9` | 25 | Error handling area |
| On-Premise zone | `#d3f9d8` | 30 | On-prem systems behind Cloud Connector |
| Cloud / SaaS zone | `#dbe4ff` | 25 | External cloud services |

### Arrow/Connection Colors
| Connection Type | Stroke Color | Style |
|---|---|---|
| Happy path flow | `#1e1e1e` | solid, strokeWidth: 2 |
| Protocol arrows (HTTP, OData, etc.) | `#8b5cf6` | solid, strokeWidth: 2 |
| Error/exception flow | `#ef4444` | dashed |
| Async / JMS queue flow | `#8b5cf6` | dashed |
| Cloud Connector tunnel | `#f59e0b` | dashed |

---

## Diagram Construction Rules

### Layout
1. **Direction**: Always Left-to-Right (matches SAP CPI Web UI orientation)
2. **Vertical layers**: Sender (left) -> CPI Processing (center) -> Receiver (right)
3. **Exception Subprocess**: Always BELOW the main Integration Process
4. **Sub-flows (ProcessDirect)**: Below the calling flow, connected by labeled arrows

### Drawing Order (Critical for Excalidraw streaming)
1. `cameraUpdate` — position the viewport
2. Title text
3. Zone backgrounds (semi-transparent, back layer)
4. System boxes (Sender, Receiver)
5. Process step boxes (inside zones)
6. Arrows connecting everything (front layer)
7. Exception subprocess zone + steps
8. Final `cameraUpdate` — zoom out to show everything

### Camera Strategy
- **Start**: Camera M or L focused on the title
- **Build**: Camera L or XL as you draw the main flow
- **Detail**: Optionally zoom into specific sections (exception handling, routing logic)
- **Final**: Camera L or XL showing the complete diagram with padding

### Sizing Guidelines
| Element | Min Width | Min Height |
|---|---|---|
| System box (Sender/Receiver) | 140 | 60 |
| Process step | 150 | 55 |
| Router/Splitter diamond | 100 | 80 |
| Zone background | Width of contained elements + 60px padding | Height + 60px |
| Arrows between steps | 40px minimum gap | — |

---

## Pattern 1: Simple Synchronous HTTP-to-OData iFlow

**Use case**: External system calls CPI via HTTPS, CPI transforms and forwards to SAP S/4HANA OData API.

**Diagram structure**:
```
[External System] --HTTPS/POST--> | Integration Process: [Content Modifier] -> [Mapping] -> [Request Reply] | --OData V2--> [S/4HANA]
                                  | Exception Subprocess: [Log Error] -> [Error Response] |
```

**Excalidraw JSON template**:
```json
[
  {"type":"cameraUpdate","width":600,"height":450,"x":100,"y":-20},
  {"type":"text","id":"title","x":250,"y":10,"text":"Sync HTTP to OData iFlow","fontSize":24,"strokeColor":"#1e1e1e"},

  {"type":"cameraUpdate","width":1200,"height":900,"x":-30,"y":-30},

  {"type":"rectangle","id":"btp_zone","x":200,"y":50,"width":620,"height":280,"backgroundColor":"#dbe4ff","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#4a9eed","strokeWidth":1,"opacity":30},
  {"type":"text","id":"btp_label","x":220,"y":58,"text":"SAP BTP - Cloud Integration","fontSize":16,"strokeColor":"#2563eb"},

  {"type":"rectangle","id":"ip_zone","x":230,"y":90,"width":560,"height":110,"backgroundColor":"#e5dbff","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#8b5cf6","strokeWidth":1,"opacity":30},
  {"type":"text","id":"ip_label","x":250,"y":96,"text":"Integration Process","fontSize":16,"strokeColor":"#7c3aed"},

  {"type":"rectangle","id":"sender","x":10,"y":110,"width":150,"height":65,"backgroundColor":"#a5d8ff","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#4a9eed","strokeWidth":2,"label":{"text":"External System","fontSize":16}},

  {"type":"rectangle","id":"cm1","x":260,"y":115,"width":155,"height":55,"backgroundColor":"#fff3bf","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#f59e0b","strokeWidth":2,"label":{"text":"Content Modifier","fontSize":16}},

  {"type":"rectangle","id":"map1","x":450,"y":115,"width":130,"height":55,"backgroundColor":"#fff3bf","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#f59e0b","strokeWidth":2,"label":{"text":"Mapping","fontSize":16}},

  {"type":"rectangle","id":"rr1","x":615,"y":115,"width":150,"height":55,"backgroundColor":"#eebefa","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#ec4899","strokeWidth":2,"label":{"text":"Request Reply","fontSize":16}},

  {"type":"rectangle","id":"receiver","x":860,"y":110,"width":150,"height":65,"backgroundColor":"#b2f2bb","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#22c55e","strokeWidth":2,"label":{"text":"S/4HANA","fontSize":16}},

  {"type":"arrow","id":"a1","x":160,"y":142,"width":100,"height":0,"points":[[0,0],[100,0]],"strokeColor":"#8b5cf6","strokeWidth":2,"endArrowhead":"arrow","label":{"text":"HTTPS/POST","fontSize":14}},
  {"type":"arrow","id":"a2","x":415,"y":142,"width":35,"height":0,"points":[[0,0],[35,0]],"strokeColor":"#1e1e1e","strokeWidth":2,"endArrowhead":"arrow"},
  {"type":"arrow","id":"a3","x":580,"y":142,"width":35,"height":0,"points":[[0,0],[35,0]],"strokeColor":"#1e1e1e","strokeWidth":2,"endArrowhead":"arrow"},
  {"type":"arrow","id":"a4","x":765,"y":142,"width":95,"height":0,"points":[[0,0],[95,0]],"strokeColor":"#8b5cf6","strokeWidth":2,"endArrowhead":"arrow","label":{"text":"OData V2","fontSize":14}},

  {"type":"rectangle","id":"ex_zone","x":230,"y":220,"width":560,"height":90,"backgroundColor":"#ffc9c9","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#ef4444","strokeWidth":1,"opacity":25},
  {"type":"text","id":"ex_label","x":250,"y":226,"text":"Exception Subprocess","fontSize":16,"strokeColor":"#dc2626"},

  {"type":"rectangle","id":"log_err","x":290,"y":250,"width":140,"height":45,"backgroundColor":"#c3fae8","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#06b6d4","strokeWidth":2,"label":{"text":"Log Error","fontSize":16}},
  {"type":"rectangle","id":"err_resp","x":500,"y":250,"width":170,"height":45,"backgroundColor":"#ffc9c9","fillStyle":"solid","roundness":{"type":3},"strokeColor":"#ef4444","strokeWidth":2,"label":{"text":"Error Response","fontSize":16}},
  {"type":"arrow","id":"a_ex","x":430,"y":272,"width":70,"height":0,"points":[[0,0],[70,0]],"strokeColor":"#ef4444","strokeWidth":2,"endArrowhead":"arrow","strokeStyle":"dashed"}
]
```

---

## Pattern 2: Async Fire-and-Forget with JMS Queue

**Use case**: Sender posts a message, CPI persists to JMS for guaranteed delivery, separate consumer iFlow processes it.

**Diagram structure**:
```
[Sender] --HTTPS--> | iFlow 1: [CM] -> [JMS Producer] | --> (JMS Queue) --> | iFlow 2: [JMS Consumer] -> [Mapping] -> [Send] | --> [Receiver]
```

**Key elements**:
- Two Integration Process zones (Producer + Consumer)
- JMS Queue represented as a rounded rectangle with purple background between them
- Dashed arrows for async connections
- Label the JMS queue name on the queue element

---

## Pattern 3: Content-Based Router

**Use case**: Route messages to different receivers based on XPath/condition.

**Diagram structure**:
```
[Sender] --> | [Content Modifier] -> <Router> --Condition A--> [Receiver A] |
                                      |--Condition B--> [Receiver B]
                                      |--Default--> [Receiver C]
```

**Key elements**:
- Router as a **diamond** shape with orange background (`#ffd8a8`)
- Multiple outgoing arrows, each labeled with the routing condition
- Each receiver in a different shade (green, blue, teal) to visually distinguish targets

---

## Pattern 4: Multicast with Gather

**Use case**: Send same message to multiple receivers in parallel, gather responses.

**Key elements**:
- Multicast diamond (orange) fans out to parallel branches
- Gather diamond (purple) collects responses
- Parallel branches drawn as vertical offsets from the main horizontal flow

---

## Pattern 5: SFTP Polling with Idempotent Processing

**Use case**: Poll files from SFTP server, ensure exactly-once processing.

**Diagram structure**:
```
(SFTP Server) --poll--> | [Timer/SFTP Poll] -> [Idempotent Process Call] -> [Mapping] -> [Send] | --> [Receiver]
                        | Exception: [Log] -> [Move to Error Folder] |
```

**Key elements**:
- SFTP Server with a file icon representation (rectangle with "SFTP" label)
- Idempotent Process Call step highlighted with purple background
- Dashed arrow from SFTP showing polling direction

---

## Pattern 6: On-Premise Integration via Cloud Connector

**Use case**: Connect CPI to on-premise SAP ECC via Cloud Connector tunnel.

**Diagram structure**:
```
[Cloud Source] --HTTPS--> | CPI: [Steps...] | --RFC via CC--> (Cloud Connector) --RFC--> [SAP ECC]
                                                                    |
                                                              On-Premise Zone
```

**Key elements**:
- Green zone background for on-premise area (opacity 30)
- Cloud Connector as an orange box bridging the two zones
- Dashed arrow through Cloud Connector indicating the tunnel
- Label the Virtual Host/Port on the arrow

---

## Pattern 7: PO/PI Migration (Before/After)

**Use case**: Show the migration path from SAP PO/PI to SAP Cloud Integration.

**Diagram layout**: Side-by-side comparison

**Left side (Before - "SAP PO/PI")**:
- Gray/muted zone background
- ICO, Operation Mapping, CC boxes
- Label: "BEFORE: SAP PO/PI"

**Right side (After - "SAP CPI")**:
- Blue BTP zone background
- iFlow with modern steps
- Label: "AFTER: SAP Cloud Integration"

**Center**: Migration arrow connecting the two

---

## Pattern 8: Event Mesh Pub-Sub Architecture

**Use case**: Event-driven integration with SAP Event Mesh.

**Key elements**:
- Event Mesh as a central purple zone
- Publishers on the left (multiple sources)
- Topic/Queue in the center
- Subscribers on the right (multiple consumers)
- Use dashed arrows for event subscriptions

---

## Pattern 9: API Management Proxy Chain

**Use case**: API proxy in front of CPI iFlow with policies.

**Diagram structure**:
```
[API Consumer] --HTTPS--> | API Management: [Auth Policy] -> [Rate Limit] -> [Mediation] | --proxy--> | CPI iFlow | --> [Backend]
```

**Key elements**:
- API Management zone (light blue)
- Policy steps as small rectangles in a chain
- CPI zone (light purple) after API Mgmt
- Backend system (green)

---

## Diagram Adaptation Guidelines

When creating diagrams for specific user requests, adapt the patterns above:

1. **Identify the closest pattern** from the list above
2. **Replace system names** with the user's actual system names
3. **Add/remove process steps** based on the specific requirements
4. **Adjust zone labels** to reflect the actual landscape (dev/test/prod)
5. **Add Externalized Parameters** annotations if the user needs environment-specific config
6. **Include volume/SLA annotations** if the user mentions performance requirements

### Common Adaptations

| User Request | Base Pattern | Adaptation |
|---|---|---|
| "Integrar SuccessFactors con ECC" | Pattern 1 (sync) | SF sender, RFC receiver, add Cloud Connector zone |
| "Replicar IDocs a S/4HC" | Pattern 2 (async/JMS) | IDoc sender adapter, OData receiver |
| "Proxy para API de pagos" | Pattern 9 (API Mgmt) | Add security policies, rate limiting |
| "Migrar PI interface a CPI" | Pattern 7 (migration) | Map user's specific PI objects to CPI equivalents |
| "Orquestar 3 APIs y consolidar" | Pattern 4 (multicast) | 3 Request Reply branches + Gather |

---

## Tips for High-Quality Diagrams

1. **Always use `cameraUpdate`** to guide attention — start focused, then reveal the full picture
2. **Label every arrow** with the protocol/adapter type — this is critical for SAP architects
3. **Include exception handling** in every diagram — it shows professional design awareness
4. **Use zone backgrounds** to clearly separate cloud vs on-premise vs external systems
5. **Keep font sizes readable**: 20px+ for titles, 16px for labels, never below 14px
6. **Draw in streaming order**: backgrounds -> shapes -> arrows, so the diagram builds progressively
7. **Match SAP CPI orientation**: Left-to-Right flow, just like the Web UI designer
8. **Add a title** with the iFlow name — always the first element drawn after the initial camera

---

**Next Steps:**
- See [cloud-integration.md](cloud-integration.md) for iFlow patterns to diagram
- See [adapters.md](adapters.md) for adapter types to label on arrows
- See [edge-integration-cell.md](edge-integration-cell.md) for hybrid deployment diagrams
- See [event-mesh.md](event-mesh.md) for event-driven architecture diagrams
- See [migration-assessment.md](migration-assessment.md) for PO/PI migration before/after diagrams
- See [api-management.md](api-management.md) for API proxy chain diagrams
