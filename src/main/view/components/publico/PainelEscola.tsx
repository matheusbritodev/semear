import type { CSSProperties } from 'react'
import {
  Exam,
  Backpack,
  BookOpen,
  Calculator,
  Globe,
  GraduationCap,
  Music,
  Palette,
  Pencil,
  Ruler,
  Shapes,
  Star,
  type TipoIcone,
} from '@/components/ui/icones'

interface ItemFlutuante {
  icone: TipoIcone
  /** Posição e tamanho em classes do Tailwind. */
  classe: string
  /** Inclinação de repouso e atraso da animação, para os itens não flutuarem juntos. */
  giro: number
  atraso: number
}

const ITENS: ItemFlutuante[] = [
  { icone: BookOpen, classe: 'top-[15%] left-[6%] size-14', giro: -8, atraso: 0 },
  { icone: Pencil, classe: 'top-[6%] right-[12%] size-12', giro: 12, atraso: 1.2 },
  { icone: Calculator, classe: 'top-[34%] left-[4%] size-11', giro: 6, atraso: 2.1 },
  { icone: Globe, classe: 'top-[26%] right-[5%] size-14', giro: -6, atraso: 0.6 },
  { icone: Palette, classe: 'top-[55%] left-[7%] size-12', giro: -12, atraso: 1.8 },
  { icone: Ruler, classe: 'top-[52%] right-[8%] size-11', giro: 18, atraso: 2.7 },
  { icone: Music, classe: 'top-[17%] left-[42%] size-10', giro: 8, atraso: 3.2 },
  { icone: Shapes, classe: 'bottom-[30%] left-[30%] size-10', giro: -10, atraso: 2.4 },
  { icone: Star, classe: 'bottom-[32%] right-[28%] size-9', giro: 14, atraso: 0.9 },
  { icone: Backpack, classe: 'bottom-[31%] left-[12%] size-10', giro: 6, atraso: 1.5 },
  { icone: Exam, classe: 'bottom-[30%] right-[12%] size-9', giro: -4, atraso: 3.6 },
]

const MODULOS = ['Matrículas', 'Financeiro', 'Pedagógico', 'Portal da família']

/** Lado verde das telas públicas: ilustração de sala de aula e a mensagem da escola. */
export function PainelEscola() {
  return (
    <aside className="relative m-3 hidden flex-col justify-between overflow-hidden rounded-3xl bg-linear-to-br from-semear-600 via-semear-700 to-semear-900 p-10 text-white shadow-2xl shadow-semear-900/20 lg:flex print:hidden dark:from-semear-800 dark:via-semear-900 dark:to-semear-950 dark:shadow-black/40">
      <Fundo />

      {ITENS.map(({ icone: Icone, classe, giro, atraso }) => (
        <span
          key={classe}
          aria-hidden
          className={`absolute grid place-items-center rounded-2xl bg-white/10 text-white/85 shadow-lg shadow-semear-950/20 ring-1 ring-white/20 backdrop-blur-sm motion-safe:animate-flutuar ${classe}`}
          style={{ '--giro': `${giro}deg`, transform: `rotate(${giro}deg)`, animationDelay: `${atraso}s` } as CSSProperties}
        >
          <Icone className="size-1/2" />
        </span>
      ))}

      <p className="relative inline-flex w-fit items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-sm font-medium ring-1 ring-white/20 backdrop-blur-sm motion-safe:animate-surgir">
        <GraduationCap className="size-4" aria-hidden /> Centro Educacional Semeando
      </p>

      <div className="relative mx-auto w-full max-w-md motion-safe:animate-surgir" style={{ animationDelay: '150ms' }}>
        <Lousa />
      </div>

      <div className="relative space-y-4 motion-safe:animate-surgir" style={{ animationDelay: '300ms' }}>
        <p className="text-3xl leading-tight font-semibold text-balance">Cuidar de cada etapa do crescimento.</p>
        <p className="max-w-md text-semear-100 dark:text-semear-200/90">
          Matrículas, cobranças e o dia a dia pedagógico do Centro Educacional Semeando em um só lugar.
        </p>
        <ul className="flex flex-wrap gap-2" aria-label="Módulos do sistema">
          {MODULOS.map((modulo) => (
            <li key={modulo} className="rounded-full bg-white/10 px-3 py-1 text-xs font-medium text-semear-50 ring-1 ring-white/15">
              {modulo}
            </li>
          ))}
        </ul>
        <p className="pt-2 text-sm text-semear-200 dark:text-semear-300/80">Maternal ao 5º ano · São Luís/MA</p>
      </div>
    </aside>
  )
}

/** Textura de caderno (pontinhos) e manchas de luz ao fundo. */
function Fundo() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0">
      <div
        className="absolute inset-0 opacity-[0.12]"
        style={{ backgroundImage: 'radial-gradient(circle, white 1px, transparent 1.5px)', backgroundSize: '22px 22px' }}
      />
      <div className="absolute -top-24 -left-24 size-80 rounded-full bg-semear-400/30 blur-3xl" />
      <div className="absolute -right-20 -bottom-32 size-96 rounded-full bg-semear-300/20 blur-3xl" />
    </div>
  )
}

