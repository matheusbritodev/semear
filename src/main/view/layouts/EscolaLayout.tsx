import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { ChevronRight, House, KeyRound, LogOut, Menu, X, type TipoIcone } from '@/components/ui/icones'
import { useAuth } from '@/auth/useAuth'
import { pode } from '@/auth/permissions'
import { Avatar } from '@/components/comuns/Avatar'
import { Logo } from '@/components/comuns/Logo'
import { ROTULO_PERFIL } from '@/models/enums'
import { GRUPOS_MENU, MENU_ESCOLA, type ItemMenu } from './menuEscola'

const dataDeHoje = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

/**
 * Textos do menu recolhível (computador): somem quando o menu vira a faixa de ícones
 * e reaparecem deslizando quando o mouse (ou o foco do teclado) entra nele.
 */
const ESMAECE_RECOLHIDO =
  'whitespace-nowrap opacity-0 -translate-x-1 transition-[opacity,translate] duration-200 group-hover/menu:translate-x-0 group-hover/menu:opacity-100 group-has-focus-visible/menu:translate-x-0 group-has-focus-visible/menu:opacity-100 motion-reduce:transition-none'

/** Item do menu: a barrinha verde à esquerda e o fundo marcam a página atual. */
function LinkMenu({
  rota,
  rotulo,
  icone: Icone,
  fim = false,
  classeRotulo,
}: {
  rota: string
  rotulo: string
  icone: TipoIcone
  fim?: boolean
  classeRotulo: string
}) {
  return (
    <NavLink
      to={rota}
      end={fim}
      className={({ isActive }) =>
        `group relative flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors duration-200 ${isActive ? 'bg-semear-50 text-semear-800' : 'text-slate-600 hover:bg-slate-100/70 hover:text-slate-900'}`
      }
    >
      {({ isActive }) => (
        <>
          <span
            aria-hidden
            className={`absolute top-1/2 left-0 h-5 w-1 -translate-y-1/2 rounded-r-full bg-semear-600 transition-transform duration-300 ${isActive ? 'scale-y-100' : 'scale-y-0'}`}
          />
          <Icone
            className={`size-5 shrink-0 transition-colors ${isActive ? 'text-semear-600' : 'text-slate-400 group-hover:text-slate-600'}`}
            aria-hidden
          />
          <span className={classeRotulo}>{rotulo}</span>
        </>
      )}
    </NavLink>
  )
}

/** Item do menu que corresponde à página aberta (o de rota mais longa que bate com o endereço). */
function itemAtual(caminho: string, itens: ItemMenu[]) {
  return itens
    .filter((item) => item.rota && (caminho === item.rota || caminho.startsWith(`${item.rota}/`)))
    .sort((a, b) => b.rota!.length - a.rota!.length)[0]
}

/**
 * Área da escola (ADMIN, SECRETARIA, FINANCEIRO, PROFESSOR).
 * O menu é montado pelas permissões do perfil, em seções; módulos ainda não implementados aparecem como "em breve".
 * No computador o menu fica recolhido numa faixa de ícones e se abre por cima do conteúdo ao passar o mouse;
 * no celular ele abre como gaveta pelo botão do topo.
 */
