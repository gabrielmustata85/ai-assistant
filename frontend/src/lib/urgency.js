// Sursa unică pentru logica de urgență a scadențelor (culori, praguri, etichete).
// Folosită de UrgencyBadge, de scadențarul din Dashboard și de orice altă pagină cu termene.

export const URGENCY_COLORS = {
  danger: '#C2410C', // ≤ 7 zile sau depășit
  amber: '#B8651B',  // 8–14 zile
  calm: '#1F6F5C',   // > 14 zile (verde accent)
}

// Extrage o dată (YYYY-MM-DD sau DD.MM.YYYY / DD/MM/YYYY) dintr-un text de scadență.
export function parseDeadline(scadenta) {
  if (!scadenta) return null
  let m = String(scadenta).match(/(\d{4})-(\d{2})-(\d{2})/)
  if (m) return new Date(+m[1], +m[2] - 1, +m[3])
  m = String(scadenta).match(/(\d{1,2})[.\/](\d{1,2})[.\/](\d{4})/)
  if (m) return new Date(+m[3], +m[2] - 1, +m[1])
  return null
}

// Câte zile până la scadență, raportat la azi (00:00). Negativ = depășit.
export function daysUntil(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return Math.round((d - today) / 86400000)
}

// Nivelul de urgență pentru un număr de zile (sau null).
export function urgencyLevel(days) {
  if (days == null) return null
  if (days < 0 || days <= 7) return 'danger'
  if (days <= 14) return 'amber'
  return 'calm'
}

// Culoarea asociată (folosită și pentru markerii din scadențar).
export function urgencyColor(days) {
  const level = urgencyLevel(days)
  return level ? URGENCY_COLORS[level] : URGENCY_COLORS.calm
}

// Eticheta text. Întoarce null peste 14 zile (atunci nu se afișează badge).
export function urgencyLabel(days) {
  if (days == null) return null
  if (days < 0) return `depășit cu ${Math.abs(days)} zile`
  if (days === 0) return 'scade azi'
  if (days === 1) return 'scade mâine'
  if (days <= 7) return `scade în ${days} zile`
  if (days <= 14) return `în ${days} zile`
  return null
}

// Helper combinat pornind de la un text de scadență.
export function urgencyFromText(scadenta) {
  const date = parseDeadline(scadenta)
  if (!date) return { date: null, days: null, level: null, label: null }
  const days = daysUntil(date)
  return { date, days, level: urgencyLevel(days), label: urgencyLabel(days) }
}
