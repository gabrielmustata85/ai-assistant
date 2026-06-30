import React, { useState, useEffect, useRef } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import BatchReviewModal from '../components/BatchReviewModal.jsx'

const EMPTY = { description: '', category: '', amount: '', expenseDate: '', deductible: false }

const EXPENSE_COLUMNS = [
  { key: 'description', label: 'Descriere', type: 'text' },
  { key: 'category', label: 'Categorie', type: 'text' },
  { key: 'amount', label: 'Sumă (LEI)', type: 'number', mono: true },
  { key: 'expenseDate', label: 'Data', type: 'date', mono: true },
  { key: 'deductible', label: 'Deductibilă', type: 'checkbox' },
]

export default function Cheltuieli() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [expenses, setExpenses] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [parsing, setParsing] = useState(false)
  const [fromPdf, setFromPdf] = useState(false)
  const pdfInputRef = useRef(null)

  // Batch state
  const [batchItems, setBatchItems] = useState(null)
  const [showBatchModal, setShowBatchModal] = useState(false)
  const [batchSaving, setBatchSaving] = useState(false)
  const [batchProgress, setBatchProgress] = useState(null)

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
      setFromPdf(false)
      setShowForm(false)
      addToast('Cheltuială adăugată!', 'success')
    } catch (err) {
      addToast(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id) {
    if (!confirm('Ștergi această cheltuială?')) return
    try {
      await apiFetch(`/expenses/${id}`, { method: 'DELETE' }, token)
      setExpenses(prev => prev.filter(e => e.id !== id))
      addToast('Cheltuială ștearsă.', 'success')
    } catch (err) {
      addToast(err.message)
    }
  }

  async function handlePdfUpload(e) {
    const files = Array.from(e.target.files || [])
    if (pdfInputRef.current) pdfInputRef.current.value = ''
    if (files.length === 0) return

    if (files.length === 1) {
      // Single file — prefill the form
      setParsing(true)
      try {
        const fd = new FormData()
        fd.append('file', files[0])
        const parsed = await apiFetch(
          `/companies/${selectedCompany.id}/expenses/parse`,
          { method: 'POST', body: fd },
          token
        )
        setForm({
          description: parsed.description || '',
          category: parsed.category || '',
          amount: parsed.amount != null ? String(parsed.amount) : '',
          expenseDate: parsed.expenseDate || '',
          deductible: Boolean(parsed.deductible),
        })
        setFromPdf(true)
        setShowForm(true)
        addToast('Date extrase! Verifică și salvează cheltuiala.', 'success')
      } catch (err) {
        const msg = err.message || 'Eroare la extragerea datelor.'
        addToast(`${msg} PDF-urile scanate (imagini) nu pot fi citite automat.`, 'error')
      } finally {
        setParsing(false)
      }
    } else {
      // Multiple files — batch
      setParsing(true)
      try {
        const fd = new FormData()
        files.forEach(f => fd.append('files', f))
        const results = await apiFetch(
          `/companies/${selectedCompany.id}/expenses/parse-batch`,
          { method: 'POST', body: fd },
          token
        )
        setBatchItems(results)
        setShowBatchModal(true)
      } catch (err) {
        const msg = err.message || 'Eroare la extragerea datelor.'
        addToast(msg, 'error')
      } finally {
        setParsing(false)
      }
    }
  }

  async function handleBatchSave(rows) {
    setBatchSaving(true)
    setBatchProgress({ done: 0, total: rows.length })
    let saved = 0
    for (const row of rows) {
      try {
        const body = {
          description: row.description || '',
          category: row.category || '',
          amount: parseFloat(row.amount) || 0,
          expenseDate: row.expenseDate || '',
          deductible: Boolean(row.deductible),
        }
        const exp = await apiFetch(`/companies/${selectedCompany.id}/expenses`, {
          method: 'POST', body: JSON.stringify(body),
        }, token)
        setExpenses(prev => [...prev, exp])
        saved++
        setBatchProgress({ done: saved, total: rows.length })
      } catch (err) {
        addToast(`Eroare la salvarea cheltuielii: ${err.message}`, 'error')
      }
    }
    setBatchSaving(false)
    setBatchProgress(null)
    setShowBatchModal(false)
    setBatchItems(null)
    addToast(`${saved} cheltuială/cheltuieli salvate cu succes!`, 'success')
  }

  function closeBatchModal() {
    if (batchSaving) return
    setShowBatchModal(false)
    setBatchItems(null)
  }

  if (!selectedCompany) return <div className="p-6 text-center text-muted text-sm">Selectează o firmă.</div>

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display font-bold text-2xl text-ink">Cheltuieli</h1>
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
            {parsing ? 'Se extrage datele…' : '↑ Încarcă PDF'}
          </button>
          <button
            onClick={() => setShowForm(v => !v)}
            className="bg-accent hover:bg-accentHover text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            {showForm ? 'Anulează' : '＋ Adaugă cheltuială'}
          </button>
        </div>
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
            <button type="button" onClick={() => { setShowForm(false); setFromPdf(false) }}
              className="border border-hairline rounded-lg px-4 py-2 text-sm text-muted hover:bg-paper transition-colors">
              Anulează
            </button>
            <button type="submit" disabled={saving}
              className="bg-accent hover:bg-accentHover text-white rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:opacity-60">
              {saving ? 'Se salvează...' : fromPdf ? 'Verifică și salvează' : 'Salvează'}
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
                <th className="px-4 py-3" />
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
                  <td className="px-4 py-2.5 text-right">
                    <button onClick={() => handleDelete(exp.id)}
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
          columnDefs={EXPENSE_COLUMNS}
          onSave={handleBatchSave}
          onClose={closeBatchModal}
          saving={batchSaving}
          saveProgress={batchProgress}
        />
      )}
    </div>
  )
}
