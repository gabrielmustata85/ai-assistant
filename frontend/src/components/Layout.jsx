import React, { useState, useEffect, useCallback, createContext, useContext } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from './Toast.jsx'
import CompanySwitcher from './CompanySwitcher.jsx'
import AddCompanyModal from './AddCompanyModal.jsx'

const CompanyContext = createContext(null)
export function useCompany() { return useContext(CompanyContext) }

// Iconuri line monocrome (moștenesc culoarea textului) — look profesional, contrast bun pe fundal închis.
const ICONS = {
  panou: <><rect x="3" y="3" width="7" height="7" rx="1.2" /><rect x="14" y="3" width="7" height="7" rx="1.2" /><rect x="3" y="14" width="7" height="7" rx="1.2" /><rect x="14" y="14" width="7" height="7" rx="1.2" /></>,
  facturi: <><path d="M6 2.5h9l3 3v16l-3-1.8-3 1.8-3-1.8-3 1.8V2.5z" /><path d="M8.5 8.5h6M8.5 12h6" /></>,
  angajati: <><circle cx="12" cy="8" r="3.4" /><path d="M5.5 20a6.5 6.5 0 0113 0" /></>,
  cheltuieli: <><rect x="3" y="5.5" width="18" height="13" rx="2" /><path d="M3 10h18" /></>,
  extrase: <><path d="M12 3l9 5H3l9-5z" /><path d="M5 10v8M9.5 10v8M14.5 10v8M19 10v8M3.5 20.5h17" /></>,
  legislatie: <><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 8h6M9 12h6M9 16h4" /></>,
  asistent: <><path d="M21 11.5a7.5 7.5 0 01-10.9 6.7L4 20l1.3-4.2A7.5 7.5 0 1121 11.5z" /></>,
}

function NavIcon({ name }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"
      strokeLinecap="round" strokeLinejoin="round" className="w-[18px] h-[18px] shrink-0">
      {ICONS[name]}
    </svg>
  )
}

const NAV = [
  { to: '/', label: 'Panou', icon: 'panou', end: true },
  { to: '/facturi', label: 'Facturi', icon: 'facturi' },
  { to: '/angajati', label: 'Angajați', icon: 'angajati' },
  { to: '/cheltuieli', label: 'Cheltuieli', icon: 'cheltuieli' },
  { to: '/extrase', label: 'Extrase bancare', icon: 'extrase' },
  { to: '/legislatie', label: 'Legislație', icon: 'legislatie' },
  { to: '/asistent', label: 'Marius', icon: 'asistent' },
]

