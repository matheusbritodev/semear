import { Link } from 'react-router'
import { AlertTriangle } from 'lucide-react'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { Badge } from '@/components/ui/Badge'
import { useFrequenciaTurma } from '@/hooks/usePedagogico'

/** Frequência acumulada de cada aluno no ano. Destaca quem está abaixo dos 75% da LDB. */
export function AbaFrequencia({ turmaId }: { turmaId: string }) {
  const frequencia = useFrequenciaTurma(turmaId)
  if (frequencia.isPending) return <Carregando />
  if (frequencia.isError) return <EstadoErro erro={frequencia.error} aoTentarDeNovo={() => void frequencia.refetch()} />
  const abaixo = frequencia.data.filter((f) => f.abaixoDoMinimo)

  return (
    <div className="space-y-4">
      {abaixo.length > 0 && (
        <p role="status" className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
          {abaixo.length} {abaixo.length === 1 ? 'aluno está' : 'alunos estão'} abaixo da frequência mínima de 75% exigida pela LDB.
          Vale conversar com a família.
        </p>
      )}
      <Tabela legenda="Frequência dos alunos">
        <thead>
          <tr>
            <Th>Aluno</Th>
            <Th className="text-right">Aulas</Th>
            <Th className="text-right">Faltas</Th>
            <Th className="text-right">Presença</Th>
          </tr>
        </thead>
        <tbody>
          {frequencia.data.map((f) => (
            <Tr key={f.matriculaId}>
              <Td>
                <Link to={`/escola/alunos/${f.alunoId}`} className="text-slate-900 hover:text-semear-700 hover:underline">
                  {f.nome}
                </Link>
              </Td>
              <Td className="text-right tabular-nums">{f.aulas}</Td>
              <Td className="text-right tabular-nums">{f.faltas}</Td>
              <Td className="text-right">
                {f.percentual === null ? (
                  <span className="text-slate-400">—</span>
                ) : (
                  <Badge cor={f.abaixoDoMinimo ? 'vermelho' : f.percentual < 85 ? 'amarelo' : 'verde'}>
                    {String(f.percentual).replace('.', ',')}%
                  </Badge>
                )}
              </Td>
            </Tr>
          ))}
        </tbody>
      </Tabela>
    </div>
  )
}
