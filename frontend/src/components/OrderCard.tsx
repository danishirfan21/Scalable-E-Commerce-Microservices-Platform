/**
 * Order card component for displaying order information
 */

import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Chip,
  Divider,
  List,
  ListItem,
  ListItemText,
} from '@mui/material';
import { Order, OrderStatus } from '../types';
import { formatCurrency, formatDate, generateOrderId } from '../utils/helpers';
import { ORDER_STATUS_COLORS } from '../utils/constants';

interface OrderCardProps {
  order: Order;
  showUserInfo?: boolean;
}

const OrderCard: React.FC<OrderCardProps> = ({ order, showUserInfo = false }) => {
  const getStatusColor = (status: OrderStatus): string => {
    return ORDER_STATUS_COLORS[status] || '#757575';
  };

  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        {/* Order Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" component="div">
            Order #{generateOrderId(order.id)}
          </Typography>
          <Chip
            label={order.status}
            sx={{
              bgcolor: getStatusColor(order.status),
              color: 'white',
              fontWeight: 'bold',
            }}
          />
        </Box>

        {/* Order Details */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Placed on: {formatDate(order.createdAt)}
          </Typography>
          {showUserInfo && (
            <Typography variant="body2" color="text.secondary" gutterBottom>
              User ID: {order.userId}
            </Typography>
          )}
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Shipping Address: {order.shippingAddress}
          </Typography>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Order Items */}
        <Typography variant="subtitle2" gutterBottom>
          Order Items:
        </Typography>
        <List dense disablePadding>
          {order.items.map((item, index) => (
            <ListItem key={index} disableGutters>
              <ListItemText
                primary={item.productName || `Product ID: ${item.productId}`}
                secondary={`Quantity: ${item.quantity} × ${formatCurrency(item.price)}`}
              />
              <Typography variant="body2" fontWeight="medium">
                {formatCurrency(item.quantity * item.price)}
              </Typography>
            </ListItem>
          ))}
        </List>

        <Divider sx={{ my: 2 }} />

        {/* Total Amount */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6" component="div">
            Total Amount:
          </Typography>
          <Typography variant="h6" component="div" color="primary" fontWeight="bold">
            {formatCurrency(order.totalAmount)}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default OrderCard;
