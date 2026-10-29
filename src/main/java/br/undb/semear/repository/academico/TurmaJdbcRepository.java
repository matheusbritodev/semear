package br.undb.semear.repository.academico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.academico.AcademicoDtos.AlunoDaTurmaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.ProfessorDaTurmaResponse;
import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoMatricula;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class TurmaJdbcRepository extends JdbcRepository<Turma> implements TurmaRepository {

    private final Clock relogio;

    public TurmaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor, Clock relogio) {
        super(jdbc, tradutor, "turma");
        this.relogio = relogio;
    }

    /** A ocupação conta as mesmas matrículas que o trigger do banco (RN04). */
    @Override
    protected String selectBase() {
        return """
                SELECT t.id, t.nome, t.capacidade_max, al.ano,
                       s.id AS serie_id, s.nome AS serie_nome, s.nivel, s.ordem,
                       (SELECT count(*) FROM matricula m
                         WHERE m.turma_id = t.id AND m.status IN ('AGUARDANDO_PAGAMENTO', 'ATIVA'))::int AS ocupadas
                  FROM turma t
                  JOIN serie s       ON s.id = t.serie_id
                  JOIN ano_letivo al ON al.id = t.ano_letivo_id
                """;
    }

    @Override
    protected String colunaId() {
        return "t.id";
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY al.ano, s.ordem, t.nome";
    }

    @Override
    protected Turma mapear(ResultSet linha, int numeroLinha) throws SQLException {
        Serie serie = new Serie(Colunas.uuid(linha, "serie_id"), linha.getString("serie_nome"),
                NivelEnsino.valueOf(linha.getString("nivel")), linha.getInt("ordem"));
        return new Turma(Colunas.uuid(linha, "id"), serie, linha.getInt("ano"), linha.getString("nome"),
                linha.getInt("capacidade_max"), linha.getInt("ocupadas"));
    }

    /** Nesta etapa só a capacidade e o nome mudam; turmas novas virão com a tela de configuração do ano. */
    @Override
    public Turma salvar(Turma turma) {
        if (!turma.estaPersistida()) {
            throw new RegraNegocioException("OPERACAO_NAO_DISPONIVEL", "Cadastro de turmas ainda não disponível");
        }
        executar(() -> jdbc.sql("UPDATE turma SET nome = :nome, capacidade_max = :capacidade WHERE id = :id")
                .param("id", turma.getId()).param("nome", turma.getNome())
                .param("capacidade", turma.getCapacidadeMax())
                .update());
        return buscarPorId(turma.getId()).orElseThrow();
    }

    @Override
    public List<Turma> listarPorAno(int ano, UUID professorId) {
        return executar(() -> jdbc.sql(selectBase() + """
                         WHERE al.ano = :ano
                           AND (CAST(:professor AS uuid) IS NULL OR EXISTS (
                                SELECT 1 FROM turma_professor tp
                                 WHERE tp.turma_id = t.id AND tp.funcionario_id = CAST(:professor AS uuid)))
                        """ + ordenacao())
                .param("ano", ano)
                .param("professor", professorId)
                .query(this::mapear)
                .list());
    }

    @Override
    public List<AlunoDaTurmaResponse> listarAlunos(UUID turmaId) {
        LocalDate hoje = LocalDate.now(relogio);
        return executar(() -> jdbc.sql("""
                        SELECT m.id AS matricula_id, a.id AS aluno_id, a.numero_cadastro, a.nome, a.data_nascimento,
                               m.tipo, m.status
                          FROM matricula m
                          JOIN aluno a ON a.id = m.aluno_id
                         WHERE m.turma_id = :turma AND m.status IN ('AGUARDANDO_PAGAMENTO', 'ATIVA')
                         ORDER BY a.nome
                        """)
                .param("turma", turmaId)
                .query((rs, n) -> new AlunoDaTurmaResponse(
                        Colunas.uuid(rs, "matricula_id"), Colunas.uuid(rs, "aluno_id"), rs.getLong("numero_cadastro"),
                        rs.getString("nome"), Period.between(Colunas.data(rs, "data_nascimento"), hoje).getYears(),
                        TipoMatricula.valueOf(rs.getString("tipo")), StatusMatricula.valueOf(rs.getString("status"))))
                .list());
    }

    @Override
    public List<ProfessorDaTurmaResponse> listarProfessores(UUID turmaId) {
        return executar(() -> jdbc.sql("""
                        SELECT f.id, f.nome, d.nome AS disciplina
                          FROM turma_professor tp
                          JOIN funcionario f ON f.id = tp.funcionario_id
                          LEFT JOIN disciplina d ON d.id = tp.disciplina_id
                         WHERE tp.turma_id = :turma
                         ORDER BY d.nome NULLS FIRST, f.nome
                        """)
                .param("turma", turmaId)
                .query((rs, n) -> new ProfessorDaTurmaResponse(Colunas.uuid(rs, "id"), rs.getString("nome"),
                        rs.getString("disciplina")))
                .list());
    }

    @Override
    public boolean professorLeciona(UUID turmaId, UUID funcionarioId) {
        return executar(() -> jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM turma_professor WHERE turma_id = :turma AND funcionario_id = :professor)
                        """)
                .param("turma", turmaId).param("professor", funcionarioId)
                .query(Boolean.class).single());
    }
}
