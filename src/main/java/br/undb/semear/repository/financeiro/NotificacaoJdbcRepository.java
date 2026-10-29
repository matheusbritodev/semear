package br.undb.semear.repository.financeiro;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.model.entity.financeiro.NotificacaoCobranca;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class NotificacaoJdbcRepository implements NotificacaoRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public NotificacaoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    @Transactional
    public int processarPendentes(int limite, Function<NotificacaoCobranca, Boolean> envio) {
        List<NotificacaoCobranca> lote = executar(() -> jdbc.sql("""
                        SELECT n.id, n.cobranca_id, n.tipo, n.canal, r.nome AS responsavel, r.email, r.telefone,
                               a.nome AS aluno, c.vencimento, c.valor_final
                          FROM notificacao_cobranca n
                          JOIN cobranca c    ON c.id = n.cobranca_id
                          JOIN responsavel r ON r.id = n.responsavel_id
                          JOIN matricula m   ON m.id = c.matricula_id
                          JOIN aluno a       ON a.id = m.aluno_id
                         WHERE n.status = 'PENDENTE'
                         ORDER BY n.criada_em
                         LIMIT :limite
                           FOR UPDATE OF n SKIP LOCKED
                        """)
                .param("limite", limite)
                .query((rs, i) -> new NotificacaoCobranca(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "cobranca_id"),
                        rs.getString("tipo"), rs.getString("canal"), rs.getString("responsavel"), rs.getString("email"),
                        rs.getString("telefone"), rs.getString("aluno"), Colunas.data(rs, "vencimento"),
                        Dinheiro.de(rs.getBigDecimal("valor_final"))))
                .list());
        int enviadas = 0;
        for (NotificacaoCobranca n : lote) {
            boolean ok = Boolean.TRUE.equals(envio.apply(n));
            enviadas += ok ? 1 : 0;
            executar(() -> jdbc.sql("""
                            UPDATE notificacao_cobranca
                               SET status = CAST(:status AS status_notificacao),
                                   enviada_em = CASE WHEN :ok THEN now() END
                             WHERE id = :id
                            """)
                    .param("id", n.id()).param("status", ok ? "ENVIADA" : "FALHA").param("ok", ok)
                    .update());
        }
        return enviadas;
    }

    @Override
    public void rotinaDiaria() {
        executar(() -> jdbc.sql("SELECT fn_rotina_diaria()").query().singleRow());
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
