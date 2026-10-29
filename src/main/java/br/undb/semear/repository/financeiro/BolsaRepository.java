package br.undb.semear.repository.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.entity.financeiro.ConcessaoBolsa;
import br.undb.semear.repository.base.Repository;

public interface BolsaRepository extends Repository<Bolsa, UUID> {

    /** Concessão com os nomes para exibir. */
    record ConcessaoComContexto(ConcessaoBolsa concessao, Bolsa bolsa, UUID alunoId, String aluno, int ano, String turma) {
    }

    /** Quantas concessões vigentes (hoje) cada bolsa tem. */
    int bolsistasVigentes(UUID bolsaId, LocalDate hoje);

    List<ConcessaoComContexto> concessoesDaMatricula(UUID matriculaId);

    /** Bolsistas de um ano letivo (vw_bolsistas + ids), inclusive as encerradas no ano. */
    List<ConcessaoComContexto> bolsistas(int ano);

    Optional<ConcessaoBolsa> buscarConcessao(UUID concessaoId);

    /** Insere a concessão; o trigger do banco recalcula as cobranças em aberto (RF02). */
    UUID conceder(ConcessaoBolsa concessao);

    void encerrarConcessao(UUID concessaoId, LocalDate fim);

    void apagarConcessao(UUID concessaoId);
}
