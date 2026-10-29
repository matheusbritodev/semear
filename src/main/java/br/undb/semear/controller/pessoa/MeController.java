package br.undb.semear.controller.pessoa;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.pessoa.MeResponse;
import br.undb.semear.security.UsuarioLogado;
import br.undb.semear.service.pessoa.UsuarioService;

/** Sessão do usuário logado: perfil e permissões, usados pela View para montar o menu. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UsuarioLogado usuarioLogado;
    private final UsuarioService usuarioService;

    public MeController(UsuarioLogado usuarioLogado, UsuarioService usuarioService) {
        this.usuarioLogado = usuarioLogado;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public MeResponse sessao() {
        UUID id = usuarioLogado.id();
        return usuarioLogado.usuario()
                .map(usuario -> MeResponse.de(usuario, usuarioService.nomeExibicao(id).orElse(null)))
                .orElseGet(() -> MeResponse.semCadastro(id, usuarioLogado.email().orElse(null)));
    }
}
