package br.undb.semear.service.controle;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.model.entity.controle.ConflitoInteresse;
import br.undb.semear.model.entity.controle.Decisor;
import br.undb.semear.model.entity.controle.SolicitacaoAprovacao;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.Acao;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.controle.ControleInternoRepository;
import br.undb.semear.repository.controle.SolicitacaoRepository;
import br.undb.semear.repository.controle.SolicitacaoRepository.SolicitacaoComNomes;
import br.undb.semear.security.UsuarioLogado;

/**
 * Ponto de entrada dos pedidos de aprovação, usado pelos services de bolsas e preços.
 * Fica separado do AprovacaoService para não haver dependência circular
 * (aprovar chama os services de bolsas e preços; eles, por sua vez, registram pedidos aqui).
 */
@Component
public class PedidosAprovacao {

    private final SolicitacaoRepository solicitacoes;
    private final ControleInternoRepository controle;
    private final UsuarioLogado usuarioLogado;

    public PedidosAprovacao(SolicitacaoRepository solicitacoes, ControleInternoRepository controle,
                            UsuarioLogado usuarioLogado) {
        this.solicitacoes = solicitacoes;
        this.controle = controle;
        this.usuarioLogado = usuarioLogado;
    }

    /** Id do login que está pedindo. */
    public UUID solicitante() {
        return usuarioLogado.exigir().getId();
    }

    /** Grava o pedido e devolve como a tela mostra. */
    public SolicitacaoResponse registrar(SolicitacaoAprovacao pedido) {
        UUID id = solicitacoes.inserir(pedido, pedido.matriculaEnvolvida().orElse(null));
        return resposta(solicitacoes.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido")));
    }

    /** Conflito de interesse de quem está logado com a família do aluno da matrícula. */
    public void exigirSemConflitoNaMatricula(UUID matriculaId, String acao) {
        ConflitoInteresse.exigirAusente(cpfDoLogado(), controle.cpfsDaFamiliaDaMatricula(matriculaId), acao);
    }

    /** Conflito de interesse de quem está logado com a família do aluno da cobrança. */
    public void exigirSemConflitoNaCobranca(UUID cobrancaId, String acao) {
        ConflitoInteresse.exigirAusente(cpfDoLogado(), controle.cpfsDaFamiliaDaCobranca(cobrancaId), acao);
    }

    public Decisor decisorAtual() {
        Usuario u = usuarioLogado.exigir();
        return new Decisor(u.getId(), u.pode(Modulo.APROVACOES, Acao.GRAVAR), controle.cpfDoUsuario(u.getId()).orElse(null));
    }

    public List<String> cpfsDaFamilia(SolicitacaoAprovacao s) {
        return s.matriculaEnvolvida().map(controle::cpfsDaFamiliaDaMatricula).orElse(List.of());
    }

    public SolicitacaoResponse resposta(SolicitacaoComNomes linha) {
        return respostas(List.of(linha)).getFirst();
    }

    /** Converte vários pedidos consultando o login uma vez só. */
    public List<SolicitacaoResponse> respostas(List<SolicitacaoComNomes> linhas) {
        var usuario = usuarioLogado.usuario();
        UUID eu = usuario.map(Usuario::getId).orElse(null);
        boolean podeAprovar = usuario.map(u -> u.pode(Modulo.APROVACOES, Acao.GRAVAR)).orElse(false);
        return linhas.stream().map(linha -> {
            SolicitacaoAprovacao s = linha.solicitacao();
            boolean minha = s.getSolicitadaPor().equals(eu);
            var d = s.getDecisao();
            return new SolicitacaoResponse(s.getId(), s.getTipo(), s.getTipo().getRotulo(), d.status(), s.getResumo(),
                    s.matriculaEnvolvida().orElse(null), linha.solicitante(), s.getSolicitadaEm(), linha.decisor(),
                    d.decididaEm(), d.motivo(), minha, s.pendente() && podeAprovar && !minha);
        }).toList();
    }

    public String descricaoMatricula(UUID matriculaId) {
        return controle.descricaoMatricula(matriculaId).orElse("matrícula");
    }

    public String nomeSerie(UUID serieId) {
        return controle.nomeSerie(serieId).orElse("série");
    }

    private String cpfDoLogado() {
        return usuarioLogado.usuario().flatMap(u -> controle.cpfDoUsuario(u.getId())).orElse(null);
    }
}
