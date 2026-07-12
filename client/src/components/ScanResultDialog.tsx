import { Product } from '../types'
import ManualEntry from './ManualEntry'

interface ScanResultDialogProps {
  mode: 'found' | 'not-found'
  product?: Product
  barcode: string
  onAddStock: () => void
  onViewDetail: () => void
  onAddProduct: () => void
  onRescan: () => void
}

export default function ScanResultDialog({ mode, product, barcode, onAddStock, onViewDetail, onAddProduct, onRescan }: ScanResultDialogProps) {
  return (
    <div className="scan-result-dialog">
      {mode === 'found' && product ? (
        <>
          <div className="scan-result-title">Produk ditemukan: {product.name}</div>
          <div className="scan-result-buttons">
            <button className="btn btn-primary" onClick={onAddStock}>Tambah 1 stok</button>
            <button className="btn" onClick={onViewDetail}>Lihat detail</button>
            <button className="btn" onClick={onRescan}>Pindai lagi</button>
          </div>
        </>
      ) : (
        <>
          <div className="scan-result-title">Barcode tidak dikenal: {barcode}</div>
          <div className="scan-result-buttons">
            <button className="btn btn-primary" onClick={onAddProduct}>Tambah produk baru</button>
            <button className="btn" onClick={onRescan}>Pindai lagi</button>
          </div>
        </>
      )}
      <ManualEntry onLookup={(p) => {}} />
    </div>
  )
}