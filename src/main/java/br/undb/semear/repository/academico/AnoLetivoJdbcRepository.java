package br.undb.semear.repository.academico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class AnoLetivoJdbcRepository extends JdbcRepository<AnoLetivo> implements AnoLetivoRepository {

    public AnoLetivoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "ano_letivo");
    }

    @Override
    protected AnoLetivo mapear(ResultSet linha, int numeroLinha) throws SQLException {
        return new AnoLetivo(Colunas.uuid(linha, "id"), linha.getInt("ano"),
                Colunas.data(linha, "data_inicio"), Colunas.data(linha, "data_fim"));
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY ano";
    }

    @Override
    public AnoLetivo salvar(AnoLetivo a) {
        UUID id = executar(() -> jdbc.sql("""
                        INSERT INTO ano_letivo (ano, data_inicio, data_fim) VALUES (:ano, :inicio, :fim)
                        ON CONFLICT (ano) DO UPDATE SET data_inicio = EXCLUDED.data_inicio, data_fim = EXCLUDED.data_fim
                        RETURNING id
                        """)
                .param("ano", a.getAno()).param("inicio", a.getDataInicio()).param("fim", a.getDataFim())
                .query(UUID.class).single());
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public Optional<AnoLetivo> buscarPorAno(int ano) {
        return executar(() -> jdbc.sql("SELECT * FROM ano_letivo WHERE ano = :ano")
                .param("ano", ano).query(this::mapear).optional());
    }

    @Override
    public int encerrar(UUID anoLetivoId) {
        return executar(() -> jdbc.sql("SELECT fn_encerrar_ano_letivo(:id)").param("id", anoLetivoId)
                .query(Integer.class).single());
    }
}