export default function Layout() {
  const { token, user, logout } = useAuth()
  const { addToast } = useToast()
  const navigate = useNavigate()
  const [companies, setCompanies] = useState([])
  const [selectedCompany, setSelectedCompany] = useState(null)
  const [showAddModal, setShowAddModal] = useState(false)
  const [usage, setUsage] = useState(null)
  const [showUpgrade, setShowUpgrade] = useState(false)

  useEffect(() => {
    apiFetch('/companies', {}, token)
      .then(data => {
        setCompanies(data || [])
        if (data && data.length > 0) setSelectedCompany(data[0])
      })
      .catch(err => addToast(err.message))
  }, [])

  const refreshUsage = useCallback(() => {
    apiFetch('/usage', {}, token).then(setUsage).catch(() => {})
  }, [token])

  useEffect(() => {
    refreshUsage()
    const iv = setInterval(refreshUsage, 15000)   // ține bara la zi după acțiuni AI
    const onQuota = () => { refreshUsage(); setShowUpgrade(true) }
    window.addEventListener('quota-exceeded', onQuota)
    return () => { clearInterval(iv); window.removeEventListener('quota-exceeded', onQuota) }
  }, [refreshUsage])

  const [upgrading, setUpgrading] = useState(false)
  async function handleUpgrade(plan) {
    setUpgrading(true)
    try {
      const s = await apiFetch('/usage/upgrade', { method: 'POST', body: JSON.stringify({ plan }) }, token)
      setUsage(s)
      setShowUpgrade(false)
      addToast(`Plan ${plan} activat — limită nouă: ${fmtTokens(s.limit)} tokens/lună.`, 'success')
    } catch (err) {
      addToast(err.message || 'Nu am putut activa planul.')
    } finally {
      setUpgrading(false)
    }
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <CompanyContext.Provider value={{ selectedCompany, setSelectedCompany, companies }}>
      <div className="flex h-screen overflow-hidden bg-paper">
        <aside className="w-64 flex-shrink-0 bg-ink flex flex-col">
          <div className="px-5 py-5">
            <div className="flex items-center gap-2.5">
              <div className="w-1.5 h-9 bg-accent rounded-full flex-shrink-0" />
              <div className="leading-none">
                <span className="block font-display font-bold text-white text-xl tracking-tight">Marius</span>
                <span className="block text-[11px] text-onDarkMuted mt-1">asistentul tău fiscal</span>
              </div>
            </div>
          </div>

          <div className="px-3 pb-3">
            <p className="text-[10px] text-onDarkMuted uppercase tracking-[0.14em] mb-1.5 px-1">Firmă</p>
            <CompanySwitcher
              companies={companies}
              selectedCompany={selectedCompany}
              onSelect={setSelectedCompany}
              onAddNew={() => setShowAddModal(true)}
            />
          </div>

          <nav className="flex-1 px-3 py-2 space-y-1 overflow-y-auto scroll-dark">
            {NAV.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                    isActive
                      ? 'bg-accent text-white font-semibold shadow-[0_2px_10px_rgba(16,145,110,0.35)]'
                      : 'text-onDarkMuted hover:text-white hover:bg-white/[0.06]'
                  }`
                }
              >
                <NavIcon name={item.icon} />
                {item.label}
              </NavLink>
            ))}
          </nav>

          {usage && <UsageBar usage={usage} onUpgrade={() => setShowUpgrade(true)} />}

          <div className="px-3 py-4 border-t border-white/10">
            <p className="text-xs text-onDarkMuted px-1 mb-2 truncate">{user?.username}</p>
            <button
              onClick={handleLogout}
              className="w-full text-left px-3 py-2 text-sm text-onDarkMuted hover:text-white hover:bg-white/[0.06] rounded-lg transition-colors"
            >
              Deconectare
            </button>
          </div>
        </aside>

        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>

      {showAddModal && (
        <AddCompanyModal
          onClose={() => setShowAddModal(false)}
          onCreated={company => {
            setCompanies(prev => [...prev, company])
            setSelectedCompany(company)
            setShowAddModal(false)
          }}
        />
      )}

      {showUpgrade && usage && (
        <UpgradeModal usage={usage} busy={upgrading} onChoose={handleUpgrade} onClose={() => setShowUpgrade(false)} />
      )}
    </CompanyContext.Provider>
  )
}

function fmtTokens(n) {
  const v = Number(n) || 0
  if (v >= 1000) return `${(v / 1000).toFixed(v >= 100000 ? 0 : 1)}k`
  return String(v)
}

function fmtReset(resetAt) {
  const ms = (Number(resetAt) || 0) - Date.now()
  if (ms <= 0) return 'curând'
  const h = Math.floor(ms / 3600000)
  const m = Math.floor((ms % 3600000) / 60000)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

function UsageBar({ usage, onUpgrade }) {
  const pct = usage.limit > 0 ? Math.min(100, Math.round((usage.used / usage.limit) * 100)) : 0
  const exhausted = usage.used >= usage.limit
  const color = exhausted ? '#C73A2B' : pct >= 80 ? '#B5611A' : '#10916E'
  const isFree = (usage.plan || 'FREE') === 'FREE'
  return (
    <div className="px-3 pt-3">
      <div className="flex items-center justify-between mb-1">
        <span className="text-[10px] uppercase tracking-[0.12em] text-onDarkMuted">
          Consum AI{isFree ? ' · Free' : ` · ${usage.plan}`}
        </span>
        <span className="text-[10px] font-mono text-onDarkMuted">{pct}%</span>
      </div>
      <div className="h-1.5 rounded-full bg-white/10 overflow-hidden">
        <div className="h-full rounded-full transition-all duration-500" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
      <div className="flex items-center justify-between mt-1">
        <span className="text-[10px] font-mono text-onDarkMuted">
          {exhausted && isFree ? `Resetare în ${fmtReset(usage.resetAt)}` : `${fmtTokens(usage.used)} / ${fmtTokens(usage.limit)}`}
        </span>
        {(exhausted || isFree) && (
          <button onClick={onUpgrade} className="text-[10px] font-semibold text-accent hover:underline">
            Upgrade
          </button>
        )}
      </div>
    </div>
  )
}

const PLANS = [
  {
    key: 'PRO', name: 'Pro', tokens: '5.000.000', price: '99 lei', period: '/ lună',
    desc: 'Pentru firme active: estimări, chat cu Marius și generare de facturi fără griji.',
  },
  {
    key: 'MAX', name: 'Max', tokens: '20.000.000', price: '299 lei', period: '/ lună',
    desc: 'Volum mare: contabilitate intensă, multe PDF-uri și documente pe lună.',
    featured: true,
  },
]

function UpgradeModal({ usage, busy, onChoose, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(11,27,46,0.65)' }}>
      <div className="bg-white rounded-2xl p-7 max-w-lg w-full shadow-[0_30px_80px_-20px_rgba(0,0,0,0.6)]">
        <div className="h-0.5 w-10 bg-accent mb-4" />
        <h2 className="font-display font-bold text-xl text-ink mb-1">Ai atins limita de întrebări</h2>
        <p className="text-sm text-muted mb-5">
          Pe planul Free poți folosi asistentul în reprize scurte.
          {' '}Se resetează peste <span className="font-semibold text-ink">{fmtReset(usage.resetAt)}</span> —
          sau treci la un plan și continui <span className="font-semibold text-ink">acum</span>, fără așteptare.
        </p>

        <div className="grid sm:grid-cols-2 gap-3 mb-5">
          {PLANS.map(p => {
            const current = usage.plan === p.key
            return (
              <div
                key={p.key}
                className={`rounded-xl border p-4 flex flex-col ${p.featured ? 'border-accent' : 'border-hairline'}`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-display font-bold text-ink text-lg">{p.name}</span>
                  {p.featured && (
                    <span className="text-[10px] uppercase tracking-wide font-semibold text-accent bg-accent/10 px-2 py-0.5 rounded-full">
                      Recomandat
                    </span>
                  )}
                </div>
                <p className="text-ink">
                  <span className="font-display font-bold text-2xl">{p.price}</span>
                  <span className="text-muted text-sm"> {p.period}</span>
                </p>
                <p className="font-mono text-xs text-accent mt-1">{p.tokens} tokens / lună</p>
                <p className="text-xs text-muted mt-2 flex-1">{p.desc}</p>
                <button
                  disabled={busy || current}
                  onClick={() => onChoose(p.key)}
                  className={`mt-3 rounded-lg px-4 py-2 text-sm font-semibold transition-colors disabled:opacity-60 ${
                    p.featured
                      ? 'bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)]'
                      : 'border border-ink text-ink hover:bg-ink hover:text-white'
                  }`}
                >
                  {current ? 'Plan curent' : busy ? 'Se activează…' : `Alege ${p.name}`}
                </button>
              </div>
            )
          })}
        </div>

        <div className="flex justify-end">
          <button onClick={onClose} className="text-sm text-muted hover:text-ink px-3 py-2 transition-colors">
            Mai târziu
          </button>
        </div>
      </div>
    </div>
  )
}
