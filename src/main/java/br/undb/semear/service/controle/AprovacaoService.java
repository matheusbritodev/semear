package br.undb.semear.service.controle;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.controle.ControleDtos.ResumoAprovacoesResponse;
import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.model.entity.controle.EfeitosAprovacao;
import br.undb.semear.model.entity.controle.SolicitacaoAprovacao;
import br.undb.semear.model.enums.StatusSolicitacao;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.controle.SolicitacaoRepository;

/**
 * Caixa de aprovações (maker-checker). Aprovar grava a decisão e aplica o efeito na mesma transação:
 * se o efeito falhar (ex.: a matrícula foi cancelada nesse meio-tempo), nada é aprovado.
 */
@Service
public class AprovacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(AprovacaoService.class);

    private final SolicitacaoRepository solicitacoes;
    private final PedidosAprovacao pedidos;
    private final EfeitosAprovacao efeitos;
    private final Clock relogio;

    public AprovacaoService(SolicitacaoRepository solicitacoes, PedidosAprovacao pedidos, EfeitosAprovacao efeitos,
                            Clock relogio) {
        this.solicitacoes = solicitacoes;
        this.pedidos = pedidos;
        this.efeitos = efeitos;
        this.relogio = relogio;
    }

    public List<SolicitacaoResponse> listar(StatusSolicitacao status) {
        return pedidos.respostas(solicitacoes.listar(status, 200));
    }

    public SolicitacaoResponse buscar(UUID id) {
        return pedidos.resposta(solicitacoes.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido")));
    }

    public ResumoAprovacoesResponse resumo() {
        return new ResumoAprovacoesResponse(solicitacoes.contarPendentes());
    }

    @Transactional
    public SolicitacaoResponse aprovar(UUID id) {
        SolicitacaoAprovacao s = paraDecidir(id);
        s.aprovar(pedidos.decisorAtual(), pedidos.cpfsDaFamilia(s), OffsetDateTime.now(relogio));
        solicitacoes.registrarDecisao(s);
        s.efetivar(efeitos);
        LOG.info("Pedido {} ({}) aprovado", id, s.getTipo());
        return buscar(id);
    }

    @Transactional
    public SolicitacaoResponse rejeitar(UUID id, String motivo) {
        SolicitacaoAprovacao s = paraDecidir(id);
        s.rejeitar(pedidos.decisorAtual(), pedidos.cpfsDaFamilia(s), motivo, OffsetDateTime.now(relogio));
        solicitacoes.registrarDecisao(s);
        LOG.info("Pedido {} ({}) recusado", id, s.getTipo());
        return buscar(id);
    }

    @Transactional
    public SolicitacaoResponse cancelar(UUID id) {
        SolicitacaoAprovacao s = paraDecidir(id);
        s.cancelar(pedidos.solicitante(), OffsetDateTime.now(relogio));
        solicitacoes.registrarDecisao(s);
        return buscar(id);
    }

    private SolicitacaoAprovacao paraDecidir(UUID id) {
        return solicitacoes.buscarParaDecidir(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido"));
    }
}
