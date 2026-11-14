import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../pages/LoginPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { ProductListPage } from '../pages/ProductListPage'
import { RegisterPage } from '../pages/RegisterPage'
import { AppLayout } from '../components/AppLayout'
import { GuestRoute } from './GuestRoute'

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
