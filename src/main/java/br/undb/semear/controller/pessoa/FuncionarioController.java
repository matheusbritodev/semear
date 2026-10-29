package br.undb.semear.controller.pessoa;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.pessoa.FuncionarioDtos.AlterarAtivoRequest;
import br.undb.semear.dto.pessoa.FuncionarioDtos.AtualizarFuncionarioRequest;
import br.undb.semear.dto.pessoa.FuncionarioDtos.FuncionarioResponse;
import br.undb.semear.dto.pessoa.FuncionarioDtos.NovoFuncionarioRequest;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.service.pessoa.FuncionarioService;
import jakarta.validation.Valid;

/** RF07: funcionários. Módulo RH (na matriz, só ADMIN). */
@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarios;

    public FuncionarioController(FuncionarioService funcionarios) {
        this.funcionarios = funcionarios;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('RH', 'LER')")
    public List<FuncionarioResponse> pesquisar(@RequestParam(required = false) String busca,
                                               @RequestParam(required = false) TipoFuncionario tipo) {
        return funcionarios.pesquisar(busca, tipo).stream().map(FuncionarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('RH', 'LER')")
    public FuncionarioResponse detalhar(@PathVariable UUID id) {
        return FuncionarioResponse.de(funcionarios.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('RH', 'GRAVAR')")
    public FuncionarioResponse criar(@Valid @RequestBody NovoFuncionarioRequest pedido) {
        return FuncionarioResponse.de(funcionarios.criar(pedido.nome(), pedido.cpf(), pedido.tipo(),
                pedido.telefone(), pedido.email(), pedido.dataAdmissao()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissoes.pode('RH', 'GRAVAR')")
    public FuncionarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarFuncionarioRequest pedido) {
        return FuncionarioResponse.de(funcionarios.atualizar(id, pedido.nome(), pedido.telefone(), pedido.email(),
                pedido.dataAdmissao()));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("@permissoes.pode('RH', 'GRAVAR')")
    public FuncionarioResponse alterarAtivo(@PathVariable UUID id, @Valid @RequestBody AlterarAtivoRequest pedido) {
        return FuncionarioResponse.de(funcionarios.alterarAtivo(id, pedido.ativo()));
    }
}
