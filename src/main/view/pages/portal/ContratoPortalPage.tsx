import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ArrowLeft } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { TermoContrato } from '@/components/formalizacao/TermoContrato'
import { Button } from '@/components/ui/Button'
import { useAssinarContrato, useContratoPortal } from '@/hooks/useFormalizacao'
import { ApiError } from '@/lib/ApiError'

/** O responsável lê o termo e assina com aceite eletrônico (RD03: só responsável vinculado ao aluno). */
export function ContratoPortalPage() {
  const { id = '' } = useParams()
  const consulta = useContratoPortal(id)
  const assinar = useAssinarContrato()
  const [aceite, setAceite] = useState(false)

  if (consulta.isPending) return <Carregando />
  if (consulta.isError) return <EstadoErro erro={consulta.error} />
  const termo = consulta.data

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        to={`/portal/alunos/${termo.alunoId}`}
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800 print:hidden"
      >
        <ArrowLeft className="size-4" aria-hidden /> {termo.aluno}
      </Link>
      <TermoContrato
        termo={termo}
        rodape={
          termo.podeAssinar && (
            <div className="space-y-3 rounded-lg border border-semear-200 bg-semear-50 p-4">
              <label className="flex items-start gap-2 text-sm text-slate-800">
                <input
                  type="checkbox"
                  checked={aceite}
                  onChange={(e) => setAceite(e.target.checked)}
                  className="mt-0.5 size-4 shrink-0 accent-semear-600"
                />
                Li e concordo com o {termo.titulo.toLowerCase()} de {termo.aluno} para {termo.ano}.
              </label>
              <Button disabled={!aceite} carregando={assinar.isPending} onClick={() => assinar.mutate(termo.id)}>
                Assinar eletronicamente
              </Button>
              <p className="text-xs text-slate-500">A assinatura fica registrada com o seu nome, a data e a hora.</p>
              {assinar.isError && (
                <p role="alert" className="text-sm text-red-600">
                  {assinar.error instanceof ApiError ? assinar.error.message : 'Não foi possível assinar.'}
                </p>
              )}
            </div>
          )
        }
      />
    </div>
  )
}
