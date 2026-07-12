import { useState, useEffect, useCallback } from 'react'
import { useAuth } from './context/AuthContext'
import { fetchProduct, searchProducts, createProduct, getSetupStatus, updateStock } from './api'
import type { Product } from './types'
import Sidebar from './components/Sidebar'
import Dashboard from './components/Dashboard'
import ProductList from './components/ProductList'
import SearchBar from './components/SearchBar'
import ProductCard from './components/ProductCard'
import ProductForm from './components/ProductForm'
import StockControls from './components/StockControls'
import Viewfinder from './components/Viewfinder'
import ManualEntry from './components/ManualEntry'
import Login from './components/Login'
import ProtectedRoute from './components/ProtectedRoute'
import SetupPage from './components/SetupPage'
import UserList from './components/UserList'
import ScanResultDialog from './components/ScanResultDialog'
import './index.css'

type View = 'dashboard' | 'products' | 'scan' | 'detail' | 'create' | 'users' | 'scan-result'

function MainApp() {
  const { user, logout } = useAuth()
  const [view, setView] = useState<View>(() => {
    if (!user) return 'dashboard'
    // Role-appropriate initial view
    switch (user.role) {
      case 'staff': return 'products'
      case 'manager': return 'users'
      case 'admin': return 'products'
      default: return 'dashboard'
    }
  })
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [scannedBarcode, setScannedBarcode] = useState<string | null>(null)
  const [searchResults, setSearchResults] = useState<Product[] | null>(null)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [autoReturn, setAutoReturn] = useState(0)
  const [scanResult, setScanResult] = useState<{ product?: Product; barcode: string } | null>(null)

  useEffect(() => {
    if (view === 'products') setSearchResults(null)
  }, [view])

  // Auto-return to scan after 3s on scanned detail
  useEffect(() => {
    if (autoReturn <= 0) return
    const id = setTimeout(() => {
      if (autoReturn <= 1) {
        setAutoReturn(0)
        setView('scan')
      } else {
        setAutoReturn(autoReturn - 1)
      }
    }, 1000)
    return () => clearTimeout(id)
  }, [autoReturn])

  const navigate = useCallback((newView: View, data?: { product?: Product; barcode?: string }) => {
    setAutoReturn(0)
    setView(newView)
    if (data?.product) setSelectedProduct(data.product)
    if (data?.barcode) setScannedBarcode(data.barcode)
  }, [])

  const handleSearch = useCallback((results: Product[] | null) => {
    setSearchResults(results)
  }, [])

  const handleScan = useCallback(async (barcode: string) => {
    const product = await fetchProduct(barcode)
    setScanResult(product ? { product, barcode } : { barcode })
    setView('scan-result')
  }, [])

  const handleAddStock = useCallback(async () => {
    if (!scanResult) return
    await updateStock(scanResult.barcode, { delta: 1 })
    setView('scan')
  }, [scanResult])

  const handleViewDetail = useCallback(() => {
    if (!scanResult?.product) return
    setSelectedProduct(scanResult.product)
    setView('detail')
  }, [scanResult])

  const handleAddProduct = useCallback(() => {
    if (!scanResult) return
    setScannedBarcode(scanResult.barcode)
    setView('create')
  }, [scanResult])

  const handleRescan = useCallback(() => {
    setView('scan')
  }, [])

  const handleProductCreated = useCallback((product: Product) => {
    setSelectedProduct(product)
    setView('detail')
  }, [])

  const handleStockUpdated = useCallback((updated: { stock: number }) => {
    setSelectedProduct(prev => prev ? { ...prev, stock: updated.stock } : null)
  }, [])

  const renderProducts = () => (
    <div className="home-screen">
      <SearchBar onSearch={handleSearch} />
      <ProductList
        searchResults={searchResults}
        onSelect={(p) => navigate('detail', { product: p })}
      />
    </div>
  )

  const renderScan = () => (
    <div className="scan-screen">
      <Viewfinder onScan={handleScan} />
      <ManualEntry onLookup={(p) => navigate('detail', { product: p })} />
      <button className="btn btn-back" onClick={() => navigate('products')}>Cancel</button>
    </div>
  )

  const renderDetail = () => (
    <div className="detail-screen">
      {selectedProduct && (
        <>
          <ProductCard product={selectedProduct} />
          <StockControls product={selectedProduct} onUpdate={handleStockUpdated} />
          <div className="detail-actions">
            <button className="btn btn-secondary" onClick={() => navigate('scan')}>Scan Another</button>
            <button className="btn btn-secondary" onClick={() => navigate('products')}>Back to List</button>
          </div>
          {autoReturn > 0 && (
            <div className="auto-return-bar">
              <span>Returning to scan in {autoReturn}...</span>
              <button className="btn btn-secondary" onClick={() => setAutoReturn(0)}>Stay</button>
            </div>
          )}
        </>
      )}
    </div>
  )

  const renderCreate = () => (
    <div className="create-screen">
      <ProductForm
        barcode={scannedBarcode}
        onCreated={handleProductCreated}
        onCancel={() => navigate('scan')}
      />
    </div>
  )

  const renderUsers = () => (
    <div className="user-screen">
      <UserList />
    </div>
  )

  return (
    <div className="app-layout">
      <Sidebar view={view} onNavigate={navigate} open={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />
      <div className="app-main-area">
        <div className="mobile-topbar">
          <button className="hamburger" onClick={() => setSidebarOpen(true)}>
            <span /><span /><span />
          </button>
          <h1 className="mobile-title">Makmur</h1>
        </div>
        <main className="app-content">
          {view === 'dashboard' && <Dashboard />}
          {view === 'products' && renderProducts()}
          {view === 'scan' && renderScan()}
          {view === 'detail' && renderDetail()}
          {view === 'create' && renderCreate()}
          {view === 'users' && renderUsers()}
          {view === 'scan-result' && scanResult && (
            <ScanResultDialog
              mode={scanResult.product ? 'found' : 'not-found'}
              product={scanResult.product}
              barcode={scanResult.barcode}
              onAddStock={handleAddStock}
              onViewDetail={handleViewDetail}
              onAddProduct={handleAddProduct}
              onRescan={handleRescan}
            />
          )}
        </main>
      </div>
    </div>
  )
}

function LoginGate() {
  const { user, token } = useAuth()
  const [checking, setChecking] = useState(true)

  // /setup route — no auth required
  if (window.location.pathname === '/setup') {
    return <div className="app"><SetupPage /></div>
  }

  useEffect(() => {
    if (token) {
      setChecking(false)
      return
    }
    getSetupStatus()
      .then(() => setChecking(false))
      .catch(() => setChecking(false))
  }, [token])

  if (checking) {
    return <div className="app"><div className="loading-screen">Memuat...</div></div>
  }

  if (token) {
    return (
      <ProtectedRoute>
        <MainApp />
      </ProtectedRoute>
    )
  }

  return (
    <div className="app">
      <Login onSuccess={() => {}} />
    </div>
  )
}

export default function App() {
  return <LoginGate />
}