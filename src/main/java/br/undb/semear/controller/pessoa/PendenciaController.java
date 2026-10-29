package br.undb.semear.controller.pessoa;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.pessoa.PendenciaDtos.PendenciaDocumentoResponse;
import br.undb.semear.dto.pessoa.PendenciaDtos.ResumoPendenciasResponse;
import br.undb.semear.service.pessoa.PendenciaService;

/** Pendências dos alunos. Por padrão: gestão e secretaria (o responsável vê as dos filhos pelo portal). */
@RestController
@RequestMapping("/api/pendencias")
public class PendenciaController {

    private final PendenciaService pendencias;

    public PendenciaController(PendenciaService pendencias) {
        this.pendencias = pendencias;
    }

    @GetMapping("/documentos")
    @PreAuthorize("@permissoes.pode('PENDENCIAS', 'LER')")
    public List<PendenciaDocumentoResponse> documentos() {
        return pendencias.documentos();
    }

    @GetMapping("/resumo")
    @PreAuthorize("@permissoes.pode('PENDENCIAS', 'LER')")
    public ResumoPendenciasResponse resumo() {
        return pendencias.resumo();
    }
}
