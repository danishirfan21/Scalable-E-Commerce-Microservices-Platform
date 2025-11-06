# Development Guide

Comprehensive guide for developing and extending the E-Commerce Platform frontend.

## Table of Contents

1. [Development Environment Setup](#development-environment-setup)
2. [Code Style Guide](#code-style-guide)
3. [Adding New Features](#adding-new-features)
4. [State Management Patterns](#state-management-patterns)
5. [API Integration](#api-integration)
6. [Component Development](#component-development)
7. [Testing Strategy](#testing-strategy)
8. [Common Tasks](#common-tasks)
9. [Debugging Tips](#debugging-tips)
10. [Performance Optimization](#performance-optimization)

## Development Environment Setup

### Required Tools

```bash
# Check versions
node --version  # Should be 18+
npm --version   # Should be 9+
```

### IDE Setup (VS Code Recommended)

**Recommended Extensions:**
- ESLint
- Prettier
- ES7+ React/Redux/React-Native snippets
- TypeScript Hero
- Auto Import
- Path Intellisense

**VS Code Settings (.vscode/settings.json):**
```json
{
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib"
}
```

### Environment Variables

Create `.env` file (already done):
```env
REACT_APP_API_URL=http://localhost:8080/api
```

Never commit `.env` to git. Use `.env.example` as template.

## Code Style Guide

### TypeScript Conventions

```typescript
// Use interface for objects
interface User {
  id: number;
  email: string;
}

// Use type for unions/primitives
type Status = 'pending' | 'active' | 'inactive';

// Always type function parameters and returns
const formatCurrency = (amount: number): string => {
  return `$${amount.toFixed(2)}`;
};

// Use const for non-reassigned variables
const API_URL = process.env.REACT_APP_API_URL;

// Use arrow functions
const handleClick = () => {
  console.log('Clicked');
};
```

### React Conventions

```typescript
// Use functional components with TypeScript
interface ProductCardProps {
  product: Product;
  onAddToCart: (product: Product) => void;
}

const ProductCard: React.FC<ProductCardProps> = ({ product, onAddToCart }) => {
  return (
    <Card>
      <Typography>{product.name}</Typography>
      <Button onClick={() => onAddToCart(product)}>Add to Cart</Button>
    </Card>
  );
};

export default ProductCard;
```

### File Organization

```typescript
// Order imports: external, internal, relative
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Box } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../redux/hooks';
import { fetchProducts } from '../features/products/productSlice';
import ProductCard from '../components/ProductCard';
import { Product } from '../types';
```

## Adding New Features

### 1. Add New Page

**Step 1: Create page component**
```typescript
// src/pages/WishlistPage.tsx
import React from 'react';
import { Container, Typography } from '@mui/material';

const WishlistPage: React.FC = () => {
  return (
    <Container>
      <Typography variant="h4">My Wishlist</Typography>
    </Container>
  );
};

export default WishlistPage;
```

**Step 2: Add route**
```typescript
// src/App.tsx
import WishlistPage from './pages/WishlistPage';

// In Routes:
<Route
  path="/wishlist"
  element={
    <PrivateRoute>
      <WishlistPage />
    </PrivateRoute>
  }
/>
```

**Step 3: Add to navigation**
```typescript
// src/components/Navbar.tsx
<Button onClick={() => navigate('/wishlist')}>
  Wishlist
</Button>
```

### 2. Add Redux Slice

**Step 1: Create slice**
```typescript
// src/features/wishlist/wishlistSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

interface WishlistState {
  items: Product[];
  loading: boolean;
}

const initialState: WishlistState = {
  items: [],
  loading: false,
};

export const fetchWishlist = createAsyncThunk(
  'wishlist/fetch',
  async () => {
    // API call here
  }
);

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    addToWishlist: (state, action) => {
      state.items.push(action.payload);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchWishlist.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.loading = false;
        state.items = action.payload;
      });
  },
});

export const { addToWishlist } = wishlistSlice.actions;
export default wishlistSlice.reducer;
```

**Step 2: Add to store**
```typescript
// src/redux/store.ts
import wishlistReducer from '../features/wishlist/wishlistSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    products: productReducer,
    orders: orderReducer,
    wishlist: wishlistReducer, // Add this
  },
});
```

### 3. Add API Endpoint

**Step 1: Add to endpoints.ts**
```typescript
// src/api/endpoints.ts
export const wishlistAPI = {
  getWishlist: async (): Promise<Product[]> => {
    const response = await axiosInstance.get('/wishlist');
    return response.data;
  },

  addToWishlist: async (productId: number): Promise<void> => {
    await axiosInstance.post('/wishlist', { productId });
  },

  removeFromWishlist: async (productId: number): Promise<void> => {
    await axiosInstance.delete(`/wishlist/${productId}`);
  },
};
```

**Step 2: Use in component**
```typescript
import { wishlistAPI } from '../api/endpoints';

const handleAddToWishlist = async (productId: number) => {
  try {
    await wishlistAPI.addToWishlist(productId);
    toast.success('Added to wishlist');
  } catch (error) {
    toast.error('Failed to add to wishlist');
  }
};
```

## State Management Patterns

### Using Redux Hooks

```typescript
import { useAppDispatch, useAppSelector } from '../redux/hooks';
import { fetchProducts } from '../features/products/productSlice';

const ProductsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { products, loading } = useAppSelector((state) => state.products);

  useEffect(() => {
    dispatch(fetchProducts({ page: 0, size: 20 }));
  }, [dispatch]);

  return <div>{/* Render products */}</div>;
};
```

### Local State vs Redux

**Use Local State for:**
- Form inputs
- UI toggles (modals, dropdowns)
- Component-specific data

**Use Redux for:**
- User authentication
- Shared data (products, orders)
- Data from API
- Data needed across pages

### Async Operations

```typescript
// In component
const handleSubmit = async (values: FormValues) => {
  const result = await dispatch(createProduct(values));

  if (createProduct.fulfilled.match(result)) {
    toast.success('Product created');
  } else {
    toast.error('Failed to create product');
  }
};
```

## API Integration

### Error Handling Pattern

```typescript
// In async thunk
export const fetchProducts = createAsyncThunk(
  'products/fetchProducts',
  async (params, { rejectWithValue }) => {
    try {
      const response = await productAPI.getProducts(params.page, params.size);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch products');
    }
  }
);
```

### Authentication Token

Automatically handled by axios interceptor:
```typescript
// src/api/axiosInstance.ts
axiosInstance.interceptors.request.use(
  (config) => {
    const token = storage.get<string>(TOKEN_KEY);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
);
```

## Component Development

### Form with Validation

```typescript
import { useFormik } from 'formik';
import * as Yup from 'yup';

const validationSchema = Yup.object({
  name: Yup.string().required('Name is required'),
  email: Yup.string().email('Invalid email').required('Email is required'),
});

const MyForm: React.FC = () => {
  const formik = useFormik({
    initialValues: {
      name: '',
      email: '',
    },
    validationSchema,
    onSubmit: (values) => {
      console.log(values);
    },
  });

  return (
    <form onSubmit={formik.handleSubmit}>
      <TextField
        name="name"
        value={formik.values.name}
        onChange={formik.handleChange}
        onBlur={formik.handleBlur}
        error={formik.touched.name && Boolean(formik.errors.name)}
        helperText={formik.touched.name && formik.errors.name}
      />
      <Button type="submit">Submit</Button>
    </form>
  );
};
```

### Reusable Component Pattern

```typescript
interface CardProps {
  title: string;
  description: string;
  actions?: React.ReactNode;
  loading?: boolean;
}

const CustomCard: React.FC<CardProps> = ({
  title,
  description,
  actions,
  loading = false,
}) => {
  if (loading) return <LoadingSpinner />;

  return (
    <Card>
      <CardContent>
        <Typography variant="h6">{title}</Typography>
        <Typography>{description}</Typography>
      </CardContent>
      {actions && <CardActions>{actions}</CardActions>}
    </Card>
  );
};
```

## Testing Strategy

### Component Test Example

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { store } from '../redux/store';
import ProductCard from './ProductCard';

const mockProduct = {
  id: 1,
  name: 'Test Product',
  price: 99.99,
  stockQuantity: 10,
};

const renderWithProviders = (component: React.ReactElement) => {
  return render(
    <Provider store={store}>
      <BrowserRouter>{component}</BrowserRouter>
    </Provider>
  );
};

describe('ProductCard', () => {
  test('renders product name', () => {
    renderWithProviders(<ProductCard product={mockProduct} />);
    expect(screen.getByText('Test Product')).toBeInTheDocument();
  });

  test('calls onAddToCart when button clicked', () => {
    const mockAddToCart = jest.fn();
    renderWithProviders(
      <ProductCard product={mockProduct} onAddToCart={mockAddToCart} />
    );

    fireEvent.click(screen.getByText('Add to Cart'));
    expect(mockAddToCart).toHaveBeenCalledWith(mockProduct);
  });
});
```

### Redux Test Example

```typescript
import reducer, { addToCart } from './cartSlice';

describe('cartSlice', () => {
  test('should add item to cart', () => {
    const initialState = { items: [] };
    const product = { id: 1, name: 'Product' };

    const state = reducer(initialState, addToCart(product));

    expect(state.items).toHaveLength(1);
    expect(state.items[0]).toEqual(product);
  });
});
```

## Common Tasks

### Add New Product Category

```typescript
// src/utils/constants.ts
export const PRODUCT_CATEGORIES = [
  'Electronics',
  'Clothing',
  'Books',
  'New Category', // Add here
];
```

### Change Theme Colors

```typescript
// src/App.tsx
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2', // Change this
    },
    secondary: {
      main: '#dc004e', // Change this
    },
  },
});
```

### Add Order Status

```typescript
// src/types/index.ts
export enum OrderStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  NEW_STATUS = 'NEW_STATUS', // Add here
}

// src/utils/constants.ts
export const ORDER_STATUS_COLORS: Record<string, string> = {
  // ... existing
  NEW_STATUS: '#FF5722', // Add color
};
```

## Debugging Tips

### Redux DevTools

1. Install Redux DevTools Extension
2. Open DevTools
3. Select Redux tab
4. Inspect actions and state changes

### Network Debugging

```typescript
// Enable axios request logging
axiosInstance.interceptors.request.use(
  (config) => {
    console.log('Request:', config.method?.toUpperCase(), config.url);
    return config;
  }
);

axiosInstance.interceptors.response.use(
  (response) => {
    console.log('Response:', response.status, response.data);
    return response;
  }
);
```

### React DevTools

1. Install React Developer Tools extension
2. Inspect component props and state
3. Use Profiler for performance issues

## Performance Optimization

### Memoization

```typescript
import { useMemo, useCallback } from 'react';

const ExpensiveComponent: React.FC<Props> = ({ data }) => {
  // Memoize expensive calculations
  const processedData = useMemo(() => {
    return data.map(item => expensiveOperation(item));
  }, [data]);

  // Memoize callbacks
  const handleClick = useCallback(() => {
    console.log('Clicked');
  }, []);

  return <div>{/* ... */}</div>;
};
```

### Code Splitting

```typescript
import React, { lazy, Suspense } from 'react';

const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));

<Suspense fallback={<LoadingSpinner />}>
  <AdminDashboard />
</Suspense>
```

### Image Optimization

```typescript
// Use next-gen formats
<img
  src="image.webp"
  alt="Product"
  loading="lazy"
  width={300}
  height={300}
/>
```

## Best Practices

1. **Always type your props and state**
2. **Keep components small and focused**
3. **Extract reusable logic to custom hooks**
4. **Use constants instead of magic strings**
5. **Handle errors gracefully**
6. **Write meaningful commit messages**
7. **Test critical user flows**
8. **Keep bundle size small**
9. **Use semantic HTML**
10. **Follow accessibility guidelines**

## Resources

- [React Documentation](https://react.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Redux Toolkit](https://redux-toolkit.js.org/)
- [Material-UI](https://mui.com/)
- [React Router](https://reactrouter.com/)

---

**Happy Coding!** Refer to this guide when adding new features or debugging issues.
