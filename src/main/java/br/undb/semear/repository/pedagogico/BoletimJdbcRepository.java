package br.undb.semear.repository.pedagogico;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.pedagogico.Boletim;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class BoletimJdbcRepository implements BoletimRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public BoletimJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public List<MatriculaDoBoletim> matriculasDoAluno(UUID alunoId) {
        return executar(() -> jdbc.sql("""
                        SELECT m.id, al.ano, s.nome AS serie, t.nome AS turma, s.nivel
                          FROM matricula m
                          JOIN turma t ON t.id = m.turma_id
                          JOIN serie s ON s.id = t.serie_id
                          JOIN ano_letivo al ON al.id = m.ano_letivo_id
                         WHERE m.aluno_id = :aluno AND m.status <> 'CANCELADA'
                         ORDER BY al.ano DESC
                        """)
                .param("aluno", alunoId)
                .query((rs, n) -> new MatriculaDoBoletim(Colunas.uuid(rs, "id"), rs.getInt("ano"), rs.getString("serie"),
                        rs.getString("turma"), NivelEnsino.valueOf(rs.getString("nivel"))))
                .list());
    }

    @Override
    public List<Boletim.Nota> notas(UUID matriculaId) {
        return executar(() -> jdbc.sql("""
                        SELECT d.nome, n.bimestre, n.valor FROM nota n JOIN disciplina d ON d.id = n.disciplina_id
                         WHERE n.matricula_id = :matricula
                        """)
                .param("matricula", matriculaId)
                .query((rs, n) -> new Boletim.Nota(rs.getString("nome"), rs.getInt("bimestre"), rs.getBigDecimal("valor")))
                .list());
    }

    @Override
    public Boletim.Frequencia frequencia(UUID matriculaId) {
        return executar(() -> jdbc.sql("""
                        SELECT count(*) AS aulas, count(*) FILTER (WHERE presente) AS presencas
                          FROM frequencia WHERE matricula_id = :matricula
                        """)
                .param("matricula", matriculaId)
                .query((rs, n) -> new Boletim.Frequencia(rs.getInt("aulas"), rs.getInt("presencas")))
                .single());
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
