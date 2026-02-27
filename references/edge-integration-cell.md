# Edge Integration Cell Reference

Comprehensive guide for deploying and operating SAP Integration Suite Edge Integration Cell -- design in cloud, run on-premise Kubernetes.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Deployment Requirements](#deployment-requirements)
- [Deployment Steps](#deployment-steps)
- [Multi-Node Deployment Patterns](#multi-node-deployment-patterns)
- [Scaling and High Availability](#scaling-and-high-availability)
- [Monitoring Edge Runtime](#monitoring-edge-runtime)
- [Content Deployment to Edge](#content-deployment-to-edge)
- [Troubleshooting](#troubleshooting)
- [Platform-Specific Notes](#platform-specific-notes)
- [Migration: Cloud to Edge](#migration-cloud-to-edge)
- [Best Practices](#best-practices)
- [Next Steps](#next-steps)

---

## Overview

### What is Edge Integration Cell?

Edge Integration Cell extends SAP Integration Suite into private landscapes. You design integration content in the cloud-based Integration Suite web UI, then deploy and execute that content on a Kubernetes cluster running in your own data center or private cloud. Sensitive payloads never leave your network perimeter while you retain full access to the cloud-based design, monitoring, and lifecycle management tools.

### Key Benefits

- **Data Sovereignty**: Message payloads and business data remain within your network boundary; only metadata (deployment descriptors, monitoring status, logs) flows to the cloud control plane.
- **Low Latency**: Eliminate round-trips through the public internet for integrations between co-located on-premise systems (SAP S/4HANA, SAP ECC, third-party databases).
- **Regulatory Compliance**: Satisfy GDPR, HIPAA, PCI-DSS, and national data-localization mandates by keeping data processing on-premise.
- **Hybrid Flexibility**: Run some iFlows on the cloud runtime and others on edge -- choose per integration flow based on data sensitivity and latency requirements.

### Edge vs Cloud Runtime Comparison

| Capability                          | Cloud Runtime              | Edge Integration Cell             |
|-------------------------------------|----------------------------|-----------------------------------|
| Design-time UI                      | Cloud (BTP)                | Cloud (BTP) -- same UI            |
| Runtime execution location          | SAP-managed cloud          | Customer-managed Kubernetes       |
| Data residency                      | SAP data center region     | Customer data center              |
| Supported adapters                  | All adapters               | Subset (see Adapter Compatibility)|
| JMS queues                          | Managed by SAP             | Customer-managed (persistent vol) |
| Keystore management                 | Cloud keystore             | Synchronized from cloud           |
| Monitoring                          | Full MPL in cloud UI       | MPL in cloud UI + K8s monitoring  |
| Auto-scaling                        | Managed by SAP             | Customer-configured HPA           |
| Infrastructure responsibility       | SAP                        | Customer                          |
| Network: internet-facing endpoints  | Native                     | Requires Ingress + DNS config     |
| Software updates                    | Automatic (SAP-managed)    | Customer-initiated via Helm       |
| Maximum message size                | 40 MB (default)            | Configurable (depends on PV size) |
| Data Store / Variables              | Cloud-managed              | Local persistent volumes          |

---

## Architecture

### Component Diagram

```
+------------------------------------------------------------------+
|                    SAP BTP (Cloud Control Plane)                  |
|                                                                  |
|  +---------------------+   +-----------------------------+      |
|  | Integration Suite UI |   | Edge Lifecycle Management   |      |
|  | (Design, Monitor,   |   | (Deployment Orchestration,  |      |
|  |  Configure)         |   |  Updates, Health Tracking)  |      |
|  +---------------------+   +-----------------------------+      |
|            |                          |                          |
+------------|--------------------------|------ Port 443 / TLS ----+
             |                          |
   ========= | ======================== | ==== Outbound HTTPS =====
             |                          |
+------------|--------------------------|---------------------------+
|            v                          v                          |
|  Customer Kubernetes Cluster (On-Premise / Private Cloud)        |
|                                                                  |
|  +-----------------------------+  +---------------------------+  |
|  | Edge Lifecycle Management   |  | Edge Integration Cell     |  |
|  | Bridge (Edge LM Bridge)     |  | Runtime                   |  |
|  |                             |  |                           |  |
|  | - Receives deployment       |  | - Executes iFlows         |  |
|  |   commands from cloud       |  | - Message processing      |  |
|  | - Manages Edge Cell         |  | - Adapter runtime         |  |
|  |   lifecycle (install,       |  | - Local data store        |  |
|  |   update, rollback)         |  | - Local keystore cache    |  |
|  +-----------------------------+  +---------------------------+  |
|                                                                  |
|  +-----------------------------+  +---------------------------+  |
|  | Edge Policy Engine          |  | Istio Service Mesh        |  |
|  |                             |  |                           |  |
|  | - Enforces security policy  |  | - mTLS between pods       |  |
|  | - Certificate management    |  | - Traffic management      |  |
|  | - Access control            |  | - Observability (traces)  |  |
|  +-----------------------------+  +---------------------------+  |
|                                                                  |
|  +------------------------------------------------------------+  |
|  | Ingress Controller (NGINX / ALB / AGIC / OpenShift Router) |  |
|  +------------------------------------------------------------+  |
|            |                                                     |
+------------|-----------------------------------------------------+
             v
     External Systems (SAP S/4HANA, Databases, Partners)
```

### Communication Flow: Cloud Control Plane <-> Edge Runtime

```
Cloud Control Plane                   Edge Runtime (K8s)
       |                                     |
       |  1. Deploy iFlow command            |
       | ----------------------------------> |
       |     (HTTPS 443, outbound from Edge) |
       |                                     |
       |  2. Keystore sync (certificates,    |
       |     credentials)                    |
       | ----------------------------------> |
       |                                     |
       |  3. Runtime status + health         |
       | <---------------------------------- |
       |                                     |
       |  4. MPL data (message logs,         |
       |     error details, metrics)         |
       | <---------------------------------- |
       |                                     |
       |  5. Software update packages        |
       | ----------------------------------> |
       |                                     |
```

**Key point:** The Edge LM Bridge initiates all connections **outbound** to the cloud control plane over HTTPS (port 443). No inbound connections from the internet to the edge cluster are required for the control plane channel. Inbound connections are only needed if external systems need to call edge-hosted endpoints (via the Ingress controller).

### Data Flow: Design -> Deploy -> Monitor

```
  1. DESIGN              2. DEPLOY               3. EXECUTE            4. MONITOR
+---------------+    +----------------+    +------------------+    +----------------+
| Integration   |    | Select target: |    | iFlow runs on    |    | Cloud UI shows |
| Suite UI      | -> | "Edge Runtime" | -> | Edge K8s pods    | -> | MPL, status,   |
| (cloud)       |    | Deploy         |    | (on-premise)     |    | metrics         |
+---------------+    +----------------+    +------------------+    +----------------+
                                                  |
                                           Payloads stay
                                           on-premise
```

### Network Requirements

| Requirement                    | Direction | Port | Protocol | Purpose                                    |
|-------------------------------|-----------|------|----------|--------------------------------------------|
| Edge LM Bridge -> BTP         | Outbound  | 443  | HTTPS    | Control plane communication                |
| Edge Cell -> BTP              | Outbound  | 443  | HTTPS    | Monitoring data, keystore sync             |
| External -> Ingress Controller| Inbound   | 443  | HTTPS    | Receive messages from external senders     |
| Pod-to-pod (Istio mesh)       | Internal  | *    | mTLS     | Inter-service communication within cluster |
| kubectl -> API Server         | Internal  | 6443 | HTTPS    | Cluster administration                     |
| Helm -> API Server            | Internal  | 6443 | HTTPS    | Deployment operations                      |
| DNS resolution                | Outbound  | 53   | UDP/TCP  | Resolve BTP endpoints and external hosts   |

**DNS endpoints that must be reachable from the edge cluster:**

- `*.hana.ondemand.com` (BTP platform services)
- `*.integration.cloud.sap` (Integration Suite services)
- `*.edge.integration.cloud.sap` (Edge-specific endpoints)
- Container registry for SAP images (region-specific)

**TLS Requirements:**

- TLS 1.2 minimum (TLS 1.3 recommended)
- Outbound traffic must not be terminated by proxy/firewall (or proxy must support HTTPS CONNECT)
- Ingress TLS certificates: customer-managed (wildcard or SAN certificates recommended)

---

## Deployment Requirements

### Supported Kubernetes Platforms

| Platform                        | Version     | Status       | Notes                                      |
|---------------------------------|-------------|--------------|--------------------------------------------|
| Amazon EKS                      | 1.27+       | Supported    | Most common; use EBS gp3 for storage       |
| Azure AKS                       | 1.27+       | Supported    | Azure Disk for PV; AGIC for ingress        |
| Google GKE                      | 1.27+       | Supported    | GCE PD for PV; GKE Ingress or NGINX       |
| Red Hat OpenShift               | 4.12+       | Supported    | Uses Routes; requires SCC configuration    |
| SUSE Rancher RKE2               | 1.27+       | Supported    | Longhorn or local-path for storage         |
| K3s                             | 1.27+       | Supported    | Lightweight; suitable for edge locations   |

### Minimum Resource Requirements (Per Worker Node)

| Resource   | Minimum (Dev/Test)     | Recommended (Production) | Notes                                  |
|------------|------------------------|--------------------------|----------------------------------------|
| vCPU       | 4 cores                | 8 cores                  | Edge Cell pods are CPU-intensive       |
| RAM        | 16 GB                  | 32 GB                    | JVM heap + Istio sidecar overhead      |
| Storage    | 100 GB SSD             | 250 GB SSD               | Persistent volumes for logs, data store|
| Network    | 1 Gbps                 | 10 Gbps                  | Throughput depends on message volume   |

### Prerequisites Checklist

| Prerequisite                              | Details                                            |
|-------------------------------------------|----------------------------------------------------|
| SAP BTP Global Account                    | With Integration Suite entitlement (Edge enabled)  |
| Integration Suite subscription            | Activated in BTP subaccount                        |
| Edge Integration Cell feature activated   | Settings -> Runtime -> Edge Integration Cell       |
| Kubernetes cluster                        | Running, accessible via kubectl                    |
| kubectl                                   | v1.27+ configured with cluster admin context       |
| Helm                                      | v3.12+ installed on workstation                    |
| Ingress Controller                        | NGINX Ingress, ALB, AGIC, or OpenShift Router      |
| DNS                                       | Wildcard or specific DNS entries for edge endpoints |
| TLS Certificates                          | For Ingress (customer-provided or cert-manager)    |
| Outbound internet access                  | HTTPS 443 to SAP BTP endpoints                     |
| Container registry access                 | Pull SAP container images (authenticated)          |
| Persistent Volume provisioner             | StorageClass configured (EBS, Azure Disk, GCE PD)  |

---

## Deployment Steps

### Step 1: Activate Edge Integration Cell in Integration Suite

1. Open SAP Integration Suite in your BTP subaccount.
2. Navigate to **Settings** -> **Runtime** -> **Edge Integration Cell**.
3. Click **Activate** to enable the Edge Integration Cell capability.
4. Note the **Tenant ID** and **Token Endpoint URL** displayed after activation -- you will need these in subsequent steps.

### Step 2: Generate Deployment Token and Artifacts

1. In the Edge Integration Cell settings page, click **Add Edge Node**.
2. Provide a meaningful name (e.g., `edge-dc-frankfurt-prod`).
3. Click **Generate** to create:
   - **Deployment Token** (one-time use; expires in 24 hours)
   - **edge-values.yaml** template (Helm values file)
4. Download the `edge-values.yaml` file and securely store the deployment token.

### Step 3: Deploy Edge Lifecycle Management Bridge

The Edge LM Bridge is the first component deployed. It establishes the secure tunnel between your cluster and the BTP cloud control plane.

```bash
# Create the namespace
kubectl create namespace edge-integration-cell

# Create the secret with the deployment token
kubectl create secret generic edge-lm-token \
  --namespace edge-integration-cell \
  --from-literal=token=<YOUR_DEPLOYMENT_TOKEN>

# Add the SAP Helm repository
helm repo add sap-edge https://edge.integration.cloud.sap/helm
helm repo update

# Deploy Edge Lifecycle Management Bridge
helm install edge-lm sap-edge/edge-lifecycle-management \
  --namespace edge-integration-cell \
  --set config.tenantId=<YOUR_TENANT_ID> \
  --set config.tokenSecretName=edge-lm-token \
  --set config.cloudEndpoint=https://<YOUR_BTP_REGION>.edge.integration.cloud.sap \
  --timeout 10m \
  --wait
```

Verify the Edge LM Bridge is running:

```bash
kubectl get pods -n edge-integration-cell -l app=edge-lm-bridge
# Expected output:
# NAME                              READY   STATUS    RESTARTS   AGE
# edge-lm-bridge-6f8d4c7b9-x2k4p   1/1     Running   0          2m
```

### Step 4: Deploy Edge Integration Cell Solution

Customize the `edge-values.yaml` file downloaded in Step 2:

```yaml
# edge-values.yaml -- Key configuration parameters
global:
  tenantId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  cloudEndpoint: "https://eu10.edge.integration.cloud.sap"

runtime:
  replicas: 2
  resources:
    requests:
      cpu: "2"
      memory: "4Gi"
    limits:
      cpu: "4"
      memory: "8Gi"
  javaOpts: "-Xms2g -Xmx6g -XX:+UseG1GC"

persistence:
  storageClassName: "gp3"          # EKS example; adjust per platform
  dataStore:
    size: "50Gi"
  logs:
    size: "20Gi"

ingress:
  enabled: true
  className: "nginx"
  host: "edge-integration.mycompany.com"
  tls:
    enabled: true
    secretName: "edge-tls-cert"

istio:
  enabled: true
  mtls:
    mode: STRICT

monitoring:
  prometheus:
    enabled: true
    port: 9090
```

Deploy the Edge Integration Cell:

```bash
helm install edge-cell sap-edge/edge-integration-cell \
  --namespace edge-integration-cell \
  --values edge-values.yaml \
  --timeout 15m \
  --wait
```

### Step 5: Configure Keystore Synchronization

The keystore is automatically synchronized from your cloud tenant to the edge runtime. Verify synchronization:

1. In Integration Suite UI, navigate to **Monitor** -> **Keystore**.
2. Confirm certificates are listed with status **Synchronized** for the edge node.
3. On the cluster, verify the keystore secret exists:

```bash
kubectl get secrets -n edge-integration-cell -l app=edge-keystore
# Expected output:
# NAME                   TYPE     DATA   AGE
# edge-keystore-sync     Opaque   3      5m
```

To force a resynchronization:

```bash
# Restart the keystore sync pod to trigger immediate re-sync
kubectl rollout restart deployment/edge-keystore-sync \
  -n edge-integration-cell
```

### Step 6: Verify Deployment

Run the following commands to confirm all components are healthy:

```bash
# Check all pods in the edge namespace
kubectl get pods -n edge-integration-cell
# Expected: All pods in Running state, READY x/x

# Verify services
kubectl get svc -n edge-integration-cell
# Expected: edge-runtime, edge-lm-bridge, edge-policy-engine services

# Check Ingress
kubectl get ingress -n edge-integration-cell
# Expected: Ingress with configured host and TLS

# Verify persistent volumes are bound
kubectl get pvc -n edge-integration-cell
# Expected: All PVCs in Bound state

# Check Edge LM Bridge connectivity to cloud
kubectl logs -n edge-integration-cell \
  deployment/edge-lm-bridge --tail=20 | grep "connection"
# Expected: "Cloud control plane connection established"

# Verify Istio sidecar injection
kubectl get pods -n edge-integration-cell -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}'
# Expected: Each pod shows istio-proxy container alongside main container
```

In the Integration Suite cloud UI:

1. Navigate to **Settings** -> **Runtime** -> **Edge Integration Cell**.
2. Verify the edge node shows status **Connected** (green indicator).

### Step 7: Deploy Integration Content to Edge Runtime

1. Open your integration package in the Integration Suite UI.
2. Select the iFlow to deploy.
3. Click **Deploy** and in the deployment dialog, select **Runtime Profile: Edge**.
4. Choose your registered edge node as the target.
5. Click **Deploy** and monitor the deployment status.

---

## Multi-Node Deployment Patterns

### Single-Node Development Setup

Suitable for development and testing. Not recommended for production.

```
+----------------------------------------------+
|  Single Worker Node (8 vCPU, 32 GB RAM)      |
|                                              |
|  [Edge LM Bridge] [Edge Runtime x1]         |
|  [Edge Policy Engine] [Istio Control Plane]  |
|  [Ingress Controller]                        |
+----------------------------------------------+
```

| Parameter           | Value        |
|---------------------|--------------|
| Worker nodes        | 1            |
| Runtime replicas    | 1            |
| CPU per node        | 8 vCPU       |
| RAM per node        | 32 GB        |
| Storage per node    | 100 GB SSD   |
| Max iFlows deployed | ~20          |
| Message throughput  | ~50 msg/sec  |

### 3-Node HA Cluster (Production Recommended)

High availability with pod anti-affinity ensuring Edge Runtime replicas run on separate nodes.

```
+--------------------+  +--------------------+  +--------------------+
|  Worker Node 1     |  |  Worker Node 2     |  |  Worker Node 3     |
|  8 vCPU, 32 GB     |  |  8 vCPU, 32 GB     |  |  8 vCPU, 32 GB     |
|                    |  |                    |  |                    |
|  [Edge Runtime]    |  |  [Edge Runtime]    |  |  [Edge Runtime]    |
|  [Istio Sidecar]   |  |  [Istio Sidecar]   |  |  [Istio Sidecar]   |
|  [Edge LM Bridge]  |  |  [Policy Engine]   |  |  [Ingress Ctrl]    |
+--------------------+  +--------------------+  +--------------------+
         |                        |                        |
         +------------------------+------------------------+
                          Shared Storage (PV)
```

### Multi-Region Edge Deployment

For organizations with multiple data centers, deploy separate Edge Integration Cell instances per region, all managed from a single cloud tenant.

```
                     +---------------------------+
                     |  SAP BTP Cloud Tenant     |
                     |  (Single Control Plane)   |
                     +-----|-----------|----------+
                           |           |
              +------------+           +-------------+
              |                                      |
   +----------v----------+            +--------------v-------+
   | Edge Cell: EU-WEST  |            | Edge Cell: US-EAST   |
   | Frankfurt DC        |            | Virginia DC          |
   | (3 nodes, HA)       |            | (3 nodes, HA)        |
   +---------------------+            +----------------------+
```

### Resource Sizing Table

| Size   | Use Case       | Nodes | vCPU/Node | RAM/Node | Storage/Node | Max iFlows | Throughput     |
|--------|----------------|-------|-----------|----------|--------------|------------|----------------|
| Small  | Development    | 1     | 4 vCPU    | 16 GB    | 100 GB SSD   | ~10        | ~20 msg/sec    |
| Medium | QA / Staging   | 2     | 8 vCPU    | 32 GB    | 150 GB SSD   | ~50        | ~200 msg/sec   |
| Large  | Production     | 3     | 8 vCPU    | 32 GB    | 250 GB SSD   | ~100       | ~500 msg/sec   |
| XLarge | High Volume    | 5+    | 16 vCPU   | 64 GB    | 500 GB SSD   | ~200       | ~2000 msg/sec  |

---

## Scaling and High Availability

### Horizontal Pod Autoscaler (HPA) Configuration

Configure HPA to auto-scale Edge Runtime worker pods based on load:

```yaml
# edge-runtime-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: edge-runtime-hpa
  namespace: edge-integration-cell
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: edge-runtime
  minReplicas: 2
  maxReplicas: 8
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 75
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 120
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 180
```

Apply the HPA:

```bash
kubectl apply -f edge-runtime-hpa.yaml

# Verify HPA status
kubectl get hpa -n edge-integration-cell
# NAME               REFERENCE              TARGETS           MINPODS   MAXPODS   REPLICAS
# edge-runtime-hpa   Deployment/edge-runtime 35%/70%, 40%/75%  2         8         2
```

### Scaling Triggers

| Trigger              | Metric                          | Scale-Up Threshold | Scale-Down Threshold |
|----------------------|---------------------------------|--------------------|----------------------|
| CPU utilization      | Pod average CPU %               | > 70%              | < 30%               |
| Memory utilization   | Pod average memory %            | > 75%              | < 40%               |
| Queue depth          | JMS pending messages (custom)   | > 1000 messages    | < 100 messages       |
| HTTP request rate    | Requests per second per pod     | > 200 req/s        | < 50 req/s           |

### Failover and Recovery Procedures

**Pod failure recovery:**

```bash
# Kubernetes automatically restarts failed pods via the Deployment controller.
# Verify restart behavior:
kubectl describe deployment edge-runtime -n edge-integration-cell | grep -A5 "Strategy"
# Expected: RollingUpdate with maxUnavailable=1
```

**Node failure recovery:**

- Pod anti-affinity rules ensure replicas are distributed across nodes.
- When a node fails, Kubernetes reschedules pods to healthy nodes (within 5 minutes by default).
- For faster failover, configure pod disruption budgets:

```yaml
# edge-runtime-pdb.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: edge-runtime-pdb
  namespace: edge-integration-cell
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: edge-runtime
```

### Backup and Restore for Persistent Volumes

```bash
# Create a VolumeSnapshot of the data store PVC
cat <<EOF | kubectl apply -f -
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: edge-datastore-snapshot-$(date +%Y%m%d)
  namespace: edge-integration-cell
spec:
  volumeSnapshotClassName: csi-snapclass
  source:
    persistentVolumeClaimName: edge-datastore-pvc
EOF

# List snapshots
kubectl get volumesnapshot -n edge-integration-cell

# Restore from snapshot (create new PVC from snapshot)
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: edge-datastore-pvc-restored
  namespace: edge-integration-cell
spec:
  storageClassName: gp3
  dataSource:
    name: edge-datastore-snapshot-20260225
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 50Gi
EOF
```

---

## Monitoring Edge Runtime

### Cloud UI Monitoring Capabilities

The Integration Suite cloud UI provides the same monitoring experience for edge-deployed iFlows as for cloud-deployed ones:

- **Message Processing Log (MPL)**: View message status (Completed, Failed, Retry, Escalated) for each iFlow execution.
- **Deployment Status**: See which iFlows are deployed to which edge node, their status, and version.
- **Edge Node Health**: View overall edge node connectivity status (Connected / Disconnected / Error).
- **Keystore Status**: Monitor certificate synchronization and expiry dates.

Access monitoring via: **Integration Suite** -> **Monitor** -> **Integrations** -> Filter by Runtime: **Edge**.

### Kubernetes-Native Monitoring: Prometheus + Grafana

Deploy Prometheus and Grafana for infrastructure-level monitoring:

```bash
# Install kube-prometheus-stack via Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=<SECURE_PASSWORD> \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false
```

Create a ServiceMonitor for Edge Integration Cell metrics:

```yaml
# edge-servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: edge-runtime-monitor
  namespace: monitoring
  labels:
    release: monitoring
spec:
  namespaceSelector:
    matchNames:
      - edge-integration-cell
  selector:
    matchLabels:
      app: edge-runtime
  endpoints:
    - port: metrics
      path: /metrics
      interval: 30s
```

### Health Check Endpoints

| Endpoint                         | Method | Expected Response | Purpose                           |
|----------------------------------|--------|-------------------|-----------------------------------|
| `/api/v1/health`                 | GET    | 200 OK            | Overall edge runtime health       |
| `/api/v1/health/readiness`       | GET    | 200 OK            | Pod readiness (K8s readiness probe)|
| `/api/v1/health/liveness`        | GET    | 200 OK            | Pod liveness (K8s liveness probe) |
| `/api/v1/health/cloud-connection`| GET    | 200 OK            | Cloud control plane connectivity  |

```bash
# Test health endpoint from within the cluster
kubectl exec -n edge-integration-cell deployment/edge-runtime -- \
  curl -s http://localhost:8080/api/v1/health
# Expected: {"status":"UP","components":{"runtime":"UP","keystore":"UP","cloudConnection":"UP"}}
```

### Alert Configuration

Example Prometheus alert rules for edge node failures:

```yaml
# edge-alerts.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: edge-runtime-alerts
  namespace: monitoring
spec:
  groups:
    - name: edge-integration-cell
      rules:
        - alert: EdgeRuntimePodDown
          expr: kube_deployment_status_replicas_available{deployment="edge-runtime", namespace="edge-integration-cell"} < 1
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Edge Runtime has no available replicas"
            description: "All Edge Runtime pods are down for more than 5 minutes."

        - alert: EdgeHighCPU
          expr: rate(container_cpu_usage_seconds_total{namespace="edge-integration-cell", container="edge-runtime"}[5m]) > 0.9
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "Edge Runtime CPU usage above 90%"

        - alert: EdgeHighMemory
          expr: container_memory_working_set_bytes{namespace="edge-integration-cell", container="edge-runtime"} / container_spec_memory_limit_bytes > 0.85
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "Edge Runtime memory usage above 85%"

        - alert: EdgeCloudConnectionLost
          expr: edge_cloud_connection_status == 0
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Edge node lost connection to cloud control plane"
```

### Key Metrics Table

| Metric                                     | Type    | Description                                | Alert Threshold        |
|--------------------------------------------|---------|--------------------------------------------|------------------------|
| `kube_pod_status_phase`                    | Gauge   | Pod lifecycle phase (Running, Pending, etc)| != Running for > 5m    |
| `container_cpu_usage_seconds_total`        | Counter | CPU usage per container                    | > 90% sustained 10m    |
| `container_memory_working_set_bytes`       | Gauge   | Current memory usage per container         | > 85% of limit         |
| `edge_message_processing_duration_seconds` | Histogram| Time to process a single message          | p99 > 30s              |
| `edge_message_processing_total`            | Counter | Total messages processed (success/failure) | Error rate > 5%        |
| `edge_jms_queue_depth`                     | Gauge   | Number of pending messages in JMS queues   | > 5000                 |
| `edge_cloud_connection_status`             | Gauge   | 1 = connected, 0 = disconnected           | == 0 for > 2m          |
| `edge_keystore_sync_age_seconds`           | Gauge   | Seconds since last keystore sync           | > 3600 (1 hour)        |

---

## Content Deployment to Edge

### Deploying iFlows to Edge Runtime

1. **From Integration Suite UI:**
   - Open the integration package containing your iFlow.
   - Click on the iFlow, then click **Deploy**.
   - In the **Runtime Profile** dropdown, select your edge node name.
   - Click **Deploy**. The deployment descriptor is sent to the cloud control plane, which pushes it to the Edge LM Bridge, which instructs the Edge Runtime to pull and start the iFlow.

2. **Switching deployment target (cloud vs edge):**
   - An iFlow can be deployed to cloud runtime, edge runtime, or both simultaneously.
   - To move an iFlow from cloud to edge: undeploy from cloud, then deploy to edge.
   - To run on both: deploy separately to each runtime (useful during migration).

### Runtime Profile Configuration

Runtime profiles define which edge node receives a deployment. Configure profiles in:

**Settings** -> **Runtime** -> **Runtime Profiles**

| Parameter                  | Description                                          |
|----------------------------|------------------------------------------------------|
| Profile Name               | Unique identifier (e.g., `edge-eu-prod`)             |
| Target Runtime             | Edge node name                                       |
| Default for Package        | Optionally set as default for specific packages      |
| Logging Level              | INFO, DEBUG, TRACE (for edge-specific log verbosity) |

### Adapter Compatibility Matrix

Not all adapters available in the cloud runtime are supported on Edge Integration Cell. The following table summarizes availability:

| Adapter               | Cloud Runtime | Edge Runtime | Notes                                       |
|-----------------------|:------------:|:------------:|---------------------------------------------|
| HTTP                  | Yes          | Yes          | Full support                                |
| HTTPS (REST)          | Yes          | Yes          | Full support                                |
| SOAP                  | Yes          | Yes          | Full support                                |
| OData V2/V4           | Yes          | Yes          | Full support                                |
| SFTP                  | Yes          | Yes          | Full support                                |
| RFC (SAP)             | Yes          | Yes          | Requires JCo libraries on edge              |
| IDoc (SAP)            | Yes          | Yes          | Requires JCo libraries on edge              |
| JDBC                  | Yes          | Yes          | Driver must be deployed to edge             |
| AMQP                  | Yes          | Yes          | Full support                                |
| Kafka                 | Yes          | Yes          | Full support                                |
| JMS                   | Yes          | Yes          | Local broker on edge PV                     |
| Mail (SMTP/IMAP)      | Yes          | Yes          | Full support                                |
| ProcessDirect         | Yes          | Yes          | Within same edge runtime only               |
| AS2                   | Yes          | Partial      | Sender only; MDN support varies             |
| AS4                   | Yes          | Partial      | Limited profile support                     |
| AmazonWebServices     | Yes          | No           | Cloud-only adapter                          |
| Microsoft Dynamics    | Yes          | No           | Cloud-only adapter                          |
| Salesforce            | Yes          | No           | Cloud-only adapter                          |
| SuccessFactors        | Yes          | Yes          | Full support                                |
| Ariba                 | Yes          | No           | Cloud-only adapter                          |
| Splunk                | Yes          | No           | Cloud-only adapter                          |

**Note:** Adapter availability is updated with each SAP Integration Suite release. Always verify the current compatibility matrix in the SAP Help Portal for your specific version.

---

## Troubleshooting

### Common Deployment Errors

| Error                                              | Likely Cause                               | Resolution                                                                                              |
|----------------------------------------------------|--------------------------------------------|---------------------------------------------------------------------------------------------------------|
| `ImagePullBackOff` during Helm install             | Cannot pull SAP container images           | Verify image pull secret exists; check registry credentials; confirm network access to SAP registry     |
| `Insufficient cpu` / `Insufficient memory`         | Node resources exhausted                   | Scale up nodes or reduce resource requests in `edge-values.yaml`; check with `kubectl describe node`   |
| `Error: INSTALLATION FAILED: ...RBAC`              | Missing Kubernetes RBAC permissions        | Ensure Helm is run with cluster-admin context; create required ClusterRole and ClusterRoleBinding       |
| Edge LM Bridge: `Cloud connection failed`          | Outbound HTTPS blocked by firewall         | Verify port 443 outbound to `*.edge.integration.cloud.sap`; check proxy/firewall rules                 |
| Edge LM Bridge: `Certificate validation failed`    | TLS inspection breaking the tunnel         | Whitelist SAP endpoints from TLS inspection; verify CA certificates in the cluster trust store          |
| `CrashLoopBackOff` on edge-runtime pod             | Out-of-memory or invalid configuration     | Check logs: `kubectl logs <pod> -n edge-integration-cell`; increase memory limits; verify `javaOpts`   |
| Keystore sync: `Synchronization failed`            | Cloud connection interrupted               | Verify Edge LM Bridge is connected; restart keystore sync pod; check cloud keystore for errors         |
| Ingress: `502 Bad Gateway`                         | Backend service not ready                  | Verify edge-runtime pods are Running; check Ingress annotations match your controller                  |
| Ingress: `Connection refused`                      | Ingress controller not routing to service  | Verify service selectors match pod labels; check Ingress class matches controller                      |
| DNS: `could not resolve host`                      | DNS not configured for edge endpoints      | Add DNS entries for edge hostnames; verify CoreDNS is running in cluster                               |
| iFlow deploy to edge: `Deployment failed`          | Adapter not supported on edge              | Check adapter compatibility matrix; switch to a supported adapter                                       |
| iFlow deploy to edge: `Runtime profile not found`  | Edge node not registered or disconnected   | Verify edge node status in cloud UI; re-register if needed                                             |

### Diagnostic Commands

```bash
# Check all pods status (first thing to check)
kubectl get pods -n edge-integration-cell -o wide

# Describe a failing pod for events and conditions
kubectl describe pod <POD_NAME> -n edge-integration-cell

# View logs for a specific pod (last 100 lines)
kubectl logs <POD_NAME> -n edge-integration-cell --tail=100

# View logs for the previous crashed container
kubectl logs <POD_NAME> -n edge-integration-cell --previous

# Check events in the namespace (sorted by time)
kubectl get events -n edge-integration-cell --sort-by='.lastTimestamp'

# Check resource usage per pod
kubectl top pods -n edge-integration-cell

# Check node resource availability
kubectl top nodes

# Check PersistentVolumeClaim status
kubectl get pvc -n edge-integration-cell

# Check Helm release status
helm status edge-cell -n edge-integration-cell

# Check Helm release history (for rollback)
helm history edge-cell -n edge-integration-cell

# Test internal connectivity to edge runtime
kubectl run test-curl --rm -it --image=curlimages/curl \
  -n edge-integration-cell -- \
  curl -s http://edge-runtime:8080/api/v1/health

# Check Istio sidecar injection
kubectl get namespace edge-integration-cell -o jsonpath='{.metadata.labels}'
# Should include: istio-injection=enabled
```

### Edge-Specific Log Locations

| Component           | Log Access                                                         | Content                              |
|---------------------|--------------------------------------------------------------------|--------------------------------------|
| Edge Runtime        | `kubectl logs deploy/edge-runtime -n edge-integration-cell`        | iFlow execution, message processing |
| Edge LM Bridge      | `kubectl logs deploy/edge-lm-bridge -n edge-integration-cell`      | Cloud connection, deployment events  |
| Edge Policy Engine  | `kubectl logs deploy/edge-policy-engine -n edge-integration-cell`  | Security policy enforcement          |
| Istio Sidecar       | `kubectl logs <pod> -c istio-proxy -n edge-integration-cell`       | mTLS, traffic routing                |
| Ingress Controller  | `kubectl logs deploy/ingress-nginx-controller -n ingress-nginx`    | Inbound request routing              |

---

## Platform-Specific Notes

### Amazon EKS

**Storage Class (EBS gp3):**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

**Ingress (AWS ALB):**

```yaml
# In edge-values.yaml
ingress:
  enabled: true
  className: "alb"
  annotations:
    alb.ingress.kubernetes.io/scheme: internal
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:eu-central-1:123456789012:certificate/abc-def
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
    alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS13-1-2-2021-06
```

**IAM Roles for Service Accounts (IRSA):**

Use IRSA to grant the edge pods access to AWS services (e.g., Secrets Manager, S3) without static credentials:

```bash
eksctl create iamserviceaccount \
  --name edge-runtime-sa \
  --namespace edge-integration-cell \
  --cluster my-edge-cluster \
  --attach-policy-arn arn:aws:iam::123456789012:policy/EdgeRuntimePolicy \
  --approve
```

### Azure AKS

**Storage Class (Azure Disk):**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: managed-premium-retain
provisioner: disk.csi.azure.com
parameters:
  skuName: Premium_LRS
  kind: Managed
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

**Ingress (Application Gateway Ingress Controller - AGIC):**

```yaml
ingress:
  enabled: true
  className: "azure-application-gateway"
  annotations:
    appgw.ingress.kubernetes.io/ssl-redirect: "true"
    appgw.ingress.kubernetes.io/use-private-ip: "true"
    appgw.ingress.kubernetes.io/backend-protocol: "https"
```

**Managed Identity:**

Use Azure AD Workload Identity to grant pods access to Azure Key Vault and other services:

```bash
az identity create \
  --name edge-runtime-identity \
  --resource-group my-edge-rg

az identity federated-credential create \
  --name edge-runtime-fc \
  --identity-name edge-runtime-identity \
  --resource-group my-edge-rg \
  --issuer "$(az aks show -n my-aks -g my-edge-rg --query oidcIssuerProfile.issuerUrl -o tsv)" \
  --subject system:serviceaccount:edge-integration-cell:edge-runtime-sa
```

### Google GKE

**Storage Class (GCE Persistent Disk):**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ssd-retain
provisioner: pd.csi.storage.gke.io
parameters:
  type: pd-ssd
reclaimPolicy: Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

**Ingress (GKE Ingress):**

```yaml
ingress:
  enabled: true
  className: "gce-internal"
  annotations:
    kubernetes.io/ingress.class: "gce-internal"
    networking.gke.io/managed-certificates: "edge-tls-cert"
```

**Workload Identity:**

```bash
gcloud iam service-accounts create edge-runtime-sa \
  --project=my-project-id

gcloud iam service-accounts add-iam-policy-binding \
  edge-runtime-sa@my-project-id.iam.gserviceaccount.com \
  --role=roles/iam.workloadIdentityUser \
  --member="serviceAccount:my-project-id.svc.id.goog[edge-integration-cell/edge-runtime-sa]"
```

### Red Hat OpenShift

**Routes (instead of Ingress):**

OpenShift uses Routes natively. Configure the edge deployment to use Routes:

```yaml
# In edge-values.yaml for OpenShift
ingress:
  enabled: false   # Disable standard Ingress

openshift:
  route:
    enabled: true
    host: "edge-integration.apps.ocp.mycompany.com"
    tls:
      termination: edge
      insecureEdgeTerminationPolicy: Redirect
      certificate: |
        -----BEGIN CERTIFICATE-----
        ...
        -----END CERTIFICATE-----
      key: |
        -----BEGIN RSA PRIVATE KEY-----
        ...
        -----END RSA PRIVATE KEY-----
```

**Security Context Constraints (SCC):**

Edge Integration Cell pods require specific Linux capabilities. Create a custom SCC:

```yaml
apiVersion: security.openshift.io/v1
kind: SecurityContextConstraints
metadata:
  name: edge-integration-scc
allowPrivilegedContainer: false
runAsUser:
  type: MustRunAsRange
  uidRangeMin: 1000
  uidRangeMax: 65534
seLinuxContext:
  type: RunAsAny
fsGroup:
  type: MustRunAs
  ranges:
    - min: 1000
      max: 65534
volumes:
  - configMap
  - downwardAPI
  - emptyDir
  - persistentVolumeClaim
  - projected
  - secret
```

Assign the SCC to the edge service account:

```bash
oc adm policy add-scc-to-user edge-integration-scc \
  -z edge-runtime-sa -n edge-integration-cell
```

---

## Migration: Cloud to Edge

### Criteria for Choosing Cloud vs Edge Runtime

| Criterion                          | Choose Cloud Runtime                   | Choose Edge Runtime                      |
|------------------------------------|----------------------------------------|------------------------------------------|
| Data sensitivity                   | Non-sensitive data                     | PII, financial data, regulated data      |
| Latency requirements               | Acceptable internet latency            | Sub-millisecond to on-premise systems    |
| Internet-facing endpoints          | Required (public APIs, SaaS)           | Not required; all systems on-premise     |
| Cloud-only adapters needed         | Yes (Salesforce, Ariba, AWS, etc.)     | No cloud-only adapters needed            |
| Infrastructure management          | Prefer SAP-managed                     | Team has K8s operational capability      |
| Regulatory compliance              | No data residency mandates             | Strict data residency requirements       |
| Partner/B2B integrations           | Partners accessible via internet       | Partners on same network / VPN           |

### Step-by-Step Migration Workflow

```
1. ASSESS                  2. PREPARE               3. MIGRATE              4. VALIDATE
+-------------------+    +------------------+    +------------------+    +------------------+
| Identify iFlows   |    | Deploy Edge Cell |    | Redeploy iFlows  |    | Run parallel     |
| for migration     | -> | Verify adapters  | -> | to Edge runtime  | -> | Compare results  |
| Check adapter     |    | Configure Ingress|    | Switch DNS if    |    | Undeploy from    |
| compatibility     |    | Set up monitoring|    | sender-facing    |    | cloud runtime    |
+-------------------+    +------------------+    +------------------+    +------------------+
```

1. **Assess:** Inventory iFlows, check adapter compatibility, identify cloud-only dependencies.
2. **Prepare:** Deploy and validate Edge Integration Cell (Steps 1-6 above). Configure DNS entries for edge endpoints.
3. **Migrate:** Deploy each iFlow to edge runtime. For sender-facing iFlows, update DNS to point to edge Ingress. Run both cloud and edge versions in parallel during transition.
4. **Validate:** Compare MPL results between cloud and edge. Verify message counts, error rates, and latency. Once validated, undeploy from cloud runtime.

### Testing Hybrid Cloud + Edge Architecture

During migration, you can run the same iFlow on both runtimes simultaneously:

- Deploy version A to cloud runtime (existing).
- Deploy version A to edge runtime (new).
- Use a load balancer or DNS-based routing to split traffic.
- Compare MPL results in the Integration Suite monitoring UI (filter by runtime).
- Gradually shift traffic to edge once confidence is established.

### Rollback Strategy

If issues arise after migrating to edge:

1. Redeploy the iFlow to the cloud runtime (it remains in the design workspace).
2. Update DNS entries to point back to the cloud endpoint.
3. Undeploy from the edge runtime.
4. Investigate and resolve edge-specific issues before retrying.

No data is lost during rollback since the design artifacts remain in the cloud tenant.

---

## Best Practices

### Design

- **Choose edge for data sensitivity and low latency.** If an iFlow processes PII, financial records, or data subject to regulatory mandates, deploy it to the edge runtime to keep payloads within your network.
- **Keep cloud for internet-facing integrations.** iFlows that interact with SaaS applications (Salesforce, Ariba, SuccessFactors) or public APIs are better suited for the cloud runtime -- they benefit from SAP-managed connectivity and cloud-only adapters.
- **Design for runtime portability.** Avoid hardcoding runtime-specific assumptions. Use externalized parameters so the same iFlow can be deployed to either runtime with minimal changes.
- **Validate adapter compatibility early.** Before designing an iFlow intended for edge, confirm all required adapters are supported on the edge runtime.

### Operations

- **Monitor health continuously.** Deploy Prometheus + Grafana alongside the edge cluster. Configure alerts for pod failures, high CPU/memory, and cloud connection loss.
- **Automate scaling.** Use HPA to scale edge runtime pods based on CPU, memory, and queue depth. Set conservative scale-down policies to avoid thrashing.
- **Test failover regularly.** Simulate node failures monthly by cordoning a node (`kubectl cordon <node>`) and verifying workloads reschedule correctly.
- **Plan maintenance windows.** Coordinate Kubernetes upgrades and Helm chart updates with SAP release notes. Use `helm upgrade --atomic` to auto-rollback on failure.
- **Keep Helm charts versioned.** Pin Helm chart versions in CI/CD pipelines. Test upgrades in a staging edge cluster before applying to production.

### Security

- **Rotate certificates proactively.** Monitor certificate expiry dates in the cloud keystore. Set alerts for certificates expiring within 30 days. Use cert-manager for automatic renewal of Ingress TLS certificates.
- **Restrict Kubernetes RBAC.** Apply the principle of least privilege. Edge Integration Cell service accounts should only have permissions within the `edge-integration-cell` namespace.
- **Encrypt persistent volume data at rest.** Enable encryption on the underlying storage (EBS encryption, Azure Disk encryption, GCE PD encryption with CMEK).
- **Enable Istio mTLS in STRICT mode.** All pod-to-pod communication within the edge namespace must be encrypted via Istio mutual TLS.
- **Audit access.** Enable Kubernetes audit logging. Review who accessed the edge namespace and what changes were made.

### Network

- **Ensure stable outbound connectivity to BTP.** The Edge LM Bridge and keystore sync require continuous HTTPS connectivity to SAP BTP. Use redundant internet links or dedicated circuits for critical edge deployments.
- **Use a dedicated Ingress controller for edge.** Separate the edge Ingress from other workloads to isolate traffic and simplify TLS certificate management.
- **Whitelist SAP endpoints from TLS inspection.** Corporate firewalls that perform TLS inspection (man-in-the-middle) will break the Edge LM Bridge tunnel. Whitelist `*.edge.integration.cloud.sap` and `*.hana.ondemand.com`.
- **Implement network policies.** Use Kubernetes NetworkPolicy resources to restrict traffic flow to/from edge pods, limiting the blast radius of any compromise.

```yaml
# Example: Restrict edge-runtime to only accept traffic from Ingress
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: edge-runtime-ingress-only
  namespace: edge-integration-cell
spec:
  podSelector:
    matchLabels:
      app: edge-runtime
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              app: ingress-nginx
      ports:
        - protocol: TCP
          port: 8080
```

---

## Next Steps

- **[Cloud Integration Reference](cloud-integration.md):** iFlow design patterns, process steps, and development best practices that apply to both cloud and edge runtimes.
- **[Cloud Connector Reference](cloud-connector.md):** If your edge-deployed iFlows need to reach SAP on-premise systems through a secure tunnel (alternative to direct network access).
- **[Security Reference](security.md):** Authentication, authorization, certificate management, and encryption patterns for SAP Integration Suite.
- **[Operations and Monitoring Reference](operations-monitoring.md):** Monitoring strategies, alerting, and operational procedures for Integration Suite runtimes.
- **[Troubleshooting Reference](troubleshooting.md):** Error resolution patterns, diagnostic approaches, and common issues across all Integration Suite components.
- **[Adapters Reference](adapters.md):** Detailed configuration for each adapter type, including edge-specific considerations.
- **[Content Transport Reference](content-transport.md):** Transporting integration content across landscapes (Dev -> QA -> Prod) including edge deployments.

### For Deeper Reading

**Use `notebooklm-mcp` → notebook `01493684-2483-49a6-97e8-9b4727a10954`** to query about Edge Integration Cell architecture, deployment, Kubernetes configuration, and hybrid integration patterns.
