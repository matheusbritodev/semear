package br.undb.semear.repository.matricula;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.CobrancaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.ContratoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.FardaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaResumoResponse;
import br.undb.semear.model.entity.matricula.Matricula;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.enums.TipoContrato;
import br.undb.semear.model.enums.TipoMatricula;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class MatriculaJdbcRepository extends JdbcRepository<Matricula> implements MatriculaRepository {

    private static final String SELECT_RESUMO = """
            SELECT m.id, a.id AS aluno_id, a.numero_cadastro, a.nome AS aluno, al.ano, t.id AS turma_id,
                   s.nome || ' ' || t.nome AS turma, m.tipo, m.status, m.data_matricula
              FROM matricula m
              JOIN aluno a       ON a.id = m.aluno_id
              JOIN turma t       ON t.id = m.turma_id
              JOIN serie s       ON s.id = t.serie_id
              JOIN ano_letivo al ON al.id = m.ano_letivo_id
            """;

    public MatriculaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "matricula");
    }

    @Override
    protected String selectBase() {
        return "SELECT m.*, al.ano FROM matricula m JOIN ano_letivo al ON al.id = m.ano_letivo_id";
    }

    @Override
    protected String colunaId() {
        return "m.id";
    }

    @Override
    protected Matricula mapear(ResultSet linha, int numeroLinha) throws SQLException {
        return new Matricula(Colunas.uuid(linha, "id"), Colunas.uuid(linha, "aluno_id"), Colunas.uuid(linha, "turma_id"),
                linha.getInt("ano"), TipoMatricula.valueOf(linha.getString("tipo")),
                StatusMatricula.valueOf(linha.getString("status")), Colunas.data(linha, "data_matricula"),
                linha.getString("motivo_cancelamento"));
    }

    @Override
    public Matricula salvar(Matricula m) {
        if (!m.estaPersistida()) {
            throw new RegraNegocioException("MATRICULA_VIA_FLUXO", "Matrículas são criadas pela aprovação ou rematrícula");
        }
        executar(() -> jdbc.sql("""
                        UPDATE matricula SET status = CAST(:status AS status_matricula), motivo_cancelamento = :motivo
                         WHERE id = :id
                        """)
                .param("id", m.getId()).param("status", m.getStatus().name())
                .param("motivo", m.getMotivoCancelamento().orElse(null))
                .update());
        return buscarPorId(m.getId()).orElseThrow();
    }

    @Override
    public PaginaResponse<MatriculaResumoResponse> pesquisar(String busca, int ano, StatusMatricula status,
                                                             int pagina, int tamanho) {
        AtomicLong total = new AtomicLong();
        List<MatriculaResumoResponse> itens = executar(() -> jdbc.sql("""
                        SELECT x.*, count(*) OVER () AS total FROM (""" + SELECT_RESUMO + """
                         WHERE al.ano = :ano
                           AND (CAST(:status AS text) IS NULL OR m.status = CAST(:status AS status_matricula))
                           AND (CAST(:busca AS text) IS NULL
                                OR a.nome ILIKE '%' || CAST(:busca AS text) || '%'
                                OR a.numero_cadastro::text = CAST(:busca AS text))
                        ) x
                         ORDER BY x.data_matricula DESC, x.aluno
                         LIMIT :tamanho OFFSET :deslocamento
                        """)
                .param("ano", ano)
                .param("status", status == null ? null : status.name())
                .param("busca", Colunas.termo(busca))
                .param("tamanho", tamanho)
                .param("deslocamento", pagina * tamanho)
                .query((rs, n) -> {
                    total.set(rs.getLong("total"));
                    return mapearResumo(rs, n);
                })
                .list());
        return new PaginaResponse<>(itens, pagina, tamanho, total.get());
    }

    @Override
    public Optional<MatriculaResumoResponse> buscarResumo(UUID id) {
        return executar(() -> jdbc.sql(SELECT_RESUMO + " WHERE m.id = :id")
                .param("id", id).query(this::mapearResumo).optional());
    }

    @Override
    public Optional<MatriculaResumoResponse> buscarDoAlunoNoAno(UUID alunoId, int ano) {
        return executar(() -> jdbc.sql(SELECT_RESUMO + " WHERE a.id = :aluno AND al.ano = :ano")
                .param("aluno", alunoId).param("ano", ano).query(this::mapearResumo).optional());
    }

    private MatriculaResumoResponse mapearResumo(ResultSet rs, int n) throws SQLException {
        return new MatriculaResumoResponse(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "aluno_id"),
                rs.getLong("numero_cadastro"), rs.getString("aluno"), rs.getInt("ano"), Colunas.uuid(rs, "turma_id"),
                rs.getString("turma"), TipoMatricula.valueOf(rs.getString("tipo")),
                StatusMatricula.valueOf(rs.getString("status")), Colunas.data(rs, "data_matricula"));
    }

    @Override
    public List<ContratoResponse> listarContratos(UUID matriculaId) {
        return executar(() -> jdbc.sql("""
                        SELECT c.id, c.tipo, c.status, r.nome AS assinado_por, c.assinado_em
                          FROM contrato c LEFT JOIN responsavel r ON r.id = c.assinado_por
                         WHERE c.matricula_id = :id
                        """)
                .param("id", matriculaId)
                .query((rs, n) -> new ContratoResponse(Colunas.uuid(rs, "id"), TipoContrato.valueOf(rs.getString("tipo")),
                        StatusContrato.valueOf(rs.getString("status")), rs.getString("assinado_por"),
                        rs.getObject("assinado_em", OffsetDateTime.class)))
                .list());
    }

    @Override
    public List<FardaResponse> listarFardas(UUID matriculaId) {
        return executar(() -> jdbc.sql("SELECT tipo, tamanho FROM farda_matricula WHERE matricula_id = :id ORDER BY tipo DESC")
                .param("id", matriculaId)
                .query((rs, n) -> new FardaResponse(rs.getString("tipo"), rs.getString("tamanho")))
                .list());
    }

    @Override
    public Optional<CobrancaResumoResponse> buscarCobrancaMatricula(UUID matriculaId) {
        return executar(() -> jdbc.sql("""
                        SELECT id, tipo, competencia, valor_final, vencimento, status, pago_em, valor_pago, origem_pagamento
                          FROM cobranca WHERE matricula_id = :id AND tipo = 'MATRICULA'
                        """)
                .param("id", matriculaId)
                .query((rs, n) -> new CobrancaResumoResponse(Colunas.uuid(rs, "id"),
                        TipoCobranca.valueOf(rs.getString("tipo")), Colunas.data(rs, "competencia"),
                        rs.getObject("valor_final", BigDecimal.class), Colunas.data(rs, "vencimento"),
                        StatusCobranca.valueOf(rs.getString("status")), rs.getObject("pago_em", OffsetDateTime.class),
                        rs.getObject("valor_pago", BigDecimal.class),
                        rs.getString("origem_pagamento") == null ? null : OrigemPagamento.valueOf(rs.getString("origem_pagamento"))))
                .optional());
    }

    @Override
    public boolean possuiDebitosVencidos(UUID alunoId) {
        return executar(() -> jdbc.sql("SELECT EXISTS (SELECT 1 FROM vw_inadimplencia WHERE aluno_id = :aluno)")
                .param("aluno", alunoId).query(Boolean.class).single());
    }

    @Override
    public Optional<String> nomeResponsavelFinanceiro(UUID alunoId) {
        return executar(() -> jdbc.sql("""
                        SELECT r.nome FROM aluno_responsavel ar JOIN responsavel r ON r.id = ar.responsavel_id
                         WHERE ar.aluno_id = :aluno AND ar.resp_financeiro
                        """)
                .param("aluno", alunoId).query(String.class).optional());
    }

    @Override
    public int cancelarCobrancasFuturas(UUID matriculaId, LocalDate hoje) {
        return executar(() -> jdbc.sql("""
                        UPDATE cobranca SET status = 'CANCELADO'
                         WHERE matricula_id = :id AND status = 'PENDENTE' AND vencimento >= :hoje
                        """)
                .param("id", matriculaId).param("hoje", hoje).update());
    }
}
