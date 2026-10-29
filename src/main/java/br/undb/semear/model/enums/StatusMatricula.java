package br.undb.semear.model.enums;

import java.util.Set;

/** Espelha o enum status_matricula do banco, com as transições válidas (padrão State via enum). */
public enum StatusMatricula {
    AGUARDANDO_PAGAMENTO,
    ATIVA,
    CANCELADA,
    ENCERRADA;

    public boolean podeIrPara(StatusMatricula destino) {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> Set.of(ATIVA, CANCELADA).contains(destino);
            case ATIVA -> Set.of(CANCELADA, ENCERRADA).contains(destino);
            case CANCELADA, ENCERRADA -> false;
        };
    }

    /** Ocupa vaga na turma (RN04). */
    public boolean ocupaVaga() {
        return this == AGUARDANDO_PAGAMENTO || this == ATIVA;
    }
}
