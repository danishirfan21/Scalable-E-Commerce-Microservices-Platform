# Quick Start Guide

## Prerequisites Checklist

- [ ] Node.js 18+ installed
- [ ] npm installed
- [ ] Backend API running on port 8080

## Quick Setup (5 minutes)

### 1. Install Dependencies

```bash
cd frontend
npm install
```

This will install all required packages (~2-3 minutes).

### 2. Configure Environment

The `.env` file is already created with default values:
```env
REACT_APP_API_URL=http://localhost:8080/api
```

If your backend is running on a different port, edit `.env` file.

### 3. Start Development Server

```bash
npm start
```

The app will automatically open at [http://localhost:3000](http://localhost:3000)

### 4. Login

Use these default credentials (after backend setup):

**Admin:**
- Email: `admin@example.com`
- Password: `Admin123!`

**User:**
- Email: `user@example.com`
- Password: `User123!`

## Common Issues

### Issue: "Cannot connect to backend"

**Solution:** Ensure backend is running:
```bash
# In backend directory
./mvnw spring-boot:run
```

### Issue: "Port 3000 already in use"

**Solution:** Kill the process or use a different port:
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:3000 | xargs kill -9
```

### Issue: CORS errors

**Solution:** Backend must allow frontend origin. Check backend CORS configuration.

## Features to Try

1. **Browse Products** - Visit Products page
2. **Add to Cart** - Click "Add to Cart" on any product
3. **Place Order** - Complete checkout with shipping address
4. **View Orders** - Check "My Orders" page
5. **Edit Profile** - Update your profile information

### Admin Features

1. **Manage Products** - Create, edit, delete products
2. **Manage Orders** - View all orders and update status
3. **Update Order Status** - Change order from PENDING to DELIVERED

## Build for Production

```bash
npm run build
```

Creates optimized production build in `build/` folder.

## Docker Quick Start

```bash
# Build image
docker build -t ecommerce-frontend .

# Run container
docker run -p 80:80 ecommerce-frontend

# Access at http://localhost
```

## Development Workflow

1. **Make changes** in `src/` files
2. **Save** - Hot reload will update browser
3. **Test** - Run `npm test` for tests
4. **Lint** - Run `npm run lint` to check code quality

## Project Structure Overview

```
src/
├── api/          → Backend API calls
├── components/   → Reusable UI components
├── features/     → Redux slices (state management)
├── pages/        → Page components
├── redux/        → Redux store configuration
├── types/        → TypeScript interfaces
├── utils/        → Helper functions & constants
└── App.tsx       → Main app with routing
```

## Next Steps

- Read [README.md](README.md) for detailed documentation
- Check TypeScript types in `src/types/index.ts`
- Explore Redux slices in `src/features/`
- Review API endpoints in `src/api/endpoints.ts`

## Getting Help

1. Check browser console for errors
2. Check Network tab for API call failures
3. Verify backend is responding: `curl http://localhost:8080/api/health`
4. Check Redux DevTools for state issues

---

**Ready to code!** Start by exploring the pages in `src/pages/`.
