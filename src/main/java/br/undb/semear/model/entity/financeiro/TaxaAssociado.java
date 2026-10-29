package br.undb.semear.model.entity.financeiro;

import br.undb.semear.model.enums.TipoCobranca;

/** Cobrança mensal da Educação Infantil (RN01). A tabela de valores não prevê desconto de pontualidade (P04). */
public final class TaxaAssociado extends Cobranca {

    public TaxaAssociado(DadosCobranca dados, RegraEncargos regraEncargos) {
        super(dados, regraEncargos);
    }

    @Override
    public TipoCobranca getTipo() {
        return TipoCobranca.TAXA_ASSOCIADO;
    }

    @Override
    protected boolean permiteDescontoPontualidade() {
        return false;
    }
}
