package br.undb.semear.model.enums;

/**
 * Espelha o enum nivel_ensino do banco. Concentra o que muda entre os níveis (RN01, RN07):
 * cada nível sabe qual cobrança mensal e qual documento de formalização usa.
 */
public enum NivelEnsino {

    EDUCACAO_INFANTIL("Educação Infantil", TipoCobranca.TAXA_ASSOCIADO, TipoContrato.FICHA_SOCIO),
    ENSINO_FUNDAMENTAL("Ensino Fundamental", TipoCobranca.MENSALIDADE, TipoContrato.CONTRATO_MATRICULA);

    private final String rotulo;
    private final TipoCobranca cobrancaMensal;
    private final TipoContrato contrato;

    NivelEnsino(String rotulo, TipoCobranca cobrancaMensal, TipoContrato contrato) {
        this.rotulo = rotulo;
        this.cobrancaMensal = cobrancaMensal;
        this.contrato = contrato;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** RN01: Infantil paga Taxa de Associado; Fundamental paga Mensalidade. */
    public TipoCobranca tipoCobrancaMensal() {
        return cobrancaMensal;
    }

    /** RN07: Infantil formaliza com Ficha de Sócio; Fundamental com Contrato. */
    public TipoContrato tipoContrato() {
        return contrato;
    }
}
