package br.undb.semear.controller.financeiro;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaDetalheResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.GeracaoResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.GerarCobrancasRequest;
import br.undb.semear.dto.financeiro.FinanceiroDtos.InadimplenciaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.PagamentoRequest;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ResumoAnoResponse;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.repository.financeiro.CobrancaRepository.Filtro;
import br.undb.semear.service.financeiro.CobrancaService;
import jakarta.validation.Valid;

/** Cobranças, inadimplência e resumo (RF04/RF05). Por padrão: gestão e financeiro. */
@RestController
@RequestMapping("/api")
public class CobrancaController {

    private final CobrancaService cobrancas;

    public CobrancaController(CobrancaService cobrancas) {
        this.cobrancas = cobrancas;
    }

    @GetMapping("/cobrancas")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'LER')")
    public PaginaResponse<CobrancaResponse> pesquisar(@RequestParam(required = false) Integer ano,
                                                      @RequestParam(required = false) Integer mes,
                                                      @RequestParam(required = false) TipoCobranca tipo,
                                                      @RequestParam(required = false) StatusCobranca status,
                                                      @RequestParam(required = false) String busca,
                                                      @RequestParam(required = false) UUID matriculaId,
                                                      @RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "20") int tamanho) {
        return cobrancas.pesquisar(new Filtro(ano, mes, tipo, status, busca, matriculaId, null, Math.max(pagina, 0),
                Math.clamp(tamanho, 1, 100)));
    }

    /** Detalhe com a composição do valor numa data (padrão: hoje). */
    @GetMapping("/cobrancas/{id}")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'LER')")
    public CobrancaDetalheResponse detalhar(@PathVariable UUID id,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return cobrancas.detalhar(id, data);
    }

    @PostMapping("/cobrancas/{id}/pagamento")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'GRAVAR')")
    public CobrancaDetalheResponse registrarPagamento(@PathVariable UUID id, @Valid @RequestBody PagamentoRequest pedido) {
        return cobrancas.registrarPagamento(id, pedido.origem());
    }

    @PostMapping("/cobrancas/geracao")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'GRAVAR')")
    public GeracaoResponse gerar(@Valid @RequestBody GerarCobrancasRequest pedido) {
        return cobrancas.gerar(pedido.ano(), pedido.mesInicio(), pedido.mesFim());
    }

    @GetMapping("/financeiro/inadimplencia")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'LER')")
    public InadimplenciaResponse inadimplencia() {
        return cobrancas.inadimplencia();
    }

    @GetMapping("/financeiro/resumo")
    @PreAuthorize("@permissoes.pode('FINANCEIRO', 'LER')")
    public ResumoAnoResponse resumo(@RequestParam(required = false) Integer ano) {
        return cobrancas.resumo(ano);
    }
}
