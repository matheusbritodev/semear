import { useState } from 'react'
import { Pencil, Plus, Trash2 } from '@/components/ui/icones'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { usePlanos, useSalvarPlano } from '@/hooks/usePedagogico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { diaDaSemana, fimDoMes, hojeIso, mesIso, somarMeses } from '@/lib/datas'
import { ROTULO_TIPO_PLANO, type Plano, type TipoPlano } from '@/models/pedagogico'

/** Planos de aula e planejamentos diários (RF08.2): este mês e o próximo. */
export function AbaPlanos({ turmaId, souDaTurma }: { turmaId: string; souDaTurma: boolean }) {
  const inicio = mesIso(hojeIso())
  const planos = usePlanos(turmaId, `${inicio}-01`, fimDoMes(somarMeses(inicio, 1)))
  const { remover } = useSalvarPlano(turmaId)
  const podeGravar = usePermissao('PEDAGOGICO', 'GRAVAR') && souDaTurma
  const [editando, setEditando] = useState<Plano | 'novo' | null>(null)

  return (
    <div className="space-y-4">
      {podeGravar && (
        <Button onClick={() => setEditando('novo')}>
          <Plus className="size-4" aria-hidden /> Novo plano
        </Button>
      )}
      {planos.isPending ? (
        <Carregando />
      ) : planos.isError ? (
        <EstadoErro erro={planos.error} aoTentarDeNovo={() => void planos.refetch()} />
      ) : planos.data.length === 0 ? (
        <EstadoVazio titulo="Nenhum plano para este mês e o próximo" />
      ) : (
        <ul className="space-y-2">
          {planos.data.map((p) => (
            <li key={p.id} className="rounded-xl border border-slate-200 bg-white p-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <p className="text-sm font-medium text-slate-900 capitalize">{diaDaSemana(p.dataPrevista)}</p>
                  <p className="text-xs text-slate-500">
                    <Badge cor={p.tipo === 'PLANO_AULA' ? 'azul' : 'cinza'}>{ROTULO_TIPO_PLANO[p.tipo]}</Badge> {p.professor}
                  </p>
                </div>
                {p.meu && podeGravar && (
                  <span className="flex gap-1">
                    <button type="button" onClick={() => setEditando(p)} className="rounded p-1 text-slate-400 hover:text-semear-700" aria-label="Editar plano">
                      <Pencil className="size-4" aria-hidden />
                    </button>
                    <button type="button" onClick={() => remover.mutate(p.id)} className="rounded p-1 text-slate-400 hover:text-red-600" aria-label="Apagar plano">
                      <Trash2 className="size-4" aria-hidden />
                    </button>
                  </span>
                )}
              </div>
              <p className="mt-2 text-sm font-medium text-slate-800">{p.objetivo}</p>
              <p className="mt-1 text-sm whitespace-pre-line text-slate-600">{p.conteudo}</p>
            </li>
          ))}
        </ul>
      )}
      {editando && <PlanoModal turmaId={turmaId} plano={editando === 'novo' ? null : editando} aoFechar={() => setEditando(null)} />}
    </div>
  )
}

function PlanoModal({ turmaId, plano, aoFechar }: { turmaId: string; plano: Plano | null; aoFechar: () => void }) {
  const { salvar } = useSalvarPlano(turmaId)
  const [tipo, setTipo] = useState<TipoPlano>(plano?.tipo ?? 'PLANO_AULA')
  const [data, setData] = useState(plano?.dataPrevista ?? hojeIso())
  const [objetivo, setObjetivo] = useState(plano?.objetivo ?? '')
  const [conteudo, setConteudo] = useState(plano?.conteudo ?? '')
  const erro = salvar.error instanceof ApiError ? salvar.error : null

  return (
    <Modal
      aberto
      titulo={plano ? 'Editar plano' : 'Novo plano'}
      aoFechar={aoFechar}
      rodape={
        <>
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          <Button
            disabled={!objetivo.trim() || !conteudo.trim() || !data}
            carregando={salvar.isPending}
            onClick={() => salvar.mutate({ id: plano?.id, dados: { tipo, dataPrevista: data, objetivo, conteudo } }, { onSuccess: aoFechar })}
          >
            Salvar
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Select rotulo="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoPlano)}>
            {(Object.keys(ROTULO_TIPO_PLANO) as TipoPlano[]).map((t) => (
              <option key={t} value={t}>
                {ROTULO_TIPO_PLANO[t]}
              </option>
            ))}
          </Select>
          <Input rotulo="Data prevista" type="date" value={data} onChange={(e) => setData(e.target.value)} />
        </div>
        <Input rotulo="Objetivo" value={objetivo} maxLength={255} onChange={(e) => setObjetivo(e.target.value)} />
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-slate-700">Atividades e recursos</span>
          <textarea
            rows={5}
            maxLength={4000}
            value={conteudo}
            onChange={(e) => setConteudo(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-semear-500 focus:ring-2 focus:ring-semear-200"
          />
        </label>
        {erro && (
          <p role="alert" className="text-sm text-red-600">
            {erro.campos[0]?.mensagem ?? erro.message}
          </p>
        )}
      </div>
    </Modal>
  )
}
