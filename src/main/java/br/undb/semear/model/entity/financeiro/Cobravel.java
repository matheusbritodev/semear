package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;

import br.undb.semear.model.valueobject.Dinheiro;

/** Tudo que pode ser cobrado do responsável: tem vencimento e um valor a pagar que depende da data. */
public interface Cobravel {

    LocalDate getVencimento();

    Dinheiro valorAPagar(LocalDate data);
}
