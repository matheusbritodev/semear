package br.undb.semear.model.enums;

import java.util.Locale;
import java.util.Set;

import br.undb.semear.model.exception.DadoInvalidoException;

/** Espelha o enum tipo_farda do banco: uniforme e educação física são coletados separadamente (RN06). */
public enum TipoFarda {
    UNIFORME,
    EDUCACAO_FISICA;

    /** Tamanhos aceitos: infantis numéricos e adultos por letra. */
    public static final Set<String> TAMANHOS = Set.of("2", "4", "6", "8", "10", "12", "14", "16", "PP", "P", "M", "G", "GG");

    /** Vazio vira null (tamanho não informado); fora da lista lança DadoInvalidoException. */
    public static String validarTamanho(String tamanho, String campo) {
        if (tamanho == null || tamanho.isBlank()) {
            return null;
        }
        String normalizado = tamanho.trim().toUpperCase(Locale.ROOT);
        if (!TAMANHOS.contains(normalizado)) {
            throw new DadoInvalidoException(campo, "Tamanho de farda inválido: " + tamanho);
        }
        return normalizado;
    }
}
