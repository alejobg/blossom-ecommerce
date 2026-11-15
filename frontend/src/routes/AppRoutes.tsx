import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../pages/LoginPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { ProductListPage } from '../pages/ProductListPage'
import { RegisterPage } from '../pages/RegisterPage'
import { CartPage } from '../pages/CartPage'
import { OrderHistoryPage } from '../pages/OrderHistoryPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { AppLayout } from '../components/AppLayout'
import { GuestRoute } from './GuestRoute'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRouteTree() {
  return (
    <Routes>
      <Route element={<GuestRoute />}>
        <Route element={<LoginPage />} path="/login" />
        <Route element={<RegisterPage />} path="/register" />
      </Route>
      <Route element={<AppLayout />}>
        <Route element={<ProductListPage />} path="/" />
        <Route element={<ProductListPage />} path="/products" />
        <Route element={<ProductDetailPage />} path="/products/:id" />
        <Route element={<CartPage />} path="/cart" />
        <Route element={<ProtectedRoute allowedRole="CUSTOMER" />}>
          <Route element={<OrderHistoryPage />} path="/orders" />
          <Route element={<OrderDetailPage />} path="/orders/:id" />
        </Route>
      </Route>
      <Route element={<Navigate replace to="/" />} path="*" />
    </Routes>
  )
}

export function AppRoutes() {
  return (
    <BrowserRouter>
      <AppRouteTree />
    </BrowserRouter>
  )
}
