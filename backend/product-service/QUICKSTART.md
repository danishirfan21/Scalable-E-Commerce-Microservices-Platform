# Product Service - Quick Start Guide

This guide will help you get the Product Service up and running quickly.

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker and Docker Compose (optional)
- PostgreSQL 14+ (if running without Docker)

## Option 1: Quick Start with Docker Compose (Recommended)

This is the easiest way to get started. Docker Compose will set up both PostgreSQL and the Product Service.

### Step 1: Build the application
```bash
mvn clean package -DskipTests
```

### Step 2: Start services with Docker Compose
```bash
docker-compose up -d
```

This will start:
- PostgreSQL on port 5432
- Product Service on port 8081

### Step 3: Check service health
```bash
curl http://localhost:8081/actuator/health
```

### Step 4: Access Swagger UI
Open your browser and navigate to:
```
http://localhost:8081/swagger-ui.html
```

### Step 5: Test the API
```bash
# Get all products
curl http://localhost:8081/api/products

# Create a product (as admin)
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 99.99,
    "quantity": 100,
    "category": "Test",
    "sku": "TEST-001"
  }'
```

### Stop services
```bash
docker-compose down
```

### Stop and remove volumes (clean database)
```bash
docker-compose down -v
```

## Option 2: Run Locally with Maven

### Step 1: Start PostgreSQL
```bash
# Using Docker
docker run -d \
  --name product-postgres \
  -e POSTGRES_DB=product_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# Or use your local PostgreSQL installation
createdb product_db
```

### Step 2: Build the application
```bash
mvn clean install
```

### Step 3: Run the application
```bash
mvn spring-boot:run
```

Or with environment variables:
```bash
DB_USERNAME=postgres DB_PASSWORD=postgres mvn spring-boot:run
```

### Step 4: Verify the service is running
```bash
curl http://localhost:8081/actuator/health
```

## Option 3: Run as Docker Container

### Step 1: Build Docker image
```bash
docker build -t product-service:1.0.0 .
```

### Step 2: Create network and start PostgreSQL
```bash
# Create network
docker network create ecommerce-network

# Start PostgreSQL
docker run -d \
  --name product-postgres \
  --network ecommerce-network \
  -e POSTGRES_DB=product_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

### Step 3: Run Product Service
```bash
docker run -d \
  --name product-service \
  --network ecommerce-network \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://product-postgres:5432/product_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  product-service:1.0.0
```

### Step 4: Check logs
```bash
docker logs -f product-service
```

## Testing the API

### Using cURL

#### Get all products
```bash
curl http://localhost:8081/api/products
```

#### Get product by ID
```bash
curl http://localhost:8081/api/products/1
```

#### Search products
```bash
curl "http://localhost:8081/api/products/search?term=wireless"
```

#### Get products by category
```bash
curl http://localhost:8081/api/products/category/Electronics
```

#### Create product (requires ADMIN role)
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN" \
  -d '{
    "name": "Gaming Mouse",
    "description": "High-precision gaming mouse",
    "price": 59.99,
    "quantity": 50,
    "category": "Electronics",
    "imageUrl": "https://example.com/gaming-mouse.jpg",
    "sku": "GAME-MOUSE-001"
  }'
```

#### Update product (requires ADMIN role)
```bash
curl -X PUT http://localhost:8081/api/products/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN" \
  -d '{
    "name": "Updated Product Name",
    "description": "Updated description",
    "price": 69.99,
    "quantity": 75,
    "category": "Electronics",
    "sku": "UPDATED-001"
  }'
```

#### Update inventory (requires ADMIN role)
```bash
curl -X PATCH "http://localhost:8081/api/products/1/inventory?quantity=100" \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN"
```

#### Check stock availability
```bash
curl "http://localhost:8081/api/products/1/check-stock?quantity=10"
```

#### Delete product (requires ADMIN role)
```bash
curl -X DELETE http://localhost:8081/api/products/1 \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN"
```

### Using Postman

Import the following collection:

1. Create a new collection named "Product Service"
2. Add requests for each endpoint
3. Set base URL: `http://localhost:8081`
4. For admin endpoints, add headers:
   - `X-User-Id`: `admin123`
   - `X-User-Roles`: `ADMIN`

## Monitoring

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

### Metrics
```bash
curl http://localhost:8081/actuator/metrics
```

### Prometheus Metrics
```bash
curl http://localhost:8081/actuator/prometheus
```

## Accessing Documentation

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs
- **API Endpoints Guide**: See `API_ENDPOINTS.md`
- **Full Documentation**: See `README.md`

## Troubleshooting

### Service won't start

1. **Check if port 8081 is available**:
```bash
# Windows
netstat -ano | findstr :8081

# Linux/Mac
lsof -i :8081
```

2. **Check database connection**:
```bash
# Test PostgreSQL connection
psql -h localhost -U postgres -d product_db
```

3. **Check logs**:
```bash
# Docker
docker logs product-service

# Local
Check console output or application logs
```

### Database errors

1. **Ensure PostgreSQL is running**:
```bash
docker ps | grep postgres
```

2. **Verify database exists**:
```bash
docker exec -it product-postgres psql -U postgres -c "\l"
```

3. **Check credentials**: Verify DB_USERNAME and DB_PASSWORD environment variables

### Permission errors (403 Forbidden)

Make sure you're sending the correct headers:
- `X-User-Id`: User identifier
- `X-User-Roles`: Comma-separated roles (ADMIN, USER, etc.)

## Next Steps

1. Integrate with Eureka Server for service discovery
2. Configure Spring Cloud Config Server
3. Set up API Gateway
4. Implement integration tests
5. Set up CI/CD pipeline

## Support

For issues and questions:
- Check logs: `docker logs product-service`
- Review documentation: `README.md`
- Contact: support@ecommerce.com
