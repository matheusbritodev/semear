import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { ArrowRight, Mail, UserPlus } from 'lucide-react'
import { z } from 'zod'
import { mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { AlertaFormulario, CabecalhoPublico, emCascata } from '@/components/publico/Formulario'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { InputSenha } from '@/components/ui/InputSenha'

const esquema = z.object({
  email: z.email('Informe um e-mail válido'),
  senha: z.string().min(1, 'Informe a senha'),
})
type Formulario = z.infer<typeof esquema>

export function LoginPage() {
  const { session, carregando, entrar } = useAuth()
  const navegar = useNavigate()
  const local = useLocation()
  const [erro, setErro] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Formulario>({ resolver: zodResolver(esquema) })

  const destino = (local.state as { de?: string } | null)?.de ?? '/'
  if (!carregando && session) return <Navigate to={destino} replace />

  async function enviar(dados: Formulario) {
    setErro(null)
    try {
      await entrar(dados.email, dados.senha)
      navegar(destino, { replace: true })
    } catch (e) {
      setErro(mensagemAuth(e))
    }
  }

  return (
    <div className="space-y-7">
      <CabecalhoPublico titulo="Bem-vindo de volta" descricao="Entre com o e-mail cadastrado na escola." />

      <form onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <div className="motion-safe:animate-surgir" style={emCascata(1)}>
          <Input
            rotulo="E-mail"
            type="email"
            icone={Mail}
            placeholder="seu.email@exemplo.com"
            autoComplete="email"
            className="py-2.5!"
            erro={errors.email?.message}
            {...register('email')}
          />
        </div>
        <div className="space-y-2 motion-safe:animate-surgir" style={emCascata(2)}>
          <InputSenha
            rotulo="Senha"
            placeholder="Sua senha"
            autoComplete="current-password"
            className="py-2.5!"
            erro={errors.senha?.message}
            {...register('senha')}
          />
          <div className="text-right">
            <Link
              to="/recuperar-senha"
              className="text-sm font-medium text-semear-700 hover:text-semear-800 hover:underline dark:text-semear-400 dark:hover:text-semear-300"
            >
              Esqueci minha senha
            </Link>
          </div>
        </div>

        {erro && <AlertaFormulario>{erro}</AlertaFormulario>}

        <div className="motion-safe:animate-surgir" style={emCascata(3)}>
          <Button type="submit" className="group w-full py-2.5!" carregando={isSubmitting}>
            Entrar
            {!isSubmitting && <ArrowRight className="size-4 transition-transform group-hover:translate-x-1" aria-hidden />}
          </Button>
        </div>
      </form>

      <div className="space-y-4 motion-safe:animate-surgir" style={emCascata(4)}>
        <div className="flex items-center gap-3 text-xs text-slate-400 uppercase dark:text-slate-500">
          <span className="h-px flex-1 bg-slate-200 dark:bg-slate-800" />
          Família nova na escola?
          <span className="h-px flex-1 bg-slate-200 dark:bg-slate-800" />
        </div>
        <Link
          to="/cadastro"
          className="flex w-full items-center justify-center gap-2 rounded-lg border border-semear-200 bg-semear-50/60 px-4 py-2.5 text-sm font-medium text-semear-700 transition-colors hover:border-semear-300 hover:bg-semear-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-semear-500 dark:border-semear-800 dark:bg-semear-950/40 dark:text-semear-300 dark:hover:border-semear-700 dark:hover:bg-semear-900/50"
        >
          <UserPlus className="size-4" aria-hidden /> Criar conta de responsável
        </Link>
      </div>
    </div>
  )
}
