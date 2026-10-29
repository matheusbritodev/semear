package br.undb.semear.dto.pessoa;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.AdministracaoContas;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.enums.TipoFuncionario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Gestão de logins (módulo USUARIOS). */
public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /** {@code eu}: é o login de quem está vendo (não pode alterar a si mesmo). */
    public record ContaResponse(UUID id, String email, PerfilUsuario perfil, boolean ativo, OffsetDateTime ultimoLogin,
                                OffsetDateTime criadoEm, AdministracaoContas.Vinculo vinculo, String nome,
                                TipoFuncionario tipoFuncionario, boolean eu) {
    }

    /** A senha provisória aparece uma única vez, para o gestor repassar. */
    public record SenhaTemporariaResponse(ContaResponse conta, String senhaTemporaria) {
    }

    public record FuncionarioSemLoginResponse(UUID id, String nome, TipoFuncionario tipo, String email) {
    }

    public record NovoAcessoRequest(@NotNull UUID funcionarioId, @NotBlank @Size(max = 255) String email,
                                    @NotNull PerfilUsuario perfil) {
    }

    public record AlterarPerfilRequest(@NotNull PerfilUsuario perfil) {
    }

    public record AlterarAtivoRequest(@NotNull Boolean ativo) {
    }
}
