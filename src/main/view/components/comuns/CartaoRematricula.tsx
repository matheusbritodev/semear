import { useState } from 'react'
import { CalendarCheck } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { ApiError } from '@/lib/ApiError'
import type { RematriculaOpcao } from '@/models/matricula'
import { CamposFarda } from './CamposFarda'
import { StatusMatriculaBadge } from './StatusMatriculaBadge'

interface Props {
  opcao: RematriculaOpcao
  podeRematricular: boolean
  /** Executa a rematrícula; o componente cuida do formulário de fardas e da confirmação. */
  rematricular: (fardaUniforme: string, fardaEdFisica: string) => Promise<unknown>
  emAndamento: boolean
  titulo?: string
}

/** Situação de rematrícula de um aluno (RF01.5) — usado no portal e na ficha do aluno. */
export function CartaoRematricula({ opcao, podeRematricular, rematricular, emAndamento, titulo }: Props) {
  const [aberto, setAberto] = useState(false)
  const [uniforme, setUniforme] = useState('')
  const [edFisica, setEdFisica] = useState('')
  const [erro, setErro] = useState<ApiError | null>(null)

  async function confirmar() {
    setErro(null)
    try {
      await rematricular(uniforme, edFisica)
      setAberto(false)
    } catch (e) {
      setErro(e instanceof ApiError ? e : ApiError.rede())
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1">
          <p className="font-semibold text-slate-900">{titulo ?? `Rematrícula ${opcao.anoDestino}`}</p>
          {opcao.turmaAtual && <p className="text-sm text-slate-500">Hoje: {opcao.turmaAtual}</p>}
          <p className="text-sm text-slate-700">
            {opcao.permitida && opcao.turmaDestino ? (
              <>
                Vaga em <strong>{opcao.turmaDestino}</strong> ({opcao.vagasDestino} {opcao.vagasDestino === 1 ? 'vaga' : 'vagas'})
              </>
            ) : (
              opcao.mensagem
            )}
          </p>
        </div>
        {opcao.statusDestino ? (
          <StatusMatriculaBadge status={opcao.statusDestino} />
        ) : opcao.permitida ? (
          <Badge cor="verde">Disponível</Badge>
        ) : (
          <Badge cor={opcao.situacao === 'PENDENCIA_FINANCEIRA' ? 'vermelho' : 'cinza'}>Indisponível</Badge>
        )}
      </div>
      {opcao.permitida && podeRematricular && (
        <Button className="mt-4" onClick={() => setAberto(true)}>
          <CalendarCheck className="size-4" aria-hidden /> Rematricular para {opcao.anoDestino}
        </Button>
      )}

      <Modal
        aberto={aberto}
        titulo={`Rematrícula ${opcao.anoDestino}`}
        aoFechar={() => setAberto(false)}
        rodape={
          <>
            <Button variante="secundario" onClick={() => setAberto(false)}>
              Cancelar
            </Button>
            <Button carregando={emAndamento} onClick={() => void confirmar()}>
              Confirmar rematrícula
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            {opcao.aluno} será rematriculado(a) em <strong>{opcao.turmaDestino}</strong>. Depois, é gerada a cobrança da
            matrícula; a vaga é confirmada com o pagamento.
          </p>
          <CamposFarda
            uniforme={uniforme}
            edFisica={edFisica}
            aoMudar={(campo, valor) => (campo === 'uniforme' ? setUniforme(valor) : setEdFisica(valor))}
          />
          {erro && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {erro.mensagemAmigavel}
            </p>
          )}
        </div>
      </Modal>
    </Card>
  )
}
