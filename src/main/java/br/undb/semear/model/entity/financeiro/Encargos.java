package br.undb.semear.model.entity.financeiro;

import br.undb.semear.model.valueobject.Dinheiro;

/** Acréscimos por atraso (RN09). */
public record Encargos(Dinheiro multa, Dinheiro juros, long diasDeAtraso) {

    public static final Encargos NENHUM = new Encargos(Dinheiro.ZERO, Dinheiro.ZERO, 0);

    public Dinheiro total() {
        return multa.somar(juros);
    }
}
