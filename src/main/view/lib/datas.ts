/** Data local no formato ISO (AAAA-MM-DD), sem converter fuso. */
export function isoLocal(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${data.getFullYear()}-${mes}-${dia}`
}

export function hojeIso(): string {
  return isoLocal(new Date())
}

/** "2026-10-28" -> "2026-10" */
export function mesIso(iso: string): string {
  return iso.slice(0, 7)
}

/** Soma meses a um "AAAA-MM" e devolve "AAAA-MM". */
export function somarMeses(mes: string, quantidade: number): string {
  const [ano = 0, m = 1] = mes.split('-').map(Number)
  const d = new Date(ano, m - 1 + quantidade, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/** Último dia de um "AAAA-MM" em ISO. */
export function fimDoMes(mes: string): string {
  const [ano = 0, m = 1] = mes.split('-').map(Number)
  return isoLocal(new Date(ano, m, 0))
}

const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']
const DIAS = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado']

/** "2026-10" -> "outubro de 2026" */
export function nomeDoMes(mes: string): string {
  const [ano = 0, m = 1] = mes.split('-').map(Number)
  return `${MESES[m - 1]} de ${ano}`
}

/** "2026-10-28" -> "quarta, 28/10" */
export function diaDaSemana(iso: string): string {
  const [ano = 0, m = 1, d = 1] = iso.split('-').map(Number)
  const data = new Date(ano, m - 1, d)
  return `${DIAS[data.getDay()]}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}`
}
