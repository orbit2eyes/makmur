import { useState, useEffect } from 'react'
import { getSetupStatus, getSetupToken, setupRegister } from '../api'

export default function SetupPage() {
  const [needsSetup, setNeedsSetup] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [qrToken, setQrToken] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const urlToken = params.get('token') || ''

    getSetupStatus()
      .then(status => {
        setNeedsSetup(status.needsSetup)
        if (status.needsSetup) {
          if (urlToken) {
            setQrToken(urlToken)
          } else {
            return getSetupToken().then(res => setQrToken(res.token))
          }
        }
      })
      .catch(() => setError('Gagal memeriksa status pengaturan'))
      .finally(() => setLoading(false))
  }, [])

  const qrUrl = qrToken ? '/api/setup/qr' : ''

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (password !== confirmPassword) {
      setError('Kata sandi tidak cocok')
      return
    }

    if (password.length < 8) {
      setError('Kata sandi minimal 8 karakter')
      return
    }

    setSubmitting(true)
    try {
      await setupRegister({ token: qrToken, username, password })
      window.location.href = '/login'
    } catch (err: any) {
      setError(err.message || 'Registrasi gagal')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="loading-screen">Memuat...</div>

  if (needsSetup === false) {
    return (
      <div className="setup-screen">
        <div className="setup-card">
          <h1 className="setup-title">Sudah Diatur</h1>
          <p className="setup-subtitle">Sistem ini sudah diinisialisasi.</p>
          <a href="/login" className="btn btn-primary setup-link">Ke Halaman Masuk</a>
        </div>
      </div>
    )
  }

  return (
    <div className="setup-screen">
      <div className="setup-card">
        <h1 className="setup-title">Atur Sistem Anda</h1>
        <p className="setup-subtitle">Buat akun admin pertama</p>

        {qrUrl && (
          <div className="setup-qr-section">
            <p className="setup-qr-label">Pindai kode QR ini untuk membuka pengaturan di ponsel:</p>
            <img src={qrUrl} alt="Setup QR code" className="setup-qr" />
          </div>
        )}

        <form onSubmit={handleSubmit} className="setup-form">
          {error && <div className="login-error">{error}</div>}
          <div className="form-group">
            <label htmlFor="setup-username">Nama Pengguna</label>
            <input
              id="setup-username"
              className="form-input"
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              autoFocus
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="setup-password">Kata Sandi</label>
            <input
              id="setup-password"
              className="form-input"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="setup-confirm">Konfirmasi Kata Sandi</label>
            <input
              id="setup-confirm"
              className="form-input"
              type="password"
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              required
            />
          </div>
          <button className="btn btn-primary setup-btn" type="submit" disabled={submitting}>
            {submitting ? 'Membuat...' : 'Buat Akun Admin'}
          </button>
        </form>
      </div>
    </div>
  )
}
