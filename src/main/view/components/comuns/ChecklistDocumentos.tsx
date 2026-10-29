import { useRef, useState } from 'react'
import { CheckCircle2, Circle, Eye, FileText, Trash2, Upload } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { useDocumentos } from '@/hooks/useDocumentos'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import { FORMATOS_ACEITOS, TAMANHO_MAXIMO_ARQUIVO, type TipoDocumentoAluno } from '@/models/documento'
import { Carregando, EstadoErro } from './Estados'

interface Props {
  alunoId: string
  area: 'escola' | 'portal'
  podeEnviar: boolean
  podeApagar?: boolean
}

/** Os 7 documentos da matrícula (RN12): o que já foi entregue, o que falta e o envio de arquivos. */
export function ChecklistDocumentos({ alunoId, area, podeEnviar, podeApagar = false }: Props) {
  const { checklist, enviar, remover, abrir } = useDocumentos(alunoId, area)
  const seletor = useRef<HTMLInputElement>(null)
  const [tipoEscolhido, setTipoEscolhido] = useState<TipoDocumentoAluno | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  if (checklist.isPending) return <Carregando texto="Carregando documentos…" />
  if (checklist.isError) return <EstadoErro erro={checklist.error} aoTentarDeNovo={() => void checklist.refetch()} />

  function escolherArquivo(tipo: TipoDocumentoAluno) {
    setAviso(null)
    setTipoEscolhido(tipo)
    seletor.current?.click()
  }

  function aoSelecionar(arquivo: File | undefined) {
    if (!arquivo || !tipoEscolhido) return
    if (arquivo.size > TAMANHO_MAXIMO_ARQUIVO) {
      setAviso('O arquivo passa de 5 MB. Tire uma foto com menos resolução ou envie um PDF menor.')
      return
    }
    enviar.mutate(
      { tipo: tipoEscolhido, arquivo },
      { onError: (e) => setAviso(e instanceof ApiError ? (e.campos[0]?.mensagem ?? e.mensagemAmigavel) : 'Falha no envio.') },
    )
  }

  const { completo, pendentes, itens } = checklist.data

  return (
    <div className="space-y-3">
      <p className="text-sm">
        {completo ? (
          <Badge cor="verde">Documentação completa</Badge>
        ) : (
          <Badge cor="amarelo">
            {pendentes} {pendentes === 1 ? 'documento pendente' : 'documentos pendentes'}
          </Badge>
        )}
      </p>
      {aviso && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {aviso}
        </p>
      )}
      <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
        {itens.map((item) => (
          <li key={item.tipo} className="flex flex-wrap items-start justify-between gap-3 px-4 py-3">
            <div className="flex min-w-0 flex-1 items-start gap-2">
              {item.enviado ? (
                <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-semear-600" aria-label="Entregue" />
              ) : (
                <Circle className={`mt-0.5 size-4 shrink-0 ${item.obrigatorio ? 'text-amber-500' : 'text-slate-300'}`} aria-label="Pendente" />
              )}
              <div className="min-w-0">
                <p className="text-sm font-medium text-slate-800">
                  {item.rotulo}
                  {!item.obrigatorio && <span className="ml-1 text-xs font-normal text-slate-500">(quando se aplica)</span>}
                </p>
                {item.documentos.map((d) => (
                  <div key={d.id} className="mt-1 flex items-center gap-1 text-xs text-slate-500">
                    <FileText className="size-3" aria-hidden /> {d.extensao.toUpperCase()} enviado em {formatarData(d.enviadoEm)}
                    <button
                      type="button"
                      onClick={() => void abrir(d.id).catch(() => setAviso('Não foi possível abrir o arquivo.'))}
                      className="ml-1 inline-flex items-center gap-0.5 rounded px-1 font-medium text-semear-700 hover:bg-semear-50"
                    >
                      <Eye className="size-3" aria-hidden /> Abrir
                    </button>
                    {podeApagar && (
                      <button
                        type="button"
                        onClick={() => remover.mutate(d.id)}
                        className="inline-flex items-center gap-0.5 rounded px-1 font-medium text-red-600 hover:bg-red-50"
                        aria-label={`Apagar ${item.rotulo}`}
                      >
                        <Trash2 className="size-3" aria-hidden /> Apagar
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
            {podeEnviar && (
              <Button
                variante="secundario"
                onClick={() => escolherArquivo(item.tipo)}
                carregando={enviar.isPending && enviar.variables?.tipo === item.tipo}
              >
                <Upload className="size-4" aria-hidden /> {item.enviado ? 'Enviar outro' : 'Enviar'}
              </Button>
            )}
          </li>
        ))}
      </ul>
      <p className="text-xs text-slate-500">PDF, JPG ou PNG de até 5 MB. Os arquivos ficam em área privada; os links de visualização expiram em 5 minutos.</p>
      <input
        ref={seletor}
        type="file"
        accept={FORMATOS_ACEITOS}
        className="hidden"
        onChange={(e) => {
          aoSelecionar(e.target.files?.[0])
          e.target.value = ''
        }}
      />
    </div>
  )
}
