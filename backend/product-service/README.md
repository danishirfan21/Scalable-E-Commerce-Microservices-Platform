# Product Service

Product Management Microservice for E-Commerce Platform

## Overview

The Product Service is responsible for managing the product catalog in the e-commerce platform. It provides comprehensive APIs for product CRUD operations, inventory management, and product queries.

## Features

- Product CRUD operations (Create, Read, Update, Delete)
- Inventory management and stock tracking
- Product search and filtering
- Category-based product retrieval
- Low stock alerts
- SKU-based product lookup
- Stock validation for orders
- RESTful API with Swagger documentation
- JWT-based authentication via API Gateway
- Database persistence with PostgreSQL
- Service discovery with Eureka
- Centralized configuration with Config Server

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security
- **Service Discovery**: Eureka Client
- **Configuration**: Spring Cloud Config
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Build Tool**: Maven
- **Containerization**: Docker

## Architecture

### Package Structure

```
com.ecommerce.product
├── config              # Configuration classes (Security, OpenAPI)
├── controller          # REST controllers
├── dto                 # Data Transfer Objects
├── exception           # Custom exceptions and global exception handler
├── model               # JPA entities
├── repository          # Data access layer
└── service             # Business logic layer
```

### Key Components

1. **Product Entity**: Core domain model with JPA annotations
2. **ProductRepository**: Data access with custom queries
3. **ProductService**: Business logic implementation
4. **ProductController**: REST API endpoints
5. **SecurityConfig**: Gateway-based authentication
6. **GlobalExceptionHandler**: Centralized error handling

## API Endpoints

### Public Endpoints

- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/sku/{sku}` - Get product by SKU
- `GET /api/products/category/{category}` - Get products by category
- `GET /api/products/search?term={term}` - Search products by name
- `GET /api/products/{id}/check-stock?quantity={qty}` - Check stock availability

### Admin-Only Endpoints (Requires ADMIN role)

- `POST /api/products` - Create new product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product
- `PATCH /api/products/{id}/inventory?quantity={qty}` - Update inventory
- `GET /api/products/low-stock?threshold={threshold}` - Get low stock products

### Service-to-Service Endpoints

- `PATCH /api/products/{id}/reduce-inventory?amount={amt}` - Reduce inventory (for order processing)

## Configuration

### Database Configuration

Configure PostgreSQL connection in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/product_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
```

### Environment Variables

- `DB_USERNAME` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (default: postgres)
- `SPRING_PROFILES_ACTIVE` - Active profile (dev/prod)

## Running the Service

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+
- Eureka Server running on port 8761
- Config Server running on port 8888 (optional)

### Local Development

1. **Start PostgreSQL**:
```bash
# Create database
createdb product_db
```

2. **Build the application**:
```bash
mvn clean install
```

3. **Run the application**:
```bash
mvn spring-boot:run
```

The service will start on port 8081.

### Docker Deployment

1. **Build Docker image**:
```bash
docker build -t product-service:1.0.0 .
```

2. **Run container**:
```bash
docker run -d \
  -p 8081:8081 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e SPRING_PROFILES_ACTIVE=prod \
  --name product-service \
  product-service:1.0.0
```

### Docker Compose

```yaml
version: '3.8'
services:
  product-service:
    image: product-service:1.0.0
    ports:
      - "8081:8081"
    environment:
      - DB_USERNAME=postgres
      - DB_PASSWORD=postgres
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - postgres
```

## API Documentation

Swagger UI is available at:
- http://localhost:8081/swagger-ui.html

OpenAPI JSON specification:
- http://localhost:8081/v3/api-docs

## Security

The service uses a gateway-based authentication model:

1. API Gateway authenticates users and validates JWT tokens
2. Gateway forwards requests with `X-User-Id` and `X-User-Roles` headers
3. Product Service extracts authentication from headers
4. Method-level security with `@PreAuthorize` annotations

### Required Roles

- **ADMIN**: Full access to all endpoints
- **USER**: Read-only access to public endpoints
- **ORDER_SERVICE**: Access to inventory reduction endpoint

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Health check
- `/actuator/info` - Service information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

## Testing

### Run all tests:
```bash
mvn test
```

### Run with coverage:
```bash
mvn clean verify jacoco:report
```

## Error Handling

The service implements comprehensive error handling:

- `ResourceNotFoundException` (404) - Product not found
- `InsufficientStockException` (400) - Insufficient inventory
- `DataIntegrityViolationException` (409) - Duplicate SKU
- `MethodArgumentNotValidException` (400) - Validation errors
- `AccessDeniedException` (403) - Authorization failure

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2025-11-05T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with ID: 123",
  "path": "/api/products/123"
}
```

## Logging

Logging levels can be configured in `application.yml`:

- `INFO` - General application flow
- `DEBUG` - Detailed service operations
- `TRACE` - SQL queries and parameters

## Database Schema

### Products Table

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| description | VARCHAR(2000) | |
| price | DECIMAL(10,2) | NOT NULL |
| quantity | INTEGER | NOT NULL |
| category | VARCHAR(100) | NOT NULL, INDEXED |
| image_url | VARCHAR(500) | |
| sku | VARCHAR(100) | NOT NULL, UNIQUE, INDEXED |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

## Best Practices Implemented

1. **SOLID Principles**: Clear separation of concerns
2. **Builder Pattern**: Used for entity and DTO construction
3. **Repository Pattern**: Clean data access layer
4. **Service Layer**: Business logic isolation
5. **DTO Pattern**: Request/Response data transfer
6. **Transaction Management**: @Transactional annotations
7. **Validation**: JSR-303 Bean Validation
8. **Exception Handling**: Global exception handler
9. **Logging**: SLF4J with appropriate levels
10. **Security**: Method-level authorization
11. **Documentation**: Comprehensive Swagger annotations
12. **Database Indexing**: Optimized queries

## Contributing

1. Follow Java code conventions
2. Write unit tests for new features
3. Update API documentation
4. Add logging for important operations
5. Handle exceptions appropriately

## License

Copyright (c) 2025 E-Commerce Platform Team

## Support

For issues and questions, please contact: support@ecommerce.com
