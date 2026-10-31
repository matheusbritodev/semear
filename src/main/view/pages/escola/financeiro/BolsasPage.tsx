import { useState } from 'react'
import { Link } from 'react-router'
import { Pencil, Plus } from '@/components/ui/icones'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Tabela, Td, Th, Tr } from '@/components/comuns/Tabela'
import { FinanceiroAbas } from '@/components/financeiro/FinanceiroAbas'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Select } from '@/components/ui/Select'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { useBolsas, useBolsistas, useSalvarBolsa } from '@/hooks/useFinanceiro'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'
import type { Solicitacao } from '@/models/controle'
import { ROTULO_TIPO_DESCONTO, descreverDesconto, type Bolsa, type TipoDesconto } from '@/models/financeiro'

/** Bolsas (RF02/RN11). A concessão é feita na matrícula do aluno; aqui ficam os tipos e a lista de bolsistas. */
export function BolsasPage() {
  const bolsas = useBolsas()
  const podeEditar = usePermissao('BOLSAS', 'GRAVAR')
  const [editando, setEditando] = useState<Bolsa | 'nova' | null>(null)
  const corrente = useAnoCorrente()
  const [anoEscolhido, setAno] = useState<number>()
  const ano = anoEscolhido ?? corrente.data?.ano
  const bolsistas = useBolsistas(ano)

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        titulo="Financeiro"
        descricao="O desconto da bolsa entra sozinho nas cobranças mensais em aberto. Nunca na matrícula e nunca em cobrança já paga."
        acoes={
          podeEditar && (
            <Button onClick={() => setEditando('nova')}>
              <Plus className="size-4" aria-hidden /> Nova bolsa
            </Button>
          )
        }
      />
      <FinanceiroAbas />

      <h2 className="mb-3 text-base font-semibold text-slate-800">Tipos de bolsa</h2>
      {bolsas.isPending ? (
        <Carregando />
      ) : bolsas.isError ? (
        <EstadoErro erro={bolsas.error} aoTentarDeNovo={() => void bolsas.refetch()} />
      ) : (
        <div className="mb-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {bolsas.data.map((b) => (
            <div key={b.id} className={`rounded-xl border bg-white p-4 ${b.ativa ? 'border-slate-200' : 'border-dashed border-slate-300 opacity-70'}`}>
              <div className="flex items-start justify-between gap-2">
                <p className="font-semibold text-slate-900">{b.nome}</p>
                {podeEditar && (
                  <button type="button" onClick={() => setEditando(b)} className="text-slate-400 hover:text-semear-700" aria-label={`Editar ${b.nome}`}>
                    <Pencil className="size-4" aria-hidden />
                  </button>
                )}
              </div>
              <p className="text-2xl font-semibold text-semear-700">{descreverDesconto(b.tipoDesconto, b.valor)}</p>
              <p className="mt-1 text-sm text-slate-500">
                {b.bolsistasVigentes} {b.bolsistasVigentes === 1 ? 'bolsista' : 'bolsistas'} hoje
              </p>
              {!b.ativa && (
                <p className="mt-2">
                  <Badge>Desativada</Badge>
                </p>
              )}
              {b.descricao && <p className="mt-2 text-xs text-slate-500">{b.descricao}</p>}
            </div>
          ))}
        </div>
      )}

      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-slate-800">Bolsistas</h2>
        <SeletorAno ano={ano} aoMudar={setAno} />
      </div>
      {bolsistas.isPending ? (
        <Carregando />
      ) : bolsistas.isError ? (
        <EstadoErro erro={bolsistas.error} aoTentarDeNovo={() => void bolsistas.refetch()} />
      ) : bolsistas.data.length === 0 ? (
        <EstadoVazio titulo="Nenhum bolsista neste ano" descricao="Conceda a bolsa pela matrícula do aluno." />
      ) : (
        <Tabela legenda={`Bolsistas de ${ano}`}>
          <thead>
            <tr>
              <Th>Aluno</Th>
              <Th>Bolsa</Th>
              <Th>Vigência</Th>
              <Th>Situação</Th>
            </tr>
          </thead>
          <tbody>
            {bolsistas.data.map((c) => (
              <Tr key={c.id}>
                <Td>
                  <Link to={`/escola/matriculas/${c.matriculaId}`} className="font-medium text-slate-900 hover:text-semear-700 hover:underline">
                    {c.aluno}
                  </Link>
                  <p className="text-xs text-slate-500">{c.turma}</p>
                </Td>
                <Td>
                  {c.bolsa} <span className="text-slate-500">({descreverDesconto(c.tipoDesconto, c.valor)})</span>
                </Td>
                <Td className="tabular-nums text-slate-600">
                  {formatarData(c.vigenciaInicio)} – {c.vigenciaFim ? formatarData(c.vigenciaFim) : 'sem fim'}
                </Td>
                <Td>{c.vigente ? <Badge cor="verde">Vigente</Badge> : <Badge>Encerrada</Badge>}</Td>
              </Tr>
            ))}
          </tbody>
        </Tabela>
      )}

      {editando && <BolsaModal bolsa={editando === 'nova' ? null : editando} aoFechar={() => setEditando(null)} />}
    </div>
  )
}

