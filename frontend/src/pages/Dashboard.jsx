import React, { useState, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import { parseDeadline, DeadlineBadge } from '../components/ClaudeResponse.jsx'

export default function Dashboard() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [obligations, setObligations] = useState(null)
  const [loadingObl, setLoadingObl] = useState(false)

  useEffect(() => {
    if (!selectedCompany) return
    setObligations(null)
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

  const estimari = obligations?.estimari || []
  const termene = obligations?.termene || []
  const recomandari = obligations?.recomandari || []
  const dataGaps = obligations?.dataGaps || []
  const totalDue = estimari.reduce((s, e) => s + (Number(e.suma) || 0), 0)

  // Cea mai apropiată scadență (cu dată parsabilă), altfel primul termen.
  const dated = termene
    .map(t => ({ ...t, date: parseDeadline(t.scadenta) }))
    .filter(t => t.date)
    .sort((a, b) => a.date - b.date)
  const next = dated[0] || termene[0] || null

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h1 className="font-display font-bold text-2xl text-ink">{selectedCompany.name}</h1>
          <p className="text-xs text-muted mt-0.5 font-mono">{selectedCompany.cui}</p>
        </div>
        <button
          onClick={loadObligations}
          disabled={loadingObl}
          className="border border-hairline text-muted hover:bg-paper text-sm font-medium px-3 py-1.5 rounded-lg transition-colors disabled:opacity-60"
        >
          {loadingObl ? 'Se calculează…' : '↻ Recalculează'}
        </button>
      </div>

      {loadingObl && !obligations && (
        <div className="bg-white border border-hairline rounded-lg p-6 text-sm text-muted">
          Se analizează datele firmei și se estimează taxele…
        </div>
      )}

      {!loadingObl && !obligations && (
        <div className="bg-white border border-hairline rounded-lg p-6 text-sm text-muted">
          Nu s-au putut calcula obligațiile. Apasă „Recalculează”.
        </div>
      )}

      {obligations && (
        <div className="space-y-4">
          {/* De plată — suma și termenul cel mai apropiat */}
          <div className="bg-white border border-hairline rounded-lg p-6">
            <div className="h-0.5 w-8 bg-accent mb-4" />
            {totalDue > 0 ? (
              <>
                <p className="text-xs text-muted uppercase tracking-wide mb-1">De plată</p>
                <p className="font-display font-bold text-4xl text-ink tabular-nums">
                  {totalDue.toLocaleString('ro-RO', { minimumFractionDigits: 2 })}
                  <span className="text-lg text-muted font-sans ml-2">LEI</span>
                </p>
                {next && (
                  <div className="flex items-center gap-2 mt-3">
                    <span className="text-sm text-ink">
                      până la <span className="font-mono">{next.scadenta}</span>
                      {next.obligatie ? <span className="text-muted"> · {next.obligatie}</span> : null}
                    </span>
                    <DeadlineBadge scadenta={next.scadenta} />
                  </div>
                )}
              </>
            ) : (
              <p className="text-sm text-ink leading-relaxed">{obligations.raspuns}</p>
            )}
          </div>

          {/* Sugestii de optimizare */}
          {recomandari.length > 0 && (
            <div className="bg-white border border-hairline rounded-lg p-5">
              <h2 className="font-display font-semibold text-sm text-ink mb-3 flex items-center gap-2">
                <span className="w-2 h-2 rounded-sm bg-accent" /> Cum poți scădea taxele
              </h2>
              <ul className="space-y-3">
                {recomandari.map((r, i) => (
                  <li key={i} className="text-sm text-ink flex items-start gap-2">
                    <span className="text-accent mt-0.5">→</span>
                    <span>
                      {r.text}
                      {r.impactEstimat && (
                        <span className="text-muted"> ({r.impactEstimat})</span>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Date lipsă — ce mai trebuie introdus */}
          {dataGaps.length > 0 && (
            <div className="border border-amber rounded-lg p-4" style={{ backgroundColor: 'rgba(184,101,27,0.05)' }}>
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

          {obligations.disclaimer && (
            <p className="text-xs text-muted px-1 italic">{obligations.disclaimer}</p>
          )}
        </div>
      )}
    </div>
  )
}
