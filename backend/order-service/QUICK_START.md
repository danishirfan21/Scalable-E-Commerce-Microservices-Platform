# Order Service - Quick Start Guide

## Prerequisites

Before running the Order Service, ensure you have:

1. **Java 17 or higher** installed
   ```bash
   java -version
   ```

2. **Maven 3.6+** installed
   ```bash
   mvn -version
   ```

3. **PostgreSQL 12+** installed and running
   ```bash
   psql --version
   ```

4. **Eureka Server** running on port 8761 (optional but recommended)

5. **Product Service** running (for inter-service communication)

6. **User Service** running (for user validation)

---

## Step 1: Database Setup

### Create Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE orderdb;

# Grant privileges (if needed)
GRANT ALL PRIVILEGES ON DATABASE orderdb TO postgres;

# Exit
\q
```

### Configure Database Connection

The default configuration in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: postgres
    password: postgres
```

If your database credentials are different, either:
- Update `application.yml`
- Set environment variables:
  ```bash
  export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orderdb
  export SPRING_DATASOURCE_USERNAME=your_username
  export SPRING_DATASOURCE_PASSWORD=your_password
  ```

---

## Step 2: Build the Project

```bash
# Navigate to the project directory
cd "D:\Scalable E-Commerce Microservices Platform\backend\order-service"

# Clean and build
mvn clean install

# Or skip tests for faster build
mvn clean install -DskipTests
```

Build output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.123 s
```

---

## Step 3: Run the Application

### Option A: Using Maven

```bash
mvn spring-boot:run
```

### Option B: Using Java

```bash
java -jar target/order-service.jar
```

### Option C: With Custom Configuration

```bash
java -jar target/order-service.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/orderdb \
  --spring.datasource.username=postgres \
  --spring.datasource.password=postgres \
  --server.port=8083
```

---

## Step 4: Verify the Application

### Check Application Startup

Look for these log messages:
```
Started OrderServiceApplication in X.XXX seconds
Tomcat started on port(s): 8083 (http)
```

### Health Check

```bash
curl http://localhost:8083/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Check Eureka Registration

If Eureka is running, verify registration at:
```
http://localhost:8761
```

Look for `ORDER-SERVICE` in the registered instances.

---

## Step 5: Test the API

### Access Swagger UI

Open in browser:
```
http://localhost:8083/swagger-ui.html
```

### Create a Test Order

```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "orderItems": [
      {
        "productId": 1,
        "quantity": 2
      }
    ]
  }'
```

### Get User Orders

```bash
curl http://localhost:8083/api/orders/user \
  -H "X-User-Id: 1"
```

---

## Step 6: Database Verification

### Connect to Database

```bash
psql -U postgres -d orderdb
```

### Check Tables

```sql
-- List all tables
\dt

-- View orders
SELECT * FROM orders;

-- View order items
SELECT * FROM order_items;

-- Check order with items
SELECT o.id, o.status, o.total_amount, oi.product_name, oi.quantity, oi.price
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id;
```

---

## Docker Deployment

### Build Docker Image

```bash
docker build -t order-service:latest .
```

### Run with Docker

```bash
docker run -d \
  --name order-service \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/orderdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  order-service:latest
```

### Check Docker Logs

```bash
docker logs -f order-service
```

### Stop and Remove

```bash
docker stop order-service
docker rm order-service
```

---

## Docker Compose (Recommended)

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: order-postgres
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - order-db-data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

  order-service:
    build: .
    container_name: order-service
    ports:
      - "8083:8083"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orderdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      - postgres
    networks:
      - ecommerce-network

volumes:
  order-db-data:

networks:
  ecommerce-network:
    external: true
```

### Run with Docker Compose

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f order-service

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## Troubleshooting

### Issue: Port 8083 already in use

**Solution 1**: Change the port in `application.yml`:
```yaml
server:
  port: 8084
```

**Solution 2**: Kill the process using the port:
```bash
# Windows
netstat -ano | findstr :8083
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8083
kill -9 <PID>
```

### Issue: Database connection failed

**Check if PostgreSQL is running**:
```bash
# Windows
sc query postgresql-x64-15

# Linux
systemctl status postgresql

# Mac
brew services list
```

**Start PostgreSQL**:
```bash
# Windows
net start postgresql-x64-15

# Linux
sudo systemctl start postgresql

# Mac
brew services start postgresql
```

### Issue: Cannot connect to Eureka

**Check Eureka Server**:
```bash
curl http://localhost:8761
```

**Disable Eureka temporarily** in `application.yml`:
```yaml
eureka:
  client:
    enabled: false
```

### Issue: Feign client errors (Cannot connect to Product/User Service)

**Check if services are running**:
```bash
# Product Service
curl http://localhost:8081/actuator/health

# User Service
curl http://localhost:8082/actuator/health
```

**Run services or mock them for testing**.

### Issue: Build fails with "tests failed"

**Skip tests**:
```bash
mvn clean install -DskipTests
```

### Issue: JPA/Hibernate errors

**Check database schema**:
```bash
psql -U postgres -d orderdb -c "\dt"
```

**Reset database** (WARNING: deletes all data):
```bash
psql -U postgres -c "DROP DATABASE orderdb;"
psql -U postgres -c "CREATE DATABASE orderdb;"
```

---

## Configuration Profiles

### Development Profile

Create `application-dev.yml`:
```yaml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    com.ecommerce.order: DEBUG
```

Run with:
```bash
java -jar target/order-service.jar --spring.profiles.active=dev
```

### Production Profile

Create `application-prod.yml`:
```yaml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    com.ecommerce.order: INFO
```

Run with:
```bash
java -jar target/order-service.jar --spring.profiles.active=prod
```

---

## Environment Variables

Common environment variables:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orderdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Server
SERVER_PORT=8083

# Eureka
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/

# Feign Timeouts
FEIGN_CLIENT_CONFIG_DEFAULT_CONNECTTIMEOUT=5000
FEIGN_CLIENT_CONFIG_DEFAULT_READTIMEOUT=10000

# Logging
LOGGING_LEVEL_COM_ECOMMERCE_ORDER=DEBUG
```

---

## Next Steps

1. **Explore API Documentation**: Visit Swagger UI
2. **Test All Endpoints**: Use provided cURL commands or Postman
3. **Monitor the Service**: Check Actuator endpoints
4. **Integrate with Other Services**: Ensure Product and User services are available
5. **Set Up Logging**: Configure log aggregation if needed
6. **Performance Testing**: Load test the service
7. **Security**: Implement proper JWT validation

---

## Useful Commands

```bash
# Build without tests
mvn clean package -DskipTests

# Run with custom port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084

# Debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Check dependencies
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates

# Generate project info
mvn site
```

---

## Support

For issues or questions:
1. Check the logs: `logs/order-service.log`
2. Review the README.md
3. Check API_EXAMPLES.md for usage examples
4. Review PROJECT_STRUCTURE.md for architecture details

---

## Success!

If you see this in your logs, you're good to go:
```
Started OrderServiceApplication in 15.234 seconds (JVM running for 16.123)
```

Happy coding!
