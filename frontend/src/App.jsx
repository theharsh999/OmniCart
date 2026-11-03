import { Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import DashboardLayout from './layouts/DashboardLayout'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import AdminDashboard from './pages/AdminDashboard'
import StaffDashboard from './pages/StaffDashboard'
import ProductsPage from './pages/ProductsPage'
import CategoriesPage from './pages/CategoriesPage'
import InventoryPage from './pages/InventoryPage'
import BillingPage from './pages/BillingPage'
import SalesPage from './pages/SalesPage'
import InvoicePage from './pages/InvoicePage'
import ProfilePage from './pages/ProfilePage'
import CashierManagementPage from './pages/CashierManagementPage'
import SalesAnalyticsPage from './pages/SalesAnalyticsPage'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route element={<ProtectedRoute roles={['ADMIN']} />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
            <Route path="/admin/analytics" element={<SalesAnalyticsPage />} />
            <Route path="/admin/products" element={<ProductsPage />} />
            <Route path="/admin/categories" element={<CategoriesPage />} />
            <Route path="/admin/cashiers" element={<CashierManagementPage />} />
          </Route>
          <Route element={<ProtectedRoute roles={['STAFF']} />}>
            <Route path="/staff/dashboard" element={<StaffDashboard />} />
          </Route>
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/billing" element={<BillingPage />} />
          <Route path="/sales" element={<SalesPage />} />
          <Route path="/sales/:id" element={<InvoicePage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
