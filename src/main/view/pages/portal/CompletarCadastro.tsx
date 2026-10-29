import { useState, type FormEvent } from 'react'
import { ClipboardList } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { ApiError } from '@/lib/ApiError'
import { formatarCpf, formatarTelefone } from '@/lib/formatters'
import { portalService } from '@/services/PortalService'

/** Primeiro acesso do responsável: liga o login a um cadastro (novo ou já existente pelo CPF). */
export function CompletarCadastro() {
  const { sessao, recarregar } = useAuth()
  const [nome, setNome] = useState('')
  const [cpf, setCpf] = useState('')
  const [telefone, setTelefone] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<ApiError | null>(null)
  const erroCampo = (campo: string) => erro?.campos.find((c) => c.campo === campo)?.mensagem

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await portalService.completarCadastro(nome, cpf, telefone)
      await recarregar()
    } catch (e) {
      setErro(e instanceof ApiError ? e : ApiError.rede())
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Card className="mx-auto max-w-lg">
      <div className="mb-5 text-center">
        <ClipboardList className="mx-auto mb-3 size-10 text-semear-600" aria-hidden />
        <h1 className="text-xl font-semibold text-slate-900">Complete seu cadastro</h1>
        <p className="mt-1 text-sm text-slate-600">
          Você entrou como <strong>{sessao?.email}</strong>. Precisamos de alguns dados para liberar o portal.
        </p>
      </div>
      <form onSubmit={(e) => void enviar(e)} className="space-y-4" noValidate>
        <Input rotulo="Seu nome completo" value={nome} onChange={(e) => setNome(e.target.value)} erro={erroCampo('nome')} />
        <Input
          rotulo="Seu CPF"
          inputMode="numeric"
          value={cpf}
          onChange={(e) => setCpf(formatarCpf(e.target.value))}
          placeholder="000.000.000-00"
          erro={erroCampo('cpf')}
        />
        <Input
          rotulo="Telefone (WhatsApp)"
          inputMode="tel"
          value={telefone}
          onChange={(e) => setTelefone(formatarTelefone(e.target.value))}
          placeholder="(98) 99999-9999"
          erro={erroCampo('telefone')}
        />
        <p className="text-xs text-slate-500">
          Se você já é responsável por um aluno da escola, use o mesmo CPF informado à secretaria: seus filhos aparecerão aqui.
        </p>
        {erro && erro.campos.length === 0 && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {erro.mensagemAmigavel}
          </p>
        )}
        <Button type="submit" className="w-full" carregando={enviando}>
          Concluir cadastro
        </Button>
      </form>
    </Card>
  )
}
