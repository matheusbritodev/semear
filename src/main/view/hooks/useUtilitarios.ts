import { useEffect, useState } from 'react'
import { useAuth } from '@/auth/useAuth'
import { pode } from '@/auth/permissions'
import type { Acao, Modulo } from '@/models/enums'

/** Atrasa a atualização de um valor (ex.: busca enquanto o usuário digita). */
export function useDebounce<T>(valor: T, atrasoMs = 300): T {
  const [atrasado, setAtrasado] = useState(valor)
  useEffect(() => {
    const temporizador = setTimeout(() => setAtrasado(valor), atrasoMs)
    return () => clearTimeout(temporizador)
  }, [valor, atrasoMs])
  return atrasado
}

/** Só para esconder botões; quem garante a permissão é o Controller. */
export function usePermissao(modulo: Modulo, acao: Acao): boolean {
  const { sessao } = useAuth()
  return pode(sessao, modulo, acao)
}
