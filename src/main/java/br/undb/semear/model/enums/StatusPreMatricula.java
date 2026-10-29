package br.undb.semear.model.enums;

/** Espelha o enum status_pre_matricula do banco. A decisão é definitiva (RD04). */
public enum StatusPreMatricula {
    PENDENTE,
    APROVADA,
    REJEITADA;

    public boolean foiDecidida() {
        return this != PENDENTE;
    }
}
