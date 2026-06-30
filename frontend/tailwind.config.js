/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#10243A',
        paper: '#F6F7F4',
        surface: '#FFFFFF',
        accent: '#1F6F5C',
        accentHover: '#185847',
        amber: '#B8651B',
        muted: '#5C6B73',
        hairline: '#E2E6E1',
        danger: '#B23A3A',
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
