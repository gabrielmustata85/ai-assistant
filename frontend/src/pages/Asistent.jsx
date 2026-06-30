import React, { useState, useRef, useEffect } from 'react'
import { useCompany } from '../components/Layout.jsx'
import { useAuth } from '../contexts/AuthContext.jsx'
import { apiFetch, SESSION_ID } from '../lib/api.js'
import { useToast } from '../components/Toast.jsx'
import ClaudeResponse from '../components/ClaudeResponse.jsx'

export default function Asistent() {
  const { selectedCompany } = useCompany()
  const { token } = useAuth()
  const { addToast } = useToast()
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  async function handleSend(e) {
    e.preventDefault()
    if (!question.trim() || !selectedCompany) return
    const q = question.trim()
    setQuestion('')
    setMessages(prev => [...prev, { type: 'user', text: q }])
    setLoading(true)
    try {
      const res = await apiFetch('/advisor/ask', {
        method: 'POST',
        headers: { 'X-Session-ID': SESSION_ID },
        body: JSON.stringify({ companyId: selectedCompany.id, question: q }),
      }, token)
      setMessages(prev => [...prev, { type: 'assistant', data: res }])
    } catch (err) {
      addToast(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleReset() {
    try {
      await apiFetch('/advisor/reset', {
        method: 'POST',
        headers: { 'X-Session-ID': SESSION_ID },
      }, token)
      setMessages([])
      addToast('Conversație resetată.', 'success')
    } catch (err) {
      addToast(err.message)
    }
  }

  if (!selectedCompany) return (
    <div className="flex items-center justify-center h-full p-8">
      <div className="text-center max-w-sm">
        <div className="w-12 h-12 rounded-lg flex items-center justify-center mx-auto mb-4" style={{backgroundColor:'rgba(31,111,92,0.1)'}}>
          <span className="text-accent text-2xl">💬</span>
        </div>
        <h2 className="font-display font-semibold text-ink mb-2">Selectează o firmă</h2>
        <p className="text-sm text-muted">Asistentul fiscal are nevoie de contextul firmei tale.</p>
      </div>
    </div>
  )

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-6 py-4 border-b border-hairline bg-white">
        <div>
          <h1 className="font-display font-bold text-xl text-ink">Dario</h1>
          <p className="text-xs text-muted">asistentul tău fiscal · {selectedCompany.name}</p>
        </div>
        <button onClick={handleReset}
          className="text-xs text-muted hover:text-danger border border-hairline rounded-lg px-3 py-1.5 transition-colors">
          Resetează conversația
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
        {messages.length === 0 && (
          <div className="text-center py-12">
            <div className="w-12 h-12 rounded-lg flex items-center justify-center mx-auto mb-3" style={{backgroundColor:'rgba(31,111,92,0.1)'}}>
              <span className="text-accent text-2xl">💬</span>
            </div>
            <p className="text-sm text-muted mb-4">Sunt Dario. Întreabă-mă orice despre taxele și obligațiile firmei tale.</p>
            <div className="flex flex-wrap gap-2 justify-center">
              {['Ce taxe am de plătit luna aceasta?', 'Cum reduc impozitul pe profit?', 'Sunt înregistrat corect pentru TVA?'].map(q => (
                <button key={q} onClick={() => setQuestion(q)}
                  className="text-xs border border-hairline rounded-full px-3 py-1.5 text-ink hover:border-accent hover:text-accent transition-colors">
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'}`}>
            {msg.type === 'user' ? (
              <div className="bg-ink text-white text-sm rounded-lg px-4 py-2.5 max-w-md">
                {msg.text}
              </div>
            ) : (
              <div className="max-w-2xl w-full">
                <ClaudeResponse data={msg.data} />
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-white border border-hairline rounded-lg px-4 py-3 text-sm text-muted">
              Asistentul analizează...
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="px-6 py-4 border-t border-hairline bg-white">
        <form onSubmit={handleSend} className="flex gap-3">
          <input
            type="text"
            value={question}
            onChange={e => setQuestion(e.target.value)}
            placeholder="Scrie întrebarea ta fiscală..."
            disabled={loading}
            className="flex-1 border border-hairline rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-accent disabled:opacity-60"
          />
          <button type="submit" disabled={loading || !question.trim()}
            className="bg-accent hover:bg-accentHover text-white shadow-[0_2px_8px_rgba(16,145,110,0.25)] font-medium px-5 py-2.5 rounded-lg text-sm transition-colors disabled:opacity-60">
            Trimite
          </button>
        </form>
      </div>
    </div>
  )
}
