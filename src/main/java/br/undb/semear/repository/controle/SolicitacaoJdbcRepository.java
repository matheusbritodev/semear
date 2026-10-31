package br.undb.semear.repository.controle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.undb.semear.model.entity.controle.SolicitacaoAprovacao;
import br.undb.semear.model.entity.controle.SolicitacaoAprovacao.Decisao;
import br.undb.semear.model.entity.controle.SolicitacaoFactory;
import br.undb.semear.model.enums.StatusSolicitacao;
import br.undb.semear.model.enums.TipoSolicitacao;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.SemearException;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class SolicitacaoJdbcRepository implements SolicitacaoRepository {

    private static final TypeReference<Map<String, String>> MAPA = new TypeReference<>() {
    };

    /** Nome de exibição: o do cadastro de funcionário, senão o e-mail do login. */
    private static final String SELECT = """
            SELECT s.*, s.dados::text AS dados_texto,
                   coalesce(fs.nome, us.email) AS nome_solicitante,
                   coalesce(fd.nome, ud.email) AS nome_decisor
              FROM solicitacao_aprovacao s
              JOIN usuario us ON us.id = s.solicitada_por
              LEFT JOIN funcionario fs ON fs.usuario_id = s.solicitada_por
              LEFT JOIN usuario ud ON ud.id = s.decidida_por
              LEFT JOIN funcionario fd ON fd.usuario_id = s.decidida_por
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;
    private final ObjectMapper json;

    public SolicitacaoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor, ObjectMapper json) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
        this.json = json;
    }

    @Override
    public UUID inserir(SolicitacaoAprovacao s, UUID matriculaId) {
        try {
            return jdbc.sql("""
                            INSERT INTO solicitacao_aprovacao (tipo, resumo, dados, chave, matricula_id, solicitada_por)
                            VALUES (CAST(:tipo AS tipo_solicitacao), :resumo, CAST(:dados AS jsonb), :chave, :matricula, :por)
                            RETURNING id
                            """)
                    .param("tipo", s.getTipo().name()).param("resumo", s.getResumo())
                    .param("dados", json.writeValueAsString(s.dados())).param("chave", s.chave())
                    .param("matricula", matriculaId).param("por", s.getSolicitadaPor())
                    .query(UUID.class).single();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Dados do pedido inválidos", e);
        } catch (DataAccessException e) {
            SemearException erro = tradutor.traduzir(e);
            if ("DUPLICADO".equals(erro.getCodigo())) {
                throw new ConflitoException("SOLICITACAO_JA_PENDENTE",
                        "Já existe um pedido igual esperando aprovação", e);
            }
            throw erro;
        }
    }

    @Override
    public Optional<SolicitacaoAprovacao> buscarParaDecidir(UUID id) {
        try {
            return jdbc.sql("SELECT s.*, s.dados::text AS dados_texto FROM solicitacao_aprovacao s WHERE s.id = :id FOR UPDATE")
                    .param("id", id).query(this::mapear).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public Optional<SolicitacaoComNomes> buscar(UUID id) {
        try {
            return jdbc.sql(SELECT + " WHERE s.id = :id").param("id", id).query(this::mapearComNomes).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public void registrarDecisao(SolicitacaoAprovacao s) {
        Decisao d = s.getDecisao();
        try {
            jdbc.sql("""
                            UPDATE solicitacao_aprovacao
                               SET status = CAST(:status AS status_solicitacao), decidida_por = :por,
                                   decidida_em = :em, motivo = :motivo
                             WHERE id = :id AND status = 'PENDENTE'
                            """)
                    .param("status", d.status().name()).param("por", d.decididaPor()).param("em", d.decididaEm())
                    .param("motivo", d.motivo()).param("id", s.getId())
                    .update();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public List<SolicitacaoComNomes> listar(StatusSolicitacao status, int limite) {
        try {
            return jdbc.sql(SELECT + """
                             WHERE (CAST(:status AS status_solicitacao) IS NULL OR s.status = CAST(:status AS status_solicitacao))
                             ORDER BY s.solicitada_em DESC
                             LIMIT :limite
                            """)
                    .param("status", status == null ? null : status.name()).param("limite", limite)
                    .query(this::mapearComNomes).list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public long contarPendentes() {
        try {
            return jdbc.sql("SELECT count(*) FROM solicitacao_aprovacao WHERE status = 'PENDENTE'").query(Long.class).single();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    private SolicitacaoComNomes mapearComNomes(ResultSet rs, int n) throws SQLException {
        return new SolicitacaoComNomes(mapear(rs, n), rs.getString("nome_solicitante"), rs.getString("nome_decisor"));
    }

    private SolicitacaoAprovacao mapear(ResultSet rs, int n) throws SQLException {
        try {
            Map<String, String> dados = json.readValue(rs.getString("dados_texto"), MAPA);
            Decisao decisao = new Decisao(StatusSolicitacao.valueOf(rs.getString("status")),
                    rs.getObject("decidida_por", UUID.class), rs.getObject("decidida_em", OffsetDateTime.class),
                    rs.getString("motivo"));
            return SolicitacaoFactory.criar(rs.getObject("id", UUID.class), TipoSolicitacao.valueOf(rs.getString("tipo")),
                    rs.getString("resumo"), rs.getObject("solicitada_por", UUID.class),
                    rs.getObject("solicitada_em", OffsetDateTime.class), decisao, dados);
        } catch (JsonProcessingException e) {
            throw new SQLException("Dados do pedido ilegíveis", e);
        }
    }
}
