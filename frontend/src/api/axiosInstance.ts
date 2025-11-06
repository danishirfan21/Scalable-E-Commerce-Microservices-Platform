/**
 * Axios instance with interceptors for authentication and error handling
 */

import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { toast } from 'react-toastify';
import { API_BASE_URL, TOKEN_KEY, ROUTES } from '../utils/constants';
import { storage } from '../utils/helpers';

// Create axios instance with default configuration
const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request interceptor to add JWT token to requests
 */
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = storage.get<string>(TOKEN_KEY);

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor for centralized error handling
 */
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    // Handle different error scenarios
    if (error.response) {
      const status = error.response.status;
      const data: any = error.response.data;

      switch (status) {
        case 401:
          // Unauthorized - clear token and redirect to login
          storage.remove(TOKEN_KEY);
          storage.remove('ecommerce_user');
          toast.error('Session expired. Please login again.');
          window.location.href = ROUTES.LOGIN;
          break;

        case 403:
          // Forbidden
          toast.error('You do not have permission to perform this action.');
          break;

        case 404:
          // Not found
          toast.error(data?.message || 'Resource not found.');
          break;

        case 422:
          // Validation error
          if (data?.errors) {
            const errorMessages = Object.values(data.errors).flat();
            errorMessages.forEach((msg: any) => toast.error(msg));
          } else {
            toast.error(data?.message || 'Validation error occurred.');
          }
          break;

        case 500:
          // Server error
          toast.error('Server error. Please try again later.');
          break;

        default:
          toast.error(data?.message || 'An error occurred. Please try again.');
      }

      return Promise.reject({
        message: data?.message || 'An error occurred',
        status,
        errors: data?.errors,
      });
    } else if (error.request) {
      // Network error
      toast.error('Network error. Please check your connection.');
      return Promise.reject({
        message: 'Network error. Please check your connection.',
        status: 0,
      });
    } else {
      // Other errors
      toast.error('An unexpected error occurred.');
      return Promise.reject({
        message: error.message || 'An unexpected error occurred',
        status: 0,
      });
    }
  }
);

export default axiosInstance;
