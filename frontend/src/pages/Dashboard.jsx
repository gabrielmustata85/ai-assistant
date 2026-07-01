import React, { useState, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import UrgencyBadge from '../components/UrgencyBadge.jsx'
import EditCompanyModal from '../components/EditCompanyModal.jsx'
import { parseDeadline, daysUntil, urgencyColor, URGENCY_COLORS } from '../lib/urgency.js'

const INK = '#0B1B2E'
const ACCENT = URGENCY_COLORS.calm
const HORIZON = 45 // câte zile arată scadențarul

function lei(n) {
  return (Number(n) || 0).toLocaleString('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export default function Dashboard() {
  const { selectedCompany, updateCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [showEditCompany, setShowEditCompany] = useState(false)
  const [invoiceData, setInvoiceData] = useState([])
  const [employeeData, setEmployeeData] = useState([])
  const [expenseData, setExpenseData] = useState([])
  const [obligations, setObligations] = useState(null)
  const [loadingObl, setLoadingObl] = useState(false)

  useEffect(() => {
    if (!selectedCompany) return
    const id = selectedCompany.id
    setObligations(null)
    apiFetch(`/companies/${id}/invoices`, {}, token).then(d => setInvoiceData(d || [])).catch(() => {})
    apiFetch(`/companies/${id}/employees`, {}, token).then(d => setEmployeeData(d || [])).catch(() => {})
    apiFetch(`/companies/${id}/expenses`, {}, token).then(d => setExpenseData(d || [])).catch(() => {})
    loadObligations()
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

  if (!selectedCompany) {
    return (
      <div className="flex items-center justify-center h-full p-8">
        <div className="text-center max-w-sm">
          <div className="w-12 h-12 rounded-lg flex items-center justify-center mx-auto mb-4" style={{ backgroundColor: 'rgba(31,111,92,0.1)' }}>
            <span className="text-accent text-2xl">▦</span>
          </div>
          <h2 className="font-display font-semibold text-ink mb-2">Nicio firmă selectată</h2>
          <p className="text-sm text-muted">Adaugă sau selectează o firmă din bara laterală pentru a vedea panoul.</p>
        </div>
      </div>
    )
  }

  const totalGross = invoiceData.reduce((s, i) => s + (i.grossAmount || 0), 0)
  const activeEmployees = employeeData.filter(e => e.active)
  const totalSalary = activeEmployees.reduce((s, e) => s + (e.grossSalary || 0), 0)

  const estimari = obligations?.estimari || []
  const termene = obligations?.termene || []
  const recomandari = obligations?.recomandari || []
  const dataGaps = obligations?.dataGaps || []
  const totalDue = estimari.reduce((s, e) => s + (Number(e.suma) || 0), 0)

  // Scadențarul: termene cu dată parsabilă, ordonate; markeri pe axa „azi → HORIZON zile".
  const markers = termene
    .map(t => {
      const date = parseDeadline(t.scadenta)
      return date ? { ...t, date, days: daysUntil(date) } : null
    })
    .filter(Boolean)
    .sort((a, b) => a.date - b.date)
  const next = markers[0] || termene[0] || null
  const nextDays = next?.days

  return (
    <div className="p-8 max-w-3xl mx-auto">
      {/* Antet firmă */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-display font-bold text-3xl text-ink leading-tight">{selectedCompany.name}</h1>
          <p className="text-xs text-muted mt-1.5 uppercase tracking-wide">
            <span className="font-mono normal-case">{selectedCompany.cui}</span>
            {selectedCompany.taxRegime ? <> · {selectedCompany.taxRegime.replace('_', ' ')}</> : null}
            {selectedCompany.vatPayer ? ' · plătitor TVA' : ''}
          </p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => setShowEditCompany(true)}
            className="border border-hairline text-muted hover:bg-paper hover:text-ink text-sm font-medium px-3 py-2 rounded-lg transition-colors"
          >
            Editează firma
          </button>
          <button
            onClick={loadObligations}
            disabled={loadingObl}
            className="bg-ink text-white hover:bg-inkSoft text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-60"
          >
            {loadingObl ? 'Se calculează…' : '↻ Recalculează'}
          </button>
        </div>
      </div>

      {showEditCompany && (
        <EditCompanyModal
          company={selectedCompany}
          onClose={() => setShowEditCompany(false)}
          onSaved={(updated) => { updateCompany(updated); setShowEditCompany(false) }}
        />
      )}

      {/* EROU — De plată + scadențar (bloc întunecat, contrast puternic) */}
      <div className="bg-ink rounded-2xl overflow-hidden mb-4 shadow-[0_10px_40px_-12px_rgba(11,27,46,0.5)]">
        <div className="h-1.5" style={{ backgroundColor: nextDays != null ? urgencyColor(nextDays) : ACCENT }} />
        <div className="p-7">
          {loadingObl && !obligations ? (
            <p className="text-sm text-onDarkMuted py-6">Se analizează datele firmei și se estimează taxele…</p>
          ) : !obligations ? (
            <p className="text-sm text-onDarkMuted py-6">Nu s-au putut calcula obligațiile. Apasă „Recalculează”.</p>
          ) : totalDue > 0 ? (
            <>
              <p className="text-[11px] text-onDarkMuted uppercase tracking-[0.16em] mb-3">De plată în perioada următoare</p>
              <div className="flex items-baseline gap-2.5">
                <span className="font-display font-bold text-white tabular-nums" style={{ fontSize: '3.5rem', lineHeight: 0.95 }}>
                  {lei(totalDue)}
                </span>
                <span className="text-lg text-onDarkMuted font-medium">lei</span>
              </div>

              {next && (
                <Scadentar markers={markers} next={next} dark />
              )}

              {obligations.raspuns && (
                <p className="text-sm text-onDarkMuted mt-5 leading-relaxed border-t border-white/10 pt-4">{obligations.raspuns}</p>
              )}
            </>
          ) : (
            <p className="text-sm text-onDark leading-relaxed py-2">{obligations.raspuns}</p>
          )}
        </div>
      </div>

      {/* Defalcare estimată — stil registru */}
      {estimari.length > 0 && (
        <div className="bg-white border border-hairline rounded-2xl p-5 mb-4 shadow-sm">
          <h2 className="font-display font-semibold text-sm text-ink mb-3">Defalcare estimată</h2>
          <ul>
            {estimari.map((e, i) => (
              <li key={i} className="flex items-baseline gap-3 py-2 border-t border-hairline first:border-t-0">
                <span className="text-sm text-ink flex-1">{e.tipTaxa}</span>
                {e.perioada && <span className="font-mono text-xs text-muted">{e.perioada}</span>}
                <span className="font-mono tabular-nums text-sm text-ink w-32 text-right">
                  {lei(e.suma)} <span className="text-muted text-xs">lei</span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Cum scazi taxele */}
      {recomandari.length > 0 && (
        <div className="bg-white border border-hairline rounded-2xl p-5 mb-4 shadow-sm">
          <h2 className="font-display font-semibold text-sm text-ink mb-3 flex items-center gap-2">
            <span className="w-2 h-2 rounded-sm" style={{ backgroundColor: ACCENT }} /> Cum poți scădea taxele
          </h2>
          <ul className="space-y-3">
            {recomandari.map((r, i) => (
              <li key={i} className="text-sm text-ink flex items-start gap-2.5">
                <span style={{ color: ACCENT }} className="mt-0.5">→</span>
                <span>
                  {r.text}
                  {r.impactEstimat && <span className="text-muted"> ({r.impactEstimat})</span>}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Date lipsă */}
      {dataGaps.length > 0 && (
        <div className="border border-amber rounded-xl p-4 mb-4" style={{ backgroundColor: 'rgba(184,101,27,0.05)' }}>
          <p className="text-sm text-amber font-medium mb-1">Ca să fie mai exact, mai adaugă:</p>
          <ul className="space-y-1">
            {dataGaps.map((g, i) => (
              <li key={i} className="text-sm text-ink flex items-start gap-2">
                <span className="text-amber mt-0.5">·</span>{g}
              </li>
            ))}
          </ul>
        </div>
      )}

      {obligations?.disclaimer && (
        <p className="text-xs text-muted px-1 italic mb-5">{obligations.disclaimer}</p>
      )}

      {/* Datele firmei — bandă discretă, subordonată eroului */}
      <div className="flex flex-wrap items-stretch gap-x-8 gap-y-3 bg-white border border-hairline rounded-2xl px-5 py-4 shadow-sm">
        <MiniStat label="Facturi" value={invoiceData.length} />
        <Divider />
        <MiniStat label="Total facturat" value={`${lei(totalGross)} lei`} mono />
        <Divider />
        <MiniStat label="Angajați activi" value={activeEmployees.length} />
        <Divider />
        <MiniStat label="Fond salarii / lună" value={`${lei(totalSalary)} lei`} mono />
      </div>
    </div>
  )
}

function Scadentar({ markers, next, dark }) {
  const within = markers.filter(m => m.days != null && m.days <= HORIZON + 5)
  const pos = days => `${Math.min(Math.max(days, 0), HORIZON) / HORIZON * 100}%`
  const urgent = next.days != null && next.days <= 7

  const lineColor = dark ? 'rgba(255,255,255,0.18)' : '#D5DBE2'
  const todayDot = dark ? '#FFFFFF' : INK
  const ringColor = dark ? '#0B1B2E' : '#FFFFFF'
  const labelCls = dark ? 'text-onDarkMuted' : 'text-muted'
  const calloutCls = dark ? 'text-onDark' : 'text-ink'

  return (
    <div className="mt-6">
      {/* Callout pe scadența cea mai apropiată */}
      <div className="flex items-center gap-2 mb-3">
        <span className={`text-sm ${calloutCls}`}>
          {next.obligatie ? <span className="font-semibold">{next.obligatie}</span> : 'Următoarea scadență'}
          {' · '}
          <span className="font-mono text-xs">{next.scadenta}</span>
        </span>
        <UrgencyBadge date={next.scadenta} />
      </div>

      {/* Axa: azi → HORIZON zile */}
      {within.length > 0 ? (
        <>
          <div className="relative h-8">
            <div className="absolute left-0 right-0 top-1/2 h-px" style={{ backgroundColor: lineColor }} />
            <div className="absolute top-1/2 -translate-y-1/2 w-2 h-2 rounded-full" style={{ left: 0, backgroundColor: todayDot }} />
            {within.map((m, i) => {
              const color = urgencyColor(m.days)
              const isUrgent = m.days != null && m.days <= 7
              return (
                <div key={i} className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2" style={{ left: pos(m.days) }}>
                  {isUrgent && (
                    <span className="absolute inset-0 rounded-full animate-ping motion-reduce:hidden" style={{ backgroundColor: color, opacity: 0.4 }} />
                  )}
                  <span className="relative block w-2.5 h-2.5 rounded-full" style={{ backgroundColor: color, boxShadow: `0 0 0 2px ${ringColor}` }} title={`${m.obligatie || ''} · ${m.scadenta}`} />
                </div>
              )
            })}
          </div>
          <div className={`flex justify-between text-[10px] uppercase tracking-wide mt-0.5 ${labelCls}`}>
            <span>azi</span>
            <span>{HORIZON} zile</span>
          </div>
        </>
      ) : (
        <p className={`text-xs ${labelCls}`}>Scadențe estimate dincolo de {HORIZON} de zile.</p>
      )}

      {urgent && (
        <p className="text-xs mt-2 font-medium" style={{ color: dark ? '#FF8A6B' : urgencyColor(next.days) }}>
          Atenție: scadența cea mai apropiată e foarte aproape.
        </p>
      )}
    </div>
  )
}

function MiniStat({ label, value, mono }) {
  return (
    <div className="min-w-0">
      <p className="text-[11px] text-muted uppercase tracking-wide mb-0.5">{label}</p>
      <p className={`text-ink font-semibold ${mono ? 'font-mono tabular-nums text-sm' : 'text-lg font-display'}`}>{value}</p>
    </div>
  )
}

function Divider() {
  return <div className="w-px self-stretch bg-hairline" />
}
