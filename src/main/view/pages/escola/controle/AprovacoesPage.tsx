import { useState } from 'react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { CartaoSolicitacao } from '@/components/controle/CartaoSolicitacao'
import { Card } from '@/components/ui/Card'
import { useAprovacoes } from '@/hooks/useControle'

type Aba = 'pendentes' | 'historico'

/**
 * Caixa de aprovações (segregação de funções): conceder bolsa, mudar desconto de bolsa e mudar preços
 * só valem depois que uma segunda pessoa aprova. Quem pediu não aprova o próprio pedido.
 */
export function AprovacoesPage() {
  const [aba, setAba] = useState<Aba>('pendentes')
  const consulta = useAprovacoes(aba === 'pendentes' ? 'PENDENTE' : undefined)

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        titulo="Aprovações"
        descricao="Bolsas e preços mexem no que as famílias pagam: só valem depois que uma segunda pessoa aprova. Quem fez o pedido não pode aprová-lo, e quem é responsável pelo aluno não decide sobre ele."
      />
      <div className="mb-4 flex gap-1 rounded-lg bg-slate-100 p-1 text-sm" role="tablist">
        {(
          [
            ['pendentes', 'Aguardando'],
            ['historico', 'Histórico'],
          ] as const
        ).map(([valor, rotulo]) => (
          <button
            key={valor}
            role="tab"
            aria-selected={aba === valor}
            onClick={() => setAba(valor)}
            className={`flex-1 rounded-md px-3 py-1.5 font-medium ${aba === valor ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600'}`}
          >
            {rotulo}
          </button>
        ))}
      </div>

      {consulta.isPending ? (
        <Carregando />
      ) : consulta.isError ? (
        <EstadoErro erro={consulta.error} aoTentarDeNovo={() => void consulta.refetch()} />
      ) : consulta.data.length === 0 ? (
        <EstadoVazio
          titulo={aba === 'pendentes' ? 'Nenhum pedido aguardando' : 'Nenhum pedido ainda'}
          descricao="Os pedidos aparecem aqui quando alguém concede uma bolsa, muda o desconto de uma bolsa ou altera preços."
        />
      ) : (
        <Card>
          <ul className="divide-y divide-slate-100">
            {consulta.data.map((s) => (
              <CartaoSolicitacao key={s.id} s={s} />
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
