# Order Service - Architecture Documentation

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Order Service (Port 8083)                │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Controller Layer                        │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │         OrderController                       │  │   │
│  │  │  - REST API Endpoints                         │  │   │
│  │  │  - Request Validation                         │  │   │
│  │  │  - Swagger Documentation                      │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Service Layer                           │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │      OrderService (Interface)                 │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  │                     ↓                                │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │      OrderServiceImpl                         │  │   │
│  │  │  - Business Logic                             │  │   │
│  │  │  - Transaction Management                     │  │   │
│  │  │  - Validation                                 │  │   │
│  │  │  - Entity-DTO Mapping                         │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│              ↓                               ↓               │
│  ┌──────────────────────┐      ┌────────────────────────┐  │
│  │  Repository Layer     │      │   Feign Clients        │  │
│  │  ┌────────────────┐  │      │  ┌──────────────────┐ │  │
│  │  │OrderRepository │  │      │  │ ProductClient    │ │  │
│  │  └────────────────┘  │      │  └──────────────────┘ │  │
│  │  ┌────────────────┐  │      │  ┌──────────────────┐ │  │
│  │  │OrderItemRepo   │  │      │  │ UserClient       │ │  │
│  │  └────────────────┘  │      │  └──────────────────┘ │  │
│  └──────────────────────┘      └────────────────────────┘  │
│              ↓                               ↓               │
│  ┌──────────────────────┐      ┌────────────────────────┐  │
│  │   PostgreSQL DB      │      │  External Services     │  │
│  │   - orders table     │      │  - Product Service     │  │
│  │   - order_items      │      │  - User Service        │  │
│  └──────────────────────┘      └────────────────────────┘  │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Configuration & Cross-Cutting              │   │
│  │  - Security Config                                   │   │
│  │  - Feign Config                                      │   │
│  │  - OpenAPI Config                                    │   │
│  │  - Global Exception Handler                          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Layer Architecture

### 1. Controller Layer (Presentation)
**Purpose**: Handle HTTP requests and responses

**Components**:
- `OrderController.java`

**Responsibilities**:
- Receive HTTP requests
- Validate request headers (X-User-Id)
- Delegate to service layer
- Format responses
- Handle HTTP status codes
- Provide API documentation

**Technologies**:
- Spring MVC
- Spring Validation
- SpringDoc OpenAPI

---

### 2. Service Layer (Business Logic)
**Purpose**: Implement business rules and orchestrate operations

**Components**:
- `OrderService.java` (Interface)
- `OrderServiceImpl.java` (Implementation)

**Responsibilities**:
- Order creation workflow
- Payment processing
- Status validation and updates
- Order cancellation
- Coordinate with external services
- Transaction management
- Entity-DTO mapping

**Technologies**:
- Spring Framework
- Spring Transactions
- Lombok

---

### 3. Repository Layer (Data Access)
**Purpose**: Interact with the database

**Components**:
- `OrderRepository.java`
- `OrderItemRepository.java`

**Responsibilities**:
- CRUD operations
- Custom queries
- Data persistence
- Query optimization

**Technologies**:
- Spring Data JPA
- Hibernate

---

### 4. Client Layer (External Communication)
**Purpose**: Communicate with other microservices

**Components**:
- `ProductClient.java`
- `UserClient.java`

**Responsibilities**:
- Inter-service communication
- Request/response transformation
- Error handling
- Load balancing

**Technologies**:
- Spring Cloud OpenFeign
- Eureka Client

---

### 5. Model Layer (Domain)
**Purpose**: Define domain entities and enums

**Components**:
- `Order.java`
- `OrderItem.java`
- `OrderStatus.java`

**Responsibilities**:
- Domain model definition
- Business methods
- Relationships
- Validation rules

**Technologies**:
- JPA
- Hibernate
- Lombok

---

### 6. DTO Layer (Data Transfer)
**Purpose**: Transfer data between layers and services

**Components**:
- Request DTOs: `OrderRequest`, `OrderItemRequest`, `PaymentRequest`
- Response DTOs: `OrderResponse`, `OrderItemResponse`
- External DTOs: `ProductResponse`, `UserResponse`

**Responsibilities**:
- Data validation
- API contract definition
- Decoupling layers
- Security (hide internal structure)

