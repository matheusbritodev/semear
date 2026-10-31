package br.undb.semear.repository.controle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.controle.SolicitacaoAprovacao;
import br.undb.semear.model.enums.StatusSolicitacao;

/** Pedidos de dupla aprovação (tabela solicitacao_aprovacao). */
public interface SolicitacaoRepository {

    /** Pedido com os nomes de quem pediu e de quem decidiu, para exibir. */
    record SolicitacaoComNomes(SolicitacaoAprovacao solicitacao, String solicitante, String decisor) {
    }

    /** Insere um pedido novo. Pedido igual já pendente: ConflitoException (índice uq_solicitacao_pendente). */
    UUID inserir(SolicitacaoAprovacao solicitacao, UUID matriculaId);

    /** Busca travando a linha, para dois aprovadores não decidirem ao mesmo tempo. */
    Optional<SolicitacaoAprovacao> buscarParaDecidir(UUID id);

    Optional<SolicitacaoComNomes> buscar(UUID id);

    /** Grava a decisão (aprovada, recusada ou cancelada). */
    void registrarDecisao(SolicitacaoAprovacao solicitacao);

    /** status nulo = todos, mais recentes primeiro. */
    List<SolicitacaoComNomes> listar(StatusSolicitacao status, int limite);

    long contarPendentes();
}
