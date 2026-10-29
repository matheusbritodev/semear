package br.undb.semear.repository.pedagogico;

import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.pedagogico.Boletim;
import br.undb.semear.model.enums.NivelEnsino;

public interface BoletimRepository {

    /** Matrícula que pode ter boletim (não cancelada). */
    record MatriculaDoBoletim(UUID matriculaId, int ano, String serie, String turma, NivelEnsino nivel) {
    }

    /** Do ano mais recente para o mais antigo. */
    List<MatriculaDoBoletim> matriculasDoAluno(UUID alunoId);

    List<Boletim.Nota> notas(UUID matriculaId);

    Boletim.Frequencia frequencia(UUID matriculaId);
}