**Technologies**:
- Jakarta Validation
- Lombok

---

### 7. Exception Layer (Error Handling)
**Purpose**: Handle errors consistently

**Components**:
- `GlobalExceptionHandler.java`
- Custom exceptions: `ResourceNotFoundException`, `InvalidOrderException`, etc.
- `ErrorResponse.java`

**Responsibilities**:
- Catch and handle exceptions
- Format error responses
- Log errors
- Return appropriate HTTP status codes

**Technologies**:
- Spring MVC
- SLF4J

---

### 8. Configuration Layer
**Purpose**: Configure application components

**Components**:
- `SecurityConfig.java`
- `FeignConfig.java`
- `OpenApiConfig.java`

**Responsibilities**:
- Security setup
- Feign client configuration
- API documentation setup
- Bean definitions

**Technologies**:
- Spring Security
- Spring Cloud Config
- SpringDoc OpenAPI

---

## Data Flow

### Order Creation Flow

```
Client Request
    ↓
[OrderController]
    ↓ (validate request)
[OrderService]
    ↓
[UserClient] → Validate User Exists
    ↓
For each OrderItem:
    ↓
[ProductClient] → Get Product Details
    ↓
[ProductClient] → Check Stock Availability
    ↓
Create OrderItem Entity
    ↓
Calculate Total Amount
    ↓
[OrderRepository] → Save Order
    ↓
Return OrderResponse
    ↓
[OrderController] → HTTP 201 Created
```

### Payment Processing Flow

```
Client Request (with Payment Details)
    ↓
[OrderController]
    ↓
[OrderService]
    ↓
[OrderRepository] → Fetch Order
    ↓
Validate Order Status (must be PENDING)
    ↓
Validate Payment Amount
    ↓
Process Payment (Simulated)
    ↓ (if successful)
For each OrderItem:
    ↓
[ProductClient] → Reduce Inventory
    ↓
Update Order Status → CONFIRMED
    ↓
[OrderRepository] → Save Order
    ↓
Return OrderResponse
    ↓
[OrderController] → HTTP 200 OK
```

### Cancellation Flow

```
Client Request (Cancel Order)
    ↓
[OrderController]
    ↓
[OrderService]
    ↓
[OrderRepository] → Fetch Order
    ↓
Validate User Authorization
    ↓
Check if Order can be Cancelled
    ↓ (if CONFIRMED)
For each OrderItem:
    ↓
[ProductClient] → Restore Inventory
    ↓
Update Order Status → CANCELLED
    ↓
[OrderRepository] → Save Order
    ↓
Return OrderResponse
    ↓
[OrderController] → HTTP 200 OK
```

---

## Integration Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                        │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────┐      ┌──────────────┐    ┌─────────────┐  │
│  │ Config Server│      │Eureka Server │    │API Gateway  │  │
│  │  (Port 8888) │      │ (Port 8761)  │    │ (Optional)  │  │
│  └──────┬───────┘      └──────┬───────┘    └──────┬──────┘  │
│         │                      │                    │          │
└─────────┼──────────────────────┼────────────────────┼─────────┘
          │                      │                    │
          ↓                      ↓                    ↓
┌─────────────────────────────────────────────────────────────┐
│                   Order Service                              │
│                   (Port 8083)                                │
└─────────────────────────────────────────────────────────────┘
          ↓                      ↓                    ↓
┌─────────┴──────────┐  ┌───────┴────────┐  ┌───────┴────────┐
│  PostgreSQL        │  │ Product Service│  │  User Service  │
│  (Port 5432)       │  │  (Port 8081)   │  │  (Port 8082)   │
└────────────────────┘  └────────────────┘  └────────────────┘
```

### Service Dependencies

**Required Services**:
1. PostgreSQL Database (for data persistence)
2. Product Service (for product validation and inventory)
3. User Service (for user validation)

**Optional Services**:
4. Eureka Server (for service discovery)
5. Config Server (for centralized configuration)
6. API Gateway (for routing and load balancing)

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────────────────────────────┐
│              orders                      │
├─────────────────────────────────────────┤
│ • id (PK, BIGSERIAL)                    │
│ • user_id (BIGINT, INDEXED)             │
│ • total_amount (DECIMAL(10,2))          │
│ • status (VARCHAR(20), INDEXED)         │
│ • created_at (TIMESTAMP)                │
│ • updated_at (TIMESTAMP)                │
└─────────────────┬───────────────────────┘
                  │ 1
                  │
                  │ has many
                  │
                  │ *
┌─────────────────┴───────────────────────┐
│           order_items                    │
├─────────────────────────────────────────┤
│ • id (PK, BIGSERIAL)                    │
│ • order_id (FK, BIGINT)                 │
│ • product_id (BIGINT)                   │
│ • product_name (VARCHAR(255))           │
│ • quantity (INTEGER)                    │
│ • price (DECIMAL(10,2))                 │
│ • created_at (TIMESTAMP)                │
└─────────────────────────────────────────┘
```

