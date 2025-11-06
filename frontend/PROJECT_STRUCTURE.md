# Frontend Project Structure

Complete file tree for the E-Commerce Platform frontend application.

```
frontend/
│
├── public/                          # Static public files
│   ├── index.html                  # Main HTML template
│   ├── manifest.json               # PWA manifest
│   └── robots.txt                  # SEO robots file
│
├── src/                            # Source code
│   │
│   ├── api/                        # API Layer
│   │   ├── axiosInstance.ts       # Axios config with interceptors
│   │   └── endpoints.ts           # API endpoint functions
│   │
│   ├── components/                 # Reusable Components
│   │   ├── LoadingSpinner.tsx     # Loading indicator component
│   │   ├── Navbar.tsx             # Navigation bar with auth menu
│   │   ├── OrderCard.tsx          # Order display card
│   │   ├── PrivateRoute.tsx       # Protected route wrapper
│   │   └── ProductCard.tsx        # Product display card
│   │
│   ├── features/                   # Redux Features (Slices)
│   │   │
│   │   ├── auth/                  # Authentication Feature
│   │   │   ├── authSlice.ts      # Auth state management
│   │   │   └── authSlice.test.ts # Auth tests
│   │   │
│   │   ├── orders/                # Orders Feature
│   │   │   └── orderSlice.ts     # Orders state management
│   │   │
│   │   └── products/              # Products Feature
│   │       └── productSlice.ts   # Products state management
│   │
│   ├── pages/                      # Page Components
│   │   ├── AdminOrdersPage.tsx    # Admin: Manage all orders
│   │   ├── AdminProductsPage.tsx  # Admin: CRUD products
│   │   ├── LoginPage.tsx          # User login
│   │   ├── LoginPage.test.tsx     # Login page tests
│   │   ├── OrdersPage.tsx         # User: View my orders
│   │   ├── ProductsPage.tsx       # Browse & shop products
│   │   ├── ProfilePage.tsx        # User: Edit profile
│   │   └── RegisterPage.tsx       # User registration
│   │
│   ├── redux/                      # Redux Configuration
│   │   ├── hooks.ts               # Typed Redux hooks
│   │   └── store.ts               # Redux store setup
│   │
│   ├── types/                      # TypeScript Types
│   │   └── index.ts               # All interfaces & types
│   │
│   ├── utils/                      # Utilities
│   │   ├── constants.ts           # App constants & config
│   │   └── helpers.ts             # Helper functions
│   │
│   ├── App.tsx                     # Main app component
│   ├── index.tsx                   # Application entry point
│   └── setupTests.ts               # Jest test configuration
│
├── .dockerignore                   # Docker ignore file
├── .env                            # Environment variables (local)
├── .env.example                    # Environment template
├── .eslintrc.json                  # ESLint configuration
├── .gitignore                      # Git ignore file
├── .prettierrc                     # Prettier configuration
├── Dockerfile                      # Docker multi-stage build
├── jest.config.js                  # Jest configuration
├── nginx.conf                      # Nginx server config
├── package.json                    # Dependencies & scripts
├── PROJECT_STRUCTURE.md            # This file
├── QUICKSTART.md                   # Quick start guide
├── README.md                       # Main documentation
└── tsconfig.json                   # TypeScript configuration
```

## File Descriptions

### Configuration Files

| File | Purpose |
|------|---------|
| `package.json` | NPM dependencies, scripts, and project metadata |
| `tsconfig.json` | TypeScript compiler configuration with strict mode |
| `.eslintrc.json` | Code linting rules and style enforcement |
| `.prettierrc` | Code formatting rules |
| `jest.config.js` | Test runner configuration |
| `.env` | Environment variables (not in git) |
| `.env.example` | Template for environment variables |
| `Dockerfile` | Docker build instructions |
| `nginx.conf` | Nginx server configuration for production |

### API Layer (`src/api/`)

| File | Purpose |
|------|---------|
| `axiosInstance.ts` | Centralized Axios setup with request/response interceptors |
| `endpoints.ts` | All API endpoint functions (auth, users, products, orders) |

### Components (`src/components/`)

| Component | Purpose |
|-----------|---------|
| `LoadingSpinner.tsx` | Reusable loading indicator with optional message |
| `Navbar.tsx` | App navigation with role-based menu items |
| `OrderCard.tsx` | Display order details with status and items |
| `PrivateRoute.tsx` | HOC for protected routes (auth + role check) |
| `ProductCard.tsx` | Product display with add to cart or admin actions |

### Features/Redux (`src/features/`)

