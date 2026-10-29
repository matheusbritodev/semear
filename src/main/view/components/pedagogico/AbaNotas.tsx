import { useState } from 'react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Select } from '@/components/ui/Select'
import { useLancarNotas, useNotasTurma } from '@/hooks/usePedagogico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import type { NotasTurma } from '@/models/pedagogico'

/** Notas por disciplina e bimestre (0 a 10). Polivalente lança as disciplinas sem especialista; especialista, só a sua. */
export function AbaNotas({ turmaId }: { turmaId: string }) {
  const [disciplinaId, setDisciplina] = useState<string>()
  const [bimestre, setBimestre] = useState<number>()
  const notas = useNotasTurma(turmaId, disciplinaId, bimestre)

  if (notas.isPending) return <Carregando />
  if (notas.isError) return <EstadoErro erro={notas.error} aoTentarDeNovo={() => void notas.refetch()} />
  const n = notas.data

  if (n.nivel === 'EDUCACAO_INFANTIL') {
    return (
      <Card>
        <p className="text-sm text-slate-600">Na Educação Infantil não há lançamento de notas.</p>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="sm:w-64">
          <Select rotulo="Disciplina" ocultarRotulo value={n.disciplinaId} onChange={(e) => setDisciplina(e.target.value)}>
            {n.disciplinas.map((d) => (
              <option key={d.id} value={d.id}>
                {d.nome}
                {d.podeLancar ? '' : ' (consulta)'}
              </option>
            ))}
          </Select>
        </div>
        <div className="sm:w-40">
          <Select rotulo="Bimestre" ocultarRotulo value={n.bimestre} onChange={(e) => setBimestre(Number(e.target.value))}>
            {[1, 2, 3, 4].map((b) => (
              <option key={b} value={b}>
                {b}º bimestre
              </option>
            ))}
          </Select>
        </div>
      </div>
      <TabelaNotas key={`${n.disciplinaId}-${n.bimestre}`} notas={n} carregando={notas.isPlaceholderData} />
    </div>
  )
}

function TabelaNotas({ notas: n, carregando }: { notas: NotasTurma; carregando: boolean }) {
  const podeGravar = usePermissao('PEDAGOGICO', 'GRAVAR') && n.podeLancar
  const lancar = useLancarNotas(n.turmaId)
  const [valores, setValores] = useState<Record<string, string>>(() =>
    Object.fromEntries(n.alunos.map((a) => [a.matriculaId, a.valor === null ? '' : String(a.valor).replace('.', ',')])),
  )
  const invalidos = Object.entries(valores).filter(([, v]) => v !== '' && !valido(v)).map(([id]) => id)

  function salvar() {
    lancar.mutate({
      disciplinaId: n.disciplinaId,
      bimestre: n.bimestre,
      notas: n.alunos.map((a) => {
        const texto = valores[a.matriculaId] ?? ''
        return { matriculaId: a.matriculaId, valor: texto === '' ? null : numero(texto) }
      }),
    })
  }

  return (
    <Card>
      {!n.podeLancar && <p className="mb-3 text-sm text-slate-500">Somente consulta: esta disciplina é de outro professor.</p>}
      <ul className={`divide-y divide-slate-100 ${carregando ? 'opacity-60' : ''}`}>
        {n.alunos.map((a) => {
          const erro = invalidos.includes(a.matriculaId)
          return (
            <li key={a.matriculaId} className="flex items-center justify-between gap-3 py-2">
              <label htmlFor={`nota-${a.matriculaId}`} className="text-sm text-slate-800">
                <span className="mr-2 text-xs text-slate-400 tabular-nums">{a.numeroCadastro}</span>
                {a.nome}
              </label>
              <input
                id={`nota-${a.matriculaId}`}
                inputMode="decimal"
                value={valores[a.matriculaId] ?? ''}
                readOnly={!podeGravar}
                aria-invalid={erro}
                onChange={(e) => setValores((v) => ({ ...v, [a.matriculaId]: e.target.value }))}
                placeholder="—"
                className={`w-20 rounded-lg border px-2 py-1 text-center text-sm tabular-nums read-only:bg-slate-50 ${
                  erro ? 'border-red-400 ring-2 ring-red-100' : 'border-slate-300'
                }`}
              />
            </li>
          )
        })}
      </ul>
      {podeGravar && (
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <Button disabled={invalidos.length > 0} carregando={lancar.isPending} onClick={salvar}>
            Salvar notas
          </Button>
          {invalidos.length > 0 && <span className="text-sm text-red-600">Use notas de 0 a 10, com até duas casas (ex.: 7,5).</span>}
          {lancar.isSuccess && <span className="text-sm text-semear-700">Notas salvas.</span>}
          <span className="text-xs text-slate-500">Deixe em branco para apagar uma nota lançada.</span>
        </div>
      )}
      {lancar.isError && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {lancar.error instanceof ApiError ? (lancar.error.campos[0]?.mensagem ?? lancar.error.message) : 'Não foi possível salvar.'}
        </p>
      )}
    </Card>
  )
}

function numero(texto: string) {
  return Number(texto.trim().replace(',', '.'))
}

function valido(texto: string) {
  return /^\d{1,2}([.,]\d{1,2})?$/.test(texto.trim()) && numero(texto) >= 0 && numero(texto) <= 10
}
