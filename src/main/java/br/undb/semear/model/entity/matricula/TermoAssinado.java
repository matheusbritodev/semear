package br.undb.semear.model.entity.matricula;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Texto do contrato congelado no momento da assinatura. O hash SHA-256 do texto prova que o termo
 * exibido depois é exatamente o que foi aceito, mesmo que a tabela de preços mude.
 */
public record TermoAssinado(String titulo, List<Clausula> clausulas, String hash) {

    public TermoAssinado {
        Objects.requireNonNull(titulo, "titulo");
        clausulas = List.copyOf(clausulas);
        Objects.requireNonNull(hash, "hash");
    }

    public static TermoAssinado de(String titulo, List<Clausula> clausulas) {
        return new TermoAssinado(titulo, clausulas, calcularHash(titulo, clausulas));
    }

    /** Confere se o texto guardado não foi alterado depois da assinatura. */
    public boolean integro() {
        return hash.equals(calcularHash(titulo, clausulas));
    }

    static String calcularHash(String titulo, List<Clausula> clausulas) {
        StringBuilder texto = new StringBuilder(titulo);
        for (Clausula c : clausulas) {
            texto.append('\n').append(c.titulo()).append(": ").append(c.texto());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
