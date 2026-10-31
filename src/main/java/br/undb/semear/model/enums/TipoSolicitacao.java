package br.undb.semear.model.enums;

/** Espelha o enum tipo_solicitacao: ações que precisam de uma segunda pessoa para valer. */
public enum TipoSolicitacao {
    CONCESSAO_BOLSA("Concessão de bolsa"),
    ALTERACAO_BOLSA("Alteração de desconto de bolsa"),
    ALTERACAO_PRECO("Alteração de preço"),
    COPIA_PRECOS("Preços do ano novo");

    private final String rotulo;

    TipoSolicitacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
