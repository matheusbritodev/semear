import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

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
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold text-slate-900">Entrar</h1>
        <p className="text-sm text-slate-500">Use o e-mail cadastrado na escola.</p>
      </div>

      <form onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <Input rotulo="E-mail" type="email" autoComplete="email" erro={errors.email?.message} {...register('email')} />
        <Input
          rotulo="Senha"
          type="password"
          autoComplete="current-password"
          erro={errors.senha?.message}
          {...register('senha')}
        />
        {erro && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {erro}
          </p>
        )}
        <Button type="submit" className="w-full" carregando={isSubmitting}>
          Entrar
        </Button>
      </form>

      <div className="space-y-2 text-center text-sm">
        <Link to="/recuperar-senha" className="block text-slate-600 hover:text-semear-700 hover:underline">
          Esqueci minha senha
        </Link>
        <p className="text-slate-500">
          Família nova na escola?{' '}
          <Link to="/cadastro" className="font-medium text-semear-700 hover:underline">
            Criar conta
          </Link>
        </p>
      </div>
    </div>
  )
}
