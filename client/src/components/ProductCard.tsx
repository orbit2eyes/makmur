import type { Product } from '../types'
import { formatPrice } from '../utils'

interface ProductCardProps {
  product: Product
}

export default function ProductCard({ product }: ProductCardProps) {
  return (
    <div className="product-card">
      <h2 className="product-card-name">{product.name}</h2>
      <div className="product-card-details">
        <div className="detail-row">
          <span className="detail-label">Barcode</span>
          <span className="detail-value">{product.barcode}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Stok</span>
          <span className="detail-value detail-stock">{product.stock}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Harga</span>
          <span className="detail-value">{formatPrice(product.price)}</span>
        </div>
      </div>
    </div>
  )
}