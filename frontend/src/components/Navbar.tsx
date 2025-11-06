/**
 * Navigation bar component with role-based menu
 */

import React, { useState } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  IconButton,
  Menu,
  MenuItem,
  Box,
  Container,
  Avatar,
  Tooltip,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ShoppingCart,
  Person,
  AdminPanelSettings,
  Logout,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../redux/hooks';
import { logout } from '../features/auth/authSlice';
import { ROUTES, APP_NAME } from '../utils/constants';
import { isAdmin } from '../utils/helpers';

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { isAuthenticated, user } = useAppSelector((state) => state.auth);

  const [anchorElNav, setAnchorElNav] = useState<null | HTMLElement>(null);
  const [anchorElUser, setAnchorElUser] = useState<null | HTMLElement>(null);

  const handleOpenNavMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElNav(event.currentTarget);
  };

  const handleOpenUserMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElUser(event.currentTarget);
  };

  const handleCloseNavMenu = () => {
    setAnchorElNav(null);
  };

  const handleCloseUserMenu = () => {
    setAnchorElUser(null);
  };

  const handleLogout = async () => {
    await dispatch(logout());
    handleCloseUserMenu();
    navigate(ROUTES.LOGIN);
  };

  const handleNavigation = (route: string) => {
    navigate(route);
    handleCloseNavMenu();
    handleCloseUserMenu();
  };

  return (
    <AppBar position="sticky">
      <Container maxWidth="xl">
        <Toolbar disableGutters>
          {/* Logo - Desktop */}
          <ShoppingCart sx={{ display: { xs: 'none', md: 'flex' }, mr: 1 }} />
          <Typography
            variant="h6"
            noWrap
            component="div"
            sx={{
              mr: 2,
              display: { xs: 'none', md: 'flex' },
              fontWeight: 700,
              letterSpacing: '.1rem',
              color: 'inherit',
              textDecoration: 'none',
              cursor: 'pointer',
            }}
            onClick={() => navigate(ROUTES.HOME)}
          >
            {APP_NAME}
          </Typography>

          {/* Mobile Menu */}
          {isAuthenticated && (
            <Box sx={{ flexGrow: 1, display: { xs: 'flex', md: 'none' } }}>
              <IconButton
                size="large"
                aria-label="menu"
                aria-controls="menu-appbar"
                aria-haspopup="true"
                onClick={handleOpenNavMenu}
                color="inherit"
              >
                <MenuIcon />
              </IconButton>
              <Menu
                id="menu-appbar"
                anchorEl={anchorElNav}
                anchorOrigin={{
                  vertical: 'bottom',
                  horizontal: 'left',
                }}
                keepMounted
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'left',
                }}
                open={Boolean(anchorElNav)}
                onClose={handleCloseNavMenu}
                sx={{
                  display: { xs: 'block', md: 'none' },
                }}
              >
                <MenuItem onClick={() => handleNavigation(ROUTES.PRODUCTS)}>
                  <Typography textAlign="center">Products</Typography>
                </MenuItem>
                <MenuItem onClick={() => handleNavigation(ROUTES.ORDERS)}>
                  <Typography textAlign="center">My Orders</Typography>
                </MenuItem>
                {user && isAdmin(user.role) && [
                  <MenuItem key="admin-products" onClick={() => handleNavigation(ROUTES.ADMIN_PRODUCTS)}>
                    <Typography textAlign="center">Manage Products</Typography>
                  </MenuItem>,
                  <MenuItem key="admin-orders" onClick={() => handleNavigation(ROUTES.ADMIN_ORDERS)}>
                    <Typography textAlign="center">Manage Orders</Typography>
                  </MenuItem>,
                ]}
              </Menu>
            </Box>
          )}

          {/* Logo - Mobile */}
          <ShoppingCart sx={{ display: { xs: 'flex', md: 'none' }, mr: 1 }} />
          <Typography
            variant="h6"
            noWrap
            component="div"
            sx={{
              mr: 2,
              display: { xs: 'flex', md: 'none' },
              flexGrow: 1,
              fontWeight: 700,
              letterSpacing: '.1rem',
              color: 'inherit',
              textDecoration: 'none',
              cursor: 'pointer',
            }}
            onClick={() => navigate(ROUTES.HOME)}
          >
            {APP_NAME}
          </Typography>

          {/* Desktop Menu */}
          {isAuthenticated && (
            <Box sx={{ flexGrow: 1, display: { xs: 'none', md: 'flex' } }}>
              <Button
                onClick={() => handleNavigation(ROUTES.PRODUCTS)}
                sx={{ my: 2, color: 'white', display: 'block' }}
              >
                Products
              </Button>
              <Button
                onClick={() => handleNavigation(ROUTES.ORDERS)}
                sx={{ my: 2, color: 'white', display: 'block' }}
              >
                My Orders
              </Button>
              {user && isAdmin(user.role) && (
                <>
                  <Button
                    onClick={() => handleNavigation(ROUTES.ADMIN_PRODUCTS)}
                    sx={{ my: 2, color: 'white', display: 'block' }}
                    startIcon={<AdminPanelSettings />}
                  >
                    Manage Products
                  </Button>
                  <Button
                    onClick={() => handleNavigation(ROUTES.ADMIN_ORDERS)}
                    sx={{ my: 2, color: 'white', display: 'block' }}
                    startIcon={<AdminPanelSettings />}
                  >
                    Manage Orders
                  </Button>
                </>
              )}
            </Box>
          )}

          {/* User Menu */}
          {isAuthenticated ? (
            <Box sx={{ flexGrow: 0 }}>
              <Tooltip title="Open settings">
                <IconButton onClick={handleOpenUserMenu} sx={{ p: 0 }}>
                  <Avatar alt={user?.firstName} sx={{ bgcolor: 'secondary.main' }}>
                    {user?.firstName?.charAt(0).toUpperCase()}
                  </Avatar>
                </IconButton>
              </Tooltip>
              <Menu
                sx={{ mt: '45px' }}
                id="menu-appbar"
                anchorEl={anchorElUser}
                anchorOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
                keepMounted
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
                open={Boolean(anchorElUser)}
                onClose={handleCloseUserMenu}
              >
                <MenuItem onClick={() => handleNavigation(ROUTES.PROFILE)}>
                  <Person sx={{ mr: 1 }} />
                  <Typography textAlign="center">Profile</Typography>
                </MenuItem>
                <MenuItem onClick={handleLogout}>
                  <Logout sx={{ mr: 1 }} />
                  <Typography textAlign="center">Logout</Typography>
                </MenuItem>
              </Menu>
            </Box>
          ) : (
            <Box sx={{ flexGrow: 0 }}>
              <Button color="inherit" onClick={() => navigate(ROUTES.LOGIN)}>
                Login
              </Button>
              <Button
                color="secondary"
                variant="contained"
                onClick={() => navigate(ROUTES.REGISTER)}
                sx={{ ml: 1 }}
              >
                Register
              </Button>
            </Box>
          )}
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Navbar;
