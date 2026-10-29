package br.undb.semear.dto.pessoa;

import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;

/**
 * Dados da sessão para a View montar menus e rotas.
 * cadastroCompleto = false quando o login existe no Supabase Auth mas ainda não na tabela "usuario"
 * (ex.: responsável no primeiro acesso) ou quando o usuário está desativado.
 */
public record MeResponse(UUID id, String email, String nome, PerfilUsuario perfil,
                         boolean cadastroCompleto, List<PermissaoResponse> permissoes) {

    public record PermissaoResponse(Modulo modulo, boolean ler, boolean gravar) {
    }

    public static MeResponse de(Usuario usuario, String nome) {
        List<PermissaoResponse> permissoes = usuario.getPermissoes().stream()
                .map(p -> new PermissaoResponse(p.modulo(), p.podeLer() || p.podeGravar(), p.podeGravar()))
                .toList();
        return new MeResponse(usuario.getId(), usuario.getEmail().getEndereco(), nome,
                usuario.getPerfil(), true, permissoes);
    }

    public static MeResponse semCadastro(UUID id, String email) {
        return new MeResponse(id, email, null, null, false, List.of());
    }
}
