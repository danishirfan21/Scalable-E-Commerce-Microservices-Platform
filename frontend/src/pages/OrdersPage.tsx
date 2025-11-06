/**
 * Orders page - View user's orders
 */

import React, { useEffect } from 'react';
import { Container, Typography, Box, Alert } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../redux/hooks';
import { fetchUserOrders } from '../features/orders/orderSlice';
import OrderCard from '../components/OrderCard';
import LoadingSpinner from '../components/LoadingSpinner';

const OrdersPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { orders, loading, error } = useAppSelector((state) => state.orders);

  useEffect(() => {
    dispatch(fetchUserOrders());
  }, [dispatch]);

  if (loading && orders.length === 0) {
    return <LoadingSpinner message="Loading your orders..." />;
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        My Orders
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {orders.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary">
            You haven't placed any orders yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Start shopping to see your orders here
          </Typography>
        </Box>
      ) : (
        <Box sx={{ mt: 3 }}>
          {orders.map((order) => (
            <OrderCard key={order.id} order={order} />
          ))}
        </Box>
      )}
    </Container>
  );
};

export default OrdersPage;
