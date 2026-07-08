import { useState, useEffect } from 'react'
import { fetchProducts } from '../api'
import type { Product } from '../types'

interface ProductListProps {
  searchResults: Product[] | null
  onSelect: (product: Product) => void
}

export default function ProductList({ searchResults, onSelect }: ProductListProps) {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    fetchProducts()
      .then(setProducts)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const display = searchResults !== null ? searchResults : products
  const isLow = (s: number) => s < 5

  if (loading) return <div className="loading">Loading...</div>
  if (display.length === 0) return <div className="empty">No products yet</div>

  return (
    <div className="product-list">
      {display.map(p => (
        <div key={p.id} className={`product-row${isLow(p.stock) ? ' product-row-low' : ''}`} onClick={() => onSelect(p)}>
          <div className="product-row-info">
            <span className="product-row-name">{p.name}</span>
            <span className="product-row-barcode">{p.barcode}</span>
          </div>
          <div className="product-row-stats">
            <span className={`product-row-stock${isLow(p.stock) ? ' stock-low' : ''}`}>{p.stock}</span>
            <span className="product-row-price">${p.price.toFixed(2)}</span>
          </div>
        </div>
      ))}
    </div>
  )
}