import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router'
import { z } from 'zod'
import { esquemaSenha, mensagemAuth } from '@/auth/mensagensAuth'
import { useAuth } from '@/auth/useAuth'
import { TelaCarregando } from '@/components/comuns/TelaCarregando'
import { Button } from '@/components/ui/Button'
import { AlertaFormulario, CabecalhoPublico, emCascata } from '@/components/publico/Formulario'
import { InputSenha } from '@/components/ui/InputSenha'

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
        <CabecalhoPublico titulo="Link inválido ou expirado" descricao="Peça um novo link de recuperação." />
        <Link to="/recuperar-senha" className="inline-block text-sm font-medium text-semear-700 hover:underline dark:text-semear-400">
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
    <div className="space-y-7">
      <CabecalhoPublico titulo="Nova senha" descricao="Escolha uma senha que só você saiba." />
      <form onSubmit={handleSubmit(enviar)} className="space-y-4 motion-safe:animate-surgir" style={emCascata(1)} noValidate>
        <InputSenha rotulo="Nova senha" autoComplete="new-password" erro={errors.senha?.message} {...register('senha')} />
        <InputSenha
          rotulo="Repita a nova senha"
          autoComplete="new-password"
          erro={errors.confirmacao?.message}
          {...register('confirmacao')}
        />
        {erro && (
          <AlertaFormulario>{erro}</AlertaFormulario>
        )}
        <Button type="submit" className="w-full" carregando={isSubmitting}>
          Salvar nova senha
        </Button>
      </form>
    </div>
  )
}
