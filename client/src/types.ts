export interface Product {
  id: number;
  barcode: string;
  name: string;
  price: number;
  stock: number;
  created_at: string;
}

export interface ProductCreatePayload {
  barcode: string;
  name: string;
  price: number;
  stock: number;
}

export interface StockUpdatePayload {
  value?: number;
  delta?: number;
}

export interface StockUpdateResponse {
  barcode: string;
  stock: number;
  previous_stock: number;
}

export interface ApiError {
  error: string;
  message?: string;
  fields?: Record<string, string>;
  existing_product?: Product;
}

export interface ApiErrorWithStatus extends ApiError {
  status: number;
}

export interface LoginResponse {
  token: string;
  user: {
    id: number;
    username: string;
    role: string;
  };
}

export interface User {
  id: number;
  username: string;
  role: 'admin' | 'manager' | 'staff';
  active: boolean;
  created_at: string;
}

export interface SetupTokenResponse {
  token: string;
  expires_at: string;
}

export interface SetupStatusResponse {
  needsSetup: boolean;
}

export interface SetupRegisterPayload {
  token: string;
  username: string;
  password: string;
}

export interface UserCreatePayload {
  username: string;
  password: string;
  role?: 'staff' | 'manager';
}

export interface UserPasswordResetPayload {
  new_password: string;
}