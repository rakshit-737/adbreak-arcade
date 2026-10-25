import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  base: '/adbreak-arcade/',
  plugins: [react()],
})
