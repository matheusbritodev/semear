package br.undb.semear.repository.financeiro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.entity.financeiro.ConcessaoBolsa;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class BolsaJdbcRepository extends JdbcRepository<Bolsa> implements BolsaRepository {

    private static final String SELECT_CONCESSAO = """
            SELECT bm.id AS concessao_id, bm.bolsa_id, bm.matricula_id, bm.vigencia_inicio, bm.vigencia_fim, bm.concedida_por,
                   b.id, b.nome, b.descricao, b.tipo_desconto, b.valor, b.ativa,
                   a.id AS aluno_id, a.nome AS aluno, al.ano, s.nome || ' ' || t.nome AS turma
              FROM bolsa_matricula bm
              JOIN bolsa b       ON b.id = bm.bolsa_id
              JOIN matricula m   ON m.id = bm.matricula_id
              JOIN ano_letivo al ON al.id = m.ano_letivo_id
              JOIN aluno a       ON a.id = m.aluno_id
              JOIN turma t       ON t.id = m.turma_id
              JOIN serie s       ON s.id = t.serie_id
            """;

    public BolsaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "bolsa");
    }

    @Override
    protected Bolsa mapear(ResultSet rs, int n) throws SQLException {
        return new Bolsa(Colunas.uuid(rs, "id"), rs.getString("nome"), rs.getString("descricao"),
                TipoDesconto.valueOf(rs.getString("tipo_desconto")), rs.getBigDecimal("valor"), rs.getBoolean("ativa"));
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY ativa DESC, nome";
    }

    @Override
    public Bolsa salvar(Bolsa bolsa) {
        UUID id = executar(() -> jdbc.sql(bolsa.estaPersistida()
                        ? """
                          UPDATE bolsa SET nome = :nome, descricao = :descricao,
                                 tipo_desconto = CAST(:tipo AS tipo_desconto), valor = :valor, ativa = :ativa
                           WHERE id = :id RETURNING id
                          """
                        : """
                          INSERT INTO bolsa (nome, descricao, tipo_desconto, valor, ativa)
                          VALUES (:nome, :descricao, CAST(:tipo AS tipo_desconto), :valor, :ativa) RETURNING id
                          """)
                .param("id", bolsa.getId()).param("nome", bolsa.getNome()).param("descricao", bolsa.getDescricao())
                .param("tipo", bolsa.getTipoDesconto().name()).param("valor", bolsa.getValor())
                .param("ativa", bolsa.isAtiva())
                .query(UUID.class).single());
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public int bolsistasVigentes(UUID bolsaId, LocalDate hoje) {
        return executar(() -> jdbc.sql("""
                        SELECT count(*) FROM bolsa_matricula bm JOIN matricula m ON m.id = bm.matricula_id
                         WHERE bm.bolsa_id = :bolsa AND m.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')
                           AND bm.vigencia_inicio <= :hoje AND (bm.vigencia_fim IS NULL OR bm.vigencia_fim >= :hoje)
                        """)
                .param("bolsa", bolsaId).param("hoje", hoje).query(Integer.class).single());
    }

    @Override
    public List<ConcessaoComContexto> concessoesDaMatricula(UUID matriculaId) {
        return executar(() -> jdbc.sql(SELECT_CONCESSAO + " WHERE bm.matricula_id = :id ORDER BY bm.vigencia_inicio DESC")
                .param("id", matriculaId).query(this::mapearConcessao).list());
    }

    @Override
    public List<ConcessaoComContexto> bolsistas(int ano) {
        return executar(() -> jdbc.sql(SELECT_CONCESSAO + " WHERE al.ano = :ano ORDER BY a.nome, b.nome")
                .param("ano", ano).query(this::mapearConcessao).list());
    }

    @Override
    public Optional<ConcessaoBolsa> buscarConcessao(UUID concessaoId) {
        return executar(() -> jdbc.sql(SELECT_CONCESSAO + " WHERE bm.id = :id").param("id", concessaoId)
                .query((rs, n) -> mapearConcessao(rs, n).concessao()).optional());
    }

    @Override
    public UUID conceder(ConcessaoBolsa c) {
        return executar(() -> jdbc.sql("""
                        INSERT INTO bolsa_matricula (bolsa_id, matricula_id, vigencia_inicio, vigencia_fim, concedida_por)
                        VALUES (:bolsa, :matricula, :inicio, :fim, :por) RETURNING id
                        """)
                .param("bolsa", c.getBolsaId()).param("matricula", c.getMatriculaId())
                .param("inicio", c.getVigenciaInicio()).param("fim", c.getVigenciaFim())
                .param("por", c.getConcedidaPor())
                .query(UUID.class).single());
    }

    @Override
    public void encerrarConcessao(UUID concessaoId, LocalDate fim) {
        executar(() -> jdbc.sql("UPDATE bolsa_matricula SET vigencia_fim = :fim WHERE id = :id")
                .param("id", concessaoId).param("fim", fim).update());
    }

    @Override
    public void apagarConcessao(UUID concessaoId) {
        executar(() -> jdbc.sql("DELETE FROM bolsa_matricula WHERE id = :id").param("id", concessaoId).update());
    }

    private ConcessaoComContexto mapearConcessao(ResultSet rs, int n) throws SQLException {
        ConcessaoBolsa concessao = new ConcessaoBolsa(Colunas.uuid(rs, "concessao_id"), Colunas.uuid(rs, "bolsa_id"),
                Colunas.uuid(rs, "matricula_id"), Colunas.data(rs, "vigencia_inicio"), Colunas.data(rs, "vigencia_fim"),
                Colunas.uuid(rs, "concedida_por"));
        return new ConcessaoComContexto(concessao, mapear(rs, n), Colunas.uuid(rs, "aluno_id"), rs.getString("aluno"),
                rs.getInt("ano"), rs.getString("turma"));
    }
}
