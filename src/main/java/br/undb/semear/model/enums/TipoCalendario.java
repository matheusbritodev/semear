package br.undb.semear.model.enums;

/** Espelha o enum tipo_calendario do banco. Cada tipo sabe se há aula no dia. */
public enum TipoCalendario {

    AULA("Dia letivo especial", true),
    FERIADO("Feriado", false),
    RECESSO("Recesso", false),
    EVENTO("Evento", true);

    private final String rotulo;
    private final boolean temAula;

    TipoCalendario(String rotulo, boolean temAula) {
        this.rotulo = rotulo;
        this.temAula = temAula;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean temAula() {
        return temAula;
    }
}
