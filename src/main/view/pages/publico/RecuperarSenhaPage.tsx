import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router'
import { ArrowLeft, Mail, MailCheck } from '@/components/ui/icones'
import { z } from 'zod'
import { mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { AlertaFormulario, CabecalhoPublico, emCascata } from '@/components/publico/Formulario'
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
    <div className="space-y-7">
      <CabecalhoPublico titulo="Recuperar senha" descricao="Enviaremos um link para você criar uma nova senha." />
      {enviado ? (
        <p
          role="status"
          className="flex items-start gap-2 rounded-xl border border-semear-200 bg-semear-50 px-3 py-3 text-sm text-semear-800 motion-safe:animate-surgir dark:border-semear-800 dark:bg-semear-950/50 dark:text-semear-200"
        >
          <MailCheck className="mt-0.5 size-4 shrink-0" aria-hidden />
          Se houver uma conta com este e-mail, o link chegará em alguns minutos. Confira também a caixa de spam.
        </p>
      ) : (
        <form onSubmit={handleSubmit(enviar)} className="space-y-4 motion-safe:animate-surgir" style={emCascata(1)} noValidate>
          <Input rotulo="E-mail" type="email" icone={Mail} autoComplete="email" erro={errors.email?.message} {...register('email')} />
          {erro && (
            <AlertaFormulario>{erro}</AlertaFormulario>
          )}
          <Button type="submit" className="w-full" carregando={isSubmitting}>
            Enviar link
          </Button>
        </form>
      )}
      <Link
        to="/login"
        className="mx-auto flex w-fit items-center gap-1.5 text-sm text-slate-600 hover:text-semear-700 dark:text-slate-400 dark:hover:text-semear-400"
      >
        <ArrowLeft className="size-4" aria-hidden /> Voltar ao login
      </Link>
    </div>
  )
}
