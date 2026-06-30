import React, { useState } from 'react'

/**
 * BatchReviewModal — shared component for reviewing batch-parsed PDF results.
 *
 * Props:
 *   items         [{ filename, parsed: object|null, error: string|null }]
 *   columnDefs    [{ key, label, type: 'text'|'number'|'date'|'select'|'checkbox', options?, mono? }]
 *   onSave(rows)  called with edited valid rows (error/removed rows excluded)
 *   onClose()
 *   saving        bool
 *   saveProgress  { done: number, total: number } | null
 */
export default function BatchReviewModal({ items, columnDefs, onSave, onClose, saving, saveProgress }) {
  const [rows, setRows] = useState(() =>
    items.map((item, idx) => ({
      _idx: idx,
      _filename: item.filename,
      _error: item.error || null,
      _removed: false,
      ...(item.parsed || {}),
    }))
  )

  const validRows = rows.filter(r => !r._error && !r._removed)

  function updateRow(idx, key, value) {
    setRows(prev => prev.map(r => r._idx === idx ? { ...r, [key]: value } : r))
  }

  function removeRow(idx) {
    setRows(prev => prev.map(r => r._idx === idx ? { ...r, _removed: true } : r))
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(11,27,46,0.6)' }}>
      <div className="bg-white rounded-2xl shadow-[0_30px_80px_-20px_rgba(0,0,0,0.6)] w-full max-w-7xl max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-hairline flex-shrink-0">
          <div>
            <div className="flex items-center gap-3">
              <div className="w-1.5 h-6 bg-accent rounded-full" />
              <h2 className="font-display font-bold text-lg text-ink">Verifică datele extrase</h2>
            </div>
            <p className="text-xs text-muted mt-0.5 ml-4">
              {items.length} fișier{items.length !== 1 ? 'e' : ''} procesate &middot;{' '}
              {validRows.length} gata de salvat
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            className="text-muted hover:text-danger text-lg leading-none px-2 py-1 disabled:opacity-40"
          >
            ✕
          </button>
        </div>

        {/* Table */}
        <div className="overflow-auto flex-1">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-ink z-10">
              <tr>
                <th className="text-left px-3 py-2.5 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide whitespace-nowrap">Fișier</th>
                {columnDefs.map(col => (
                  <th
                    key={col.key}
                    className={`px-3 py-2.5 text-[11px] text-onDarkMuted font-semibold uppercase tracking-wide whitespace-nowrap ${col.mono ? 'text-right' : 'text-left'}`}
                  >
                    {col.label}
                  </th>
                ))}
                <th className="px-3 py-2.5 w-8" />
              </tr>
            </thead>
            <tbody>
              {rows.filter(r => !r._removed).map(row => {
                if (row._error) {
                  const isScanned = /scan|image|ocr/i.test(row._error)
                  return (
                    <tr key={row._idx} className="border-t border-hairline bg-red-50">
                      <td className="px-3 py-2.5 text-xs font-mono text-muted max-w-xs truncate" title={row._filename}>
                        {row._filename}
                      </td>
                      <td colSpan={columnDefs.length} className="px-3 py-2.5 text-xs text-danger">
                        {isScanned
                          ? 'PDF-urile scanate nu pot fi citite automat'
                          : row._error}
                      </td>
                      <td className="px-3 py-2.5 text-center">
                        <button
                          type="button"
                          onClick={() => removeRow(row._idx)}
                          className="text-xs text-muted hover:text-danger"
                          title="Elimină"
                        >
                          ✕
                        </button>
                      </td>
                    </tr>
                  )
                }

                return (
                  <tr key={row._idx} className="border-t border-hairline even:bg-[#F4F6F8] hover:bg-[#EAF3F0] transition-colors">
                    <td className="px-3 py-2 text-xs font-mono text-muted max-w-xs truncate" title={row._filename}>
                      {row._filename}
                    </td>
                    {columnDefs.map(col => (
                      <td key={col.key} className={`px-1.5 py-1 ${col.mono ? 'text-right' : ''}`}>
                        {col.type === 'checkbox' ? (
                          <div className={col.mono ? 'flex justify-end' : 'flex justify-start'}>
                            <input
                              type="checkbox"
                              checked={Boolean(row[col.key])}
                              onChange={e => updateRow(row._idx, col.key, e.target.checked)}
                              className="accent-accent"
                            />
                          </div>
                        ) : col.type === 'select' ? (
                          <select
                            value={row[col.key] || ''}
                            onChange={e => updateRow(row._idx, col.key, e.target.value)}
                            className="border border-hairline rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-accent bg-white"
                          >
                            {(col.options || []).map(opt => (
                              <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                          </select>
                        ) : (
                          <input
                            type={col.type === 'number' ? 'number' : col.type === 'date' ? 'date' : 'text'}
                            step={col.type === 'number' ? '0.01' : undefined}
                            value={row[col.key] ?? ''}
                            onChange={e => updateRow(row._idx, col.key, e.target.value)}
                            className={`border border-hairline rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-accent ${col.mono ? 'font-mono tabular-nums text-right' : ''}`}
                            style={{
                              minWidth: col.type === 'number' ? '80px' : col.type === 'date' ? '112px' : '90px',
                            }}
                          />
                        )}
                      </td>
                    ))}
                    <td className="px-1.5 py-1 text-center">
                      <button
                        type="button"
                        onClick={() => removeRow(row._idx)}
                        className="text-xs text-muted hover:text-danger"
                        title="Elimină rândul"
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-hairline flex-shrink-0 flex items-center justify-between gap-4">
          <div className="text-xs text-muted">
            {saveProgress
              ? `Se salvează ${saveProgress.done} din ${saveProgress.total}…`
              : `${validRows.length} intrări valide`}
          </div>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="border border-hairline rounded-lg px-4 py-2 text-sm text-muted hover:bg-paper transition-colors disabled:opacity-40"
            >
              Anulează
            </button>
            <button
              type="button"
              disabled={saving || validRows.length === 0}
              onClick={() => onSave(validRows)}
              className="bg-accent hover:bg-accentHover text-white rounded-lg px-5 py-2 text-sm font-semibold transition-colors disabled:opacity-60 shadow-[0_2px_8px_rgba(16,145,110,0.25)]"
            >
              {saving
                ? 'Se salvează…'
                : `Salvează toate (${validRows.length})`}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
