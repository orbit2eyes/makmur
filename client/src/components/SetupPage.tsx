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
      .catch(() => setError('Failed to check setup status'))
      .finally(() => setLoading(false))
  }, [])

  const qrUrl = qrToken
    ? `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(window.location.origin + '/setup?token=' + qrToken)}`
    : ''

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }

    setSubmitting(true)
    try {
      await setupRegister({ token: qrToken, username, password })
      window.location.href = '/login'
    } catch (err: any) {
      setError(err.message || 'Registration failed')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="loading-screen">Loading…</div>

  if (needsSetup === false) {
    return (
      <div className="setup-screen">
        <div className="setup-card">
          <h1 className="setup-title">Already Set Up</h1>
          <p className="setup-subtitle">This system has been initialized.</p>
          <a href="/login" className="btn btn-primary setup-link">Go to Login</a>
        </div>
      </div>
    )
  }

  return (
    <div className="setup-screen">
      <div className="setup-card">
        <h1 className="setup-title">Setup Your System</h1>
        <p className="setup-subtitle">Create the first admin account</p>

        {qrUrl && (
          <div className="setup-qr-section">
            <p className="setup-qr-label">Scan this QR code to open setup on mobile:</p>
            <img src={qrUrl} alt="Setup QR code" className="setup-qr" />
          </div>
        )}

        <form onSubmit={handleSubmit} className="setup-form">
          {error && <div className="login-error">{error}</div>}
          <div className="form-group">
            <label htmlFor="setup-username">Username</label>
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
            <label htmlFor="setup-password">Password</label>
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
            <label htmlFor="setup-confirm">Confirm Password</label>
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
            {submitting ? 'Creating…' : 'Create Admin Account'}
          </button>
        </form>
      </div>
    </div>
  )
}
