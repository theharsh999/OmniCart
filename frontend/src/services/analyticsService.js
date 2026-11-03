import api from './api'

/**
 * Service invoking Owner Sales Analytics backend endpoints.
 */

export async function fetchAnalyticsDashboard(params = {}) {
  const res = await api.get('/api/analytics/dashboard', { params })
  return res.data
}

export async function fetchAnalyticsSummary(params = {}) {
  const res = await api.get('/api/analytics/summary', { params })
  return res.data
}

export async function fetchRevenueTrend(params = {}) {
  const res = await api.get('/api/analytics/revenue-trend', { params })
  return res.data
}

export async function fetchPaymentBreakdown(params = {}) {
  const res = await api.get('/api/analytics/payment-breakdown', { params })
  return res.data
}

export async function fetchCategoryBreakdown(params = {}) {
  const res = await api.get('/api/analytics/category-breakdown', { params })
  return res.data
}

export async function fetchTopProducts(params = {}) {
  const res = await api.get('/api/analytics/top-products', { params })
  return res.data
}

export async function fetchCashierPerformance(params = {}) {
  const res = await api.get('/api/analytics/cashier-performance', { params })
  return res.data
}

export async function fetchRecentSales(limit = 8) {
  const res = await api.get('/api/analytics/recent-sales', { params: { limit } })
  return res.data
}


