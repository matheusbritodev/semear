package br.undb.semear.service.pessoa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.repository.pessoa.UsuarioRepository;

/** Casos de uso de acesso: quem é o usuário logado e o que ele pode fazer. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Usuário cadastrado na tabela "usuario" (pode estar inativo). Vazio = só existe no Supabase Auth. */
    public Optional<Usuario> buscar(UUID id) {
        return usuarios.buscarPorId(id);
    }

    public Optional<String> nomeExibicao(UUID id) {
        return usuarios.buscarNomeExibicao(id);
    }
}
