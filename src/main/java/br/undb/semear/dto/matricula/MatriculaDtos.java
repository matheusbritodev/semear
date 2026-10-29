package br.undb.semear.dto.matricula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.dto.academico.AcademicoDtos.TurmaResponse;
import br.undb.semear.model.entity.matricula.Rematricula;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.enums.TipoContrato;
import br.undb.semear.model.enums.TipoMatricula;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Entrada e saída dos endpoints de pré-matrícula, matrícula e rematrícula. */
public final class MatriculaDtos {

    private MatriculaDtos() {
    }

    /* ---- Pré-matrícula ---- */

    public record NovaPreMatriculaRequest(
            @NotNull(message = "Escolha o ano letivo") Integer ano,
            @NotNull(message = "Escolha a série") UUID serieId,
            @NotBlank(message = "Informe o nome da criança") @Size(max = 150) String alunoNome,
            @NotNull(message = "Informe a data de nascimento") @PastOrPresent LocalDate alunoNascimento,
            String alunoCpf,
            @NotBlank(message = "Informe o parentesco") @Size(max = 50) String parentesco,
            @Size(max = 10) String fardaUniforme,
            @Size(max = 10) String fardaEdFisica) {
    }

    public record PreMatriculaResumoResponse(UUID id, OffsetDateTime criadoEm, int ano, String serie, NivelEnsino nivel,
                                             String alunoNome, LocalDate alunoNascimento, int idade,
                                             String responsavel, String telefone, StatusPreMatricula status,
                                             String motivoRejeicao, UUID matriculaId) {
    }

    public record PreMatriculaDetalheResponse(PreMatriculaResumoResponse preMatricula, UUID serieId, String parentesco,
                                              String alunoCpf, String fardaUniforme, String fardaEdFisica,
                                              UUID responsavelId, String responsavelEmail,
                                              List<TurmaResponse> turmasDisponiveis) {
    }

    public record AprovarPreMatriculaRequest(@NotNull(message = "Escolha a turma") UUID turmaId) {
    }

    public record RejeitarPreMatriculaRequest(@NotBlank(message = "Informe o motivo") @Size(max = 500) String motivo) {
    }

    public record DecisaoResponse(UUID preMatriculaId, StatusPreMatricula status, UUID matriculaId) {
    }

    /* ---- Matrícula ---- */

    public record MatriculaResumoResponse(UUID id, UUID alunoId, Long numeroCadastro, String aluno, int ano,
                                          UUID turmaId, String turma, TipoMatricula tipo, StatusMatricula status,
                                          LocalDate dataMatricula) {
    }

    public record ContratoResponse(UUID id, TipoContrato tipo, StatusContrato status, String assinadoPor,
                                   OffsetDateTime assinadoEm) {
    }

    public record FardaResponse(String tipo, String tamanho) {
    }

    public record CobrancaResumoResponse(UUID id, TipoCobranca tipo, LocalDate competencia, BigDecimal valorFinal,
                                         LocalDate vencimento, StatusCobranca status, OffsetDateTime pagoEm,
                                         BigDecimal valorPago, OrigemPagamento origemPagamento) {
    }

    public record MatriculaDetalheResponse(MatriculaResumoResponse matricula, String responsavelFinanceiro,
                                           String motivoCancelamento, List<ContratoResponse> contratos,
                                           List<FardaResponse> fardas, CobrancaResumoResponse cobrancaMatricula,
                                           boolean possuiDebitosVencidos, boolean documentacaoCompleta,
                                           int documentosPendentes) {
    }

    public record CancelarMatriculaRequest(@NotBlank(message = "Informe o motivo") @Size(max = 500) String motivo) {
    }

    public record RegistrarPagamentoRequest(@NotNull(message = "Informe a origem") OrigemPagamento origem) {
    }

    /* ---- Rematrícula ---- */

    public record RematriculaOpcaoResponse(UUID alunoId, String aluno, String turmaAtual, int anoDestino,
                                           String situacao, String mensagem, boolean permitida,
                                           UUID turmaDestinoId, String turmaDestino, Integer vagasDestino,
                                           UUID matriculaDestinoId, StatusMatricula statusDestino) {

        public static RematriculaOpcaoResponse de(UUID alunoId, String aluno, String turmaAtual, int anoDestino,
                                                  Rematricula.Avaliacao avaliacao, UUID matriculaDestinoId,
                                                  StatusMatricula statusDestino) {
            var turma = avaliacao.turmaSugerida();
            return new RematriculaOpcaoResponse(alunoId, aluno, turmaAtual, anoDestino,
                    avaliacao.situacao().name(), avaliacao.situacao().getMensagem(), avaliacao.situacao().permite(),
                    turma.map(t -> t.getId()).orElse(null), turma.map(t -> t.getNomeCompleto()).orElse(null),
                    turma.map(t -> t.vagasRestantes()).orElse(null), matriculaDestinoId, statusDestino);
        }
    }

    public record RematricularRequest(@NotNull(message = "Informe o aluno") UUID alunoId,
                                      @Size(max = 10) String fardaUniforme,
                                      @Size(max = 10) String fardaEdFisica) {
    }
}
