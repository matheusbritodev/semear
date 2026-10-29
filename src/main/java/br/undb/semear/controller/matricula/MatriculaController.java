package br.undb.semear.controller.matricula;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.CancelarMatriculaRequest;
import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaDetalheResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RegistrarPagamentoRequest;
import br.undb.semear.dto.matricula.MatriculaDtos.RematriculaOpcaoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RematricularRequest;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.service.matricula.MatriculaService;
import br.undb.semear.service.matricula.RematriculaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaController {

    private final MatriculaService matriculas;
    private final RematriculaService rematriculas;

    public MatriculaController(MatriculaService matriculas, RematriculaService rematriculas) {
        this.matriculas = matriculas;
        this.rematriculas = rematriculas;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public PaginaResponse<MatriculaResumoResponse> pesquisar(@RequestParam(required = false) String busca,
                                                             @RequestParam(required = false) Integer ano,
                                                             @RequestParam(required = false) StatusMatricula status,
                                                             @RequestParam(defaultValue = "0") int pagina,
                                                             @RequestParam(defaultValue = "20") int tamanho) {
        return matriculas.pesquisar(busca, ano, status, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public MatriculaDetalheResponse detalhar(@PathVariable UUID id) {
        return matriculas.detalhar(id);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public MatriculaDetalheResponse cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarMatriculaRequest pedido) {
        return matriculas.cancelar(id, pedido.motivo());
    }

    /** Baixa da cobrança de matrícula (ex.: pagamento na secretaria). Módulo financeiro. */
    @PostMapping("/{id}/pagamento-matricula")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'GRAVAR')")
    public MatriculaDetalheResponse registrarPagamento(@PathVariable UUID id,
                                                       @Valid @RequestBody RegistrarPagamentoRequest pedido) {
        return matriculas.registrarPagamentoMatricula(id, pedido.origem());
    }

    /** Situação de rematrícula de um aluno para o próximo ano (RF01.5, RN13). */
    @GetMapping("/rematricula/opcoes")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public RematriculaOpcaoResponse opcaoRematricula(@RequestParam UUID alunoId) {
        return rematriculas.opcao(alunoId);
    }

    @PostMapping("/rematricula")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public MatriculaDetalheResponse rematricular(@Valid @RequestBody RematricularRequest pedido) {
        UUID id = rematriculas.rematricular(pedido.alunoId(), pedido.fardaUniforme(), pedido.fardaEdFisica());
        return matriculas.detalhar(id);
    }
}
