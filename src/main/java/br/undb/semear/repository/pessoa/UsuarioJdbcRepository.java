package br.undb.semear.repository.pessoa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.pessoa.AdministracaoContas;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Permissao;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

/** Persistência de usuários na tabela "usuario" (id = auth.users.id). */
@Repository
public class UsuarioJdbcRepository extends JdbcRepository<Usuario> implements UsuarioRepository {

    public UsuarioJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "usuario");
    }

    @Override
    protected Usuario mapear(ResultSet linha, int numeroLinha) throws SQLException {
        PerfilUsuario perfil = PerfilUsuario.valueOf(linha.getString("perfil"));
        return new Usuario(
                linha.getObject("id", UUID.class),
                new Email(linha.getString("email")),
                perfil,
                linha.getBoolean("ativo"),
                linha.getObject("ultimo_login", OffsetDateTime.class),
                listarPermissoes(perfil));
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        executar(() -> jdbc.sql("""
                        INSERT INTO usuario (id, email, perfil, ativo)
                        VALUES (:id, :email, :perfil, :ativo)
                        ON CONFLICT (id) DO UPDATE
                           SET email = EXCLUDED.email, perfil = EXCLUDED.perfil, ativo = EXCLUDED.ativo
                        """)
                .param("id", usuario.getId())
                .param("email", usuario.getEmail().getEndereco())
                .param("perfil", usuario.getPerfil().name())
                .param("ativo", usuario.isAtivo())
                .update());
        return buscarPorId(usuario.getId()).orElseThrow();
    }

    @Override
    public Optional<String> buscarNomeExibicao(UUID usuarioId) {
        return executar(() -> jdbc.sql("""
                        SELECT nome FROM funcionario WHERE usuario_id = :id
                        UNION ALL
                        SELECT nome FROM responsavel WHERE usuario_id = :id
                        UNION ALL
                        SELECT nome FROM aluno       WHERE usuario_id = :id
                        LIMIT 1
                        """)
                .param("id", usuarioId)
                .query(String.class)
                .optional());
    }

    @Override
    public List<Permissao> listarPermissoes(PerfilUsuario perfil) {
        return executar(() -> jdbc.sql("""
                        SELECT modulo, pode_ler, pode_gravar
                          FROM perfil_permissao
                         WHERE perfil = :perfil
                        """)
                .param("perfil", perfil.name())
                .query((linha, n) -> new Permissao(
                        Modulo.valueOf(linha.getString("modulo")),
                        linha.getBoolean("pode_ler"),
                        linha.getBoolean("pode_gravar")))
                .list());
    }

    private static final String SELECT_CONTA = """
            SELECT u.*,
                   COALESCE(f.nome, r.nome, a.nome) AS nome, f.tipo AS tipo_funcionario,
                   CASE WHEN f.id IS NOT NULL THEN 'FUNCIONARIO' WHEN r.id IS NOT NULL THEN 'RESPONSAVEL'
                        WHEN a.id IS NOT NULL THEN 'ALUNO' ELSE 'NENHUM' END AS vinculo
              FROM usuario u
              LEFT JOIN funcionario f ON f.usuario_id = u.id
              LEFT JOIN responsavel r ON r.usuario_id = u.id
              LEFT JOIN aluno a ON a.usuario_id = u.id
            """;

    @Override
    public List<ContaComVinculo> listarContas() {
        return executar(() -> jdbc.sql(SELECT_CONTA + " ORDER BY (u.perfil = 'RESPONSAVEL'), u.perfil, nome NULLS LAST, u.email")
                .query(this::mapearConta).list());
    }

    @Override
    public Optional<ContaComVinculo> buscarConta(UUID usuarioId) {
        return executar(() -> jdbc.sql(SELECT_CONTA + " WHERE u.id = :id").param("id", usuarioId)
                .query(this::mapearConta).optional());
    }

    @Override
    public long contarAdminsAtivos() {
        return executar(() -> jdbc.sql("SELECT count(*) FROM usuario WHERE perfil = 'ADMIN' AND ativo")
                .query(Long.class).single());
    }

    @Override
    public boolean vincularFuncionario(UUID funcionarioId, UUID usuarioId) {
        return executar(() -> jdbc.sql("UPDATE funcionario SET usuario_id = :usuario WHERE id = :id AND usuario_id IS NULL")
                .param("id", funcionarioId).param("usuario", usuarioId).update()) == 1;
    }

    private ContaComVinculo mapearConta(ResultSet rs, int n) throws SQLException {
        String tipo = rs.getString("tipo_funcionario");
        return new ContaComVinculo(mapear(rs, n), rs.getObject("criado_em", OffsetDateTime.class),
                AdministracaoContas.Vinculo.valueOf(rs.getString("vinculo")), rs.getString("nome"),
                tipo == null ? null : TipoFuncionario.valueOf(tipo));
    }
}
