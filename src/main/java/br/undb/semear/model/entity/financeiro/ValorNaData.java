package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;

import br.undb.semear.model.valueobject.Dinheiro;

/**
 * Quanto se paga numa data, com a composição do valor (para a tela explicar ao responsável):
 * total = valorFinal − descontoPontualidade + multa + juros.
 */
public record ValorNaData(LocalDate data, Dinheiro valorFinal, Dinheiro descontoPontualidade, Encargos encargos,
                          Dinheiro total) {

    static ValorNaData calcular(LocalDate data, Dinheiro valorFinal, Dinheiro descontoPontualidade, Encargos encargos) {
        return new ValorNaData(data, valorFinal, descontoPontualidade, encargos,
                valorFinal.subtrair(descontoPontualidade).somar(encargos.total()));
    }

    static ValorNaData jaPago(LocalDate data, Dinheiro valorFinal, Dinheiro valorPago) {
        return new ValorNaData(data, valorFinal, Dinheiro.ZERO, Encargos.NENHUM, valorPago);
    }
}
