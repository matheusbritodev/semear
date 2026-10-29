package br.undb.semear.model.valueobject;

import java.util.Locale;
import java.util.regex.Pattern;

import br.undb.semear.model.exception.DadoInvalidoException;

/** E-mail válido, sempre em minúsculas (o banco exige: CHECK email = lower(email)). Imutável. */
public final class Email {

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String endereco;

    public Email(String entrada) {
        String normalizado = entrada == null ? "" : entrada.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 255 || !FORMATO.matcher(normalizado).matches()) {
            throw new DadoInvalidoException("email", "E-mail inválido");
        }
        this.endereco = normalizado;
    }

    /** Converte texto opcional: vazio vira null; inválido lança DadoInvalidoException. */
    public static Email opcional(String entrada) {
        return entrada == null || entrada.isBlank() ? null : new Email(entrada);
    }

    public String getEndereco() {
        return endereco;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Email email && endereco.equals(email.endereco);
    }

    @Override
    public int hashCode() {
        return endereco.hashCode();
    }

    @Override
    public String toString() {
        return endereco;
    }
}
