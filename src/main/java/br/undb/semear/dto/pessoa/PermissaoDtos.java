package br.undb.semear.dto.pessoa;

import java.util.Arrays;
import java.util.List;

import br.undb.semear.model.entity.pessoa.MatrizPermissoes;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import jakarta.validation.constraints.NotNull;

/** Tela de perfis de acesso (/api/permissoes). */
public final class PermissaoDtos {

    private PermissaoDtos() {
    }

    public record CelulaResponse(Modulo modulo, boolean ler, boolean gravar) {
    }

    public record PerfilResponse(PerfilUsuario perfil, List<CelulaResponse> permissoes) {
    }

    public record MatrizResponse(List<Modulo> modulos, List<PerfilResponse> perfis) {

        public static MatrizResponse de(MatrizPermissoes matriz) {
            List<PerfilResponse> perfis = Arrays.stream(PerfilUsuario.values())
                    .map(p -> new PerfilResponse(p, matriz.doPerfil(p).stream()
                            .map(c -> new CelulaResponse(c.modulo(), c.podeLer(), c.podeGravar()))
                            .toList()))
                    .toList();
            return new MatrizResponse(List.of(Modulo.values()), perfis);
        }
    }

    public record AlterarPermissaoRequest(@NotNull Boolean ler, @NotNull Boolean gravar) {
    }
}
