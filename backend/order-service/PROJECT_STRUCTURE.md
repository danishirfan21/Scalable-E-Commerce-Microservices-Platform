# Order Service - Project Structure

## Directory Structure

```
order-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── ecommerce/
│       │           └── order/
│       │               ├── OrderServiceApplication.java
│       │               ├── client/
│       │               │   ├── ProductClient.java
│       │               │   └── UserClient.java
│       │               ├── config/
│       │               │   ├── FeignConfig.java
│       │               │   ├── OpenApiConfig.java
│       │               │   └── SecurityConfig.java
│       │               ├── controller/
│       │               │   └── OrderController.java
│       │               ├── dto/
│       │               │   ├── OrderItemRequest.java
│       │               │   ├── OrderItemResponse.java
│       │               │   ├── OrderRequest.java
│       │               │   ├── OrderResponse.java
│       │               │   ├── PaymentRequest.java
│       │               │   ├── ProductResponse.java
│       │               │   └── UserResponse.java
│       │               ├── exception/
│       │               │   ├── ErrorResponse.java
│       │               │   ├── GlobalExceptionHandler.java
│       │               │   ├── InsufficientStockException.java
│       │               │   ├── InvalidOrderException.java
│       │               │   ├── PaymentFailedException.java
│       │               │   └── ResourceNotFoundException.java
│       │               ├── model/
│       │               │   ├── Order.java
│       │               │   ├── OrderItem.java
│       │               │   └── OrderStatus.java
│       │               ├── repository/
│       │               │   ├── OrderItemRepository.java
│       │               │   └── OrderRepository.java
│       │               └── service/
│       │                   ├── OrderService.java
│       │                   └── OrderServiceImpl.java
│       └── resources/
│           └── application.yml
├── .dockerignore
├── .gitignore
├── Dockerfile
├── pom.xml
├── PROJECT_STRUCTURE.md
└── README.md
```

## Components Overview

### 1. Main Application
- **OrderServiceApplication.java**: Entry point with @SpringBootApplication and @EnableFeignClients

### 2. Model Layer (Entities)
- **Order.java**: Main order entity with relationships
  - Fields: id, userId, orderItems, totalAmount, status, createdAt, updatedAt
  - OneToMany relationship with OrderItem
  - Business methods: addOrderItem(), removeOrderItem(), calculateTotalAmount(), canBeCancelled()

- **OrderItem.java**: Individual items within an order
  - Fields: id, orderId, productId, productName, quantity, price, createdAt
  - Method: getSubtotal()

- **OrderStatus.java**: Enum for order lifecycle
  - Values: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

### 3. DTOs (Data Transfer Objects)
- **OrderRequest.java**: Request for creating orders
- **OrderItemRequest.java**: Request for order items
- **OrderResponse.java**: Response containing order details
- **OrderItemResponse.java**: Response for order items
- **PaymentRequest.java**: Payment processing request
- **ProductResponse.java**: Product details from Product Service
- **UserResponse.java**: User details from User Service

### 4. Repository Layer
- **OrderRepository.java**: JPA repository for Order
  - Custom queries: findByUserId, findByStatus, findByUserIdAndStatus
  - Optimized queries with JOIN FETCH for eager loading

- **OrderItemRepository.java**: JPA repository for OrderItem
  - Custom queries: findByOrderId, findByProductId

### 5. Service Layer
- **OrderService.java**: Interface defining business operations
  - Methods: createOrder, getOrderById, getUserOrders, getAllOrders
  - Methods: updateOrderStatus, cancelOrder, processPayment

- **OrderServiceImpl.java**: Implementation with business logic
  - Transaction management
  - Integration with Feign clients
  - Inventory coordination
  - Payment processing
  - Status validation
  - Entity-to-DTO mapping

### 6. Controller Layer
- **OrderController.java**: REST API endpoints
  - POST /api/orders - Create order
  - GET /api/orders/{id} - Get order by ID
  - GET /api/orders/user - Get user orders
  - GET /api/orders - Get all orders (Admin)
  - PUT /api/orders/{id}/status - Update status (Admin)
  - DELETE /api/orders/{id} - Cancel order
  - POST /api/orders/{id}/payment - Process payment
  - Swagger/OpenAPI annotations
  - Header-based user identification

### 7. Feign Clients
- **ProductClient.java**: Communication with Product Service
  - getProduct(id)
  - checkStock(id, quantity)
  - reduceInventory(id, quantity)
  - restoreInventory(id, quantity)

- **UserClient.java**: Communication with User Service
  - getUser(id)

### 8. Exception Handling
- **GlobalExceptionHandler.java**: Centralized exception handling
  - Handles all custom exceptions
  - Validation error handling
  - Feign client error handling
  - Generic exception handling

