package br.undb.semear.repository.academico;

import java.util.List;
import java.util.UUID;

import br.undb.semear.dto.academico.AcademicoDtos.AlunoDaTurmaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.ProfessorDaTurmaResponse;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.repository.base.Repository;

public interface TurmaRepository extends Repository<Turma, UUID> {

    /** Turmas do ano; se professorId vier preenchido, só as turmas em que ele leciona. */
    List<Turma> listarPorAno(int ano, UUID professorId);

    /** Alunos que ocupam vaga (aguardando pagamento ou ativos). */
    List<AlunoDaTurmaResponse> listarAlunos(UUID turmaId);

    List<ProfessorDaTurmaResponse> listarProfessores(UUID turmaId);

    boolean professorLeciona(UUID turmaId, UUID funcionarioId);
}
