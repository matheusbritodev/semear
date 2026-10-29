package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;

/** Estratégia de acréscimos por atraso (Strategy). Trocar a regra não muda a Cobranca. */
public interface RegraEncargos {

    Encargos calcular(Cobranca cobranca, LocalDate data);
}
