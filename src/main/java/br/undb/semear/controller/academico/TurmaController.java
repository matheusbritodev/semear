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
}
