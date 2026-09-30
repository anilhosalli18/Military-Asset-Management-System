/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        tactical: {
          950: '#070b12',
          900: '#0c1322',
          800: '#141f36',
          700: '#1d2e50',
          600: '#2b4273',
          500: '#3d5c9e',
          accent: '#10b981', // emerald
          amber: '#f59e0b',
          danger: '#ef4444',
          cyan: '#06b6d4',
        }
      },
      fontFamily: {
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
