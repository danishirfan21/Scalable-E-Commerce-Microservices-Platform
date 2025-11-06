# AWS ECS Deployment Guide

Complete guide for deploying the E-Commerce Microservices Platform to AWS using Elastic Container Service (ECS) with Fargate.

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
# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verify installation
aws --version

# Configure AWS credentials
aws configure
```

### Required AWS Services

- **ECS** (Elastic Container Service) - Container orchestration
- **ECR** (Elastic Container Registry) - Docker image repository
- **RDS** (Relational Database Service) - PostgreSQL databases
- **ALB** (Application Load Balancer) - Load balancing
- **VPC** (Virtual Private Cloud) - Network isolation
- **CloudWatch** - Logging and monitoring
- **IAM** - Access management
- **Secrets Manager** - Secret storage

### Estimated Costs

| Service | Monthly Cost (Estimate) |
|---------|------------------------|
| ECS Fargate (7 services) | $150-200 |
| RDS PostgreSQL (3 instances) | $90-120 |
| ALB | $20-30 |
| NAT Gateway | $35-45 |
| CloudWatch Logs | $10-20 |
| **Total** | **$305-415/month** |

---

## Architecture Overview

### AWS Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                            INTERNET                                  │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │    Route 53 (DNS)       │
                    └────────────┬────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────┐
│                           AWS CLOUD                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    VPC (10.0.0.0/16)                         │  │
│  │                                                              │  │
│  │  ┌────────────────────────────────────────────────────────┐ │  │
│  │  │         Application Load Balancer (Public)             │ │  │
│  │  │              Port 80/443 → Target Groups               │ │  │
│  │  └──────────────────────┬─────────────────────────────────┘ │  │
│  │                         │                                    │  │
│  │  ┌──────────────────────┴─────────────────────────────────┐ │  │
│  │  │              Private Subnet 1 (10.0.1.0/24)            │ │  │
│  │  │                                                        │ │  │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │ │  │
│  │  │  │ Config Server│  │Eureka Server │  │ API Gateway  │ │ │  │
│  │  │  │  ECS Task    │  │  ECS Task    │  │  ECS Task    │ │ │  │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘ │ │  │
│  │  │                                                        │ │  │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │ │  │
│  │  │  │ User Service │  │Product Service│  │Order Service │ │ │  │
│  │  │  │  ECS Task    │  │  ECS Task    │  │  ECS Task    │ │ │  │
│  │  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │ │  │
│  │  └─────────┼──────────────────┼──────────────────┼────────┘ │  │
│  │            │                  │                  │          │  │
│  │            ▼                  ▼                  ▼          │  │
│  │  ┌────────────────────────────────────────────────────────┐ │  │
│  │  │           Private Subnet 2 (10.0.2.0/24) - Database    │ │  │
│  │  │                                                        │ │  │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │ │  │
│  │  │  │ RDS Postgres │  │ RDS Postgres │  │ RDS Postgres │ │ │  │
│  │  │  │   (userdb)   │  │ (productdb)  │  │  (orderdb)   │ │ │  │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘ │ │  │
│  │  └────────────────────────────────────────────────────────┘ │  │
│  │                                                              │  │
│  │  ┌────────────────────────────────────────────────────────┐ │  │
│  │  │                   NAT Gateway                          │ │  │
│  │  └────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    Supporting Services                        │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │  • ECR (Container Registry)                                  │  │
│  │  • CloudWatch (Logs & Metrics)                               │  │
│  │  • Secrets Manager (Credentials)                             │  │
│  │  • IAM (Access Control)                                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Deployment

### Step 1: Create VPC and Networking

```bash
# Create VPC
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=ecommerce-vpc}]'

# Save VPC ID
export VPC_ID=<vpc-id-from-output>

# Create Internet Gateway
aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=ecommerce-igw}]'

export IGW_ID=<igw-id-from-output>

# Attach Internet Gateway to VPC
aws ec2 attach-internet-gateway \
  --internet-gateway-id $IGW_ID \
  --vpc-id $VPC_ID

# Create Public Subnets (for ALB)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.0.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=ecommerce-public-1a}]'

aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.3.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=ecommerce-public-1b}]'

export PUBLIC_SUBNET_1=<subnet-id-1>
export PUBLIC_SUBNET_2=<subnet-id-2>

# Create Private Subnets (for ECS tasks)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=ecommerce-private-app-1a}]'

aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.4.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=ecommerce-private-app-1b}]'

