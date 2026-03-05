/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'risk-high': '#EF4444', // red-500
        'risk-medium-high': '#F97316', // orange-500
        'risk-medium': '#EAB308', // yellow-500
        'risk-low': '#22C55E', // green-500
      }
    },
  },
  plugins: [],
}
