package br.undb.semear.model.enums;

/**
 * Situação de uma obrigação numa data (calculada pelo Model; o banco só guarda PENDENTE/CUMPRIDA/ATRASADA).
 * EM_ALERTA = dentro da antecedência de aviso.
 */
public enum SituacaoObrigacao {
    EM_DIA,
    EM_ALERTA,
    ATRASADA,
    CUMPRIDA
}
