# Content Transport Reference

Complete guide for transporting SAP Integration Suite content across landscapes (Dev, QA, Prod), including TMS setup, Content Agent configuration, CI/CD automation, and externalized parameter management.

## Table of Contents

- [Overview](#overview)
- [Transport Methods](#transport-methods)
- [SAP Cloud Transport Management (TMS)](#1-sap-cloud-transport-management-tms--recommended)
- [SAP Content Agent](#2-sap-content-agent)
- [Manual Export/Import](#3-manual-exportimport)
- [Integration Content API (CI/CD Automation)](#4-integration-content-api-cicd-automation)
- [CI/CD Pipeline Examples](#cicd-pipeline-examples)
- [Externalized Parameters Management](#externalized-parameters-management)
- [Version Control Strategies](#version-control-strategies)
- [Transport Best Practices](#transport-best-practices)
- [Next Steps](#next-steps)

---

## Overview

Transport management ensures consistency when moving integration artifacts (iFlows, value mappings, script collections, packages) from development through quality assurance to production. Without a disciplined transport strategy, environments drift apart, hotfixes get lost, and production deployments carry unverified changes.

### Why Transport Management Is Critical

- **Environment Consistency:** Guarantees that what is tested in QA is exactly what runs in Prod
- **Audit Trail:** Provides a record of who transported what, when, and with whose approval
- **Rollback Safety:** Enables reverting to known-good artifact versions if a deployment fails
- **Parallel Development:** Allows multiple teams to develop and release independently
- **Compliance:** Meets SOX, GxP, and internal audit requirements for change control

### Transport Landscape Architecture

```
 ┌─────────────────────────────────────────────────────────────────────────┐
 │                     BTP Global Account                                  │
 │                                                                         │
 │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
 │  │   DEV        │    │   QA         │    │   PROD       │              │
 │  │  Subaccount  │    │  Subaccount  │    │  Subaccount  │              │
 │  │              │    │              │    │              │              │
 │  │ Integration  │    │ Integration  │    │ Integration  │              │
 │  │   Suite      │    │   Suite      │    │   Suite      │              │
 │  │  (Design &   │    │  (Test &     │    │  (Runtime    │              │
 │  │   Develop)   │    │   Verify)    │    │   Only)      │              │
 │  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘              │
 │         │                   │                   │                      │
 │         ▼                   ▼                   ▼                      │
 │  ┌──────────────────────────────────────────────────────┐              │
 │  │         SAP Cloud Transport Management (TMS)          │              │
 │  │                                                        │              │
 │  │   [DEV Node] ──Export──► [QA Node] ──Approve──► [PROD Node]         │
 │  │       │                     │                     │    │              │
 │  │   Transport            Approval Gate          Import   │              │
 │  │   Request              (QA Lead)              & Deploy │              │
 │  └──────────────────────────────────────────────────────┘              │
 │                                                                         │
 │  ┌──────────────────────┐                                              │
 │  │  SAP Content Agent   │  ← Middleware: packages artifacts            │
 │  │  (Service Instance)  │     and attaches to TMS requests             │
 │  └──────────────────────┘                                              │
 └─────────────────────────────────────────────────────────────────────────┘
```

---

## Transport Methods

### Method Comparison

| Criteria | TMS (Recommended) | Content Agent + TMS | Manual Export/Import | Integration Content API |
|---|---|---|---|---|
| **Automation Level** | High | High | None | Full (scriptable) |
| **Approval Workflow** | Built-in gates | Built-in via TMS | Manual/email | Custom (pipeline gates) |
| **Audit Trail** | Complete | Complete | Manual logging | Custom (pipeline logs) |
| **Bundled Transport** | Yes (MTAR) | Yes (packages) | Individual artifacts | Individual artifacts |
| **Rollback Support** | Version history | Version history | Manual re-import | API re-deploy |
| **Setup Complexity** | Medium | Medium-High | None | High (CI/CD infra) |
| **Best For** | Standard 3-tier landscapes | Complex multi-package | Ad-hoc, emergencies | DevOps-driven teams |

---

### 1. SAP Cloud Transport Management (TMS) -- Recommended

**Recommended for:** Cloud Foundry multi-tenant deployments, enterprises requiring formal approval workflows and audit compliance.

#### Prerequisites

- BTP Global Account with entitlements for Transport Management Service
- Separate subaccounts for each landscape tier (Dev, QA, Prod)
- Integration Suite provisioned in each subaccount
- Administrator access to BTP Cockpit

#### Step 1: Subscribe to Transport Management Service

**Navigation:** BTP Cockpit > Global Account > Subaccount (e.g., DEV) > Service Marketplace

1. Search for **Cloud Transport Management**
2. Click **Create** to create a new subscription
3. Select Plan: **standard** (for productive use) or **lite** (for evaluation)
4. Assign the following role collections to transport administrators:

| Role Collection | Purpose | Assign To |
|---|---|---|
| `TMS_LandscapeOperator` | Create/manage transport nodes and routes | Platform Admin |
| `TMS_Admin` | Full TMS administration | Transport Manager |
| `TMS_Viewer` | View transport requests, read-only | Developers, Auditors |
| `TMS_Import_Operator` | Import transports into target nodes | QA Lead, Release Mgr |
| `TMS_Export_Operator` | Create and export transport requests | Developers |

#### Step 2: Create Transport Nodes

**Navigation:** TMS Application > Transport Nodes > Create

Create one node per environment:

| Node Name | Description | Allow Upload | Forward Mode | Content Type |
|---|---|---|---|---|
| `DEV_CPI` | Development Integration Suite | Yes | Manual | Multi-Target Application |
| `QA_CPI` | Quality Assurance Integration Suite | No | Manual | Multi-Target Application |
| `PROD_CPI` | Production Integration Suite | No | Manual | Multi-Target Application |

For each target node (QA, PROD), configure the **Destination** to point to the Integration Suite tenant:

```
Destination Name:       TMS_QA_CPI
URL:                    https://<qa-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com
Authentication:         OAuth2ClientCredentials
Client ID:              <service-key-client-id>
Client Secret:          <service-key-client-secret>
Token Service URL:      https://<qa-tenant>.authentication.<region>.hana.ondemand.com/oauth/token
```

#### Step 3: Define Transport Routes

**Navigation:** TMS Application > Transport Routes > Create

Define the sequence of environments:

```
DEV_CPI ────► QA_CPI ────► PROD_CPI
   │              │              │
   │         Approval Gate  Approval Gate
   │         (QA Lead)      (Release Mgr)
   │
   └── Developers export from here
```

Route configuration:

| Source Node | Target Node | Route Type |
|---|---|---|
| `DEV_CPI` | `QA_CPI` | Delivery |
| `QA_CPI` | `PROD_CPI` | Delivery |

#### Step 4: Transport Action Sequence

The end-to-end transport follows these steps:

```
Developer                QA Lead                  Release Manager
    │                        │                          │
    ├─ 1. Export iFlow       │                          │
    │     as MTAR            │                          │
    ├─ 2. Upload MTAR        │                          │
    │     to DEV_CPI node    │                          │
    ├─ 3. Create Transport   │                          │
    │     Request (TR)       │                          │
    ├─ 4. Add MTAR to TR     │                          │
    ├─ 5. Release TR         │                          │
    │     ──────────────────►│                          │
    │                        ├─ 6. Review TR in QA_CPI  │
    │                        ├─ 7. Import to QA         │
    │                        ├─ 8. Verify in QA env     │
    │                        ├─ 9. Approve for PROD     │
    │                        │     ──────────────────►   │
    │                        │                          ├─ 10. Review TR in PROD_CPI
    │                        │                          ├─ 11. Import to PROD
    │                        │                          ├─ 12. Verify deployment
    │                        │                          └─ 13. Close TR
```

#### Pattern: 3-Tier Landscape Setup from Scratch

1. Create three BTP subaccounts: `integration-dev`, `integration-qa`, `integration-prod`
2. Provision Integration Suite in each subaccount
3. Subscribe to TMS in one subaccount (typically DEV or a shared services subaccount)
4. Create service keys in QA and PROD for destination authentication
5. Create three transport nodes with destinations pointing to each tenant
6. Define two transport routes: DEV to QA, QA to PROD
7. Assign role collections to your team:
   - Developers: `TMS_Export_Operator` on DEV node
   - QA Lead: `TMS_Import_Operator` on QA node
   - Release Manager: `TMS_Import_Operator` on PROD node
   - All: `TMS_Viewer` for visibility
8. Perform a test transport with a simple iFlow to validate the entire chain

---

### 2. SAP Content Agent

**What It Is:** A middleware service that sits between SAP Integration Suite and Cloud Transport Management. It discovers, selects, and packages integration content into MTAR archives that TMS can transport.

#### Prerequisites

- TMS already configured with nodes and routes (see Section 1)
- Content Agent service entitlement in your BTP subaccount
- Destinations configured for both Integration Suite and TMS endpoints

#### Step 1: Create Content Agent Service Instance

**Navigation:** BTP Cockpit > Subaccount (DEV) > Service Marketplace > Content Agent

1. Click **Create** and select plan: **standard**
2. Create a service key for API access:
   - **Navigation:** Service Instance > Create Service Key
   - Note the `clientid`, `clientsecret`, and `url` from the generated key

#### Step 2: Configure Destinations

Create the following destinations in the DEV subaccount:

**Destination 1: Integration Suite (source)**

| Property | Value |
|---|---|
| Name | `CloudIntegration` |
| Type | HTTP |
| URL | `https://<dev-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1` |
| Proxy Type | Internet |
| Authentication | OAuth2ClientCredentials |
| Client ID | `<integration-suite-service-key-clientid>` |
| Client Secret | `<integration-suite-service-key-clientsecret>` |
| Token Service URL | `https://<dev-tenant>.authentication.<region>.hana.ondemand.com/oauth/token` |

**Destination 2: Transport Management Service (target)**

| Property | Value |
|---|---|
| Name | `TransportManagementService` |
| Type | HTTP |
| URL | `https://transport-service-app-backend.ts.cfapps.<region>.hana.ondemand.com` |
| Proxy Type | Internet |
| Authentication | OAuth2ClientCredentials |
| Client ID | `<tms-service-key-clientid>` |
| Client Secret | `<tms-service-key-clientsecret>` |
| Token Service URL | `https://<auth-subdomain>.authentication.<region>.hana.ondemand.com/oauth/token` |

#### Step 3: Content Agent Workflow

```
┌──────────────────────────────────────────────────────────────┐
│                   Content Agent Workflow                       │
│                                                                │
│  1. Open Content Agent  ──► 2. Select Content                 │
│     application               (iFlows, value mappings,        │
│                                script collections, packages)  │
│                                        │                      │
│                                        ▼                      │
│  4. Attach to TMS       ◄── 3. Create Transport Request       │
│     transport route              (MTAR packaging)             │
│         │                                                      │
│         ▼                                                      │
│  5. Appears in TMS queue for import at target node            │
└──────────────────────────────────────────────────────────────┘
```

#### Content Selection: Supported Artifact Types

| Artifact Type | Selectable | Notes |
|---|---|---|
| Integration Flows (iFlows) | Yes | Core transport artifact |
| Value Mappings | Yes | Include with dependent iFlows |
| Script Collections | Yes | Shared Groovy/JS libraries |
| Integration Packages | Yes | Transports all artifacts within the package |
| Number Ranges | Yes | Sequence generators |
| Custom Tags | Yes | Organizational metadata |
| Security Material (Keystore) | No | Must be configured separately per tenant |
| User Credentials | No | Must be created manually in each tenant |

#### Pattern: Transport 10 iFlows and 2 Value Mappings as a Bundle

1. Open Content Agent application in BTP Cockpit
2. Click **Select Integration Content**
3. Choose source: **CloudIntegration** destination
4. Browse or search for artifacts:
   - Select all 10 iFlows by checking their boxes
   - Switch to Value Mappings tab, select the 2 value mappings
5. Click **Create Transport Request**
   - Description: `SPRINT-42: Order processing interface bundle`
   - Transport Node: `DEV_CPI`
6. Content Agent packages all 12 artifacts into a single MTAR
7. MTAR is uploaded to TMS and appears in the DEV_CPI transport queue
8. Follow the standard TMS approval flow (DEV > QA > PROD)

---

### 3. Manual Export/Import

**Use for:** Ad-hoc transports, simple scenarios with few artifacts, emergency production fixes, environments without TMS configured.

#### When to Use Manual Transport

- Emergency hotfix that cannot wait for TMS approval cycle
- Initial setup before TMS infrastructure is in place
- One-time migration from trial to productive tenant
- Transferring artifacts between unrelated landscapes (e.g., different global accounts)

#### Export Steps

**Navigation:** Integration Suite > Design > Integration Content

1. Navigate to the integration package containing your artifact
2. Select the artifact (iFlow, value mapping, etc.)
3. Click **Actions** (three-dot menu) > **Export**
4. The artifact downloads as a `.zip` file
5. For entire packages: Select the package > **Actions** > **Export**
   - This exports all artifacts in the package as a single `.zip`

#### Import Steps

**Navigation:** Integration Suite (target tenant) > Design > Integration Content

1. Navigate to the target integration package (create one if it does not exist)
2. Click **Actions** > **Import**
3. Browse and select the `.zip` file exported from the source tenant
4. Review the import preview (artifact names, versions)
5. Click **Import** to confirm
6. **Critical:** Configure externalized parameters for the target environment
   - Navigation: Monitor > Integrations and APIs > Manage Integration Content
   - Select the imported artifact > **Configure**
   - Update all environment-specific values (URLs, paths, queue names)
7. **Deploy** the artifact

#### Limitations vs TMS

| Capability | Manual Export/Import | TMS |
|---|---|---|
| Approval workflow | None (manual coordination) | Built-in approval gates |
| Audit trail | None (unless manually logged) | Automatic, complete |
| Bundle multiple artifacts | One package at a time | MTAR with mixed artifacts |
| Rollback | Re-import previous version | Version history in TMS |
| Automation | Not possible | API-driven, CI/CD ready |
| Environment drift detection | None | Transport queue shows pending |

---

### 4. Integration Content API (CI/CD Automation)

**Use for:** DevOps pipelines, automated deployments, teams practicing continuous integration.

#### API Endpoints

The Integration Content OData API operates on design-time and runtime artifacts.

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/IntegrationPackages` | GET | List all integration packages |
| `/api/v1/IntegrationPackages('{PackageId}')` | GET | Get specific package details |
| `/api/v1/IntegrationDesigntimeArtifacts` | GET | List all design-time artifacts |
| `/api/v1/IntegrationDesigntimeArtifacts(Id='{Id}',Version='{Version}')` | GET | Get specific artifact metadata |
| `/api/v1/IntegrationDesigntimeArtifacts(Id='{Id}',Version='{Version}')/$value` | GET | Download artifact binary (.zip) |
| `/api/v1/IntegrationDesigntimeArtifacts` | POST | Upload new artifact |
| `/api/v1/IntegrationDesigntimeArtifacts(Id='{Id}',Version='{Version}')` | PUT | Update existing artifact |
| `/api/v1/IntegrationDesigntimeArtifacts(Id='{Id}',Version='{Version}')` | DELETE | Delete artifact |
| `/api/v1/DeployIntegrationDesigntimeArtifact?Id='{Id}'&Version='{Version}'` | POST | Deploy artifact |
| `/api/v1/IntegrationRuntimeArtifacts('{Id}')` | GET | Get runtime artifact status |

#### Authentication: OAuth 2.0 Client Credentials

Before making API calls, obtain an OAuth 2.0 token using the service key credentials from your Integration Suite service instance.

```bash
# Step 1: Get OAuth token
TOKEN=$(curl -s -X POST \
  "https://<tenant>.authentication.<region>.hana.ondemand.com/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=<client-id>" \
  -d "client_secret=<client-secret>" \
  | jq -r '.access_token')

echo "Token acquired: ${TOKEN:0:20}..."
```

#### Example API Calls

**List all design-time artifacts:**

```bash
curl -s -X GET \
  "https://<tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1/IntegrationDesigntimeArtifacts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq '.d.results[] | {Id: .Id, Name: .Name, Version: .Version, PackageId: .PackageId}'
```

**Download a specific iFlow:**

```bash
curl -s -X GET \
  "https://<dev-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1/IntegrationDesigntimeArtifacts(Id='Order_Processing_S4_to_SF',Version='1.2.0')/\$value" \
  -H "Authorization: Bearer $DEV_TOKEN" \
  -o "Order_Processing_S4_to_SF.zip"

echo "Downloaded: Order_Processing_S4_to_SF.zip ($(wc -c < Order_Processing_S4_to_SF.zip) bytes)"
```

**Upload iFlow to target tenant:**

```bash
# Fetch CSRF token first (required for write operations)
CSRF_TOKEN=$(curl -s -X GET \
  "https://<qa-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1/" \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "X-CSRF-Token: Fetch" \
  -D - 2>/dev/null | grep -i 'x-csrf-token' | awk '{print $2}' | tr -d '\r')

# Upload artifact
curl -s -X POST \
  "https://<qa-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1/IntegrationDesigntimeArtifacts" \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "X-CSRF-Token: $CSRF_TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@Order_Processing_S4_to_SF.zip" \
  -F "Id=Order_Processing_S4_to_SF" \
  -F "Name=Order Processing S4 to Salesforce" \
  -F "PackageId=OrderManagement" \
  -F "Version=1.2.0"
```

**Deploy artifact:**

```bash
curl -s -X POST \
  "https://<qa-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1/DeployIntegrationDesigntimeArtifact?Id='Order_Processing_S4_to_SF'&Version='1.2.0'" \
  -H "Authorization: Bearer $QA_TOKEN" \
  -H "X-CSRF-Token: $CSRF_TOKEN" \
  -H "Content-Type: application/json"
```

**Check deployment status (poll until STARTED):**

```bash
# Poll deployment status every 10 seconds, up to 5 minutes
for i in $(seq 1 30); do
  STATUS=$(curl -s -X GET \
    "https://<qa-tenant>.it-cpi018.cfapps.<region>.hana.ondemand.com/api/v1/IntegrationRuntimeArtifacts('Order_Processing_S4_to_SF')" \
    -H "Authorization: Bearer $QA_TOKEN" \
    -H "Accept: application/json" \
    | jq -r '.d.Status')

  echo "Attempt $i: Status = $STATUS"

  if [ "$STATUS" = "STARTED" ]; then
    echo "Deployment successful."
    break
  elif [ "$STATUS" = "ERROR" ]; then
    echo "Deployment FAILED. Check Monitor > Integrations for details."
    exit 1
  fi

  sleep 10
done
```

---

## CI/CD Pipeline Examples

### Common CI/CD Pattern

All pipelines follow the same logical sequence:

```
┌────────────┐    ┌────────────┐    ┌────────────┐    ┌────────┐    ┌────────┐
│ Get OAuth  │───►│ Download   │───►│ Upload to  │───►│ Deploy │───►│ Verify │
│ Token      │    │ from DEV   │    │ Target     │    │        │    │ Status │
└────────────┘    └────────────┘    └────────────┘    └────────┘    └────────┘
                                                           │
                                                    ┌──────┴──────┐
                                                    │  On Failure  │
                                                    │  Rollback    │
                                                    │  + Notify    │
                                                    └─────────────┘
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any

    environment {
        DEV_TENANT    = 'https://dev-tenant.it-cpi018.cfapps.us10.hana.ondemand.com'
        QA_TENANT     = 'https://qa-tenant.it-cpi018.cfapps.us10.hana.ondemand.com'
        AUTH_URL      = 'https://dev-tenant.authentication.us10.hana.ondemand.com/oauth/token'
        QA_AUTH_URL   = 'https://qa-tenant.authentication.us10.hana.ondemand.com/oauth/token'
        IFLOW_ID      = 'Order_Processing_S4_to_SF'
        IFLOW_VERSION = '1.2.0'
        PACKAGE_ID    = 'OrderManagement'
    }

    stages {
        stage('Get DEV Token') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'cpi-dev-oauth',
                    usernameVariable: 'CLIENT_ID',
                    passwordVariable: 'CLIENT_SECRET'
                )]) {
                    script {
                        def response = sh(
                            script: """
                                curl -s -X POST "${AUTH_URL}" \
                                  -d "grant_type=client_credentials" \
                                  -d "client_id=${CLIENT_ID}" \
                                  -d "client_secret=${CLIENT_SECRET}"
                            """,
                            returnStdout: true
                        )
                        env.DEV_TOKEN = readJSON(text: response).access_token
                    }
                }
            }
        }

        stage('Download from DEV') {
            steps {
                sh """
                    curl -s -X GET \
                      "${DEV_TENANT}/api/v1/IntegrationDesigntimeArtifacts(Id='${IFLOW_ID}',Version='${IFLOW_VERSION}')/\\\$value" \
                      -H "Authorization: Bearer ${DEV_TOKEN}" \
                      -o "${IFLOW_ID}.zip"
                """
                archiveArtifacts artifacts: "${IFLOW_ID}.zip"
            }
        }

        stage('Get QA Token') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'cpi-qa-oauth',
                    usernameVariable: 'CLIENT_ID',
                    passwordVariable: 'CLIENT_SECRET'
                )]) {
                    script {
                        def response = sh(
                            script: """
                                curl -s -X POST "${QA_AUTH_URL}" \
                                  -d "grant_type=client_credentials" \
                                  -d "client_id=${CLIENT_ID}" \
                                  -d "client_secret=${CLIENT_SECRET}"
                            """,
                            returnStdout: true
                        )
                        env.QA_TOKEN = readJSON(text: response).access_token
                    }
                }
            }
        }

        stage('Upload to QA') {
            steps {
                script {
                    // Fetch CSRF token
                    def csrfResponse = sh(
                        script: """
                            curl -s -D - "${QA_TENANT}/api/v1/" \
                              -H "Authorization: Bearer ${QA_TOKEN}" \
                              -H "X-CSRF-Token: Fetch" 2>/dev/null \
                              | grep -i 'x-csrf-token' | awk '{print \$2}' | tr -d '\\r'
                        """,
                        returnStdout: true
                    ).trim()
                    env.CSRF_TOKEN = csrfResponse

                    sh """
                        curl -s -X POST \
                          "${QA_TENANT}/api/v1/IntegrationDesigntimeArtifacts" \
                          -H "Authorization: Bearer ${QA_TOKEN}" \
                          -H "X-CSRF-Token: ${CSRF_TOKEN}" \
                          -F "file=@${IFLOW_ID}.zip" \
                          -F "Id=${IFLOW_ID}" \
                          -F "Name=Order Processing S4 to Salesforce" \
                          -F "PackageId=${PACKAGE_ID}" \
                          -F "Version=${IFLOW_VERSION}"
                    """
                }
            }
        }

        stage('Deploy to QA') {
            steps {
                sh """
                    curl -s -X POST \
                      "${QA_TENANT}/api/v1/DeployIntegrationDesigntimeArtifact?Id='${IFLOW_ID}'&Version='${IFLOW_VERSION}'" \
                      -H "Authorization: Bearer ${QA_TOKEN}" \
                      -H "X-CSRF-Token: ${CSRF_TOKEN}" \
                      -H "Content-Type: application/json"
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def deployed = false
                        while (!deployed) {
                            def status = sh(
                                script: """
                                    curl -s "${QA_TENANT}/api/v1/IntegrationRuntimeArtifacts('${IFLOW_ID}')" \
                                      -H "Authorization: Bearer ${QA_TOKEN}" \
                                      -H "Accept: application/json" \
                                      | jq -r '.d.Status'
                                """,
                                returnStdout: true
                            ).trim()
                            if (status == 'STARTED') {
                                deployed = true
                                echo "iFlow deployed and running in QA."
                            } else if (status == 'ERROR') {
                                error("Deployment failed with ERROR status.")
                            } else {
                                echo "Status: ${status}. Waiting 15s..."
                                sleep 15
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed. Review logs and consider rollback."
            // Add notification: email, Slack, Teams, etc.
        }
        success {
            echo "Transport to QA completed successfully."
        }
    }
}
```

### GitHub Actions Workflow

```yaml
name: Deploy iFlows to QA

on:
  push:
    branches: [main]
    paths:
      - 'integration-content/**'

env:
  DEV_TENANT: https://dev-tenant.it-cpi018.cfapps.us10.hana.ondemand.com
  QA_TENANT: https://qa-tenant.it-cpi018.cfapps.us10.hana.ondemand.com
  IFLOW_ID: Order_Processing_S4_to_SF
  IFLOW_VERSION: '1.2.0'
  PACKAGE_ID: OrderManagement

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Authenticate to DEV tenant
        id: dev-auth
        run: |
          TOKEN=$(curl -sf -X POST \
            "${{ secrets.DEV_AUTH_URL }}" \
            -d "grant_type=client_credentials" \
            -d "client_id=${{ secrets.DEV_CLIENT_ID }}" \
            -d "client_secret=${{ secrets.DEV_CLIENT_SECRET }}" \
            | jq -r '.access_token')
          echo "token=$TOKEN" >> "$GITHUB_OUTPUT"

      - name: Download iFlow from DEV
        run: |
          curl -sf -X GET \
            "${{ env.DEV_TENANT }}/api/v1/IntegrationDesigntimeArtifacts(Id='${{ env.IFLOW_ID }}',Version='${{ env.IFLOW_VERSION }}')/\$value" \
            -H "Authorization: Bearer ${{ steps.dev-auth.outputs.token }}" \
            -o "${{ env.IFLOW_ID }}.zip"
          echo "Downloaded $(wc -c < ${{ env.IFLOW_ID }}.zip) bytes"

      - name: Authenticate to QA tenant
        id: qa-auth
        run: |
          TOKEN=$(curl -sf -X POST \
            "${{ secrets.QA_AUTH_URL }}" \
            -d "grant_type=client_credentials" \
            -d "client_id=${{ secrets.QA_CLIENT_ID }}" \
            -d "client_secret=${{ secrets.QA_CLIENT_SECRET }}" \
            | jq -r '.access_token')
          echo "token=$TOKEN" >> "$GITHUB_OUTPUT"

      - name: Fetch CSRF Token
        id: csrf
        run: |
          CSRF=$(curl -sf -D - "${{ env.QA_TENANT }}/api/v1/" \
            -H "Authorization: Bearer ${{ steps.qa-auth.outputs.token }}" \
            -H "X-CSRF-Token: Fetch" 2>/dev/null \
            | grep -i 'x-csrf-token' | awk '{print $2}' | tr -d '\r')
          echo "token=$CSRF" >> "$GITHUB_OUTPUT"

      - name: Upload iFlow to QA
        run: |
          curl -sf -X POST \
            "${{ env.QA_TENANT }}/api/v1/IntegrationDesigntimeArtifacts" \
            -H "Authorization: Bearer ${{ steps.qa-auth.outputs.token }}" \
            -H "X-CSRF-Token: ${{ steps.csrf.outputs.token }}" \
            -F "file=@${{ env.IFLOW_ID }}.zip" \
            -F "Id=${{ env.IFLOW_ID }}" \
            -F "Name=Order Processing S4 to Salesforce" \
            -F "PackageId=${{ env.PACKAGE_ID }}" \
            -F "Version=${{ env.IFLOW_VERSION }}"

      - name: Deploy iFlow in QA
        run: |
          curl -sf -X POST \
            "${{ env.QA_TENANT }}/api/v1/DeployIntegrationDesigntimeArtifact?Id='${{ env.IFLOW_ID }}'&Version='${{ env.IFLOW_VERSION }}'" \
            -H "Authorization: Bearer ${{ steps.qa-auth.outputs.token }}" \
            -H "X-CSRF-Token: ${{ steps.csrf.outputs.token }}" \
            -H "Content-Type: application/json"

      - name: Verify deployment status
        run: |
          for i in $(seq 1 20); do
            STATUS=$(curl -sf "${{ env.QA_TENANT }}/api/v1/IntegrationRuntimeArtifacts('${{ env.IFLOW_ID }}')" \
              -H "Authorization: Bearer ${{ steps.qa-auth.outputs.token }}" \
              -H "Accept: application/json" \
              | jq -r '.d.Status')
            echo "Attempt $i: Status=$STATUS"
            if [ "$STATUS" = "STARTED" ]; then
              echo "Deployment verified successfully."
              exit 0
            elif [ "$STATUS" = "ERROR" ]; then
              echo "::error::Deployment failed with ERROR status"
              exit 1
            fi
            sleep 15
          done
          echo "::error::Deployment verification timed out after 5 minutes"
          exit 1
```

### Azure DevOps Pipeline

```yaml
trigger:
  branches:
    include:
      - main
  paths:
    include:
      - integration-content/*

pool:
  vmImage: 'ubuntu-latest'

variables:
  devTenant: 'https://dev-tenant.it-cpi018.cfapps.us10.hana.ondemand.com'
  qaTenant: 'https://qa-tenant.it-cpi018.cfapps.us10.hana.ondemand.com'
  iFlowId: 'Order_Processing_S4_to_SF'
  iFlowVersion: '1.2.0'
  packageId: 'OrderManagement'

steps:
  - task: Bash@3
    displayName: 'Authenticate to DEV'
    inputs:
      targetType: inline
      script: |
        DEV_TOKEN=$(curl -sf -X POST "$(DEV_AUTH_URL)" \
          -d "grant_type=client_credentials" \
          -d "client_id=$(DEV_CLIENT_ID)" \
          -d "client_secret=$(DEV_CLIENT_SECRET)" \
          | jq -r '.access_token')
        echo "##vso[task.setvariable variable=devToken;issecret=true]$DEV_TOKEN"

  - task: Bash@3
    displayName: 'Download iFlow from DEV'
    inputs:
      targetType: inline
      script: |
        curl -sf -X GET \
          "$(devTenant)/api/v1/IntegrationDesigntimeArtifacts(Id='$(iFlowId)',Version='$(iFlowVersion)')/\$value" \
          -H "Authorization: Bearer $(devToken)" \
          -o "$(iFlowId).zip"

  - task: Bash@3
    displayName: 'Authenticate to QA'
    inputs:
      targetType: inline
      script: |
        QA_TOKEN=$(curl -sf -X POST "$(QA_AUTH_URL)" \
          -d "grant_type=client_credentials" \
          -d "client_id=$(QA_CLIENT_ID)" \
          -d "client_secret=$(QA_CLIENT_SECRET)" \
          | jq -r '.access_token')
        echo "##vso[task.setvariable variable=qaToken;issecret=true]$QA_TOKEN"

  - task: Bash@3
    displayName: 'Upload and Deploy to QA'
    inputs:
      targetType: inline
      script: |
        # Fetch CSRF
        CSRF=$(curl -sf -D - "$(qaTenant)/api/v1/" \
          -H "Authorization: Bearer $(qaToken)" \
          -H "X-CSRF-Token: Fetch" 2>/dev/null \
          | grep -i 'x-csrf-token' | awk '{print $2}' | tr -d '\r')

        # Upload
        curl -sf -X POST "$(qaTenant)/api/v1/IntegrationDesigntimeArtifacts" \
          -H "Authorization: Bearer $(qaToken)" \
          -H "X-CSRF-Token: $CSRF" \
          -F "file=@$(iFlowId).zip" \
          -F "Id=$(iFlowId)" \
          -F "Name=Order Processing S4 to Salesforce" \
          -F "PackageId=$(packageId)" \
          -F "Version=$(iFlowVersion)"

        # Deploy
        curl -sf -X POST \
          "$(qaTenant)/api/v1/DeployIntegrationDesigntimeArtifact?Id='$(iFlowId)'&Version='$(iFlowVersion)'" \
          -H "Authorization: Bearer $(qaToken)" \
          -H "X-CSRF-Token: $CSRF" \
          -H "Content-Type: application/json"

  - task: Bash@3
    displayName: 'Verify Deployment'
    inputs:
      targetType: inline
      script: |
        for i in $(seq 1 20); do
          STATUS=$(curl -sf \
            "$(qaTenant)/api/v1/IntegrationRuntimeArtifacts('$(iFlowId)')" \
            -H "Authorization: Bearer $(qaToken)" \
            -H "Accept: application/json" \
            | jq -r '.d.Status')
          echo "Attempt $i: Status=$STATUS"
          if [ "$STATUS" = "STARTED" ]; then
            echo "Deployment verified."
            exit 0
          elif [ "$STATUS" = "ERROR" ]; then
            echo "##vso[task.logissue type=error]Deployment failed."
            exit 1
          fi
          sleep 15
        done
        echo "##vso[task.logissue type=error]Verification timed out."
        exit 1
```

---

## Externalized Parameters Management

Externalized parameters are the bridge between portable artifacts and environment-specific configuration. An iFlow that hardcodes `https://prod-api.example.com` cannot be tested in DEV. Externalizing that URL as `${parameter.url_target_api}` lets the same artifact run in any environment with different configured values.

### What Should Be Externalized

| Category | Examples | Naming Convention |
|---|---|---|
| Endpoint URLs | API base URLs, SOAP endpoints, SFTP hosts | `url_target_system`, `url_callback_endpoint` |
| Credential References | User Credential artifact names | `cred_target_system`, `cred_sftp_server` |
| File Paths | SFTP directories, file prefixes | `path_inbound_dir`, `path_archive_dir` |
| Queue Names | JMS queue names | `queue_order_processing`, `queue_error_dlq` |
| System IDs | SAP client numbers, logical system names | `sys_sap_client`, `sys_logical_name` |
| Timeouts | Connection and processing timeouts | `timeout_http_connect`, `timeout_processing` |
| Feature Flags | Enable/disable optional processing steps | `flag_enable_enrichment`, `flag_debug_logging` |

### Parameter Naming Conventions

Use a consistent prefix scheme to make parameters self-documenting:

```
{category}_{target/context}_{detail}

Examples:
  url_s4hana_odata         → S/4HANA OData endpoint URL
  cred_sf_api              → Salesforce API credential artifact name
  path_sftp_inbound        → SFTP inbound directory path
  queue_order_main         → Main order processing JMS queue
  timeout_http_90          → HTTP connection timeout (seconds)
  flag_enable_retry        → Whether to enable retry logic
```

### Environment-Specific Configuration Template

| Parameter Name | DEV Value | QA Value | PROD Value |
|---|---|---|---|
| `url_s4hana_odata` | `https://dev-s4.example.com/sap/opu/odata/sap/API_BUSINESS_PARTNER` | `https://qa-s4.example.com/sap/opu/odata/sap/API_BUSINESS_PARTNER` | `https://prod-s4.example.com/sap/opu/odata/sap/API_BUSINESS_PARTNER` |
| `url_sf_api` | `https://test.salesforce.com/services/data/v58.0` | `https://test.salesforce.com/services/data/v58.0` | `https://login.salesforce.com/services/data/v58.0` |
| `cred_s4hana` | `S4HANA_DEV_User` | `S4HANA_QA_User` | `S4HANA_PROD_User` |
| `cred_sf_oauth` | `SF_DEV_OAuth` | `SF_QA_OAuth` | `SF_PROD_OAuth` |
| `path_sftp_inbound` | `/dev/inbound/orders` | `/qa/inbound/orders` | `/prod/inbound/orders` |
| `path_sftp_archive` | `/dev/archive/orders` | `/qa/archive/orders` | `/prod/archive/orders` |
| `queue_order_main` | `dev.order.processing` | `qa.order.processing` | `prod.order.processing` |
| `queue_error_dlq` | `dev.order.error` | `qa.order.error` | `prod.order.error` |
| `timeout_http_connect` | `30000` | `30000` | `60000` |
| `flag_enable_retry` | `true` | `true` | `true` |

### How to Configure After Transport

**Navigation:** Monitor > Integrations and APIs > Manage Integration Content

1. Locate the newly transported artifact (filter by status: **Deployed** or **Design**)
2. Click the artifact name to open details
3. Click **Configure** (pencil icon or Configure button)
4. The externalized parameters appear in a form:
   - Update each parameter to the correct value for this environment
   - Reference the environment-specific configuration table above
5. Click **Save**
6. Click **Deploy** to activate the configuration

### Secure Parameter Handling

- **Never transport secrets:** Credential artifacts, OAuth client secrets, and private keys must be created separately in each tenant. They are not included in transport packages.
- **Reference by name:** In iFlows, reference credentials by their artifact name (e.g., `S4HANA_PROD_User`). Create an artifact with that exact name in each target tenant.
- **Keystore entries:** Certificates and key pairs must be deployed independently to each tenant's keystore via Monitor > Security Material > Keystore.

---

## Version Control Strategies

### iFlow Versioning: Semantic Versioning

Adopt semantic versioning (SemVer) for all integration artifacts:

```
MAJOR.MINOR.PATCH

  MAJOR  → Breaking changes (new message structure, removed fields, changed API contract)
  MINOR  → New features, backward-compatible (added optional field, new route branch)
  PATCH  → Bug fixes, no functional change (fixed mapping, corrected XPath)
```

**Examples:**

| Change Description | Version Bump | Before | After |
|---|---|---|---|
| Fix null pointer in Groovy script | Patch | 1.3.2 | 1.3.3 |
| Add new routing branch for APAC orders | Minor | 1.3.3 | 1.4.0 |
| Restructure payload from flat to nested XML | Major | 1.4.0 | 2.0.0 |
| Update endpoint URL (externalized, no code change) | No bump | 2.0.0 | 2.0.0 |

### Naming Conventions with Version

```
{Source}_{Direction}_{Target}_{Purpose}_v{Major}.{Minor}

Examples:
  S4_to_SF_OrderSync_v1.0
  SF_to_S4_ContactUpdate_v2.1
  File_to_S4_MaterialUpload_v1.3
  S4_to_Ariba_PurchaseOrder_v3.0
```

### Change Documentation

Use the iFlow **Description** field as a changelog. This description travels with the artifact during transport:

```
v2.1.0 (2026-02-20) - Added APAC region routing branch
v2.0.0 (2026-01-15) - Restructured to nested XML payload format
v1.4.2 (2025-12-10) - Fixed duplicate detection for retry scenarios
v1.4.1 (2025-12-05) - Corrected XPath for optional address fields
v1.4.0 (2025-11-20) - Added email notification on processing failure
v1.3.0 (2025-10-01) - Initial production release
```

### Rollback Using Previous Versions

When a newly transported version causes issues in production:

1. **Via TMS:** Navigate to the previous transport request in TMS history. Re-import the older MTAR to the production node.
2. **Via API:** Download the previous version from DEV (if still available) or from a stored artifact archive, then upload and deploy.
3. **Via Manual:** If you maintain exported `.zip` files of each version, import the previous version's `.zip` and deploy.

**Rollback best practice:** Always keep at least the last 3 versions of each iFlow as exported `.zip` files in a shared file repository or artifact store (e.g., Nexus, Artifactory, S3 bucket).

---

## Transport Best Practices

### Design for Transport

1. **Always externalize environment-specific values**
   - Every URL, credential reference, file path, queue name, and system identifier must be a parameter
   - Test with different parameter values in DEV before transporting

2. **Use meaningful artifact names and descriptions**
   - Names should convey source, target, and purpose at a glance
   - Include version history in the description field

3. **Test in lower environments before production**
   - Run end-to-end tests in QA with production-like data volumes
   - Verify error handling paths, not just happy path

4. **Include all dependent artifacts in same transport**
   - If an iFlow depends on a script collection, value mapping, or number range, transport them together
   - Use Content Agent to bundle related artifacts into a single transport request

5. **Document transport dependencies**
   - Maintain a dependency matrix: which iFlows depend on which script collections, value mappings, and credential artifacts
   - Include prerequisite setup steps (e.g., "Create credential artifact X before deploying")

### Operations

1. **Transport during maintenance windows**
   - Schedule production transports during low-traffic periods
   - Notify stakeholders before and after production transport

2. **Verify deployment after transport**
   - Check Monitor > Integrations and APIs for deployment status
   - Send a test message through the iFlow to confirm end-to-end function
   - Verify externalized parameters are correctly configured

3. **Keep transport history for auditing**
   - TMS maintains full history automatically
   - For manual transports, maintain a transport log spreadsheet:
     - Date, artifact name, version, source, target, transported by, approved by, notes

4. **Implement approval workflow for production transports**
   - Require at least two approvals for production changes (four-eyes principle)
   - TMS approval gates enforce this automatically
   - For API-based transports, add manual approval stages in CI/CD pipelines

### CI/CD

1. **Automate repetitive transports**
   - If you transport the same set of artifacts weekly or more frequently, automate with CI/CD
   - Use pipeline templates to standardize transport procedures across teams

2. **Include automated testing after deployment**
   - Send synthetic test messages after deployment
   - Validate response codes, payload structure, and message processing log entries
   - Fail the pipeline if tests do not pass

3. **Use feature branches for parallel development**
   - Develop new iFlow versions in separate packages or with distinct names
   - Merge to the main artifact only after feature validation in DEV
   - Avoid overwriting each other's changes in shared packages

4. **Implement rollback automation**
   - Store each deployed version's `.zip` as a pipeline artifact
   - Include a rollback stage that can be triggered manually or on failure
   - Test the rollback procedure periodically to ensure it works

---

## Next Steps

- See [operations-monitoring.md](operations-monitoring.md) for deployment verification and message monitoring after transport
- See [cloud-integration.md](cloud-integration.md) for iFlow design patterns and best practices before building transportable artifacts
- See [security.md](security.md) for credential artifact management and secure parameter handling across environments
- See [adapters.md](adapters.md) for adapter-specific externalized parameter requirements
- **For deeper reading:** Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954` to query about transport management, CI/CD pipelines, Content Agent, and environment promotion strategies.