/** Lousa com giz, livros, lápis e maçã — desenhada em SVG para ficar nítida em qualquer tamanho. */
function Lousa() {
  const giz = { fill: 'none', stroke: 'white', strokeWidth: 3, strokeLinecap: 'round', strokeLinejoin: 'round' } as const
  return (
    <svg viewBox="0 0 400 290" className="w-full drop-shadow-2xl" role="img" aria-label="Ilustração de uma lousa escolar com livros, lápis e maçã">
      {/* Moldura e quadro */}
      <rect x="20" y="10" width="360" height="200" rx="16" fill="white" fillOpacity="0.18" />
      <rect x="32" y="22" width="336" height="176" rx="10" className="fill-semear-950" fillOpacity="0.85" />

      {/* Escrita a giz */}
      <g opacity="0.92">
        <text x="60" y="88" fill="white" fontSize="44" fontWeight="700" letterSpacing="10" fontFamily="'Segoe Print', 'Comic Sans MS', cursive">
          ABC
        </text>
        <text x="62" y="140" fill="white" fontSize="26" fontFamily="'Segoe Print', 'Comic Sans MS', cursive" opacity="0.85">
          1 + 2 = 3
        </text>
        <path d="M60 160 q 40 -10 80 0 t 80 0" {...giz} strokeWidth={2} opacity="0.5" />
        {/* Brotinho: a semente do Semear */}
        <g transform="translate(290 70)">
          <path d="M0 80 V 30" {...giz} />
          <path d="M0 42 C 0 18 18 4 40 4 C 40 26 22 42 0 42 Z" {...giz} className="fill-semear-400/40" />
          <path d="M0 56 C 0 38 -14 26 -32 26 C -32 44 -18 56 0 56 Z" {...giz} className="fill-semear-400/40" />
          <path d="M-24 80 H 24" {...giz} />
        </g>
        {/* Estrelinhas */}
        <path d="M250 45 l3 7 7 3 -7 3 -3 7 -3 -7 -7 -3 7 -3 z" fill="white" opacity="0.8" />
        <path d="M225 120 l2 5 5 2 -5 2 -2 5 -2 -5 -5 -2 5 -2 z" fill="white" opacity="0.6" />
      </g>

      {/* Bandeja de giz */}
      <rect x="40" y="198" width="320" height="10" rx="5" fill="white" fillOpacity="0.3" />
      <rect x="70" y="192" width="22" height="7" rx="3" fill="white" />
      <rect x="100" y="193" width="14" height="6" rx="3" className="fill-semear-200" />

      {/* Pilha de livros */}
      <g transform="translate(40 222)">
        <rect x="0" y="36" width="140" height="22" rx="4" className="fill-semear-300" />
        <rect x="8" y="40" width="4" height="14" rx="2" fill="white" fillOpacity="0.6" />
        <rect x="12" y="16" width="120" height="22" rx="4" fill="white" fillOpacity="0.9" />
        <rect x="20" y="20" width="4" height="14" rx="2" className="fill-semear-500" />
        <rect x="4" y="-4" width="128" height="22" rx="4" className="fill-semear-500" />
        <rect x="12" y="0" width="4" height="14" rx="2" fill="white" fillOpacity="0.6" />
      </g>

      {/* Maçã verde sobre os livros */}
      <g transform="translate(130 196)">
        <path d="M12 4 C 4 -2 -8 4 -6 16 C -4 28 6 34 12 30 C 18 34 28 28 30 16 C 32 4 20 -2 12 4 Z" className="fill-semear-300" />
        <path d="M12 4 C 12 -2 14 -6 17 -8" stroke="white" strokeWidth="2.5" strokeLinecap="round" fill="none" />
        <path d="M15 -3 C 20 -8 26 -6 27 -3 C 22 0 18 0 15 -3 Z" fill="white" fillOpacity="0.85" />
      </g>

      {/* Lápis */}
      <g transform="translate(222 262) rotate(-12)">
        <rect x="0" y="0" width="110" height="16" rx="3" className="fill-amber-300" />
        <rect x="0" y="0" width="110" height="5" rx="2" className="fill-amber-200" />
        <rect x="-14" y="0" width="16" height="16" rx="3" className="fill-semear-200" />
        <path d="M110 0 L 134 8 L 110 16 Z" fill="#f5deb3" />
        <path d="M126 5 L 134 8 L 126 11 Z" className="fill-slate-700" />
      </g>

      {/* Régua */}
      <g transform="translate(250 226) rotate(4)">
        <rect x="0" y="0" width="120" height="18" rx="3" fill="white" fillOpacity="0.85" />
        {Array.from({ length: 11 }, (_, i) => (
          <rect key={i} x={6 + i * 11} y="0" width="1.5" height={i % 2 ? 6 : 10} className="fill-semear-700" />
        ))}
      </g>
    </svg>
  )
}
