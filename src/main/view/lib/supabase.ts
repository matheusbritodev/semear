import { createClient } from '@supabase/supabase-js'
import { env } from '@/config/env'

// A View usa o Supabase apenas para autenticação (login, sessão, senha).
// Todo dado de negócio vem dos Controllers Java via HttpClient.
export const supabase = createClient(env.VITE_SUPABASE_URL, env.VITE_SUPABASE_PUBLISHABLE_KEY, {
  auth: { persistSession: true, autoRefreshToken: true },
})
