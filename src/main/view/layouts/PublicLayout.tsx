import { Link, Outlet, useLocation } from 'react-router'
import { AlternarTema } from '@/components/comuns/AlternarTema'
import { Logo } from '@/components/comuns/Logo'
import { PainelEscola } from '@/components/publico/PainelEscola'
import { useAplicarTema } from '@/tema/useTema'

/** Telas sem login: entrar, criar conta, recuperar senha. */
export function PublicLayout() {
  useAplicarTema('publico')
  const local = useLocation()

  return (
    <div className="grid min-h-full lg:grid-cols-[1.05fr_1fr]">
      <PainelEscola />

      <main className="relative flex flex-col overflow-hidden px-4 py-6 sm:px-8">
        {/* Brilhos suaves ao fundo do formulário */}
        <div aria-hidden className="pointer-events-none absolute inset-0">
          <div className="absolute -top-32 -right-24 size-96 rounded-full bg-semear-200/40 blur-3xl dark:bg-semear-700/20" />
          <div className="absolute -bottom-40 -left-24 size-96 rounded-full bg-semear-100/60 blur-3xl dark:bg-semear-900/30" />
        </div>

        <div className="relative flex justify-end">
          <AlternarTema />
        </div>

        <div className="relative flex flex-1 flex-col items-center justify-center py-8">
          <div className="w-full max-w-md">
            <Link
              to="/login"
              className="mx-auto mb-8 block w-fit rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-semear-500 motion-safe:animate-surgir"
              aria-label="Semear — ir para o login"
            >
              <Logo tamanho="grande" />
            </Link>

            {/* key: o cartão reanima ao trocar de tela (login → cadastro → recuperar senha) */}
            <div
              key={local.pathname}
              className="rounded-3xl border border-slate-200/80 bg-white/85 p-6 shadow-xl shadow-semear-900/5 backdrop-blur-xl motion-safe:animate-surgir sm:p-8 dark:border-slate-800 dark:bg-slate-900/75 dark:shadow-black/30"
              style={{ animationDelay: '80ms' }}
            >
              <Outlet />
            </div>
          </div>
        </div>

        <p className="relative text-center text-xs text-slate-400 dark:text-slate-500">
          © {new Date().getFullYear()} Centro Educacional Semeando · Sistema Semear
        </p>
      </main>
    </div>
  )
}