export function EscolaLayout() {
  const { sessao, sair } = useAuth()
  const local = useLocation()
  const [menuAberto, setMenuAberto] = useState(false)
  const itens = MENU_ESCOLA.filter((item) => pode(sessao, item.modulo, 'LER'))
  const atual = itemAtual(local.pathname, itens)

  useEffect(() => setMenuAberto(false), [local.pathname])

  function navegacao(recolhivel: boolean) {
    const classeTexto = recolhivel ? ESMAECE_RECOLHIDO : ''
    return (
      <nav className="flex-1 space-y-5 overflow-x-hidden overflow-y-auto px-3 py-4" aria-label="Menu principal">
        <LinkMenu rota="/escola" rotulo="Início" icone={House} fim classeRotulo={classeTexto} />
        {GRUPOS_MENU.map((grupo) => {
          const doGrupo = itens.filter((item) => item.grupo === grupo)
          if (doGrupo.length === 0) return null
          return (
            <div key={grupo} className="space-y-0.5">
              <p className={`px-3 pb-1.5 text-[11px] font-semibold tracking-wider text-slate-400 uppercase ${classeTexto}`}>{grupo}</p>
              {doGrupo.map((item) =>
                item.rota ? (
                  <LinkMenu key={item.rotulo} rota={item.rota} rotulo={item.rotulo} icone={item.icone} classeRotulo={classeTexto} />
                ) : (
                  <span
                    key={item.rotulo}
                    className="flex items-center justify-between rounded-xl px-3 py-2 text-sm text-slate-400"
                    title="Disponível nas próximas etapas"
                  >
                    <span className="flex items-center gap-3">
                      <item.icone className="size-5 shrink-0" aria-hidden /> <span className={classeTexto}>{item.rotulo}</span>
                    </span>
                    <span className={`rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium ${classeTexto}`}>em breve</span>
                  </span>
                ),
              )}
            </div>
          )
        })}
      </nav>
    )
  }

  function usuario(recolhivel: boolean) {
    const classeTexto = recolhivel ? ESMAECE_RECOLHIDO : ''
    return (
      <div className="border-t border-slate-200/80 p-2">
        <div className="flex items-center gap-3 rounded-xl p-2">
          <Avatar nome={sessao?.nome ?? sessao?.email} />
          <div className={`min-w-0 flex-1 leading-tight ${classeTexto}`}>
            <p className="truncate text-sm font-semibold text-slate-800">{sessao?.nome ?? sessao?.email}</p>
            {sessao?.perfil && <p className="truncate text-xs text-slate-500">{ROTULO_PERFIL[sessao.perfil]}</p>}
          </div>
          <div className={`flex items-center ${classeTexto}`}>
            <Link
              to="/redefinir-senha"
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
              title="Trocar senha"
              aria-label="Trocar senha"
            >
              <KeyRound className="size-4" aria-hidden />
            </Link>
            <button
              type="button"
              onClick={() => void sair()}
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-600"
              title="Sair"
              aria-label="Sair"
            >
              <LogOut className="size-4" aria-hidden />
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-full">
      {/* Computador: a faixa reserva o espaço dos ícones; o menu aberto passa por cima do conteúdo */}
      <div className="hidden w-[68px] shrink-0 md:block print:hidden">
        <aside
          className="group/menu fixed inset-y-0 left-0 z-40 flex w-[68px] flex-col overflow-hidden border-r border-slate-200/80 bg-white transition-[width,box-shadow] delay-200 duration-300 ease-[cubic-bezier(0.2,0.7,0.2,1)] has-focus-visible:w-68 has-focus-visible:shadow-2xl has-focus-visible:shadow-slate-900/15 hover:w-68 hover:shadow-2xl hover:shadow-slate-900/15 hover:delay-75 motion-reduce:transition-none"
        >
          <div className="flex h-16 shrink-0 items-center px-[18px]">
            <Logo classeNome={ESMAECE_RECOLHIDO} />
          </div>
          {navegacao(true)}
          {usuario(true)}
        </aside>
      </div>

      {menuAberto && (
        <div className="fixed inset-0 z-40 md:hidden">
          <button
            className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px] motion-safe:animate-esmaecer"
            aria-label="Fechar menu"
            onClick={() => setMenuAberto(false)}
          />
          <aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-white shadow-2xl motion-safe:animate-gaveta">
            <div className="flex h-16 items-center justify-between px-5">
              <Logo />
              <button
                className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100"
                onClick={() => setMenuAberto(false)}
                aria-label="Fechar menu"
              >
                <X className="size-5" aria-hidden />
              </button>
            </div>
            {navegacao(false)}
            {usuario(false)}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-slate-200/80 bg-white/80 px-4 backdrop-blur-md md:px-8 print:hidden">
          <button
            className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 md:hidden"
            onClick={() => setMenuAberto(true)}
            aria-label="Abrir menu"
          >
            <Menu className="size-5" aria-hidden />
          </button>
          <nav aria-label="Você está em" className="flex min-w-0 items-center gap-1.5 text-sm">
            {atual ? (
              <>
                <span className="hidden text-slate-400 sm:inline">{atual.grupo}</span>
                <ChevronRight className="hidden size-3.5 text-slate-300 sm:inline" aria-hidden />
                <span className="truncate font-medium text-slate-800">{atual.rotulo}</span>
              </>
            ) : (
              <span className="font-medium text-slate-800">Início</span>
            )}
          </nav>
          <p className="ml-auto hidden text-sm text-slate-500 first-letter:uppercase lg:block">{dataDeHoje.format(new Date())}</p>
          <Avatar nome={sessao?.nome ?? sessao?.email} className="ml-auto md:hidden" />
        </header>

        <main className="flex-1 p-4 md:p-8 print:p-0">
          {/* key: cada troca de página reanima a entrada do conteúdo */}
          <div key={local.pathname} className="motion-safe:animate-entrar">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
