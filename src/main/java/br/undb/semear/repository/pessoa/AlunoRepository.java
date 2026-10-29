package br.undb.semear.repository.pessoa;

import java.util.List;
import java.util.UUID;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.AlunoResumoResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.MatriculaHistoricoResponse;
import br.undb.semear.model.entity.pessoa.Aluno;
import br.undb.semear.repository.base.Repository;

/** Alunos nascem pela aprovação da pré-matrícula (fn_aprovar_pre_matricula); aqui só se lê e atualiza. */
public interface AlunoRepository extends Repository<Aluno, UUID> {

    /**
     * Alunos matriculados no ano, com busca por nome ou número de cadastro.
     * turmaId e professorId são filtros opcionais (professor só vê as próprias turmas).
     */
    PaginaResponse<AlunoResumoResponse> pesquisar(String busca, int ano, UUID turmaId, UUID professorId,
                                                  int pagina, int tamanho);

    List<MatriculaHistoricoResponse> listarMatriculas(UUID alunoId);

    /** O aluno tem matrícula em alguma turma deste professor? */
    boolean estudaComProfessor(UUID alunoId, UUID funcionarioId);
}
