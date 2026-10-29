package br.undb.semear.repository.pessoa;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Permissao;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class PermissaoJdbcRepository implements PermissaoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PermissaoJdbcRepository.class);

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public PermissaoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public Map<PerfilUsuario, List<Permissao>> carregarMatriz() {
        Map<PerfilUsuario, List<Permissao>> matriz = new EnumMap<>(PerfilUsuario.class);
        try {
            jdbc.sql("SELECT perfil, modulo, pode_ler, pode_gravar FROM perfil_permissao").query(rs -> {
                try {
                    matriz.computeIfAbsent(PerfilUsuario.valueOf(rs.getString("perfil")), p -> new ArrayList<>())
                            .add(new Permissao(Modulo.valueOf(rs.getString("modulo")), rs.getBoolean("pode_ler"),
                                    rs.getBoolean("pode_gravar")));
                } catch (IllegalArgumentException e) {
                    LOG.warn("Módulo desconhecido em perfil_permissao ignorado: {}", rs.getString("modulo"));
                }
            });
            return matriz;
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public void salvar(PerfilUsuario perfil, Permissao permissao) {
        try {
            if (!permissao.podeLer() && !permissao.podeGravar()) {
                jdbc.sql("DELETE FROM perfil_permissao WHERE perfil = CAST(:perfil AS perfil_usuario) AND modulo = :modulo")
                        .param("perfil", perfil.name()).param("modulo", permissao.modulo().name()).update();
                return;
            }
            jdbc.sql("""
                            INSERT INTO perfil_permissao (perfil, modulo, pode_ler, pode_gravar)
                            VALUES (CAST(:perfil AS perfil_usuario), :modulo, :ler, :gravar)
                            ON CONFLICT (perfil, modulo) DO UPDATE SET pode_ler = EXCLUDED.pode_ler, pode_gravar = EXCLUDED.pode_gravar
                            """)
                    .param("perfil", perfil.name()).param("modulo", permissao.modulo().name())
                    .param("ler", permissao.podeLer()).param("gravar", permissao.podeGravar())
                    .update();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public void restaurarPadrao() {
        try {
            jdbc.sql("DELETE FROM perfil_permissao").update();
            jdbc.sql("INSERT INTO perfil_permissao SELECT * FROM perfil_permissao_padrao").update();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
