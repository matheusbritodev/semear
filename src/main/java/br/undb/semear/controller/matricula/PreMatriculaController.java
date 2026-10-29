package br.undb.semear.controller.matricula;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.matricula.MatriculaDtos.AprovarPreMatriculaRequest;
import br.undb.semear.dto.matricula.MatriculaDtos.DecisaoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaDetalheResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RejeitarPreMatriculaRequest;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.service.matricula.PreMatriculaService;
import jakarta.validation.Valid;

/** Fila e análise de pré-matrículas pela secretaria (RF01.2). O pedido é feito pelo portal. */
@RestController
@RequestMapping("/api/pre-matriculas")
public class PreMatriculaController {

    private final PreMatriculaService preMatriculas;

    public PreMatriculaController(PreMatriculaService preMatriculas) {
        this.preMatriculas = preMatriculas;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public List<PreMatriculaResumoResponse> listar(@RequestParam(required = false) StatusPreMatricula status,
                                                   @RequestParam(required = false) Integer ano) {
        return preMatriculas.listar(status, ano);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public PreMatriculaDetalheResponse detalhar(@PathVariable UUID id) {
        return preMatriculas.detalhar(id);
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public DecisaoResponse aprovar(@PathVariable UUID id, @Valid @RequestBody AprovarPreMatriculaRequest pedido) {
        UUID matriculaId = preMatriculas.aprovar(id, pedido.turmaId());
        return new DecisaoResponse(id, StatusPreMatricula.APROVADA, matriculaId);
    }

    @PostMapping("/{id}/rejeitar")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public DecisaoResponse rejeitar(@PathVariable UUID id, @Valid @RequestBody RejeitarPreMatriculaRequest pedido) {
        preMatriculas.rejeitar(id, pedido.motivo());
        return new DecisaoResponse(id, StatusPreMatricula.REJEITADA, null);
    }
}
