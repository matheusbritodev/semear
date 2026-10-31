package br.undb.semear.controller.relatorio;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.relatorio.RelatorioDtos.RelatorioAlunosResponse;
import br.undb.semear.service.relatorio.RelatorioService;

/** Emissão de relatórios com os dados cadastrados. A View imprime (PDF) e exporta planilha (CSV). */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    /** Alunos matriculados no ano (opcional: uma turma), com totais por situação e por turma. */
    @GetMapping("/alunos")
    @PreAuthorize("@permissoes.pode('ALUNOS', 'LER')")
    public RelatorioAlunosResponse alunos(@RequestParam(required = false) Integer ano,
                                          @RequestParam(required = false) UUID turmaId) {
        return relatorios.alunos(ano, turmaId);
    }
}
