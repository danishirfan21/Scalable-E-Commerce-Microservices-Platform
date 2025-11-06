# Product Service API Endpoints

Base URL: `http://localhost:8081/api/products`

## Public Endpoints (No Authentication Required)

### 1. Get All Products
```http
GET /api/products
```

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "name": "Product Name",
    "description": "Product description",
    "price": 99.99,
    "quantity": 100,
    "category": "Electronics",
    "imageUrl": "https://example.com/image.jpg",
    "sku": "PROD-001",
    "inStock": true,
    "createdAt": "2025-11-05T10:00:00",
    "updatedAt": "2025-11-05T10:00:00"
  }
]
```

### 2. Get Product by ID
```http
GET /api/products/{id}
```

**Parameters**:
- `id` (path) - Product ID

**Response**: `200 OK` (same structure as above)

### 3. Get Product by SKU
```http
GET /api/products/sku/{sku}
```

**Parameters**:
- `sku` (path) - Product SKU

**Response**: `200 OK`

### 4. Get Products by Category
```http
GET /api/products/category/{category}
```

**Parameters**:
- `category` (path) - Product category

**Response**: `200 OK` (array of products)

### 5. Search Products by Name
```http
GET /api/products/search?term={searchTerm}
```

**Parameters**:
- `term` (query) - Search term

**Response**: `200 OK` (array of products)

### 6. Check Stock Availability
```http
GET /api/products/{id}/check-stock?quantity={quantity}
```

**Parameters**:
- `id` (path) - Product ID
- `quantity` (query) - Required quantity

**Response**: `200 OK`
```json
true
```

## Admin-Only Endpoints (Requires ADMIN Role)

### 7. Create Product
```http
POST /api/products
Content-Type: application/json
X-User-Id: user123
X-User-Roles: ADMIN
```

**Request Body**:
```json
{
  "name": "New Product",
  "description": "Product description",
  "price": 99.99,
  "quantity": 100,
  "category": "Electronics",
  "imageUrl": "https://example.com/image.jpg",
  "sku": "PROD-002"
}
```

**Response**: `201 Created`

**Validation Rules**:
- `name`: 3-255 characters, required
- `description`: max 2000 characters
- `price`: > 0, required, max 8 digits + 2 decimals
- `quantity`: >= 0, required
- `category`: 2-100 characters, required
- `imageUrl`: valid URL ending with jpg/jpeg/png/gif/webp
- `sku`: 3-100 characters, uppercase letters/numbers/hyphens only, required

### 8. Update Product
```http
PUT /api/products/{id}
Content-Type: application/json
X-User-Id: user123
X-User-Roles: ADMIN
```

**Parameters**:
- `id` (path) - Product ID

**Request Body**: Same as Create Product

**Response**: `200 OK`

### 9. Delete Product
```http
DELETE /api/products/{id}
X-User-Id: user123
X-User-Roles: ADMIN
```

**Parameters**:
- `id` (path) - Product ID

**Response**: `204 No Content`

### 10. Update Inventory
```http
PATCH /api/products/{id}/inventory?quantity={quantity}
X-User-Id: user123
X-User-Roles: ADMIN
```

**Parameters**:
- `id` (path) - Product ID
- `quantity` (query) - New quantity (>= 0)

**Response**: `200 OK`

### 11. Get Low Stock Products
```http
GET /api/products/low-stock?threshold={threshold}
X-User-Id: user123
X-User-Roles: ADMIN
```

**Parameters**:
- `threshold` (query, optional) - Stock threshold (default: 10)

**Response**: `200 OK` (array of products)

## Service-to-Service Endpoints

### 12. Reduce Inventory
```http
PATCH /api/products/{id}/reduce-inventory?amount={amount}
X-User-Id: order-service
X-User-Roles: ORDER_SERVICE
```

**Parameters**:
- `id` (path) - Product ID
- `amount` (query) - Amount to reduce (>= 1)

**Response**: `200 OK`

**Note**: This endpoint is used by the Order Service during order processing.

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-05T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input data",
  "path": "/api/products",
  "validationErrors": {
    "name": "Product name is required",
    "price": "Price must be greater than 0"
  }
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-05T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with ID: 123",
  "path": "/api/products/123"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-11-05T10:30:00",
  "status": 409,
  "error": "Data Integrity Violation",
  "message": "Product with SKU PROD-001 already exists",
  "path": "/api/products"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-05T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "path": "/api/products"
}
```

## Testing with cURL

### Create a Product
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN" \
  -d '{
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse",
    "price": 29.99,
    "quantity": 50,
    "category": "Electronics",
    "imageUrl": "https://example.com/mouse.jpg",
    "sku": "MOUSE-001"
  }'
```

### Get All Products
```bash
curl http://localhost:8081/api/products
```

### Search Products
```bash
curl "http://localhost:8081/api/products/search?term=wireless"
```

### Update Inventory
```bash
curl -X PATCH "http://localhost:8081/api/products/1/inventory?quantity=75" \
  -H "X-User-Id: admin123" \
  -H "X-User-Roles: ADMIN"
```

### Check Stock
```bash
curl "http://localhost:8081/api/products/1/check-stock?quantity=10"
```

## Swagger UI

Interactive API documentation available at:
```
http://localhost:8081/swagger-ui.html
```

## Actuator Endpoints

- Health Check: `http://localhost:8081/actuator/health`
- Metrics: `http://localhost:8081/actuator/metrics`
- Info: `http://localhost:8081/actuator/info`
