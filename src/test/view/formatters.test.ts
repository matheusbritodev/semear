import { describe, expect, it } from 'vitest'
import { formatarCpf, formatarData, formatarMoeda, formatarTelefone, somenteDigitos } from '@/lib/formatters'
import { pode } from '@/auth/permissions'
import type { Sessao } from '@/models/sessao'

describe('formatadores', () => {
  it('formata CPF e extrai dígitos', () => {
    expect(formatarCpf('00000000191')).toBe('000.000.001-91')
    expect(somenteDigitos('000.000.001-91')).toBe('00000000191')
  })

  it('formata moeda em reais', () => {
    expect(formatarMoeda('375.00')).toBe('R$ 375,00')
    expect(formatarMoeda(1234.5)).toBe('R$ 1.234,50')
  })

  it('formata data sem mexer no fuso', () => {
    expect(formatarData('2027-02-10')).toBe('10/02/2027')
  })

  it('formata telefone fixo e celular', () => {
    expect(formatarTelefone('98999991234')).toBe('(98) 99999-1234')
    expect(formatarTelefone('9833334444')).toBe('(98) 3333-4444')
  })
})

describe('permissões na View', () => {
  const sessao: Sessao = {
    id: '1',
    email: 'secretaria@teste.exemplo',
    nome: 'Secretaria',
    perfil: 'SECRETARIA',
    cadastroCompleto: true,
    permissoes: [
      { modulo: 'MATRICULA', ler: true, gravar: true },
      { modulo: 'PEDAGOGICO', ler: true, gravar: false },
    ],
  }

  it('segue a matriz do perfil', () => {
    expect(pode(sessao, 'MATRICULA', 'GRAVAR')).toBe(true)
    expect(pode(sessao, 'PEDAGOGICO', 'GRAVAR')).toBe(false)
    expect(pode(sessao, 'FINANCEIRO', 'LER')).toBe(false)
    expect(pode(null, 'MATRICULA', 'LER')).toBe(false)
  })
})
