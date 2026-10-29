package br.undb.semear.model.enums;

/** Espelha o enum tipo_plano do banco (RF08.2). */
public enum TipoPlano {

    PLANO_AULA("Plano de aula"),
    PLANEJAMENTO_DIARIO("Planejamento diário");

    private final String rotulo;

    TipoPlano(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
