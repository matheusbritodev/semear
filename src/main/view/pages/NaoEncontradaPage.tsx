import { Link } from 'react-router'

export function NaoEncontradaPage() {
  return (
    <div className="mx-auto mt-24 max-w-md space-y-3 px-4 text-center">
      <p className="text-sm font-semibold text-semear-600">404</p>
      <h1 className="text-2xl font-semibold text-slate-900">Página não encontrada</h1>
      <Link to="/" className="inline-block text-sm font-medium text-semear-700 underline">
        Voltar ao início
      </Link>
    </div>
  )
}
