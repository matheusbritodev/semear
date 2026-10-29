package br.undb.semear.repository.institucional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.institucional.ComprovanteObrigacao;
import br.undb.semear.model.entity.institucional.ObrigacaoInstitucional;
import br.undb.semear.model.enums.OrgaoExterno;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class ObrigacaoJdbcRepository implements ObrigacaoRepository {

    private static final String SELECT = """
            SELECT o.*, f.nome AS responsavel, fc.nome AS cumprida_por FROM obrigacao_institucional o
              LEFT JOIN funcionario f ON f.id = o.responsavel_id
              LEFT JOIN funcionario fc ON fc.id = o.ultimo_cumprimento_por
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public ObrigacaoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public List<ObrigacaoComContexto> listar() {
        Map<UUID, List<ComprovanteObrigacao>> comprovantes = executar(() -> jdbc.sql(
                        "SELECT * FROM documento_obrigacao ORDER BY enviado_em DESC")
                .query((rs, n) -> mapearComprovante(rs)).list())
                .stream().collect(Collectors.groupingBy(ComprovanteObrigacao::getObrigacaoId));
        return executar(() -> jdbc.sql(SELECT + " ORDER BY o.proximo_vencimento")
                .query((rs, n) -> new ObrigacaoComContexto(mapear(rs), rs.getString("responsavel"),
                        comprovantes.getOrDefault(Colunas.uuid(rs, "id"), List.of()), rs.getString("cumprida_por")))
                .list());
    }

    @Override
    public Optional<ObrigacaoComContexto> buscar(UUID id) {
        List<ComprovanteObrigacao> comprovantes = executar(() -> jdbc.sql(
                        "SELECT * FROM documento_obrigacao WHERE obrigacao_id = :id ORDER BY enviado_em DESC")
                .param("id", id).query((rs, n) -> mapearComprovante(rs)).list());
        return executar(() -> jdbc.sql(SELECT + " WHERE o.id = :id").param("id", id)
                .query((rs, n) -> new ObrigacaoComContexto(mapear(rs), rs.getString("responsavel"), comprovantes,
                        rs.getString("cumprida_por")))
                .optional());
    }

    @Override
    public UUID salvar(ObrigacaoInstitucional o) {
        String status = """
                CASE WHEN :encerrada THEN 'CUMPRIDA'::status_obrigacao
                     WHEN CAST(:vencimento AS date) < current_date THEN 'ATRASADA'::status_obrigacao
                     ELSE 'PENDENTE'::status_obrigacao END""";
        return executar(() -> jdbc.sql(o.estaPersistida()
                        ? "UPDATE obrigacao_institucional SET orgao = CAST(:orgao AS orgao_externo), descricao = :descricao,"
                          + " periodicidade_meses = :periodicidade, proximo_vencimento = :vencimento,"
                          + " dias_alerta_antecedencia = :alerta, responsavel_id = :responsavel, status = " + status
                          + " WHERE id = :id RETURNING id"
                        : "INSERT INTO obrigacao_institucional (orgao, descricao, periodicidade_meses, proximo_vencimento,"
                          + " dias_alerta_antecedencia, responsavel_id, status) VALUES (CAST(:orgao AS orgao_externo), :descricao,"
                          + " :periodicidade, :vencimento, :alerta, :responsavel, " + status + ") RETURNING id")
                .param("id", o.getId()).param("orgao", o.getOrgao().name()).param("descricao", o.getDescricao())
                .param("periodicidade", o.getPeriodicidadeMeses()).param("vencimento", o.getProximoVencimento())
                .param("alerta", o.getDiasAlerta()).param("responsavel", o.getResponsavelId())
                .param("encerrada", o.isEncerrada())
                .query(UUID.class).single());
    }

    @Override
    public void cumprir(UUID id, UUID cumpridaPor) {
        executar(() -> jdbc.sql("SELECT fn_cumprir_obrigacao(:id)").param("id", id).query().singleRow());
        executar(() -> jdbc.sql("UPDATE obrigacao_institucional SET ultimo_cumprimento_por = :por WHERE id = :id")
                .param("id", id).param("por", cumpridaPor).update());
    }

    @Override
    public void remover(UUID id) {
        executar(() -> jdbc.sql("DELETE FROM obrigacao_institucional WHERE id = :id").param("id", id).update());
    }

    @Override
    public UUID inserirComprovante(ComprovanteObrigacao c) {
        return executar(() -> jdbc.sql("""
                        INSERT INTO documento_obrigacao (obrigacao_id, arquivo_url) VALUES (:obrigacao, :caminho) RETURNING id
                        """)
                .param("obrigacao", c.getObrigacaoId()).param("caminho", c.getCaminho())
                .query(UUID.class).single());
    }

    @Override
    public Optional<ComprovanteObrigacao> comprovante(UUID id) {
        return executar(() -> jdbc.sql("SELECT * FROM documento_obrigacao WHERE id = :id").param("id", id)
                .query((rs, n) -> mapearComprovante(rs)).optional());
    }

    @Override
    public void removerComprovante(UUID id) {
        executar(() -> jdbc.sql("DELETE FROM documento_obrigacao WHERE id = :id").param("id", id).update());
    }

    @Override
    public List<Funcionario> funcionariosAtivos() {
        return executar(() -> jdbc.sql("SELECT id, nome FROM funcionario WHERE ativo ORDER BY nome")
                .query((rs, n) -> new Funcionario(Colunas.uuid(rs, "id"), rs.getString("nome"))).list());
    }

    private static ObrigacaoInstitucional mapear(ResultSet rs) throws SQLException {
        Integer periodicidade = (Integer) rs.getObject("periodicidade_meses");
        return new ObrigacaoInstitucional(Colunas.uuid(rs, "id"), OrgaoExterno.valueOf(rs.getString("orgao")),
                rs.getString("descricao"), periodicidade, Colunas.data(rs, "proximo_vencimento"),
                rs.getInt("dias_alerta_antecedencia"), Colunas.data(rs, "ultimo_cumprimento"),
                "CUMPRIDA".equals(rs.getString("status")), Colunas.uuid(rs, "responsavel_id"));
    }

    private static ComprovanteObrigacao mapearComprovante(ResultSet rs) throws SQLException {
        return new ComprovanteObrigacao(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "obrigacao_id"), rs.getString("arquivo_url"),
                rs.getObject("enviado_em", OffsetDateTime.class));
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
