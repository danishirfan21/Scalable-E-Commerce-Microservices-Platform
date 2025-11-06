# Product Service - Project Structure

## Complete Directory Structure

```
product-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ecommerce/
│   │   │           └── product/
│   │   │               ├── ProductServiceApplication.java          # Main application class
│   │   │               ├── config/                                # Configuration classes
│   │   │               │   ├── OpenApiConfig.java                # Swagger/OpenAPI config
│   │   │               │   └── SecurityConfig.java               # Security configuration
│   │   │               ├── controller/                           # REST controllers
│   │   │               │   └── ProductController.java            # Product endpoints
│   │   │               ├── dto/                                  # Data Transfer Objects
│   │   │               │   ├── ProductRequest.java              # Request DTO
│   │   │               │   └── ProductResponse.java             # Response DTO
│   │   │               ├── exception/                            # Exception handling
│   │   │               │   ├── GlobalExceptionHandler.java      # Global exception handler
│   │   │               │   ├── InsufficientStockException.java  # Stock exception
│   │   │               │   └── ResourceNotFoundException.java   # Not found exception
│   │   │               ├── model/                                # Domain entities
│   │   │               │   └── Product.java                     # Product entity
│   │   │               ├── repository/                           # Data access layer
│   │   │               │   └── ProductRepository.java           # Product repository
│   │   │               └── service/                              # Business logic
│   │   │                   ├── ProductService.java              # Service interface
│   │   │                   └── ProductServiceImpl.java          # Service implementation
│   │   └── resources/
│   │       ├── application.yml                                   # Main configuration
│   │       ├── application-docker.yml                           # Docker profile
│   │       ├── bootstrap.yml                                     # Bootstrap config
│   │       └── data.sql                                         # Sample data
│   └── test/
│       ├── java/                                                # Test classes
│       └── resources/
│           └── application-test.yml                             # Test configuration
├── .dockerignore                                                # Docker ignore file
├── .gitignore                                                   # Git ignore file
├── API_ENDPOINTS.md                                            # API documentation
├── docker-compose.yml                                          # Docker Compose config
├── Dockerfile                                                  # Docker build file
├── pom.xml                                                     # Maven configuration
├── PROJECT_STRUCTURE.md                                        # This file
├── QUICKSTART.md                                               # Quick start guide
└── README.md                                                   # Main documentation
```

## File Descriptions

### Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven project configuration with all dependencies |
| `application.yml` | Main application configuration (database, server, logging) |
| `application-docker.yml` | Docker-specific configuration |
| `bootstrap.yml` | Bootstrap configuration for Config Server |
| `application-test.yml` | Test environment configuration (H2 database) |
| `docker-compose.yml` | Multi-container Docker setup |
| `Dockerfile` | Multi-stage Docker build |

### Java Source Files

#### Main Application
- `ProductServiceApplication.java`: Spring Boot application entry point with @EnableDiscoveryClient and @EnableJpaAuditing

#### Configuration Layer
- `SecurityConfig.java`: Security configuration with gateway authentication filter
- `OpenApiConfig.java`: Swagger/OpenAPI documentation configuration

#### Controller Layer
- `ProductController.java`: REST API endpoints with:
  - CRUD operations
  - Search and filtering
  - Inventory management
  - Stock checking
  - Swagger annotations
  - Method-level security

#### Service Layer
- `ProductService.java`: Service interface defining business operations
- `ProductServiceImpl.java`: Service implementation with:
  - Transaction management
  - Business logic
  - SLF4J logging
  - Error handling

#### Repository Layer
- `ProductRepository.java`: JPA repository with custom queries:
  - findByCategory
  - findByNameContaining
  - findBySkuAndQuantityGreaterThan
  - findLowStockProducts
  - findAllInStock

#### Model Layer
- `Product.java`: JPA entity with:
  - Validation annotations
  - Audit fields (createdAt, updatedAt)
  - Business methods (isInStock, hasSufficientStock, etc.)
  - Database indexes

#### DTO Layer
- `ProductRequest.java`: Request DTO with validation:
  - JSR-303 annotations
  - Custom validation patterns
  - Field-level constraints
- `ProductResponse.java`: Response DTO with:
  - Entity to DTO mapper
  - Computed fields (inStock)

#### Exception Layer
- `GlobalExceptionHandler.java`: Centralized exception handling for:
  - ResourceNotFoundException (404)
  - InsufficientStockException (400)
  - ValidationException (400)
  - DataIntegrityViolationException (409)
  - AccessDeniedException (403)
  - Generic exceptions (500)
- `ResourceNotFoundException.java`: Custom exception for missing resources
- `InsufficientStockException.java`: Custom exception for stock issues

