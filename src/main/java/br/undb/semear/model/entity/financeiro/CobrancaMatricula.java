package br.undb.semear.model.entity.financeiro;

import br.undb.semear.model.enums.TipoCobranca;

/** Cobrança da matrícula (arras). Pagar ativa a matrícula (RD02). Sem desconto de pontualidade. */
public final class CobrancaMatricula extends Cobranca {

    public CobrancaMatricula(DadosCobranca dados, RegraEncargos regraEncargos) {
        super(dados, regraEncargos);
    }

    @Override
    public TipoCobranca getTipo() {
        return TipoCobranca.MATRICULA;
    }

    @Override
    protected boolean permiteDescontoPontualidade() {
        return false;
    }
}
