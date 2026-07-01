import React, { useState, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'

const EMPTY = { name: '', cui: '', regCom: '', iban: '', phone: '', email: '', address: '' }

export default function Colaboratori() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [partners, setPartners] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)

  function load() {
    if (!selectedCompany) return
    apiFetch(`/companies/${selectedCompany.id}/partners`, {}, token)
      .then(d => setPartners(d || []))
      .catch(err => addToast(err.message))
  }

  useEffect(() => { load() }, [selectedCompany?.id])

  async function handleAdd(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const p = await apiFetch(`/companies/${selectedCompany.id}/partners`, {
        method: 'POST', body: JSON.stringify(form),
      }, token)
      setPartners(prev => [...prev, p])
      setForm(EMPTY)
      setShowForm(false)
      addToast('Colaborator adăugat!', 'success')
    } catch (err) {
      addToast(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id) {
    if (!confirm('Ștergi acest colaborator?')) return
    try {
      await apiFetch(`/partners/${id}`, { method: 'DELETE' }, token)
      setPartners(prev => prev.filter(p => p.id !== id))
      addToast('Colaborator șters.', 'success')
    } catch (err) {
      addToast(err.message)
    }
  }

  if (!selectedCompany) return <div className="p-6 text-center text-muted text-sm">Selectează o firmă.</div>

  const FIELDS = [
    { key: 'name', label: 'Nume', required: true },
    { key: 'cui', label: 'CUI', mono: true },
    { key: 'regCom', label: 'Nr. reg. com.', mono: true },
    { key: 'iban', label: 'IBAN', mono: true },
    { key: 'phone', label: 'Telefon', mono: true },
    { key: 'email', label: 'Email' },
    { key: 'address', label: 'Adresă', wide: true },
  ]

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-2">
        <h1 className="font-display font-bold text-2xl text-ink">Colaboratori</h1>
        <button onClick={() => setShowForm(v => !v)}
          className="bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)] text-sm font-medium px-4 py-2 rounded-lg transition-colors">
          {showForm ? 'Anulează' : '＋ Adaugă colaborator'}
        </button>
      </div>
      <p className="text-xs text-muted mb-5">
        Se completează automat din facturile pe care le încarci (nume, CUI, IBAN, telefon, email, adresă).
      </p>

      {showForm && (
        <form onSubmit={handleAdd} className="bg-white border border-hairline rounded-2xl p-5 mb-6 space-y-4 shadow-sm">
          <div className="h-0.5 w-8 bg-accent mb-1" />
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {FIELDS.map(f => (
              <div key={f.key} className={f.wide ? 'md:col-span-3' : ''}>
                <label className="block text-xs text-muted mb-1">{f.label}</label>
                <input
                  className={`w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent ${f.mono ? 'font-mono' : ''}`}
                  value={form[f.key]}
                  onChange={e => setForm(s => ({ ...s, [f.key]: e.target.value }))}
                  required={f.required}
                />
              </div>
            ))}
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setShowForm(false)}
              className="border border-hairline rounded-lg px-4 py-2 text-sm text-muted hover:bg-paper transition-colors">
              Anulează
            </button>
            <button type="submit" disabled={saving}
              className="bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)] rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:opacity-60">
              {saving ? 'Se salvează...' : 'Salvează'}
            </button>
          </div>
        </form>
      )}

      <div className="bg-white border border-hairline rounded-2xl overflow-hidden shadow-sm">
        <div className="h-0.5 bg-accent" />
        {partners.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">
            Niciun colaborator încă. Încarcă o factură pe pagina Facturi și partenerul apare aici automat.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-ink">
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Nume</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">CUI</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Reg. com.</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">IBAN</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Telefon</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Email</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Adresă</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {partners.map(p => (
                <tr key={p.id} className="border-t border-hairline even:bg-[#F4F6F8] hover:bg-[#EAF3F0] transition-colors">
                  <td className="px-4 py-2.5 font-medium text-ink">{p.name}</td>
                  <td className="px-4 py-2.5 font-mono text-xs tabular-nums text-muted">{p.cui || '—'}</td>
                  <td className="px-4 py-2.5 font-mono text-xs text-muted">{p.regCom || '—'}</td>
                  <td className="px-4 py-2.5 font-mono text-xs text-muted">{p.iban || '—'}</td>
                  <td className="px-4 py-2.5 font-mono text-xs text-muted">{p.phone || '—'}</td>
                  <td className="px-4 py-2.5 text-xs text-muted">{p.email || '—'}</td>
                  <td className="px-4 py-2.5 text-xs text-muted max-w-xs truncate" title={p.address}>{p.address || '—'}</td>
                  <td className="px-4 py-2.5 text-right">
                    <button onClick={() => handleDelete(p.id)}
                      className="text-xs text-muted hover:text-danger transition-colors">
                      Șterge
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
