import { type ReactNode } from 'react';
import { isTokenExpired, clearAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  children: ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const token = sessionStorage.getItem('token');

  if (!token) {
    return null; // App.tsx gate will handle redirect
  }

  if (isTokenExpired(token)) {
    clearAuth();
    window.location.href = '/login';
    return null;
  }

  return <>{children}</>;
}
