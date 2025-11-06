# Order Service - API Examples

## Base URL
```
http://localhost:8083
```

## Authentication Headers
All API requests (except public endpoints) require:
```
X-User-Id: <user-id>
Authorization: Bearer <jwt-token>
```

Admin operations also require:
```
X-User-Role: ADMIN
```

---

## 1. Create Order

**Endpoint**: `POST /api/orders`

**Headers**:
```
Content-Type: application/json
X-User-Id: 1
Authorization: Bearer eyJhbGc...
```

**Request Body**:
```json
{
  "orderItems": [
    {
      "productId": 101,
      "quantity": 2
    },
    {
      "productId": 102,
      "quantity": 1
    }
  ]
}
```

**Success Response** (201 Created):
```json
{
  "id": 1,
  "userId": 1,
  "orderItems": [
    {
      "id": 1,
      "orderId": 1,
      "productId": 101,
      "productName": "Laptop",
      "quantity": 2,
      "price": 999.99,
      "subtotal": 1999.98,
      "createdAt": "2025-01-15T10:30:00"
    },
    {
      "id": 2,
      "orderId": 1,
      "productId": 102,
      "productName": "Mouse",
      "quantity": 1,
      "price": 29.99,
      "subtotal": 29.99,
      "createdAt": "2025-01-15T10:30:00"
    }
  ],
  "totalAmount": 2029.97,
  "status": "PENDING",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

**Error Response** (409 Conflict - Insufficient Stock):
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 409,
  "error": "Insufficient Stock",
  "message": "Insufficient stock for product ID 101. Requested quantity: 2",
  "path": "/api/orders"
}
```

---

## 2. Get Order by ID

**Endpoint**: `GET /api/orders/{orderId}`

**Headers**:
```
X-User-Id: 1
Authorization: Bearer eyJhbGc...
```

**Success Response** (200 OK):
```json
{
  "id": 1,
  "userId": 1,
  "orderItems": [
    {
      "id": 1,
      "orderId": 1,
      "productId": 101,
      "productName": "Laptop",
      "quantity": 2,
      "price": 999.99,
      "subtotal": 1999.98,
      "createdAt": "2025-01-15T10:30:00"
    }
  ],
  "totalAmount": 1999.98,
  "status": "CONFIRMED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:35:00"
}
```

**Error Response** (404 Not Found):
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: '999'",
  "path": "/api/orders/999"
}
```

**Error Response** (403 Forbidden - Not Owner):
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "You are not authorized to access this order",
  "path": "/api/orders/1"
}
```

---

## 3. Get User Orders

**Endpoint**: `GET /api/orders/user`

**Headers**:
```
X-User-Id: 1
Authorization: Bearer eyJhbGc...
```

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "userId": 1,
    "orderItems": [...],
    "totalAmount": 1999.98,
    "status": "CONFIRMED",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:35:00"
  },
  {
    "id": 2,
    "userId": 1,
    "orderItems": [...],
    "totalAmount": 599.99,
    "status": "SHIPPED",
    "createdAt": "2025-01-14T09:20:00",
    "updatedAt": "2025-01-15T08:15:00"
  }
]
```

---

## 4. Get All Orders (Admin Only)

**Endpoint**: `GET /api/orders`

**Headers**:
```
X-User-Id: 1
X-User-Role: ADMIN
Authorization: Bearer eyJhbGc...
```

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "userId": 1,
    "orderItems": [...],
    "totalAmount": 1999.98,
    "status": "CONFIRMED",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:35:00"
  },
  {
    "id": 2,
    "userId": 2,
    "orderItems": [...],
    "totalAmount": 299.99,
    "status": "PENDING",
    "createdAt": "2025-01-15T11:00:00",
    "updatedAt": "2025-01-15T11:00:00"
  }
]
```

---

## 5. Process Payment

**Endpoint**: `POST /api/orders/{orderId}/payment`

**Headers**:
```
Content-Type: application/json
X-User-Id: 1
Authorization: Bearer eyJhbGc...
```

**Request Body**:
```json
{
  "paymentMethod": "CREDIT_CARD",
  "amount": 1999.98
}
```

**Success Response** (200 OK):
```json
{
  "id": 1,
  "userId": 1,
  "orderItems": [...],
  "totalAmount": 1999.98,
  "status": "CONFIRMED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:35:00"
}
```

**Error Response** (400 Bad Request - Wrong Status):
```json
{
  "timestamp": "2025-01-15T10:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Order must be in PENDING status to process payment",
  "path": "/api/orders/1/payment"
}
```

**Error Response** (402 Payment Required):
```json
{
  "timestamp": "2025-01-15T10:35:00",
  "status": 402,
  "error": "Payment Failed",
  "message": "Payment amount does not match order total",
  "path": "/api/orders/1/payment"
}
```

