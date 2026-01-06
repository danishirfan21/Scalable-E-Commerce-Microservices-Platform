/**
 * Redux slice for product state management
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { productAPI } from '../../api/endpoints';
import { ProductState, CreateProductRequest, UpdateProductRequest } from '../../types';
import { toast } from 'react-toastify';

const initialState: ProductState = {
  products: [],
  currentProduct: null,
  loading: false,
  error: null,
  totalPages: 0,
  currentPage: 0,
};

// ==================== Async Thunks ====================

/**
 * Fetch all products
 */
export const fetchProducts = createAsyncThunk(
  'products/fetchProducts',
  async ({ page = 0, size = 20 }: { page?: number; size?: number }, { rejectWithValue }) => {
    try {
      const response = await productAPI.getProducts(page, size);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch products');
    }
  }
);

/**
 * Fetch single product by ID
 */
export const fetchProductById = createAsyncThunk(
  'products/fetchProductById',
  async (id: number, { rejectWithValue }) => {
    try {
      const response = await productAPI.getProductById(id);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch product');
    }
  }
);

/**
 * Create new product
 */
export const createProduct = createAsyncThunk(
  'products/createProduct',
  async (productData: CreateProductRequest, { rejectWithValue }) => {
    try {
      const response = await productAPI.createProduct(productData);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to create product');
    }
  }
);

/**
 * Update product
 */
export const updateProduct = createAsyncThunk(
  'products/updateProduct',
  async (productData: UpdateProductRequest, { rejectWithValue }) => {
    try {
      const response = await productAPI.updateProduct(productData);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to update product');
    }
  }
);

/**
 * Delete product
 */
export const deleteProduct = createAsyncThunk(
  'products/deleteProduct',
  async (id: number, { rejectWithValue }) => {
    try {
      await productAPI.deleteProduct(id);
      return id;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete product');
    }
  }
);

/**
 * Search products
 */
export const searchProducts = createAsyncThunk(
  'products/searchProducts',
  async (
    { query, page = 0, size = 20 }: { query: string; page?: number; size?: number },
    { rejectWithValue }
  ) => {
    try {
      const response = await productAPI.searchProducts(query, page, size);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to search products');
    }
  }
);

// ==================== Slice ====================

const productSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {
    /**
     * Clear error message
     */
    clearError: (state) => {
      state.error = null;
    },

    /**
     * Clear current product
     */
    clearCurrentProduct: (state) => {
      state.currentProduct = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch products
    builder
      .addCase(fetchProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.products = action.payload.content;
        state.totalPages = action.payload.totalPages;
        state.currentPage = action.payload.number;
        state.error = null;
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Fetch product by ID
    builder
      .addCase(fetchProductById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProductById.fulfilled, (state, action) => {
        state.loading = false;
        state.currentProduct = action.payload;
        state.error = null;
      })
      .addCase(fetchProductById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Create product
    builder
      .addCase(createProduct.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createProduct.fulfilled, (state, action) => {
        state.loading = false;
        state.products.unshift(action.payload);
        state.error = null;
        toast.success('Product created successfully!');
      })
      .addCase(createProduct.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Update product
    builder
      .addCase(updateProduct.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(updateProduct.fulfilled, (state, action) => {
        state.loading = false;
        const index = state.products.findIndex((p) => p.id === action.payload.id);
        if (index !== -1) {
          state.products[index] = action.payload;
        }
        if (state.currentProduct?.id === action.payload.id) {
          state.currentProduct = action.payload;
        }
        state.error = null;
        toast.success('Product updated successfully!');
      })
      .addCase(updateProduct.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Delete product
    builder
      .addCase(deleteProduct.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(deleteProduct.fulfilled, (state, action) => {
        state.loading = false;
        state.products = state.products.filter((p) => p.id !== action.payload);
        state.error = null;
        toast.success('Product deleted successfully!');
      })
      .addCase(deleteProduct.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Search products
    builder
      .addCase(searchProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(searchProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.products = action.payload.content;
        state.totalPages = action.payload.totalPages;
        state.currentPage = action.payload.number;
        state.error = null;
      })
      .addCase(searchProducts.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearError, clearCurrentProduct } = productSlice.actions;
export default productSlice.reducer;
