import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useNavigate } from 'react-router'
import { MailCheck } from 'lucide-react'
import { z } from 'zod'
import { esquemaSenha, mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

const esquema = z
  .object({
    email: z.email('Informe um e-mail válido'),
    senha: esquemaSenha,
    confirmacao: z.string(),
  })
  .refine((d) => d.senha === d.confirmacao, { message: 'As senhas não conferem', path: ['confirmacao'] })
type Formulario = z.infer<typeof esquema>

/** Conta do responsável no portal. Depois de entrar, ele completa o cadastro (nome, CPF, telefone). */
export function CadastroPage() {
  const { session, carregando, criarConta } = useAuth()
  const navegar = useNavigate()
  const [erro, setErro] = useState<string | null>(null)
  const [emailEnviadoPara, setEmailEnviadoPara] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Formulario>({ resolver: zodResolver(esquema) })

  if (!carregando && session) return <Navigate to="/portal" replace />

  async function enviar(dados: Formulario) {
    setErro(null)
    try {
      const entrou = await criarConta(dados.email, dados.senha)
      if (entrou) navegar('/portal', { replace: true })
      else setEmailEnviadoPara(dados.email)
    } catch (e) {
      setErro(mensagemAuth(e))
    }
  }

  if (emailEnviadoPara) {
    return (
      <div className="space-y-4 text-center">
        <MailCheck className="mx-auto size-10 text-semear-600" aria-hidden />
        <h1 className="text-2xl font-semibold text-slate-900">Confirme seu e-mail</h1>
        <p className="text-sm text-slate-600">
          Enviamos um link para <strong>{emailEnviadoPara}</strong>. Abra o e-mail e clique no link para ativar a conta; depois
          é só entrar.
        </p>
        <Link to="/login" className="inline-block text-sm font-medium text-semear-700 hover:underline">
          Ir para o login
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold text-slate-900">Criar conta</h1>
        <p className="text-sm text-slate-500">Para responsáveis fazerem a pré-matrícula e acompanharem os filhos.</p>
      </div>
      <form onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <Input rotulo="E-mail" type="email" autoComplete="email" erro={errors.email?.message} {...register('email')} />
        <Input rotulo="Senha" type="password" autoComplete="new-password" erro={errors.senha?.message} {...register('senha')} />
        <Input
          rotulo="Repita a senha"
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
          Criar conta
        </Button>
      </form>
      <p className="text-center text-sm text-slate-500">
        Já tem conta?{' '}
        <Link to="/login" className="font-medium text-semear-700 hover:underline">
          Entrar
        </Link>
      </p>
    </div>
  )
}
