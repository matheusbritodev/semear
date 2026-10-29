package br.undb.semear.controller.pessoa;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.pessoa.PermissaoDtos.AlterarPermissaoRequest;
import br.undb.semear.dto.pessoa.PermissaoDtos.MatrizResponse;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.service.pessoa.PermissaoService;
import jakarta.validation.Valid;

/** Perfis de acesso: o gestor liga/desliga o que cada perfil consulta ou altera. */
@RestController
@RequestMapping("/api/permissoes")
public class PermissaoController {

    private final PermissaoService permissoes;

    public PermissaoController(PermissaoService permissoes) {
        this.permissoes = permissoes;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'LER')")
    public MatrizResponse matriz() {
        return MatrizResponse.de(permissoes.matriz());
    }

    @PutMapping("/{perfil}/{modulo}")
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public MatrizResponse alterar(@PathVariable PerfilUsuario perfil, @PathVariable Modulo modulo,
                                  @Valid @RequestBody AlterarPermissaoRequest pedido) {
        return MatrizResponse.de(permissoes.alterar(perfil, modulo, pedido.ler(), pedido.gravar()));
    }

    @PostMapping("/restaurar-padrao")
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public MatrizResponse restaurarPadrao() {
        return MatrizResponse.de(permissoes.restaurarPadrao());
    }
}
