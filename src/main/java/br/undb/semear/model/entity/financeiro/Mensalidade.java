package br.undb.semear.model.entity.financeiro;

import br.undb.semear.model.enums.TipoCobranca;

/** Cobrança mensal do Ensino Fundamental (RN01). Tem desconto de 10% até o vencimento (RN08). */
public final class Mensalidade extends Cobranca {

    public Mensalidade(DadosCobranca dados, RegraEncargos regraEncargos) {
        super(dados, regraEncargos);
    }

    @Override
    public TipoCobranca getTipo() {
        return TipoCobranca.MENSALIDADE;
    }

    @Override
    protected boolean permiteDescontoPontualidade() {
        return true;
    }
}
