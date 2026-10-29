package br.undb.semear.dto.publico;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ContaDtos {

    private ContaDtos() {
    }

    public record CriarContaRequest(@NotBlank @Size(max = 255) String email, @NotBlank @Size(max = 72) String senha) {

        /** A senha nunca vai para log. */
        @Override
        public String toString() {
            return "CriarContaRequest[***]";
        }
    }

    public record ContaCriadaResponse(UUID usuarioId) {
    }
}