**Error Response** (400 Validation Error):
```json
{
  "timestamp": "2025-01-15T10:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/orders/1/payment",
  "validationErrors": {
    "paymentMethod": "Payment method is required",
    "amount": "Amount must be greater than 0"
  }
}
```

---

## 6. Update Order Status (Admin Only)

**Endpoint**: `PUT /api/orders/{orderId}/status?status=SHIPPED`

**Headers**:
```
X-User-Role: ADMIN
Authorization: Bearer eyJhbGc...
```

**Success Response** (200 OK):
```json
{
  "id": 1,
  "userId": 1,
  "orderItems": [...],
  "totalAmount": 1999.98,
  "status": "SHIPPED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T12:00:00"
}
```

**Error Response** (400 Bad Request - Invalid Transition):
```json
{
  "timestamp": "2025-01-15T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid status transition from CANCELLED to SHIPPED",
  "path": "/api/orders/1/status"
}
```

**Valid Status Transitions**:
- PENDING → CONFIRMED, CANCELLED
- CONFIRMED → SHIPPED, CANCELLED
- SHIPPED → DELIVERED
- DELIVERED → (none)
- CANCELLED → (none)

---

## 7. Cancel Order

**Endpoint**: `DELETE /api/orders/{orderId}`

**Headers**:
```
X-User-Id: 1
Authorization: Bearer eyJhbGc...
```

**Success Response** (200 OK):
```json
{
  "id": 1,
  "userId": 1,
  "orderItems": [...],
  "totalAmount": 1999.98,
  "status": "CANCELLED",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T12:15:00"
}
```

**Error Response** (400 Bad Request - Cannot Cancel):
```json
{
  "timestamp": "2025-01-15T12:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Order cannot be cancelled in current status: DELIVERED",
  "path": "/api/orders/1"
}
```

---

## 8. Get Orders by Status (Admin Only)

**Endpoint**: `GET /api/orders/status/{status}`

**Example**: `GET /api/orders/status/PENDING`

**Headers**:
```
X-User-Role: ADMIN
Authorization: Bearer eyJhbGc...
```

**Success Response** (200 OK):
```json
[
  {
    "id": 2,
    "userId": 2,
    "orderItems": [...],
    "totalAmount": 299.99,
    "status": "PENDING",
    "createdAt": "2025-01-15T11:00:00",
    "updatedAt": "2025-01-15T11:00:00"
  },
  {
    "id": 3,
    "userId": 3,
    "orderItems": [...],
    "totalAmount": 149.99,
    "status": "PENDING",
    "createdAt": "2025-01-15T11:30:00",
    "updatedAt": "2025-01-15T11:30:00"
  }
]
```

---

## Common Error Responses

### Validation Error (400)
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/orders",
  "validationErrors": {
    "orderItems": "Order must contain at least one item",
    "orderItems[0].quantity": "Quantity must be at least 1"
  }
}
```

### Service Communication Error (500)
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 503,
  "error": "Service Communication Error",
  "message": "Failed to communicate with external service: product-service",
  "path": "/api/orders"
}
```

### Unauthorized (401)
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required",
  "path": "/api/orders"
}
```

---

## cURL Examples

### Create Order
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "orderItems": [
      {"productId": 101, "quantity": 2}
    ]
  }'
```

### Get User Orders
```bash
curl -X GET http://localhost:8083/api/orders/user \
  -H "X-User-Id: 1" \
  -H "Authorization: Bearer <token>"
```

### Process Payment
```bash
curl -X POST http://localhost:8083/api/orders/1/payment \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "paymentMethod": "CREDIT_CARD",
    "amount": 1999.98
  }'
```

### Update Order Status (Admin)
```bash
curl -X PUT "http://localhost:8083/api/orders/1/status?status=SHIPPED" \
  -H "X-User-Role: ADMIN" \
  -H "Authorization: Bearer <token>"
```

### Cancel Order
```bash
curl -X DELETE http://localhost:8083/api/orders/1 \
  -H "X-User-Id: 1" \
  -H "Authorization: Bearer <token>"
```

---

## Postman Collection

Import these as environment variables in Postman:
```
BASE_URL=http://localhost:8083
USER_ID=1
USER_ROLE=ADMIN
AUTH_TOKEN=<your-jwt-token>
```

Then use `{{BASE_URL}}`, `{{USER_ID}}`, etc. in your requests.

---

## Health Check

```bash
curl http://localhost:8083/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

## Swagger UI

Access interactive API documentation:
```
http://localhost:8083/swagger-ui.html
```

View OpenAPI specification:
```
http://localhost:8083/v3/api-docs
```
