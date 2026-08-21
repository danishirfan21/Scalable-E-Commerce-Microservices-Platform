/**
 * API endpoint functions for all backend services
 */

import axiosInstance from './axiosInstance';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
  Product,
  CreateProductRequest,
  UpdateProductRequest,
  Order,
  CreateOrderRequest,
  UpdateOrderStatusRequest,
} from '../types';
import { API_ENDPOINTS } from '../utils/constants';

// ==================== Authentication Endpoints ====================

export const authAPI = {
  /**
   * Login user
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>(API_ENDPOINTS.AUTH.LOGIN, credentials);
    return response.data;
  },

  /**
   * Register new user
   */
  register: async (userData: RegisterRequest): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>(API_ENDPOINTS.AUTH.REGISTER, userData);
    return response.data;
  },

  /**
   * Logout user. JWT auth is stateless - there is no server-side session to invalidate, so this
   * is purely a client-side token clear (see authSlice's logout thunk).
   */
  logout: async (): Promise<void> => {
    return Promise.resolve();
  },
};

// ==================== User Endpoints ====================

export const userAPI = {
  /**
   * Get current user profile
   */
  getProfile: async (): Promise<User> => {
    const response = await axiosInstance.get<User>(API_ENDPOINTS.USERS.PROFILE);
    return response.data;
  },

  /**
   * Update user profile
   */
  updateProfile: async (userData: Partial<User>): Promise<User> => {
    const response = await axiosInstance.put<User>(API_ENDPOINTS.USERS.UPDATE, userData);
    return response.data;
  },
};

// ==================== Product Endpoints ====================

export const productAPI = {
  /**
   * Get all products. product-service has no server-side pagination - it returns the full
   * catalog as a plain array - so this returns Product[] directly rather than a paginated
   * envelope.
   */
  getProducts: async (): Promise<Product[]> => {
    const response = await axiosInstance.get<Product[]>(API_ENDPOINTS.PRODUCTS.BASE);
    return response.data;
  },

  /**
   * Get product by ID
   */
  getProductById: async (id: number): Promise<Product> => {
    const response = await axiosInstance.get<Product>(API_ENDPOINTS.PRODUCTS.BY_ID(id));
    return response.data;
  },

  /**
   * Create new product (Admin only)
   */
  createProduct: async (productData: CreateProductRequest): Promise<Product> => {
    const response = await axiosInstance.post<Product>(API_ENDPOINTS.PRODUCTS.BASE, productData);
    return response.data;
  },

  /**
   * Update product (Admin only)
   */
  updateProduct: async (productData: UpdateProductRequest): Promise<Product> => {
    const response = await axiosInstance.put<Product>(
      API_ENDPOINTS.PRODUCTS.BY_ID(productData.id),
      productData
    );
    return response.data;
  },

  /**
   * Delete product (Admin only)
   */
  deleteProduct: async (id: number): Promise<void> => {
    await axiosInstance.delete(API_ENDPOINTS.PRODUCTS.BY_ID(id));
  },

  /**
   * Search products by name (matches ProductController's /api/products/search?term=)
   */
  searchProducts: async (term: string): Promise<Product[]> => {
    const response = await axiosInstance.get<Product[]>(`${API_ENDPOINTS.PRODUCTS.BASE}/search`, {
      params: { term },
    });
    return response.data;
  },
};

// ==================== Order Endpoints ====================

export const orderAPI = {
  /**
   * Create new order
   */
  createOrder: async (orderData: CreateOrderRequest): Promise<Order> => {
    const response = await axiosInstance.post<Order>(API_ENDPOINTS.ORDERS.BASE, orderData);
    return response.data;
  },

  /**
   * Get user's orders
   */
  getUserOrders: async (): Promise<Order[]> => {
    const response = await axiosInstance.get<Order[]>(API_ENDPOINTS.ORDERS.USER_ORDERS);
    return response.data;
  },

  /**
   * Get all orders (Admin only). order-service has no server-side pagination.
   */
  getAllOrders: async (): Promise<Order[]> => {
    const response = await axiosInstance.get<Order[]>(API_ENDPOINTS.ORDERS.BASE);
    return response.data;
  },

  /**
   * Get order by ID
   */
  getOrderById: async (id: number): Promise<Order> => {
    const response = await axiosInstance.get<Order>(API_ENDPOINTS.ORDERS.BY_ID(id));
    return response.data;
  },

  /**
   * Update order status (Admin only)
   */
  updateOrderStatus: async (data: UpdateOrderStatusRequest): Promise<Order> => {
    const response = await axiosInstance.put<Order>(
      API_ENDPOINTS.ORDERS.UPDATE_STATUS(data.orderId),
      null,
      { params: { status: data.status } }
    );
    return response.data;
  },
};
