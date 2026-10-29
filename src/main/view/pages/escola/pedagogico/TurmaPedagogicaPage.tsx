import { useParams, useSearchParams } from 'react-router'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { AbaChamada } from '@/components/pedagogico/AbaChamada'
import { AbaDiarios } from '@/components/pedagogico/AbaDiarios'
import { AbaFrequencia } from '@/components/pedagogico/AbaFrequencia'
import { AbaNotas } from '@/components/pedagogico/AbaNotas'
import { AbaPlanos } from '@/components/pedagogico/AbaPlanos'
import { useTurmasPedagogicas } from '@/hooks/usePedagogico'
import { useTurma } from '@/hooks/useAcademico'
import { hojeIso } from '@/lib/datas'

const ABAS = [
  { id: 'chamada', rotulo: 'Diário e chamada' },
  { id: 'diarios', rotulo: 'Diários do mês' },
  { id: 'planos', rotulo: 'Planos' },
  { id: 'notas', rotulo: 'Notas' },
  { id: 'frequencia', rotulo: 'Frequência' },
] as const
type Aba = (typeof ABAS)[number]['id']

/** Uma turma no pedagógico (RF08). A aba e o dia ficam na URL, para voltar e compartilhar o link. */
export function TurmaPedagogicaPage() {
  const { id = '' } = useParams()
  const [parametros, setParametros] = useSearchParams()
  const turma = useTurma(id)
  const lista = useTurmasPedagogicas(turma.data?.turma.ano)
  const minha = lista.data?.find((t) => t.turmaId === id)
  const souDaTurma = (minha?.disciplinas.length ?? 0) > 0
  const infantil = turma.data?.turma.nivel === 'EDUCACAO_INFANTIL'

  const aba = (ABAS.find((a) => a.id === parametros.get('aba'))?.id ?? 'chamada') as Aba
  const data = parametros.get('data') ?? hojeIso()
  const ir = (novaAba: Aba, novaData?: string) =>
    setParametros((p) => {
      p.set('aba', novaAba)
      if (novaData) p.set('data', novaData)
      return p
    })

  if (turma.isPending) return <Carregando />
  if (turma.isError) return <EstadoErro erro={turma.error} aoTentarDeNovo={() => void turma.refetch()} />

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        titulo={turma.data.turma.nomeCompleto}
        descricao={minha && souDaTurma ? `${turma.data.turma.ano} · ${minha.disciplinas.join(' · ')}` : `${turma.data.turma.ano} · somente consulta`}
        voltarPara={{ rota: '/escola/pedagogico', rotulo: 'Pedagógico' }}
      />
      <nav aria-label="Seções da turma" className="mb-6 flex gap-1 overflow-x-auto border-b border-slate-200">
        {ABAS.filter((a) => !(infantil && a.id === 'notas')).map((a) => (
          <button
            key={a.id}
            type="button"
            aria-current={aba === a.id ? 'page' : undefined}
            onClick={() => ir(a.id)}
            className={`-mb-px shrink-0 border-b-2 px-3 py-2 text-sm font-medium ${
              aba === a.id ? 'border-semear-600 text-semear-700' : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            {a.rotulo}
          </button>
        ))}
      </nav>

      {aba === 'chamada' && <AbaChamada turmaId={id} data={data} aoMudarData={(d) => ir('chamada', d)} />}
      {aba === 'diarios' && <AbaDiarios turmaId={id} aoAbrirDia={(d) => ir('chamada', d)} />}
      {aba === 'planos' && <AbaPlanos turmaId={id} souDaTurma={souDaTurma} />}
      {aba === 'notas' && <AbaNotas turmaId={id} />}
      {aba === 'frequencia' && <AbaFrequencia turmaId={id} />}
    </div>
  )
}
