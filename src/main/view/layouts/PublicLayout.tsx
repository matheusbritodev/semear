import { Outlet } from 'react-router'
import { Logo } from '@/components/comuns/Logo'

/** Telas sem login: entrar, criar conta, recuperar senha. */
export function PublicLayout() {
  return (
    <div className="grid min-h-full lg:grid-cols-2">
      <aside className="hidden flex-col justify-between bg-semear-700 p-10 text-white lg:flex">
        <Logo claro />
        <div className="space-y-3">
          <p className="text-3xl font-semibold leading-tight">Cuidar de cada etapa do crescimento.</p>
          <p className="text-semear-100">
            Matrículas, cobranças e o dia a dia pedagógico do Centro Educacional Semeando em um só lugar.
          </p>
        </div>
        <p className="text-sm text-semear-200">Centro Educacional Semeando · Maternal ao 5º ano</p>
      </aside>
      <main className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <Logo />
          </div>
          <Outlet />
        </div>
      </main>
    </div>
  )
}
