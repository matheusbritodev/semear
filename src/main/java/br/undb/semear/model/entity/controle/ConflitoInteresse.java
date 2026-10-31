package br.undb.semear.model.entity.controle;

import java.util.Collection;

import br.undb.semear.model.exception.AcessoNegadoException;

/**
 * Conflito de interesse: o funcionário que também é responsável pelo aluno (mesmo CPF) não decide
 * nada que mexa no dinheiro da própria família — conceder ou aprovar bolsa, dar baixa em cobrança.
 * Outra pessoa da equipe precisa fazer.
 */
public final class ConflitoInteresse {

    private ConflitoInteresse() {
    }

    /** Sem CPF do agente (login sem cadastro de funcionário) não há como comparar: não acusa conflito. */
    public static boolean existe(String cpfDoAgente, Collection<String> cpfsDaFamilia) {
        String agente = somenteDigitos(cpfDoAgente);
        return !agente.isEmpty() && cpfsDaFamilia.stream().map(ConflitoInteresse::somenteDigitos).anyMatch(agente::equals);
    }

    /** @param acao o que se queria fazer, para a mensagem (ex.: "a baixa desta cobrança"). */
    public static void exigirAusente(String cpfDoAgente, Collection<String> cpfsDaFamilia, String acao) {
        if (existe(cpfDoAgente, cpfsDaFamilia)) {
            throw new AcessoNegadoException("CONFLITO_DE_INTERESSE",
                    "Você é responsável por este aluno: " + acao + " precisa ser feita por outra pessoa da equipe");
        }
    }

    private static String somenteDigitos(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("[^0-9]", "");
    }
}
