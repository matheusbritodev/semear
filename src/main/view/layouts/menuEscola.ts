import {
  CalendarCog,
  Backpack,
  BookOpen,
  Briefcase,
  ClipboardList,
  FileWarning,
  FileText,
  GraduationCap,
  HandCoins,
  Inbox,
  Landmark,
  School,
  Settings,
  UserCog,
  Users,
  type LucideIcon,
} from 'lucide-react'
import type { Modulo } from '@/models/enums'

export interface ItemMenu {
  rotulo: string
  modulo: Modulo
  icone: LucideIcon
  /** Sem rota = ainda não implementado ("em breve"). */
  rota?: string
}

/** Menu da Área da escola. Cada item aparece se o perfil puder LER o módulo. */
export const MENU_ESCOLA: ItemMenu[] = [
  { rotulo: 'Alunos', modulo: 'ALUNOS', icone: GraduationCap, rota: '/escola/alunos' },
  { rotulo: 'Responsáveis', modulo: 'ALUNOS', icone: Users, rota: '/escola/responsaveis' },
  { rotulo: 'Turmas', modulo: 'TURMAS', icone: School, rota: '/escola/turmas' },
  { rotulo: 'Funcionários', modulo: 'RH', icone: Briefcase, rota: '/escola/rh/funcionarios' },
  { rotulo: 'Pré-matrículas', modulo: 'MATRICULA', icone: Inbox, rota: '/escola/pre-matriculas' },
  { rotulo: 'Matrículas', modulo: 'MATRICULA', icone: ClipboardList, rota: '/escola/matriculas' },
  { rotulo: 'Pendências', modulo: 'PENDENCIAS', icone: FileWarning, rota: '/escola/pendencias' },
  { rotulo: 'Listas de materiais', modulo: 'MATRICULA', icone: Backpack, rota: '/escola/config/materiais' },
  { rotulo: 'Financeiro', modulo: 'FINANCEIRO', icone: Landmark, rota: '/escola/financeiro/cobrancas' },
  { rotulo: 'Bolsas', modulo: 'BOLSAS', icone: HandCoins, rota: '/escola/financeiro/bolsas' },
  { rotulo: 'Pedagógico', modulo: 'PEDAGOGICO', icone: BookOpen, rota: '/escola/pedagogico' },
  { rotulo: 'Obrigações', modulo: 'OBRIGACOES', icone: FileText, rota: '/escola/obrigacoes' },
  { rotulo: 'Usuários', modulo: 'USUARIOS', icone: UserCog, rota: '/escola/config/usuarios' },
  { rotulo: 'Ano letivo e preços', modulo: 'CONFIGURACOES', icone: CalendarCog, rota: '/escola/config/ano-letivo' },
  { rotulo: 'Perfis de acesso', modulo: 'CONFIGURACOES', icone: Settings, rota: '/escola/config/perfis' },
]
