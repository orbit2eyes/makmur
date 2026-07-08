import type { Product } from '../types'

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
          <span className="detail-label">Stock</span>
          <span className="detail-value detail-stock">{product.stock}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Price</span>
          <span className="detail-value">${Number(product.price).toFixed(2)}</span>
        </div>
      </div>
    </div>
  )
}