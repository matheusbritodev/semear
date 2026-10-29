package br.undb.semear.repository.matricula;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.matricula.CondicoesContrato;
import br.undb.semear.model.entity.matricula.Contrato;
import br.undb.semear.model.entity.matricula.EvidenciaAssinatura;
import br.undb.semear.model.entity.matricula.TermoAssinado;
import br.undb.semear.model.enums.StatusMatricula;

public interface ContratoRepository {

    /**
     * Contrato com o que o termo precisa (aluno, série, valores da tabela de preços, responsável financeiro)
     * e, se já assinado pelo sistema, o texto congelado e como foi assinado.
     */
    record ContratoComContexto(Contrato contrato, UUID alunoId, StatusMatricula statusMatricula,
                               CondicoesContrato condicoes, String assinante, Optional<TermoAssinado> termoAssinado,
                               EvidenciaAssinatura.Canal canal, String registradaPor) {
    }

    /** Responsável vinculado ao aluno: quem pode assinar (RD03). */
    record Assinante(UUID id, String nome, String parentesco, boolean financeiro) {
    }

    Optional<ContratoComContexto> buscar(UUID id);

    List<ContratoComContexto> doAluno(UUID alunoId);

    List<Assinante> responsaveisDoAluno(UUID alunoId);

    /** Grava a assinatura com o texto congelado e a evidência (só se ainda estiver pendente). */
    void registrarAssinatura(UUID contratoId, UUID responsavelId, OffsetDateTime quando, TermoAssinado termo,
                             EvidenciaAssinatura evidencia);
}
