import React, { useState, useEffect, createContext, useContext } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from './Toast.jsx'
import CompanySwitcher from './CompanySwitcher.jsx'
import AddCompanyModal from './AddCompanyModal.jsx'

const CompanyContext = createContext(null)
export function useCompany() { return useContext(CompanyContext) }

const NAV = [
  { to: '/', label: 'Panou', icon: '▦', end: true },
  { to: '/facturi', label: 'Facturi', icon: '🧾' },
  { to: '/angajati', label: 'Angajați', icon: '👤' },
  { to: '/cheltuieli', label: 'Cheltuieli', icon: '💳' },
  { to: '/legislatie', label: 'Legislație', icon: '📋' },
  { to: '/asistent', label: 'Asistent', icon: '💬' },
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
        <aside className="w-60 flex-shrink-0 bg-white border-r border-hairline flex flex-col">
          <div className="px-5 py-5 border-b border-hairline">
            <div className="flex items-center gap-2">
              <div className="w-2 h-6 bg-accent rounded-sm flex-shrink-0" />
              <span className="font-display font-bold text-ink text-lg leading-tight">
                Asistent<br />Fiscal
              </span>
            </div>
          </div>

          <div className="px-3 py-3 border-b border-hairline">
            <p className="text-xs text-muted uppercase tracking-wide mb-1.5 px-1">Firmă</p>
            <CompanySwitcher
              companies={companies}
              selectedCompany={selectedCompany}
              onSelect={setSelectedCompany}
              onAddNew={() => setShowAddModal(true)}
            />
          </div>

          <nav className="flex-1 px-3 py-3 space-y-0.5 overflow-y-auto">
            {NAV.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors ${
                    isActive
                      ? 'bg-accent text-white font-medium'
                      : 'text-ink hover:bg-paper'
                  }`
                }
              >
                <span className="text-base">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="px-3 py-4 border-t border-hairline">
            <p className="text-xs text-muted px-1 mb-2 truncate">{user?.username}</p>
            <button
              onClick={handleLogout}
              className="w-full text-left px-3 py-2 text-sm text-muted hover:text-danger hover:bg-red-50 rounded-lg transition-colors"
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
