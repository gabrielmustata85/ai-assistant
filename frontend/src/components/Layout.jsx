import React, { useState, useEffect, createContext, useContext } from 'react'
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
  { to: '/asistent', label: 'Asistent', icon: 'asistent' },
]

export default function Layout() {
  const { token, user, logout } = useAuth()
  const { addToast } = useToast()
  const navigate = useNavigate()
  const [companies, setCompanies] = useState([])
  const [selectedCompany, setSelectedCompany] = useState(null)
  const [showAddModal, setShowAddModal] = useState(false)

  useEffect(() => {
    apiFetch('/companies', {}, token)
      .then(data => {
        setCompanies(data || [])
        if (data && data.length > 0) setSelectedCompany(data[0])
      })
      .catch(err => addToast(err.message))
  }, [])

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
              <div className="w-1.5 h-7 bg-accent rounded-full flex-shrink-0" />
              <span className="font-display font-bold text-white text-lg leading-none tracking-tight">
                Asistent Fiscal
              </span>
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
    </CompanyContext.Provider>
  )
}
