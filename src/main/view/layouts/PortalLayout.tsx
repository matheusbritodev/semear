import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { CalendarCheck, ClipboardText, House, KeyRound, LogOut, Wallet, type TipoIcone } from '@/components/ui/icones'
import { useAuth } from '@/auth/useAuth'
import { AlternarTema } from '@/components/comuns/AlternarTema'
import { Avatar } from '@/components/comuns/Avatar'
import { Logo } from '@/components/comuns/Logo'

const ABAS: { rota: string; rotulo: string; curto?: string; icone: TipoIcone; fim?: boolean }[] = [
  { rota: '/portal', rotulo: 'Início', icone: House, fim: true },
  { rota: '/portal/pre-matricula/nova', rotulo: 'Nova pré-matrícula', curto: 'Pré-matrícula', icone: ClipboardText },
  { rota: '/portal/financeiro', rotulo: 'Financeiro', icone: Wallet },
  { rota: '/portal/rematricula', rotulo: 'Rematrícula', icone: CalendarCheck },
]

/** Portal do responsável/aluno: navegação simples, pensada para o celular. */
export function PortalLayout() {
  const { sessao, sair } = useAuth()
  const local = useLocation()
  const cadastroCompleto = sessao?.cadastroCompleto ?? false

  return (
    <div className="min-h-full">
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/85 backdrop-blur-md print:hidden">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between gap-3 px-4">
          <Logo />
          <div className="flex items-center gap-1">
            <AlternarTema className="mr-2" />
            <div className="mr-2 hidden items-center gap-2.5 sm:flex">
              <Avatar nome={sessao?.nome ?? sessao?.email} className="size-8" />
              <span className="max-w-48 truncate text-sm font-medium text-slate-700">{sessao?.nome ?? sessao?.email}</span>
            </div>
            <Link
              to="/redefinir-senha"
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
              title="Trocar senha"
              aria-label="Trocar senha"
            >
              <KeyRound className="size-5" aria-hidden />
            </Link>
            <button
              type="button"
              onClick={() => void sair()}
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-600"
              title="Sair"
              aria-label="Sair"
            >
              <LogOut className="size-5" aria-hidden />
            </button>
          </div>
        </div>
        {cadastroCompleto && (
          <nav className="mx-auto flex max-w-5xl gap-1 overflow-x-auto overflow-y-hidden px-2" aria-label="Portal">
            {ABAS.map(({ rota, rotulo, curto, icone: Icone, fim }) => (
              <NavLink
                key={rota}
                to={rota}
                end={fim}
                className={({ isActive }) =>
                  `group relative flex items-center gap-2 px-3 py-3 text-sm font-medium whitespace-nowrap transition-colors duration-200 ${isActive ? 'text-semear-700' : 'text-slate-500 hover:text-slate-900'}`
                }
              >
                {({ isActive }) => (
                  <>
                    <Icone className={`size-[18px] ${isActive ? 'text-semear-600' : 'text-slate-400 group-hover:text-slate-600'}`} aria-hidden />
                    {curto ? (
                      <>
                        <span className="sm:hidden">{curto}</span>
                        <span className="hidden sm:inline">{rotulo}</span>
                      </>
                    ) : (
                      rotulo
                    )}
                    <span
                      aria-hidden
                      className={`absolute inset-x-3 bottom-0 h-0.5 rounded-full bg-semear-600 transition-transform duration-300 ${isActive ? 'scale-x-100' : 'scale-x-0'}`}
                    />
                  </>
                )}
              </NavLink>
            ))}
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6 md:py-10">
        <div key={local.pathname} className="motion-safe:animate-entrar">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
