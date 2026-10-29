package br.undb.semear.repository.matricula;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.pessoa.PendenciaDtos.PendenciaDocumentoResponse;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class PendenciaJdbcRepository implements PendenciaRepository {

    /** Uma linha por aluno: a matrícula vigente mais recente (o ano seguinte, se já houver). */
    private static final String MATRICULAS_VIGENTES = """
            SELECT DISTINCT ON (m.aluno_id) m.aluno_id, m.turma_id, m.status, al.ano
              FROM matricula m JOIN ano_letivo al ON al.id = m.ano_letivo_id
             WHERE m.status IN ('AGUARDANDO_PAGAMENTO', 'ATIVA') AND al.ano >= :anoMinimo
             ORDER BY m.aluno_id, al.ano DESC
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public PendenciaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public List<PendenciaDocumentoResponse> documentosPendentes(int anoMinimo) {
        try {
            return jdbc.sql("""
                            WITH vigentes AS (""" + MATRICULAS_VIGENTES + """
                            )
                            SELECT a.id, a.numero_cadastro, a.nome, v.ano, v.turma_id, s.nome || ' ' || t.nome AS turma, v.status,
                                   ARRAY(SELECT DISTINCT d.tipo FROM documento_aluno d
                                          WHERE d.aluno_id = a.id AND d.tipo = ANY(CAST(:obrigatorios AS text[]))) AS entregues,
                                   r.nome AS responsavel, r.telefone
                              FROM vigentes v
                              JOIN aluno a ON a.id = v.aluno_id
                              JOIN turma t ON t.id = v.turma_id
                              JOIN serie s ON s.id = t.serie_id
                              LEFT JOIN aluno_responsavel ar ON ar.aluno_id = a.id AND ar.resp_financeiro
                              LEFT JOIN responsavel r ON r.id = ar.responsavel_id
                             WHERE """ + " " + DocumentosObrigatoriosSql.entreguesDo("a.id") + " < :total\n"
                            + " ORDER BY " + DocumentosObrigatoriosSql.entreguesDo("a.id") + ", a.nome")
                    .param("anoMinimo", anoMinimo)
                    .param("obrigatorios", DocumentosObrigatoriosSql.LISTA)
                    .param("total", DocumentosObrigatoriosSql.TOTAL)
                    .query((rs, n) -> {
                        Set<String> entregues = Set.copyOf(Arrays.asList(lerArray(rs.getArray("entregues"))));
                        List<String> faltando = Arrays.stream(TipoDocumentoAluno.values())
                                .filter(t -> t.isObrigatorio() && !entregues.contains(t.name()))
                                .map(TipoDocumentoAluno::getRotulo)
                                .toList();
                        return new PendenciaDocumentoResponse(Colunas.uuid(rs, "id"), rs.getLong("numero_cadastro"),
                                rs.getString("nome"), rs.getInt("ano"), Colunas.uuid(rs, "turma_id"), rs.getString("turma"),
                                StatusMatricula.valueOf(rs.getString("status")), faltando.size(), faltando,
                                rs.getString("responsavel"), Telefone.formatar(rs.getString("telefone")));
                    })
                    .list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public int alunosComMatriculaVigente(int anoMinimo) {
        try {
            return jdbc.sql("SELECT count(*) FROM (" + MATRICULAS_VIGENTES + ") v")
                    .param("anoMinimo", anoMinimo).query(Integer.class).single();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    private static String[] lerArray(Array array) throws java.sql.SQLException {
        return array == null ? new String[0] : (String[]) array.getArray();
    }
}
