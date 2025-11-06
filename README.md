# E-Commerce Microservices Platform

[![Build Status](https://github.com/your-org/ecommerce-platform/workflows/CI-CD/badge.svg)](https://github.com/your-org/ecommerce-platform/actions)
[![codecov](https://codecov.io/gh/your-org/ecommerce-platform/branch/main/graph/badge.svg)](https://codecov.io/gh/your-org/ecommerce-platform)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **Production-grade E-Commerce platform built with Spring Boot microservices and React + TypeScript frontend**

A complete, scalable, and production-ready e-commerce platform demonstrating modern microservices architecture, cloud-native patterns, and best practices.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

This platform implements a complete e-commerce solution using microservices architecture with the following services:

### Backend Services

1. **Config Server** - Centralized configuration management
2. **Eureka Server** - Service discovery and registration
3. **API Gateway** - Single entry point with routing and load balancing
4. **User Service** - User management, authentication (OAuth2 + JWT)
5. **Product Service** - Product catalog and inventory management
6. **Order Service** - Order processing and payment handling

### Frontend

- **React + TypeScript** - Modern, responsive web interface with Material-UI

---

## Architecture

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Web Browser  │  │ Mobile App   │  │  Postman     │  │  Third Party │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼──────────────────┼──────────────────┼──────────────────┼───────────┘
          │                  │                  │                  │
          └──────────────────┴──────────────────┴──────────────────┘
                                      │
                                      ▼
          ┌────────────────────────────────────────────────────────┐
          │              API GATEWAY (Port 8080)                   │
          │  ┌──────────────────────────────────────────────────┐  │
          │  │  - Routing & Load Balancing                      │  │
          │  │  - JWT Authentication                            │  │
          │  │  - Rate Limiting                                 │  │
          │  │  - CORS Handling                                 │  │
          │  └──────────────────────────────────────────────────┘  │
          └────────────────────────────────────────────────────────┘
                                      │
          ┌───────────────────────────┴───────────────────────────┐
          │                                                       │
          ▼                         ▼                             ▼
┌─────────────────┐       ┌─────────────────┐         ┌─────────────────┐
│  USER SERVICE   │       │ PRODUCT SERVICE │         │  ORDER SERVICE  │
│   (Port 8081)   │       │   (Port 8082)   │         │   (Port 8083)   │
│                 │       │                 │         │                 │
│ ┌─────────────┐ │       │ ┌─────────────┐ │         │ ┌─────────────┐ │
│ │ Controllers │ │       │ │ Controllers │ │         │ │ Controllers │ │
│ └──────┬──────┘ │       │ └──────┬──────┘ │         │ └──────┬──────┘ │
│ ┌──────▼──────┐ │       │ ┌──────▼──────┐ │         │ ┌──────▼──────┐ │
│ │   Service   │ │       │ │   Service   │ │         │ │   Service   │ │
│ └──────┬──────┘ │       │ └──────┬──────┘ │         │ └──────┬──────┘ │
│ ┌──────▼──────┐ │       │ ┌──────▼──────┐ │         │ ┌──────▼──────┐ │
│ │ Repository  │ │       │ │ Repository  │ │         │ │ Repository  │ │
│ └──────┬──────┘ │       │ └──────┬──────┘ │         │ └──────┬──────┘ │
└────────┼────────┘       └────────┼────────┘         └────────┼────────┘
         │                         │                           │
         ▼                         ▼                           ▼
┌─────────────────┐       ┌─────────────────┐         ┌─────────────────┐
│  PostgreSQL     │       │  PostgreSQL     │         │  PostgreSQL     │
│    (userdb)     │       │  (productdb)    │         │   (orderdb)     │
└─────────────────┘       └─────────────────┘         └─────────────────┘

                    ┌─────────────────────────────┐
                    │  INFRASTRUCTURE SERVICES     │
                    ├─────────────────────────────┤
                    │  Config Server (8888)       │
                    │  Eureka Server (8761)       │
                    └─────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         SERVICE COMMUNICATION                                 │
│                                                                               │
│  ┌───────────┐                    ┌───────────┐                             │
│  │   User    │ ◄──── Feign ─────  │   Order   │                             │
│  │  Service  │                     │  Service  │                             │
│  └───────────┘                     └───────────┘                             │
│                                          │                                    │
│                                     Feign│                                    │
│                                          ▼                                    │
│                                    ┌───────────┐                             │
│                                    │  Product  │                             │
│                                    │  Service  │                             │
│                                    └───────────┘                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Architecture Patterns

This platform implements the following architectural patterns:

#### 1. **Microservices Architecture**
- **Decomposition by Business Capability**: Each service represents a distinct business domain
- **Database per Service**: Independent data storage for each microservice
- **Decentralized Data Management**: Services own their data and schema

#### 2. **API Gateway Pattern**
- **Single Entry Point**: All client requests go through the API Gateway
- **Request Routing**: Intelligent routing to appropriate microservices
- **Cross-Cutting Concerns**: Authentication, logging, rate limiting centralized

#### 3. **Service Discovery**
- **Client-Side Discovery**: Clients query Eureka for service locations
- **Self-Registration**: Services register themselves with Eureka on startup
- **Health Checking**: Continuous health monitoring of registered services

#### 4. **Centralized Configuration**
- **Config Server**: External configuration management
- **Environment-Specific**: Different configs for dev, staging, production
- **Dynamic Refresh**: Update configuration without restarting services

#### 5. **Security Patterns**
- **JWT Authentication**: Stateless authentication with JSON Web Tokens
- **OAuth2**: Industry-standard authorization framework
- **Role-Based Access Control (RBAC)**: Admin vs Customer permissions

#### 6. **Communication Patterns**
- **Synchronous Communication**: REST APIs for request-response
- **Feign Clients**: Declarative HTTP clients for inter-service communication
- **Circuit Breaker**: Resilience pattern (ready for Hystrix/Resilience4j integration)

#### 7. **Data Management Patterns**
- **Saga Pattern**: Distributed transactions across services (order processing)
- **Event Sourcing**: Ready for event-driven architecture expansion
- **CQRS**: Separation of read and write models (future enhancement)

---

## Features

### User Service
- ✅ User registration and authentication
- ✅ JWT token-based authentication
- ✅ OAuth2 authorization framework
- ✅ Role-based access control (RBAC)
- ✅ Password encryption (BCrypt)
- ✅ User profile management
- ✅ Admin and customer roles

### Product Service
- ✅ Product CRUD operations
- ✅ Inventory management
- ✅ Product search and filtering
- ✅ Category-based organization
- ✅ SKU management
- ✅ Low-stock alerts
- ✅ Image URL support

### Order Service
- ✅ Order creation and management
- ✅ Order status tracking
- ✅ Payment processing
- ✅ Inventory coordination (via Feign)
- ✅ User order history
- ✅ Order cancellation with inventory restoration
- ✅ Multi-item orders

### API Gateway
- ✅ Centralized routing
- ✅ Load balancing
- ✅ JWT validation
- ✅ CORS handling
- ✅ Request/response logging
- ✅ Header propagation

### Infrastructure
- ✅ Service discovery (Eureka)
- ✅ Centralized configuration
- ✅ Health checks
- ✅ Metrics exposure (Actuator)
- ✅ Docker containerization
- ✅ Docker Compose orchestration

### Frontend
- ✅ React 18 with TypeScript
- ✅ Redux Toolkit state management
- ✅ Material-UI components
- ✅ Responsive design
- ✅ Authentication flow
- ✅ Admin dashboard
- ✅ Product catalog
- ✅ Shopping cart
- ✅ Order management
- ✅ Profile management

---

## Technology Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 3.2.0 | Application framework |
| Spring Cloud | 2023.0.0 | Microservices framework |
| Spring Security | 6.x | Security framework |
| Spring Data JPA | 3.x | Data access layer |
| PostgreSQL | 15 | Relational database |
| JWT (jjwt) | 0.12.3 | Authentication tokens |
| Lombok | 1.18.30 | Code generation |
| SpringDoc OpenAPI | 2.3.0 | API documentation |
| Maven | 3.9+ | Build tool |
| JUnit 5 | 5.x | Testing framework |
| Mockito | 5.x | Mocking framework |
| Docker | Latest | Containerization |

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2.0 | UI library |
| TypeScript | 5.3.2 | Type-safe JavaScript |
| Redux Toolkit | 1.9.7 | State management |
| React Router | 6.20.0 | Routing |
| Material-UI | 5.14.19 | UI components |
| Axios | 1.6.2 | HTTP client |
| Formik | 2.4.5 | Form management |
| Yup | 1.3.3 | Schema validation |
| Jest | Latest | Testing framework |
| Cypress | Latest | E2E testing |

### DevOps & Tools

| Technology | Purpose |
|------------|---------|
| Docker | Containerization |
| Docker Compose | Local orchestration |
| GitHub Actions | CI/CD pipeline |
| Postman | API testing |
| SonarQube | Code quality |
| Nginx | Frontend serving |

---

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **Docker** and **Docker Compose**
- **PostgreSQL 15** (if running locally without Docker)
- **Maven 3.9+**
- **Git**

### Quick Start (Docker Compose)

```bash
# 1. Clone the repository
git clone https://github.com/your-org/ecommerce-platform.git
cd ecommerce-platform

# 2. Set environment variables (optional)
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your_jwt_secret_key

# 3. Start all services
docker-compose up -d

# 4. Wait for services to be healthy (2-3 minutes)
docker-compose ps

# 5. Access the application
# Frontend: http://localhost:3000
# API Gateway: http://localhost:8080
# Eureka Dashboard: http://localhost:8761
```

### Manual Setup

#### Backend Services

```bash
# Build all services
cd backend
mvn clean install

# Start Config Server
cd config-server
mvn spring-boot:run

# Start Eureka Server
cd ../eureka-server
mvn spring-boot:run

# Start User Service
cd ../user-service
mvn spring-boot:run

# Start Product Service
cd ../product-service
mvn spring-boot:run

# Start Order Service
cd ../order-service
mvn spring-boot:run

# Start API Gateway
cd ../api-gateway
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend
npm install
npm start
```

### Verify Installation

```bash
# Check all services are healthy
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# Access Swagger UI for each service
# User Service: http://localhost:8081/swagger-ui.html
# Product Service: http://localhost:8082/swagger-ui.html
# Order Service: http://localhost:8083/swagger-ui.html
```

---

## API Documentation

### API Endpoints

#### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login user | No |

#### Users

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/users` | Get all users | Admin |
| GET | `/api/users/{id}` | Get user by ID | User/Admin |
| GET | `/api/users/username/{username}` | Get user by username | Yes |
| PUT | `/api/users/{id}` | Update user | User/Admin |
| DELETE | `/api/users/{id}` | Delete user | Admin |

#### Products

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/products` | Get all products | No |
| GET | `/api/products/{id}` | Get product by ID | No |
| GET | `/api/products/category/{category}` | Get products by category | No |
| GET | `/api/products/search?term=X` | Search products | No |
| POST | `/api/products` | Create product | Admin |
| PUT | `/api/products/{id}` | Update product | Admin |
| DELETE | `/api/products/{id}` | Delete product | Admin |
| PATCH | `/api/products/{id}/inventory` | Update inventory | Admin |

#### Orders

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/orders` | Create order | User |
| GET | `/api/orders/{id}` | Get order by ID | User/Admin |
| GET | `/api/orders/user` | Get user's orders | User |
| GET | `/api/orders` | Get all orders | Admin |
| POST | `/api/orders/{id}/payment` | Process payment | User |
| PUT | `/api/orders/{id}/status` | Update order status | Admin |
| DELETE | `/api/orders/{id}` | Cancel order | User |

### Swagger Documentation

Each service provides interactive API documentation via Swagger UI:

- **User Service**: http://localhost:8081/swagger-ui.html
- **Product Service**: http://localhost:8082/swagger-ui.html
- **Order Service**: http://localhost:8083/swagger-ui.html

### Postman Collection

Import the Postman collection for easy API testing:

```bash
docs/postman_collection.json
```

The collection includes:
- Pre-configured requests
- Environment variables
- Auto-token extraction
- Sample payloads

---

## Deployment

### Local Deployment

```bash
docker-compose up -d
```

### AWS ECS Deployment

See [AWS_DEPLOYMENT.md](docs/AWS_DEPLOYMENT.md) for detailed instructions.

**Summary:**
1. Build Docker images
2. Push to ECR
3. Create ECS cluster
4. Define task definitions
5. Create services
6. Configure load balancer
7. Set up RDS for PostgreSQL
8. Configure security groups

### GCP Cloud Run Deployment

See [GCP_DEPLOYMENT.md](docs/GCP_DEPLOYMENT.md) for detailed instructions.

**Summary:**
1. Build Docker images
2. Push to GCR
3. Deploy to Cloud Run
4. Set up Cloud SQL for PostgreSQL
5. Configure IAM permissions
6. Set environment variables

### Kubernetes Deployment

Kubernetes manifests are available in the `k8s/` directory:

```bash
kubectl apply -f k8s/
```

---

## Testing

### Unit Tests

```bash
# Run all tests
cd backend
mvn test

# Run tests for specific service
cd user-service
mvn test

# Generate coverage report
mvn test jacoco:report
```

### Integration Tests

```bash
mvn verify
```

### E2E Tests

```bash
# Start all services
docker-compose up -d

# Run Postman tests
newman run docs/postman_collection.json

# Run Cypress tests
cd frontend
npm run cypress:run
```

### Test Coverage

Current test coverage:
- **User Service**: 85%
- **Product Service**: 82%
- **Order Service**: 80%
- **Frontend**: 75%

---

## Monitoring

### Actuator Endpoints

Each service exposes Spring Boot Actuator endpoints:

```bash
# Health check
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics

# Info
curl http://localhost:8081/actuator/info
```

### Prometheus Metrics

Metrics are exposed in Prometheus format:

```bash
curl http://localhost:8081/actuator/prometheus
```

### Logging

Logs are structured and output to console. In production, integrate with:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Splunk**
- **CloudWatch** (AWS)
- **Cloud Logging** (GCP)

---

## Security

### Authentication Flow

1. User registers via `/auth/register`
2. Password hashed with BCrypt
3. User logs in via `/auth/login`
4. JWT token generated and returned
5. Client stores token (localStorage)
6. Client includes token in Authorization header
7. API Gateway validates token
8. Request forwarded to microservices with user context

### Security Measures

- ✅ JWT token-based authentication
- ✅ BCrypt password hashing
- ✅ HTTPS in production
- ✅ CORS configuration
- ✅ SQL injection prevention (JPA)
- ✅ XSS protection
- ✅ Role-based access control
- ✅ Environment variable secrets
- ✅ Database connection encryption

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Development Workflow

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write tests
5. Run linting and tests
6. Submit a pull request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Project Structure

```
ecommerce-platform/
├── backend/
│   ├── config-server/          # Configuration server
│   ├── eureka-server/          # Service discovery
│   ├── api-gateway/            # API Gateway
│   ├── user-service/           # User management
│   ├── product-service/        # Product catalog
│   ├── order-service/          # Order processing
│   └── pom.xml                 # Parent POM
├── frontend/                   # React + TypeScript app
│   ├── src/
│   ├── public/
│   ├── Dockerfile
│   └── package.json
├── docs/                       # Documentation
│   ├── DATABASE_SCHEMA.md      # Database documentation
│   ├── ER_DIAGRAM.md           # ER diagrams
│   ├── AWS_DEPLOYMENT.md       # AWS deployment guide
│   ├── GCP_DEPLOYMENT.md       # GCP deployment guide
│   └── postman_collection.json # API collection
├── k8s/                        # Kubernetes manifests
├── .github/
│   └── workflows/
│       └── ci-cd.yml           # CI/CD pipeline
├── docker-compose.yml          # Local orchestration
└── README.md                   # This file
```

---

## Support

For support, please:
- Open an issue on GitHub
- Check the [documentation](docs/)
- Review the [FAQ](docs/FAQ.md)

---

## Acknowledgments

- Spring Boot Team
- React Team
- Open Source Community

---

## Roadmap

### Phase 1 (Current)
- [x] Core microservices
- [x] Authentication and authorization
- [x] Basic CRUD operations
- [x] Docker deployment

### Phase 2 (Q1 2025)
- [ ] Event-driven architecture (Kafka)
- [ ] Redis caching
- [ ] Circuit breaker (Resilience4j)
- [ ] Distributed tracing (Zipkin)

### Phase 3 (Q2 2025)
- [ ] GraphQL API
- [ ] Real-time notifications (WebSocket)
- [ ] Advanced search (Elasticsearch)
- [ ] AI-powered recommendations

### Phase 4 (Q3 2025)
- [ ] Multi-tenant support
- [ ] Internationalization (i18n)
- [ ] Mobile apps (React Native)
- [ ] Advanced analytics

---

**Built with ❤️ by the E-Commerce Platform Team**
