import React, { useState } from 'react'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from './Toast.jsx'

export default function AddCompanyModal({ onClose, onCreated }) {
  const { token } = useAuth()
  const { addToast } = useToast()
  const [form, setForm] = useState({
    cui: '', name: '', companyType: 'SRL', taxRegime: 'MICRO_1', vatPayer: false
  })
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    try {
      const company = await apiFetch('/companies', {
        method: 'POST',
        body: JSON.stringify(form),
      }, token)
      addToast('Firmă creată cu succes!', 'success')
      onCreated(company)
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" style={{backgroundColor: 'rgba(16,36,58,0.3)'}}>
      <div className="bg-white rounded-lg shadow-lg border border-hairline p-6 w-full max-w-md">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg font-semibold text-ink">Firmă nouă</h2>
          <button onClick={onClose} className="text-muted hover:text-ink text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-muted mb-1">CUI</label>
            <input
              className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
              value={form.cui} onChange={e => setForm(f => ({ ...f, cui: e.target.value }))}
              required placeholder="RO12345678"
            />
          </div>
          <div>
            <label className="block text-sm text-muted mb-1">Denumire</label>
            <input
              className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
              value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
              required placeholder="SC Exemplu SRL"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm text-muted mb-1">Tip firmă</label>
              <select
                className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.companyType} onChange={e => setForm(f => ({ ...f, companyType: e.target.value }))}
              >
                <option value="SRL">SRL</option>
                <option value="PFA">PFA</option>
                <option value="II">II</option>
              </select>
            </div>
            <div>
              <label className="block text-sm text-muted mb-1">Regim fiscal</label>
              <select
                className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.taxRegime} onChange={e => setForm(f => ({ ...f, taxRegime: e.target.value }))}
              >
                <option value="MICRO_1">Micro 1%</option>
                <option value="MICRO_3">Micro 3%</option>
                <option value="PROFIT_16">Profit 16%</option>
                <option value="PFA">PFA</option>
              </select>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox" id="vat" checked={form.vatPayer}
              onChange={e => setForm(f => ({ ...f, vatPayer: e.target.checked }))}
              className="rounded border-hairline"
            />
            <label htmlFor="vat" className="text-sm text-ink">Plătitor TVA</label>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="flex-1 border border-hairline rounded-lg px-4 py-2 text-sm text-muted hover:bg-paper transition-colors">
              Anulează
            </button>
            <button type="submit" disabled={loading}
              className="flex-1 bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)] rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:opacity-60">
              {loading ? 'Se salvează...' : 'Creează'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
