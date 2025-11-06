/**
 * Admin Orders page - View and manage all orders
 */

import React, { useEffect, useState } from 'react';
import {
  Container,
  Typography,
  Box,
  Alert,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  TextField,
} from '@mui/material';
import { useAppDispatch, useAppSelector } from '../redux/hooks';
import { fetchAllOrders, updateOrderStatus } from '../features/orders/orderSlice';
import { Order, OrderStatus } from '../types';
import OrderCard from '../components/OrderCard';
import LoadingSpinner from '../components/LoadingSpinner';

const AdminOrdersPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { orders, loading, error } = useAppSelector((state) => state.orders);

  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [newStatus, setNewStatus] = useState<OrderStatus>(OrderStatus.PENDING);

  useEffect(() => {
    dispatch(fetchAllOrders({ page: 0, size: 100 }));
  }, [dispatch]);

  const handleOpenStatusDialog = (order: Order) => {
    setSelectedOrder(order);
    setNewStatus(order.status);
    setStatusDialogOpen(true);
  };

  const handleCloseStatusDialog = () => {
    setStatusDialogOpen(false);
    setSelectedOrder(null);
  };

  const handleUpdateStatus = async () => {
    if (selectedOrder) {
      await dispatch(
        updateOrderStatus({
          orderId: selectedOrder.id,
          status: newStatus,
        })
      );
      handleCloseStatusDialog();
    }
  };

  if (loading && orders.length === 0) {
    return <LoadingSpinner message="Loading orders..." />;
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Manage Orders
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {orders.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary">
            No orders found
          </Typography>
        </Box>
      ) : (
        <Box sx={{ mt: 3 }}>
          {orders.map((order) => (
            <Box key={order.id} sx={{ position: 'relative', mb: 3 }}>
              <OrderCard order={order} showUserInfo />
              <Box
                sx={{
                  position: 'absolute',
                  top: 16,
                  right: 16,
                }}
              >
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => handleOpenStatusDialog(order)}
                >
                  Update Status
                </Button>
              </Box>
            </Box>
          ))}
        </Box>
      )}

      {/* Update Status Dialog */}
      <Dialog open={statusDialogOpen} onClose={handleCloseStatusDialog} maxWidth="xs" fullWidth>
        <DialogTitle>Update Order Status</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Order #{selectedOrder?.id}
          </Typography>
          <TextField
            fullWidth
            select
            label="New Status"
            value={newStatus}
            onChange={(e) => setNewStatus(e.target.value as OrderStatus)}
            sx={{ mt: 2 }}
          >
            {Object.values(OrderStatus).map((status) => (
              <MenuItem key={status} value={status}>
                {status}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseStatusDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleUpdateStatus}
            disabled={!selectedOrder || newStatus === selectedOrder.status}
          >
            Update
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default AdminOrdersPage;