- **Custom Exceptions**:
  - ResourceNotFoundException - 404 errors
  - InvalidOrderException - 400 errors
  - PaymentFailedException - 402 errors
  - InsufficientStockException - 409 errors

- **ErrorResponse.java**: Standardized error response format

### 9. Configuration
- **SecurityConfig.java**: Spring Security configuration
  - Stateless session management
  - Public endpoints for actuator and swagger
  - Authentication required for API endpoints

- **FeignConfig.java**: Feign client configuration
  - Custom error decoder
  - Request interceptor for header propagation
  - Logging configuration

- **OpenApiConfig.java**: Swagger/OpenAPI documentation setup
  - API information and metadata
  - Security scheme configuration
  - Server configuration

### 10. Configuration Files
- **application.yml**: Application configuration
  - Database settings (PostgreSQL)
  - Eureka client configuration
  - Feign client settings
  - Actuator endpoints
  - Logging configuration
  - OpenAPI settings

- **pom.xml**: Maven dependencies
  - Spring Boot 3.2.0
  - Spring Cloud 2023.0.0
  - PostgreSQL driver
  - Lombok
  - SpringDoc OpenAPI
  - Eureka Client
  - Config Client
  - OpenFeign

### 11. Docker
- **Dockerfile**: Multi-stage build
  - Build stage with Maven
  - Runtime stage with JRE
  - Non-root user for security
  - Health check configuration
  - Optimized for containerized environments

- **.dockerignore**: Excludes unnecessary files from Docker context

### 12. Documentation
- **README.md**: Comprehensive service documentation
- **PROJECT_STRUCTURE.md**: This file, detailing project structure
- **.gitignore**: Git ignore rules

## Key Features Implemented

### Transaction Management
- @Transactional annotations on service methods
- Rollback on exceptions
- Consistent state management

### Order Workflow
1. Create order (PENDING)
2. Validate products and stock
3. Process payment
4. Reduce inventory
5. Confirm order (CONFIRMED)
6. Ship order (SHIPPED)
7. Deliver order (DELIVERED)

### Cancellation Workflow
- Can cancel PENDING or CONFIRMED orders
- Restores inventory if CONFIRMED
- Updates status to CANCELLED

### Inter-Service Communication
- Feign clients for Product and User services
- Error handling for failed communications
- Header propagation (X-User-Id, X-User-Role)

### Error Handling
- Global exception handler
- Custom exceptions for domain errors
- Validation error handling
- Standardized error responses

### Security
- Spring Security integration
- Header-based authentication
- Role-based authorization
- CSRF disabled for REST API

### API Documentation
- Swagger UI at /swagger-ui.html
- OpenAPI 3.0 specification
- Detailed endpoint documentation
- Request/response examples

### Monitoring
- Actuator endpoints
- Health checks
- Metrics exposure
- Prometheus integration ready

### Logging
- SLF4J with Logback
- Configurable log levels
- Request/response logging
- SQL query logging

## SOLID Principles Applied

1. **Single Responsibility Principle**
   - Each class has one clear responsibility
   - Separation of concerns across layers

2. **Open/Closed Principle**
   - Extensible through interfaces
   - New payment methods can be added without modifying core logic

3. **Liskov Substitution Principle**
   - Service implementations are interchangeable
   - Repository abstractions work with any JPA implementation

4. **Interface Segregation Principle**
   - Focused interfaces (OrderService, repositories)
   - Clients depend only on methods they use

5. **Dependency Inversion Principle**
   - Dependencies on abstractions (interfaces)
   - Loose coupling through dependency injection

## Design Patterns Used

1. **Repository Pattern**: Data access abstraction
2. **Service Layer Pattern**: Business logic separation
3. **DTO Pattern**: Data transfer and validation
4. **Builder Pattern**: Entity and DTO construction
5. **Strategy Pattern**: Payment processing (extensible)
6. **Facade Pattern**: OrderService simplifies complex operations

## Best Practices

- Constructor injection for dependencies
- Immutable DTOs where appropriate
- Comprehensive validation
- Proper exception handling
- Clean code principles
- Meaningful variable and method names
- Comprehensive JavaDoc comments
- Separation of concerns
- DRY (Don't Repeat Yourself)
- Fail-fast approach

## Database Optimization

- Indexed columns (user_id, status, user_id+status)
- JOIN FETCH for N+1 problem prevention
- Batch insert configuration
- Connection pooling with HikariCP

## Testing Considerations

- Service layer should be unit tested
- Repository layer with integration tests
- Controller layer with MockMvc tests
- Feign clients can be mocked
- Test data builders for entities and DTOs

## Deployment

- Dockerized application
- Health checks included
- Configurable through environment variables
- Ready for Kubernetes deployment
- Connects to Config Server for centralized config
- Registers with Eureka for service discovery
