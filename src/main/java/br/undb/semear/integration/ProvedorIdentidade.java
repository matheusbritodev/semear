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
}
