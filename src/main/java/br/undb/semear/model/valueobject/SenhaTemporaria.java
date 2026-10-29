package br.undb.semear.model.valueobject;

import java.security.SecureRandom;

/**
 * Senha provisória que a escola repassa ao usuário (não há servidor de e-mail — decisão D10).
 * 12 caracteres, sem letras e números que se confundem (0/O, 1/l/I), sempre com letra e número.
 */
public final class SenhaTemporaria {

    private static final String LETRAS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String NUMEROS = "23456789";
    private static final String TODOS = LETRAS + NUMEROS;
    private static final int TAMANHO = 12;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SenhaTemporaria() {
    }

    public static String gerar() {
        char[] senha = new char[TAMANHO];
        for (int i = 0; i < TAMANHO; i++) {
            senha[i] = TODOS.charAt(ALEATORIO.nextInt(TODOS.length()));
        }
        // Garante pelo menos uma letra e um número em posições sorteadas diferentes.
        int posLetra = ALEATORIO.nextInt(TAMANHO);
        int posNumero = (posLetra + 1 + ALEATORIO.nextInt(TAMANHO - 1)) % TAMANHO;
        senha[posLetra] = LETRAS.charAt(ALEATORIO.nextInt(LETRAS.length()));
        senha[posNumero] = NUMEROS.charAt(ALEATORIO.nextInt(NUMEROS.length()));
        return new String(senha);
    }
}
