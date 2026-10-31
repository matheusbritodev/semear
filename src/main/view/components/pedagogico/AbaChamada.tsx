import { useState } from 'react'
import { CheckCheck } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { useChamada, useRegistrarDiario } from '@/hooks/usePedagogico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { diaDaSemana, hojeIso } from '@/lib/datas'
import type { Chamada } from '@/models/pedagogico'

type Marcacao = { presente: boolean; justificativa: string }

/** Diário do dia: conteúdo e chamada de todos os alunos ativos (RF08.1). Só o professor da turma grava (RD07). */
export function AbaChamada({ turmaId, data, aoMudarData }: { turmaId: string; data: string; aoMudarData: (data: string) => void }) {
  const chamada = useChamada(turmaId, data)

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <div className="w-48">
          <Input rotulo="Dia da aula" type="date" value={data} max={hojeIso()} onChange={(e) => e.target.value && aoMudarData(e.target.value)} />
        </div>
        <p className="pb-2 text-sm text-slate-500">{diaDaSemana(data)}</p>
      </div>
      {chamada.isPending ? (
        <Carregando />
      ) : chamada.isError ? (
        <EstadoErro erro={chamada.error} aoTentarDeNovo={() => void chamada.refetch()} />
      ) : (
        <FormularioChamada key={`${chamada.data.data}-${chamada.data.diarioId ?? 'novo'}`} chamada={chamada.data} />
      )}
    </div>
  )
}

function FormularioChamada({ chamada }: { chamada: Chamada }) {
  const podeGravar = usePermissao('PEDAGOGICO', 'GRAVAR')
  const registrar = useRegistrarDiario(chamada.turmaId)
  const [conteudo, setConteudo] = useState(chamada.conteudo ?? '')
  const [marcas, setMarcas] = useState<Record<string, Marcacao>>(() => inicial(chamada))
  const editavel = chamada.podeRegistrar && podeGravar

  if (chamada.motivoSemAula) {
    return (
      <Card>
        <p className="text-sm text-slate-600">
          Sem aula neste dia: <strong>{chamada.motivoSemAula}</strong>.
        </p>
      </Card>
    )
  }

  const faltas = Object.values(marcas).filter((m) => !m.presente).length
  const marcaDe = (id: string): Marcacao => marcas[id] ?? { presente: true, justificativa: '' }
  const marcar = (id: string, mudanca: Partial<Marcacao>) => setMarcas((atual) => ({ ...atual, [id]: { ...(atual[id] ?? { presente: true, justificativa: '' }), ...mudanca } }))

  function salvar() {
    registrar.mutate({
      data: chamada.data,
      conteudo,
      presencas: chamada.alunos.map((a) => ({
        matriculaId: a.matriculaId,
        presente: marcaDe(a.matriculaId).presente,
        justificativa: marcaDe(a.matriculaId).presente ? null : marcaDe(a.matriculaId).justificativa || null,
      })),
    })
  }

  return (
    <Card>
      {chamada.diarioId && (
        <p className="mb-3 text-sm text-slate-500">
          Diário registrado{chamada.professor ? ` por ${chamada.professor}` : ''}.{editavel && ' Você pode corrigir e salvar de novo.'}
        </p>
      )}
      {!chamada.diarioId && !editavel && <p className="mb-3 text-sm text-slate-500">Nenhum diário registrado neste dia.</p>}

      <label className="mb-4 flex flex-col gap-1.5 text-sm">
        <span className="font-medium text-slate-700">Conteúdo da aula</span>
        <textarea
          rows={3}
          maxLength={2000}
          value={conteudo}
          readOnly={!editavel}
          onChange={(e) => setConteudo(e.target.value)}
          placeholder="O que foi trabalhado hoje"
          className="rounded-lg border border-slate-300 px-3 py-2 outline-none read-only:bg-slate-50 focus:border-semear-500 focus:ring-2 focus:ring-semear-200"
        />
      </label>

      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium text-slate-700">
          Chamada · {chamada.alunos.length - faltas} presentes, {faltas} {faltas === 1 ? 'falta' : 'faltas'}
        </p>
        {editavel && (
          <Button
            variante="fantasma"
            onClick={() => setMarcas(Object.fromEntries(chamada.alunos.map((a) => [a.matriculaId, { presente: true, justificativa: '' }])))}
          >
            <CheckCheck className="size-4" aria-hidden /> Todos presentes
          </Button>
        )}
      </div>
      <ul className="divide-y divide-slate-100">
        {chamada.alunos.map((a) => {
          const m = marcaDe(a.matriculaId)
          return (
            <li key={a.matriculaId} className="flex flex-col gap-2 py-2 sm:flex-row sm:items-center">
              <span className="flex-1 text-sm text-slate-800">
                <span className="mr-2 text-xs text-slate-400 tabular-nums">{a.numeroCadastro}</span>
                {a.nome}
              </span>
              <div role="radiogroup" aria-label={`Presença de ${a.nome}`} className="flex gap-1">
                {[true, false].map((presente) => (
                  <button
                    key={String(presente)}
                    type="button"
                    role="radio"
                    aria-checked={m.presente === presente}
                    disabled={!editavel}
                    onClick={() => marcar(a.matriculaId, { presente })}
                    className={`rounded-lg px-3 py-1 text-sm font-medium disabled:cursor-default ${
                      m.presente === presente
                        ? presente
                          ? 'bg-semear-600 text-white'
                          : 'bg-red-600 text-white'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {presente ? 'Presente' : 'Falta'}
                  </button>
                ))}
              </div>
              {!m.presente && (
                <input
                  aria-label={`Justificativa da falta de ${a.nome}`}
                  value={m.justificativa}
                  maxLength={255}
                  readOnly={!editavel}
                  onChange={(e) => marcar(a.matriculaId, { justificativa: e.target.value })}
                  placeholder="Justificativa (opcional)"
                  className="rounded-lg border border-slate-300 px-2 py-1 text-sm read-only:bg-slate-50 sm:w-56"
                />
              )}
            </li>
          )
        })}
      </ul>

      {editavel && (
        // Fixo no rodapé: com turma cheia, a professora não precisa rolar até o fim para salvar.
        <div className="sticky bottom-0 -mx-5 mt-4 flex flex-wrap items-center gap-3 border-t border-slate-100 bg-white/95 px-5 py-3 backdrop-blur">
          <Button disabled={!conteudo.trim()} carregando={registrar.isPending} onClick={salvar}>
            {chamada.diarioId ? 'Salvar alterações' : 'Registrar diário'}
          </Button>
          {registrar.isSuccess && <span className="text-sm text-semear-700">Diário salvo.</span>}
        </div>
      )}
      {registrar.isError && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {registrar.error instanceof ApiError ? registrar.error.message : 'Não foi possível salvar o diário.'}
        </p>
      )}
    </Card>
  )
}

function inicial(chamada: Chamada): Record<string, Marcacao> {
  return Object.fromEntries(chamada.alunos.map((a) => [a.matriculaId, { presente: a.presente, justificativa: a.justificativa ?? '' }]))
}
