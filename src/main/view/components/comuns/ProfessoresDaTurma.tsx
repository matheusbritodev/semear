import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Select } from '@/components/ui/Select'
import { useOpcoesProfessores, useProfessoresDaTurma } from '@/hooks/useConfiguracao'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import type { ProfessorDaTurma } from '@/models/academico'

/**
 * Professores da turma: um regente (polivalente, dá as disciplinas sem especialista) e especialistas por disciplina.
 * O vínculo define o que o professor vê e lança (RD07).
 */
export function ProfessoresDaTurmaCard({ turmaId, professores }: { turmaId: string; professores: ProfessorDaTurma[] }) {
  const podeEditar = usePermissao('TURMAS', 'GRAVAR')
  const opcoes = useOpcoesProfessores(podeEditar)
  const { vincular, remover } = useProfessoresDaTurma(turmaId)
  const [professor, setProfessor] = useState('')
  const [disciplina, setDisciplina] = useState('')
  const temRegente = professores.some((p) => p.disciplinaId === null)
  const ocupadas = new Set(professores.map((p) => p.disciplinaId).filter(Boolean))
  const erro = vincular.error ?? remover.error

  return (
    <Card titulo="Professores" className="md:col-span-2">
      {professores.length === 0 ? (
        <p className="text-sm text-slate-500">Nenhum professor vinculado.</p>
      ) : (
        <ul className="divide-y divide-slate-100 text-sm">
          {professores.map((p) => (
            <li key={p.vinculoId} className="flex items-center justify-between gap-2 py-2">
              <span>{p.nome}</span>
              <span className="flex items-center gap-2 text-slate-500">
                {p.disciplina ?? 'Regente (polivalente)'}
                {podeEditar && (
                  <button
                    type="button"
                    onClick={() => remover.mutate(p.vinculoId)}
                    className="rounded p-1 text-slate-400 hover:text-red-600"
                    aria-label={`Tirar ${p.nome} da turma`}
                  >
                    <Trash2 className="size-4" aria-hidden />
                  </button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
      {podeEditar && opcoes.data && (
        <div className="mt-4 flex flex-col gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:items-end">
          <div className="sm:flex-1">
            <Select rotulo="Professor" value={professor} onChange={(e) => setProfessor(e.target.value)}>
              <option value="">Escolha…</option>
              {opcoes.data.professores.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.nome}
                </option>
              ))}
            </Select>
          </div>
          <div className="sm:w-56">
            <Select rotulo="Função" value={disciplina} onChange={(e) => setDisciplina(e.target.value)}>
              <option value="" disabled={temRegente}>
                Regente (polivalente){temRegente ? ' — já tem' : ''}
              </option>
              {opcoes.data.disciplinas.map((d) => (
                <option key={d.id} value={d.id} disabled={ocupadas.has(d.id)}>
                  {d.nome}
                  {ocupadas.has(d.id) ? ' — já tem' : ''}
                </option>
              ))}
            </Select>
          </div>
          <Button
            disabled={!professor || (disciplina === '' && temRegente)}
            carregando={vincular.isPending}
            onClick={() =>
              vincular.mutate({ funcionarioId: professor, disciplinaId: disciplina || null }, { onSuccess: () => setProfessor('') })
            }
          >
            <Plus className="size-4" aria-hidden /> Vincular
          </Button>
        </div>
      )}
      {erro && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {erro instanceof ApiError ? erro.message : 'Não foi possível concluir.'}
        </p>
      )}
    </Card>
  )
}
