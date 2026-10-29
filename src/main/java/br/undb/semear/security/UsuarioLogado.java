package br.undb.semear.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.service.pessoa.UsuarioService;

/**
 * Dá acesso ao usuário da requisição atual, a partir do JWT do Supabase
 * (claim "sub" = auth.users.id = usuario.id).
 */
@Component
public class UsuarioLogado {

    private final UsuarioService usuarioService;

    public UsuarioLogado(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public UUID id() {
        return token().map(t -> UUID.fromString(t.getName()))
                .orElseThrow(() -> new AcessoNegadoException("NAO_AUTENTICADO", "Faça login para continuar"));
    }

    public Optional<String> email() {
        return token().map(t -> t.getToken().getClaimAsString("email"));
    }

    /** Usuário cadastrado e ativo; vazio se só existir no Supabase Auth ou estiver desativado. */
    public Optional<Usuario> usuario() {
        return token().flatMap(t -> usuarioService.buscar(UUID.fromString(t.getName())))
                .filter(Usuario::isAtivo);
    }

    /** Usuário cadastrado e ativo, ou 403. */
    public Usuario exigir() {
        return usuario().orElseThrow(() -> new AcessoNegadoException(
                "CADASTRO_INCOMPLETO", "Seu acesso ainda não foi liberado pela escola"));
    }

    private static Optional<JwtAuthenticationToken> token() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao instanceof JwtAuthenticationToken jwt ? Optional.of(jwt) : Optional.empty();
    }
}
