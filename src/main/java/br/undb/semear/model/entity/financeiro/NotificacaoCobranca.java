package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;
import java.util.UUID;

import br.undb.semear.model.valueobject.Dinheiro;

/**
 * Aviso de cobrança da fila notificacao_cobranca (RF05). O banco cria as linhas; a aplicação envia.
 * {@code tipo} e {@code canal} ficam como texto: espelham tipo_notificacao e canal_notificacao.
 */
public record NotificacaoCobranca(UUID id, UUID cobrancaId, String tipo, String canal, String responsavel,
                                  String email, String telefone, String aluno, LocalDate vencimento,
                                  Dinheiro valorFinal) {

    public boolean ehInadimplencia() {
        return "INADIMPLENCIA".equals(tipo);
    }

    /** Sem dados pessoais (LGPD): só para log. */
    @Override
    public String toString() {
        return "NotificacaoCobranca[" + id + ", " + tipo + ", " + canal + "]";
    }
}
