import { useState } from 'react'
import { Link } from 'react-router'
import { CalendarPlus } from 'lucide-react'
import { CampoBusca } from '@/components/comuns/CampoBusca'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { Paginacao } from '@/components/comuns/Paginacao'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { StatusCobrancaBadge } from '@/components/comuns/StatusBadges'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { DetalheCobrancaModal } from '@/components/financeiro/DetalheCobrancaModal'
import { FinanceiroAbas } from '@/components/financeiro/FinanceiroAbas'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { useCobrancas, useGerarCobrancas } from '@/hooks/useFinanceiro'
import { useDebounce, usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData, formatarMoeda } from '@/lib/formatters'
import { ROTULO_STATUS_COBRANCA, type StatusCobranca, type TipoCobranca } from '@/models/enums'
import { ROTULO_TIPO_COBRANCA, formatarCompetencia } from '@/models/financeiro'

const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro']

/** Cobranças (RF04): busca, filtros, valor de hoje (RN08/RN09), baixa presencial e geração de um período. */
export function CobrancasPage() {
  const corrente = useAnoCorrente()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  // Abre no mês atual: é o que o financeiro acompanha no dia a dia.
  const [mes, setMes] = useState<number | undefined>(new Date().getMonth() + 1)
  const [tipo, setTipo] = useState<TipoCobranca>()
  const [status, setStatus] = useState<StatusCobranca>()
  const [busca, setBusca] = useState('')
  const [pagina, setPagina] = useState(0)
  const termo = useDebounce(busca, 300)
  const [aberta, setAberta] = useState<string | null>(null)
  const [gerando, setGerando] = useState(false)
  const podeGerar = usePermissao('FINANCEIRO', 'GRAVAR')

  const cobrancas = useCobrancas({ ano, mes, tipo, status, busca: termo, pagina, tamanho: 20 }, ano !== undefined)
  const mudar = <T,>(setter: (v: T) => void) => (v: T) => {
    setter(v)
    setPagina(0)
  }

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Financeiro"
        descricao="Taxa de Associado (Infantil), Mensalidade (Fundamental) e matrícula. O valor de hoje já considera desconto de pontualidade ou multa e juros."
        acoes={
          podeGerar && (
            <Button onClick={() => setGerando(true)}>
              <CalendarPlus className="size-4" aria-hidden /> Gerar cobranças
            </Button>
          )
        }
      />
      <FinanceiroAbas />

      <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:flex-wrap lg:items-end">
        <CampoBusca valor={busca} aoMudar={mudar(setBusca)} placeholder="Aluno, responsável ou nº de cadastro" />
        <SeletorAno ano={ano} aoMudar={mudar(setAno)} />
        <div className="w-full sm:w-40">
          <Select rotulo="Mês" ocultarRotulo value={mes ?? ''} onChange={(e) => mudar(setMes)(e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">Todos os meses</option>
            {MESES.map((m, i) => (
              <option key={m} value={i + 1}>
                {m}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-full sm:w-48">
          <Select rotulo="Tipo" ocultarRotulo value={tipo ?? ''} onChange={(e) => mudar(setTipo)((e.target.value || undefined) as TipoCobranca | undefined)}>
            <option value="">Todos os tipos</option>
            {(Object.keys(ROTULO_TIPO_COBRANCA) as TipoCobranca[]).map((t) => (
              <option key={t} value={t}>
                {ROTULO_TIPO_COBRANCA[t]}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-full sm:w-40">
          <Select
            rotulo="Situação"
            ocultarRotulo
            value={status ?? ''}
            onChange={(e) => mudar(setStatus)((e.target.value || undefined) as StatusCobranca | undefined)}
          >
            <option value="">Todas as situações</option>
            {(Object.keys(ROTULO_STATUS_COBRANCA) as StatusCobranca[]).map((s) => (
              <option key={s} value={s}>
                {ROTULO_STATUS_COBRANCA[s]}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {cobrancas.isPending ? (
        <Carregando />
      ) : cobrancas.isError ? (
        <EstadoErro erro={cobrancas.error} aoTentarDeNovo={() => void cobrancas.refetch()} />
      ) : cobrancas.data.itens.length === 0 ? (
        <EstadoVazio titulo="Nenhuma cobrança encontrada" descricao="Ajuste os filtros ou gere as cobranças do período." />
      ) : (
        <>
          <ul className={`space-y-3 md:hidden ${cobrancas.isPlaceholderData ? 'opacity-60' : ''}`} aria-label="Cobranças">
            {cobrancas.data.itens.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => setAberta(c.id)}
                  className="w-full rounded-xl border border-slate-200 bg-white p-4 text-left hover:border-semear-200"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-slate-900">{c.aluno}</span>
                    <StatusCobrancaBadge status={c.status} />
                  </div>
                  <p className="text-xs text-slate-500">
                    {c.turma} · {c.responsavel}
                  </p>
                  <div className="mt-2 flex items-end justify-between gap-2">
                    <span className="text-sm text-slate-600">
                      {ROTULO_TIPO_COBRANCA[c.tipo]} · {formatarCompetencia(c.competencia)}
                      <br />
                      vence {formatarData(c.vencimento)}
                      {c.diasDeAtraso > 0 && <span className="text-red-700"> · {c.diasDeAtraso} dias</span>}
                    </span>
                    <span className="text-lg font-semibold text-slate-900 tabular-nums">
                      {c.status === 'PAGO' ? formatarMoeda(c.valorPago ?? c.valorHoje) : c.status === 'CANCELADO' ? '—' : formatarMoeda(c.valorHoje)}
                    </span>
                  </div>
                </button>
              </li>
            ))}
          </ul>
          <div className="hidden md:block">
          <Tabela legenda="Cobranças">
            <thead>
              <tr>
                <Th>Aluno</Th>
                <Th>Cobrança</Th>
                <Th>Vencimento</Th>
                <Th className="text-right">Valor</Th>
                <Th className="text-right">Hoje</Th>
                <Th>Situação</Th>
              </tr>
            </thead>
            <tbody className={cobrancas.isPlaceholderData ? 'opacity-60' : ''}>
              {cobrancas.data.itens.map((c) => (
                <Tr key={c.id}>
                  <Td>
                    <Link to={`/escola/alunos/${c.alunoId}`} className="font-medium text-slate-900 hover:text-semear-700 hover:underline">
                      {c.aluno}
                    </Link>
                    <p className="text-xs text-slate-500">
                      {c.turma} · {c.responsavel}
                    </p>
                  </Td>
                  <Td>
                    <button type="button" onClick={() => setAberta(c.id)} className="text-left font-medium text-semear-700 hover:underline">
                      {ROTULO_TIPO_COBRANCA[c.tipo]}
                    </button>
                    <p className="text-xs text-slate-500">{formatarCompetencia(c.competencia)}</p>
                  </Td>
                  <Td className="tabular-nums">
                    {formatarData(c.vencimento)}
                    {c.diasDeAtraso > 0 && <p className="text-xs text-red-700">{c.diasDeAtraso} dias</p>}
                  </Td>
                  <Td className="text-right tabular-nums">
                    {formatarMoeda(c.valorFinal)}
                    {c.descontoBolsa > 0 && <p className="text-xs text-semear-700">bolsa −{formatarMoeda(c.descontoBolsa)}</p>}
                  </Td>
                  <Td className="text-right font-medium tabular-nums">
                    {c.status === 'PAGO' ? formatarMoeda(c.valorPago ?? c.valorHoje) : c.status === 'CANCELADO' ? '—' : formatarMoeda(c.valorHoje)}
                  </Td>
                  <Td>
                    <StatusCobrancaBadge status={c.status} />
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Tabela>
          </div>
          <Paginacao pagina={pagina} tamanho={20} total={cobrancas.data.total} aoMudar={setPagina} />
        </>
      )}

      <DetalheCobrancaModal id={aberta} aoFechar={() => setAberta(null)} />
      {gerando && <GerarCobrancasModal anoInicial={ano} aoFechar={() => setGerando(false)} />}
    </div>
  )
}

/** Gera as cobranças mensais de um intervalo de meses (fn_gerar_cobrancas_periodo; não duplica, RN01). */
function GerarCobrancasModal({ anoInicial, aoFechar }: { anoInicial?: number; aoFechar: () => void }) {
  const [ano, setAno] = useState(anoInicial)
  const [inicio, setInicio] = useState(new Date().getMonth() + 1)
  const [fim, setFim] = useState(new Date().getMonth() + 1)
  const gerar = useGerarCobrancas()

  return (
    <Modal
      aberto
      titulo="Gerar cobranças mensais"
      aoFechar={aoFechar}
      rodape={
        gerar.isSuccess ? (
          <Button onClick={aoFechar}>Fechar</Button>
        ) : (
          <>
            <Button variante="secundario" onClick={aoFechar}>
              Cancelar
            </Button>
            <Button
              disabled={ano === undefined || inicio > fim}
              carregando={gerar.isPending}
              onClick={() => ano !== undefined && gerar.mutate({ ano, mesInicio: inicio, mesFim: fim })}
            >
              Gerar
            </Button>
          </>
        )
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-slate-600">
          Cria a Taxa de Associado ou a Mensalidade de cada matrícula ativa, com o valor da tabela e a bolsa aplicada. Quem já tem cobrança no
          mês não recebe outra. Os boletos são emitidos em seguida.
        </p>
        <SeletorAno ano={ano} aoMudar={setAno} />
        <div className="grid grid-cols-2 gap-3">
          <Select rotulo="De" value={inicio} onChange={(e) => setInicio(Number(e.target.value))}>
            {MESES.map((m, i) => (
              <option key={m} value={i + 1}>
                {m}
              </option>
            ))}
          </Select>
          <Select rotulo="Até" value={fim} onChange={(e) => setFim(Number(e.target.value))} erro={inicio > fim ? 'Escolha um mês igual ou depois do inicial' : undefined}>
            {MESES.map((m, i) => (
              <option key={m} value={i + 1}>
                {m}
              </option>
            ))}
          </Select>
        </div>
        {gerar.isSuccess && (
          <p role="status" className="rounded-lg bg-semear-50 px-3 py-2 text-sm text-semear-800">
            {gerar.data.criadas === 0
              ? 'Nenhuma cobrança nova: todas as matrículas ativas já tinham cobrança nesses meses.'
              : `${gerar.data.criadas} cobranças criadas.`}{' '}
            {gerar.data.boletosEmitidos > 0 && `${gerar.data.boletosEmitidos} boletos emitidos.`}
          </p>
        )}
        {gerar.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {gerar.error instanceof ApiError ? gerar.error.message : 'Não foi possível gerar as cobranças.'}
          </p>
        )}
      </div>
    </Modal>
  )
}
