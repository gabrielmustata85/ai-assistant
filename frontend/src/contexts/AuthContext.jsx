import React, { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('fiscal_token'))
  const [user, setUser] = useState(() => {
    const u = localStorage.getItem('fiscal_user')
    return u ? JSON.parse(u) : null
  })

  function login(newToken, newUser) {
    localStorage.setItem('fiscal_token', newToken)
    localStorage.setItem('fiscal_user', JSON.stringify(newUser))
    setToken(newToken)
    setUser(newUser)
  }

  function logout() {
    localStorage.removeItem('fiscal_token')
    localStorage.removeItem('fiscal_user')
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ token, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
