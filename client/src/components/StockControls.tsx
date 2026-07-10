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
      if (apiErr.status === 422) setError('Tidak dapat memperbarui stok menjadi negatif')
    }
  }

  const handleDelta = (delta: number) => doUpdate({ delta })

  const handleAbsolute = async (e: React.FormEvent) => {
    e.preventDefault()
    const v = Number(value)
    if (isNaN(v) || !Number.isInteger(v) || v < 0) {
      setError('Masukkan bilangan bulat non-negatif')
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
          {showInput ? 'Batal' : 'Perbarui Stok'}
        </button>
        {flash && <span className="stock-updated">Tersimpan!</span>}
      </div>
      {showInput && (
        <form className="stock-absolute" onSubmit={handleAbsolute}>
          <input
            type="number"
            min="0"
            step="1"
            value={value}
            onChange={e => setValue(e.target.value)}
            placeholder="Jumlah stok baru"
            className="form-input"
            autoFocus
          />
          <button type="submit" className="btn btn-primary">Atur</button>
        </form>
      )}
      {error && <span className="form-error">{error}</span>}
    </div>
  )
}