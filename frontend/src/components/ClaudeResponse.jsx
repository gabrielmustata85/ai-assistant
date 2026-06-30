import React from 'react'

export default function ClaudeResponse({ data, onReset }) {
  if (!data) return null
  return (
    <div className="space-y-4">
      <div className="bg-white border border-hairline rounded-lg p-4">
        <div className="h-0.5 w-8 bg-accent mb-3" />
        <p className="text-sm text-ink leading-relaxed whitespace-pre-wrap">{data.raspuns}</p>
      </div>

      {data.estimari && data.estimari.length > 0 && (
        <div className="bg-white border border-hairline rounded-lg overflow-hidden">
          <div className="px-4 pt-4 pb-2 flex items-center gap-2 border-b border-hairline">
            <div className="w-2 h-2 rounded-sm bg-accent" />
            <h3 className="font-display font-semibold text-sm text-ink">Estimări taxe</h3>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-paper">
                <th className="text-left px-4 py-2 text-xs text-muted font-medium">Taxă</th>
                <th className="text-right px-4 py-2 text-xs text-muted font-medium">Sumă</th>
                <th className="text-left px-4 py-2 text-xs text-muted font-medium">Perioadă</th>
              </tr>
            </thead>
            <tbody>
              {data.estimari.map((e, i) => (
                <tr key={i} className="border-t border-hairline hover:bg-paper transition-colors">
                  <td className="px-4 py-2.5 text-ink">{e.tipTaxa}</td>
                  <td className="px-4 py-2.5 text-right font-mono tabular-nums">
                    {e.suma} <span className="text-muted text-xs">LEI</span>
                  </td>
                  <td className="px-4 py-2.5 font-mono text-muted text-xs">{e.perioada}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data.termene && data.termene.length > 0 && (
        <div className="bg-white border border-hairline rounded-lg overflow-hidden">
          <div className="px-4 pt-4 pb-2 flex items-center gap-2 border-b border-hairline">
            <div className="w-2 h-2 rounded-sm bg-amber" />
            <h3 className="font-display font-semibold text-sm text-ink">Termene</h3>
          </div>
          <ul className="divide-y divide-hairline">
            {data.termene.map((t, i) => (
              <li key={i} className="px-4 py-2.5 flex items-center justify-between hover:bg-paper transition-colors">
                <span className="text-sm text-ink">{t.obligatie}</span>
                <span className="font-mono text-xs text-amber tabular-nums">{t.scadenta}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {data.recomandari && data.recomandari.length > 0 && (
        <div className="bg-white border border-hairline rounded-lg overflow-hidden">
          <div className="px-4 pt-4 pb-2 flex items-center gap-2 border-b border-hairline">
            <div className="w-2 h-2 rounded-sm bg-accent" />
            <h3 className="font-display font-semibold text-sm text-ink">Recomandări</h3>
          </div>
          <ul className="divide-y divide-hairline">
            {data.recomandari.map((r, i) => (
              <li key={i} className="px-4 py-2.5 hover:bg-paper transition-colors">
                <p className="text-sm text-ink">{r.text}</p>
                {r.impactEstimat && (
                  <p className="text-xs text-muted mt-0.5">Impact: {r.impactEstimat}</p>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {data.dataGaps && data.dataGaps.length > 0 && (
        <div className="border border-amber rounded-lg p-4" style={{backgroundColor: 'rgba(184,101,27,0.05)'}}>
          <h3 className="font-display font-semibold text-sm text-amber mb-2">Date lipsă</h3>
          <ul className="space-y-1">
            {data.dataGaps.map((g, i) => (
              <li key={i} className="text-sm text-ink flex items-start gap-2">
                <span className="text-amber mt-0.5">·</span>{g}
              </li>
            ))}
          </ul>
        </div>
      )}

      {data.disclaimer && (
        <p className="text-xs text-muted px-1 italic">{data.disclaimer}</p>
      )}

      {onReset && (
        <button onClick={onReset} className="text-xs text-muted hover:text-danger underline">
          Reset conversație
        </button>
      )}
    </div>
  )
}
