/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        blossom: {
          50: '#f4f7f1',
          100: '#e8eee2',
          500: '#5e7557',
          600: '#4d6247',
          700: '#394a35',
        },
        ink: '#242923',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
