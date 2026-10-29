package br.undb.semear.controller.pessoa;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.pessoa.UsuarioDtos.AlterarAtivoRequest;
import br.undb.semear.dto.pessoa.UsuarioDtos.AlterarPerfilRequest;
import br.undb.semear.dto.pessoa.UsuarioDtos.ContaResponse;
import br.undb.semear.dto.pessoa.UsuarioDtos.FuncionarioSemLoginResponse;
import br.undb.semear.dto.pessoa.UsuarioDtos.NovoAcessoRequest;
import br.undb.semear.dto.pessoa.UsuarioDtos.SenhaTemporariaResponse;
import br.undb.semear.service.pessoa.ContaUsuarioService;
import jakarta.validation.Valid;

/** Logins do sistema. Por padrão só a administração (módulo USUARIOS). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final ContaUsuarioService contas;

    public UsuarioController(ContaUsuarioService contas) {
        this.contas = contas;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('USUARIOS', 'LER')")
    public List<ContaResponse> listar() {
        return contas.listar();
    }

    @GetMapping("/funcionarios-sem-login")
    @PreAuthorize("@permissoes.pode('USUARIOS', 'GRAVAR')")
    public List<FuncionarioSemLoginResponse> funcionariosSemLogin() {
        return contas.funcionariosSemLogin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('USUARIOS', 'GRAVAR')")
    public SenhaTemporariaResponse criar(@Valid @RequestBody NovoAcessoRequest pedido) {
        return contas.criarParaFuncionario(pedido.funcionarioId(), pedido.email(), pedido.perfil());
    }

    @PatchMapping("/{id}/perfil")
    @PreAuthorize("@permissoes.pode('USUARIOS', 'GRAVAR')")
    public ContaResponse alterarPerfil(@PathVariable UUID id, @Valid @RequestBody AlterarPerfilRequest pedido) {
        return contas.alterarPerfil(id, pedido.perfil());
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("@permissoes.pode('USUARIOS', 'GRAVAR')")
    public ContaResponse alterarAtivo(@PathVariable UUID id, @Valid @RequestBody AlterarAtivoRequest pedido) {
        return contas.alterarAtivo(id, pedido.ativo());
    }

    @PostMapping("/{id}/senha-temporaria")
    @PreAuthorize("@permissoes.pode('USUARIOS', 'GRAVAR')")
    public SenhaTemporariaResponse senhaTemporaria(@PathVariable UUID id) {
        return contas.gerarSenhaTemporaria(id);
    }
}
