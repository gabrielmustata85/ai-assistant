import React, { useState, useRef, useEffect } from 'react'

export default function CompanySwitcher({ companies, selectedCompany, onSelect, onAddNew }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center justify-between gap-2 bg-paper border border-hairline rounded-lg px-3 py-2 text-sm text-ink hover:border-accent transition-colors focus:outline-none focus:ring-2 focus:ring-accent"
      >
        <span className="truncate font-medium">
          {selectedCompany ? selectedCompany.name : 'Selectează firma'}
        </span>
        <svg className="w-4 h-4 text-muted flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-hairline rounded-lg shadow-md z-30 overflow-hidden">
          {companies.length === 0 && (
            <p className="px-3 py-2 text-sm text-muted italic">Nicio firmă</p>
          )}
          {companies.map(c => (
            <button
              key={c.id}
              onClick={() => { onSelect(c); setOpen(false) }}
              className={`w-full text-left px-3 py-2 text-sm hover:bg-paper transition-colors ${
                selectedCompany?.id === c.id ? 'text-accent font-medium' : 'text-ink'
              }`}
            >
              <span className="block truncate">{c.name}</span>
              <span className="text-xs text-muted">{c.companyType} · {c.cui}</span>
            </button>
          ))}
          <div className="border-t border-hairline">
            <button
              onClick={() => { onAddNew(); setOpen(false) }}
              className="w-full text-left px-3 py-2 text-sm text-accent hover:bg-paper transition-colors font-medium"
            >
              ＋ Firmă nouă
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
