/**
 * Products page - Browse and add products to cart
 */

import React, { useEffect, useState } from 'react';
import {
  Container,
  Typography,
  Grid,
  Box,
  TextField,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
} from '@mui/material';
import { ShoppingCart as ShoppingCartIcon } from '@mui/icons-material';
import { useAppDispatch, useAppSelector } from '../redux/hooks';
import { fetchProducts } from '../features/products/productSlice';
import { createOrder } from '../features/orders/orderSlice';
import { Product, OrderItem } from '../types';
import ProductCard from '../components/ProductCard';
import LoadingSpinner from '../components/LoadingSpinner';
import { toast } from 'react-toastify';

const ProductsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { products, loading, error } = useAppSelector((state) => state.products);

  const [cart, setCart] = useState<Map<number, { product: Product; quantity: number }>>(
    new Map()
  );
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [shippingAddress, setShippingAddress] = useState('');

  useEffect(() => {
    dispatch(fetchProducts({ page: 0, size: 100 }));
  }, [dispatch]);

  const handleAddToCart = (product: Product) => {
    const newCart = new Map(cart);
    const existing = newCart.get(product.id);

    if (existing) {
      if (existing.quantity < product.stockQuantity) {
        newCart.set(product.id, {
          product,
          quantity: existing.quantity + 1,
        });
        toast.success(`Added another ${product.name} to cart`);
      } else {
        toast.warning('Cannot add more than available stock');
        return;
      }
    } else {
      newCart.set(product.id, { product, quantity: 1 });
      toast.success(`${product.name} added to cart`);
    }

    setCart(newCart);
  };

  const handleRemoveFromCart = (productId: number) => {
    const newCart = new Map(cart);
    newCart.delete(productId);
    setCart(newCart);
    toast.info('Item removed from cart');
  };

  const handleUpdateQuantity = (productId: number, quantity: number) => {
    if (quantity < 1) {
      handleRemoveFromCart(productId);
      return;
    }

    const newCart = new Map(cart);
    const item = newCart.get(productId);
    if (item && quantity <= item.product.stockQuantity) {
      newCart.set(productId, { ...item, quantity });
      setCart(newCart);
    } else {
      toast.warning('Cannot exceed available stock');
    }
  };

  const calculateTotal = (): number => {
    let total = 0;
    cart.forEach((item) => {
      total += item.product.price * item.quantity;
    });
    return total;
  };

  const handleCheckout = async () => {
    if (!shippingAddress.trim()) {
      toast.error('Please enter a shipping address');
      return;
    }

    const items: OrderItem[] = Array.from(cart.values()).map((item) => ({
      productId: item.product.id,
      quantity: item.quantity,
      price: item.product.price,
    }));

    const result = await dispatch(
      createOrder({
        items,
        shippingAddress: shippingAddress.trim(),
      })
    );

    if (createOrder.fulfilled.match(result)) {
      setCart(new Map());
      setShippingAddress('');
      setCheckoutOpen(false);
    }
  };

  if (loading && products.length === 0) {
    return <LoadingSpinner message="Loading products..." />;
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1">
          Products
        </Typography>
        <Button
          variant="contained"
          startIcon={<ShoppingCartIcon />}
          onClick={() => setCheckoutOpen(true)}
          disabled={cart.size === 0}
        >
          Cart ({cart.size})
        </Button>
      </Box>

      {/* Error Message */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Products Grid */}
      {products.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary">
            No products available
          </Typography>
        </Box>
      ) : (
        <Grid container spacing={3}>
          {products.map((product) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={product.id}>
              <ProductCard product={product} onAddToCart={handleAddToCart} />
            </Grid>
          ))}
        </Grid>
      )}

      {/* Checkout Dialog */}
      <Dialog open={checkoutOpen} onClose={() => setCheckoutOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Checkout</DialogTitle>
        <DialogContent>
          <Typography variant="h6" gutterBottom>
            Cart Items
          </Typography>
          {Array.from(cart.values()).map((item) => (
            <Box
              key={item.product.id}
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                mb: 2,
                p: 2,
                border: '1px solid #e0e0e0',
                borderRadius: 1,
              }}
            >
              <Box>
                <Typography variant="body1">{item.product.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  ${item.product.price.toFixed(2)} each
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Button
                  size="small"
                  onClick={() => handleUpdateQuantity(item.product.id, item.quantity - 1)}
                >
                  -
                </Button>
                <Typography>{item.quantity}</Typography>
                <Button
                  size="small"
                  onClick={() => handleUpdateQuantity(item.product.id, item.quantity + 1)}
                >
                  +
                </Button>
                <Button
                  size="small"
                  color="error"
                  onClick={() => handleRemoveFromCart(item.product.id)}
                >
                  Remove
                </Button>
              </Box>
            </Box>
          ))}

          <Typography variant="h6" sx={{ mt: 3, mb: 2 }}>
            Total: ${calculateTotal().toFixed(2)}
          </Typography>

          <TextField
            fullWidth
            label="Shipping Address"
            multiline
            rows={3}
            value={shippingAddress}
            onChange={(e) => setShippingAddress(e.target.value)}
            placeholder="Enter your complete shipping address"
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCheckoutOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCheckout}
            disabled={cart.size === 0 || !shippingAddress.trim()}
          >
            Place Order
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ProductsPage;