### Indexes

```sql
-- Primary Keys (Automatic)
orders.id
order_items.id

-- Foreign Keys (Automatic)
order_items.order_id → orders.id

-- Custom Indexes
orders.user_id (for user queries)
orders.status (for status queries)
orders.(user_id, status) (composite for user+status queries)
```

---

## Security Architecture

```
Client Request
    ↓
┌─────────────────────────┐
│   Spring Security       │
│   Filter Chain          │
├─────────────────────────┤
│ 1. CSRF Filter (OFF)    │
│ 2. Auth Filter          │
│ 3. Authorization Filter │
└─────────────────────────┘
    ↓
Header Validation
    ↓
┌─────────────────────────┐
│   X-User-Id Header      │
│   X-User-Role Header    │
│   Authorization Header  │
└─────────────────────────┘
    ↓
Controller
    ↓
Service Layer Authorization
    ↓
Data Access
```

### Security Features

1. **Authentication**: Header-based (X-User-Id, Authorization)
2. **Authorization**: Role-based (USER, ADMIN)
3. **Session Management**: Stateless
4. **CSRF**: Disabled (REST API)
5. **Public Endpoints**: Actuator, Swagger
6. **Protected Endpoints**: All API endpoints

---

## Error Handling Architecture

```
Exception Occurs
    ↓
┌─────────────────────────────────────┐
│   GlobalExceptionHandler            │
├─────────────────────────────────────┤
│ @ExceptionHandler methods for:      │
│ • ResourceNotFoundException → 404   │
│ • InvalidOrderException → 400       │
│ • PaymentFailedException → 402      │
│ • InsufficientStockException → 409  │
│ • ValidationException → 400         │
│ • FeignException → varies           │
│ • Generic Exception → 500           │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│   ErrorResponse (Standardized)      │
├─────────────────────────────────────┤
│ • timestamp                         │
│ • status                            │
│ • error                             │
│ • message                           │
│ • path                              │
│ • validationErrors (if applicable)  │
└─────────────────────────────────────┘
    ↓
HTTP Response to Client
```

---

## Transaction Management

```
@Transactional Methods
    ↓
┌─────────────────────────────────────┐
│   Spring Transaction Manager        │
├─────────────────────────────────────┤
│ • Begin Transaction                 │
│ • Execute Business Logic            │
│ • Commit or Rollback                │
└─────────────────────────────────────┘
    ↓
Database Operations
```

### Transactional Boundaries

**Read-Only Transactions**:
- `getOrderById()`
- `getUserOrders()`
- `getAllOrders()`

**Write Transactions**:
- `createOrder()` - Creates order and items
- `processPayment()` - Updates status, reduces inventory
- `cancelOrder()` - Updates status, restores inventory
- `updateOrderStatus()` - Updates order status

---

## Monitoring Architecture

```
Order Service
    ↓
Spring Boot Actuator
    ↓
┌─────────────────────────────────────┐
│   Actuator Endpoints                │
├─────────────────────────────────────┤
│ • /actuator/health                  │
│ • /actuator/info                    │
│ • /actuator/metrics                 │
│ • /actuator/prometheus              │
└─────────────────────────────────────┘
    ↓
┌──────────────┐  ┌──────────────┐
│  Prometheus  │  │   Grafana    │
│  (Metrics)   │  │  (Dashboards)│
└──────────────┘  └──────────────┘
```

---

## Deployment Architecture

### Docker Container

