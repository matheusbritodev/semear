package br.undb.semear.repository.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.undb.semear.model.entity.Entidade;

/**
 * Base dos repositórios JDBC (padrão Template Method): implementa as operações comuns
 * e deixa para as subclasses o mapeamento da linha e a gravação.
 * Subclasses com junções sobrescrevem {@link #selectBase()} e {@link #colunaId()}.
 * Toda exceção do banco sai daqui já traduzida em exceção do domínio.
 */
public abstract class JdbcRepository<T extends Entidade> implements Repository<T, UUID> {

    protected final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;
    private final String tabela;

    protected JdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor, String tabela) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
        this.tabela = tabela;
    }

    /** Converte uma linha do ResultSet na entidade. */
    protected abstract T mapear(ResultSet linha, int numeroLinha) throws SQLException;

    /** SELECT usado por buscarPorId e listar (sem WHERE). */
    protected String selectBase() {
        return "SELECT * FROM " + tabela;
    }

    /** Coluna do id no selectBase (ex.: "t.id" quando há alias). */
    protected String colunaId() {
        return "id";
    }

    /** Ordenação de listar(). */
    protected String ordenacao() {
        return "";
    }

    @Override
    public Optional<T> buscarPorId(UUID id) {
        return executar(() -> jdbc.sql(selectBase() + " WHERE " + colunaId() + " = :id")
                .param("id", id)
                .query(this::mapear)
                .optional());
    }

    @Override
    public List<T> listar() {
        return executar(() -> jdbc.sql(selectBase() + " " + ordenacao()).query(this::mapear).list());
    }

    @Override
    public void remover(UUID id) {
        executar(() -> jdbc.sql("DELETE FROM " + tabela + " WHERE id = :id").param("id", id).update());
    }

    /** Executa uma operação no banco traduzindo erros para exceções do domínio. */
    protected <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
