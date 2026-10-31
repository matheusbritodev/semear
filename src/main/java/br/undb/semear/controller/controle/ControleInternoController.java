package br.undb.semear.controller.controle;

import java.time.LocalDate;
import java.util.List;
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
import br.undb.semear.dto.controle.ControleDtos.AutorResponse;
import br.undb.semear.dto.controle.ControleDtos.RegistrarRevisaoRequest;
import br.undb.semear.dto.controle.ControleDtos.RegistroAuditoriaResponse;
import br.undb.semear.dto.controle.ControleDtos.RejeitarRequest;
import br.undb.semear.dto.controle.ControleDtos.RelatorioRevisaoResponse;
import br.undb.semear.dto.controle.ControleDtos.ResumoAprovacoesResponse;
import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.dto.controle.ControleDtos.TabelaAuditadaResponse;
import br.undb.semear.dto.controle.ControleDtos.VerificacaoResponse;
import br.undb.semear.model.enums.StatusSolicitacao;
import br.undb.semear.repository.controle.AuditoriaRepository.Filtro;
import br.undb.semear.service.controle.AprovacaoService;
import br.undb.semear.service.controle.AuditoriaService;
import br.undb.semear.service.controle.RevisaoAcessoService;
import jakarta.validation.Valid;

/** Controles internos (SoD e SOX): aprovações, trilha de auditoria e revisão de acessos. */
@RestController
@RequestMapping("/api")
public class ControleInternoController {

    private final AprovacaoService aprovacoes;
    private final AuditoriaService auditoria;
    private final RevisaoAcessoService revisoes;

    public ControleInternoController(AprovacaoService aprovacoes, AuditoriaService auditoria,
                                     RevisaoAcessoService revisoes) {
        this.aprovacoes = aprovacoes;
        this.auditoria = auditoria;
        this.revisoes = revisoes;
    }

    /* ---- Aprovações: quem pede (bolsas, preços) acompanha; quem tem Aprovações decide ---- */

    @GetMapping("/aprovacoes")
    @PreAuthorize("@permissoes.pode('APROVACOES', 'LER') or @permissoes.pode('BOLSAS', 'GRAVAR') or @permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public List<SolicitacaoResponse> listar(@RequestParam(required = false) StatusSolicitacao status) {
        return aprovacoes.listar(status);
    }

    @GetMapping("/aprovacoes/resumo")
    @PreAuthorize("@permissoes.pode('APROVACOES', 'LER') or @permissoes.pode('BOLSAS', 'GRAVAR') or @permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public ResumoAprovacoesResponse resumo() {
        return aprovacoes.resumo();
    }

    @PostMapping("/aprovacoes/{id}/aprovar")
    @PreAuthorize("@permissoes.pode('APROVACOES', 'GRAVAR')")
    public SolicitacaoResponse aprovar(@PathVariable UUID id) {
        return aprovacoes.aprovar(id);
    }

    @PostMapping("/aprovacoes/{id}/rejeitar")
    @PreAuthorize("@permissoes.pode('APROVACOES', 'GRAVAR')")
    public SolicitacaoResponse rejeitar(@PathVariable UUID id, @Valid @RequestBody RejeitarRequest pedido) {
        return aprovacoes.rejeitar(id, pedido.motivo());
    }

    /** O próprio solicitante desiste do pedido (a regra de "só o solicitante" fica no Model). */
    @PostMapping("/aprovacoes/{id}/cancelar")
    @PreAuthorize("@permissoes.pode('BOLSAS', 'GRAVAR') or @permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public SolicitacaoResponse cancelar(@PathVariable UUID id) {
        return aprovacoes.cancelar(id);
    }

    /* ---- Auditoria ---- */

    @GetMapping("/auditoria")
    @PreAuthorize("@permissoes.pode('AUDITORIA', 'LER')")
    public PaginaResponse<RegistroAuditoriaResponse> auditoria(
            @RequestParam(required = false) String tabela, @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) String registroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "30") int tamanho) {
        String reg = registroId == null || registroId.isBlank() ? null : registroId.trim();
        String tab = tabela == null || tabela.isBlank() ? null : tabela;
        return auditoria.pesquisar(new Filtro(tab, usuarioId, reg, de, ate, Math.max(pagina, 0),
                Math.min(Math.max(tamanho, 1), 100)));
    }

    @GetMapping("/auditoria/autores")
    @PreAuthorize("@permissoes.pode('AUDITORIA', 'LER')")
    public List<AutorResponse> autores() {
        return auditoria.autores();
    }

    @GetMapping("/auditoria/tabelas")
    @PreAuthorize("@permissoes.pode('AUDITORIA', 'LER')")
    public List<TabelaAuditadaResponse> tabelas() {
        return auditoria.tabelasAuditadas();
    }

    @GetMapping("/auditoria/verificacao")
    @PreAuthorize("@permissoes.pode('AUDITORIA', 'LER')")
    public VerificacaoResponse verificar() {
        return auditoria.verificar();
    }

    /* ---- Revisão de acessos ---- */

    @GetMapping("/revisao-acessos")
    @PreAuthorize("@permissoes.pode('USUARIOS', 'LER')")
    public RelatorioRevisaoResponse relatorio() {
        return revisoes.relatorio();
    }

    @PostMapping("/revisao-acessos")
    @PreAuthorize("@permissoes.pode('USUARIOS', 'GRAVAR')")
    public RelatorioRevisaoResponse registrar(@Valid @RequestBody RegistrarRevisaoRequest pedido) {
        return revisoes.registrar(pedido.observacao());
    }
}
