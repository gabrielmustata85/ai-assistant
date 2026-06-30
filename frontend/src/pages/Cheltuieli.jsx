import React, { useState, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'

const EMPTY = { description: '', category: '', amount: '', expenseDate: '', deductible: false }

export default function Cheltuieli() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [expenses, setExpenses] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!selectedCompany) return
    apiFetch(`/companies/${selectedCompany.id}/expenses`, {}, token)
      .then(d => setExpenses(d || []))
      .catch(err => addToast(err.message))
  }, [selectedCompany?.id])

  async function handleAdd(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const body = { ...form, amount: parseFloat(form.amount) }
      const exp = await apiFetch(`/companies/${selectedCompany.id}/expenses`, {
        method: 'POST', body: JSON.stringify(body),
      }, token)
      setExpenses(prev => [...prev, exp])
      setForm(EMPTY)
      setShowForm(false)
      addToast('Cheltuială adăugată!', 'success')
    } catch (err) {
      addToast(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (!selectedCompany) return <div className="p-6 text-center text-muted text-sm">Selectează o firmă.</div>

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display font-bold text-2xl text-ink">Cheltuieli</h1>
        <button onClick={() => setShowForm(v => !v)}
          className="bg-accent hover:bg-accentHover text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
          {showForm ? 'Anulează' : '＋ Adaugă cheltuială'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleAdd} className="bg-white border border-hairline rounded-lg p-5 mb-6 space-y-4">
          <div className="h-0.5 w-8 bg-accent mb-1" />
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div className="md:col-span-2">
              <label className="block text-xs text-muted mb-1">Descriere</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Categorie</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Sumă (LEI)</label>
              <input type="number" step="0.01" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.amount} onChange={e => setForm(f => ({ ...f, amount: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Data</label>
              <input type="date" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.expenseDate} onChange={e => setForm(f => ({ ...f, expenseDate: e.target.value }))} required />
            </div>
            <div className="flex items-end pb-1">
              <label className="flex items-center gap-2 text-sm text-ink">
                <input type="checkbox" checked={form.deductible}
                  onChange={e => setForm(f => ({ ...f, deductible: e.target.checked }))} />
                Deductibilă
              </label>
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setShowForm(false)}
              className="border border-hairline rounded-lg px-4 py-2 text-sm text-muted hover:bg-paper transition-colors">
              Anulează
            </button>
            <button type="submit" disabled={saving}
              className="bg-accent hover:bg-accentHover text-white rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:opacity-60">
              {saving ? 'Se salvează...' : 'Salvează'}
            </button>
          </div>
        </form>
      )}

      <div className="bg-white border border-hairline rounded-lg overflow-hidden">
        <div className="h-0.5 bg-accent" />
        {expenses.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">Nicio cheltuială înregistrată.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-paper border-b border-hairline">
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Descriere</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Categorie</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Deductibilă</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Data</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Sumă (LEI)</th>
              </tr>
            </thead>
            <tbody>
              {expenses.map(exp => (
                <tr key={exp.id} className="border-t border-hairline hover:bg-paper transition-colors">
                  <td className="px-4 py-2.5 text-ink">{exp.description}</td>
                  <td className="px-4 py-2.5 text-muted text-xs">{exp.category}</td>
                  <td className="px-4 py-2.5">
                    {exp.deductible
                      ? <span className="text-xs font-medium text-accent">Da</span>
                      : <span className="text-xs text-muted">Nu</span>}
                  </td>
                  <td className="px-4 py-2.5 text-right font-mono text-xs tabular-nums text-muted">{exp.expenseDate}</td>
                  <td className="px-4 py-2.5 text-right font-mono tabular-nums font-medium">
                    {(exp.amount || 0).toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
                    <span className="text-muted text-xs ml-1">LEI</span>
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
