package br.undb.semear.model.enums;

/** Espelha o enum orgao_externo do banco (RF09). */
public enum OrgaoExterno {

    CENSO_INEP("Censo Escolar (INEP)"),
    SMTT("SMTT"),
    SISTEMA_PRESENCA("Sistema Presença (MEC)"),
    CORA("Banco Cora");

    private final String rotulo;

    OrgaoExterno(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
