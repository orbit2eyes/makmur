import { useState } from 'react'
import { createProduct } from '../api'
import type { Product } from '../types'
import type { ApiErrorWithStatus } from '../types'

interface ProductFormProps {
  barcode: string | null
  onCreated: (product: Product) => void
  onCancel: () => void
}

export default function ProductForm({ barcode, onCreated, onCancel }: ProductFormProps) {
  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [stock, setStock] = useState('0')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  const validate = () => {
    const e: Record<string, string> = {}
    if (!name.trim()) e.name = 'Name is required'
    const p = Number(price)
    if (!price || isNaN(p) || p <= 0) e.price = 'Price must be a positive number'
    const s = Number(stock)
    if (isNaN(s) || !Number.isInteger(s) || s < 0) e.stock = 'Stock must be a non-negative integer'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setSubmitting(true)
    try {
      const product = await createProduct({
        barcode: barcode || '',
        name: name.trim(),
        price: Number(price),
        stock: Number(stock)
      })
      onCreated(product)
    } catch (err: unknown) {
      const apiErr = err as ApiErrorWithStatus
      if (apiErr.status === 409) {
        if (apiErr.existing_product) onCreated(apiErr.existing_product)
      } else if (apiErr.status === 422) {
        setErrors(apiErr.fields || {})
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="product-form" onSubmit={handleSubmit}>
      <h2>Add Product</h2>
      <div className="form-group">
        <label>Barcode</label>
        <input type="text" value={barcode || ''} disabled className="form-input" />
      </div>
      <div className="form-group">
        <label>Name *</label>
        <input type="text" value={name} onChange={e => setName(e.target.value)} className="form-input" />
        {errors.name && <span className="form-error">{errors.name}</span>}
      </div>
      <div className="form-group">
        <label>Price *</label>
        <input type="number" step="0.01" min="0.01" value={price} onChange={e => setPrice(e.target.value)} className="form-input" />
        {errors.price && <span className="form-error">{errors.price}</span>}
      </div>
      <div className="form-group">
        <label>Initial Stock</label>
        <input type="number" min="0" step="1" value={stock} onChange={e => setStock(e.target.value)} className="form-input" />
        {errors.stock && <span className="form-error">{errors.stock}</span>}
      </div>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save'}
        </button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}