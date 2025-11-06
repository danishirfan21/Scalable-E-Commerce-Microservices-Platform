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
  PaginatedResponse,
} from '../types';
import { API_ENDPOINTS } from '../utils/constants';

// ==================== Authentication Endpoints ====================

export const authAPI = {
  /**
   * Login user
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>(
      API_ENDPOINTS.AUTH.LOGIN,
      credentials
    );
    return response.data;
  },

  /**
   * Register new user
   */
  register: async (userData: RegisterRequest): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>(
      API_ENDPOINTS.AUTH.REGISTER,
      userData
    );
    return response.data;
  },

  /**
   * Logout user
   */
  logout: async (): Promise<void> => {
    await axiosInstance.post(API_ENDPOINTS.AUTH.LOGOUT);
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
   * Get all products with pagination
   */
  getProducts: async (page = 0, size = 20): Promise<PaginatedResponse<Product>> => {
    const response = await axiosInstance.get<PaginatedResponse<Product>>(
      API_ENDPOINTS.PRODUCTS.BASE,
      {
        params: { page, size },
      }
    );
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
    const response = await axiosInstance.post<Product>(
      API_ENDPOINTS.PRODUCTS.BASE,
      productData
    );
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
   * Search products by name or category
   */
  searchProducts: async (
    query: string,
    page = 0,
    size = 20
  ): Promise<PaginatedResponse<Product>> => {
    const response = await axiosInstance.get<PaginatedResponse<Product>>(
      `${API_ENDPOINTS.PRODUCTS.BASE}/search`,
      {
        params: { query, page, size },
      }
    );
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
   * Get all orders (Admin only)
   */
  getAllOrders: async (page = 0, size = 20): Promise<PaginatedResponse<Order>> => {
    const response = await axiosInstance.get<PaginatedResponse<Order>>(
      API_ENDPOINTS.ORDERS.BASE,
      {
        params: { page, size },
      }
    );
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
    const response = await axiosInstance.patch<Order>(
      API_ENDPOINTS.ORDERS.UPDATE_STATUS(data.orderId),
      { status: data.status }
    );
    return response.data;
  },
};
