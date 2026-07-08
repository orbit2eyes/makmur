import { useState, useRef, useCallback } from 'react'
import { searchProducts } from '../api'
import type { Product } from '../types'

interface SearchBarProps {
  onSearch: (results: Product[] | null) => void
}

export default function SearchBar({ onSearch }: SearchBarProps) {
  const [query, setQuery] = useState('')
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value
    setQuery(val)
    if (timer.current) clearTimeout(timer.current)
    if (val.length < 2) {
      onSearch(null)
      return
    }
    timer.current = setTimeout(async () => {
      try {
        const results = await searchProducts(val)
        onSearch(results)
      } catch { onSearch(null) }
    }, 200)
  }, [onSearch])

  const handleClear = useCallback(() => {
    setQuery('')
    onSearch(null)
    if (timer.current) clearTimeout(timer.current)
  }, [onSearch])

  return (
    <div className="search-bar">
      <input
        type="text"
        placeholder="Search products..."
        value={query}
        onChange={handleChange}
        className="search-input"
      />
      {query && (
        <button className="search-clear" onClick={handleClear}>&times;</button>
      )}
    </div>
  )
}