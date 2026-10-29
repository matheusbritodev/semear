import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router'
import { z } from 'zod'
import { mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

const esquema = z.object({ email: z.email('Informe um e-mail válido') })

export function RecuperarSenhaPage() {
  const { recuperarSenha } = useAuth()
  const [enviado, setEnviado] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<{ email: string }>({ resolver: zodResolver(esquema) })

  async function enviar({ email }: { email: string }) {
    setErro(null)
    try {
      await recuperarSenha(email)
      setEnviado(true)
    } catch (e) {
      setErro(mensagemAuth(e))
    }
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold text-slate-900">Recuperar senha</h1>
        <p className="text-sm text-slate-500">Enviaremos um link para você criar uma nova senha.</p>
      </div>
      {enviado ? (
        <p role="status" className="rounded-lg bg-semear-50 px-3 py-3 text-sm text-semear-800">
          Se houver uma conta com este e-mail, o link chegará em alguns minutos. Confira também a caixa de spam.
        </p>
      ) : (
        <form onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
          <Input rotulo="E-mail" type="email" autoComplete="email" erro={errors.email?.message} {...register('email')} />
          {erro && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {erro}
            </p>
          )}
          <Button type="submit" className="w-full" carregando={isSubmitting}>
            Enviar link
          </Button>
        </form>
      )}
      <Link to="/login" className="block text-center text-sm text-slate-600 hover:text-semear-700 hover:underline">
        Voltar ao login
      </Link>
    </div>
  )
}
