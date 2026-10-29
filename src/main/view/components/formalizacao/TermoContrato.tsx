import type { ReactNode } from 'react'
import { Printer } from 'lucide-react'
import { Logo } from '@/components/comuns/Logo'
import { StatusContratoBadge } from '@/components/comuns/StatusBadges'
import { Button } from '@/components/ui/Button'
import { formatarData } from '@/lib/formatters'
import type { ContratoTermo } from '@/models/formalizacao'

/**
 * Texto do Contrato de Matrícula ou da Ficha de Sócio, pronto para ler e imprimir (Ctrl+P / salvar em PDF).
 * O rodapé (assinar ou registrar assinatura) muda entre escola e portal.
 */
export function TermoContrato({ termo, rodape }: { termo: ContratoTermo; rodape?: ReactNode }) {
  return (
    <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs md:p-8 print:border-0 print:p-0 print:shadow-none">
      <header className="mb-6 flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 pb-4">
        <div className="space-y-2">
          <Logo />
          <h1 className="text-xl font-semibold text-slate-900">
            {termo.titulo} · {termo.ano}
          </h1>
          <p className="text-sm text-slate-500">
            {termo.aluno} · {termo.serie} {termo.turma}
          </p>
        </div>
        <div className="flex items-center gap-2 print:hidden">
          <StatusContratoBadge status={termo.status} />
          <Button variante="secundario" onClick={() => window.print()}>
            <Printer className="size-4" aria-hidden /> Imprimir
          </Button>
        </div>
      </header>

      <ol className="space-y-4">
        {termo.clausulas.map((c, i) => (
          <li key={c.titulo} className="text-sm leading-relaxed text-slate-700">
            <h2 className="font-semibold text-slate-900">
              Cláusula {i + 1}ª · {c.titulo}
            </h2>
            <p>{c.texto}</p>
          </li>
        ))}
      </ol>

      <p className="mt-6 text-xs text-slate-400">
        Versão resumida para o protótipo acadêmico, com os valores da tabela de preços da escola. O texto integral é o contrato oficial do Centro
        Educacional Semeando.
      </p>

      {termo.status === 'ASSINADO' && (
        <p className="mt-6 rounded-lg bg-semear-50 px-4 py-3 text-sm text-semear-800">
          Assinado por <strong>{termo.assinadoPor}</strong>
          {termo.assinadoEm && <> em {formatarData(termo.assinadoEm)}</>}.
        </p>
      )}
      {rodape && <div className="mt-6 print:hidden">{rodape}</div>}
    </article>
  )
}
