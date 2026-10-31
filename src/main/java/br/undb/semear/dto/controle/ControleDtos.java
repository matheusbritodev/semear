package br.undb.semear.dto.controle;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.controle.AlteracaoCampo;
import br.undb.semear.model.entity.controle.PoliticaSegregacao;
import br.undb.semear.model.entity.controle.RevisaoAcesso;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.enums.StatusSolicitacao;
import br.undb.semear.model.enums.TipoSolicitacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Controles internos: aprovações, auditoria, revisão de acessos e regras de SoD. */
public final class ControleDtos {

    private ControleDtos() {
    }

    /* ---- Aprovações ---- */

    public record SolicitacaoResponse(UUID id, TipoSolicitacao tipo, String tipoRotulo, StatusSolicitacao status,
                                      String resumo, UUID matriculaId, String solicitante, OffsetDateTime solicitadaEm,
                                      String decisor, OffsetDateTime decididaEm, String motivo, boolean minha,
                                      boolean podeDecidir) {
    }

    public record ResumoAprovacoesResponse(long pendentes) {
    }

    public record RejeitarRequest(@NotBlank @Size(max = 500) String motivo) {
    }

    /* ---- SoD ---- */

    public record RegraSodResponse(String codigo, Modulo moduloA, Modulo moduloB, String risco) {

        public static RegraSodResponse de(PoliticaSegregacao.Regra r) {
            return new RegraSodResponse(r.codigo(), r.moduloA(), r.moduloB(), r.risco());
        }
    }

    public record ConflitoSodResponse(PerfilUsuario perfil, String codigo, String risco, boolean excecaoDocumentada) {

        public static ConflitoSodResponse de(PoliticaSegregacao.Conflito c) {
            return new ConflitoSodResponse(c.perfil(), c.regra().codigo(), c.regra().risco(), c.excecaoDocumentada());
        }
    }

    public record PoliticaSodResponse(List<RegraSodResponse> regras, List<ConflitoSodResponse> conflitos,
                                      PerfilUsuario perfilComExcecao, String controlesCompensatorios) {
    }

    /* ---- Auditoria ---- */

    public record RegistroAuditoriaResponse(long id, OffsetDateTime ocorridoEm, String autor, String papelBanco,
                                            String tabela, String tabelaRotulo, String operacao, String registroId,
                                            List<AlteracaoCampo> alteracoes) {
    }

    public record AutorResponse(UUID usuarioId, String nome) {
    }

    public record TabelaAuditadaResponse(String tabela, String rotulo) {
    }

    public record VerificacaoResponse(boolean integra, long verificados, long ultimoId, String ultimoHash,
                                      int ancorasConferidas, Long registroComProblema, String tipoProblema,
                                      String mensagem, OffsetDateTime verificadaEm) {
    }

    /* ---- Revisão de acessos ---- */

    public record AlertaResponse(RevisaoAcesso.TipoAlerta tipo, String detalhe) {
    }

    public record ContaRevisadaResponse(UUID usuarioId, String email, String nome, PerfilUsuario perfil, boolean ativo,
                                        OffsetDateTime ultimoAcesso, List<AlertaResponse> alertas) {

        public static ContaRevisadaResponse de(RevisaoAcesso.ContaAvaliada c) {
            RevisaoAcesso.Conta k = c.conta();
            return new ContaRevisadaResponse(k.usuarioId(), k.email(), k.nome(), k.perfil(), k.ativo(), k.ultimoAcesso(),
                    c.alertas().stream().map(a -> new AlertaResponse(a.tipo(), a.detalhe())).toList());
        }
    }

    public record RevisaoRealizadaResponse(UUID id, OffsetDateTime realizadaEm, String realizadaPor, int contasRevisadas,
                                           int alertas, String observacao) {
    }

    public record RelatorioRevisaoResponse(List<ContaRevisadaResponse> contas, int totalAlertas, long contasComAlerta,
                                           long contasPortalAtivas, List<ConflitoSodResponse> conflitosPerfis,
                                           LocalDate ultimaRevisao, LocalDate proximaRevisao, boolean atrasada,
                                           List<RevisaoRealizadaResponse> historico) {
    }

    public record RegistrarRevisaoRequest(@Size(max = 1000) String observacao) {
    }
}
