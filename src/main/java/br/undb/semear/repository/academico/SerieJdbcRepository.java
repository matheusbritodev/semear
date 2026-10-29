package br.undb.semear.repository.academico;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class SerieJdbcRepository extends JdbcRepository<Serie> implements SerieRepository {

    public SerieJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "serie");
    }

    @Override
    protected Serie mapear(ResultSet linha, int numeroLinha) throws SQLException {
        return new Serie(Colunas.uuid(linha, "id"), linha.getString("nome"),
                NivelEnsino.valueOf(linha.getString("nivel")), linha.getInt("ordem"));
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY ordem";
    }

    @Override
    public Serie salvar(Serie serie) {
        throw new RegraNegocioException("OPERACAO_NAO_DISPONIVEL", "As séries são fixas; mudanças são feitas por migration");
    }
}
