import { useState, useEffect } from 'react'
import { fetchProducts } from '../api'
import type { Product } from '../types'

interface DashboardStats {
  totalProducts: number
  totalStock: number
  lowStockItems: number
}

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    fetchProducts()
      .then((products: Product[]) => {
        setStats({
          totalProducts: products.length,
          totalStock: products.reduce((sum, p) => sum + p.stock, 0),
          lowStockItems: products.filter(p => p.stock < 5).length,
        })
      })
      .catch(() => setStats({ totalProducts: 0, totalStock: 0, lowStockItems: 0 }))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="dashboard"><div className="loading">Loading...</div></div>

  return (
    <div className="dashboard">
      <h2 className="dashboard-heading">Dashboard</h2>
      <div className="dashboard-cards">
        <div className="dash-card">
          <span className="dash-card-icon">{String.fromCodePoint(0x1F4E6)}</span>
          <span className="dash-card-value">{stats?.totalProducts ?? 0}</span>
          <span className="dash-card-label">Total Products</span>
        </div>
        <div className="dash-card">
          <span className="dash-card-icon">{String.fromCodePoint(0x1F4CA)}</span>
          <span className="dash-card-value">{stats?.totalStock ?? 0}</span>
          <span className="dash-card-label">Total Stock</span>
        </div>
        <div className="dash-card">
          <span className="dash-card-icon">{String.fromCodePoint(0x26A0, 0xFE0F)}</span>
          <span className="dash-card-value">{stats?.lowStockItems ?? 0}</span>
          <span className="dash-card-label">Low Stock Items</span>
        </div>
      </div>
    </div>
  )
}