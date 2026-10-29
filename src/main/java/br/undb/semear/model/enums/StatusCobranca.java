package br.undb.semear.model.enums;

/** Espelha o enum status_cobranca do banco. PAGO é imutável (RD05). */
public enum StatusCobranca {
    PENDENTE,
    PAGO,
    VENCIDO,
    CANCELADO;

    public boolean emAberto() {
        return this == PENDENTE || this == VENCIDO;
    }
}
