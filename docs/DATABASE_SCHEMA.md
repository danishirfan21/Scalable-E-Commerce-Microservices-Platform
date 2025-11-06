# Database Schema Documentation

## Overview

The E-Commerce Microservices Platform uses a **database-per-service** pattern with three separate PostgreSQL databases:

- **userdb** - User Service database
- **productdb** - Product Service database
- **orderdb** - Order Service database

---

## User Service Database (userdb)

### Table: `users`

Stores user account information and authentication details.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier |
| username | VARCHAR(50) | UNIQUE, NOT NULL | User's login name |
| email | VARCHAR(100) | UNIQUE, NOT NULL | User's email address |
| password | VARCHAR(255) | NOT NULL | Bcrypt hashed password |
| first_name | VARCHAR(50) | | User's first name |
| last_name | VARCHAR(50) | | User's last name |
| phone | VARCHAR(20) | | Contact phone number |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Account active status |
| account_non_expired | BOOLEAN | NOT NULL, DEFAULT TRUE | Account expiration status |
| account_non_locked | BOOLEAN | NOT NULL, DEFAULT TRUE | Account lock status |
| credentials_non_expired | BOOLEAN | NOT NULL, DEFAULT TRUE | Password expiration status |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Account creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `username`
- UNIQUE INDEX on `email`
- INDEX on `created_at` for sorting

### Table: `user_roles`

Stores user roles for role-based access control (RBAC).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FOREIGN KEY → users(id) | Reference to user |
| role | VARCHAR(50) | NOT NULL | Role name (ROLE_ADMIN, ROLE_CUSTOMER) |

**Indexes:**
- COMPOSITE INDEX on `(user_id, role)`
- FOREIGN KEY constraint on `user_id` with CASCADE delete

**Available Roles:**
- `ROLE_ADMIN` - Full system access
- `ROLE_CUSTOMER` - Standard customer access

---

## Product Service Database (productdb)

### Table: `products`

Stores product catalog and inventory information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique product identifier |
| name | VARCHAR(255) | NOT NULL | Product name |
| description | TEXT | | Detailed product description |
| price | DECIMAL(10,2) | NOT NULL, CHECK (price >= 0) | Product price |
| quantity | INTEGER | NOT NULL, DEFAULT 0, CHECK (quantity >= 0) | Available inventory |
| category | VARCHAR(100) | NOT NULL | Product category |
| image_url | VARCHAR(500) | | Product image URL |
| sku | VARCHAR(100) | UNIQUE, NOT NULL | Stock Keeping Unit |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `sku`
- INDEX on `category` for filtering
- INDEX on `name` for search
- INDEX on `(quantity, category)` for inventory queries

**Constraints:**
- CHECK constraint ensuring `price >= 0`
- CHECK constraint ensuring `quantity >= 0`

---

## Order Service Database (orderdb)

### Table: `orders`

Stores customer order information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique order identifier |
| user_id | BIGINT | NOT NULL, INDEX | Reference to user (from User Service) |
| total_amount | DECIMAL(10,2) | NOT NULL, CHECK (total_amount >= 0) | Order total price |
| status | VARCHAR(20) | NOT NULL | Order status |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Order creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes:**
- PRIMARY KEY on `id`
- INDEX on `user_id` for user order queries
- INDEX on `status` for status filtering
- COMPOSITE INDEX on `(user_id, status)` for user-specific status queries
- INDEX on `created_at` for sorting by date

**Status Values:**
- `PENDING` - Order created, awaiting payment
- `CONFIRMED` - Payment received, ready for processing
- `SHIPPED` - Order shipped to customer
- `DELIVERED` - Order delivered successfully
- `CANCELLED` - Order cancelled

### Table: `order_items`

Stores individual items within each order.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique order item identifier |
| order_id | BIGINT | FOREIGN KEY → orders(id), NOT NULL | Reference to parent order |
| product_id | BIGINT | NOT NULL | Reference to product (from Product Service) |
| product_name | VARCHAR(255) | NOT NULL | Product name (denormalized) |
| quantity | INTEGER | NOT NULL, CHECK (quantity > 0) | Quantity ordered |
| price | DECIMAL(10,2) | NOT NULL, CHECK (price >= 0) | Unit price at time of order |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes:**
- PRIMARY KEY on `id`
- FOREIGN KEY on `order_id` with CASCADE delete
- INDEX on `product_id` for product order history
- COMPOSITE INDEX on `(order_id, product_id)`

