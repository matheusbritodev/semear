import { useState } from 'react'
import { Link } from 'react-router'
import { BookOpen, CalendarCheck, ClipboardCheck, NotebookPen } from 'lucide-react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Badge } from '@/components/ui/Badge'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { useTurmasPedagogicas } from '@/hooks/usePedagogico'
import { formatarData } from '@/lib/formatters'

/** Turmas para o pedagógico (RF08). Professor vê só as suas; gestão e secretaria veem todas, só para consultar. */
export function TurmasPedagogicasPage() {
  const corrente = useAnoCorrente()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  const turmas = useTurmasPedagogicas(ano)
  const podeGravar = usePermissao('PEDAGOGICO', 'GRAVAR')
  const minhas = turmas.data?.filter((t) => t.disciplinas.length > 0) ?? []
  const titulo = minhas.length > 0 ? 'Minhas turmas' : 'Pedagógico'

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        titulo={titulo}
        descricao="Diário de classe com chamada, planos de aula, notas por bimestre e frequência."
        acoes={
          <>
            <Link to="/escola/pedagogico/calendario" className="inline-flex items-center gap-1 text-sm font-medium text-semear-700 hover:underline">
              <CalendarCheck className="size-4" aria-hidden /> Calendário letivo
            </Link>
            <SeletorAno ano={ano} aoMudar={setAno} />
          </>
        }
      />

      {turmas.isPending ? (
        <Carregando />
      ) : turmas.isError ? (
        <EstadoErro erro={turmas.error} aoTentarDeNovo={() => void turmas.refetch()} />
      ) : turmas.data.length === 0 ? (
        <EstadoVazio titulo="Nenhuma turma" descricao="Você ainda não está vinculado(a) a turmas neste ano." />
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {turmas.data.map((t) => {
            return (
              <div key={t.turmaId} className="flex flex-col rounded-xl border border-slate-200 bg-white p-4">
                <div className="flex items-start justify-between gap-2">
                  <Link to={`/escola/pedagogico/turmas/${t.turmaId}`} className="font-semibold text-slate-900 hover:text-semear-700">
                    {t.nomeCompleto}
                  </Link>
                  <Badge cor="cinza">{t.bimestreAtual}º bim.</Badge>
                </div>
                <p className="text-sm text-slate-500">{t.alunosAtivos} alunos ativos</p>
                {t.disciplinas.length > 0 && <p className="mt-1 text-sm text-slate-600">{t.disciplinas.join(' · ')}</p>}
                <p className="mt-2 text-xs text-slate-500">
                  Último diário: {t.ultimoDiario ? formatarData(t.ultimoDiario) : 'nenhum'}
                </p>
                <div className="mt-4 flex flex-wrap gap-2 text-sm">
                  <Atalho to={`/escola/pedagogico/turmas/${t.turmaId}?aba=chamada`} icone={ClipboardCheck} rotulo={podeGravar ? 'Chamada' : 'Diário'} />
                  <Atalho to={`/escola/pedagogico/turmas/${t.turmaId}?aba=planos`} icone={NotebookPen} rotulo="Planos" />
                  {t.nivel === 'ENSINO_FUNDAMENTAL' && <Atalho to={`/escola/pedagogico/turmas/${t.turmaId}?aba=notas`} icone={BookOpen} rotulo="Notas" />}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

function Atalho({ to, icone: Icone, rotulo }: { to: string; icone: typeof BookOpen; rotulo: string }) {
  return (
    <Link to={to} className="inline-flex items-center gap-1 rounded-lg bg-semear-50 px-2.5 py-1.5 font-medium text-semear-800 hover:bg-semear-100">
      <Icone className="size-4" aria-hidden /> {rotulo}
    </Link>
  )
}
