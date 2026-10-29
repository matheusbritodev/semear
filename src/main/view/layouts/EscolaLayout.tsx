import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { House, LogOut, Menu, X } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { pode } from '@/auth/permissions'
import { Logo } from '@/components/comuns/Logo'
import { Button } from '@/components/ui/Button'
import { ROTULO_PERFIL } from '@/models/enums'
import { MENU_ESCOLA } from './menuEscola'

const estiloLink = ({ isActive }: { isActive: boolean }) =>
  `flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-semear-50 text-semear-700' : 'text-slate-600 hover:bg-slate-50'}`

/**
 * Área da escola (ADMIN, SECRETARIA, FINANCEIRO, PROFESSOR).
 * O menu é montado pelas permissões do perfil; módulos ainda não implementados aparecem como "em breve".
 */
export function EscolaLayout() {
  const { sessao, sair } = useAuth()
  const local = useLocation()
  const [menuAberto, setMenuAberto] = useState(false)
  const itens = MENU_ESCOLA.filter((item) => pode(sessao, item.modulo, 'LER'))

  useEffect(() => setMenuAberto(false), [local.pathname])

  const navegacao = (
    <nav className="flex-1 space-y-1 overflow-y-auto p-3" aria-label="Menu principal">
      <NavLink to="/escola" end className={estiloLink}>
        <House className="size-4" aria-hidden /> Início
      </NavLink>
      <p className="px-3 pt-4 pb-1 text-xs font-semibold tracking-wide text-slate-400 uppercase">Módulos</p>
      {itens.map((item) =>
        item.rota ? (
          <NavLink key={item.rotulo} to={item.rota} className={estiloLink}>
            <item.icone className="size-4" aria-hidden /> {item.rotulo}
          </NavLink>
        ) : (
          <span
            key={item.rotulo}
            className="flex items-center justify-between rounded-lg px-3 py-2 text-sm text-slate-400"
            title="Disponível nas próximas etapas"
          >
            <span className="flex items-center gap-2">
              <item.icone className="size-4" aria-hidden /> {item.rotulo}
            </span>
            <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium">em breve</span>
          </span>
        ),
      )}
    </nav>
  )

  return (
    <div className="flex min-h-full">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-slate-200 bg-white md:flex print:hidden">
        <div className="border-b border-slate-200 p-4">
          <Logo />
        </div>
        {navegacao}
      </aside>

      {menuAberto && (
        <div className="fixed inset-0 z-40 md:hidden">
          <button className="absolute inset-0 bg-slate-900/40" aria-label="Fechar menu" onClick={() => setMenuAberto(false)} />
          <aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-200 p-4">
              <Logo />
              <button className="rounded p-1 text-slate-500 hover:bg-slate-100" onClick={() => setMenuAberto(false)} aria-label="Fechar menu">
                <X className="size-5" aria-hidden />
              </button>
            </div>
            {navegacao}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3 md:px-6 print:hidden">
          <button className="rounded p-1 text-slate-600 hover:bg-slate-100 md:hidden" onClick={() => setMenuAberto(true)} aria-label="Abrir menu">
            <Menu className="size-5" aria-hidden />
          </button>
          <div className="ml-auto flex items-center gap-3">
            <div className="text-right leading-tight">
              <p className="text-sm font-medium text-slate-800">{sessao?.nome ?? sessao?.email}</p>
              {sessao?.perfil && <p className="text-xs text-slate-500">{ROTULO_PERFIL[sessao.perfil]}</p>}
            </div>
            <Link to="/redefinir-senha" className="hidden text-sm text-slate-500 hover:text-slate-800 sm:inline" title="Trocar a senha">
              Trocar senha
            </Link>
            <Button variante="fantasma" onClick={() => void sair()} aria-label="Sair">
              <LogOut className="size-4" aria-hidden /> <span className="hidden sm:inline">Sair</span>
            </Button>
          </div>
        </header>
        <main className="flex-1 p-4 md:p-8 print:p-0">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
