import React, { useState, useEffect, useRef } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import BatchReviewModal from '../components/BatchReviewModal.jsx'

const EMPTY_FORM = {
  direction: 'ISSUED', invoiceNumber: '', partnerName: '', partnerCui: '',
  issueDate: '', dueDate: '', netAmount: '', vatAmount: '', grossAmount: '',
  category: '', deductible: false,
}

const INVOICE_COLUMNS = [
  { key: 'direction', label: 'Direcție', type: 'select', options: [{ value: 'ISSUED', label: 'Emisă' }, { value: 'RECEIVED', label: 'Primită' }] },
  { key: 'invoiceNumber', label: 'Nr. factură', type: 'text' },
  { key: 'partnerName', label: 'Partener', type: 'text' },
  { key: 'partnerCui', label: 'CUI', type: 'text', mono: true },
  { key: 'issueDate', label: 'Data emiterii', type: 'date', mono: true },
  { key: 'dueDate', label: 'Scadență', type: 'date', mono: true },
  { key: 'netAmount', label: 'Net (LEI)', type: 'number', mono: true },
  { key: 'vatAmount', label: 'TVA (LEI)', type: 'number', mono: true },
  { key: 'grossAmount', label: 'Brut (LEI)', type: 'number', mono: true },
  { key: 'category', label: 'Categorie', type: 'text' },
  { key: 'deductible', label: 'Deductibilă', type: 'checkbox' },
]

