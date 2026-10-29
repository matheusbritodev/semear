package br.undb.semear.repository.pedagogico;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.pedagogico.DiarioClasse;
import br.undb.semear.model.entity.pedagogico.EventoCalendario;
import br.undb.semear.model.entity.pedagogico.FormacaoContinuada;
import br.undb.semear.model.entity.pedagogico.LancamentoNotas;
import br.undb.semear.model.entity.pedagogico.PlanoAula;
import br.undb.semear.model.entity.pedagogico.ProfessoresDaTurma;
import br.undb.semear.model.enums.TipoCalendario;
import br.undb.semear.model.enums.TipoPlano;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class PedagogicoJdbcRepository implements PedagogicoRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public PedagogicoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    /* ---- Turma ---- */

    @Override
    public List<AlunoAtivo> alunosAtivos(UUID turmaId) {
        return executar(() -> jdbc.sql("""
                        SELECT m.id, a.id AS aluno_id, a.numero_cadastro, a.nome
                          FROM matricula m JOIN aluno a ON a.id = m.aluno_id
                         WHERE m.turma_id = :turma AND m.status = 'ATIVA'
                         ORDER BY a.nome
                        """)
                .param("turma", turmaId)
                .query((rs, n) -> new AlunoAtivo(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "aluno_id"),
                        rs.getLong("numero_cadastro"), rs.getString("nome")))
                .list());
    }

    @Override
    public ProfessoresDaTurma professores(UUID turmaId) {
        return new ProfessoresDaTurma(executar(() -> jdbc.sql(
                        "SELECT funcionario_id, disciplina_id FROM turma_professor WHERE turma_id = :turma")
                .param("turma", turmaId)
                .query((rs, n) -> new ProfessoresDaTurma.Atribuicao(Colunas.uuid(rs, "funcionario_id"),
                        Colunas.uuid(rs, "disciplina_id")))
                .list()));
    }

    /* ---- Diário ---- */

    @Override
    public Optional<DiarioClasse> diario(UUID turmaId, UUID professorId, LocalDate data) {
        return executar(() -> jdbc.sql("""
                        SELECT id FROM diario_classe WHERE turma_id = :turma AND funcionario_id = :professor AND data_aula = :data
                        """)
                .param("turma", turmaId).param("professor", professorId).param("data", data)
                .query(UUID.class).optional())
                .flatMap(this::diario);
    }

    @Override
    public Optional<DiarioClasse> diario(UUID diarioId) {
        List<DiarioClasse.Presenca> presencas = executar(() -> jdbc.sql("""
                        SELECT matricula_id, presente, justificativa FROM frequencia WHERE diario_id = :id
                        """)
                .param("id", diarioId)
                .query((rs, n) -> new DiarioClasse.Presenca(Colunas.uuid(rs, "matricula_id"), rs.getBoolean("presente"),
                        rs.getString("justificativa")))
                .list());
        return executar(() -> jdbc.sql("SELECT id, turma_id, funcionario_id, data_aula, conteudo FROM diario_classe WHERE id = :id")
                .param("id", diarioId)
                .query((rs, n) -> new DiarioClasse(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "turma_id"),
                        Colunas.uuid(rs, "funcionario_id"), Colunas.data(rs, "data_aula"), rs.getString("conteudo"), presencas))
                .optional());
    }

    @Override
    public List<DiarioResumo> diariosDaTurma(UUID turmaId, LocalDate de, LocalDate ate) {
        return executar(() -> jdbc.sql("""
                        SELECT d.id, d.data_aula, d.funcionario_id, f.nome AS professor, d.conteudo,
                               count(fr.*) FILTER (WHERE fr.presente) AS presentes,
                               count(fr.*) FILTER (WHERE NOT fr.presente) AS faltas
                          FROM diario_classe d
                          JOIN funcionario f ON f.id = d.funcionario_id
                          LEFT JOIN frequencia fr ON fr.diario_id = d.id
                         WHERE d.turma_id = :turma AND d.data_aula BETWEEN :de AND :ate
                         GROUP BY d.id, f.nome
                         ORDER BY d.data_aula DESC, f.nome
                        """)
                .param("turma", turmaId).param("de", de).param("ate", ate)
                .query((rs, n) -> new DiarioResumo(Colunas.uuid(rs, "id"), Colunas.data(rs, "data_aula"),
                        Colunas.uuid(rs, "funcionario_id"), rs.getString("professor"), rs.getString("conteudo"),
                        rs.getInt("presentes"), rs.getInt("faltas")))
                .list());
    }

    @Override
    public UUID salvarDiario(DiarioClasse d) {
        UUID id = executar(() -> jdbc.sql("""
                        INSERT INTO diario_classe (turma_id, funcionario_id, data_aula, conteudo)
                        VALUES (:turma, :professor, :data, :conteudo)
                        ON CONFLICT (turma_id, funcionario_id, data_aula) DO UPDATE SET conteudo = EXCLUDED.conteudo
                        RETURNING id
                        """)
                .param("turma", d.getTurmaId()).param("professor", d.getProfessorId()).param("data", d.getData())
                .param("conteudo", d.getConteudo())
                .query(UUID.class).single());
        executar(() -> jdbc.sql("DELETE FROM frequencia WHERE diario_id = :id").param("id", id).update());
        for (DiarioClasse.Presenca p : d.getPresencas()) {
            executar(() -> jdbc.sql("""
                            INSERT INTO frequencia (diario_id, matricula_id, presente, justificativa)
                            VALUES (:diario, :matricula, :presente, :justificativa)
                            """)
                    .param("diario", id).param("matricula", p.matriculaId()).param("presente", p.presente())
                    .param("justificativa", p.justificativa())
                    .update());
        }
        return id;
    }

    @Override
    public List<FrequenciaAluno> frequenciaDaTurma(UUID turmaId) {
        return executar(() -> jdbc.sql("""
                        SELECT m.id, a.id AS aluno_id, a.nome, count(fr.*) AS aulas,
                               count(fr.*) FILTER (WHERE fr.presente) AS presencas
                          FROM matricula m
                          JOIN aluno a ON a.id = m.aluno_id
                          LEFT JOIN frequencia fr ON fr.matricula_id = m.id
                         WHERE m.turma_id = :turma AND m.status = 'ATIVA'
                         GROUP BY m.id, a.id, a.nome
                         ORDER BY a.nome
                        """)
                .param("turma", turmaId)
                .query((rs, n) -> new FrequenciaAluno(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "aluno_id"),
                        rs.getString("nome"), rs.getInt("aulas"), rs.getInt("presencas")))
                .list());
    }

    /* ---- Planos ---- */

    @Override
    public List<PlanoComAutor> planosDaTurma(UUID turmaId, LocalDate de, LocalDate ate) {
        return executar(() -> jdbc.sql("""
                        SELECT p.*, f.nome AS professor FROM plano_aula p JOIN funcionario f ON f.id = p.funcionario_id
                         WHERE p.turma_id = :turma AND p.data_prevista BETWEEN :de AND :ate
                         ORDER BY p.data_prevista, p.tipo
                        """)
                .param("turma", turmaId).param("de", de).param("ate", ate)
                .query((rs, n) -> new PlanoComAutor(mapearPlano(rs), rs.getString("professor")))
                .list());
    }

    @Override
    public Optional<PlanoAula> plano(UUID id) {
        return executar(() -> jdbc.sql("SELECT * FROM plano_aula WHERE id = :id").param("id", id)
                .query((rs, n) -> mapearPlano(rs)).optional());
    }

    @Override
    public UUID salvarPlano(PlanoAula p) {
        return executar(() -> jdbc.sql(p.estaPersistida()
                        ? """
                          UPDATE plano_aula SET tipo = CAST(:tipo AS tipo_plano), data_prevista = :data,
                                 objetivo = :objetivo, conteudo = :conteudo
                           WHERE id = :id RETURNING id
                          """
                        : """
                          INSERT INTO plano_aula (turma_id, funcionario_id, tipo, data_prevista, objetivo, conteudo)
                          VALUES (:turma, :professor, CAST(:tipo AS tipo_plano), :data, :objetivo, :conteudo) RETURNING id
                          """)
                .param("id", p.getId()).param("turma", p.getTurmaId()).param("professor", p.getProfessorId())
                .param("tipo", p.getTipo().name()).param("data", p.getDataPrevista())
                .param("objetivo", p.getObjetivo()).param("conteudo", p.getConteudo())
                .query(UUID.class).single());
    }

    @Override
    public void removerPlano(UUID id) {
        executar(() -> jdbc.sql("DELETE FROM plano_aula WHERE id = :id").param("id", id).update());
    }

    /* ---- Notas ---- */

    @Override
    public List<Disciplina> disciplinas() {
        return executar(() -> jdbc.sql("SELECT id, nome FROM disciplina ORDER BY nome")
                .query((rs, n) -> new Disciplina(Colunas.uuid(rs, "id"), rs.getString("nome"))).list());
    }

    @Override
    public Map<UUID, BigDecimal> notas(UUID turmaId, UUID disciplinaId, int bimestre) {
        Map<UUID, BigDecimal> mapa = new HashMap<>();
        executarSemRetorno(() -> jdbc.sql("""
                        SELECT n.matricula_id, n.valor FROM nota n JOIN matricula m ON m.id = n.matricula_id
                         WHERE m.turma_id = :turma AND n.disciplina_id = :disciplina AND n.bimestre = :bimestre
                        """)
                .param("turma", turmaId).param("disciplina", disciplinaId).param("bimestre", bimestre)
                .query(rs -> {
                    mapa.put(Colunas.uuid(rs, "matricula_id"), rs.getBigDecimal("valor"));
                }));
        return mapa;
    }

    @Override
    public void salvarNotas(LancamentoNotas l) {
        l.getNotas().forEach((matricula, nota) -> executar(() -> nota.isPresent()
                ? jdbc.sql("""
                          INSERT INTO nota (matricula_id, disciplina_id, bimestre, valor) VALUES (:m, :d, :b, :v)
                          ON CONFLICT (matricula_id, disciplina_id, bimestre) DO UPDATE SET valor = EXCLUDED.valor
                          """)
                        .param("m", matricula).param("d", l.getDisciplinaId()).param("b", l.getBimestre())
                        .param("v", nota.get().getValor()).update()
                : jdbc.sql("DELETE FROM nota WHERE matricula_id = :m AND disciplina_id = :d AND bimestre = :b")
                        .param("m", matricula).param("d", l.getDisciplinaId()).param("b", l.getBimestre()).update()));
    }

    /* ---- Calendário ---- */

    private static final String SELECT_EVENTO = """
            SELECT c.id, al.ano, c.data, c.tipo, c.descricao
              FROM calendario_letivo c JOIN ano_letivo al ON al.id = c.ano_letivo_id
            """;

    @Override
    public List<EventoCalendario> eventos(int ano) {
        return executar(() -> jdbc.sql(SELECT_EVENTO + " WHERE al.ano = :ano ORDER BY c.data")
                .param("ano", ano).query((rs, n) -> mapearEvento(rs)).list());
    }

    @Override
    public Optional<EventoCalendario> evento(UUID id) {
        return executar(() -> jdbc.sql(SELECT_EVENTO + " WHERE c.id = :id").param("id", id)
                .query((rs, n) -> mapearEvento(rs)).optional());
    }

    @Override
    public UUID inserirEvento(EventoCalendario e) {
        return executar(() -> jdbc.sql("""
                        INSERT INTO calendario_letivo (ano_letivo_id, data, tipo, descricao)
                        SELECT al.id, :data, CAST(:tipo AS tipo_calendario), :descricao FROM ano_letivo al WHERE al.ano = :ano
                        RETURNING id
                        """)
                .param("ano", e.getAno()).param("data", e.getData()).param("tipo", e.getTipo().name())
                .param("descricao", e.getDescricao())
                .query(UUID.class).optional())
                .orElseThrow(() -> new RegraNegocioException("ANO_LETIVO_INEXISTENTE", "Ano letivo não cadastrado"));
    }

    @Override
    public void removerEvento(UUID id) {
        executar(() -> jdbc.sql("DELETE FROM calendario_letivo WHERE id = :id").param("id", id).update());
    }

    /* ---- Formação continuada ---- */

    @Override
    public List<FormacaoContinuada> formacoes(UUID funcionarioId) {
        return executar(() -> jdbc.sql("""
                        SELECT * FROM formacao_continuada WHERE funcionario_id = :f
                         ORDER BY concluida_em DESC NULLS FIRST, curso
                        """)
                .param("f", funcionarioId).query((rs, n) -> mapearFormacao(rs)).list());
    }

    @Override
    public Optional<FormacaoContinuada> formacao(UUID id) {
        return executar(() -> jdbc.sql("SELECT * FROM formacao_continuada WHERE id = :id").param("id", id)
                .query((rs, n) -> mapearFormacao(rs)).optional());
    }

    @Override
    public UUID inserirFormacao(FormacaoContinuada f) {
        return executar(() -> jdbc.sql("""
                        INSERT INTO formacao_continuada (funcionario_id, curso, carga_horaria, concluida_em)
                        VALUES (:f, :curso, :carga, :concluida) RETURNING id
                        """)
                .param("f", f.getFuncionarioId()).param("curso", f.getCurso()).param("carga", f.getCargaHoraria())
                .param("concluida", f.getConcluidaEm())
                .query(UUID.class).single());
    }

    @Override
    public void removerFormacao(UUID id) {
        executar(() -> jdbc.sql("DELETE FROM formacao_continuada WHERE id = :id").param("id", id).update());
    }

    /* ---- Mapeamento ---- */

    private static PlanoAula mapearPlano(ResultSet rs) throws SQLException {
        return new PlanoAula(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "turma_id"), Colunas.uuid(rs, "funcionario_id"),
                TipoPlano.valueOf(rs.getString("tipo")), Colunas.data(rs, "data_prevista"), rs.getString("objetivo"),
                rs.getString("conteudo"));
    }

    private static EventoCalendario mapearEvento(ResultSet rs) throws SQLException {
        return new EventoCalendario(Colunas.uuid(rs, "id"), rs.getInt("ano"), Colunas.data(rs, "data"),
                TipoCalendario.valueOf(rs.getString("tipo")), rs.getString("descricao"));
    }

    private static FormacaoContinuada mapearFormacao(ResultSet rs) throws SQLException {
        return new FormacaoContinuada(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "funcionario_id"), rs.getString("curso"),
                rs.getInt("carga_horaria"), Colunas.data(rs, "concluida_em"));
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    private void executarSemRetorno(Runnable operacao) {
        executar(() -> {
            operacao.run();
            return null;
        });
    }
}
