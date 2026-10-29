package br.undb.semear.dto.pessoa;

import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.StatusMatricula;

/** Painel de pendências dos alunos (/api/pendencias). */
public final class PendenciaDtos {

    private PendenciaDtos() {
    }

    /** Aluno com matrícula vigente e documentos obrigatórios faltando (RN12). */
    public record PendenciaDocumentoResponse(UUID alunoId, Long numeroCadastro, String aluno, int ano, UUID turmaId,
                                             String turma, StatusMatricula statusMatricula, int pendentes,
                                             List<String> faltando, String responsavel, String telefone) {
    }

    public record ResumoPendenciasResponse(int alunosComPendencia, int documentosFaltando, int alunosConsiderados) {
    }
}
