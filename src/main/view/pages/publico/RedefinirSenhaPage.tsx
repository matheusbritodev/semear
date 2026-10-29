import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router'
import { z } from 'zod'
import { esquemaSenha, mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { TelaCarregando } from '@/components/comuns/TelaCarregando'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

const esquema = z
  .object({ senha: esquemaSenha, confirmacao: z.string() })
  .refine((d) => d.senha === d.confirmacao, { message: 'As senhas não conferem', path: ['confirmacao'] })
type Formulario = z.infer<typeof esquema>

/**
 * Nova senha: destino do link de recuperação (sessão temporária do Supabase) e também o "Trocar senha"
 * de quem já está logado — por exemplo, para trocar a senha provisória dada pela escola.
 */
export function RedefinirSenhaPage() {
  const { session, carregando, redefinirSenha } = useAuth()
  const navegar = useNavigate()
  const [erro, setErro] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Formulario>({ resolver: zodResolver(esquema) })

  if (carregando) return <TelaCarregando />

  if (!session) {
    return (
      <div className="space-y-4 text-center">
        <h1 className="text-2xl font-semibold text-slate-900">Link inválido ou expirado</h1>
        <p className="text-sm text-slate-600">Peça um novo link de recuperação.</p>
        <Link to="/recuperar-senha" className="inline-block text-sm font-medium text-semear-700 hover:underline">
          Recuperar senha
        </Link>
      </div>
    )
  }

  async function enviar(dados: Formulario) {
    setErro(null)
    try {
      await redefinirSenha(dados.senha)
      navegar('/', { replace: true })
    } catch (e) {
      setErro(mensagemAuth(e))
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Nova senha</h1>
      <form onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <Input rotulo="Nova senha" type="password" autoComplete="new-password" erro={errors.senha?.message} {...register('senha')} />
        <Input
          rotulo="Repita a nova senha"
          type="password"
          autoComplete="new-password"
          erro={errors.confirmacao?.message}
          {...register('confirmacao')}
        />
        {erro && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {erro}
          </p>
        )}
        <Button type="submit" className="w-full" carregando={isSubmitting}>
          Salvar nova senha
        </Button>
      </form>
    </div>
  )
}
