import { useState } from 'react'
import { updateStock } from '../api'
import type { Product } from '../types'

interface StockControlsProps {
  product: Product
  onUpdate: (result: { stock: number }) => void
}

export default function StockControls({ product, onUpdate }: StockControlsProps) {
  const [value, setValue] = useState('')
  const [showInput, setShowInput] = useState(false)
  const [flash, setFlash] = useState(false)
  const [error, setError] = useState('')

  const doUpdate = async (payload: { value?: number; delta?: number }) => {
    try {
      const result = await updateStock(product.barcode, payload)
      onUpdate(result)
      setFlash(true)
      setTimeout(() => setFlash(false), 1500)
      setError('')
    } catch (err: unknown) {
      const apiErr = err as { status?: number }
      if (apiErr.status === 422) setError('Cannot update stock to negative value')
    }
  }

  const handleDelta = (delta: number) => doUpdate({ delta })

  const handleAbsolute = async (e: React.FormEvent) => {
    e.preventDefault()
    const v = Number(value)
    if (isNaN(v) || !Number.isInteger(v) || v < 0) {
      setError('Enter a non-negative integer')
      return
    }
    setShowInput(false)
    setValue('')
    await doUpdate({ value: v })
  }

  return (
    <div className={`stock-controls ${flash ? 'stock-flash' : ''}`}>
      <div className="stock-buttons">
        <button className="btn btn-stock" onClick={() => handleDelta(1)}>+1</button>
        <button className="btn btn-stock" onClick={() => handleDelta(-1)} disabled={product.stock <= 0}>-1</button>
        <button className="btn btn-secondary" onClick={() => setShowInput(!showInput)}>
          {showInput ? 'Cancel' : 'Update Stock'}
        </button>
        {flash && <span className="stock-updated">Updated!</span>}
      </div>
      {showInput && (
        <form className="stock-absolute" onSubmit={handleAbsolute}>
          <input
            type="number"
            min="0"
            step="1"
            value={value}
            onChange={e => setValue(e.target.value)}
            placeholder="New stock count"
            className="form-input"
            autoFocus
          />
          <button type="submit" className="btn btn-primary">Set</button>
        </form>
      )}
      {error && <span className="form-error">{error}</span>}
    </div>
  )
}