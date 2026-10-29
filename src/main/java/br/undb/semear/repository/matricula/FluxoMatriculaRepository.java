package br.undb.semear.repository.matricula;

import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.repository.base.TradutorErroBanco;

/**
 * Chama as funções transacionais do banco (decisão D06): elas criam aluno, vínculo, matrícula,
 * contrato e cobrança de uma vez. O Model valida antes; o banco é a garantia final.
 */
@Repository
public class FluxoMatriculaRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public FluxoMatriculaRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    /** fn_aprovar_pre_matricula: devolve o id da matrícula criada. */
    public UUID aprovarPreMatricula(UUID preMatriculaId, UUID funcionarioId, UUID turmaId) {
        try {
            return jdbc.sql("SELECT fn_aprovar_pre_matricula(:pre, :funcionario, :turma)")
                    .param("pre", preMatriculaId).param("funcionario", funcionarioId).param("turma", turmaId)
                    .query(UUID.class).single();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    public void rejeitarPreMatricula(UUID preMatriculaId, UUID funcionarioId, String motivo) {
        try {
            jdbc.sql("SELECT fn_rejeitar_pre_matricula(:pre, :funcionario, :motivo)")
                    .param("pre", preMatriculaId).param("funcionario", funcionarioId).param("motivo", motivo)
                    .query().singleRow();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    /** fn_rematricular: mesmo cadastro de aluno, nova matrícula (tipo REMATRICULA). */
    public UUID rematricular(UUID alunoId, UUID turmaId, String fardaUniforme, String fardaEdFisica) {
        try {
            return jdbc.sql("SELECT fn_rematricular(:aluno, :turma, :uniforme, :edFisica)")
                    .param("aluno", alunoId).param("turma", turmaId)
                    .param("uniforme", fardaUniforme).param("edFisica", fardaEdFisica)
                    .query(UUID.class).single();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