**Constraints:**
- CHECK constraint ensuring `quantity > 0`
- CHECK constraint ensuring `price >= 0`
- CASCADE DELETE when parent order is deleted

---

## Cross-Service Relationships

### Microservices Pattern

The platform follows **loose coupling** principles:

1. **No Foreign Key Constraints Across Databases**
   - `orders.user_id` references `users.id` logically, not with DB constraint
   - `order_items.product_id` references `products.id` logically
   - This allows independent scaling and deployment

2. **Service Communication**
   - User Service validates user existence via REST API
   - Order Service validates products and reduces inventory via Feign clients
   - Data consistency maintained through application logic, not DB constraints

3. **Data Denormalization**
   - `order_items.product_name` stored to preserve order history even if product is deleted
   - Order total calculated and stored for performance

---

## Database Initialization Scripts

### User Service (schema.sql)

```sql
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User roles table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
```

### Product Service (schema.sql)

```sql
-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    category VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    sku VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_quantity_category ON products(quantity, category);
```

### Order Service (schema.sql)

```sql
-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
```

---

## Performance Considerations

### Indexing Strategy

1. **Primary Keys**: Auto-indexed by PostgreSQL
2. **Foreign Keys**: Indexed for join performance
3. **Search Fields**: Indexed (username, email, sku, category)
4. **Filter Fields**: Indexed (status, category, user_id)
5. **Composite Indexes**: For common multi-column queries

### Query Optimization

- Use `EXPLAIN ANALYZE` for query planning
- Implement pagination for large result sets
- Use connection pooling (HikariCP configured)
- Lazy loading for relationships where appropriate

### Scaling Strategy

- **Horizontal Scaling**: Each database can be scaled independently
- **Read Replicas**: Can be added per service
- **Sharding**: Possible for high-volume services
- **Caching**: Redis can be added for frequently accessed data

---

## Backup and Recovery

### Backup Strategy

```bash
# User database backup
pg_dump -h localhost -U postgres userdb > userdb_backup.sql

# Product database backup
pg_dump -h localhost -U postgres productdb > productdb_backup.sql

# Order database backup
pg_dump -h localhost -U postgres orderdb > orderdb_backup.sql
```

### Restore Strategy

```bash
# Restore user database
psql -h localhost -U postgres userdb < userdb_backup.sql
```

---

## Migration Strategy

The platform uses **JPA/Hibernate DDL auto-update** for development:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

For production, consider:
- **Flyway** or **Liquibase** for versioned migrations
- Set `ddl-auto: validate` in production
- Manual schema change review process

---

## Connection Configuration

### Development

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### Production (Docker)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres-user:5432/userdb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

---

## Data Integrity

### Application-Level Constraints

1. **User Registration**
   - Unique username and email validation
   - Password strength requirements
   - Default ROLE_CUSTOMER assignment

2. **Product Management**
   - Price non-negative validation
   - Quantity non-negative validation
   - SKU uniqueness check

3. **Order Processing**
   - Stock availability check before order creation
   - Inventory reduction on order confirmation
   - Inventory restoration on order cancellation
   - Total amount calculated from order items

### Transaction Management

- `@Transactional` annotations on service methods
- Rollback on exceptions
- Isolation level: READ_COMMITTED (default)
- Propagation: REQUIRED (default)

---

## Monitoring

### Health Checks

Each service exposes `/actuator/health` including database status:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

### Metrics

Database connection pool metrics available via Actuator:
- Active connections
- Idle connections
- Pending threads
- Connection timeout count

---

## Security

### Database Security

1. **Password Storage**: BCrypt hashing (strength 10)
2. **SQL Injection Prevention**: JPA parameterized queries
3. **Least Privilege**: Service-specific database users
4. **Connection Encryption**: SSL in production recommended

### Best Practices

- Never store passwords in plain text
- Use prepared statements (JPA handles this)
- Validate input at application layer
- Regular security audits
- Keep PostgreSQL updated

---

## Troubleshooting

### Common Issues

**Connection Refused**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres
# Check connection string
docker logs <container-name>
```

**Migration Errors**
```bash
# Drop and recreate database (development only!)
dropdb userdb && createdb userdb
```

**Performance Issues**
```sql
-- Check active connections
SELECT * FROM pg_stat_activity;

-- Check slow queries
SELECT query, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

## References

- PostgreSQL Documentation: https://www.postgresql.org/docs/
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Database Per Service Pattern: https://microservices.io/patterns/data/database-per-service.html
