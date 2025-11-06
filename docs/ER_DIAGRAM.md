# Entity-Relationship Diagram

## Complete E-Commerce Platform ER Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          E-COMMERCE MICROSERVICES PLATFORM                       │
│                              DATABASE ARCHITECTURE                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                          USER SERVICE DATABASE (userdb)                         ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

┌──────────────────────────────┐
│          USERS               │
├──────────────────────────────┤
│ ▸ id (PK)                    │
│   username (UNIQUE)          │
│   email (UNIQUE)             │
│   password                   │
│   first_name                 │
│   last_name                  │
│   phone                      │
│   enabled                    │
│   account_non_expired        │
│   account_non_locked         │
│   credentials_non_expired    │
│   created_at                 │
│   updated_at                 │
└──────────────────────────────┘
                │
                │ 1
                │
                │
                │ N
                ▼
┌──────────────────────────────┐
│       USER_ROLES             │
├──────────────────────────────┤
│ ▸ user_id (FK, PK)           │
│ ▸ role (PK)                  │
│   - ROLE_ADMIN               │
│   - ROLE_CUSTOMER            │
└──────────────────────────────┘


┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                       PRODUCT SERVICE DATABASE (productdb)                      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

┌──────────────────────────────┐
│         PRODUCTS             │
├──────────────────────────────┤
│ ▸ id (PK)                    │
│   name                       │
│   description                │
│   price                      │
│   quantity                   │
│   category                   │
│   image_url                  │
│   sku (UNIQUE)               │
│   created_at                 │
│   updated_at                 │
└──────────────────────────────┘


┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                        ORDER SERVICE DATABASE (orderdb)                         ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

┌──────────────────────────────┐
│          ORDERS              │
├──────────────────────────────┤
│ ▸ id (PK)                    │
│   user_id (logical FK)       │ ───┐ Logical reference to
│   total_amount               │    │ users.id (User Service)
│   status                     │    │ via REST API
│   - PENDING                  │    │
│   - CONFIRMED                │    │
│   - SHIPPED                  │    │
│   - DELIVERED                │    │
│   - CANCELLED                │    │
│   created_at                 │    │
│   updated_at                 │    │
└──────────────────────────────┘    │
                │                    │
                │ 1                  │
                │                    │
                │                    │
                │ N                  │
                ▼                    │
┌──────────────────────────────┐    │
│       ORDER_ITEMS            │    │
├──────────────────────────────┤    │
│ ▸ id (PK)                    │    │
│ ◂ order_id (FK)              │    │
│   product_id (logical FK)    │─┐  │ Logical reference to
│   product_name (denormalized)│ │  │ products.id (Product Service)
│   quantity                   │ │  │ via Feign Client
│   price                      │ │  │
│   created_at                 │ │  │
└──────────────────────────────┘ │  │
                                 │  │
                                 ▼  ▼


┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                         CROSS-SERVICE RELATIONSHIPS                              ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

    USERS (User Service)                                PRODUCTS (Product Service)
          │                                                     │
          │ Validated via                                      │ Validated via
          │ REST API Call                                      │ Feign Client
          │                                                     │
          ▼                                                     ▼
    ┌──────────┐                                         ┌──────────┐
    │ user_id  │◄────── ORDERS ────────┬───────────────►│product_id│
    └──────────┘    (Order Service)    │                └──────────┘
                                        │
                                        │ 1
                                        │
                                        │
                                        │ N
                                        ▼
                                  ORDER_ITEMS
                            (Order Service)
```

---

## Detailed Entity Descriptions

### User Service Entities

#### USERS Entity
- **Purpose**: Store user authentication and profile data
- **Primary Key**: `id` (auto-generated)
- **Unique Constraints**: `username`, `email`
- **Relationships**: One-to-Many with USER_ROLES
- **Security**: Password stored as BCrypt hash

#### USER_ROLES Entity
- **Purpose**: Implement Role-Based Access Control (RBAC)
- **Composite Primary Key**: `(user_id, role)`
- **Relationships**: Many-to-One with USERS
- **Cascade**: DELETE CASCADE on user deletion

---

### Product Service Entities

#### PRODUCTS Entity
- **Purpose**: Product catalog and inventory management
- **Primary Key**: `id` (auto-generated)
- **Unique Constraints**: `sku`
- **Business Rules**:
  - Price must be non-negative
  - Quantity must be non-negative
- **Indexed Fields**: `category`, `name`, `sku`

---

### Order Service Entities

#### ORDERS Entity
- **Purpose**: Customer order tracking
- **Primary Key**: `id` (auto-generated)
- **Logical Foreign Key**: `user_id` (references User Service)
- **Relationships**: One-to-Many with ORDER_ITEMS
- **Status Workflow**: PENDING → CONFIRMED → SHIPPED → DELIVERED
- **Cancellation**: Possible from PENDING or CONFIRMED states

#### ORDER_ITEMS Entity
- **Purpose**: Line items for each order
- **Primary Key**: `id` (auto-generated)
- **Foreign Key**: `order_id` (references ORDERS)
- **Logical Foreign Key**: `product_id` (references Product Service)
- **Denormalization**: `product_name` stored for historical record
- **Cascade**: DELETE CASCADE when parent order is deleted

---

## Cardinality and Relationships

### Within User Service
```
USERS (1) ────────< USER_ROLES (N)
  One user can have multiple roles