### Documentation Files

| File | Content |
|------|---------|
| `README.md` | Comprehensive documentation with setup, features, and architecture |
| `API_ENDPOINTS.md` | Complete API reference with examples |
| `QUICKSTART.md` | Step-by-step quick start guide |
| `PROJECT_STRUCTURE.md` | This file - project structure overview |

## Key Features by Layer

### Security Features
- Gateway-based authentication
- Header-based user identification (X-User-Id, X-User-Roles)
- Method-level authorization (@PreAuthorize)
- Stateless session management
- Public and protected endpoints

### Database Features
- PostgreSQL for production
- H2 for testing
- JPA/Hibernate ORM
- Database migrations support
- Connection pooling (HikariCP)
- Audit fields with @CreatedDate and @LastModifiedDate
- Optimized indexes

### API Features
- RESTful endpoints
- JSON request/response
- Comprehensive validation
- Swagger/OpenAPI documentation
- Error responses with details
- CORS support ready

### Monitoring Features
- Spring Boot Actuator
- Health checks
- Metrics endpoint
- Prometheus metrics
- Custom health indicators ready

### Service Discovery
- Eureka client integration
- Service registration
- Load balancing ready
- Fault tolerance ready

### Configuration Management
- Config Server integration
- Profile-based configuration
- Environment variable support
- Externalized configuration

## Technology Stack Summary

### Core Framework
- Spring Boot 3.2.0
- Java 17

### Spring Modules
- Spring Web (REST APIs)
- Spring Data JPA (Data access)
- Spring Security (Authentication/Authorization)
- Spring Cloud Config (Configuration)
- Spring Cloud Netflix Eureka (Service discovery)
- Spring Boot Actuator (Monitoring)

### Database
- PostgreSQL (Production)
- H2 (Testing)

### Documentation
- SpringDoc OpenAPI 3

### Build & Deployment
- Maven 3.9+
- Docker
- Docker Compose

### Development Tools
- Lombok (Boilerplate reduction)
- SLF4J (Logging)
- Jakarta Validation (Bean validation)

## Design Patterns Used

1. **Layered Architecture**: Clear separation of concerns
2. **Repository Pattern**: Data access abstraction
3. **Service Layer Pattern**: Business logic encapsulation
4. **DTO Pattern**: Data transfer objects
5. **Builder Pattern**: Entity and DTO construction
6. **Singleton Pattern**: Spring beans
7. **Factory Pattern**: Entity to DTO conversion
8. **Strategy Pattern**: Service implementations

## SOLID Principles

- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Service interface and implementation
- **Interface Segregation**: Focused interfaces
- **Dependency Inversion**: Depend on abstractions (interfaces)

## API Endpoint Summary

### Public Endpoints (11 endpoints)
- GET /api/products (all)
- GET /api/products/{id}
- GET /api/products/sku/{sku}
- GET /api/products/category/{category}
- GET /api/products/search
- GET /api/products/{id}/check-stock

### Admin Endpoints (5 endpoints)
- POST /api/products
- PUT /api/products/{id}
- DELETE /api/products/{id}
- PATCH /api/products/{id}/inventory
- GET /api/products/low-stock

### Service Endpoints (1 endpoint)
- PATCH /api/products/{id}/reduce-inventory

## Database Schema

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(10,2) NOT NULL,
    quantity INTEGER NOT NULL,
    category VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    sku VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_category ON products(category);
CREATE UNIQUE INDEX idx_sku ON products(sku);
CREATE INDEX idx_name ON products(name);
```

## Configuration Summary

### Application Ports
- Service: 8081
- Database: 5432
- Eureka: 8761
- Config Server: 8888

### Environment Variables
- DB_USERNAME
- DB_PASSWORD
- SPRING_PROFILES_ACTIVE

### Profiles
- `default`: Local development
- `docker`: Docker deployment
- `test`: Testing environment

## Next Development Steps

1. Add Redis caching for frequently accessed products
2. Implement pagination for large result sets
3. Add product image upload functionality
4. Implement product reviews and ratings
5. Add product variants (size, color)
6. Implement inventory reservation for orders
7. Add audit logging
8. Implement event publishing (Kafka/RabbitMQ)
9. Add GraphQL support
10. Implement advanced search (Elasticsearch)

## Maintenance Notes

### Regular Tasks
- Monitor low stock products
- Review and optimize slow queries
- Update dependencies regularly
- Review logs for errors
- Monitor metrics and performance

### Backup Strategy
- Regular database backups
- Configuration backups
- Document schema changes

### Scaling Considerations
- Database connection pool tuning
- Add read replicas for read-heavy loads
- Implement caching layer
- Consider sharding for large catalogs
