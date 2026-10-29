package br.undb.semear.repository.pessoa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Permissao;
import br.undb.semear.repository.base.Repository;

public interface UsuarioRepository extends Repository<Usuario, UUID> {

    /** Nome da pessoa ligada ao login (funcionário, responsável ou aluno), se houver. */
    Optional<String> buscarNomeExibicao(UUID usuarioId);

    /** Linhas da matriz perfil_permissao para um perfil. */
    List<Permissao> listarPermissoes(PerfilUsuario perfil);
}
