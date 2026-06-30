import React from 'react'
import { urgencyFromText, URGENCY_COLORS } from '../lib/urgency.js'

/**
 * Etichetă de urgență consistentă pentru orice scadență.
 * `date` = textul scadenței (YYYY-MM-DD sau DD.MM.YYYY). Nu afișează nimic peste 14 zile sau dacă data nu e parsabilă.
 */
export default function UrgencyBadge({ date }) {
  const { level, label } = urgencyFromText(date)
  if (!label) return null

  const style = level === 'danger'
    ? { backgroundColor: URGENCY_COLORS.danger, color: '#fff' }
    : { backgroundColor: 'rgba(184,101,27,0.12)', color: URGENCY_COLORS.amber }

  return (
    <span className="text-xs font-medium px-2 py-0.5 rounded-full whitespace-nowrap" style={style}>
      {label}
    </span>
  )
}
