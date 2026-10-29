import { CartaoRematricula } from '@/components/comuns/CartaoRematricula'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { PageHeader } from '@/components/comuns/PageHeader'
import { useOpcoesRematricula, useRematricular } from '@/hooks/usePortal'

/** RF01.5: rematrícula dos filhos para o próximo ano, pelo portal. */
export function RematriculaPage() {
  const opcoes = useOpcoesRematricula()
  const rematricular = useRematricular()

  return (
    <div>
      <PageHeader
        titulo="Rematrícula"
        descricao="Garanta a vaga dos seus filhos no próximo ano. A vaga é confirmada com o pagamento da matrícula."
        voltarPara={{ rota: '/portal', rotulo: 'Início' }}
      />
      {opcoes.isPending ? (
        <Carregando />
      ) : opcoes.isError ? (
        <EstadoErro erro={opcoes.error} aoTentarDeNovo={() => void opcoes.refetch()} />
      ) : opcoes.data.length === 0 ? (
        <EstadoVazio titulo="Nenhum aluno ligado ao seu cadastro" />
      ) : (
        <div className="space-y-3">
          {opcoes.data.map((opcao) => (
            <CartaoRematricula
              key={opcao.alunoId}
              titulo={opcao.aluno ?? undefined}
              opcao={opcao}
              podeRematricular
              emAndamento={rematricular.isPending}
              rematricular={(uniforme, edFisica) =>
                rematricular.mutateAsync({ alunoId: opcao.alunoId, fardaUniforme: uniforme, fardaEdFisica: edFisica })
              }
            />
          ))}
        </div>
      )}
    </div>
  )
}