function BolsaModal({ bolsa, aoFechar }: { bolsa: Bolsa | null; aoFechar: () => void }) {
  const salvar = useSalvarBolsa()
  const [nome, setNome] = useState(bolsa?.nome ?? '')
  const [descricao, setDescricao] = useState(bolsa?.descricao ?? '')
  const [tipo, setTipo] = useState<TipoDesconto>(bolsa?.tipoDesconto ?? 'PERCENTUAL')
  const [valor, setValor] = useState(bolsa ? String(bolsa.valor) : '')
  const [ativa, setAtiva] = useState(bolsa?.ativa ?? true)
  const [pedido, setPedido] = useState<Solicitacao | null>(null)

  const numero = Number(valor.replace(',', '.'))
  const erroValor =
    valor === '' ? undefined : !(numero > 0) ? 'Informe um valor maior que zero' : tipo === 'PERCENTUAL' && numero > 100 ? 'No máximo 100%' : undefined
  const valido = nome.trim() !== '' && valor !== '' && !erroValor

  async function enviar() {
    const resposta = await salvar.mutateAsync({ id: bolsa?.id, dados: { nome, descricao, tipoDesconto: tipo, valor: numero, ativa } })
    if ('solicitacao' in resposta && resposta.solicitacao) {
      setPedido(resposta.solicitacao)
      return
    }
    aoFechar()
  }

  return (
    <Modal
      aberto
      titulo={bolsa ? `Editar ${bolsa.nome}` : 'Nova bolsa'}
      aoFechar={aoFechar}
      rodape={
        <>
          <Button variante="secundario" onClick={aoFechar}>
            Cancelar
          </Button>
          {pedido ? (
            <Button onClick={aoFechar}>Fechar</Button>
          ) : (
            <Button disabled={!valido} carregando={salvar.isPending} onClick={() => void enviar().catch(() => undefined)}>
              Salvar
            </Button>
          )}
        </>
      }
    >
      <div className="space-y-4">
        <Input rotulo="Nome" value={nome} maxLength={100} onChange={(e) => setNome(e.target.value)} />
        <Input rotulo="Descrição (opcional)" value={descricao} maxLength={500} onChange={(e) => setDescricao(e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <Select rotulo="Tipo de desconto" value={tipo} onChange={(e) => setTipo(e.target.value as TipoDesconto)}>
            {(Object.keys(ROTULO_TIPO_DESCONTO) as TipoDesconto[]).map((t) => (
              <option key={t} value={t}>
                {ROTULO_TIPO_DESCONTO[t]}
              </option>
            ))}
          </Select>
          <Input
            rotulo={tipo === 'PERCENTUAL' ? 'Percentual (%)' : 'Valor (R$)'}
            inputMode="decimal"
            value={valor}
            onChange={(e) => setValor(e.target.value)}
            erro={erroValor}
          />
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={ativa} onChange={(e) => setAtiva(e.target.checked)} className="size-4 accent-semear-600" />
          Bolsa ativa
        </label>
        {bolsa && (
          <p className="text-xs text-slate-500">
            Nome, descrição e desativar valem na hora. Mudar o tipo ou o valor do desconto, ou reativar, afeta os {bolsa.bolsistasVigentes}{' '}
            bolsistas: vai para aprovação de outra pessoa. As cobranças já pagas não mudam.
          </p>
        )}
        {pedido && (
          <p role="status" className="rounded-lg bg-semear-50 px-3 py-2 text-sm text-semear-800">
            Enviado para aprovação: {pedido.resumo}. O desconto muda quando outra pessoa aprovar em Aprovações.
          </p>
        )}
        {salvar.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {salvar.error instanceof ApiError ? salvar.error.message : 'Não foi possível salvar a bolsa.'}
          </p>
        )}
      </div>
    </Modal>
  )
}
