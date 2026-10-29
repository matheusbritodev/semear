import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { TipoDocumentoAluno } from '@/models/documento'
import { documentoServiceEscola, documentoServicePortal } from '@/services/DocumentoService'

/** Checklist RN12 de um aluno, pela escola ou pelo portal. */
export function useDocumentos(alunoId: string, area: 'escola' | 'portal', habilitado = true) {
  const servico = area === 'portal' ? documentoServicePortal : documentoServiceEscola
  const cliente = useQueryClient()
  const chave = ['documentos', area, alunoId]

  const checklist = useQuery({ queryKey: chave, queryFn: () => servico.checklist(alunoId), enabled: habilitado })

  const atualizar = () => {
    void cliente.invalidateQueries({ queryKey: chave })
    void cliente.invalidateQueries({ queryKey: ['matriculas'] })
    void cliente.invalidateQueries({ queryKey: ['alunos'] })
    void cliente.invalidateQueries({ queryKey: ['pendencias'] })
    void cliente.invalidateQueries({ queryKey: ['portal', 'filhos'] })
  }

  const enviar = useMutation({
    mutationFn: ({ tipo, arquivo }: { tipo: TipoDocumentoAluno; arquivo: File }) => servico.enviar(alunoId, tipo, arquivo),
    onSuccess: atualizar,
  })

  const remover = useMutation({ mutationFn: (documentoId: string) => servico.remover(documentoId), onSuccess: atualizar })

  /** Abre o arquivo em nova aba com link temporário (a aba é aberta antes, para não ser bloqueada). */
  async function abrir(documentoId: string) {
    const aba = window.open('', '_blank')
    try {
      const { url } = await servico.link(documentoId)
      if (aba) {
        aba.opener = null
        aba.location.href = url
      } else {
        window.location.href = url
      }
    } catch (e) {
      aba?.close()
      throw e
    }
  }

  return { checklist, enviar, remover, abrir }
}
