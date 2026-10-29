package br.undb.semear.model.valueobject;

import br.undb.semear.model.exception.DadoInvalidoException;

/** Telefone brasileiro com DDD: 10 (fixo) ou 11 (celular) dígitos. Imutável. */
public final class Telefone {

    private final String digitos;

    public Telefone(String entrada) {
        String somenteDigitos = entrada == null ? "" : entrada.replaceAll("\\D", "");
        if (!somenteDigitos.matches("\\d{10,11}")) {
            throw new DadoInvalidoException("telefone", "Telefone deve ter DDD e 8 ou 9 dígitos");
        }
        this.digitos = somenteDigitos;
    }

    public String getDigitos() {
        return digitos;
    }

    public boolean ehCelular() {
        return digitos.length() == 11;
    }

    /** Formato de exibição: (98) 99999-9999 ou (98) 3333-3333. */
    public String formatado() {
        return formatar(digitos);
    }

    /** Formata sem validar (para exibir dados já gravados); valores fora do padrão voltam como estão. */
    public static String formatar(String digitos) {
        if (digitos == null || !digitos.matches("\\d{10,11}")) {
            return digitos;
        }
        String ddd = digitos.substring(0, 2);
        String numero = digitos.substring(2);
        int corte = numero.length() - 4;
        return "(" + ddd + ") " + numero.substring(0, corte) + "-" + numero.substring(corte);
    }

    /** Converte texto opcional: vazio vira null; inválido lança DadoInvalidoException. */
    public static Telefone opcional(String entrada) {
        return entrada == null || entrada.isBlank() ? null : new Telefone(entrada);
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Telefone telefone && digitos.equals(telefone.digitos);
    }

    @Override
    public int hashCode() {
        return digitos.hashCode();
    }

    @Override
    public String toString() {
        return formatado();
    }
}
