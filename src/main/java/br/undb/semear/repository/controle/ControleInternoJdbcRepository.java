package br.undb.semear.repository.controle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class ControleInternoJdbcRepository implements ControleInternoRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public ControleInternoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public Optional<String> cpfDoUsuario(UUID usuarioId) {
        try {
            return jdbc.sql("SELECT cpf FROM funcionario WHERE usuario_id = :id").param("id", usuarioId)
                    .query(String.class).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public List<String> cpfsDaFamiliaDaMatricula(UUID matriculaId) {
        try {
            return jdbc.sql("""
                            SELECT r.cpf FROM matricula m
                              JOIN aluno_responsavel ar ON ar.aluno_id = m.aluno_id
                              JOIN responsavel r ON r.id = ar.responsavel_id
                             WHERE m.id = :id AND r.cpf IS NOT NULL
                            """)
                    .param("id", matriculaId).query(String.class).list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public List<String> cpfsDaFamiliaDaCobranca(UUID cobrancaId) {
        try {
            return jdbc.sql("""
                            SELECT r.cpf FROM cobranca c
                              JOIN matricula m ON m.id = c.matricula_id
                              JOIN aluno_responsavel ar ON ar.aluno_id = m.aluno_id
                              JOIN responsavel r ON r.id = ar.responsavel_id
                             WHERE c.id = :id AND r.cpf IS NOT NULL
                            """)
                    .param("id", cobrancaId).query(String.class).list();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public Optional<String> descricaoMatricula(UUID matriculaId) {
        try {
            return jdbc.sql("""
                            SELECT a.nome || ' (' || t.nome || ', ' || al.ano || ')'
                              FROM matricula m
                              JOIN aluno a ON a.id = m.aluno_id
                              JOIN turma t ON t.id = m.turma_id
                              JOIN ano_letivo al ON al.id = m.ano_letivo_id
                             WHERE m.id = :id
                            """)
                    .param("id", matriculaId).query(String.class).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }

    @Override
    public Optional<String> nomeSerie(UUID serieId) {
        try {
            return jdbc.sql("SELECT nome FROM serie WHERE id = :id").param("id", serieId).query(String.class).optional();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
