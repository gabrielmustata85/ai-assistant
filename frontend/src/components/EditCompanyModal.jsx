import React, { useState } from 'react'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from './Toast.jsx'

const FIELDS = [
  { key: 'name', label: 'Denumire' },
  { key: 'cui', label: 'CIF / CUI', mono: true },
  { key: 'regCom', label: 'Nr. reg. comerțului', mono: true },
  { key: 'address', label: 'Adresă', wide: true },
  { key: 'iban', label: 'IBAN', mono: true },
  { key: 'bank', label: 'Bancă' },
  { key: 'phone', label: 'Telefon', mono: true },
  { key: 'email', label: 'Email' },
]

export default function EditCompanyModal({ company, onClose, onSaved }) {
  const { token } = useAuth()
  const { addToast } = useToast()
  const [form, setForm] = useState(() => {
    const f = {}
    FIELDS.forEach(x => { f[x.key] = company[x.key] || '' })
    return f
  })
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await apiFetch(`/companies/${company.id}`, {
        method: 'PATCH', body: JSON.stringify(form),
      }, token)
      onSaved(updated)
      addToast('Datele firmei au fost salvate.', 'success')
    } catch (err) {
      addToast(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(11,27,46,0.6)' }}>
      <div className="bg-white rounded-2xl p-6 max-w-xl w-full shadow-[0_30px_80px_-20px_rgba(0,0,0,0.6)] max-h-[90vh] overflow-auto">
        <div className="h-0.5 w-10 bg-accent mb-4" />
        <h2 className="font-display font-bold text-xl text-ink mb-1">Datele firmei</h2>
        <p className="text-sm text-muted mb-5">Apar pe facturile emise (vânzător).</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            {FIELDS.map(f => (
              <div key={f.key} className={f.wide ? 'col-span-2' : ''}>
                <label className="block text-xs text-muted mb-1">{f.label}</label>
                <input
                  className={`w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent ${f.mono ? 'font-mono' : ''}`}
                  value={form[f.key]}
                  onChange={e => setForm(s => ({ ...s, [f.key]: e.target.value }))}
                />
              </div>
            ))}
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="border border-hairline rounded-lg px-4 py-2 text-sm text-muted hover:bg-paper transition-colors">
              Anulează
            </button>
            <button type="submit" disabled={saving}
              className="bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)] rounded-lg px-4 py-2 text-sm font-semibold transition-colors disabled:opacity-60">
              {saving ? 'Se salvează...' : 'Salvează'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
