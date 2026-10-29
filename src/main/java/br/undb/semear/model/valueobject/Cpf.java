package br.undb.semear.model.valueobject;

import br.undb.semear.model.exception.DadoInvalidoException;

/**
 * CPF válido (dígitos verificadores conferidos), guardado só com os 11 dígitos. Imutável.
 * toString() mascara o número (LGPD): nunca imprime o CPF completo em log.
 */
public final class Cpf {

    private final String digitos;

    public Cpf(String entrada) {
        String somenteDigitos = entrada == null ? "" : entrada.replaceAll("\\D", "");
        if (!ehValido(somenteDigitos)) {
            throw new DadoInvalidoException("cpf", "CPF inválido");
        }
        this.digitos = somenteDigitos;
    }

    public static boolean ehValido(String digitos) {
        if (digitos == null || !digitos.matches("\\d{11}") || digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
                && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Os 11 dígitos, como o banco guarda (char(11)). */
    public String getDigitos() {
        return digitos;
    }

    /** Formato de exibição: 000.000.000-00. */
    public String formatado() {
        return formatar(digitos);
    }

    /** Formata 11 dígitos sem validar (para exibir dados já gravados); outros valores voltam como estão. */
    public static String formatar(String digitos) {
        if (digitos == null || !digitos.matches("\\d{11}")) {
            return digitos;
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Cpf cpf && digitos.equals(cpf.digitos);
    }

    @Override
    public int hashCode() {
        return digitos.hashCode();
    }

    @Override
    public String toString() {
        return "***.***.***-" + digitos.substring(9);
    }
}
