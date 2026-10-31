package br.undb.semear.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Avisa o banco quem é o usuário da requisição, a cada conexão tirada do pool:
 * {@code set_config('semear.usuario_id', <id do login>)}. O trigger de auditoria (fn_auditar) lê esse valor
 * para registrar a autoria. Rotinas automáticas e o webhook do banco Cora não têm login: ficam em branco ("Sistema").
 * <p>
 * Usa o "sub" do JWT direto (sem consultar o banco), para não abrir outra conexão dentro desta.
 */
@Component
public class DataSourceComUsuario implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String nome) {
        if (bean instanceof DataSource ds && !(bean instanceof ComUsuario)) {
            return new ComUsuario(ds);
        }
        return bean;
    }

    static final class ComUsuario extends DelegatingDataSource {

        /**
         * Último login informado em cada conexão física do pool. O valor vale para a sessão inteira do PostgreSQL,
         * então só repete o set_config quando muda: evita uma ida ao banco a mais em toda consulta.
         */
        private final Map<Connection, String> informado = Collections.synchronizedMap(new WeakHashMap<>());

        ComUsuario(DataSource alvo) {
            super(alvo);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return informarUsuario(super.getConnection());
        }

        @Override
        public Connection getConnection(String usuario, String senha) throws SQLException {
            return informarUsuario(super.getConnection(usuario, senha));
        }

        private Connection informarUsuario(Connection conexao) throws SQLException {
            try {
                // O pool entrega um "embrulho" novo a cada vez; a chave é a conexão física de dentro.
                Connection fisica = conexao.unwrap(Connection.class);
                String usuario = usuarioAtual();
                if (!usuario.equals(informado.get(fisica))) {
                    try (PreparedStatement ps = conexao.prepareStatement("SELECT set_config('semear.usuario_id', ?, false)")) {
                        ps.setString(1, usuario);
                        ps.execute();
                    }
                    informado.put(fisica, usuario);
                }
                return conexao;
            } catch (SQLException e) {
                conexao.close();
                throw e;
            }
        }

        private static String usuarioAtual() {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            return a instanceof JwtAuthenticationToken jwt ? jwt.getName() : "";
        }
    }
}
