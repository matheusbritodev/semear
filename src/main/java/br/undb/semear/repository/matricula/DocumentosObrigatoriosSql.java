package br.undb.semear.repository.matricula;

import java.util.Arrays;
import java.util.stream.Collectors;

import br.undb.semear.model.enums.TipoDocumentoAluno;

/**
 * Trechos de SQL para contar documentos obrigatórios (RN12) a partir do enum Java,
 * para a lista de obrigatórios existir num lugar só (TipoDocumentoAluno).
 */
public final class DocumentosObrigatoriosSql {

    /** Literal de array do PostgreSQL, ex.: {CERTIDAO_NASCIMENTO,...}; usar com CAST(:obrigatorios AS text[]). */
    public static final String LISTA = Arrays.stream(TipoDocumentoAluno.values())
            .filter(TipoDocumentoAluno::isObrigatorio)
            .map(Enum::name)
            .collect(Collectors.joining(",", "{", "}"));

    public static final int TOTAL = (int) Arrays.stream(TipoDocumentoAluno.values())
            .filter(TipoDocumentoAluno::isObrigatorio).count();

    /** Subconsulta: quantos obrigatórios distintos o aluno (alias da coluna informado) já entregou. */
    public static String entreguesDo(String colunaAlunoId) {
        return "(SELECT count(DISTINCT d.tipo) FROM documento_aluno d WHERE d.aluno_id = " + colunaAlunoId
                + " AND d.tipo = ANY(CAST(:obrigatorios AS text[])))";
    }

    private DocumentosObrigatoriosSql() {
    }
}
