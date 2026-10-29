package br.undb.semear.controller.academico;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import br.undb.semear.dto.academico.AcademicoDtos.NovaTurmaRequest;
import br.undb.semear.dto.academico.AcademicoDtos.OpcoesProfessoresResponse;
import br.undb.semear.dto.academico.AcademicoDtos.ProfessorDaTurmaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.VincularProfessorRequest;
import jakarta.validation.Valid;
import br.undb.semear.dto.academico.AcademicoDtos.AlterarCapacidadeRequest;
import br.undb.semear.dto.academico.AcademicoDtos.TurmaDetalheResponse;
import br.undb.semear.dto.academico.AcademicoDtos.TurmaResponse;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.service.academico.TurmaService;
import jakarta.validation.Valid;

/** Turmas e ocupação (RF03 / RN04). Professores veem só as próprias turmas (filtro no service). */
@RestController
@RequestMapping("/api/turmas")
public class TurmaController {

    private final TurmaService turmas;

    public TurmaController(TurmaService turmas) {
        this.turmas = turmas;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('TURMAS', 'LER')")
    public List<TurmaResponse> listar(@RequestParam(required = false) Integer ano) {
        return turmas.listar(ano).stream().map(TurmaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('TURMAS', 'LER')")
    public TurmaDetalheResponse detalhar(@PathVariable UUID id) {
        Turma turma = turmas.buscar(id);
        return new TurmaDetalheResponse(TurmaResponse.de(turma), turmas.alunos(id), turmas.professores(id));
    }

    @PatchMapping("/{id}/capacidade")
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public TurmaResponse alterarCapacidade(@PathVariable UUID id, @Valid @RequestBody AlterarCapacidadeRequest pedido) {
        return TurmaResponse.de(turmas.alterarCapacidade(id, pedido.capacidade()));
    }

    /* ---- Cadastro (configuração do ano) ---- */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public TurmaResponse criar(@Valid @RequestBody NovaTurmaRequest pedido) {
        return TurmaResponse.de(turmas.criar(pedido.ano(), pedido.serieId(), pedido.nome(), pedido.capacidade()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public void remover(@PathVariable UUID id) {
        turmas.remover(id);
    }

    @GetMapping("/opcoes-professores")
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public OpcoesProfessoresResponse opcoesProfessores() {
        return turmas.opcoesProfessores();
    }

    @PostMapping("/{id}/professores")
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public List<ProfessorDaTurmaResponse> vincularProfessor(@PathVariable UUID id, @Valid @RequestBody VincularProfessorRequest pedido) {
        return turmas.vincularProfessor(id, pedido.funcionarioId(), pedido.disciplinaId());
    }

    @DeleteMapping("/{id}/professores/{vinculoId}")
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public List<ProfessorDaTurmaResponse> removerProfessor(@PathVariable UUID id, @PathVariable UUID vinculoId) {
        return turmas.removerProfessor(id, vinculoId);
    }
}
