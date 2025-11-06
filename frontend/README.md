# E-Commerce Platform - Frontend

A production-grade React + TypeScript frontend application for the E-Commerce microservices platform.

## Features

- **Modern Tech Stack**: React 18, TypeScript, Redux Toolkit, Material-UI
- **Authentication & Authorization**: JWT-based authentication with role-based access control
- **State Management**: Redux Toolkit with persisted authentication state
- **Form Validation**: Formik + Yup for robust form handling
- **API Integration**: Axios with interceptors for token management and error handling
- **Responsive Design**: Mobile-first design using Material-UI components
- **Toast Notifications**: User-friendly notifications for all actions
- **Protected Routes**: Role-based route protection for admin features
- **TypeScript**: Strict type checking for enhanced code quality
- **Testing**: Jest + React Testing Library setup
- **Production Ready**: Docker multi-stage build with nginx

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Redux Toolkit** - State management
- **React Router v6** - Routing
- **Material-UI (MUI)** - Component library
- **Axios** - HTTP client
- **Formik + Yup** - Form handling and validation
- **React Toastify** - Notifications
- **Jest + Testing Library** - Testing

## Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── api/
│   │   ├── axiosInstance.ts      # Axios configuration with interceptors
│   │   └── endpoints.ts           # API endpoint functions
│   ├── components/
│   │   ├── LoadingSpinner.tsx    # Loading indicator
│   │   ├── Navbar.tsx             # Navigation bar
│   │   ├── OrderCard.tsx          # Order display component
│   │   ├── PrivateRoute.tsx       # Protected route wrapper
│   │   └── ProductCard.tsx        # Product display component
│   ├── features/
│   │   ├── auth/
│   │   │   ├── authSlice.ts       # Auth Redux slice
│   │   │   └── authSlice.test.ts  # Auth tests
│   │   ├── orders/
│   │   │   └── orderSlice.ts      # Orders Redux slice
│   │   └── products/
│   │       └── productSlice.ts    # Products Redux slice
│   ├── pages/
│   │   ├── AdminOrdersPage.tsx    # Admin order management
│   │   ├── AdminProductsPage.tsx  # Admin product management
│   │   ├── LoginPage.tsx          # Login page
│   │   ├── LoginPage.test.tsx     # Login tests
│   │   ├── OrdersPage.tsx         # User orders page
│   │   ├── ProductsPage.tsx       # Products browsing page
│   │   ├── ProfilePage.tsx        # User profile page
│   │   └── RegisterPage.tsx       # Registration page
│   ├── redux/
│   │   ├── hooks.ts               # Typed Redux hooks
│   │   └── store.ts               # Redux store configuration
│   ├── types/
│   │   └── index.ts               # TypeScript interfaces
│   ├── utils/
│   │   ├── constants.ts           # Application constants
│   │   └── helpers.ts             # Utility functions
│   ├── App.tsx                    # Main app component
│   ├── index.tsx                  # Entry point
│   └── setupTests.ts              # Test configuration
├── .dockerignore
├── .env.example
├── .eslintrc.json
├── .gitignore
├── .prettierrc
├── Dockerfile
├── nginx.conf
├── package.json
├── tsconfig.json
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend API running on port 8080 (or configure REACT_APP_API_URL)

### Installation

1. **Clone the repository**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   ```

   Edit `.env` and update the API URL:
   ```env
   REACT_APP_API_URL=http://localhost:8080/api
   ```

4. **Start the development server**
   ```bash
   npm start
   ```

   The application will open at [http://localhost:3000](http://localhost:3000)

## Available Scripts

### Development

- `npm start` - Start development server (port 3000)
- `npm test` - Run tests in watch mode
- `npm run test:coverage` - Run tests with coverage report

### Production

- `npm run build` - Build production bundle
- `npm run lint` - Run ESLint
- `npm run lint:fix` - Fix ESLint issues
- `npm run format` - Format code with Prettier

## Docker Deployment

### Build Docker Image

```bash
docker build -t ecommerce-frontend:latest .
```

### Run Container

```bash
docker run -p 80:80 -d ecommerce-frontend:latest
```

The application will be available at [http://localhost](http://localhost)

### Environment Variables for Docker

To use different API URL in production, modify the build:

```bash
docker build --build-arg REACT_APP_API_URL=https://api.example.com/api -t ecommerce-frontend:latest .
```

## User Roles

### Regular User
- Browse products
- Add products to cart
- Place orders
- View order history
- Edit profile

### Admin User
- All user features
- Create/Edit/Delete products
- View all orders
- Update order status

## Default Test Credentials

After setting up the backend with sample data:

**Admin User:**
- Email: admin@example.com
- Password: Admin123!

**Regular User:**
- Email: user@example.com
- Password: User123!

## API Integration

The frontend communicates with the backend API using Axios:

### Endpoints

- **Auth**: `/api/auth/login`, `/api/auth/register`
- **Users**: `/api/users/profile`
- **Products**: `/api/products`, `/api/products/{id}`
- **Orders**: `/api/orders`, `/api/orders/user`, `/api/orders/{id}/status`

### Authentication Flow

1. User logs in with credentials
2. Backend returns JWT token
3. Token stored in localStorage
4. Axios interceptor adds token to all requests
5. On 401 response, user is logged out and redirected to login

## Form Validation

All forms use Formik and Yup for validation:

- **Email**: Valid email format
- **Password**: Min 6 characters, must contain uppercase, lowercase, and number
- **Required Fields**: All fields validated before submission

## State Management

Redux Toolkit manages application state:

- **Auth Slice**: User authentication and profile
- **Products Slice**: Product catalog and CRUD operations
- **Orders Slice**: Order management and status updates

State is persisted in localStorage for authentication.

## Responsive Design

The application is fully responsive using Material-UI's grid system:

- Mobile: Single column layout
- Tablet: 2-column product grid
- Desktop: 4-column product grid

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Testing

Run tests with coverage:

```bash
npm run test:coverage
```

### Test Files

- `src/pages/LoginPage.test.tsx` - Login component tests
- `src/features/auth/authSlice.test.ts` - Auth Redux slice tests

## Linting and Formatting

### ESLint

```bash
npm run lint
npm run lint:fix
```

### Prettier

```bash
npm run format
```

## Performance Optimizations

- Code splitting with React.lazy (if needed)
- Image lazy loading
- Gzip compression in nginx
- Static asset caching
- Production build minification

## Security Features

- JWT token stored in localStorage
- XSS protection headers
- CSRF protection via token-based auth
- Input validation and sanitization
- Role-based access control
- Secure nginx configuration

## Troubleshooting

### CORS Issues

If you encounter CORS errors, ensure the backend is configured to allow requests from the frontend origin:

```java
@CrossOrigin(origins = "http://localhost:3000")
```

### API Connection Issues

1. Verify backend is running: `curl http://localhost:8080/api/health`
2. Check `.env` file has correct API URL
3. Ensure no firewall blocking port 8080

### Build Errors

Clear cache and reinstall:
```bash
rm -rf node_modules package-lock.json
npm install
```

## Future Enhancements

- [ ] Shopping cart persistence
- [ ] Product search and filters
- [ ] Product reviews and ratings
- [ ] Payment integration
- [ ] Order tracking
- [ ] Email notifications
- [ ] Admin analytics dashboard
- [ ] Product image upload
- [ ] Wishlist functionality
- [ ] Multi-language support

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## License

This project is part of the E-Commerce Microservices Platform.

## Support

For issues and questions:
- Check existing GitHub issues
- Create a new issue with detailed description
- Include browser console logs for errors

---

**Built with React + TypeScript + Material-UI**