export PRIVATE_SUBNET_APP_1=<subnet-id-1>
export PRIVATE_SUBNET_APP_2=<subnet-id-2>

# Create Private Subnets (for RDS)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=ecommerce-private-db-1a}]'

aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.5.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=ecommerce-private-db-1b}]'

export PRIVATE_SUBNET_DB_1=<subnet-id-1>
export PRIVATE_SUBNET_DB_2=<subnet-id-2>

# Create NAT Gateway (for outbound internet from private subnets)
# Allocate Elastic IP
aws ec2 allocate-address --domain vpc

export EIP_ALLOC_ID=<allocation-id>

# Create NAT Gateway in public subnet
aws ec2 create-nat-gateway \
  --subnet-id $PUBLIC_SUBNET_1 \
  --allocation-id $EIP_ALLOC_ID \
  --tag-specifications 'ResourceType=natgateway,Tags=[{Key=Name,Value=ecommerce-nat}]'

export NAT_GW_ID=<nat-gateway-id>

# Create Route Tables
# Public route table
aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=ecommerce-public-rt}]'

export PUBLIC_RT_ID=<route-table-id>

# Add route to internet gateway
aws ec2 create-route \
  --route-table-id $PUBLIC_RT_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID

# Associate public subnets
aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_1 --route-table-id $PUBLIC_RT_ID
aws ec2 associate-route-table --subnet-id $PUBLIC_SUBNET_2 --route-table-id $PUBLIC_RT_ID

# Private route table
aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=ecommerce-private-rt}]'

export PRIVATE_RT_ID=<route-table-id>

# Add route to NAT gateway
aws ec2 create-route \
  --route-table-id $PRIVATE_RT_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --nat-gateway-id $NAT_GW_ID

# Associate private subnets
aws ec2 associate-route-table --subnet-id $PRIVATE_SUBNET_APP_1 --route-table-id $PRIVATE_RT_ID
aws ec2 associate-route-table --subnet-id $PRIVATE_SUBNET_APP_2 --route-table-id $PRIVATE_RT_ID
aws ec2 associate-route-table --subnet-id $PRIVATE_SUBNET_DB_1 --route-table-id $PRIVATE_RT_ID
aws ec2 associate-route-table --subnet-id $PRIVATE_SUBNET_DB_2 --route-table-id $PRIVATE_RT_ID
```

### Step 2: Create Security Groups

```bash
# ALB Security Group
aws ec2 create-security-group \
  --group-name ecommerce-alb-sg \
  --description "Security group for Application Load Balancer" \
  --vpc-id $VPC_ID

export ALB_SG_ID=<security-group-id>

# Allow HTTP and HTTPS
aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG_ID \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# ECS Tasks Security Group
aws ec2 create-security-group \
  --group-name ecommerce-ecs-sg \
  --description "Security group for ECS tasks" \
  --vpc-id $VPC_ID

export ECS_SG_ID=<security-group-id>

# Allow traffic from ALB
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG_ID \
  --protocol tcp \
  --port 8080-8083 \
  --source-group $ALB_SG_ID

# Allow inter-service communication
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG_ID \
  --protocol tcp \
  --port 0-65535 \
  --source-group $ECS_SG_ID

# RDS Security Group
aws ec2 create-security-group \
  --group-name ecommerce-rds-sg \
  --description "Security group for RDS databases" \
  --vpc-id $VPC_ID

export RDS_SG_ID=<security-group-id>

# Allow PostgreSQL from ECS tasks
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG_ID \
  --protocol tcp \
  --port 5432 \
  --source-group $ECS_SG_ID
```

### Step 3: Create RDS Databases

```bash
# Create DB Subnet Group
aws rds create-db-subnet-group \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --db-subnet-group-description "Subnet group for ecommerce databases" \
  --subnet-ids $PRIVATE_SUBNET_DB_1 $PRIVATE_SUBNET_DB_2

# Create User Database
aws rds create-db-instance \
  --db-instance-identifier ecommerce-userdb \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --vpc-security-group-ids $RDS_SG_ID \
  --db-name userdb \
  --backup-retention-period 7 \
  --no-publicly-accessible

# Create Product Database
aws rds create-db-instance \
  --db-instance-identifier ecommerce-productdb \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --vpc-security-group-ids $RDS_SG_ID \
  --db-name productdb \
  --backup-retention-period 7 \
  --no-publicly-accessible

