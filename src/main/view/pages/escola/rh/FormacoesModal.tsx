import { useState, type FormEvent } from 'react'
import { Trash2 } from '@/components/ui/icones'
import { Carregando, EstadoErro } from '@/components/comuns/Estados'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { useEditarFormacoes, useFormacoes } from '@/hooks/usePedagogico'
import { usePermissao } from '@/hooks/useUtilitarios'
import { ApiError } from '@/lib/ApiError'
import { formatarData } from '@/lib/formatters'

/** Formação continuada de um funcionário (RF08.4): cursos, carga horária e conclusão. */
export function FormacoesModal({ funcionarioId, nome, aoFechar }: { funcionarioId: string; nome: string; aoFechar: () => void }) {
  const formacoes = useFormacoes(funcionarioId)
  const editar = useEditarFormacoes(funcionarioId)
  const podeEditar = usePermissao('RH', 'GRAVAR')
  const [curso, setCurso] = useState('')
  const [carga, setCarga] = useState('')
  const [concluida, setConcluida] = useState('')
  const erro = editar.registrar.error ?? editar.remover.error
  const total = (formacoes.data ?? []).filter((f) => f.concluida).reduce((s, f) => s + f.cargaHoraria, 0)

  function registrar(e: FormEvent) {
    e.preventDefault()
    editar.registrar.mutate(
      { curso, cargaHoraria: Number(carga), concluidaEm: concluida || null },
      { onSuccess: () => (setCurso(''), setCarga(''), setConcluida('')) },
    )
  }

  return (
    <Modal aberto titulo={`Formação continuada · ${nome}`} aoFechar={aoFechar} rodape={<Button onClick={aoFechar}>Fechar</Button>}>
      {formacoes.isPending ? (
        <Carregando />
      ) : formacoes.isError ? (
        <EstadoErro erro={formacoes.error} />
      ) : (
        <div className="space-y-4">
          {formacoes.data.length === 0 ? (
            <p className="text-sm text-slate-600">Nenhum curso registrado.</p>
          ) : (
            <>
              <p className="text-sm text-slate-500">{total} horas concluídas.</p>
              <ul className="divide-y divide-slate-100 text-sm">
                {formacoes.data.map((f) => (
                  <li key={f.id} className="flex items-center justify-between gap-2 py-2">
                    <span>
                      <span className="text-slate-800">{f.curso}</span>{' '}
                      <span className="text-slate-500">· {f.cargaHoraria} h</span>
                    </span>
                    <span className="flex items-center gap-2">
                      {f.concluidaEm ? <Badge cor="verde">Concluído em {formatarData(f.concluidaEm)}</Badge> : <Badge cor="amarelo">Em andamento</Badge>}
                      {podeEditar && (
                        <button type="button" onClick={() => editar.remover.mutate(f.id)} className="rounded p-1 text-slate-400 hover:text-red-600" aria-label={`Remover ${f.curso}`}>
                          <Trash2 className="size-4" aria-hidden />
                        </button>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
          {podeEditar && (
            <form onSubmit={registrar} className="space-y-3 border-t border-slate-100 pt-4">
              <Input rotulo="Curso" value={curso} maxLength={200} onChange={(e) => setCurso(e.target.value)} />
              <div className="grid grid-cols-2 gap-3">
                <Input rotulo="Carga horária (h)" type="number" min={1} max={2000} value={carga} onChange={(e) => setCarga(e.target.value)} />
                <Input rotulo="Concluído em (opcional)" type="date" value={concluida} onChange={(e) => setConcluida(e.target.value)} />
              </div>
              <Button type="submit" disabled={!curso.trim() || !(Number(carga) >= 1)} carregando={editar.registrar.isPending}>
                Registrar curso
              </Button>
            </form>
          )}
          {erro && (
            <p role="alert" className="text-sm text-red-600">
              {erro instanceof ApiError ? (erro.campos[0]?.mensagem ?? erro.message) : 'Não foi possível salvar.'}
            </p>
          )}
        </div>
      )}
    </Modal>
  )
}
