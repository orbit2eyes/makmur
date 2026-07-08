import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import Dashboard from '../../components/Dashboard'

// Mock the API module
vi.mock('../../api', () => ({
  fetchProducts: vi.fn(),
}))

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders 3 summary cards with product data', async () => {
    const { fetchProducts } = await import('../../api')
    vi.mocked(fetchProducts).mockResolvedValue([
      { id: 1, barcode: '5901234567890', name: 'Milk', price: 12.5, stock: 42, created_at: '2026-01-01' },
      { id: 2, barcode: '5901234567891', name: 'Bread', price: 5.0, stock: 3, created_at: '2026-01-01' },
      { id: 3, barcode: '5901234567892', name: 'Eggs', price: 8.0, stock: 0, created_at: '2026-01-01' },
    ])

    render(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText('Total Products')).toBeInTheDocument()
      expect(screen.getByText('Total Stock')).toBeInTheDocument()
      expect(screen.getByText('Low Stock Items')).toBeInTheDocument()
    })

    // Values: 3 products, 42+3+0=45 stock, 2 low stock (<5)
    expect(screen.getAllByText('3').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('45')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('shows 0 values when fetch fails', async () => {
    const { fetchProducts } = await import('../../api')
    vi.mocked(fetchProducts).mockRejectedValue(new Error('Network error'))

    render(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText('Total Products')).toBeInTheDocument()
    })

    // All 3 cards show 0 on error
    const zeros = screen.getAllByText('0')
    expect(zeros).toHaveLength(3)
  })

  it('shows loading state initially', async () => {
    const { fetchProducts } = await import('../../api')
    vi.mocked(fetchProducts).mockReturnValue(new Promise(() => {})) // never resolves

    render(<Dashboard />)

    expect(screen.getByText('Loading...')).toBeInTheDocument()
  })
})