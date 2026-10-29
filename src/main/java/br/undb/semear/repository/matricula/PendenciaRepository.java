package br.undb.semear.repository.matricula;

import java.util.List;

import br.undb.semear.dto.pessoa.PendenciaDtos.PendenciaDocumentoResponse;

public interface PendenciaRepository {

    /** Alunos com matrícula vigente (aguardando pagamento ou ativa) a partir do ano informado e documentos faltando. */
    List<PendenciaDocumentoResponse> documentosPendentes(int anoMinimo);

    /** Quantos alunos com matrícula vigente entram na conta (para o percentual do painel). */
    int alunosComMatriculaVigente(int anoMinimo);
}