| Slice | Purpose |
|-------|---------|
| `auth/authSlice.ts` | User authentication, login, register, logout |
| `products/productSlice.ts` | Product CRUD operations and catalog |
| `orders/orderSlice.ts` | Order creation and management |

### Pages (`src/pages/`)

| Page | Route | Auth Required | Admin Only |
|------|-------|---------------|------------|
| `LoginPage.tsx` | `/login` | No | No |
| `RegisterPage.tsx` | `/register` | No | No |
| `ProductsPage.tsx` | `/products` | Yes | No |
| `OrdersPage.tsx` | `/orders` | Yes | No |
| `ProfilePage.tsx` | `/profile` | Yes | No |
| `AdminProductsPage.tsx` | `/admin/products` | Yes | Yes |
| `AdminOrdersPage.tsx` | `/admin/orders` | Yes | Yes |

### Redux (`src/redux/`)

| File | Purpose |
|------|---------|
| `store.ts` | Redux store configuration with all slices |
| `hooks.ts` | Typed useDispatch and useSelector hooks |

### Types (`src/types/`)

| File | Purpose |
|------|---------|
| `index.ts` | All TypeScript interfaces (User, Product, Order, etc.) |

### Utils (`src/utils/`)

| File | Purpose |
|------|---------|
| `constants.ts` | App constants (routes, API endpoints, categories) |
| `helpers.ts` | Utility functions (formatting, validation, storage) |

## Key Features by File

### Authentication Flow
- `src/features/auth/authSlice.ts` - State management
- `src/api/axiosInstance.ts` - Token injection
- `src/components/PrivateRoute.tsx` - Route protection
- `src/pages/LoginPage.tsx` - Login UI
- `src/pages/RegisterPage.tsx` - Registration UI

### Product Management
- `src/features/products/productSlice.ts` - State
- `src/pages/ProductsPage.tsx` - User view
- `src/pages/AdminProductsPage.tsx` - Admin CRUD
- `src/components/ProductCard.tsx` - Display

### Order Management
- `src/features/orders/orderSlice.ts` - State
- `src/pages/OrdersPage.tsx` - User orders
- `src/pages/AdminOrdersPage.tsx` - Admin view
- `src/components/OrderCard.tsx` - Display

## Code Organization Principles

1. **Separation of Concerns**: API, components, state, pages are clearly separated
2. **Feature-Based Structure**: Redux organized by feature (auth, products, orders)
3. **Reusability**: Common components extracted and parameterized
4. **Type Safety**: Comprehensive TypeScript types in dedicated folder
5. **Testing**: Test files co-located with source files

## File Naming Conventions

- **Components**: PascalCase (e.g., `ProductCard.tsx`)
- **Utilities**: camelCase (e.g., `helpers.ts`)
- **Tests**: `*.test.ts` or `*.test.tsx`
- **Types**: `index.ts` for barrel exports
- **Constants**: UPPER_SNAKE_CASE in files

## Import Path Aliases (Configured in tsconfig.json)

```typescript
import { User } from '@types/index';
import { useAppDispatch } from '@redux/hooks';
import axiosInstance from '@api/axiosInstance';
import ProductCard from '@components/ProductCard';
import { formatCurrency } from '@utils/helpers';
```

## Lines of Code by Category

| Category | Approximate LOC |
|----------|----------------|
| Redux (State) | ~800 lines |
| Pages | ~1,500 lines |
| Components | ~600 lines |
| API Layer | ~400 lines |
| Utils & Types | ~400 lines |
| Tests | ~200 lines |
| Config | ~300 lines |
| **Total** | **~4,200 lines** |

## Dependencies Summary

### Production Dependencies (13)
- React ecosystem: react, react-dom, react-router-dom
- State management: @reduxjs/toolkit, react-redux
- UI: @mui/material, @emotion/react, @emotion/styled
- Forms: formik, yup
- HTTP: axios
- Notifications: react-toastify
- Language: typescript

### Development Dependencies (15)
- Testing: @testing-library/react, @testing-library/jest-dom, jest
- Linting: eslint, @typescript-eslint/*
- Formatting: prettier
- Types: @types/*

## Build Output

```
build/
├── static/
│   ├── css/           # Minified CSS
│   ├── js/            # Minified JavaScript bundles
│   └── media/         # Optimized images/fonts
├── index.html         # Entry HTML
├── manifest.json      # PWA manifest
└── robots.txt         # SEO file
```

Typical production build size: ~500KB (gzipped)

---

**This structure follows React + TypeScript best practices for scalable applications.**
