import { useState } from 'react'
import { fetchProduct } from '../api'
import type { Product } from '../types'

interface ManualEntryProps {
  onLookup: (product: Product) => void
}

export default function ManualEntry({ onLookup }: ManualEntryProps) {
  const [barcode, setBarcode] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!/^\d{13}$/.test(barcode)) {
      setError('Enter a valid 13-digit EAN-13 barcode')
      return
    }
    setError('')
    const product = await fetchProduct(barcode)
    if (product) {
      onLookup(product)
    } else {
      setError('Product not found')
    }
  }

  return (
    <form className="manual-entry" onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Enter barcode number"
        value={barcode}
        onChange={e => setBarcode(e.target.value.replace(/\D/g, '').slice(0, 13))}
        className="form-input"
      />
      <button type="submit" className="btn btn-primary">Look Up</button>
      {error && <span className="form-error">{error}</span>}
    </form>
  )
}