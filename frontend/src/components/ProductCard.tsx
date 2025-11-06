/**
 * Product card component for displaying product information
 */

import React from 'react';
import {
  Card,
  CardMedia,
  CardContent,
  CardActions,
  Typography,
  Button,
  Box,
  Chip,
} from '@mui/material';
import { ShoppingCart, Edit, Delete } from '@mui/icons-material';
import { Product } from '../types';
import { formatCurrency } from '../utils/helpers';

interface ProductCardProps {
  product: Product;
  onAddToCart?: (product: Product) => void;
  onEdit?: (product: Product) => void;
  onDelete?: (product: Product) => void;
  isAdmin?: boolean;
}

const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onAddToCart,
  onEdit,
  onDelete,
  isAdmin = false,
}) => {
  const isOutOfStock = product.stockQuantity === 0;

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        transition: 'transform 0.2s',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: 6,
        },
      }}
    >
      <CardMedia
        component="img"
        height="200"
        image={
          product.imageUrl ||
          'https://via.placeholder.com/400x300.png?text=No+Image'
        }
        alt={product.name}
        sx={{ objectFit: 'cover' }}
      />
      <CardContent sx={{ flexGrow: 1 }}>
        <Typography gutterBottom variant="h6" component="div" noWrap>
          {product.name}
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            mb: 2,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
          }}
        >
          {product.description}
        </Typography>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6" color="primary">
            {formatCurrency(product.price)}
          </Typography>
          <Chip
            label={product.category}
            size="small"
            color="default"
            variant="outlined"
          />
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="body2" color="text.secondary">
            Stock: {product.stockQuantity}
          </Typography>
          {isOutOfStock && (
            <Chip label="Out of Stock" size="small" color="error" />
          )}
        </Box>
      </CardContent>
      <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
        {isAdmin ? (
          <>
            <Button
              size="small"
              variant="outlined"
              startIcon={<Edit />}
              onClick={() => onEdit?.(product)}
            >
              Edit
            </Button>
            <Button
              size="small"
              variant="outlined"
              color="error"
              startIcon={<Delete />}
              onClick={() => onDelete?.(product)}
            >
              Delete
            </Button>
          </>
        ) : (
          <Button
            size="small"
            variant="contained"
            fullWidth
            startIcon={<ShoppingCart />}
            onClick={() => onAddToCart?.(product)}
            disabled={isOutOfStock}
          >
            {isOutOfStock ? 'Out of Stock' : 'Add to Cart'}
          </Button>
        )}
      </CardActions>
    </Card>
  );
};

export default ProductCard;
