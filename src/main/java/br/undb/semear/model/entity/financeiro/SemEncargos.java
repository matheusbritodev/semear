package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;

/**
 * Null Object: cobrança que não tem multa nem juros (a de matrícula — se não for paga,
 * a matrícula simplesmente não é ativada, RD02).
 */
public final class SemEncargos implements RegraEncargos {

    public static final SemEncargos INSTANCIA = new SemEncargos();

    private SemEncargos() {
    }

    @Override
    public Encargos calcular(Cobranca cobranca, LocalDate data) {
        return Encargos.NENHUM;
    }
}
