package br.undb.semear.controller.financeiro;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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

import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.AlteracaoBolsaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.BolsaRequest;
import br.undb.semear.dto.financeiro.FinanceiroDtos.BolsaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ConcederBolsaRequest;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ConcessaoResponse;
import br.undb.semear.service.financeiro.BolsaService;
import jakarta.validation.Valid;

/** Bolsas (RF02/RN11): tipos de bolsa e concessão por matrícula. */
@RestController
@RequestMapping("/api")
public class BolsaController {

    private final BolsaService bolsas;

    public BolsaController(BolsaService bolsas) {
        this.bolsas = bolsas;
    }

    @GetMapping("/bolsas")
    @PreAuthorize("@permissoes.pode('BOLSAS', 'LER')")
    public List<BolsaResponse> listar() {
        return bolsas.listar();
    }

    @PostMapping("/bolsas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('BOLSAS', 'GRAVAR')")
    public BolsaResponse criar(@Valid @RequestBody BolsaRequest pedido) {
        return bolsas.criar(pedido);
    }

    /** Mudança de desconto ou reativação volta com "solicitacao": precisa da aprovação de outra pessoa. */
    @PutMapping("/bolsas/{id}")
    @PreAuthorize("@permissoes.pode('BOLSAS', 'GRAVAR')")
    public AlteracaoBolsaResponse alterar(@PathVariable UUID id, @Valid @RequestBody BolsaRequest pedido) {
        return bolsas.alterar(id, pedido);
    }

    @GetMapping("/bolsas/bolsistas")
    @PreAuthorize("@permissoes.pode('BOLSAS', 'LER')")
    public List<ConcessaoResponse> bolsistas(@RequestParam(required = false) Integer ano) {
        return bolsas.bolsistas(ano);
    }

    @GetMapping("/matriculas/{id}/bolsas")
    @PreAuthorize("@permissoes.pode('BOLSAS', 'LER')")
    public List<ConcessaoResponse> daMatricula(@PathVariable UUID id) {
        return bolsas.daMatricula(id);
    }

    /** Pede a bolsa: só vale depois que outra pessoa aprovar (202 + pedido). */
    @PostMapping("/matriculas/{id}/bolsas")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("@permissoes.pode('BOLSAS', 'GRAVAR')")
    public SolicitacaoResponse conceder(@PathVariable UUID id, @Valid @RequestBody ConcederBolsaRequest pedido) {
        return bolsas.solicitarConcessao(id, pedido.bolsaId(), pedido.vigenciaInicio());
    }

    @DeleteMapping("/matriculas/{id}/bolsas/{concessaoId}")
    @PreAuthorize("@permissoes.pode('BOLSAS', 'GRAVAR')")
    public List<ConcessaoResponse> revogar(@PathVariable UUID id, @PathVariable UUID concessaoId) {
        return bolsas.revogar(id, concessaoId);
    }
}
