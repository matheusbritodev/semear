package br.undb.semear.repository.financeiro;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.model.entity.financeiro.DadosCobranca;
import br.undb.semear.model.entity.financeiro.Pagamento;
import br.undb.semear.model.entity.financeiro.ReferenciaCora;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class CobrancaJdbcRepository implements CobrancaRepository {

    private static final String SELECT = """
            SELECT c.*, a.id AS aluno_id, a.numero_cadastro, a.nome AS aluno, al.ano,
                   s.nome || ' ' || t.nome AS turma, r.nome AS responsavel, r.telefone, fb.nome AS baixa_por
              FROM cobranca c
              JOIN matricula m   ON m.id = c.matricula_id
              JOIN ano_letivo al ON al.id = m.ano_letivo_id
              JOIN aluno a       ON a.id = m.aluno_id
              JOIN turma t       ON t.id = m.turma_id
              JOIN serie s       ON s.id = t.serie_id
              JOIN responsavel r ON r.id = c.responsavel_id
              LEFT JOIN funcionario fb ON fb.id = c.baixa_registrada_por
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public CobrancaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public PaginaResponse<CobrancaComContexto> pesquisar(Filtro f, LocalDate hoje) {
        AtomicLong total = new AtomicLong();
        String status = f.status() == null ? null : f.status().name();
        List<CobrancaComContexto> itens = executar(() -> jdbc.sql(SELECT.replace("SELECT c.*,", "SELECT c.*, count(*) OVER () AS total_linhas,") + """
                         WHERE (CAST(:ano AS int) IS NULL OR EXTRACT(year FROM c.competencia) = CAST(:ano AS int))
                           AND (CAST(:mes AS int) IS NULL OR EXTRACT(month FROM c.competencia) = CAST(:mes AS int))
                           AND (CAST(:tipo AS text) IS NULL OR c.tipo::text = CAST(:tipo AS text))
                           AND (CAST(:matricula AS uuid) IS NULL OR c.matricula_id = CAST(:matricula AS uuid))
                           AND (CAST(:responsavel AS uuid) IS NULL OR c.responsavel_id = CAST(:responsavel AS uuid))
                           AND (CAST(:status AS text) IS NULL
                                OR (CAST(:status AS text) = 'VENCIDO' AND c.status IN ('PENDENTE','VENCIDO') AND c.vencimento < :hoje)
                                OR (CAST(:status AS text) = 'PENDENTE' AND c.status IN ('PENDENTE','VENCIDO') AND c.vencimento >= :hoje)
                                OR (CAST(:status AS text) IN ('PAGO','CANCELADO') AND c.status::text = CAST(:status AS text)))
                           AND (CAST(:busca AS text) IS NULL
                                OR a.nome ILIKE '%' || CAST(:busca AS text) || '%'
                                OR r.nome ILIKE '%' || CAST(:busca AS text) || '%'
                                OR a.numero_cadastro::text = CAST(:busca AS text))
                         ORDER BY c.vencimento DESC, a.nome, c.tipo
                         LIMIT :tamanho OFFSET :deslocamento
                        """)
                .param("ano", f.ano()).param("mes", f.mes())
                .param("tipo", f.tipo() == null ? null : f.tipo().name())
                .param("matricula", f.matriculaId()).param("responsavel", f.responsavelId())
                .param("status", status).param("hoje", hoje)
                .param("busca", Colunas.termo(f.busca()))
                .param("tamanho", f.tamanho()).param("deslocamento", f.pagina() * f.tamanho())
                .query((rs, n) -> {
                    total.set(rs.getLong("total_linhas"));
                    return mapear(rs);
                })
                .list());
        return new PaginaResponse<>(itens, f.pagina(), f.tamanho(), total.get());
    }

    @Override
    public Optional<CobrancaComContexto> buscar(UUID id) {
        return executar(() -> jdbc.sql(SELECT + " WHERE c.id = :id").param("id", id)
                .query((rs, n) -> mapear(rs)).optional());
    }

    @Override
    public List<CobrancaComContexto> vencidas(LocalDate hoje) {
        return executar(() -> jdbc.sql(SELECT + """
                         WHERE c.status IN ('PENDENTE','VENCIDO') AND c.vencimento < :hoje
                         ORDER BY a.nome, c.vencimento
                        """).param("hoje", hoje).query((rs, n) -> mapear(rs)).list());
    }

    @Override
    public List<CobrancaComContexto> travarSemBoleto(int limite) {
        return executar(() -> jdbc.sql(SELECT + """
                         WHERE c.status IN ('PENDENTE','VENCIDO') AND c.cora_id IS NULL
                         ORDER BY c.vencimento LIMIT :limite
                           FOR UPDATE OF c SKIP LOCKED
                        """).param("limite", limite).query((rs, n) -> mapear(rs)).list());
    }

    @Override
    public boolean vincularBoleto(UUID cobrancaId, ReferenciaCora boleto) {
        return executar(() -> jdbc.sql("""
                        UPDATE cobranca SET cora_id = :coraId, cora_url = :url, cora_sincronizado_em = now()
                         WHERE id = :id AND cora_id IS NULL
                        """)
                .param("id", cobrancaId).param("coraId", boleto.id()).param("url", boleto.url())
                .update()) == 1;
    }

    @Override
    public Optional<UUID> buscarIdPorCora(String coraId) {
        return executar(() -> jdbc.sql("SELECT id FROM cobranca WHERE cora_id = :cora").param("cora", coraId)
                .query(UUID.class).optional());
    }

    @Override
    public void registrarPagamento(UUID cobrancaId, OrigemPagamento origem, Dinheiro valorPago, OffsetDateTime pagoEm,
                                   UUID registradoPor) {
        executar(() -> jdbc.sql("""
                        SELECT fn_registrar_pagamento(:id, CAST(:origem AS origem_pagamento), :valor, :pagoEm)
                        """)
                .param("id", cobrancaId).param("origem", origem.name())
                .param("valor", valorPago.getValor()).param("pagoEm", pagoEm)
                .query().singleRow());
        if (origem == OrigemPagamento.SITE) {
            executar(() -> jdbc.sql("UPDATE cobranca SET cora_sincronizado_em = now() WHERE id = :id")
                    .param("id", cobrancaId).update());
        }
        if (registradoPor != null) {
            // Permitido em cobrança paga: o trigger só bloqueia mudança de valor, status ou matrícula (RD05).
            executar(() -> jdbc.sql("UPDATE cobranca SET baixa_registrada_por = :por WHERE id = :id")
                    .param("id", cobrancaId).param("por", registradoPor).update());
        }
    }

    @Override
    public int gerarPeriodo(int ano, int mesInicio, int mesFim) {
        return executar(() -> jdbc.sql("SELECT fn_gerar_cobrancas_periodo(:ano, :ini, :fim)")
                .param("ano", ano).param("ini", mesInicio).param("fim", mesFim)
                .query(Integer.class).single());
    }

    @Override
    public List<ResumoMensal> resumo(int ano) {
        return executar(() -> jdbc.sql("""
                        SELECT competencia, tipo, qtd_cobrancas, total_previsto, total_descontos, total_recebido, total_em_atraso
                          FROM vw_resumo_financeiro_mensal
                         WHERE EXTRACT(year FROM competencia) = :ano
                         ORDER BY competencia, tipo
                        """)
                .param("ano", ano)
                .query((rs, n) -> new ResumoMensal(Colunas.data(rs, "competencia"),
                        TipoCobranca.valueOf(rs.getString("tipo")), rs.getInt("qtd_cobrancas"),
                        dinheiro(rs, "total_previsto"), dinheiro(rs, "total_descontos"),
                        dinheiro(rs, "total_recebido"), dinheiro(rs, "total_em_atraso")))
                .list());
    }

    @Override
    public List<LocalDate> feriados() {
        return executar(() -> jdbc.sql("SELECT data FROM calendario_letivo WHERE tipo = 'FERIADO'")
                .query(LocalDate.class).list());
    }

    private static CobrancaComContexto mapear(ResultSet rs) throws SQLException {
        StatusCobranca status = StatusCobranca.valueOf(rs.getString("status"));
        Pagamento pagamento = status == StatusCobranca.PAGO
                ? new Pagamento(rs.getObject("pago_em", OffsetDateTime.class), dinheiro(rs, "valor_pago"),
                        OrigemPagamento.valueOf(rs.getString("origem_pagamento")))
                : null;
        String coraId = rs.getString("cora_id");
        DadosCobranca dados = new DadosCobranca(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "matricula_id"),
                Colunas.uuid(rs, "responsavel_id"), TipoCobranca.valueOf(rs.getString("tipo")),
                YearMonth.from(Colunas.data(rs, "competencia")), dinheiro(rs, "valor_bruto"),
                dinheiro(rs, "desconto_bolsa"), dinheiro(rs, "valor_final"), Colunas.data(rs, "vencimento"), status,
                pagamento, coraId == null ? null : new ReferenciaCora(coraId, rs.getString("cora_url")));
        return new CobrancaComContexto(dados, Colunas.uuid(rs, "aluno_id"), rs.getLong("numero_cadastro"),
                rs.getString("aluno"), rs.getInt("ano"), rs.getString("turma"), rs.getString("responsavel"),
                Telefone.formatar(rs.getString("telefone")), rs.getString("baixa_por"));
    }

    private static Dinheiro dinheiro(ResultSet rs, String coluna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(coluna);
        return valor == null ? Dinheiro.ZERO : Dinheiro.de(valor);
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
