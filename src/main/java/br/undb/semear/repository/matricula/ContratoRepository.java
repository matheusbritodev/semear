package br.undb.semear.repository.matricula;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.matricula.CondicoesContrato;
import br.undb.semear.model.entity.matricula.Contrato;
import br.undb.semear.model.enums.StatusMatricula;

public interface ContratoRepository {

    /** Contrato com o que o termo precisa (aluno, série, valores da tabela de preços, responsável financeiro). */
    record ContratoComContexto(Contrato contrato, UUID alunoId, StatusMatricula statusMatricula,
                               CondicoesContrato condicoes, String assinante) {
    }

    /** Responsável vinculado ao aluno: quem pode assinar (RD03). */
    record Assinante(UUID id, String nome, String parentesco, boolean financeiro) {
    }

    Optional<ContratoComContexto> buscar(UUID id);

    List<ContratoComContexto> doAluno(UUID alunoId);

    List<Assinante> responsaveisDoAluno(UUID alunoId);

    void registrarAssinatura(UUID contratoId, UUID responsavelId, OffsetDateTime quando);
}
