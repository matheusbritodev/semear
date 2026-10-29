package br.undb.semear.repository.matricula;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.matricula.DocumentoAluno;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class DocumentoAlunoJdbcRepository extends JdbcRepository<DocumentoAluno> implements DocumentoAlunoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentoAlunoJdbcRepository.class);

    public DocumentoAlunoJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        super(jdbc, tradutor, "documento_aluno");
    }

    @Override
    protected DocumentoAluno mapear(ResultSet linha, int numeroLinha) throws SQLException {
        return new DocumentoAluno(Colunas.uuid(linha, "id"), Colunas.uuid(linha, "aluno_id"),
                TipoDocumentoAluno.valueOf(linha.getString("tipo")), linha.getString("arquivo_url"),
                linha.getObject("enviado_em", OffsetDateTime.class));
    }

    @Override
    public DocumentoAluno salvar(DocumentoAluno documento) {
        if (documento.estaPersistida()) {
            throw new RegraNegocioException("DOCUMENTO_IMUTAVEL", "Documentos não são editados: envie um novo arquivo");
        }
        UUID id = executar(() -> jdbc.sql("""
                        INSERT INTO documento_aluno (aluno_id, tipo, arquivo_url) VALUES (:aluno, :tipo, :caminho)
                        RETURNING id
                        """)
                .param("aluno", documento.getAlunoId())
                .param("tipo", documento.getTipo().name())
                .param("caminho", documento.getCaminho())
                .query(UUID.class).single());
        return buscarPorId(id).orElseThrow();
    }

    /** Linhas com tipo desconhecido (gravadas fora do sistema) são ignoradas com aviso. */
    @Override
    public List<DocumentoAluno> listarDoAluno(UUID alunoId) {
        return executar(() -> jdbc.sql("SELECT * FROM documento_aluno WHERE aluno_id = :aluno ORDER BY enviado_em DESC")
                .param("aluno", alunoId)
                .query((rs, n) -> {
                    try {
                        return mapear(rs, n);
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Documento com tipo desconhecido ignorado: {}", rs.getString("tipo"));
                        return null;
                    }
                })
                .list()
                .stream().filter(d -> d != null).toList());
    }
}
