package br.undb.semear.dto.pedagogico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.TipoCalendario;
import br.undb.semear.model.enums.TipoPlano;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Pedagógico (RF08): turmas do professor, diário e chamada, planos, notas, calendário e formação. */
public final class PedagogicoDtos {

    private PedagogicoDtos() {
    }

    /* ---- Turmas ---- */

    /** {@code disciplinas}: o que o professor logado leciona ali ("Polivalente" ou o nome); vazia para quem só consulta. */
    public record TurmaPedagogicaResponse(UUID turmaId, int ano, String nomeCompleto, NivelEnsino nivel, int alunosAtivos,
                                          List<String> disciplinas, LocalDate ultimoDiario, int bimestreAtual) {
    }

    /* ---- Diário ---- */

    public record ItemChamadaResponse(UUID matriculaId, UUID alunoId, long numeroCadastro, String nome, boolean presente,
                                      String justificativa) {
    }

    /**
     * Chamada de um dia. Sem diário ainda, todos vêm como presentes. {@code podeRegistrar}: quem está logado
     * leciona na turma, o dia tem aula e já chegou.
     */
    public record ChamadaResponse(UUID turmaId, String turma, LocalDate data, String motivoSemAula, UUID diarioId,
                                  String professor, String conteudo, boolean podeRegistrar, List<ItemChamadaResponse> alunos) {
    }

    public record PresencaRequest(@NotNull UUID matriculaId, @NotNull Boolean presente, @Size(max = 255) String justificativa) {
    }

    public record DiarioRequest(@NotBlank @Size(max = 2000) String conteudo, @NotNull @Valid List<PresencaRequest> presencas) {
    }

    public record DiarioResumoResponse(UUID id, LocalDate data, String professor, String conteudo, int presentes, int faltas,
                                       boolean meu) {
    }

    public record FrequenciaAlunoResponse(UUID matriculaId, UUID alunoId, String nome, int aulas, int presencas, int faltas,
                                          BigDecimal percentual, boolean abaixoDoMinimo) {
    }

    /* ---- Planos ---- */

    public record PlanoResponse(UUID id, TipoPlano tipo, LocalDate dataPrevista, String objetivo, String conteudo,
                                String professor, boolean meu) {
    }

    public record PlanoRequest(@NotNull TipoPlano tipo, @NotNull LocalDate dataPrevista,
                               @NotBlank @Size(max = 255) String objetivo, @NotBlank @Size(max = 4000) String conteudo) {
    }

    /* ---- Notas ---- */

    public record DisciplinaOpcaoResponse(UUID id, String nome, boolean podeLancar) {
    }

    public record NotaAlunoResponse(UUID matriculaId, UUID alunoId, long numeroCadastro, String nome, BigDecimal valor) {
    }

    public record NotasTurmaResponse(UUID turmaId, String turma, NivelEnsino nivel, UUID disciplinaId, String disciplina,
                                     int bimestre, boolean podeLancar, List<DisciplinaOpcaoResponse> disciplinas,
                                     List<NotaAlunoResponse> alunos) {
    }

    /** {@code valor} nulo apaga a nota lançada. */
    public record NotaRequest(@NotNull UUID matriculaId, BigDecimal valor) {
    }

    public record LancarNotasRequest(@NotNull UUID disciplinaId, @NotNull @Min(1) @Max(4) Integer bimestre,
                                     @NotNull @Valid List<NotaRequest> notas) {
    }

    /* ---- Calendário ---- */

    public record EventoResponse(UUID id, LocalDate data, TipoCalendario tipo, String tipoRotulo, String descricao,
                                 boolean temAula) {
    }

    public record EventoRequest(@NotNull Integer ano, @NotNull LocalDate data, @NotNull TipoCalendario tipo,
                                @NotBlank @Size(max = 200) String descricao) {
    }

    /* ---- Formação continuada ---- */

    public record FormacaoResponse(UUID id, String curso, int cargaHoraria, LocalDate concluidaEm, boolean concluida) {
    }

    public record FormacaoRequest(@NotBlank @Size(max = 200) String curso, @NotNull @Min(1) @Max(2000) Integer cargaHoraria,
                                  LocalDate concluidaEm) {
    }
}
