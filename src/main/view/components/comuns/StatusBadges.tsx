import { Badge, type CorBadge } from '@/components/ui/Badge'
import {
  ROTULO_STATUS_COBRANCA,
  ROTULO_STATUS_CONTRATO,
  ROTULO_STATUS_PRE_MATRICULA,
  type StatusCobranca,
  type StatusContrato,
  type StatusPreMatricula,
} from '@/models/enums'

const COR_PRE: Record<StatusPreMatricula, CorBadge> = { PENDENTE: 'amarelo', APROVADA: 'verde', REJEITADA: 'vermelho' }
const COR_CONTRATO: Record<StatusContrato, CorBadge> = { PENDENTE: 'amarelo', ASSINADO: 'verde', CANCELADO: 'cinza' }
const COR_COBRANCA: Record<StatusCobranca, CorBadge> = { PENDENTE: 'amarelo', PAGO: 'verde', VENCIDO: 'vermelho', CANCELADO: 'cinza' }

export function StatusPreMatriculaBadge({ status }: { status: StatusPreMatricula }) {
  return <Badge cor={COR_PRE[status]}>{ROTULO_STATUS_PRE_MATRICULA[status]}</Badge>
}

export function StatusContratoBadge({ status }: { status: StatusContrato }) {
  return <Badge cor={COR_CONTRATO[status]}>{ROTULO_STATUS_CONTRATO[status]}</Badge>
}

export function StatusCobrancaBadge({ status }: { status: StatusCobranca }) {
  return <Badge cor={COR_COBRANCA[status]}>{ROTULO_STATUS_COBRANCA[status]}</Badge>
}
