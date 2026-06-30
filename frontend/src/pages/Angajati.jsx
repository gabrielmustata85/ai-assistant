import React, { useState, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'

const EMPTY = { fullName: '', cnp: '', grossSalary: '', position: '', startDate: '', active: true }

export default function Angajati() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [employees, setEmployees] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!selectedCompany) return
    apiFetch(`/companies/${selectedCompany.id}/employees`, {}, token)
      .then(d => setEmployees(d || []))
      .catch(err => addToast(err.message))
  }, [selectedCompany?.id])

  async function handleAdd(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const body = { ...form, grossSalary: parseFloat(form.grossSalary) }
      const emp = await apiFetch(`/companies/${selectedCompany.id}/employees`, {
        method: 'POST', body: JSON.stringify(body),
      }, token)
      setEmployees(prev => [...prev, emp])
      setForm(EMPTY)
      setShowForm(false)
      addToast('Angajat adăugat!', 'success')
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
        <h1 className="font-display font-bold text-2xl text-ink">Angajați</h1>
        <button onClick={() => setShowForm(v => !v)}
          className="bg-accent hover:bg-accentHover text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
          {showForm ? 'Anulează' : '＋ Adaugă angajat'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleAdd} className="bg-white border border-hairline rounded-lg p-5 mb-6 space-y-4">
          <div className="h-0.5 w-8 bg-accent mb-1" />
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs text-muted mb-1">Nume complet</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">CNP</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.cnp} onChange={e => setForm(f => ({ ...f, cnp: e.target.value }))} maxLength={13} />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Funcție</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.position} onChange={e => setForm(f => ({ ...f, position: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Salariu brut (LEI)</label>
              <input type="number" step="0.01" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.grossSalary} onChange={e => setForm(f => ({ ...f, grossSalary: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Data angajării</label>
              <input type="date" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.startDate} onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))} />
            </div>
            <div className="flex items-end pb-1">
              <label className="flex items-center gap-2 text-sm text-ink">
                <input type="checkbox" checked={form.active}
                  onChange={e => setForm(f => ({ ...f, active: e.target.checked }))} />
                Activ
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
        {employees.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">Niciun angajat înregistrat.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-paper border-b border-hairline">
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Nume</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Funcție</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Status</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Data angajare</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Salariu brut</th>
              </tr>
            </thead>
            <tbody>
              {employees.map(emp => (
                <tr key={emp.id} className="border-t border-hairline hover:bg-paper transition-colors">
                  <td className="px-4 py-2.5 font-medium text-ink">{emp.fullName}</td>
                  <td className="px-4 py-2.5 text-muted">{emp.position}</td>
                  <td className="px-4 py-2.5">
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      emp.active ? 'bg-accent text-white' : 'bg-hairline text-muted'
                    }`}>
                      {emp.active ? 'Activ' : 'Inactiv'}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-right font-mono text-xs tabular-nums text-muted">{emp.startDate}</td>
                  <td className="px-4 py-2.5 text-right font-mono tabular-nums font-medium">
                    {(emp.grossSalary || 0).toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
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