# Create Order Database
aws rds create-db-instance \
  --db-instance-identifier ecommerce-orderdb \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --vpc-security-group-ids $RDS_SG_ID \
  --db-name orderdb \
  --backup-retention-period 7 \
  --no-publicly-accessible

# Wait for databases to be available (10-15 minutes)
aws rds wait db-instance-available --db-instance-identifier ecommerce-userdb
aws rds wait db-instance-available --db-instance-identifier ecommerce-productdb
aws rds wait db-instance-available --db-instance-identifier ecommerce-orderdb

# Get database endpoints
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-userdb \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

# Save endpoints for later use
export USERDB_ENDPOINT=<endpoint>
export PRODUCTDB_ENDPOINT=<endpoint>
export ORDERDB_ENDPOINT=<endpoint>
```

### Step 4: Create ECR Repositories

```bash
# Create repositories for each service
aws ecr create-repository --repository-name ecommerce/config-server
aws ecr create-repository --repository-name ecommerce/eureka-server
aws ecr create-repository --repository-name ecommerce/api-gateway
aws ecr create-repository --repository-name ecommerce/user-service
aws ecr create-repository --repository-name ecommerce/product-service
aws ecr create-repository --repository-name ecommerce/order-service
aws ecr create-repository --repository-name ecommerce/frontend

# Get ECR login command
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

### Step 5: Build and Push Docker Images

```bash
# Get AWS account ID
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=us-east-1
export ECR_REGISTRY=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Build and push each service
services=("config-server" "eureka-server" "api-gateway" "user-service" "product-service" "order-service")

for service in "${services[@]}"; do
  echo "Building and pushing $service..."
  cd backend/$service
  docker build -t ecommerce/$service:latest .
  docker tag ecommerce/$service:latest $ECR_REGISTRY/ecommerce/$service:latest
  docker push $ECR_REGISTRY/ecommerce/$service:latest
  cd ../..
done

# Build and push frontend
cd frontend
docker build -t ecommerce/frontend:latest .
docker tag ecommerce/frontend:latest $ECR_REGISTRY/ecommerce/frontend:latest
docker push $ECR_REGISTRY/ecommerce/frontend:latest
cd ..
```

### Step 6: Create ECS Cluster

```bash
# Create ECS cluster
aws ecs create-cluster \
  --cluster-name ecommerce-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE,weight=1 \
    capacityProvider=FARGATE_SPOT,weight=4

# Create CloudWatch log group
aws logs create-log-group --log-group-name /ecs/ecommerce
```

### Step 7: Create IAM Roles

```bash
# Create ECS task execution role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-trust-policy.json

# Attach policies
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess

# Create ecs-trust-policy.json
cat > ecs-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
```

### Step 8: Create Task Definitions

Create task definition files for each service. Example for User Service:

```json
{
  "family": "user-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "user-service",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce/user-service:latest",
      "portMappings": [
        {
          "containerPort": 8081,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "CONFIG_SERVER",
          "value": "config-server.ecommerce.local"
        },
        {
          "name": "EUREKA_SERVER",
          "value": "eureka-server.ecommerce.local"
        },
        {
          "name": "DB_HOST",
          "value": "<userdb-endpoint>"
        },
        {
          "name": "DB_USER",
          "value": "postgres"
        },
        {
          "name": "DB_PASSWORD",
          "value": "YourSecurePassword123!"
        },
        {
          "name": "JWT_SECRET",
          "value": "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ecommerce",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "user-service"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "wget --spider -q http://localhost:8081/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

Register task definitions:

```bash
aws ecs register-task-definition --cli-input-json file://user-service-task-def.json
aws ecs register-task-definition --cli-input-json file://product-service-task-def.json
aws ecs register-task-definition --cli-input-json file://order-service-task-def.json
# ... repeat for all services
```

### Step 9: Create Application Load Balancer

```bash
# Create ALB
aws elbv2 create-load-balancer \
  --name ecommerce-alb \
  --subnets $PUBLIC_SUBNET_1 $PUBLIC_SUBNET_2 \
  --security-groups $ALB_SG_ID \
  --scheme internet-facing \
  --type application

export ALB_ARN=<load-balancer-arn>

# Create target groups for each service
aws elbv2 create-target-group \
  --name user-service-tg \
  --protocol HTTP \
  --port 8081 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /actuator/health

export USER_SERVICE_TG_ARN=<target-group-arn>

# Repeat for other services...

# Create listener
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$API_GATEWAY_TG_ARN

