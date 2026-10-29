import { env } from '@/config/env'
import { HttpClient } from '@/lib/HttpClient'
import { supabase } from '@/lib/supabase'

/** Instância única do cliente HTTP, com o token da sessão atual do Supabase. */
export const http = new HttpClient(env.VITE_API_URL, async () => {
  const { data } = await supabase.auth.getSession()
  return data.session?.access_token ?? null
})
