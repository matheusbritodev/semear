package br.undb.semear.integration;

import java.util.UUID;

import br.undb.semear.model.valueobject.Email;

/**
 * Onde ficam as contas de acesso (hoje o Supabase Auth). O Model não conhece o fornecedor:
 * trocar de provedor é escrever outra implementação.
 */
public interface ProvedorIdentidade {

    /** Cria a conta já confirmada e devolve o id do usuário. Lança ConflitoException se o e-mail já existir. */
    UUID criarContaConfirmada(Email email, String senha);

    /** Troca a senha (senha provisória gerada pela escola). */
    void definirSenha(UUID usuarioId, String senha);

    /** Apaga a conta (usado para desfazer uma criação que falhou no banco da escola). */
    void remover(UUID usuarioId);
}
