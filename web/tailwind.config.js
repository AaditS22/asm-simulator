/** @type {import('tailwindcss').Config} */
export default {
    content: [
        './index.html',
        './src/**/*.{js,jsx,ts,tsx}',
    ],
    theme: {
        extend: {
            colors: {
                'bg-base':    '#2B2B2B',
                'bg-panel':   '#313335',
                'bg-raised':  '#3C3F41',
                'bg-hover':   '#4C5052',
                'bg-editor':  '#1E1F22',
                'border-soft':'#424547',
                'amber':      '#E8A845',
                'text-primary':'#BBBBBB',
                'text-bright': '#E8E8E8',
                'text-muted':  '#777777',
            },
            fontFamily: {
                sans: ["'Segoe UI'", "'Helvetica Neue'", 'Arial', 'sans-serif'],
                mono: ["'JetBrains Mono'", 'Consolas', "'Courier New'", 'monospace'],
            },
        },
    },
    plugins: [],
}