package br.undb.semear.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.entity.financeiro.ValorNaData;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.enums.TipoDesconto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Entrada e saída do módulo financeiro (/api/cobrancas, /api/financeiro, /api/bolsas). */
public final class FinanceiroDtos {

    private FinanceiroDtos() {
    }

    /* ---- Cobranças ---- */

    /**
     * Cobrança como as telas mostram. {@code status} é o real hoje (pendente vencida = VENCIDO)
     * e {@code valorHoje} já tem desconto de pontualidade ou encargos (RN08/RN09).
     */
    public record CobrancaResponse(UUID id, TipoCobranca tipo, LocalDate competencia, UUID matriculaId, UUID alunoId,
                                   long numeroCadastro, String aluno, int ano, String turma, String responsavel,
                                   String telefone, BigDecimal valorBruto, BigDecimal descontoBolsa,
                                   BigDecimal valorFinal, LocalDate vencimento, StatusCobranca status, long diasDeAtraso,
                                   BigDecimal valorHoje, OffsetDateTime pagoEm, BigDecimal valorPago,
                                   OrigemPagamento origemPagamento, boolean temBoleto, String linkPagamento,
                                   String baixaRegistradaPor) {
    }

    /** Composição do valor numa data: total = valorFinal − descontoPontualidade + multa + juros. */
    public record ValorNaDataResponse(LocalDate data, BigDecimal valorFinal, BigDecimal descontoPontualidade,
                                      BigDecimal multa, BigDecimal juros, long diasDeAtraso, BigDecimal total) {

        public static ValorNaDataResponse de(ValorNaData v) {
            return new ValorNaDataResponse(v.data(), v.valorFinal().getValor(), v.descontoPontualidade().getValor(),
                    v.encargos().multa().getValor(), v.encargos().juros().getValor(), v.encargos().diasDeAtraso(),
                    v.total().getValor());
        }
    }

    public record CobrancaDetalheResponse(CobrancaResponse cobranca, ValorNaDataResponse composicao) {
    }

    public record GerarCobrancasRequest(@NotNull Integer ano, @NotNull @Min(1) @Max(12) Integer mesInicio,
                                        @NotNull @Min(1) @Max(12) Integer mesFim) {
    }

    public record GeracaoResponse(int criadas, int boletosEmitidos) {
    }

    public record PagamentoRequest(@NotNull OrigemPagamento origem) {
    }

    /* ---- Inadimplência e resumo ---- */

    public record InadimplenteResponse(UUID alunoId, long numeroCadastro, String aluno, String turma, String responsavel,
                                       String telefone, int cobrancas, long maiorAtraso, BigDecimal valorOriginal,
                                       BigDecimal valorAtualizado, List<CobrancaResponse> itens) {
    }

    public record InadimplenciaResponse(int alunos, int cobrancas, BigDecimal valorOriginal, BigDecimal valorAtualizado,
                                        List<InadimplenteResponse> inadimplentes) {
    }

    public record MesResumoResponse(LocalDate competencia, int quantidade, BigDecimal previsto, BigDecimal descontosBolsa,
                                    BigDecimal recebido, BigDecimal emAtraso) {
    }

    public record ResumoAnoResponse(int ano, List<MesResumoResponse> meses, MesResumoResponse total) {
    }

    /* ---- Bolsas ---- */

    public record BolsaResponse(UUID id, String nome, String descricao, TipoDesconto tipoDesconto, BigDecimal valor,
                                boolean ativa, int bolsistasVigentes) {

        public static BolsaResponse de(Bolsa b, int bolsistas) {
            return new BolsaResponse(b.getId(), b.getNome(), b.getDescricao(), b.getTipoDesconto(), b.getValor(),
                    b.isAtiva(), bolsistas);
        }
    }

    public record BolsaRequest(@NotBlank @Size(max = 100) String nome, @Size(max = 500) String descricao,
                               @NotNull TipoDesconto tipoDesconto, @NotNull @Positive BigDecimal valor,
                               @NotNull Boolean ativa) {
    }

    public record ConcessaoResponse(UUID id, UUID matriculaId, UUID bolsaId, String bolsa, TipoDesconto tipoDesconto,
                                    BigDecimal valor, boolean bolsaAtiva, UUID alunoId, String aluno, int ano,
                                    String turma, LocalDate vigenciaInicio, LocalDate vigenciaFim, boolean vigente) {
    }

    /** Alteração de bolsa: aplicada (solicitacao nula) ou enviada para aprovação (bolsa ainda como estava). */
    public record AlteracaoBolsaResponse(BolsaResponse bolsa,
                                         br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse solicitacao) {
    }

    public record ConcederBolsaRequest(@NotNull UUID bolsaId, LocalDate vigenciaInicio) {
    }
}
