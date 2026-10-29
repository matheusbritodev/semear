package br.undb.semear.security;

import org.springframework.stereotype.Component;

import br.undb.semear.model.enums.Acao;
import br.undb.semear.model.enums.Modulo;

/**
 * Checagem de permissão usada nos Controllers:
 * {@code @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")}.
 */
@Component("permissoes")
public class Permissoes {

    private final UsuarioLogado usuarioLogado;

    public Permissoes(UsuarioLogado usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }

    public boolean pode(String modulo, String acao) {
        Modulo m = Modulo.valueOf(modulo);
        Acao a = Acao.valueOf(acao);
        return usuarioLogado.usuario().map(u -> u.pode(m, a)).orElse(false);
    }
}
