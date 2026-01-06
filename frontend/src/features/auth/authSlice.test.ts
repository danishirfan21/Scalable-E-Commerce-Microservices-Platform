/**
 * Tests for authSlice
 */

import authReducer, { clearError, login, logout } from './authSlice';
import { AuthState, UserRole } from '../../types';

describe('authSlice', () => {
  const initialState: AuthState = {
    user: null,
    token: null,
    isAuthenticated: false,
    loading: false,
    error: null,
  };

  test('should return the initial state', () => {
    expect(authReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  test('should handle clearError', () => {
    const stateWithError: AuthState = {
      ...initialState,
      error: 'Some error',
    };

    const actual = authReducer(stateWithError, clearError());
    expect(actual.error).toBeNull();
  });

  test('should handle login.pending', () => {
    const actual = authReducer(initialState, login.pending('', { email: '', password: '' }));
    expect(actual.loading).toBe(true);
    expect(actual.error).toBeNull();
  });

  test('should handle login.fulfilled', () => {
    const mockUser = {
      id: 1,
      email: 'test@example.com',
      username: 'testuser',
      firstName: 'Test',
      lastName: 'User',
      role: UserRole.USER,
    };

    const mockPayload = {
      token: 'mock-token',
      user: mockUser,
    };

    const actual = authReducer(
      initialState,
      login.fulfilled(mockPayload, '', { email: '', password: '' })
    );

    expect(actual.loading).toBe(false);
    expect(actual.isAuthenticated).toBe(true);
    expect(actual.user).toEqual(mockUser);
    expect(actual.token).toBe('mock-token');
    expect(actual.error).toBeNull();
  });

  test('should handle login.rejected', () => {
    const actual = authReducer(
      initialState,
      login.rejected(null, '', { email: '', password: '' }, 'Login failed')
    );

    expect(actual.loading).toBe(false);
    expect(actual.isAuthenticated).toBe(false);
    expect(actual.user).toBeNull();
    expect(actual.token).toBeNull();
    expect(actual.error).toBe('Login failed');
  });

  test('should handle logout.fulfilled', () => {
    const authenticatedState: AuthState = {
      user: {
        id: 1,
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        role: UserRole.USER,
      },
      token: 'mock-token',
      isAuthenticated: true,
      loading: false,
      error: null,
    };

    const actual = authReducer(authenticatedState, logout.fulfilled(null, '', undefined));

    expect(actual.loading).toBe(false);
    expect(actual.isAuthenticated).toBe(false);
    expect(actual.user).toBeNull();
    expect(actual.token).toBeNull();
    expect(actual.error).toBeNull();
  });
});
