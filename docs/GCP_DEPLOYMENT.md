# GCP Cloud Run Deployment Guide

Complete guide for deploying the E-Commerce Microservices Platform to Google Cloud Platform using Cloud Run and Cloud SQL.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Architecture Overview](#architecture-overview)
- [Step-by-Step Deployment](#step-by-step-deployment)
- [Configuration](#configuration)
- [Monitoring and Logging](#monitoring-and-logging)
- [Scaling](#scaling)
- [Cost Optimization](#cost-optimization)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools

```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Initialize gcloud
gcloud init

# Install kubectl (optional, for GKE)
gcloud components install kubectl

# Verify installation
gcloud --version
```

### Required GCP Services

- **Cloud Run** - Serverless container platform
- **Cloud SQL** - Managed PostgreSQL
- **Cloud Build** - Container image building
- **Container Registry (GCR)** or **Artifact Registry** - Image storage
- **Cloud Load Balancing** - HTTP(S) load balancer
- **Cloud Logging** - Centralized logging
- **Cloud Monitoring** - Metrics and alerting
- **Secret Manager** - Secret storage
- **VPC** - Network isolation

### Estimated Costs

| Service | Monthly Cost (Estimate) |
|---------|------------------------|
| Cloud Run (7 services) | $50-100 |
| Cloud SQL PostgreSQL (3 instances) | $80-120 |
| Cloud Load Balancer | $20-30 |
| Cloud NAT | $35-45 |
| Cloud Logging | $10-20 |
| **Total** | **$195-315/month** |

*Costs are lower than AWS due to Cloud Run's request-based pricing*

---

## Architecture Overview

### GCP Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                            INTERNET                                  │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │   Cloud DNS (Optional)  │
                    └────────────┬────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────┐
│                        GOOGLE CLOUD PLATFORM                        │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │         HTTPS Load Balancer (Global, Serverless NEG)        │  │
│  │              SSL Certificate, CDN-enabled                    │  │
│  └──────────────────────┬───────────────────────────────────────┘  │
│                         │                                          │
│  ┌──────────────────────┴───────────────────────────────────────┐  │
│  │                    Cloud Run Services                        │  │
│  │              (Fully Managed, Auto-scaling)                   │  │
│  │                                                              │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │  │
│  │  │Config Service│  │Eureka Service│  │  API Gateway │      │  │
│  │  │  (0-N pods)  │  │  (0-N pods)  │  │  (0-N pods)  │      │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘      │  │
│  │                                                              │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │  │
│  │  │ User Service │  │Product Service│  │Order Service │      │  │
│  │  │  (0-N pods)  │  │  (0-N pods)  │  │  (0-N pods)  │      │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │  │
│  └─────────┼──────────────────┼──────────────────┼─────────────┘  │
│            │                  │                  │                │
│            │                  │                  │                │
│            ▼                  ▼                  ▼                │
│  ┌────────────────────────────────────────────────────────┐      │
│  │              VPC Connector (Private IP)                │      │
│  └────────────────────┬───────────────────────────────────┘      │
│                       │                                          │
│                       ▼                                          │
│  ┌────────────────────────────────────────────────────────┐      │
│  │              Cloud SQL - Private IP                    │      │
│  │                                                        │      │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │      │
│  │  │  PostgreSQL  │  │  PostgreSQL  │  │  PostgreSQL  │ │      │
│  │  │   (userdb)   │  │ (productdb)  │  │  (orderdb)   │ │      │
│  │  │   HA Setup   │  │   HA Setup   │  │   HA Setup   │ │      │
│  │  └──────────────┘  └──────────────┘  └──────────────┘ │      │
│  └────────────────────────────────────────────────────────┘      │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │                   Supporting Services                         ││
│  ├──────────────────────────────────────────────────────────────┤│
│  │  • Container Registry (Image Storage)                        ││
│  │  • Cloud Logging (Logs & Queries)                            ││
│  │  • Cloud Monitoring (Metrics & Alerts)                       ││
│  │  • Secret Manager (Credentials)                              ││
│  │  • Cloud Build (CI/CD)                                       ││
│  │  • IAM (Access Control)                                      ││
│  └──────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Deployment

### Step 1: Set Up GCP Project

```bash
# Set project variables
export PROJECT_ID=ecommerce-platform-$(date +%s)
export REGION=us-central1
export ZONE=us-central1-a

# Create new project
gcloud projects create $PROJECT_ID --name="E-Commerce Platform"

# Set current project
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  sqladmin.googleapis.com \
  vpcaccess.googleapis.com \
  secretmanager.googleapis.com \
  cloudresourcemanager.googleapis.com \
  compute.googleapis.com \
  logging.googleapis.com \
  monitoring.googleapis.com

# Link billing account
gcloud beta billing projects link $PROJECT_ID \
  --billing-account=<YOUR_BILLING_ACCOUNT_ID>
```

### Step 2: Create VPC and Serverless VPC Connector

```bash
# Create VPC network
gcloud compute networks create ecommerce-vpc \
  --subnet-mode=custom \
  --bgp-routing-mode=regional

# Create subnet
gcloud compute networks subnets create ecommerce-subnet \
  --network=ecommerce-vpc \
  --region=$REGION \
  --range=10.0.0.0/24

# Create Serverless VPC Access connector
gcloud compute networks vpc-access connectors create ecommerce-connector \
  --region=$REGION \
  --network=ecommerce-vpc \
  --range=10.8.0.0/28 \
  --min-throughput=200 \
  --max-throughput=400

# Enable Private Google Access
gcloud compute networks subnets update ecommerce-subnet \
  --region=$REGION \
  --enable-private-ip-google-access
```

### Step 3: Create Cloud SQL Instances

```bash
# Create User Database
gcloud sql instances create ecommerce-userdb \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=$REGION \
  --network=projects/$PROJECT_ID/global/networks/ecommerce-vpc \
  --no-assign-ip \
  --root-password=YourSecurePassword123! \
  --backup-start-time=03:00 \
  --maintenance-window-day=SUN \
  --maintenance-window-hour=04

# Create database
gcloud sql databases create userdb \
  --instance=ecommerce-userdb

# Create Product Database
gcloud sql instances create ecommerce-productdb \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=$REGION \
  --network=projects/$PROJECT_ID/global/networks/ecommerce-vpc \
  --no-assign-ip \
  --root-password=YourSecurePassword123! \
  --backup-start-time=03:00

gcloud sql databases create productdb \
  --instance=ecommerce-productdb

# Create Order Database
gcloud sql instances create ecommerce-orderdb \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=$REGION \
  --network=projects/$PROJECT_ID/global/networks/ecommerce-vpc \
  --no-assign-ip \
  --root-password=YourSecurePassword123! \
  --backup-start-time=03:00

gcloud sql databases create orderdb \
  --instance=ecommerce-orderdb

# Get connection names
export USERDB_CONNECTION=$(gcloud sql instances describe ecommerce-userdb \
  --format='value(connectionName)')
export PRODUCTDB_CONNECTION=$(gcloud sql instances describe ecommerce-productdb \
  --format='value(connectionName)')
export ORDERDB_CONNECTION=$(gcloud sql instances describe ecommerce-orderdb \
  --format='value(connectionName)')
```

### Step 4: Create Secrets

```bash
# Create database password secret
echo -n "YourSecurePassword123!" | \
  gcloud secrets create db-password --data-file=-

# Create JWT secret
echo -n "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" | \
  gcloud secrets create jwt-secret --data-file=-

# Grant Cloud Run service account access to secrets
gcloud secrets add-iam-policy-binding db-password \
  --member=serviceAccount:$PROJECT_ID@appspot.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor

gcloud secrets add-iam-policy-binding jwt-secret \
  --member=serviceAccount:$PROJECT_ID@appspot.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor
```

### Step 5: Build and Push Docker Images

```bash
# Configure Docker to use gcloud as credential helper
gcloud auth configure-docker

# Build images using Cloud Build
gcloud builds submit --tag gcr.io/$PROJECT_ID/config-server backend/config-server
gcloud builds submit --tag gcr.io/$PROJECT_ID/eureka-server backend/eureka-server
gcloud builds submit --tag gcr.io/$PROJECT_ID/api-gateway backend/api-gateway
gcloud builds submit --tag gcr.io/$PROJECT_ID/user-service backend/user-service
gcloud builds submit --tag gcr.io/$PROJECT_ID/product-service backend/product-service
gcloud builds submit --tag gcr.io/$PROJECT_ID/order-service backend/order-service
gcloud builds submit --tag gcr.io/$PROJECT_ID/frontend frontend

# Alternatively, build locally and push
# docker build -t gcr.io/$PROJECT_ID/user-service backend/user-service
# docker push gcr.io/$PROJECT_ID/user-service
```

### Step 6: Deploy Config Server

```bash
gcloud run deploy config-server \
  --image gcr.io/$PROJECT_ID/config-server \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8888 \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 1 \
  --max-instances 3 \
  --timeout 300 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=native"

# Get Config Server URL
export CONFIG_SERVER_URL=$(gcloud run services describe config-server \
  --region=$REGION \
  --format='value(status.url)')
```

### Step 7: Deploy Eureka Server

```bash
gcloud run deploy eureka-server \
  --image gcr.io/$PROJECT_ID/eureka-server \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8761 \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 1 \
  --max-instances 3 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,CONFIG_SERVER=$CONFIG_SERVER_URL"

export EUREKA_SERVER_URL=$(gcloud run services describe eureka-server \
  --region=$REGION \
  --format='value(status.url)')
```

### Step 8: Deploy User Service

```bash
# Get private IP of Cloud SQL instance
export USERDB_PRIVATE_IP=$(gcloud sql instances describe ecommerce-userdb \
  --format='value(ipAddresses[0].ipAddress)')

gcloud run deploy user-service \
  --image gcr.io/$PROJECT_ID/user-service \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8081 \
  --memory 1Gi \
  --cpu 2 \
  --min-instances 0 \
  --max-instances 10 \
  --timeout 300 \
  --vpc-connector ecommerce-connector \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,\
CONFIG_SERVER=$CONFIG_SERVER_URL,\
EUREKA_SERVER=$EUREKA_SERVER_URL,\
DB_HOST=$USERDB_PRIVATE_IP,\
DB_PORT=5432,\
DB_NAME=userdb,\
DB_USER=postgres" \
  --set-secrets="DB_PASSWORD=db-password:latest,\
JWT_SECRET=jwt-secret:latest"
```

### Step 9: Deploy Product Service

```bash
export PRODUCTDB_PRIVATE_IP=$(gcloud sql instances describe ecommerce-productdb \
  --format='value(ipAddresses[0].ipAddress)')

gcloud run deploy product-service \
  --image gcr.io/$PROJECT_ID/product-service \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8082 \
  --memory 1Gi \
  --cpu 2 \
  --min-instances 0 \
  --max-instances 10 \
  --vpc-connector ecommerce-connector \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,\
CONFIG_SERVER=$CONFIG_SERVER_URL,\
EUREKA_SERVER=$EUREKA_SERVER_URL,\
DB_HOST=$PRODUCTDB_PRIVATE_IP,\
DB_PORT=5432,\
DB_NAME=productdb,\
DB_USER=postgres" \
  --set-secrets="DB_PASSWORD=db-password:latest"
```

### Step 10: Deploy Order Service

```bash
export ORDERDB_PRIVATE_IP=$(gcloud sql instances describe ecommerce-orderdb \
  --format='value(ipAddresses[0].ipAddress)')

gcloud run deploy order-service \
  --image gcr.io/$PROJECT_ID/order-service \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8083 \
  --memory 1Gi \
  --cpu 2 \
  --min-instances 0 \
  --max-instances 10 \
  --vpc-connector ecommerce-connector \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,\
CONFIG_SERVER=$CONFIG_SERVER_URL,\
EUREKA_SERVER=$EUREKA_SERVER_URL,\
DB_HOST=$ORDERDB_PRIVATE_IP,\
DB_PORT=5432,\
DB_NAME=orderdb,\
DB_USER=postgres" \
  --set-secrets="DB_PASSWORD=db-password:latest"
```

### Step 11: Deploy API Gateway

```bash
gcloud run deploy api-gateway \
  --image gcr.io/$PROJECT_ID/api-gateway \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8080 \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 1 \
  --max-instances 10 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,\
CONFIG_SERVER=$CONFIG_SERVER_URL,\
EUREKA_SERVER=$EUREKA_SERVER_URL" \
  --set-secrets="JWT_SECRET=jwt-secret:latest"

export API_GATEWAY_URL=$(gcloud run services describe api-gateway \
  --region=$REGION \
  --format='value(status.url)')
```

### Step 12: Deploy Frontend

```bash
gcloud run deploy frontend \
  --image gcr.io/$PROJECT_ID/frontend \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 80 \
  --memory 256Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 5 \
  --set-env-vars="REACT_APP_API_URL=$API_GATEWAY_URL/api"

export FRONTEND_URL=$(gcloud run services describe frontend \
  --region=$REGION \
  --format='value(status.url)')

echo "Application deployed successfully!"
echo "Frontend URL: $FRONTEND_URL"
echo "API Gateway URL: $API_GATEWAY_URL"
```

### Step 13: Configure Cloud Load Balancer (Optional)

For custom domain and SSL:

```bash
# Reserve static IP
gcloud compute addresses create ecommerce-ip \
  --network-tier=PREMIUM \
  --ip-version=IPV4 \
  --global

# Create SSL certificate (replace with your domain)
gcloud compute ssl-certificates create ecommerce-cert \
  --domains=ecommerce.example.com

# Create backend service
gcloud compute backend-services create ecommerce-backend \
  --global \
  --load-balancing-scheme=EXTERNAL \
  --protocol=HTTP

# Add Cloud Run services as backends
gcloud compute backend-services add-backend ecommerce-backend \
  --global \
  --network-endpoint-group=api-gateway-neg \
  --network-endpoint-group-region=$REGION

# Create URL map
gcloud compute url-maps create ecommerce-lb \
  --default-service ecommerce-backend

# Create HTTPS proxy
gcloud compute target-https-proxies create ecommerce-https-proxy \
  --url-map=ecommerce-lb \
  --ssl-certificates=ecommerce-cert

# Create forwarding rule
gcloud compute forwarding-rules create ecommerce-https-rule \
  --address=ecommerce-ip \
  --global \
  --target-https-proxy=ecommerce-https-proxy \
  --ports=443
```

---

## Configuration

### Environment Variables

All environment variables are set during deployment via `--set-env-vars` flag.

### Secrets Management

Sensitive data stored in Secret Manager and injected via `--set-secrets` flag.

### Update a Service

```bash
# Update environment variable
gcloud run services update user-service \
  --region=$REGION \
  --set-env-vars="NEW_VAR=value"

# Deploy new version
gcloud run deploy user-service \
  --image gcr.io/$PROJECT_ID/user-service:v2 \
  --region=$REGION
```

---

## Monitoring and Logging

### Cloud Logging

```bash
# View logs for a service
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=user-service" \
  --limit 50 \
  --format json

# Stream logs
gcloud logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=user-service"

# Create log-based metrics
gcloud logging metrics create error-rate \
  --description="Error rate for services" \
  --log-filter='resource.type="cloud_run_revision" AND severity="ERROR"'
```

### Cloud Monitoring

```bash
# Create uptime check
gcloud monitoring uptime create user-service-uptime \
  --resource-type=uptime-url \
  --url="$API_GATEWAY_URL/actuator/health"

# Create alert policy
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="High Error Rate" \
  --condition-display-name="Error rate > 5%" \
  --condition-threshold-value=5 \
  --condition-threshold-duration=300s
```

---

## Scaling

### Auto-scaling Configuration

Cloud Run automatically scales based on:
- Request concurrency (default: 80 concurrent requests per instance)
- CPU and memory utilization
- Min and max instances set during deployment

```bash
# Update scaling parameters
gcloud run services update user-service \
  --region=$REGION \
  --min-instances=2 \
  --max-instances=20 \
  --concurrency=100
```

### Cold Start Optimization

```bash
# Set minimum instances to avoid cold starts
gcloud run services update user-service \
  --region=$REGION \
  --min-instances=1

# Increase CPU during startup
gcloud run services update user-service \
  --region=$REGION \
  --cpu-boost
```

---

## Cost Optimization

### Best Practices

1. **Use Minimum Instances Wisely**
   - Set to 0 for dev/test environments
   - Set to 1-2 for production (avoid cold starts)

2. **Right-size Resources**
```bash
# Monitor actual usage
gcloud monitoring time-series list \
  --filter='metric.type="run.googleapis.com/container/cpu/utilizations"'

# Adjust CPU/memory based on metrics
gcloud run services update user-service \
  --cpu=1 \
  --memory=512Mi
```

3. **Use Request-based Pricing**
   - Pay only for requests and compute time
   - No charges when idle (with min-instances=0)

4. **Optimize Database Tier**
```bash
# Use smaller instance for low traffic
gcloud sql instances patch ecommerce-userdb \
  --tier=db-f1-micro

# Upgrade for production
gcloud sql instances patch ecommerce-userdb \
  --tier=db-n1-standard-1
```

5. **Enable Cloud CDN**
```bash
# Cache static content
gcloud compute backend-services update ecommerce-backend \
  --global \
  --enable-cdn
```

---

## Troubleshooting

### Common Issues

**Service won't start:**
```bash
# Check logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=user-service" \
  --limit 10 \
  --format json

# Check service status
gcloud run services describe user-service \
  --region=$REGION
```

**Database connection errors:**
```bash
# Verify VPC connector
gcloud compute networks vpc-access connectors describe ecommerce-connector \
  --region=$REGION

# Test Cloud SQL connection
gcloud sql connect ecommerce-userdb --user=postgres

# Check Cloud SQL logs
gcloud sql operations list --instance=ecommerce-userdb
```

**High latency:**
```bash
# Check service metrics
gcloud monitoring time-series list \
  --filter='metric.type="run.googleapis.com/request_latencies"'

# Increase CPU/memory
gcloud run services update user-service \
  --cpu=2 \
  --memory=2Gi

# Add more instances
gcloud run services update user-service \
  --min-instances=3
```

**503 Errors:**
```bash
# Check if service is overwhelmed
gcloud logging read "resource.type=cloud_run_revision AND httpRequest.status=503"

# Increase max instances
gcloud run services update user-service \
  --max-instances=20

# Increase concurrency
gcloud run services update user-service \
  --concurrency=200
```

---

## Cleanup

To destroy all resources and stop charges:

```bash
# Delete Cloud Run services
gcloud run services delete config-server --region=$REGION --quiet
gcloud run services delete eureka-server --region=$REGION --quiet
gcloud run services delete api-gateway --region=$REGION --quiet
gcloud run services delete user-service --region=$REGION --quiet
gcloud run services delete product-service --region=$REGION --quiet
gcloud run services delete order-service --region=$REGION --quiet
gcloud run services delete frontend --region=$REGION --quiet

# Delete Cloud SQL instances
gcloud sql instances delete ecommerce-userdb --quiet
gcloud sql instances delete ecommerce-productdb --quiet
gcloud sql instances delete ecommerce-orderdb --quiet

# Delete VPC connector
gcloud compute networks vpc-access connectors delete ecommerce-connector \
  --region=$REGION --quiet

# Delete VPC
gcloud compute networks subnets delete ecommerce-subnet \
  --region=$REGION --quiet
gcloud compute networks delete ecommerce-vpc --quiet

# Delete secrets
gcloud secrets delete db-password --quiet
gcloud secrets delete jwt-secret --quiet

# Delete project (WARNING: Deletes everything)
gcloud projects delete $PROJECT_ID --quiet
```

---

## Comparison: Cloud Run vs AWS ECS

| Feature | Cloud Run | AWS ECS |
|---------|-----------|---------|
| **Pricing** | Pay per request | Pay per hour (Fargate) |
| **Cold Starts** | ~1-2 seconds | N/A (always running) |
| **Min Cost** | $0 (scale to zero) | ~$150/month (min 2 tasks) |
| **Scaling** | Automatic, instant | Manual or auto-scaling |
| **Management** | Fully managed | More configuration required |
| **Networking** | Simpler VPC integration | More complex VPC setup |
| **Best For** | Variable/unpredictable traffic | Steady, predictable traffic |

---

## Security Best Practices

1. **Use Private IPs for Cloud SQL**
2. **Store secrets in Secret Manager**
3. **Enable VPC Service Controls**
4. **Use Cloud Armor for DDoS protection**
5. **Implement Cloud IAM roles properly**
6. **Enable audit logging**
7. **Use Binary Authorization for deployment**

---

**For support, contact the DevOps team or open an issue.**
