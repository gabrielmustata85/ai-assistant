import React, { useState, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import ClaudeResponse from '../components/ClaudeResponse.jsx'

function StatCard({ label, value, sub }) {
  return (
    <div className="bg-white border border-hairline rounded-lg p-5">
      <div className="h-0.5 w-6 bg-accent mb-3" />
      <p className="text-xs text-muted uppercase tracking-wide mb-1">{label}</p>
      <p className="font-display font-bold text-2xl text-ink">{value}</p>
      {sub && <p className="font-mono text-xs text-muted mt-1 tabular-nums">{sub}</p>}
    </div>
  )
}

export default function Dashboard() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [invoiceData, setInvoiceData] = useState([])
  const [employeeData, setEmployeeData] = useState([])
  const [expenseData, setExpenseData] = useState([])
  const [obligations, setObligations] = useState(null)
  const [loadingObl, setLoadingObl] = useState(false)

  useEffect(() => {
    if (!selectedCompany) return
    const id = selectedCompany.id
    apiFetch(`/companies/${id}/invoices`, {}, token).then(d => setInvoiceData(d || [])).catch(() => {})
    apiFetch(`/companies/${id}/employees`, {}, token).then(d => setEmployeeData(d || [])).catch(() => {})
    apiFetch(`/companies/${id}/expenses`, {}, token).then(d => setExpenseData(d || [])).catch(() => {})
  }, [selectedCompany?.id])

  async function loadObligations() {
    if (!selectedCompany) return
    setLoadingObl(true)
    try {
      const res = await apiFetch(`/advisor/obligations/${selectedCompany.id}`, {}, token)
      setObligations(res)
    } catch (err) {
      addToast(err.message)
    } finally {
      setLoadingObl(false)
    }
  }

  const totalGross = invoiceData.reduce((s, i) => s + (i.grossAmount || 0), 0)
  const totalSalary = employeeData.filter(e => e.active).reduce((s, e) => s + (e.grossSalary || 0), 0)

  if (!selectedCompany) {
    return (
      <div className="flex items-center justify-center h-full p-8">
        <div className="text-center max-w-sm">
          <div className="w-12 h-12 rounded-lg flex items-center justify-center mx-auto mb-4" style={{backgroundColor: 'rgba(31,111,92,0.1)'}}>
            <span className="text-accent text-2xl">▦</span>
          </div>
          <h2 className="font-display font-semibold text-ink mb-2">Nicio firmă selectată</h2>
          <p className="text-sm text-muted">Adaugă sau selectează o firmă din bara laterală pentru a vedea panoul.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="font-display font-bold text-2xl text-ink">{selectedCompany.name}</h1>
        <p className="text-sm text-muted mt-1">
          <span className="font-mono">{selectedCompany.cui}</span>
          {' · '}{selectedCompany.companyType}
          {' · '}{selectedCompany.taxRegime?.replace('_', ' ')}
          {selectedCompany.vatPayer ? ' · Plătitor TVA' : ''}
        </p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatCard label="Facturi" value={invoiceData.length} />
        <StatCard
          label="Total facturi"
          value={<span className="font-mono tabular-nums text-xl">{totalGross.toLocaleString('ro-RO', { maximumFractionDigits: 2 })}</span>}
          sub="LEI brut"
        />
        <StatCard label="Angajați activi" value={employeeData.filter(e => e.active).length} />
        <StatCard
          label="Fond salarii"
          value={<span className="font-mono tabular-nums text-xl">{totalSalary.toLocaleString('ro-RO', { maximumFractionDigits: 2 })}</span>}
          sub="LEI/lună brut"
        />
      </div>

      <div className="bg-white border border-hairline rounded-lg p-5 mb-6">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-display font-semibold text-ink">Obligații fiscale</h2>
          <button
            onClick={loadObligations}
            disabled={loadingObl}
            className="bg-accent hover:bg-accentHover text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-60"
          >
            {loadingObl ? 'Se calculează...' : 'Ce taxe am de plătit?'}
          </button>
        </div>
        {obligations ? <ClaudeResponse data={obligations} /> : (
          <p className="text-sm text-muted">Apasă butonul pentru a vedea obligațiile fiscale ale firmei.</p>
        )}
      </div>
    </div>
  )
}
