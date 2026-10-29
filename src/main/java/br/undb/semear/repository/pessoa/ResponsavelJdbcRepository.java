package br.undb.semear.repository.pessoa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.AlunoDoResponsavelResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.ResponsavelResumoResponse;
import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.matricula.DocumentosObrigatoriosSql;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class ResponsavelJdbcRepository extends JdbcRepository<Responsavel> implements ResponsavelRepository {

    public ResponsavelJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "responsavel");
    }

    @Override
    protected Responsavel mapear(ResultSet linha, int numeroLinha) throws SQLException {
        return mapearResponsavel(linha);
    }

    /** Reutilizado por outros repositórios que fazem JOIN com responsavel (colunas id, nome, cpf, telefone, email, usuario_id). */
    static Responsavel mapearResponsavel(ResultSet linha) throws SQLException {
        return new Responsavel(
                Colunas.uuid(linha, "id"),
                linha.getString("nome"),
                Colunas.cpf(linha, "cpf"),
                Colunas.telefone(linha, "telefone"),
                Colunas.email(linha, "email"),
                Colunas.uuid(linha, "usuario_id"));
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY nome";
    }

    @Override
    public Responsavel salvar(Responsavel r) {
        if (!r.estaPersistida()) {
            UUID id = executar(() -> jdbc.sql("""
                            INSERT INTO responsavel (nome, cpf, telefone, email)
                            VALUES (:nome, :cpf, :telefone, :email) RETURNING id
                            """)
                    .param("nome", r.getNome())
                    .param("cpf", r.getCpfObrigatorio().getDigitos())
                    .param("telefone", r.getTelefone().map(Telefone::getDigitos).orElse(null))
                    .param("email", r.getEmail().map(e -> e.getEndereco()).orElse(null))
                    .query(UUID.class).single());
            return buscarPorId(id).orElseThrow();
        }
        executar(() -> jdbc.sql("UPDATE responsavel SET nome = :nome, telefone = :telefone, email = :email WHERE id = :id")
                .param("id", r.getId())
                .param("nome", r.getNome())
                .param("telefone", r.getTelefone().map(Telefone::getDigitos).orElse(null))
                .param("email", r.getEmail().map(e -> e.getEndereco()).orElse(null))
                .update());
        return buscarPorId(r.getId()).orElseThrow();
    }

    @Override
    public PaginaResponse<ResponsavelResumoResponse> pesquisar(String busca, int pagina, int tamanho) {
        AtomicLong total = new AtomicLong();
        List<ResponsavelResumoResponse> itens = executar(() -> jdbc.sql("""
                        SELECT r.id, r.nome, r.cpf, r.telefone, r.email, r.usuario_id,
                               (SELECT count(*) FROM aluno_responsavel ar WHERE ar.responsavel_id = r.id)::int AS qtd_alunos,
                               count(*) OVER () AS total
                          FROM responsavel r
                         WHERE CAST(:busca AS text) IS NULL
                            OR r.nome ILIKE '%' || CAST(:busca AS text) || '%'
                            OR r.cpf = regexp_replace(CAST(:busca AS text), '\\D', '', 'g')
                         ORDER BY r.nome
                         LIMIT :tamanho OFFSET :deslocamento
                        """)
                .param("busca", Colunas.termo(busca))
                .param("tamanho", tamanho)
                .param("deslocamento", pagina * tamanho)
                .query((rs, n) -> {
                    total.set(rs.getLong("total"));
                    return new ResponsavelResumoResponse(
                            Colunas.uuid(rs, "id"), rs.getString("nome"), Cpf.formatar(rs.getString("cpf")),
                            Telefone.formatar(rs.getString("telefone")), rs.getString("email"),
                            rs.getInt("qtd_alunos"), rs.getObject("usuario_id") != null);
                })
                .list());
        return new PaginaResponse<>(itens, pagina, tamanho, total.get());
    }

    @Override
    public Optional<Responsavel> buscarPorUsuario(UUID usuarioId) {
        return executar(() -> jdbc.sql("SELECT * FROM responsavel WHERE usuario_id = :usuario")
                .param("usuario", usuarioId).query(this::mapear).optional());
    }

    @Override
    public Optional<Responsavel> buscarPorCpf(String cpfDigitos) {
        return executar(() -> jdbc.sql("SELECT * FROM responsavel WHERE cpf = :cpf")
                .param("cpf", cpfDigitos).query(this::mapear).optional());
    }

    @Override
    public void vincularUsuario(UUID responsavelId, UUID usuarioId) {
        executar(() -> jdbc.sql("UPDATE responsavel SET usuario_id = :usuario WHERE id = :id")
                .param("id", responsavelId).param("usuario", usuarioId).update());
    }

    @Override
    public boolean estaVinculadoAoAluno(UUID responsavelId, UUID alunoId) {
        return executar(() -> jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM aluno_responsavel WHERE responsavel_id = :responsavel AND aluno_id = :aluno)
                        """)
                .param("responsavel", responsavelId).param("aluno", alunoId)
                .query(Boolean.class).single());
    }

    @Override
    public List<AlunoDoResponsavelResponse> listarAlunos(UUID responsavelId) {
        return executar(() -> jdbc.sql("""
                        SELECT a.id, a.nome, ar.parentesco, ar.resp_financeiro, ult.turma, ult.status,
                               :total - (SELECT count(DISTINCT d.tipo) FROM documento_aluno d
                                 WHERE d.aluno_id = a.id AND d.tipo = ANY(CAST(:obrigatorios AS text[]))) AS documentos_pendentes,
                               (SELECT count(*) FROM contrato ct JOIN matricula mc ON mc.id = ct.matricula_id
                                 WHERE mc.aluno_id = a.id AND ct.status = 'PENDENTE'
                                   AND mc.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')) AS contratos_pendentes
                          FROM aluno_responsavel ar
                          JOIN aluno a ON a.id = ar.aluno_id
                          LEFT JOIN LATERAL (
                                SELECT s.nome || ' ' || t.nome || ' (' || al.ano || ')' AS turma, m.status
                                  FROM matricula m
                                  JOIN turma t ON t.id = m.turma_id
                                  JOIN serie s ON s.id = t.serie_id
                                  JOIN ano_letivo al ON al.id = m.ano_letivo_id
                                 WHERE m.aluno_id = a.id
                                 ORDER BY al.ano DESC LIMIT 1) ult ON true
                         WHERE ar.responsavel_id = :id
                         ORDER BY a.nome
                        """)
                .param("id", responsavelId)
                .param("obrigatorios", DocumentosObrigatoriosSql.LISTA)
                .param("total", DocumentosObrigatoriosSql.TOTAL)
                .query((rs, n) -> new AlunoDoResponsavelResponse(
                        Colunas.uuid(rs, "id"), rs.getString("nome"), rs.getString("parentesco"),
                        rs.getBoolean("resp_financeiro"), rs.getString("turma"),
                        rs.getString("status") == null ? null : StatusMatricula.valueOf(rs.getString("status")),
                        rs.getInt("documentos_pendentes"), rs.getInt("contratos_pendentes")))
                .list());
    }
}
