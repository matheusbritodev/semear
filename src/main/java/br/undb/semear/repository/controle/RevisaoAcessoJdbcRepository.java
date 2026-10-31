package br.undb.semear.repository.controle;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.controle.RevisaoAcesso;
import br.undb.semear.model.entity.pessoa.AdministracaoContas.Vinculo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class RevisaoAcessoJdbcRepository implements RevisaoAcessoRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public RevisaoAcessoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public List<RevisaoAcesso.Conta> contasDaEquipe() {
        try {
            // Último acesso: o do Supabase Auth (login real); usuario.ultimo_login como reserva.
            return jdbc.sql("""
                            SELECT u.id, u.email, u.perfil, u.ativo, f.nome, f.ativo AS funcionario_ativo,
                                   CASE WHEN f.id IS NOT NULL THEN 'FUNCIONARIO' ELSE 'NENHUM' END AS vinculo,
                                   coalesce(au.last_sign_in_at, u.ultimo_login) AS ultimo_acesso
                              FROM usuario u
                              LEFT JOIN funcionario f ON f.usuario_id = u.id
                              LEFT JOIN auth.users au ON au.id = u.id
                             WHERE u.perfil NOT IN ('RESPONSAVEL', 'ALUNO')
                             ORDER BY u.ativo DESC, u.perfil, f.nome NULLS LAST, u.email
                            """)
                    .query((rs, n) -> new RevisaoAcesso.Conta(rs.getObject("id", UUID.class), rs.getString("email"),
                            rs.getString("nome"), PerfilUsuario.valueOf(rs.getString("perfil")), rs.getBoolean("ativo"),
                            Vinculo.valueOf(rs.getString("vinculo")), (Boolean) rs.getObject("funcionario_ativo"),
                            rs.getObject("ultimo_acesso", OffsetDateTime.class)))
                    .list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public long contasDoPortalAtivas() {
        try {
            return jdbc.sql("SELECT count(*) FROM usuario WHERE perfil IN ('RESPONSAVEL', 'ALUNO') AND ativo")
                    .query(Long.class).single();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public UUID salvar(UUID realizadaPor, int contasRevisadas, int alertas, String observacao, String resultadoJson) {
        try {
            return jdbc.sql("""
                            INSERT INTO revisao_acesso (realizada_por, contas_revisadas, alertas, observacao, resultado)
                            VALUES (:por, :contas, :alertas, :obs, CAST(:resultado AS jsonb))
                            RETURNING id
                            """)
                    .param("por", realizadaPor).param("contas", contasRevisadas).param("alertas", alertas)
                    .param("obs", observacao).param("resultado", resultadoJson)
                    .query(UUID.class).single();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public List<RevisaoRealizada> listar() {
        try {
            return jdbc.sql("""
                            SELECT r.id, r.realizada_em, coalesce(f.nome, u.email) AS por, r.contas_revisadas, r.alertas,
                                   r.observacao, (r.resultado -> 'ancora' ->> 'id')::bigint AS ancora_id,
                                   r.resultado -> 'ancora' ->> 'hash' AS ancora_hash
                              FROM revisao_acesso r
                              JOIN usuario u ON u.id = r.realizada_por
                              LEFT JOIN funcionario f ON f.usuario_id = r.realizada_por
                             ORDER BY r.realizada_em DESC
                            """)
                    .query((rs, n) -> new RevisaoRealizada(rs.getObject("id", UUID.class),
                            rs.getObject("realizada_em", OffsetDateTime.class), rs.getString("por"),
                            rs.getInt("contas_revisadas"), rs.getInt("alertas"), rs.getString("observacao"),
                            (Long) rs.getObject("ancora_id"), rs.getString("ancora_hash")))
                    .list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
