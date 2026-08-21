/**
 * Redux slice for authentication state management
 */

import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { authAPI } from '../../api/endpoints';
import { AuthResponse, AuthState, LoginRequest, RegisterRequest, User } from '../../types';
import { TOKEN_KEY, USER_KEY } from '../../utils/constants';
import { storage } from '../../utils/helpers';
import { toast } from 'react-toastify';

/**
 * user-service's AuthResponse is flat (token/id/username/email/roles) and does not include
 * firstName/lastName - those are populated later from GET /users/profile. This adapts it into
 * the frontend's richer User shape used throughout the app.
 */
function toUser(auth: AuthResponse): User {
  return {
    id: auth.id,
    username: auth.username,
    email: auth.email,
    firstName: '',
    lastName: '',
    roles: auth.roles ?? [],
  };
}

// Load persisted state from localStorage
const token = storage.get<string>(TOKEN_KEY);
const user = storage.get<User>(USER_KEY);

const initialState: AuthState = {
  user: user,
  token: token,
  isAuthenticated: !!token,
  loading: false,
  error: null,
};

// ==================== Async Thunks ====================

/**
 * Login user
 */
export const login = createAsyncThunk(
  'auth/login',
  async (credentials: LoginRequest, { rejectWithValue }) => {
    try {
      const response = await authAPI.login(credentials);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Login failed');
    }
  }
);

/**
 * Register new user
 */
export const register = createAsyncThunk(
  'auth/register',
  async (userData: RegisterRequest, { rejectWithValue }) => {
    try {
      const response = await authAPI.register(userData);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Registration failed');
    }
  }
);

/**
 * Logout user
 */
export const logout = createAsyncThunk('auth/logout', async () => {
  try {
    await authAPI.logout();
  } catch (error: any) {
    // Continue with logout even if API call fails
    console.error('Logout API error:', error);
  }
  return null;
});

// ==================== Slice ====================

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    /**
     * Clear error message
     */
    clearError: (state) => {
      state.error = null;
    },

    /**
     * Update user data
     */
    updateUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      storage.set(USER_KEY, action.payload);
    },
  },
  extraReducers: (builder) => {
    // Login
    builder
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.isAuthenticated = true;
        const user = toUser(action.payload);
        state.user = user;
        state.token = action.payload.token;
        state.error = null;

        // Persist to localStorage
        storage.set(TOKEN_KEY, action.payload.token);
        storage.set(USER_KEY, user);

        toast.success(`Welcome back, ${user.username}!`);
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.error = action.payload as string;
      });

    // Register
    builder
      .addCase(register.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(register.fulfilled, (state, action) => {
        state.loading = false;
        state.isAuthenticated = true;
        const user = toUser(action.payload);
        state.user = user;
        state.token = action.payload.token;
        state.error = null;

        // Persist to localStorage
        storage.set(TOKEN_KEY, action.payload.token);
        storage.set(USER_KEY, user);

        toast.success('Registration successful! Welcome to our platform.');
      })
      .addCase(register.rejected, (state, action) => {
        state.loading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.error = action.payload as string;
      });

    // Logout
    builder
      .addCase(logout.pending, (state) => {
        state.loading = true;
      })
      .addCase(logout.fulfilled, (state) => {
        state.loading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.error = null;

        // Clear from localStorage
        storage.remove(TOKEN_KEY);
        storage.remove(USER_KEY);

        toast.info('You have been logged out.');
      })
      .addCase(logout.rejected, (state) => {
        // Clear state even if API call fails
        state.loading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.error = null;

        // Clear from localStorage
        storage.remove(TOKEN_KEY);
        storage.remove(USER_KEY);
      });
  },
});

export const { clearError, updateUser } = authSlice.actions;
export default authSlice.reducer;
