package br.undb.semear.repository.matricula;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.matricula.Clausula;
import br.undb.semear.model.entity.matricula.CondicoesContrato;
import br.undb.semear.model.entity.matricula.Contrato;
import br.undb.semear.model.entity.matricula.ContratoFactory;
import br.undb.semear.model.entity.matricula.EvidenciaAssinatura;
import br.undb.semear.model.entity.matricula.TermoAssinado;
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
                   rf.nome AS responsavel_financeiro, ra.nome AS assinante,
                   ct.termo_assinado::text AS termo_json, ct.termo_hash, ct.assinatura_canal, fr.nome AS registrada_por
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
              LEFT JOIN funcionario fr ON fr.id = ct.assinatura_registrada_por
            """;

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;
    private final ObjectMapper json;

    public ContratoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor, ObjectMapper json) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
        this.json = json;
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
    public void registrarAssinatura(UUID contratoId, UUID responsavelId, OffsetDateTime quando, TermoAssinado termo,
                                    EvidenciaAssinatura evidencia) {
        String termoJson;
        try {
            termoJson = json.writeValueAsString(Map.of("titulo", termo.titulo(), "clausulas", termo.clausulas()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível guardar o texto do contrato", e);
        }
        executar(() -> jdbc.sql("""
                        UPDATE contrato
                           SET status = 'ASSINADO', assinado_por = :responsavel, assinado_em = :quando,
                               termo_assinado = CAST(:termo AS jsonb), termo_hash = :hash,
                               assinatura_canal = :canal, assinatura_ip = :ip, assinatura_navegador = :navegador,
                               assinatura_registrada_por = :registradaPor
                         WHERE id = :id AND status = 'PENDENTE'
                        """)
                .param("id", contratoId).param("responsavel", responsavelId).param("quando", quando)
                .param("termo", termoJson).param("hash", termo.hash()).param("canal", evidencia.canal().name())
                .param("ip", evidencia.ip()).param("navegador", evidencia.navegador())
                .param("registradaPor", evidencia.registradaPor())
                .update());
    }

    private ContratoComContexto mapear(ResultSet rs) throws SQLException {
        Contrato contrato = ContratoFactory.criar(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "matricula_id"),
                TipoContrato.valueOf(rs.getString("tipo")), StatusContrato.valueOf(rs.getString("status")),
                Colunas.uuid(rs, "assinado_por"), rs.getObject("assinado_em", OffsetDateTime.class));
        int dia = rs.getInt("dia_vencimento");
        CondicoesContrato condicoes = new CondicoesContrato(rs.getString("aluno"), rs.getLong("numero_cadastro"),
                rs.getString("serie"), rs.getString("turma"), rs.getInt("ano"), Colunas.data(rs, "data_inicio"),
                Colunas.data(rs, "data_fim"), dinheiro(rs.getBigDecimal("valor_mensal")),
                dinheiro(rs.getBigDecimal("valor_matricula")), dia == 0 ? 10 : dia, rs.getString("responsavel_financeiro"));
        String canal = rs.getString("assinatura_canal");
        return new ContratoComContexto(contrato, Colunas.uuid(rs, "aluno_id"),
                StatusMatricula.valueOf(rs.getString("status_matricula")), condicoes, rs.getString("assinante"),
                lerTermo(rs.getString("termo_json"), rs.getString("termo_hash")),
                canal == null ? null : EvidenciaAssinatura.Canal.valueOf(canal), rs.getString("registrada_por"));
    }

    /** Texto congelado gravado na assinatura (contratos assinados antes desta versão não têm). */
    private Optional<TermoAssinado> lerTermo(String termoJson, String hash) {
        if (termoJson == null || hash == null) {
            return Optional.empty();
        }
        try {
            JsonNode no = json.readTree(termoJson);
            List<Clausula> clausulas = new java.util.ArrayList<>();
            no.path("clausulas").forEach(c -> clausulas.add(new Clausula(c.path("titulo").asText(), c.path("texto").asText())));
            return Optional.of(new TermoAssinado(no.path("titulo").asText(), clausulas, hash.trim()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Texto do contrato gravado em formato inválido", e);
        }
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
