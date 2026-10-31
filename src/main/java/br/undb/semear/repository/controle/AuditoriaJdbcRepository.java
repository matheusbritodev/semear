package br.undb.semear.repository.controle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.model.entity.controle.RegistroAuditoria;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class AuditoriaJdbcRepository implements AuditoriaRepository {

    /** Mesmos textos que entraram no hash (fn_auditoria_conteudo): data em UTC e jsonb como texto. */
    private static final String COLUNAS = """
            a.id, a.ocorrido_em, to_char(a.ocorrido_em AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US') AS ocorrido_utc,
            a.usuario_id, a.papel_banco, a.tabela, a.operacao, a.registro_id,
            a.dados_antes::text AS antes, a.dados_depois::text AS depois, a.hash_anterior, a.hash
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public AuditoriaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public PaginaResponse<LinhaAuditoria> pesquisar(Filtro f) {
        AtomicLong total = new AtomicLong();
        try {
            List<LinhaAuditoria> itens = jdbc.sql("SELECT " + COLUNAS + """
                            , count(*) OVER () AS total_linhas, coalesce(fu.nome, u.email) AS autor
                              FROM auditoria a
                              LEFT JOIN usuario u ON u.id = a.usuario_id
                              LEFT JOIN funcionario fu ON fu.usuario_id = a.usuario_id
                             WHERE (CAST(:tabela AS text) IS NULL OR a.tabela = :tabela)
                               AND (CAST(:usuario AS uuid) IS NULL OR a.usuario_id = :usuario)
                               AND (CAST(:registro AS text) IS NULL OR a.registro_id = :registro)
                               AND (CAST(:de AS date) IS NULL OR a.ocorrido_em >= CAST(:de AS date))
                               AND (CAST(:ate AS date) IS NULL OR a.ocorrido_em < CAST(:ate AS date) + 1)
                             ORDER BY a.id DESC
                             LIMIT :tamanho OFFSET :deslocamento
                            """)
                    .param("tabela", f.tabela()).param("usuario", f.usuarioId()).param("registro", f.registroId())
                    .param("de", f.de()).param("ate", f.ate())
                    .param("tamanho", f.tamanho()).param("deslocamento", f.pagina() * f.tamanho())
                    .query((rs, n) -> {
                        total.set(rs.getLong("total_linhas"));
                        return new LinhaAuditoria(mapear(rs, n), rs.getObject("ocorrido_em", OffsetDateTime.class),
                                rs.getString("autor"));
                    }).list();
            return new PaginaResponse<>(itens, f.pagina(), f.tamanho(), total.get());
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public List<RegistroAuditoria> lote(long depoisDoId, int limite) {
        try {
            return jdbc.sql("SELECT " + COLUNAS + " FROM auditoria a WHERE a.id > :id ORDER BY a.id LIMIT :limite")
                    .param("id", depoisDoId).param("limite", limite).query(this::mapear).list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public Optional<String> hashDe(long id) {
        try {
            return jdbc.sql("SELECT hash FROM auditoria WHERE id = :id").param("id", id).query(String.class).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public Optional<RegistroAuditoria> ultimo() {
        try {
            return jdbc.sql("SELECT " + COLUNAS + " FROM auditoria a ORDER BY a.id DESC LIMIT 1").query(this::mapear).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public List<Autor> autores() {
        try {
            return jdbc.sql("""
                            SELECT DISTINCT a.usuario_id, coalesce(fu.nome, u.email, 'Login removido') AS nome
                              FROM auditoria a
                              LEFT JOIN usuario u ON u.id = a.usuario_id
                              LEFT JOIN funcionario fu ON fu.usuario_id = a.usuario_id
                             WHERE a.usuario_id IS NOT NULL
                             ORDER BY nome
                            """)
                    .query((rs, n) -> new Autor(rs.getObject("usuario_id", UUID.class), rs.getString("nome"))).list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    private RegistroAuditoria mapear(ResultSet rs, int n) throws SQLException {
        return new RegistroAuditoria(rs.getLong("id"), rs.getString("ocorrido_utc"), rs.getObject("usuario_id", UUID.class),
                rs.getString("papel_banco"), rs.getString("tabela"), rs.getString("operacao"), rs.getString("registro_id"),
                rs.getString("antes"), rs.getString("depois"), rs.getString("hash_anterior"), rs.getString("hash"));
    }
}
