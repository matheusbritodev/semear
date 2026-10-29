import { z } from 'zod'
import { ApiError } from '@/lib/ApiError'

/** Senha forte o bastante para dados de crianças (o Supabase exige no mínimo 6). */
export const esquemaSenha = z
  .string()
  .min(8, 'Use pelo menos 8 caracteres')
  .regex(/[A-Za-z]/, 'Inclua ao menos uma letra')
  .regex(/\d/, 'Inclua ao menos um número')

/** Traduz as mensagens do Supabase Auth para o usuário. */
export function mensagemAuth(erro: unknown): string {
  // Erros da aplicação Java já vêm com mensagem pronta para o usuário.
  if (erro instanceof ApiError) return erro.message
  const texto = erro instanceof Error ? erro.message : ''
  if (texto.includes('Invalid login credentials')) return 'E-mail ou senha incorretos.'
  if (texto.includes('Email not confirmed')) return 'Confirme seu e-mail pelo link enviado antes de entrar.'
  if (texto.includes('User already registered')) return 'Este e-mail já tem conta. Entre ou recupere a senha.'
  if (texto.toLowerCase().includes('rate limit') || texto.includes('security purposes'))
    return 'Muitas tentativas em pouco tempo. Aguarde alguns minutos e tente de novo.'
  if (texto.includes('Password should')) return 'A senha não atende aos requisitos mínimos.'
  if (texto.includes('different from the old')) return 'A nova senha precisa ser diferente da atual.'
  if (texto.toLowerCase().includes('fetch')) return 'Não foi possível conectar. Verifique sua internet.'
  return 'Não foi possível concluir. Tente novamente.'
}
