package br.undb.semear.model.entity.financeiro;

import java.util.Objects;

/** Boleto emitido no banco Cora (RF04): identificador e link de pagamento. */
public record ReferenciaCora(String id, String url) {

    public ReferenciaCora {
        Objects.requireNonNull(id, "id");
    }
}
