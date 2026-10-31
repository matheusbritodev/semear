import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useNavigate } from 'react-router'
import { Mail, MailCheck } from '@/components/ui/icones'
import { z } from 'zod'
import { esquemaSenha, mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { AlertaFormulario, CabecalhoPublico, emCascata } from '@/components/publico/Formulario'
import { Input } from '@/components/ui/Input'
import { InputSenha } from '@/components/ui/InputSenha'

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
        <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-semear-50 text-semear-600 motion-safe:animate-surgir dark:bg-semear-950/60 dark:text-semear-400">
          <MailCheck className="size-7" aria-hidden />
        </span>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Confirme seu e-mail</h1>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Enviamos um link para <strong>{emailEnviadoPara}</strong>. Abra o e-mail e clique no link para ativar a conta; depois
          é só entrar.
        </p>
        <Link to="/login" className="inline-block text-sm font-medium text-semear-700 hover:underline dark:text-semear-400">
          Ir para o login
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-7">
      <CabecalhoPublico titulo="Criar conta" descricao="Para responsáveis fazerem a pré-matrícula e acompanharem os filhos." />
      <form onSubmit={handleSubmit(enviar)} className="space-y-4 motion-safe:animate-surgir" style={emCascata(1)} noValidate>
        <Input rotulo="E-mail" type="email" icone={Mail} autoComplete="email" erro={errors.email?.message} {...register('email')} />
        <InputSenha rotulo="Senha" autoComplete="new-password" erro={errors.senha?.message} {...register('senha')} />
        <InputSenha
          rotulo="Repita a senha"
          autoComplete="new-password"
          erro={errors.confirmacao?.message}
          {...register('confirmacao')}
        />
        {erro && (
          <AlertaFormulario>{erro}</AlertaFormulario>
        )}
        <Button type="submit" className="w-full" carregando={isSubmitting}>
          Criar conta
        </Button>
      </form>
      <p className="text-center text-sm text-slate-500 motion-safe:animate-surgir dark:text-slate-400" style={emCascata(2)}>
        Já tem conta?{' '}
        <Link to="/login" className="font-medium text-semear-700 hover:underline dark:text-semear-400">
          Entrar
        </Link>
      </p>
    </div>
  )
}
