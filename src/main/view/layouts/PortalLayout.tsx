import { NavLink, Outlet } from 'react-router'
import { LogOut } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { Logo } from '@/components/comuns/Logo'
import { Button } from '@/components/ui/Button'

const estiloLink = ({ isActive }: { isActive: boolean }) =>
  `whitespace-nowrap border-b-2 px-3 py-2.5 text-sm font-medium ${isActive ? 'border-semear-600 text-semear-700' : 'border-transparent text-slate-600 hover:text-slate-900'}`

/** Portal do responsável/aluno: navegação simples, pensada para o celular. */
export function PortalLayout() {
  const { sessao, sair } = useAuth()
  const cadastroCompleto = sessao?.cadastroCompleto ?? false

  return (
    <div className="min-h-full">
      <header className="border-b border-slate-200 bg-white print:hidden">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
          <Logo />
          <div className="flex items-center gap-2">
            <span className="hidden text-sm text-slate-600 sm:inline">{sessao?.nome ?? sessao?.email}</span>
            <Button variante="fantasma" onClick={() => void sair()} aria-label="Sair">
              <LogOut className="size-4" aria-hidden /> <span className="hidden sm:inline">Sair</span>
            </Button>
          </div>
        </div>
        {cadastroCompleto && (
          <nav className="mx-auto flex max-w-4xl gap-1 overflow-x-auto px-2" aria-label="Portal">
            <NavLink to="/portal" end className={estiloLink}>
              Início
            </NavLink>
            <NavLink to="/portal/pre-matricula/nova" className={estiloLink}>
              <span className="sm:hidden">Pré-matrícula</span>
              <span className="hidden sm:inline">Nova pré-matrícula</span>
            </NavLink>
            <NavLink to="/portal/financeiro" className={estiloLink}>
              Financeiro
            </NavLink>
            <NavLink to="/portal/rematricula" className={estiloLink}>
              Rematrícula
            </NavLink>
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-4xl px-4 py-6 md:py-10">
        <Outlet />
      </main>
    </div>
  )
}
