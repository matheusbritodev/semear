package br.undb.semear.controller.academico;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.academico.AcademicoDtos.AlertaResponse;
import br.undb.semear.service.academico.TurmaService;

/** Avisos à secretaria, como "turma lotada" (RF03). */
@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final TurmaService turmas;

    public AlertaController(TurmaService turmas) {
        this.turmas = turmas;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR') or @permissoes.pode('MATRICULA', 'LER')")
    public List<AlertaResponse> listar(@RequestParam(defaultValue = "true") boolean apenasNaoLidos) {
        return turmas.alertas(apenasNaoLidos);
    }

    @PatchMapping("/{id}/lido")
    @PreAuthorize("@permissoes.pode('TURMAS', 'GRAVAR')")
    public ResponseEntity<Void> marcarComoLido(@PathVariable UUID id) {
        turmas.marcarAlertaComoLido(id);
        return ResponseEntity.noContent().build();
    }
}
