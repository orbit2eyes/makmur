import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchUsers, createUser, deactivateUser, reactivateUser, resetUserPassword } from '../api'
import type { User } from '../types'

export default function UserList() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)

  // Create form state
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'staff' | 'manager'>('staff')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')
  const [notification, setNotification] = useState('')

  const loadUsers = () => {
    setLoading(true)
    setError('')
    fetchUsers()
      .then(setUsers)
      .catch(err => setError(err.message || 'Gagal memuat pengguna'))
      .finally(() => setLoading(false))
  }

  useEffect(loadUsers, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError('')

    if (password.length < 8) {
      setFormError('Kata sandi minimal 8 karakter')
      return
    }

    setSubmitting(true)
    try {
      await createUser({ username, password, role })
      setUsername('')
      setPassword('')
      setRole('staff')
      setShowForm(false)
      loadUsers()
    } catch (err: any) {
      setFormError(err.message || 'Gagal membuat pengguna')
    } finally {
      setSubmitting(false)
    }
  }

  const handleResetPassword = async (u: User) => {
    const newPassword = window.prompt(`Enter new password for "${u.username}":`)
    if (!newPassword) return
    if (newPassword.length < 8) {
      setError('Kata sandi minimal 8 karakter')
      return
    }
    try {
      await resetUserPassword(u.id, { new_password: newPassword })
      setNotification(`Password reset for ${u.username}`)
      setTimeout(() => setNotification(''), 8000)
    } catch (err: any) {
      setError(err.message || 'Gagal mengatur ulang kata sandi')
    }
  }

  const handleToggleActive = async (u: User) => {
    try {
      if (u.active) {
        await deactivateUser(u.id)
      } else {
        await reactivateUser(u.id)
      }
      loadUsers()
    } catch (err: any) {
      setError(err.message || 'Gagal memperbarui pengguna')
    }
  }

  const roleOptions = currentUser?.role === 'admin'
    ? (['staff', 'manager'] as const)
    : (['staff'] as const)

  return (
    <div className="user-management">
      <div className="user-header">
        <h2 className="user-heading">Manajemen Staf</h2>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Batal' : 'Tambah Pengguna'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="user-form">
          {formError && <div className="login-error">{formError}</div>}
          <div className="form-group">
            <label htmlFor="new-username">Nama Pengguna</label>
            <input
              id="new-username"
              className="form-input"
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="new-password">Kata Sandi</label>
            <input
              id="new-password"
              className="form-input"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="new-role">Peran</label>
            <select
              id="new-role"
              className="form-input"
              value={role}
              onChange={e => setRole(e.target.value as 'staff' | 'manager')}
            >
              {roleOptions.map(r => (
                <option key={r} value={r}>{r.charAt(0).toUpperCase() + r.slice(1)}</option>
              ))}
            </select>
          </div>
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? 'Membuat...' : 'Buat Pengguna'}
          </button>
        </form>
      )}

      {notification && <div className="success-banner">{notification}</div>}
      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <div className="loading">Memuat pengguna...</div>
      ) : (
        <div className="user-table-wrapper">
          <table className="user-table">
            <thead>
              <tr>
                <th>Nama Pengguna</th>
                <th>Peran</th>
                <th>Status</th>
                <th>Dibuat</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id} className="user-row">
                  <td className="user-cell-name">{u.username}</td>
                  <td>
                    <span className={`user-role-badge user-role-${u.role}`}>{u.role}</span>
                  </td>
                  <td>
                    <span className={`user-status-dot ${u.active ? 'user-active' : 'user-inactive'}`}>
                      {u.active ? 'Aktif' : 'Nonaktif'}
                    </span>
                  </td>
                  <td className="user-cell-date">
                    {new Date(u.created_at).toLocaleDateString()}
                  </td>
                  <td className="user-actions">
                    <button
                      className="btn btn-sm btn-secondary"
                      onClick={() => handleResetPassword(u)}
                    >
                      Atur Ulang Kata Sandi
                    </button>
                    <button
                      className={`btn btn-sm ${u.active ? 'btn-danger' : 'btn-secondary'}`}
                      onClick={() => handleToggleActive(u)}
                      disabled={u.id === currentUser?.id}
                      title={u.id === currentUser?.id ? 'Tidak dapat mengubah diri sendiri' : ''}
                    >
                      {u.active ? 'Nonaktifkan' : 'Aktifkan'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
