/** Formatação só para exibição. Para a API, envie sempre os valores crus (dígitos, ISO). */

export function somenteDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

/** 00000000191 -> 000.000.001-91 */
export function formatarCpf(valor: string): string {
  const d = somenteDigitos(valor).slice(0, 11)
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2')
}

/** "375.00" ou 375 -> "R$ 375,00" */
export function formatarMoeda(valor: string | number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
    .format(Number(valor))
    .replace(/ /g, ' ')
}

/** "2027-02-10" -> "10/02/2027" (sem converter fuso: datas de calendário) */
export function formatarData(isoData: string): string {
  const [ano, mes, dia] = isoData.slice(0, 10).split('-')
  return ano && mes && dia ? `${dia}/${mes}/${ano}` : isoData
}

/** 98999991234 -> (98) 99999-1234 */
export function formatarTelefone(valor: string): string {
  const d = somenteDigitos(valor).slice(0, 11)
  if (d.length < 10) return d
  const corte = d.length - 4
  return `(${d.slice(0, 2)}) ${d.slice(2, corte)}-${d.slice(corte)}`
}
