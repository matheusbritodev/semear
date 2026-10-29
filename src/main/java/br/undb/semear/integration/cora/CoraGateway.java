package br.undb.semear.integration.cora;

import br.undb.semear.model.entity.financeiro.Cobranca;
import br.undb.semear.model.entity.financeiro.ReferenciaCora;

/**
 * Banco Cora (RF04): emite o boleto de uma cobrança e valida os avisos de pagamento.
 * Hoje só existe a implementação falsa; a real é outra classe com a mesma interface.
 */
public interface CoraGateway {

    ReferenciaCora emitir(Cobranca cobranca, String descricao);

    /** Link que o responsável abre para pagar. */
    String linkPagamento(ReferenciaCora boleto, java.util.UUID cobrancaId);

    /** Confere a assinatura do webhook (HMAC do corpo com o segredo combinado). */
    boolean assinaturaValida(byte[] corpo, String assinatura);

    /** true quando os pagamentos são simulados (permite o botão "simular pagamento" no portal). */
    boolean simulado();
}
