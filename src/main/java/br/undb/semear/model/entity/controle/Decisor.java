package br.undb.semear.model.entity.controle;

import java.util.Objects;
import java.util.UUID;

/**
 * Quem está decidindo um pedido: o login, se o perfil tem o módulo Aprovações e o CPF do cadastro
 * de funcionário (para o conflito de interesse; pode ser nulo).
 */
public record Decisor(UUID usuarioId, boolean podeAprovar, String cpf) {

    public Decisor {
        Objects.requireNonNull(usuarioId, "usuarioId");
    }
}
