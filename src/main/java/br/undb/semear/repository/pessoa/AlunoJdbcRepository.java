package br.undb.semear.repository.pessoa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.AlunoResumoResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.MatriculaHistoricoResponse;
import br.undb.semear.model.entity.pessoa.Aluno;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoMatricula;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.matricula.DocumentosObrigatoriosSql;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class AlunoJdbcRepository extends JdbcRepository<Aluno> implements AlunoRepository {

    private final Clock relogio;

    public AlunoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor, Clock relogio) {
        super(jdbc, tradutor, "aluno");
        this.relogio = relogio;
    }

    /** O aluno sempre vem completo, com os vínculos (responsáveis). */
    @Override
    protected Aluno mapear(ResultSet linha, int numeroLinha) throws SQLException {
        Aluno aluno = new Aluno(
                Colunas.uuid(linha, "id"),
                linha.getLong("numero_cadastro"),
                linha.getString("nome"),
                Colunas.data(linha, "data_nascimento"),
                Colunas.cpf(linha, "cpf"),
                linha.getBoolean("ativo"));
        jdbc.sql("""
                        SELECT r.id, r.nome, r.cpf, r.telefone, r.email, r.usuario_id, ar.parentesco, ar.resp_financeiro
                          FROM aluno_responsavel ar
                          JOIN responsavel r ON r.id = ar.responsavel_id
                         WHERE ar.aluno_id = :id
                         ORDER BY ar.resp_financeiro, r.nome
                        """)
                .param("id", aluno.getId())
                .query((rs, n) -> {
                    aluno.vincularResponsavel(ResponsavelJdbcRepository.mapearResponsavel(rs),
                            rs.getString("parentesco"), rs.getBoolean("resp_financeiro"));
                    return null;
                })
                .list();
        return aluno;
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY nome";
    }

    /**
     * Atualiza nome, situação e o responsável financeiro (RD01).
     * Duas etapas para respeitar o índice único parcial: primeiro desmarca todos, depois marca o escolhido.
     * Precisa rodar dentro de transação (@Transactional no service).
     */
    @Override
    public Aluno salvar(Aluno aluno) {
        if (!aluno.estaPersistida()) {
            throw new RegraNegocioException("ALUNO_VIA_PRE_MATRICULA",
                    "Novos alunos são cadastrados pela aprovação da pré-matrícula");
        }
        executar(() -> jdbc.sql("UPDATE aluno SET nome = :nome, ativo = :ativo WHERE id = :id")
                .param("id", aluno.getId()).param("nome", aluno.getNome()).param("ativo", aluno.isAtivo())
                .update());
        executar(() -> jdbc.sql("UPDATE aluno_responsavel SET resp_financeiro = false WHERE aluno_id = :id AND resp_financeiro")
                .param("id", aluno.getId()).update());
        aluno.getResponsavelFinanceiro().ifPresent(r ->
                executar(() -> jdbc.sql("""
                                UPDATE aluno_responsavel SET resp_financeiro = true
                                 WHERE aluno_id = :aluno AND responsavel_id = :responsavel
                                """)
                        .param("aluno", aluno.getId()).param("responsavel", r.getId()).update()));
        return buscarPorId(aluno.getId()).orElseThrow();
    }

    @Override
    public PaginaResponse<AlunoResumoResponse> pesquisar(String busca, int ano, UUID turmaId, UUID professorId,
                                                         int pagina, int tamanho) {
        LocalDate hoje = LocalDate.now(relogio);
        AtomicLong total = new AtomicLong();
        List<AlunoResumoResponse> itens = executar(() -> jdbc.sql("""
                        SELECT a.id, a.numero_cadastro, a.nome, a.data_nascimento, a.ativo,
                               m.id AS matricula_id, t.id AS turma_id, s.nome || ' ' || t.nome AS turma,
                               m.status, m.tipo,
                               (SELECT r.nome FROM aluno_responsavel ar JOIN responsavel r ON r.id = ar.responsavel_id
                                 WHERE ar.aluno_id = a.id AND ar.resp_financeiro) AS responsavel_financeiro,
                               :total - (SELECT count(DISTINCT d.tipo) FROM documento_aluno d
                                 WHERE d.aluno_id = a.id AND d.tipo = ANY(CAST(:obrigatorios AS text[]))) AS documentos_pendentes,
                               count(*) OVER () AS total
                          FROM aluno a
                          JOIN matricula m   ON m.aluno_id = a.id
                          JOIN ano_letivo al ON al.id = m.ano_letivo_id AND al.ano = :ano
                          JOIN turma t       ON t.id = m.turma_id
                          JOIN serie s       ON s.id = t.serie_id
                         WHERE (CAST(:busca AS text) IS NULL
                                OR a.nome ILIKE '%' || CAST(:busca AS text) || '%'
                                OR a.numero_cadastro::text = CAST(:busca AS text))
                           AND (CAST(:turma AS uuid) IS NULL OR t.id = CAST(:turma AS uuid))
                           AND (CAST(:professor AS uuid) IS NULL OR EXISTS (
                                SELECT 1 FROM turma_professor tp
                                 WHERE tp.turma_id = t.id AND tp.funcionario_id = CAST(:professor AS uuid)))
                         ORDER BY a.nome
                         LIMIT :tamanho OFFSET :deslocamento
                        """)
                .param("ano", ano)
                .param("obrigatorios", DocumentosObrigatoriosSql.LISTA)
                .param("total", DocumentosObrigatoriosSql.TOTAL)
                .param("busca", Colunas.termo(busca))
                .param("turma", turmaId)
                .param("professor", professorId)
                .param("tamanho", tamanho)
                .param("deslocamento", pagina * tamanho)
                .query((rs, n) -> {
                    total.set(rs.getLong("total"));
                    LocalDate nascimento = Colunas.data(rs, "data_nascimento");
                    return new AlunoResumoResponse(
                            Colunas.uuid(rs, "id"), rs.getLong("numero_cadastro"), rs.getString("nome"), nascimento,
                            Period.between(nascimento, hoje).getYears(), rs.getBoolean("ativo"),
                            Colunas.uuid(rs, "matricula_id"), Colunas.uuid(rs, "turma_id"), rs.getString("turma"),
                            StatusMatricula.valueOf(rs.getString("status")), TipoMatricula.valueOf(rs.getString("tipo")),
                            rs.getString("responsavel_financeiro"), rs.getInt("documentos_pendentes"));
                })
                .list());
        return new PaginaResponse<>(itens, pagina, tamanho, total.get());
    }

    @Override
    public List<MatriculaHistoricoResponse> listarMatriculas(UUID alunoId) {
        return executar(() -> jdbc.sql("""
                        SELECT m.id, al.ano, t.id AS turma_id, s.nome || ' ' || t.nome AS turma,
                               m.tipo, m.status, m.data_matricula
                          FROM matricula m
                          JOIN ano_letivo al ON al.id = m.ano_letivo_id
                          JOIN turma t       ON t.id = m.turma_id
                          JOIN serie s       ON s.id = t.serie_id
                         WHERE m.aluno_id = :id
                         ORDER BY al.ano DESC
                        """)
                .param("id", alunoId)
                .query((rs, n) -> new MatriculaHistoricoResponse(
                        Colunas.uuid(rs, "id"), rs.getInt("ano"), Colunas.uuid(rs, "turma_id"), rs.getString("turma"),
                        TipoMatricula.valueOf(rs.getString("tipo")), StatusMatricula.valueOf(rs.getString("status")),
                        Colunas.data(rs, "data_matricula")))
                .list());
    }

    @Override
    public boolean estudaComProfessor(UUID alunoId, UUID funcionarioId) {
        return executar(() -> jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM matricula m
                                         JOIN turma_professor tp ON tp.turma_id = m.turma_id
                                        WHERE m.aluno_id = :aluno AND tp.funcionario_id = :professor)
                        """)
                .param("aluno", alunoId).param("professor", funcionarioId)
                .query(Boolean.class).single());
    }
}
