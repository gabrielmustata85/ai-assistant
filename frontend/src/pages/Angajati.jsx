import React, { useState, useEffect, useRef } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import BatchReviewModal from '../components/BatchReviewModal.jsx'

const EMPTY = { fullName: '', cnp: '', grossSalary: '', position: '', startDate: '', active: true }

const EMPLOYEE_COLUMNS = [
  { key: 'fullName', label: 'Nume complet', type: 'text' },
  { key: 'position', label: 'Funcție', type: 'text' },
  { key: 'cnp', label: 'CNP', type: 'text', mono: true },
  { key: 'grossSalary', label: 'Salariu brut (LEI)', type: 'number', mono: true },
  { key: 'startDate', label: 'Data angajării', type: 'date', mono: true },
]

export default function Angajati() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [employees, setEmployees] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [parsing, setParsing] = useState(false)
  const pdfInputRef = useRef(null)

  // Batch state
  const [batchItems, setBatchItems] = useState(null)
  const [showBatchModal, setShowBatchModal] = useState(false)
  const [batchSaving, setBatchSaving] = useState(false)
  const [batchProgress, setBatchProgress] = useState(null)

  useEffect(() => {
    if (!selectedCompany) return
    apiFetch(`/companies/${selectedCompany.id}/employees`, {}, token)
      .then(d => setEmployees(d || []))
      .catch(err => addToast(err.message))
  }, [selectedCompany?.id])

  async function handlePdfUpload(e) {
    const files = Array.from(e.target.files || [])
    if (pdfInputRef.current) pdfInputRef.current.value = ''
    if (files.length === 0) return

    // Un stat de plată conține de obicei mai mulți angajați → mereu prin tabelul de verificare.
    setParsing(true)
    try {
      const fd = new FormData()
      files.forEach(f => fd.append('files', f))
      const results = await apiFetch(
        `/companies/${selectedCompany.id}/employees/parse-batch`,
        { method: 'POST', body: fd },
        token
      )
      setBatchItems(results)
      setShowBatchModal(true)
    } catch (err) {
      const msg = err.message || 'Eroare la extragerea datelor.'
      addToast(`${msg} PDF-urile scanate (imagini) nu pot fi citite automat.`, 'error')
    } finally {
      setParsing(false)
    }
  }

  async function handleBatchSave(rows) {
    setBatchSaving(true)
    setBatchProgress({ done: 0, total: rows.length })
    let saved = 0
    for (const row of rows) {
      try {
        const body = {
          fullName: row.fullName || '',
          cnp: row.cnp || '',
          grossSalary: parseFloat(row.grossSalary) || 0,
          position: row.position || '',
          startDate: row.startDate || null,
          active: true,
        }
        const emp = await apiFetch(`/companies/${selectedCompany.id}/employees`, {
          method: 'POST', body: JSON.stringify(body),
        }, token)
        setEmployees(prev => [...prev, emp])
        saved++
        setBatchProgress({ done: saved, total: rows.length })
      } catch (err) {
        addToast(`Eroare la salvarea angajatului: ${err.message}`, 'error')
      }
    }
    setBatchSaving(false)
    setBatchProgress(null)
    setShowBatchModal(false)
    setBatchItems(null)
    addToast(`${saved} angajat/angajați salvați cu succes!`, 'success')
  }

  function closeBatchModal() {
    if (batchSaving) return
    setShowBatchModal(false)
    setBatchItems(null)
  }

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

  async function handleDelete(id) {
    if (!confirm('Ștergi acest angajat?')) return
    try {
      await apiFetch(`/employees/${id}`, { method: 'DELETE' }, token)
      setEmployees(prev => prev.filter(e => e.id !== id))
      addToast('Angajat șters.', 'success')
    } catch (err) {
      addToast(err.message)
    }
  }

  if (!selectedCompany) return <div className="p-6 text-center text-muted text-sm">Selectează o firmă.</div>

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display font-bold text-2xl text-ink">Angajați</h1>
        <div className="flex items-center gap-2">
          <input
            ref={pdfInputRef}
            type="file"
            accept="application/pdf"
            multiple
            className="hidden"
            onChange={handlePdfUpload}
          />
          <button
            type="button"
            disabled={parsing}
            onClick={() => pdfInputRef.current?.click()}
            className="border border-accent text-accent hover:bg-accent hover:text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-60"
          >
            {parsing ? 'Se extrag datele…' : '↑ Încarcă stat de plată'}
          </button>
          <button onClick={() => setShowForm(v => !v)}
            className="bg-accent hover:bg-accentHover text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
            {showForm ? 'Anulează' : '＋ Adaugă angajat'}
          </button>
        </div>
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

      <div className="bg-white border border-hairline rounded-2xl overflow-hidden shadow-sm">
        <div className="h-0.5 bg-accent" />
        {employees.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">Niciun angajat înregistrat.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-ink">
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Nume</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Funcție</th>
                <th className="text-left px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Status</th>
                <th className="text-right px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Data angajare</th>
                <th className="text-right px-4 py-3 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide">Salariu brut</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {employees.map(emp => (
                <tr key={emp.id} className="border-t border-hairline even:bg-[#F4F6F8] hover:bg-[#EAF3F0] transition-colors">
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
                  <td className="px-4 py-2.5 text-right">
                    <button onClick={() => handleDelete(emp.id)}
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

      {showBatchModal && batchItems && (
        <BatchReviewModal
          items={batchItems}
          columnDefs={EMPLOYEE_COLUMNS}
          onSave={handleBatchSave}
          onClose={closeBatchModal}
          saving={batchSaving}
          saveProgress={batchProgress}
        />
      )}
    </div>
  )
}