```

### Within Order Service
```
ORDERS (1) ────────< ORDER_ITEMS (N)
  One order contains multiple items
```

### Cross-Service (Logical)
```
USERS (1) ────────< ORDERS (N)
  One user can place multiple orders
  Validated via REST API, not DB constraint

PRODUCTS (1) ────────< ORDER_ITEMS (N)
  One product can appear in multiple order items
  Validated via Feign Client, not DB constraint
```

---

## Microservices Pattern: Database Per Service

### Why Logical References?

This architecture follows the **Database Per Service** pattern:

1. **Service Independence**
   - Each service owns its database
   - No direct database-level foreign keys across services
   - Services can be deployed, scaled, and maintained independently

2. **Data Consistency**
   - Maintained through application logic
   - REST API calls for validation
   - Compensating transactions for failures

3. **Benefits**
   - Independent scaling
   - Technology diversity (could use different databases)
   - Fault isolation
   - Team autonomy

4. **Trade-offs**
   - No referential integrity at DB level
   - Application must handle consistency
   - Potential for orphaned references
   - More complex transaction management

---

## Data Flow Diagrams

### User Registration Flow
```
Client → User Service → userdb
                          │
                          ├─ Insert into USERS
                          └─ Insert into USER_ROLES (ROLE_CUSTOMER)
```

### Product Creation Flow (Admin)
```
Client → API Gateway → Product Service → productdb
          (JWT Auth)                       │
                                          └─ Insert into PRODUCTS
```

### Order Creation Flow
```
Client → API Gateway → Order Service → orderdb
          (JWT Auth)        │              │
                           │              ├─ Insert into ORDERS
                           │              └─ Insert into ORDER_ITEMS
                           │
                           ├─ Feign → Product Service
                           │           (Check stock)
                           │
                           └─ Feign → User Service
                                      (Validate user)
```

### Order Confirmation Flow
```
Order Service → productdb (orderdb)
    │            │
    │            ├─ Update ORDERS status
    │            └─ Retrieve ORDER_ITEMS
    │
    └─ Feign → Product Service
               (Reduce inventory)
```

---

## Indexing Strategy

### User Service
- **users.id**: Primary key (auto-indexed)
- **users.username**: Unique index for login
- **users.email**: Unique index for login
- **users.created_at**: Index for sorting
- **user_roles.user_id**: Index for role lookup

### Product Service
- **products.id**: Primary key (auto-indexed)
- **products.sku**: Unique index for lookup
- **products.category**: Index for filtering
- **products.name**: Index for search
- **products(quantity, category)**: Composite index for inventory queries

### Order Service
- **orders.id**: Primary key (auto-indexed)
- **orders.user_id**: Index for user order history
- **orders.status**: Index for status filtering
- **orders(user_id, status)**: Composite index for combined queries
- **orders.created_at**: Index for sorting
- **order_items.order_id**: Foreign key index
- **order_items.product_id**: Index for product order history

---

## Constraints Summary

### Check Constraints
- `products.price >= 0`
- `products.quantity >= 0`
- `orders.total_amount >= 0`
- `order_items.quantity > 0`
- `order_items.price >= 0`

### Unique Constraints
- `users.username`
- `users.email`
- `products.sku`

### Foreign Key Constraints (Within Service)
- `user_roles.user_id → users.id (CASCADE DELETE)`
- `order_items.order_id → orders.id (CASCADE DELETE)`

### No Cross-Service Foreign Keys
- `orders.user_id` (validated via REST API)
- `order_items.product_id` (validated via Feign Client)

---

## Sample Data Relationships

### Example User
```
users.id = 1
users.username = "johndoe"
users.email = "john@example.com"

user_roles:
  (user_id=1, role="ROLE_CUSTOMER")
```

### Example Product
```
products.id = 100
products.name = "Laptop"
products.sku = "ELEC-LAPTOP-001"
products.quantity = 50
products.price = 999.99
```

### Example Order
```
orders.id = 500
orders.user_id = 1  (references johndoe)
orders.total_amount = 1999.98
orders.status = "CONFIRMED"

order_items:
  (id=501, order_id=500, product_id=100, product_name="Laptop", quantity=2, price=999.99)
```

---

## Evolution and Migration

### Adding New Fields
```sql
-- User Service
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);

-- Product Service
ALTER TABLE products ADD COLUMN weight DECIMAL(10,2);

-- Order Service
ALTER TABLE orders ADD COLUMN shipping_address TEXT;
```

### Schema Versioning
- Use Flyway or Liquibase for production
- Version control all schema changes
- Test migrations in staging environment

---

## Legend

```
▸ Primary Key
◂ Foreign Key
─ Relationship Line
┌─┐ Entity Box
├─┤ Entity Separator
└─┘ Entity Border
```

---

## Tools for Visualization

This diagram can be rendered using:
- **dbdiagram.io** - Online ER diagram tool
- **draw.io** - Free diagram software
- **PlantUML** - Text-to-diagram tool
- **MySQL Workbench** - ER diagram designer
- **DBeaver** - Database management with ER diagrams

---

## Conclusion

This ER design supports a microservices architecture with:
- ✅ Service independence
- ✅ Scalability
- ✅ Data integrity through application logic
- ✅ Clear separation of concerns
- ✅ Flexibility for future changes
