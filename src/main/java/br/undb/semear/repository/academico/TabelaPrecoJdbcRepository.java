package br.undb.semear.repository.academico;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.academico.TabelaPreco;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class TabelaPrecoJdbcRepository implements TabelaPrecoRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public TabelaPrecoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public List<TabelaPreco> doAno(int ano) {
        return executar(() -> jdbc.sql("""
                        SELECT tp.*, al.ano FROM tabela_preco tp JOIN ano_letivo al ON al.id = tp.ano_letivo_id
                         WHERE al.ano = :ano
                        """)
                .param("ano", ano)
                .query((rs, n) -> new TabelaPreco(Colunas.uuid(rs, "serie_id"), rs.getInt("ano"),
                        Dinheiro.de(rs.getBigDecimal("valor_matricula")), Dinheiro.de(rs.getBigDecimal("valor_mensal")),
                        rs.getInt("dia_vencimento"), rs.getInt("dias_prazo_matricula")))
                .list());
    }

    @Override
    public void salvar(TabelaPreco p) {
        int linhas = executar(() -> jdbc.sql("""
                        INSERT INTO tabela_preco (serie_id, ano_letivo_id, valor_matricula, valor_mensal, dia_vencimento, dias_prazo_matricula)
                        SELECT :serie, al.id, :matricula, :mensal, :dia, :prazo FROM ano_letivo al WHERE al.ano = :ano
                        ON CONFLICT (serie_id, ano_letivo_id) DO UPDATE
                           SET valor_matricula = EXCLUDED.valor_matricula, valor_mensal = EXCLUDED.valor_mensal,
                               dia_vencimento = EXCLUDED.dia_vencimento, dias_prazo_matricula = EXCLUDED.dias_prazo_matricula
                        """)
                .param("serie", p.getSerieId()).param("ano", p.getAno())
                .param("matricula", p.getValorMatricula().getValor()).param("mensal", p.getValorMensal().getValor())
                .param("dia", p.getDiaVencimento()).param("prazo", p.getDiasPrazoMatricula())
                .update());
        if (linhas == 0) {
            throw new RegraNegocioException("ANO_LETIVO_INEXISTENTE", "Cadastre o ano letivo " + p.getAno());
        }
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
