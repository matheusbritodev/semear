import { Badge, type CorBadge } from '@/components/ui/Badge'
import { ROTULO_STATUS_MATRICULA, type StatusMatricula } from '@/models/enums'

const COR: Record<StatusMatricula, CorBadge> = {
  AGUARDANDO_PAGAMENTO: 'amarelo',
  ATIVA: 'verde',
  CANCELADA: 'vermelho',
  ENCERRADA: 'cinza',
}

export function StatusMatriculaBadge({ status }: { status: StatusMatricula }) {
  return <Badge cor={COR[status]}>{ROTULO_STATUS_MATRICULA[status]}</Badge>
}
