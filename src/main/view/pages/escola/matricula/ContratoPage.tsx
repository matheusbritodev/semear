import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ArrowLeft } from 'lucide-react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { TermoContrato } from '@/components/formalizacao/TermoContrato'
import { Button } from '@/components/ui/Button'
import { Select } from '@/components/ui/Select'
import { useContrato, useRegistrarAssinatura } from '@/hooks/useFormalizacao'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'

/** Termo na área da escola: imprimir e registrar quando o responsável assinou o papel na secretaria (RD03). */
export function ContratoPage() {
  const { id = '' } = useParams()
  const consulta = useContrato(id)
  const registrar = useRegistrarAssinatura(id)
  const podeRegistrar = usePermissao('MATRICULA', 'GRAVAR')
  const [responsavel, setResponsavel] = useState('')

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
  const termo = consulta.data

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        to={`/escola/matriculas/${termo.matriculaId}`}
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800 print:hidden"
      >
        <ArrowLeft className="size-4" aria-hidden /> Matrícula
      </Link>
      <TermoContrato
        termo={termo}
        rodape={
          termo.podeAssinar &&
          podeRegistrar && (
            <div className="space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm text-slate-600">
                O responsável assinou na secretaria? Registre quem assinou. Ele também pode assinar sozinho pelo Portal do Responsável.
              </p>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <div className="sm:w-72">
                  <Select rotulo="Quem assinou" value={responsavel} onChange={(e) => setResponsavel(e.target.value)}>
                    <option value="">Escolha…</option>
                    {termo.responsaveis.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.nome}
                        {r.parentesco ? ` (${r.parentesco})` : ''}
                        {r.financeiro ? ' · financeiro' : ''}
                      </option>
                    ))}
                  </Select>
                </div>
                <Button disabled={!responsavel} carregando={registrar.isPending} onClick={() => registrar.mutate(responsavel)}>
                  Registrar assinatura
                </Button>
              </div>
              {registrar.isError && (
                <p role="alert" className="text-sm text-red-600">
                  {registrar.error instanceof ApiError ? registrar.error.message : 'Não foi possível registrar.'}
                </p>
              )}
            </div>
          )
        }
      />
    </div>
  )
}
