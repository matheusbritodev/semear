package br.undb.semear.dto.academico;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoMatricula;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Entrada e saída dos endpoints acadêmicos (/api/anos-letivos, /api/turmas, /api/alertas). */
public final class AcademicoDtos {

    private AcademicoDtos() {
    }

    public record AnoLetivoResponse(UUID id, int ano, LocalDate dataInicio, LocalDate dataFim, boolean emAndamento) {

        public static AnoLetivoResponse de(AnoLetivo a, LocalDate hoje) {
            return new AnoLetivoResponse(a.getId(), a.getAno(), a.getDataInicio(), a.getDataFim(), a.emAndamento(hoje));
        }
    }

    public record TurmaResponse(UUID id, int ano, String serie, NivelEnsino nivel, String nivelRotulo, int ordem,
                                String nome, String nomeCompleto, int capacidadeMax, int ocupadas, int vagas,
                                int percentualOcupacao, boolean lotada) {

        public static TurmaResponse de(Turma t) {
            return new TurmaResponse(t.getId(), t.getAnoLetivo(), t.getSerie().getNome(), t.getSerie().getNivel(),
                    t.getSerie().getNivel().getRotulo(), t.getSerie().getOrdem(), t.getNome(), t.getNomeCompleto(),
                    t.getCapacidadeMax(), t.getOcupadas(), t.vagasRestantes(), t.percentualOcupacao(), t.estaLotada());
        }
    }

    public record TurmaDetalheResponse(TurmaResponse turma, List<AlunoDaTurmaResponse> alunos,
                                       List<ProfessorDaTurmaResponse> professores) {
    }

    public record AlunoDaTurmaResponse(UUID matriculaId, UUID alunoId, Long numeroCadastro, String nome, int idade,
                                       TipoMatricula tipo, StatusMatricula status) {
    }

    /** {@code disciplina} nula = regente (polivalente). {@code vinculoId} serve para remover o vínculo. */
    public record ProfessorDaTurmaResponse(UUID funcionarioId, String nome, String disciplina, UUID vinculoId,
                                           UUID disciplinaId) {
    }

    public record NovaTurmaRequest(@jakarta.validation.constraints.NotNull Integer ano,
                                   @jakarta.validation.constraints.NotNull UUID serieId,
                                   @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 20) String nome,
                                   @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1)
                                   @jakarta.validation.constraints.Max(30) Integer capacidade) {
    }

    /** {@code disciplinaId} nulo = professor regente (polivalente). */
    public record VincularProfessorRequest(@jakarta.validation.constraints.NotNull UUID funcionarioId, UUID disciplinaId) {
    }

    public record OpcaoResponse(UUID id, String nome) {
    }

    public record OpcoesProfessoresResponse(java.util.List<OpcaoResponse> professores, java.util.List<OpcaoResponse> disciplinas) {
    }

    public record AnoLetivoRequest(@jakarta.validation.constraints.NotNull Integer ano,
                                   @jakarta.validation.constraints.NotNull java.time.LocalDate dataInicio,
                                   @jakarta.validation.constraints.NotNull java.time.LocalDate dataFim) {
    }

    /** Linha da tabela de preços: valores nulos = série ainda sem preço no ano. */
    public record PrecoSerieResponse(UUID serieId, String serie, br.undb.semear.model.enums.NivelEnsino nivel, int ordem,
                                     java.math.BigDecimal valorMatricula, java.math.BigDecimal valorMensal,
                                     Integer diaVencimento, Integer diasPrazoMatricula) {
    }

    public record PrecoRequest(@jakarta.validation.constraints.NotNull UUID serieId, @jakarta.validation.constraints.NotNull Integer ano,
                               @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero java.math.BigDecimal valorMatricula,
                               @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive java.math.BigDecimal valorMensal,
                               @jakarta.validation.constraints.NotNull Integer diaVencimento,
                               @jakarta.validation.constraints.NotNull Integer diasPrazoMatricula) {
    }

    public record CopiarPrecosRequest(@jakarta.validation.constraints.NotNull Integer deAno,
                                      @jakarta.validation.constraints.NotNull Integer paraAno,
                                      @jakarta.validation.constraints.NotNull java.math.BigDecimal reajustePercentual) {
    }

    public record AlterarCapacidadeRequest(
            @NotNull @Min(value = 1, message = "Mínimo de 1 aluno") @Max(value = 30, message = "Máximo de 30 alunos (RN04)")
            Integer capacidade) {
    }

    public record AlertaResponse(UUID id, String tipo, String mensagem, UUID turmaId, boolean lido,
                                 OffsetDateTime criadoEm) {
    }
}
