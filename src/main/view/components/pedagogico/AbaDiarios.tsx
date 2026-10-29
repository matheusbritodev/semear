import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Carregando, EstadoErro, EstadoVazio } from '@/components/comuns/Estados'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { useDiarios } from '@/hooks/usePedagogico'
import { diaDaSemana, hojeIso, mesIso, nomeDoMes, somarMeses } from '@/lib/datas'

/** Diários registrados no mês, do mais recente para o mais antigo. */
export function AbaDiarios({ turmaId, aoAbrirDia }: { turmaId: string; aoAbrirDia: (data: string) => void }) {
  const [mes, setMes] = useState(mesIso(hojeIso()))
  const diarios = useDiarios(turmaId, mes)

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Button variante="fantasma" aria-label="Mês anterior" onClick={() => setMes(somarMeses(mes, -1))}>
          <ChevronLeft className="size-4" aria-hidden />
        </Button>
        <span className="min-w-40 text-center text-sm font-medium text-slate-800 capitalize">{nomeDoMes(mes)}</span>
        <Button variante="fantasma" aria-label="Próximo mês" onClick={() => setMes(somarMeses(mes, 1))}>
          <ChevronRight className="size-4" aria-hidden />
        </Button>
      </div>
      {diarios.isPending ? (
        <Carregando />
      ) : diarios.isError ? (
        <EstadoErro erro={diarios.error} aoTentarDeNovo={() => void diarios.refetch()} />
      ) : diarios.data.length === 0 ? (
        <EstadoVazio titulo="Nenhum diário neste mês" />
      ) : (
        <ul className={`space-y-2 ${diarios.isPlaceholderData ? 'opacity-60' : ''}`}>
          {diarios.data.map((d) => (
            <li key={d.id}>
              <button
                type="button"
                onClick={() => aoAbrirDia(d.data)}
                className="w-full rounded-xl border border-slate-200 bg-white p-3 text-left hover:border-semear-200"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm font-medium text-slate-900 capitalize">{diaDaSemana(d.data)}</span>
                  <span className="flex items-center gap-2 text-xs text-slate-500">
                    {d.professor}
                    <Badge cor={d.faltas > 0 ? 'amarelo' : 'verde'}>
                      {d.faltas} {d.faltas === 1 ? 'falta' : 'faltas'}
                    </Badge>
                  </span>
                </div>
                <p className="mt-1 text-sm text-slate-600">{d.conteudo}</p>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
