package br.undb.semear.model.entity.pessoa;

import java.util.Objects;

/**
 * Vínculo aluno–responsável (tabela aluno_responsavel). É classe própria porque o
 * parentesco e o papel de responsável financeiro pertencem à relação, não à pessoa. Imutável.
 */
public record VinculoResponsavel(Responsavel responsavel, String parentesco, boolean financeiro) {

    public VinculoResponsavel {
        Objects.requireNonNull(responsavel, "responsavel");
    }

    public VinculoResponsavel comFinanceiro(boolean ehFinanceiro) {
        return new VinculoResponsavel(responsavel, parentesco, ehFinanceiro);
    }
}
