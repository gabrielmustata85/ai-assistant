import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext.jsx'
import { ToastProvider } from './components/Toast.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Facturi from './pages/Facturi.jsx'
import Angajati from './pages/Angajati.jsx'
import Cheltuieli from './pages/Cheltuieli.jsx'
import Legislatie from './pages/Legislatie.jsx'
import Asistent from './pages/Asistent.jsx'
import ExtraseBancare from './pages/ExtraseBancare.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Dashboard />} />
              <Route path="facturi" element={<Facturi />} />
              <Route path="angajati" element={<Angajati />} />
              <Route path="cheltuieli" element={<Cheltuieli />} />
              <Route path="legislatie" element={<Legislatie />} />
              <Route path="asistent" element={<Asistent />} />
              <Route path="extrase" element={<ExtraseBancare />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
