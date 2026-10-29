import { z } from 'zod'

// Variáveis do .env da raiz. Só as que começam com VITE_ chegam ao navegador:
// nunca coloque segredo (chave secreta, senha do banco) em variável VITE_.
const esquema = z.object({
  VITE_SUPABASE_URL: z.url(),
  VITE_SUPABASE_PUBLISHABLE_KEY: z.string().min(20),
  VITE_API_URL: z.string().min(1).default('/api'),
})

const resultado = esquema.safeParse(import.meta.env)

if (!resultado.success) {
  const campos = resultado.error.issues.map((problema) => problema.path.join('.')).join(', ')
  throw new Error(`Configuração da View inválida. Confira no .env da raiz: ${campos}`)
}

export const env = resultado.data
