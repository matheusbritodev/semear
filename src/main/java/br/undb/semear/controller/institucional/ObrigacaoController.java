package br.undb.semear.controller.institucional;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.undb.semear.controller.matricula.DocumentoController;
import br.undb.semear.dto.institucional.ObrigacaoDtos.FuncionarioOpcaoResponse;
import br.undb.semear.dto.institucional.ObrigacaoDtos.ObrigacaoRequest;
import br.undb.semear.dto.institucional.ObrigacaoDtos.ObrigacaoResponse;
import br.undb.semear.dto.matricula.DocumentoDtos.LinkArquivoResponse;
import br.undb.semear.service.institucional.ObrigacaoService;
import jakarta.validation.Valid;

/** Obrigações institucionais (RF09). Por padrão: gestão e secretaria. */
@RestController
@RequestMapping("/api/obrigacoes")
public class ObrigacaoController {

    private final ObrigacaoService obrigacoes;

    public ObrigacaoController(ObrigacaoService obrigacoes) {
        this.obrigacoes = obrigacoes;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'LER')")
    public List<ObrigacaoResponse> listar() {
        return obrigacoes.listar();
    }

    @GetMapping("/alertas")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'LER')")
    public List<ObrigacaoResponse> alertas() {
        return obrigacoes.alertas();
    }

    @GetMapping("/responsaveis")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'LER')")
    public List<FuncionarioOpcaoResponse> responsaveis() {
        return obrigacoes.responsaveisPossiveis();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'LER')")
    public ObrigacaoResponse detalhar(@PathVariable UUID id) {
        return obrigacoes.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'GRAVAR')")
    public ObrigacaoResponse criar(@Valid @RequestBody ObrigacaoRequest pedido) {
        return obrigacoes.criar(pedido);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'GRAVAR')")
    public ObrigacaoResponse alterar(@PathVariable UUID id, @Valid @RequestBody ObrigacaoRequest pedido) {
        return obrigacoes.alterar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'GRAVAR')")
    public void remover(@PathVariable UUID id) {
        obrigacoes.remover(id);
    }

    @PostMapping("/{id}/cumprir")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'GRAVAR')")
    public ObrigacaoResponse cumprir(@PathVariable UUID id) {
        return obrigacoes.cumprir(id);
    }

    @PostMapping(path = "/{id}/comprovantes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'GRAVAR')")
    public ObrigacaoResponse enviarComprovante(@PathVariable UUID id, @RequestParam MultipartFile arquivo) {
        return obrigacoes.enviarComprovante(id, DocumentoController.lerBytes(arquivo));
    }

    @GetMapping("/comprovantes/{comprovanteId}/link")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'LER')")
    public LinkArquivoResponse link(@PathVariable UUID comprovanteId) {
        return obrigacoes.link(comprovanteId);
    }

    @DeleteMapping("/comprovantes/{comprovanteId}")
    @PreAuthorize("@permissoes.pode('OBRIGACOES', 'GRAVAR')")
    public ObrigacaoResponse removerComprovante(@PathVariable UUID comprovanteId) {
        return obrigacoes.removerComprovante(comprovanteId);
    }
}
