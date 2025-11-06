/**
 * Application-wide constants
 */

export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const APP_NAME = process.env.REACT_APP_NAME || 'E-Commerce Platform';

export const TOKEN_KEY = 'ecommerce_token';
export const USER_KEY = 'ecommerce_user';

export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  PRODUCTS: '/products',
  ORDERS: '/orders',
  PROFILE: '/profile',
  ADMIN_PRODUCTS: '/admin/products',
  ADMIN_ORDERS: '/admin/orders',
} as const;

export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    LOGOUT: '/auth/logout',
  },
  USERS: {
    PROFILE: '/users/profile',
    UPDATE: '/users/profile',
  },
  PRODUCTS: {
    BASE: '/products',
    BY_ID: (id: number) => `/products/${id}`,
  },
  ORDERS: {
    BASE: '/orders',
    BY_ID: (id: number) => `/orders/${id}`,
    USER_ORDERS: '/orders/user',
    UPDATE_STATUS: (id: number) => `/orders/${id}/status`,
  },
} as const;

export const ORDER_STATUS_COLORS: Record<string, string> = {
  PENDING: '#FFA500',
  PROCESSING: '#2196F3',
  SHIPPED: '#9C27B0',
  DELIVERED: '#4CAF50',
  CANCELLED: '#F44336',
};

export const PRODUCT_CATEGORIES = [
  'Electronics',
  'Clothing',
  'Books',
  'Home & Garden',
  'Sports & Outdoors',
  'Toys & Games',
  'Food & Beverage',
  'Other',
];
