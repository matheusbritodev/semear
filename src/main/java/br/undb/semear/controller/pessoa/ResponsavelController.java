package br.undb.semear.controller.pessoa;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.AtualizarContatoRequest;
import br.undb.semear.dto.pessoa.ResponsavelDtos.ResponsavelDetalheResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.ResponsavelResumoResponse;
import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.service.pessoa.ResponsavelService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/responsaveis")
public class ResponsavelController {

    private final ResponsavelService responsaveis;

    public ResponsavelController(ResponsavelService responsaveis) {
        this.responsaveis = responsaveis;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('ALUNOS', 'LER')")
    public PaginaResponse<ResponsavelResumoResponse> pesquisar(@RequestParam(required = false) String busca,
                                                               @RequestParam(defaultValue = "0") int pagina,
                                                               @RequestParam(defaultValue = "20") int tamanho) {
        return responsaveis.pesquisar(busca, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('ALUNOS', 'LER')")
    public ResponsavelDetalheResponse detalhar(@PathVariable UUID id) {
        return paraResposta(responsaveis.buscar(id));
    }

    @PutMapping("/{id}/contato")
    @PreAuthorize("@permissoes.pode('ALUNOS', 'GRAVAR')")
    public ResponsavelDetalheResponse atualizarContato(@PathVariable UUID id,
                                                       @Valid @RequestBody AtualizarContatoRequest pedido) {
        return paraResposta(responsaveis.atualizarContato(id, pedido.telefone(), pedido.email()));
    }

    private ResponsavelDetalheResponse paraResposta(Responsavel r) {
        return ResponsavelDetalheResponse.de(r, responsaveis.alunos(r.getId()));
    }
}
