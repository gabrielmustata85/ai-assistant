import React, { useState, useEffect, useRef } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import BatchReviewModal from '../components/BatchReviewModal.jsx'

const BANK_COLUMNS = [
  { key: 'txnDate', label: 'Data', type: 'date', mono: true },
  {
    key: 'direction', label: 'Sens', type: 'select',
    options: [{ value: 'IN', label: 'Intrare' }, { value: 'OUT', label: 'Ieșire' }],
  },
  { key: 'description', label: 'Descriere', type: 'text' },
  { key: 'counterparty', label: 'Contraparte', type: 'text' },
  { key: 'amount', label: 'Sumă (LEI)', type: 'number', mono: true },
]

export default function ExtraseBancare() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [transactions, setTransactions] = useState([])
  const [parsing, setParsing] = useState(false)
  const pdfInputRef = useRef(null)

  const [batchItems, setBatchItems] = useState(null)
  const [showBatchModal, setShowBatchModal] = useState(false)
  const [batchSaving, setBatchSaving] = useState(false)

  useEffect(() => {
    if (!selectedCompany) return
    apiFetch(`/companies/${selectedCompany.id}/bank/transactions`, {}, token)
      .then(d => setTransactions(d || []))
      .catch(err => addToast(err.message))
  }, [selectedCompany?.id])

  async function handlePdfUpload(e) {
    const file = e.target.files?.[0]
    if (pdfInputRef.current) pdfInputRef.current.value = ''
    if (!file) return
    setParsing(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const data = await apiFetch(
        `/companies/${selectedCompany.id}/bank/parse`,
        { method: 'POST', body: fd },
        token
      )
      const items = (data.transactions || []).map(txn => ({
        filename: file.name,
        parsed: txn,
        error: null,
      }))
      setBatchItems(items)
      setShowBatchModal(true)
    } catch (err) {
      const msg = err.message || 'Eroare la extragerea datelor.'
      addToast(`${msg} PDF-urile scanate nu pot fi citite automat.`, 'error')
    } finally {
      setParsing(false)
    }
  }

  async function handleBatchSave(rows) {
    setBatchSaving(true)
    try {
      const payload = rows.map(r => ({
        txnDate: r.txnDate || '',
        description: r.description || '',
        counterparty: r.counterparty || '',
        direction: r.direction || 'IN',
        amount: parseFloat(r.amount) || 0,
      }))
      const result = await apiFetch(
        `/companies/${selectedCompany.id}/bank/transactions`,
        { method: 'POST', body: JSON.stringify(payload) },
        token
      )
      setShowBatchModal(false)
      setBatchItems(null)
      const skipped = result?.skippedDuplicates || 0
      const saved = result?.saved ?? (result?.transactions?.length || 0)
      addToast(
        skipped > 0
          ? `${saved} tranzacții salvate, ${skipped} duplicate ignorate.`
          : `${saved} tranzacții salvate!`,
        'success'
      )
      setTransactions(prev => [...prev, ...(result?.transactions || [])])
    } catch (err) {
      addToast(err.message || 'Eroare la salvare.', 'error')
    } finally {
      setBatchSaving(false)
    }
  }

  function closeBatchModal() {
    if (batchSaving) return
    setShowBatchModal(false)
    setBatchItems(null)
  }

  async function handleDelete(id) {
    if (!confirm('Ștergi această tranzacție?')) return
    try {
      await apiFetch(`/bank/transactions/${id}`, { method: 'DELETE' }, token)
      setTransactions(prev => prev.filter(t => t.id !== id))
      addToast('Tranzacție ștearsă.', 'success')
    } catch (err) {
      addToast(err.message)
    }
  }

  if (!selectedCompany) return (
    <div className="p-6 text-center text-muted text-sm">Selectează o firmă.</div>
  )

  const totalIn = transactions
    .filter(t => t.direction === 'IN')
    .reduce((s, t) => s + (t.amount || 0), 0)
  const totalOut = transactions
    .filter(t => t.direction === 'OUT')
    .reduce((s, t) => s + (t.amount || 0), 0)

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display font-bold text-2xl text-ink">Extrase bancare</h1>
        <div className="flex items-center gap-2">
          <input
            ref={pdfInputRef}
            type="file"
            accept="application/pdf"
            className="hidden"
            onChange={handlePdfUpload}
          />
          <button
            type="button"
            disabled={parsing}
            onClick={() => pdfInputRef.current?.click()}
            className="border border-accent text-accent hover:bg-accent hover:text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-60"
          >
            {parsing ? 'Se extrag tranzacțiile…' : '↑ Încarcă extras (PDF)'}
          </button>
        </div>
      </div>

      {transactions.length > 0 && (
        <div className="flex gap-6 mb-5 bg-white border border-hairline rounded-lg px-5 py-4">
          <div>
            <p className="text-xs text-muted mb-0.5">Total intrări</p>
            <p className="font-mono tabular-nums text-accent font-semibold">
              {totalIn.toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
              <span className="text-xs font-sans text-muted ml-1">LEI</span>
            </p>
          </div>
          <div className="w-px bg-hairline" />
          <div>
            <p className="text-xs text-muted mb-0.5">Total ieșiri</p>
            <p className="font-mono tabular-nums text-amber font-semibold">
              {totalOut.toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
              <span className="text-xs font-sans text-muted ml-1">LEI</span>
            </p>
          </div>
        </div>
      )}

      <div className="bg-white border border-hairline rounded-lg overflow-hidden">
        <div className="h-0.5 bg-accent" />
        {transactions.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">
            Nicio tranzacție înregistrată. Încarcă un extras bancar (PDF) pentru a începe.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-paper border-b border-hairline">
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Data</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Sens</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Descriere</th>
                <th className="text-left px-4 py-3 text-xs text-muted font-medium">Contraparte</th>
                <th className="text-right px-4 py-3 text-xs text-muted font-medium">Sumă (LEI)</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {transactions.map(txn => (
                <tr key={txn.id} className="border-t border-hairline hover:bg-paper transition-colors">
                  <td className="px-4 py-2.5 font-mono text-xs tabular-nums text-muted">{txn.txnDate}</td>
                  <td className="px-4 py-2.5">
                    <span
                      className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${
                        txn.direction === 'IN' ? 'bg-accent text-white' : 'text-amber'
                      }`}
                      style={txn.direction !== 'IN' ? { backgroundColor: 'rgba(184,101,27,0.1)' } : {}}
                    >
                      {txn.direction === 'IN' ? 'Intrare' : 'Ieșire'}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-ink max-w-xs truncate" title={txn.description}>
                    {txn.description}
                  </td>
                  <td className="px-4 py-2.5 text-ink">{txn.counterparty}</td>
                  <td className="px-4 py-2.5 text-right font-mono tabular-nums font-medium">
                    {(txn.amount || 0).toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
                    <span className="text-muted text-xs ml-1">LEI</span>
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <button
                      onClick={() => handleDelete(txn.id)}
                      className="text-xs text-muted hover:text-danger transition-colors"
                    >
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
          columnDefs={BANK_COLUMNS}
          onSave={handleBatchSave}
          onClose={closeBatchModal}
          saving={batchSaving}
          saveProgress={null}
        />
      )}
    </div>
  )
}