export default function Facturi() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [invoices, setInvoices] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
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
    apiFetch(`/companies/${selectedCompany.id}/invoices`, {}, token)
      .then(d => setInvoices(d || []))
      .catch(err => addToast(err.message))
  }, [selectedCompany?.id])

  async function handleAdd(e) {
    e.preventDefault()
    setSaving(true)
    try {
      const body = {
        ...form,
        netAmount: parseFloat(form.netAmount) || 0,
        vatAmount: parseFloat(form.vatAmount) || 0,
        grossAmount: parseFloat(form.grossAmount),
      }
      const inv = await apiFetch(`/companies/${selectedCompany.id}/invoices`, {
        method: 'POST', body: JSON.stringify(body),
      }, token)
      setInvoices(prev => [...prev, inv])
      setForm(EMPTY_FORM)
      setShowForm(false)
      addToast('Factură adăugată!', 'success')
    } catch (err) {
      addToast(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id) {
    if (!confirm('Ștergi această factură?')) return
    try {
      await apiFetch(`/invoices/${id}`, { method: 'DELETE' }, token)
      setInvoices(prev => prev.filter(i => i.id !== id))
      addToast('Factură ștearsă.', 'success')
    } catch (err) {
      addToast(err.message)
    }
  }

  async function handlePdfUpload(e) {
    const files = Array.from(e.target.files || [])
    if (pdfInputRef.current) pdfInputRef.current.value = ''
    if (files.length === 0) return

    setParsing(true)
    try {
      const fd = new FormData()
      files.forEach(f => fd.append('files', f))
      const results = await apiFetch(
        `/companies/${selectedCompany.id}/invoices/parse-batch`,
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
          direction: row.direction || 'RECEIVED',
          invoiceNumber: row.invoiceNumber || '',
          partnerName: row.partnerName || '',
          partnerCui: row.partnerCui || '',
          issueDate: row.issueDate || '',
          dueDate: row.dueDate || '',
          netAmount: parseFloat(row.netAmount) || 0,
          vatAmount: parseFloat(row.vatAmount) || 0,
          grossAmount: parseFloat(row.grossAmount) || 0,
          category: row.category || '',
          deductible: Boolean(row.deductible),
        }
        const inv = await apiFetch(`/companies/${selectedCompany.id}/invoices`, {
          method: 'POST', body: JSON.stringify(body),
        }, token)
        setInvoices(prev => [...prev, inv])
        saved++
        setBatchProgress({ done: saved, total: rows.length })
      } catch (err) {
        addToast(`Eroare la salvarea facturii: ${err.message}`, 'error')
      }
    }
    setBatchSaving(false)
    setBatchProgress(null)
    setShowBatchModal(false)
    setBatchItems(null)
    addToast(`${saved} factură/facturi salvate cu succes!`, 'success')
  }

  function closeBatchModal() {
    if (batchSaving) return
    setShowBatchModal(false)
    setBatchItems(null)
  }

  if (!selectedCompany) return (
    <div className="p-6 text-center text-muted text-sm">Selectează o firmă.</div>
  )

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display font-bold text-2xl text-ink">Facturi</h1>
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
            {parsing ? `Se extrage datele…` : '↑ Încarcă PDF'}
          </button>
          <button
            onClick={() => setShowForm(v => !v)}
            className="bg-accent hover:bg-accentHover text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            {showForm ? 'Anulează' : '＋ Adaugă factură'}
          </button>
        </div>
      </div>

      {showForm && (
        <form onSubmit={handleAdd} className="bg-white border border-hairline rounded-lg p-5 mb-6 space-y-4">
          <div className="h-0.5 w-8 bg-accent mb-1" />
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs text-muted mb-1">Direcție</label>
              <select className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.direction} onChange={e => setForm(f => ({ ...f, direction: e.target.value }))}>
                <option value="ISSUED">Emisă</option>
                <option value="RECEIVED">Primită</option>
              </select>
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Nr. factură</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.invoiceNumber} onChange={e => setForm(f => ({ ...f, invoiceNumber: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Partener</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.partnerName} onChange={e => setForm(f => ({ ...f, partnerName: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">CUI partener</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.partnerCui} onChange={e => setForm(f => ({ ...f, partnerCui: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Data emiterii</label>
              <input type="date" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.issueDate} onChange={e => setForm(f => ({ ...f, issueDate: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Scadență</label>
              <input type="date" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.dueDate} onChange={e => setForm(f => ({ ...f, dueDate: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Net (LEI)</label>
              <input type="number" step="0.01" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.netAmount} onChange={e => setForm(f => ({ ...f, netAmount: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">TVA (LEI)</label>
              <input type="number" step="0.01" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.vatAmount} onChange={e => setForm(f => ({ ...f, vatAmount: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Brut (LEI)</label>
              <input type="number" step="0.01" className="w-full border border-hairline rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.grossAmount} onChange={e => setForm(f => ({ ...f, grossAmount: e.target.value }))} required />
            </div>
            <div>
              <label className="block text-xs text-muted mb-1">Categorie</label>
              <input className="w-full border border-hairline rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent"
                value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} />
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
        {invoices.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">Nicio factură înregistrată.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-paper border-b border-hairline">
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Nr.</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Tip</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Partener</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Data emisă</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Scadență</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Brut (LEI)</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {invoices.map(inv => (
                <tr key={inv.id} className="border-t border-hairline hover:bg-paper transition-colors">
                  <td className="px-4 py-2.5 font-mono text-xs text-muted">{inv.invoiceNumber}</td>
                  <td className="px-4 py-2.5">
                    <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${
                      inv.direction === 'ISSUED' ? 'bg-accent text-white' : 'text-amber'
                    }`} style={inv.direction !== 'ISSUED' ? {backgroundColor:'rgba(184,101,27,0.1)'} : {}}>
                      {inv.direction === 'ISSUED' ? 'Emisă' : 'Primită'}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-ink">{inv.partnerName}</td>
                  <td className="px-4 py-2.5 text-right font-mono text-xs tabular-nums text-muted">{inv.issueDate}</td>
                  <td className="px-4 py-2.5 text-right font-mono text-xs tabular-nums text-amber">{inv.dueDate}</td>
                  <td className="px-4 py-2.5 text-right font-mono tabular-nums font-medium">
                    {(inv.grossAmount || 0).toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
                    <span className="text-muted text-xs ml-1">LEI</span>
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <button onClick={() => handleDelete(inv.id)}
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
          columnDefs={INVOICE_COLUMNS}
          onSave={handleBatchSave}
          onClose={closeBatchModal}
          saving={batchSaving}
          saveProgress={batchProgress}
        />
      )}
    </div>
  )
}
