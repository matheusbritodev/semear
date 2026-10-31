package br.undb.semear.repository.controle;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.controle.RevisaoAcesso;

/** Contas da equipe para revisar e histórico das revisões (tabela revisao_acesso). */
public interface RevisaoAcessoRepository {

    /** Revisão já feita. ancoraId/ancoraHash: último registro da trilha de auditoria naquele momento. */
    record RevisaoRealizada(UUID id, OffsetDateTime realizadaEm, String realizadaPor, int contasRevisadas, int alertas,
                            String observacao, Long ancoraId, String ancoraHash) {
    }

    /** Logins da Área da escola (todos os perfis menos Responsável e Aluno), com o último acesso. */
    List<RevisaoAcesso.Conta> contasDaEquipe();

    long contasDoPortalAtivas();

    UUID salvar(UUID realizadaPor, int contasRevisadas, int alertas, String observacao, String resultadoJson);

    /** Mais recentes primeiro. */
    List<RevisaoRealizada> listar();
}
