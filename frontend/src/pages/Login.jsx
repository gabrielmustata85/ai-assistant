import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiFetch } from '../lib/api.js'
import { useAuth } from '../contexts/AuthContext.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await apiFetch('/auth/login', {
        method: 'POST',
        body: JSON.stringify(form),
      })
      login(data.token, { username: form.username })
      navigate('/')
    } catch (err) {
      setError(err.message || 'Autentificare eșuată.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-ink flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="flex items-center gap-2.5 mb-8">
          <div className="w-1.5 h-10 bg-accent rounded-full" />
          <div className="leading-none">
            <span className="block font-display font-bold text-white text-2xl tracking-tight">Marius</span>
            <span className="block text-xs text-onDarkMuted mt-1">asistentul tău fiscal</span>
          </div>
        </div>
        <div className="bg-white rounded-2xl p-7 shadow-[0_20px_60px_-15px_rgba(0,0,0,0.5)]">
          <div className="h-0.5 w-10 bg-accent mb-4" />
          <h1 className="font-display text-xl font-semibold text-ink mb-1">Bun venit</h1>
          <p className="text-sm text-muted mb-6">Autentifică-te pentru a continua</p>
          {error && (
            <div className="mb-4 border border-danger rounded-lg px-3 py-2 text-sm text-danger" style={{backgroundColor: 'rgba(178,58,58,0.07)'}}>
              {error}
            </div>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm text-muted mb-1">Utilizator</label>
              <input
                type="text"
                autoComplete="username"
                className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.username}
                onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
                required
              />
            </div>
            <div>
              <label className="block text-sm text-muted mb-1">Parolă</label>
              <input
                type="password"
                autoComplete="current-password"
                className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                required
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)] font-medium rounded-lg py-2.5 text-sm transition-colors disabled:opacity-60"
            >
              {loading ? 'Se autentifică...' : 'Autentifică-te'}
            </button>
          </form>
          <p className="mt-4 text-center text-sm text-muted">
            Nu ai cont?{' '}
            <Link to="/register" className="text-accent hover:underline font-medium">
              Înregistrează-te
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
