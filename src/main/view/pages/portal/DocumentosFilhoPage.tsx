import { useParams } from 'react-router'
import { ChecklistDocumentos } from '@/components/comuns/ChecklistDocumentos'
import { PageHeader } from '@/components/comuns/PageHeader'
import { useFilhos } from '@/hooks/usePortal'

/** RF10 + RN12: a família envia os documentos da matrícula pelo celular. */
export function DocumentosFilhoPage() {
  const { id = '' } = useParams()
  const filhos = useFilhos()
  const filho = filhos.data?.find((f) => f.alunoId === id)

  return (
    <div>
      <PageHeader
        titulo="Documentos da matrícula"
        descricao={filho ? `${filho.nome} · ${filho.turmaAtual ?? ''}` : undefined}
        voltarPara={{ rota: '/portal', rotulo: 'Início' }}
      />
      <p className="mb-4 text-sm text-slate-600">
        Fotografe ou digitalize cada documento e envie por aqui. A matrícula só é efetivada com os documentos entregues e a
        primeira parcela paga.
      </p>
      <ChecklistDocumentos alunoId={id} area="portal" podeEnviar />
    </div>
  )
}
