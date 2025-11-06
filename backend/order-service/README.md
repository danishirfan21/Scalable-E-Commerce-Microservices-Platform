# Order Service

Order management microservice for the E-Commerce platform. Handles order creation, payment processing, status tracking, and inventory coordination.

## Features

- **Order Management**: Create, retrieve, update, and cancel orders
- **Payment Processing**: Process payments with validation and error handling
- **Inventory Coordination**: Integrates with Product Service to check stock and reduce inventory
- **Order Status Workflow**: PENDING → CONFIRMED → SHIPPED → DELIVERED
- **User Authorization**: Users can only access their own orders
- **Admin Operations**: Admin users can view all orders and update statuses
- **Comprehensive Error Handling**: Custom exceptions for various error scenarios
- **Transaction Management**: Ensures data consistency across operations

## Technology Stack

- **Spring Boot 3.2.0**: Core framework
- **Spring Data JPA**: Data persistence
- **PostgreSQL**: Database
- **Spring Cloud Netflix Eureka**: Service discovery
- **Spring Cloud Config**: Centralized configuration
- **Spring Cloud OpenFeign**: Declarative REST client
- **Spring Security**: Authentication and authorization
- **Lombok**: Reduce boilerplate code
- **SpringDoc OpenAPI**: API documentation
- **Maven**: Build tool

## Architecture

### Layers

1. **Controller Layer**: REST API endpoints
2. **Service Layer**: Business logic
3. **Repository Layer**: Data access
4. **Model Layer**: Entity definitions
5. **DTO Layer**: Data transfer objects
6. **Exception Layer**: Custom exception handling
7. **Config Layer**: Configuration classes
8. **Client Layer**: Feign clients for inter-service communication

### SOLID Principles

- **Single Responsibility**: Each class has one well-defined purpose
- **Open/Closed**: Extensible through interfaces and abstract classes
- **Liskov Substitution**: Service implementations are substitutable
- **Interface Segregation**: Focused, specific interfaces
- **Dependency Inversion**: Dependencies on abstractions, not concrete implementations

## Database Schema

### Orders Table
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Order Items Table
```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

## API Endpoints

### Order Operations

- `POST /api/orders` - Create a new order
- `GET /api/orders/{orderId}` - Get order by ID
- `GET /api/orders/user` - Get all orders for authenticated user
- `GET /api/orders` - Get all orders (Admin only)
- `PUT /api/orders/{orderId}/status` - Update order status (Admin only)
- `DELETE /api/orders/{orderId}` - Cancel an order
- `POST /api/orders/{orderId}/payment` - Process payment for an order
- `GET /api/orders/status/{status}` - Get orders by status (Admin only)

### Headers Required

- `X-User-Id`: User ID from authentication token
- `X-User-Role`: User role (ADMIN, USER) for authorization
- `Authorization`: Bearer token for authentication

## Configuration

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: postgres
    password: postgres
```

### Eureka Configuration
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Feign Configuration
```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
```

## Running the Service

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Eureka Server running on port 8761
- Config Server running on port 8888 (optional)

### Local Development

1. **Start PostgreSQL and create database**:
```bash
createdb orderdb
```

2. **Build the project**:
```bash
mvn clean install
```

3. **Run the application**:
```bash
mvn spring-boot:run
```

Or:
```bash
java -jar target/order-service.jar
```

The service will start on port **8083**.

### Docker

1. **Build Docker image**:
```bash
docker build -t order-service:latest .
```

2. **Run container**:
```bash
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/orderdb \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  order-service:latest
```

## API Documentation

Once the service is running, access Swagger UI at:
```
http://localhost:8083/swagger-ui.html
```

OpenAPI specification available at:
```
http://localhost:8083/v3/api-docs
```

## Health Check

Health endpoint:
```
http://localhost:8083/actuator/health
```

## Order Workflow

1. **Create Order**: User submits order with product IDs and quantities
2. **Validate Products**: Check if products exist via Product Service
3. **Check Stock**: Verify sufficient inventory is available
4. **Create Order**: Save order with PENDING status
5. **Process Payment**: Accept payment details and process
6. **Reduce Inventory**: Decrease product stock via Product Service
7. **Confirm Order**: Update status to CONFIRMED
8. **Ship Order**: Admin updates status to SHIPPED
9. **Deliver Order**: Admin updates status to DELIVERED

### Cancellation Flow

- Orders can be cancelled in PENDING or CONFIRMED status
- If CONFIRMED, inventory is restored via Product Service
- Order status updated to CANCELLED

## Error Handling

### Custom Exceptions

- `ResourceNotFoundException`: Entity not found (404)
- `InvalidOrderException`: Invalid operation (400)
- `PaymentFailedException`: Payment processing failed (402)
- `InsufficientStockException`: Not enough inventory (409)

### Global Exception Handler

All exceptions are handled by `GlobalExceptionHandler` which returns standardized error responses:

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: '123'",
  "path": "/api/orders/123"
}
```

## Feign Clients

### ProductClient
- `getProduct(id)`: Get product details
- `checkStock(id, quantity)`: Check stock availability
- `reduceInventory(id, quantity)`: Reduce product inventory
- `restoreInventory(id, quantity)`: Restore inventory (on cancellation)

### UserClient
- `getUser(id)`: Get user details

## Transaction Management

- Order creation is transactional
- Payment processing includes inventory reduction in a single transaction
- Rollback occurs if any operation fails

## Logging

Configured logging levels:
- Application: DEBUG
- SQL: DEBUG
- Feign: DEBUG
- Spring Web: INFO

## Monitoring

Actuator endpoints exposed:
- `/actuator/health`: Health status
- `/actuator/info`: Application info
- `/actuator/metrics`: Application metrics
- `/actuator/prometheus`: Prometheus metrics

## Testing

Run tests:
```bash
mvn test
```

## Security

- Stateless session management
- JWT token-based authentication
- Role-based authorization
- CSRF disabled for REST API

## Future Enhancements

- [ ] Implement circuit breaker with Resilience4j
- [ ] Add message queue integration for async processing
- [ ] Implement order notifications
- [ ] Add order history tracking
- [ ] Implement refund processing
- [ ] Add order analytics and reporting
- [ ] Implement inventory reservation
- [ ] Add distributed tracing with Zipkin
- [ ] Implement rate limiting
- [ ] Add comprehensive integration tests

## Contributing

1. Follow the existing code structure
2. Maintain SOLID principles
3. Add unit tests for new features
4. Update documentation
5. Follow Java coding conventions

## License

Copyright 2025 E-Commerce Platform. All rights reserved.
