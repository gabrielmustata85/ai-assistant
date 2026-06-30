import React, { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'

export default function Legislatie() {
  const { token } = useAuth()
  const { addToast } = useToast()
  const [docs, setDocs] = useState([])
  const [uploading, setUploading] = useState(false)
  const [file, setFile] = useState(null)

  useEffect(() => {
    apiFetch('/knowledge/documents', {}, token)
      .then(d => setDocs(d || []))
      .catch(err => addToast(err.message))
  }, [])

  async function handleUpload(e) {
    e.preventDefault()
    if (!file) return
    setUploading(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const doc = await apiFetch('/knowledge/upload', { method: 'POST', body: fd }, token)
      setDocs(prev => [...prev, doc])
      setFile(null)
      e.target.reset()
      addToast('Document încărcat!', 'success')
    } catch (err) {
      addToast(err.message)
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="font-display font-bold text-2xl text-ink mb-2">Legislație</h1>
      <p className="text-sm text-muted mb-6">
        Documentele PDF încărcate sunt indexate și folosite de asistentul fiscal pentru răspunsuri mai precise (RAG).
      </p>

      <div className="bg-white border border-hairline rounded-lg p-5 mb-6">
        <div className="h-0.5 w-8 bg-accent mb-4" />
        <h2 className="font-display font-semibold text-ink mb-3">Încarcă document legislativ</h2>
        <form onSubmit={handleUpload} className="flex items-end gap-3">
          <div className="flex-1">
            <label className="block text-xs text-muted mb-1">Fișier PDF</label>
            <input
              type="file" accept=".pdf"
              onChange={e => setFile(e.target.files[0])}
              className="w-full border border-hairline rounded-lg px-3 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent"
            />
          </div>
          <button type="submit" disabled={uploading || !file}
            className="bg-accent hover:bg-accentHover text-white font-medium px-4 py-2 rounded-lg text-sm transition-colors disabled:opacity-60 whitespace-nowrap">
            {uploading ? 'Se încarcă...' : 'Încarcă'}
          </button>
        </form>
      </div>

      <div className="bg-white border border-hairline rounded-2xl overflow-hidden shadow-sm">
        <div className="h-0.5 bg-accent" />
        <div className="px-4 pt-4 pb-2 border-b border-hairline">
          <h2 className="font-display font-semibold text-ink text-sm">Documente indexate ({docs.length})</h2>
        </div>
        {docs.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted">Niciun document încărcat.</div>
        ) : (
          <ul className="divide-y divide-hairline">
            {docs.map(d => (
              <li key={d.id} className="px-4 py-3 flex items-center justify-between hover:bg-paper transition-colors">
                <div>
                  <p className="text-sm font-medium text-ink">{d.title || d.source}</p>
                  <p className="text-xs text-muted font-mono tabular-nums mt-0.5">{d.uploadedAt ? d.uploadedAt.slice(0, 10) : ''}</p>
                </div>
                <span className="text-xs text-accent px-2 py-0.5 rounded-full" style={{backgroundColor:'rgba(31,111,92,0.1)'}}>
                  {d.namespace || 'fiscal'}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
