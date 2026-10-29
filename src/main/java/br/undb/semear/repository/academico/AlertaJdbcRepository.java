package br.undb.semear.repository.academico;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.academico.AcademicoDtos.AlertaResponse;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class AlertaJdbcRepository implements AlertaRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public AlertaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public List<AlertaResponse> listar(boolean apenasNaoLidos) {
        try {
            return jdbc.sql("""
                            SELECT id, tipo, mensagem, turma_id, lido, criado_em
                              FROM alerta_secretaria
                             WHERE NOT :apenasNaoLidos OR NOT lido
                             ORDER BY criado_em DESC
                             LIMIT 50
                            """)
                    .param("apenasNaoLidos", apenasNaoLidos)
                    .query((rs, n) -> new AlertaResponse(Colunas.uuid(rs, "id"), rs.getString("tipo"),
                            rs.getString("mensagem"), Colunas.uuid(rs, "turma_id"), rs.getBoolean("lido"),
                            rs.getObject("criado_em", OffsetDateTime.class)))
                    .list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public boolean marcarComoLido(UUID id) {
        try {
            return jdbc.sql("UPDATE alerta_secretaria SET lido = true WHERE id = :id").param("id", id).update() > 0;
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
