/**
 * Core TypeScript interfaces and types for the E-Commerce platform
 */

// User and Authentication Types. Matches user-service's UserResponse DTO - roles is a plural
// array (a user can hold multiple roles), not a single `role` field.
export interface User {
  id: number;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: string[];
  createdAt?: string;
  updatedAt?: string;
}

// Matches the role strings user-service actually issues (see UserServiceImpl - default role is
// ROLE_CUSTOMER; ROLE_ADMIN is assigned to admin accounts).
export enum UserRole {
  ROLE_CUSTOMER = 'ROLE_CUSTOMER',
  ROLE_ADMIN = 'ROLE_ADMIN',
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  firstName: string;
  lastName: string;
}

// Matches user-service's AuthResponse DTO exactly - flat, no nested `user` object, and does not
// include firstName/lastName (those come from GET /users/profile after login).
export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

// Product Types
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  quantity: number;
  category: string;
  imageUrl?: string;
  sku?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductState {
  products: Product[];
  currentProduct: Product | null;
  loading: boolean;
  error: string | null;
  totalPages: number;
  currentPage: number;
}

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  category: string;
  imageUrl?: string;
  sku?: string;
}

export interface UpdateProductRequest extends Partial<CreateProductRequest> {
  id: number;
}

// Order Types
export interface OrderItem {
  id?: number;
  productId: number;
  productName?: string;
  quantity: number;
  price: number;
}

export interface Order {
  id: number;
  userId: number;
  orderItems: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt?: string;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  REJECTED = 'REJECTED',
}

export interface OrderState {
  orders: Order[];
  currentOrder: Order | null;
  loading: boolean;
  error: string | null;
}

export interface CreateOrderRequest {
  orderItems: { productId: number; quantity: number }[];
}

export interface UpdateOrderStatusRequest {
  orderId: number;
  status: OrderStatus;
}

// Cart Types
export interface CartItem {
  product: Product;
  quantity: number;
}

export interface CartState {
  items: CartItem[];
  totalAmount: number;
}

// API Response Types
export interface ApiError {
  message: string;
  status: number;
  errors?: Record<string, string[]>;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Form Types
export interface LoginFormValues {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterFormValues {
  email: string;
  username: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
}

export interface ProductFormValues {
  name: string;
  description: string;
  price: number | string;
  quantity: number | string;
  category: string;
  imageUrl?: string;
  sku?: string;
}

export interface ProfileFormValues {
  email: string;
  username: string;
  firstName: string;
  lastName: string;
}
