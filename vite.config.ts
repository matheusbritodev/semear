import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const raiz = (caminho: string) => fileURLToPath(new URL(caminho, import.meta.url))

// View do MVC: código em src/main/view, compilado para dentro da aplicação Java
// (target/classes/static), que a serve no mesmo endereço da API.
export default defineConfig({
  root: raiz('./src/main/view'),
  envDir: raiz('./'), // .env único na raiz; só variáveis VITE_ chegam ao navegador
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': raiz('./src/main/view') },
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  build: {
    outDir: raiz('./target/classes/static'),
    emptyOutDir: true,
  },
  test: {
    root: raiz('./'),
    include: ['src/test/view/**/*.test.ts'],
    environment: 'node',
  },
})
