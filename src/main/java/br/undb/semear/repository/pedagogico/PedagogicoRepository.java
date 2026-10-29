package br.undb.semear.repository.pedagogico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.pedagogico.DiarioClasse;
import br.undb.semear.model.entity.pedagogico.EventoCalendario;
import br.undb.semear.model.entity.pedagogico.FormacaoContinuada;
import br.undb.semear.model.entity.pedagogico.LancamentoNotas;
import br.undb.semear.model.entity.pedagogico.PlanoAula;
import br.undb.semear.model.entity.pedagogico.ProfessoresDaTurma;

/** Persistência do pedagógico (RF08): diário e chamada, planos, notas, calendário e formação continuada. */
public interface PedagogicoRepository {

    record AlunoAtivo(UUID matriculaId, UUID alunoId, long numeroCadastro, String nome) {
    }

    record DiarioResumo(UUID id, LocalDate data, UUID professorId, String professor, String conteudo, int presentes,
                        int faltas) {
    }

    record FrequenciaAluno(UUID matriculaId, UUID alunoId, String nome, int aulas, int presencas) {
    }

    record PlanoComAutor(PlanoAula plano, String professor) {
    }

    record Disciplina(UUID id, String nome) {
    }

    /* ---- Turma ---- */

    /** Matrículas ATIVAS da turma (quem entra na chamada e nas notas), por nome. */
    List<AlunoAtivo> alunosAtivos(UUID turmaId);

    ProfessoresDaTurma professores(UUID turmaId);

    /* ---- Diário ---- */

    Optional<DiarioClasse> diario(UUID turmaId, UUID professorId, LocalDate data);

    Optional<DiarioClasse> diario(UUID diarioId);

    List<DiarioResumo> diariosDaTurma(UUID turmaId, LocalDate de, LocalDate ate);

    /** Grava o diário e substitui a chamada inteira (numa transação do serviço). Devolve o id. */
    UUID salvarDiario(DiarioClasse diario);

    List<FrequenciaAluno> frequenciaDaTurma(UUID turmaId);

    /* ---- Planos ---- */

    List<PlanoComAutor> planosDaTurma(UUID turmaId, LocalDate de, LocalDate ate);

    Optional<PlanoAula> plano(UUID id);

    UUID salvarPlano(PlanoAula plano);

    void removerPlano(UUID id);

    /* ---- Notas ---- */

    List<Disciplina> disciplinas();

    /** Matrícula → nota lançada, da turma na disciplina e bimestre. */
    Map<UUID, BigDecimal> notas(UUID turmaId, UUID disciplinaId, int bimestre);

    void salvarNotas(LancamentoNotas lancamento);

    /* ---- Calendário ---- */

    List<EventoCalendario> eventos(int ano);

    Optional<EventoCalendario> evento(UUID id);

    UUID inserirEvento(EventoCalendario evento);

    void removerEvento(UUID id);

    /* ---- Formação continuada ---- */

    List<FormacaoContinuada> formacoes(UUID funcionarioId);

    Optional<FormacaoContinuada> formacao(UUID id);

    UUID inserirFormacao(FormacaoContinuada formacao);

    void removerFormacao(UUID id);
}
