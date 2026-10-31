import {
  Backpack,
  BookOpen,
  Briefcase,
  CalendarDots,
  Chalkboard,
  ClipboardText,
  Printer,
  FileText,
  FileWarning,
  HandCoins,
  Landmark,
  ShieldCheck,
  Stamp,
  Student,
  Tray,
  UserCheck,
  UserGear,
  UsersThree,
  type TipoIcone,
} from '@/components/ui/icones'
import type { Modulo } from '@/models/enums'

export type GrupoMenu = 'Pessoas' | 'Matrícula' | 'Financeiro' | 'Pedagógico' | 'Administração'

export interface ItemMenu {
  rotulo: string
  modulo: Modulo
  icone: TipoIcone
  grupo: GrupoMenu
  /** Sem rota = ainda não implementado ("em breve"). */
  rota?: string
}

/** Ordem das seções no menu lateral. */
export const GRUPOS_MENU: GrupoMenu[] = ['Pessoas', 'Matrícula', 'Financeiro', 'Pedagógico', 'Administração']

/** Menu da Área da escola. Cada item aparece se o perfil puder LER o módulo. */
export const MENU_ESCOLA: ItemMenu[] = [
  { rotulo: 'Alunos', modulo: 'ALUNOS', icone: Student, grupo: 'Pessoas', rota: '/escola/alunos' },
  { rotulo: 'Relatório de alunos', modulo: 'ALUNOS', icone: Printer, grupo: 'Pessoas', rota: '/escola/relatorios' },
  { rotulo: 'Responsáveis', modulo: 'ALUNOS', icone: UsersThree, grupo: 'Pessoas', rota: '/escola/responsaveis' },
  { rotulo: 'Funcionários', modulo: 'RH', icone: Briefcase, grupo: 'Pessoas', rota: '/escola/rh/funcionarios' },
  { rotulo: 'Pré-matrículas', modulo: 'MATRICULA', icone: Tray, grupo: 'Matrícula', rota: '/escola/pre-matriculas' },
  { rotulo: 'Matrículas', modulo: 'MATRICULA', icone: ClipboardText, grupo: 'Matrícula', rota: '/escola/matriculas' },
  { rotulo: 'Pendências', modulo: 'PENDENCIAS', icone: FileWarning, grupo: 'Matrícula', rota: '/escola/pendencias' },
  { rotulo: 'Listas de materiais', modulo: 'MATRICULA', icone: Backpack, grupo: 'Matrícula', rota: '/escola/config/materiais' },
  { rotulo: 'Financeiro', modulo: 'FINANCEIRO', icone: Landmark, grupo: 'Financeiro', rota: '/escola/financeiro/cobrancas' },
  { rotulo: 'Bolsas', modulo: 'BOLSAS', icone: HandCoins, grupo: 'Financeiro', rota: '/escola/financeiro/bolsas' },
  { rotulo: 'Aprovações', modulo: 'APROVACOES', icone: Stamp, grupo: 'Financeiro', rota: '/escola/aprovacoes' },
  { rotulo: 'Turmas', modulo: 'TURMAS', icone: Chalkboard, grupo: 'Pedagógico', rota: '/escola/turmas' },
  { rotulo: 'Pedagógico', modulo: 'PEDAGOGICO', icone: BookOpen, grupo: 'Pedagógico', rota: '/escola/pedagogico' },
  { rotulo: 'Obrigações', modulo: 'OBRIGACOES', icone: FileText, grupo: 'Administração', rota: '/escola/obrigacoes' },
  { rotulo: 'Usuários', modulo: 'USUARIOS', icone: UserGear, grupo: 'Administração', rota: '/escola/config/usuarios' },
  { rotulo: 'Ano letivo e preços', modulo: 'CONFIGURACOES', icone: CalendarDots, grupo: 'Administração', rota: '/escola/config/ano-letivo' },
  { rotulo: 'Perfis de acesso', modulo: 'CONFIGURACOES', icone: ShieldCheck, grupo: 'Administração', rota: '/escola/config/perfis' },
  { rotulo: 'Revisão de acessos', modulo: 'USUARIOS', icone: UserCheck, grupo: 'Administração', rota: '/escola/config/revisao-acessos' },
  { rotulo: 'Auditoria', modulo: 'AUDITORIA', icone: ShieldCheck, grupo: 'Administração', rota: '/escola/auditoria' },
]
