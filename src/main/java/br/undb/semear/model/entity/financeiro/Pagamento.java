package br.undb.semear.model.entity.financeiro;

import java.time.OffsetDateTime;
import java.util.Objects;

import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.valueobject.Dinheiro;

/** Baixa de uma cobrança: quando, quanto e por onde (site/Cora ou presencial). */
public record Pagamento(OffsetDateTime pagoEm, Dinheiro valorPago, OrigemPagamento origem) {

    public Pagamento {
        Objects.requireNonNull(pagoEm, "pagoEm");
        Objects.requireNonNull(valorPago, "valorPago");
        Objects.requireNonNull(origem, "origem");
    }
}
