package br.undb.semear.repository.matricula;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.matricula.CondicoesContrato;
import br.undb.semear.model.entity.matricula.Contrato;
import br.undb.semear.model.entity.matricula.ContratoFactory;
import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoContrato;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class ContratoJdbcRepository implements ContratoRepository {

    private static final String SELECT = """
            SELECT ct.id, ct.matricula_id, ct.tipo, ct.status, ct.assinado_por, ct.assinado_em,
                   m.aluno_id, m.status AS status_matricula, a.nome AS aluno, a.numero_cadastro,
                   s.nome AS serie, t.nome AS turma, al.ano, al.data_inicio, al.data_fim,
                   tp.valor_mensal, tp.valor_matricula, tp.dia_vencimento,
                   rf.nome AS responsavel_financeiro, ra.nome AS assinante
              FROM contrato ct
              JOIN matricula m   ON m.id = ct.matricula_id
              JOIN aluno a       ON a.id = m.aluno_id
              JOIN turma t       ON t.id = m.turma_id
              JOIN serie s       ON s.id = t.serie_id
              JOIN ano_letivo al ON al.id = m.ano_letivo_id
              LEFT JOIN tabela_preco tp ON tp.serie_id = s.id AND tp.ano_letivo_id = m.ano_letivo_id
              LEFT JOIN aluno_responsavel arf ON arf.aluno_id = a.id AND arf.resp_financeiro
              LEFT JOIN responsavel rf ON rf.id = arf.responsavel_id
              LEFT JOIN responsavel ra ON ra.id = ct.assinado_por
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public ContratoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public Optional<ContratoComContexto> buscar(UUID id) {
        return executar(() -> jdbc.sql(SELECT + " WHERE ct.id = :id").param("id", id)
                .query((rs, n) -> mapear(rs)).optional());
    }

    @Override
    public List<ContratoComContexto> doAluno(UUID alunoId) {
        return executar(() -> jdbc.sql(SELECT + " WHERE m.aluno_id = :aluno ORDER BY al.ano DESC")
                .param("aluno", alunoId).query((rs, n) -> mapear(rs)).list());
    }

    @Override
    public List<Assinante> responsaveisDoAluno(UUID alunoId) {
        return executar(() -> jdbc.sql("""
                        SELECT r.id, r.nome, ar.parentesco, ar.resp_financeiro
                          FROM aluno_responsavel ar JOIN responsavel r ON r.id = ar.responsavel_id
                         WHERE ar.aluno_id = :aluno
                         ORDER BY ar.resp_financeiro DESC, r.nome
                        """)
                .param("aluno", alunoId)
                .query((rs, n) -> new Assinante(Colunas.uuid(rs, "id"), rs.getString("nome"), rs.getString("parentesco"),
                        rs.getBoolean("resp_financeiro")))
                .list());
    }

    @Override
    public void registrarAssinatura(UUID contratoId, UUID responsavelId, OffsetDateTime quando) {
        executar(() -> jdbc.sql("""
                        UPDATE contrato SET status = 'ASSINADO', assinado_por = :responsavel, assinado_em = :quando
                         WHERE id = :id AND status = 'PENDENTE'
                        """)
                .param("id", contratoId).param("responsavel", responsavelId).param("quando", quando)
                .update());
    }

    private static ContratoComContexto mapear(ResultSet rs) throws SQLException {
        Contrato contrato = ContratoFactory.criar(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "matricula_id"),
                TipoContrato.valueOf(rs.getString("tipo")), StatusContrato.valueOf(rs.getString("status")),
                Colunas.uuid(rs, "assinado_por"), rs.getObject("assinado_em", OffsetDateTime.class));
        int dia = rs.getInt("dia_vencimento");
        CondicoesContrato condicoes = new CondicoesContrato(rs.getString("aluno"), rs.getLong("numero_cadastro"),
                rs.getString("serie"), rs.getString("turma"), rs.getInt("ano"), Colunas.data(rs, "data_inicio"),
                Colunas.data(rs, "data_fim"), dinheiro(rs.getBigDecimal("valor_mensal")),
                dinheiro(rs.getBigDecimal("valor_matricula")), dia == 0 ? 10 : dia, rs.getString("responsavel_financeiro"));
        return new ContratoComContexto(contrato, Colunas.uuid(rs, "aluno_id"),
                StatusMatricula.valueOf(rs.getString("status_matricula")), condicoes, rs.getString("assinante"));
    }

    private static Dinheiro dinheiro(BigDecimal valor) {
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
