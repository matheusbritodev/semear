package br.undb.semear.repository.pessoa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.pessoa.DadosFuncionario;
import br.undb.semear.model.entity.pessoa.Funcionario;
import br.undb.semear.model.entity.pessoa.FuncionarioFactory;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

/** A coluna "tipo" decide, pela FuncionarioFactory, qual subclasse de Funcionario é criada. */
@Repository
public class FuncionarioJdbcRepository extends JdbcRepository<Funcionario> implements FuncionarioRepository {

    public FuncionarioJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "funcionario");
    }

    @Override
    protected Funcionario mapear(ResultSet linha, int numeroLinha) throws SQLException {
        DadosFuncionario dados = new DadosFuncionario(
                Colunas.uuid(linha, "id"),
                linha.getString("nome"),
                Colunas.cpf(linha, "cpf"),
                Colunas.telefone(linha, "telefone"),
                Colunas.email(linha, "email"),
                Colunas.data(linha, "data_admissao"),
                linha.getBoolean("ativo"),
                Colunas.uuid(linha, "usuario_id"));
        return FuncionarioFactory.criar(TipoFuncionario.valueOf(linha.getString("tipo")), dados);
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY nome";
    }

    @Override
    public Funcionario salvar(Funcionario f) {
        String telefone = f.getTelefone().map(Telefone::getDigitos).orElse(null);
        String email = f.getEmail().map(e -> e.getEndereco()).orElse(null);
        if (!f.estaPersistida()) {
            UUID id = executar(() -> jdbc.sql("""
                            INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao, ativo)
                            VALUES (:nome, :cpf, :tipo, :telefone, :email, :admissao, :ativo)
                            RETURNING id
                            """)
                    .param("nome", f.getNome())
                    .param("cpf", f.getCpf().orElseThrow().getDigitos())
                    .param("tipo", f.getTipo().name())
                    .param("telefone", telefone)
                    .param("email", email)
                    .param("admissao", f.getDataAdmissao().orElse(null))
                    .param("ativo", f.isAtivo())
                    .query(UUID.class).single());
            return buscarPorId(id).orElseThrow();
        }
        executar(() -> jdbc.sql("""
                        UPDATE funcionario
                           SET nome = :nome, telefone = :telefone, email = :email,
                               data_admissao = :admissao, ativo = :ativo
                         WHERE id = :id
                        """)
                .param("id", f.getId())
                .param("nome", f.getNome())
                .param("telefone", telefone)
                .param("email", email)
                .param("admissao", f.getDataAdmissao().orElse(null))
                .param("ativo", f.isAtivo())
                .update());
        return buscarPorId(f.getId()).orElseThrow();
    }

    @Override
    public List<Funcionario> pesquisar(String busca, TipoFuncionario tipo) {
        return executar(() -> jdbc.sql("""
                        SELECT * FROM funcionario
                         WHERE (CAST(:busca AS text) IS NULL
                                OR nome ILIKE '%' || CAST(:busca AS text) || '%'
                                OR cpf = regexp_replace(CAST(:busca AS text), '\\D', '', 'g'))
                           AND (CAST(:tipo AS text) IS NULL OR tipo = CAST(:tipo AS tipo_funcionario))
                         ORDER BY ativo DESC, nome
                        """)
                .param("busca", Colunas.termo(busca))
                .param("tipo", tipo == null ? null : tipo.name())
                .query(this::mapear)
                .list());
    }

    @Override
    public Optional<UUID> buscarIdPorUsuario(UUID usuarioId) {
        return executar(() -> jdbc.sql("SELECT id FROM funcionario WHERE usuario_id = :usuario")
                .param("usuario", usuarioId)
                .query(UUID.class).optional());
    }
}
