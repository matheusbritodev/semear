import { useState, type FormEvent } from 'react'
import { Check, Copy, Pencil, Plus, Trash2, X } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { SeletorAno } from '@/components/comuns/SeletorAno'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { useAnoCorrente } from '@/hooks/useAcademico'
import { useEditarLista, useListaMaterial, useSeries } from '@/hooks/useFormalizacao'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import type { ItemMaterial } from '@/models/formalizacao'

/** Listas de materiais por série e ano (RF06). O portal mostra ao responsável a lista da série do filho. */
export function MateriaisPage() {
  const series = useSeries()
  const corrente = useAnoCorrente()
  const [serieEscolhida, setSerie] = useState<string>()
  const [anoEscolhido, setAno] = useState<number>()
  const serieId = serieEscolhida ?? series.data?.[0]?.id
  const ano = anoEscolhido ?? corrente.data?.ano
  const lista = useListaMaterial(serieId, ano)
  const editar = useEditarLista(serieId, ano)
  const podeEditar = usePermissao('MATRICULA', 'GRAVAR')
  const [descricao, setDescricao] = useState('')
  const [quantidade, setQuantidade] = useState('1')

  const erro = [editar.adicionar, editar.alterar, editar.remover, editar.copiar].find((m) => m.isError)?.error

  function adicionar(e: FormEvent) {
    e.preventDefault()
    editar.adicionar.mutate(
      { descricao, quantidade: Number(quantidade) },
      { onSuccess: () => (setDescricao(''), setQuantidade('1')) },
    )
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader titulo="Listas de materiais" descricao="Uma lista por série e ano letivo. O responsável vê a da série do filho no portal." />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <div className="sm:w-56">
          <Select rotulo="Série" ocultarRotulo value={serieId ?? ''} onChange={(e) => setSerie(e.target.value)}>
            {series.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nome}
              </option>
            ))}
          </Select>
        </div>
        <SeletorAno ano={ano} aoMudar={setAno} />
      </div>

      {lista.isPending ? (
        <Carregando />
      ) : lista.isError ? (
        <EstadoErro erro={lista.error} aoTentarDeNovo={() => void lista.refetch()} />
      ) : (
        <Card>
          {lista.data.itens.length === 0 ? (
            <div className="space-y-3 py-2 text-sm text-slate-600">
              <p>Nenhum item na lista desta série em {ano}.</p>
              {podeEditar && ano !== undefined && (
                <Button variante="secundario" carregando={editar.copiar.isPending} onClick={() => editar.copiar.mutate(ano - 1)}>
                  <Copy className="size-4" aria-hidden /> Copiar a lista de {ano - 1}
                </Button>
              )}
            </div>
          ) : (
            <ul className={`divide-y divide-slate-100 ${lista.isPlaceholderData ? 'opacity-60' : ''}`}>
              {lista.data.itens.map((item) => (
                <LinhaItem key={item.id} item={item} podeEditar={podeEditar} editar={editar} />
              ))}
            </ul>
          )}

          {podeEditar && (
            <form onSubmit={adicionar} className="mt-4 flex flex-col gap-3 border-t border-slate-100 pt-4 sm:flex-row sm:items-end">
              <div className="flex-1">
                <Input rotulo="Novo item" value={descricao} maxLength={200} onChange={(e) => setDescricao(e.target.value)} />
              </div>
              <div className="sm:w-24">
                <Input rotulo="Qtd." type="number" min={1} max={999} value={quantidade} onChange={(e) => setQuantidade(e.target.value)} />
              </div>
              <Button type="submit" disabled={!descricao.trim() || !(Number(quantidade) >= 1)} carregando={editar.adicionar.isPending}>
                <Plus className="size-4" aria-hidden /> Adicionar
              </Button>
            </form>
          )}
          {erro && (
            <p role="alert" className="mt-3 text-sm text-red-600">
              {erro instanceof ApiError ? erro.message : 'Não foi possível salvar.'}
            </p>
          )}
        </Card>
      )}
    </div>
  )
}

function LinhaItem({
  item,
  podeEditar,
  editar,
}: {
  item: ItemMaterial
  podeEditar: boolean
  editar: ReturnType<typeof useEditarLista>
}) {
  const [editando, setEditando] = useState(false)
  const [descricao, setDescricao] = useState(item.descricao)
  const [quantidade, setQuantidade] = useState(String(item.quantidade))

  if (editando) {
    return (
      <li className="flex flex-col gap-2 py-2 sm:flex-row sm:items-end">
        <div className="flex-1">
          <Input rotulo="Item" value={descricao} maxLength={200} onChange={(e) => setDescricao(e.target.value)} />
        </div>
        <div className="sm:w-24">
          <Input rotulo="Qtd." type="number" min={1} max={999} value={quantidade} onChange={(e) => setQuantidade(e.target.value)} />
        </div>
        <div className="flex gap-1">
          <Button
            aria-label="Salvar item"
            carregando={editar.alterar.isPending}
            onClick={() =>
              editar.alterar.mutate({ id: item.id, descricao, quantidade: Number(quantidade) }, { onSuccess: () => setEditando(false) })
            }
          >
            <Check className="size-4" aria-hidden />
          </Button>
          <Button variante="fantasma" aria-label="Cancelar" onClick={() => setEditando(false)}>
            <X className="size-4" aria-hidden />
          </Button>
        </div>
      </li>
    )
  }

  return (
    <li className="flex items-center justify-between gap-3 py-2 text-sm">
      <span className="text-slate-800">{item.descricao}</span>
      <span className="flex items-center gap-2">
        <span className="tabular-nums text-slate-500">{item.quantidade}×</span>
        {podeEditar && (
          <>
            <button type="button" onClick={() => setEditando(true)} className="rounded p-1 text-slate-400 hover:text-semear-700" aria-label={`Editar ${item.descricao}`}>
              <Pencil className="size-4" aria-hidden />
            </button>
            <button
              type="button"
              onClick={() => editar.remover.mutate(item.id)}
              className="rounded p-1 text-slate-400 hover:text-red-600"
              aria-label={`Remover ${item.descricao}`}
            >
              <Trash2 className="size-4" aria-hidden />
            </button>
          </>
        )}
      </span>
    </li>
  )
}
