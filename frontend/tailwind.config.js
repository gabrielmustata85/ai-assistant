/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#0B1B2E',        // near-black navy — chrome & text
        inkSoft: '#13263D',    // panels on dark
        paper: '#EBEEF2',      // cool grey workspace (contrast vs. white cards)
        surface: '#FFFFFF',
        accent: '#10916E',     // vivid emerald
        accentHover: '#0C7257',
        amber: '#B5611A',
        muted: '#586675',
        onDark: '#E6ECF2',     // primary text on ink
        onDarkMuted: '#8597A8',// secondary text on ink
        hairline: '#D5DBE2',   // crisp line
        danger: '#C73A2B',
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        sans: ['"IBM Plex Sans"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
    },
  },
  plugins: [],
}
