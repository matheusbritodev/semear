import { Link, useSearchParams } from 'react-router'
import { AlertasTurmas } from '@/components/comuns/AlertasTurmas'
import { BarraOcupacao } from '@/components/comuns/BarraOcupacao'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Badge } from '@/components/ui/Badge'
import { useAnoCorrente, useTurmas } from '@/hooks/useAcademico'
import type { Turma } from '@/models/academico'

export function TurmasPage() {
  const [parametros, setParametros] = useSearchParams()
  const corrente = useAnoCorrente()
  const ano = parametros.get('ano') ? Number(parametros.get('ano')) : corrente.data?.ano
  const turmas = useTurmas(ano, ano !== undefined)

  const lista = turmas.data ?? []
  const niveis = [...new Set(lista.map((t) => t.nivelRotulo))]
  const ocupadas = lista.reduce((soma, t) => soma + t.ocupadas, 0)
  const vagas = lista.reduce((soma, t) => soma + t.vagas, 0)
  const lotadas = lista.filter((t) => t.lotada).length

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Turmas"
        descricao="Ocupação de cada turma. O limite é de 30 alunos (RN04)."
        acoes={<SeletorAno ano={ano} aoMudar={(novo) => setParametros({ ano: String(novo) })} />}
      />
      <AlertasTurmas />

      {turmas.isPending ? (
        <Carregando />
      ) : turmas.isError ? (
        <EstadoErro erro={turmas.error} aoTentarDeNovo={() => void turmas.refetch()} />
      ) : lista.length === 0 ? (
        <EstadoVazio titulo="Nenhuma turma neste ano" />
      ) : (
        <>
          <dl className="mb-6 grid grid-cols-3 gap-3">
            <Resumo rotulo="Alunos" valor={ocupadas} />
            <Resumo rotulo="Vagas livres" valor={vagas} />
            <Resumo rotulo="Turmas lotadas" valor={lotadas} destaque={lotadas > 0} />
          </dl>
          {niveis.map((nivel) => (
            <section key={nivel} className="mb-8">
              <h2 className="mb-3 text-sm font-semibold tracking-wide text-slate-500 uppercase">{nivel}</h2>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {lista
                  .filter((t) => t.nivelRotulo === nivel)
                  .map((turma) => (
                    <CartaoTurma key={turma.id} turma={turma} />
                  ))}
              </div>
            </section>
          ))}
        </>
      )}
    </div>
  )
}

function Resumo({ rotulo, valor, destaque = false }: { rotulo: string; valor: number; destaque?: boolean }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-4 py-3">
      <dt className="text-xs text-slate-500">{rotulo}</dt>
      <dd className={`text-2xl font-semibold ${destaque ? 'text-red-600' : 'text-slate-900'}`}>{valor}</dd>
    </div>
  )
}

function CartaoTurma({ turma }: { turma: Turma }) {
  return (
    <Link
      to={`/escola/turmas/${turma.id}`}
      className="block rounded-xl border border-slate-200 bg-white p-4 shadow-xs transition hover:border-semear-200 hover:shadow-sm focus-visible:outline-2 focus-visible:outline-semear-600"
    >
      <div className="mb-3 flex items-start justify-between gap-2">
        <div>
          <p className="font-semibold text-slate-900">{turma.nomeCompleto}</p>
          <p className="text-xs text-slate-500">{turma.ano}</p>
        </div>
        {turma.lotada ? <Badge cor="vermelho">Lotada</Badge> : <Badge cor="verde">{turma.vagas} vagas</Badge>}
      </div>
      <BarraOcupacao ocupadas={turma.ocupadas} capacidade={turma.capacidadeMax} />
    </Link>
  )
}