# Create path-based routing rules
aws elbv2 create-rule \
  --listener-arn $LISTENER_ARN \
  --conditions Field=path-pattern,Values='/api/users/*' \
  --priority 10 \
  --actions Type=forward,TargetGroupArn=$USER_SERVICE_TG_ARN
```

### Step 10: Create ECS Services

```bash
# Create service for each microservice
aws ecs create-service \
  --cluster ecommerce-cluster \
  --service-name user-service \
  --task-definition user-service:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$PRIVATE_SUBNET_APP_1,$PRIVATE_SUBNET_APP_2],securityGroups=[$ECS_SG_ID]}" \
  --load-balancers targetGroupArn=$USER_SERVICE_TG_ARN,containerName=user-service,containerPort=8081 \
  --health-check-grace-period-seconds 60

# Repeat for all services
```

### Step 11: Configure Auto Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/ecommerce-cluster/user-service \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/ecommerce-cluster/user-service \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json

# scaling-policy.json
cat > scaling-policy.json <<EOF
{
  "TargetValue": 70.0,
  "PredefinedMetricSpecification": {
    "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
  },
  "ScaleInCooldown": 300,
  "ScaleOutCooldown": 60
}
EOF
```

---

## Configuration

### Environment Variables

Use AWS Secrets Manager for sensitive data:

```bash
# Store database password
aws secretsmanager create-secret \
  --name ecommerce/database/password \
  --secret-string "YourSecurePassword123!"

# Store JWT secret
aws secretsmanager create-secret \
  --name ecommerce/jwt/secret \
  --secret-string "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
```

Update task definitions to use secrets:

```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:ecommerce/database/password"
  },
  {
    "name": "JWT_SECRET",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:ecommerce/jwt/secret"
  }
]
```

---

## Monitoring and Logging

### CloudWatch Dashboards

Create a comprehensive dashboard:

```bash
aws cloudwatch put-dashboard \
  --dashboard-name ecommerce-dashboard \
  --dashboard-body file://dashboard.json
```

### CloudWatch Alarms

```bash
# CPU utilization alarm
aws cloudwatch put-metric-alarm \
  --alarm-name user-service-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=ServiceName,Value=user-service Name=ClusterName,Value=ecommerce-cluster
```

---

## Scaling

### Horizontal Scaling

- Configured via Application Auto Scaling
- Based on CPU/Memory metrics
- Min: 2 tasks, Max: 10 tasks per service

### Vertical Scaling

Update task definitions with higher CPU/memory:

```bash
# Update to larger instance
aws ecs register-task-definition \
  --family user-service \
  --cpu 1024 \
  --memory 2048 \
  # ... rest of definition
```

---

## Cost Optimization

1. **Use Fargate Spot** for non-critical workloads (50-70% savings)
2. **Right-size tasks** based on actual usage
3. **Use Application Auto Scaling** to scale down during off-peak
4. **Enable RDS storage autoscaling**
5. **Use Reserved Capacity** for predictable workloads

---

## Troubleshooting

### Common Issues

**Services won't start:**
```bash
# Check task logs
aws logs tail /ecs/ecommerce --follow

# Check task status
aws ecs describe-tasks \
  --cluster ecommerce-cluster \
  --tasks <task-arn>
```

**Database connection errors:**
```bash
# Verify security group rules
aws ec2 describe-security-groups --group-ids $RDS_SG_ID

# Test connectivity from ECS task
aws ecs execute-command \
  --cluster ecommerce-cluster \
  --task <task-id> \
  --container user-service \
  --interactive \
  --command "pg_isready -h $USERDB_ENDPOINT"
```

**High latency:**
- Check ALB target health
- Review CloudWatch metrics
- Enable X-Ray tracing

---

## Cleanup

To destroy all resources:

```bash
# Delete ECS services
aws ecs delete-service --cluster ecommerce-cluster --service user-service --force
# ... repeat for all services

# Delete ECS cluster
aws ecs delete-cluster --cluster ecommerce-cluster

# Delete RDS instances
aws rds delete-db-instance --db-instance-identifier ecommerce-userdb --skip-final-snapshot
# ... repeat for all databases

# Delete ALB
aws elbv2 delete-load-balancer --load-balancer-arn $ALB_ARN

# Delete NAT Gateway
aws ec2 delete-nat-gateway --nat-gateway-id $NAT_GW_ID

# Delete VPC and associated resources
aws ec2 delete-vpc --vpc-id $VPC_ID
```

---

**For support, contact the DevOps team or open an issue.**