```
┌─────────────────────────────────────────┐
│   Docker Container (order-service)      │
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐ │
│  │   JRE 17 (Alpine Linux)           │ │
│  └───────────────────────────────────┘ │
│                  ↓                      │
│  ┌───────────────────────────────────┐ │
│  │   order-service.jar               │ │
│  └───────────────────────────────────┘ │
│                                         │
│  User: spring (non-root)                │
│  Port: 8083                             │
│  Health Check: /actuator/health         │
│                                         │
└─────────────────────────────────────────┘
```

### Kubernetes Deployment (Recommended)

```yaml
Deployment → ReplicaSet → Pods
    ↓
Service (ClusterIP/LoadBalancer)
    ↓
Ingress (External Access)

ConfigMap → Environment Variables
Secret → Sensitive Data
PersistentVolume → Logs (if needed)
```

---

## Scalability Considerations

### Horizontal Scaling
- ✅ Stateless design
- ✅ No session storage
- ✅ Database connection pooling
- ✅ Supports multiple instances

### Vertical Scaling
- ✅ JVM memory configuration
- ✅ Connection pool sizing
- ✅ Thread pool configuration

### Database Scaling
- ✅ Read replicas (for read-heavy operations)
- ✅ Connection pooling
- ✅ Query optimization
- ✅ Indexing strategy

---

## Design Patterns Summary

| Pattern | Location | Purpose |
|---------|----------|---------|
| **MVC** | Controller/Service | Separation of concerns |
| **Repository** | Repository layer | Data access abstraction |
| **Service Layer** | Service package | Business logic |
| **DTO** | DTO package | Data transfer |
| **Builder** | Entities/DTOs | Object construction |
| **Singleton** | Spring beans | Single instance |
| **Proxy** | Feign clients | Remote service calls |
| **Template Method** | Exception handling | Error processing |
| **Strategy** | Payment processing | Payment methods |
| **Facade** | OrderService | Simplified interface |
| **Dependency Injection** | Throughout | Loose coupling |

---

## Technology Stack Details

### Core Framework
- **Spring Boot 3.2.0**: Application framework
- **Spring Framework 6.x**: Core components

### Data Persistence
- **Spring Data JPA**: Repository abstraction
- **Hibernate 6.x**: ORM implementation
- **PostgreSQL**: Relational database
- **HikariCP**: Connection pooling

### Microservices
- **Spring Cloud 2023.0.0**: Cloud-native features
- **Eureka Client**: Service discovery
- **OpenFeign**: Declarative REST client
- **Spring Cloud Config**: Configuration management
- **Spring Cloud LoadBalancer**: Client-side load balancing

### Security
- **Spring Security 6.x**: Authentication & Authorization
- **JWT**: Token-based auth (ready to integrate)

### Monitoring
- **Spring Boot Actuator**: Production-ready features
- **Micrometer**: Metrics facade
- **Prometheus**: Metrics collection (ready)

### Documentation
- **SpringDoc OpenAPI 3**: API documentation
- **Swagger UI**: Interactive API explorer

### Development
- **Lombok**: Reduce boilerplate
- **Maven**: Build tool
- **SLF4J/Logback**: Logging

### Containerization
- **Docker**: Containerization
- **Alpine Linux**: Base image

---

## Performance Characteristics

### Expected Latency
- **Create Order**: 200-500ms (depends on external services)
- **Get Order**: 50-100ms
- **List Orders**: 100-200ms
- **Update Status**: 50-100ms
- **Process Payment**: 300-600ms (with inventory update)

### Throughput
- **Concurrent Users**: 100+ (with single instance)
- **Requests per Second**: 50-100 (single instance)
- **Scalability**: Linear with instances

### Resource Usage
- **Memory**: 512MB minimum, 1GB recommended
- **CPU**: 0.5 core minimum, 1 core recommended
- **Storage**: Minimal (database is external)

---

## Conclusion

The Order Service follows a clean, layered architecture with clear separation of concerns. It's designed to be:

- **Maintainable**: Clear structure and documentation
- **Scalable**: Stateless and horizontally scalable
- **Reliable**: Comprehensive error handling
- **Observable**: Built-in monitoring
- **Secure**: Authentication and authorization
- **Testable**: Loosely coupled components

The architecture supports both monolithic deployment (for simplicity) and microservices deployment (for scalability).
