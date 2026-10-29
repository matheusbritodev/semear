package br.undb.semear.service.institucional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.institucional.ObrigacaoDtos.ComprovanteResponse;
import br.undb.semear.dto.institucional.ObrigacaoDtos.FuncionarioOpcaoResponse;
import br.undb.semear.dto.institucional.ObrigacaoDtos.ObrigacaoRequest;
import br.undb.semear.dto.institucional.ObrigacaoDtos.ObrigacaoResponse;
import br.undb.semear.dto.matricula.DocumentoDtos.LinkArquivoResponse;
import br.undb.semear.integration.ArmazenamentoArquivos;
import br.undb.semear.model.entity.institucional.ComprovanteObrigacao;
import br.undb.semear.model.entity.institucional.ObrigacaoInstitucional;
import br.undb.semear.model.enums.SituacaoObrigacao;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.ArquivoEnviado;
import br.undb.semear.repository.institucional.ObrigacaoRepository;
import br.undb.semear.repository.institucional.ObrigacaoRepository.ObrigacaoComContexto;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.matricula.DocumentoService;

/**
 * Obrigações institucionais (RF09): prazos com aviso, cumprimento com comprovante (D14)
 * e renovação automática das periódicas (fn_cumprir_obrigacao).
 */
@Service
public class ObrigacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(ObrigacaoService.class);

    private final ObrigacaoRepository obrigacoes;
    private final ArmazenamentoArquivos armazenamento;
    private final AnoLetivoService anosLetivos;
    private final Clock relogio;
    private final EscopoAcesso escopo;

    public ObrigacaoService(ObrigacaoRepository obrigacoes, ArmazenamentoArquivos armazenamento,
                            AnoLetivoService anosLetivos, Clock relogio, EscopoAcesso escopo) {
        this.obrigacoes = obrigacoes;
        this.armazenamento = armazenamento;
        this.anosLetivos = anosLetivos;
        this.relogio = relogio;
        this.escopo = escopo;
    }

    public List<ObrigacaoResponse> listar() {
        LocalDate hoje = anosLetivos.hoje();
        return obrigacoes.listar().stream().map(o -> resposta(o, hoje)).toList();
    }

    /** Atrasadas e dentro da antecedência de aviso (card do painel). */
    public List<ObrigacaoResponse> alertas() {
        return listar().stream()
                .filter(o -> o.situacao() == SituacaoObrigacao.ATRASADA || o.situacao() == SituacaoObrigacao.EM_ALERTA)
                .toList();
    }

    public ObrigacaoResponse detalhar(UUID id) {
        return resposta(buscar(id), anosLetivos.hoje());
    }

    @Transactional
    public ObrigacaoResponse criar(ObrigacaoRequest r) {
        UUID id = obrigacoes.salvar(ObrigacaoInstitucional.nova(r.orgao(), r.descricao(), r.periodicidadeMeses(),
                r.proximoVencimento(), r.diasAlerta(), r.responsavelId()));
        return detalhar(id);
    }

    @Transactional
    public ObrigacaoResponse alterar(UUID id, ObrigacaoRequest r) {
        ObrigacaoInstitucional o = buscar(id).obrigacao();
        o.alterar(r.orgao(), r.descricao(), r.periodicidadeMeses(), r.proximoVencimento(), r.diasAlerta(), r.responsavelId());
        obrigacoes.salvar(o);
        return detalhar(id);
    }

    /** O Model confere (comprovante do ciclo, não encerrada); o banco grava e programa o próximo vencimento. */
    @Transactional
    public ObrigacaoResponse cumprir(UUID id) {
        ObrigacaoComContexto c = buscar(id);
        c.obrigacao().cumprir(anosLetivos.hoje(), c.comprovantes().stream().map(ComprovanteObrigacao::getEnviadoEm).toList());
        obrigacoes.cumprir(id, escopo.funcionarioOpcional().orElse(null));
        LOG.info("Obrigação {} ({}) cumprida", id, c.obrigacao().getOrgao());
        return detalhar(id);
    }

    /** Só obrigação nunca cumprida e sem comprovantes pode ser apagada (cadastro errado). */
    @Transactional
    public void remover(UUID id) {
        ObrigacaoComContexto c = buscar(id);
        if (c.obrigacao().getUltimoCumprimento() != null || !c.comprovantes().isEmpty()) {
            throw new RegraNegocioException("OBRIGACAO_COM_HISTORICO",
                    "Esta obrigação já tem histórico (cumprimento ou comprovante) e não pode ser apagada");
        }
        obrigacoes.remover(id);
    }

    /* ---- Comprovantes ---- */

    @Transactional
    public ObrigacaoResponse enviarComprovante(UUID id, byte[] conteudo) {
        buscar(id);
        ArquivoEnviado arquivo = new ArquivoEnviado(conteudo);
        ComprovanteObrigacao novo = ComprovanteObrigacao.novo(id, arquivo);
        armazenamento.enviar(ComprovanteObrigacao.BUCKET, novo.getCaminho(), arquivo.getConteudo(),
                arquivo.getFormato().getContentType());
        try {
            obrigacoes.inserirComprovante(novo);
        } catch (RuntimeException e) {
            armazenamento.remover(ComprovanteObrigacao.BUCKET, novo.getCaminho());
            throw e;
        }
        return detalhar(id);
    }

    public LinkArquivoResponse link(UUID comprovanteId) {
        ComprovanteObrigacao c = comprovante(comprovanteId);
        String url = armazenamento.urlTemporaria(ComprovanteObrigacao.BUCKET, c.getCaminho(), DocumentoService.VALIDADE_LINK);
        return new LinkArquivoResponse(url, OffsetDateTime.now(relogio).plus(DocumentoService.VALIDADE_LINK));
    }

    /** Comprovante do ciclo já cumprido fica como histórico; só o do ciclo atual pode ser trocado. */
    @Transactional
    public ObrigacaoResponse removerComprovante(UUID comprovanteId) {
        ComprovanteObrigacao c = comprovante(comprovanteId);
        ObrigacaoInstitucional o = buscar(c.getObrigacaoId()).obrigacao();
        if (o.comprovantesDoCiclo(List.of(c.getEnviadoEm())) == 0) {
            throw new RegraNegocioException("COMPROVANTE_DE_CICLO_CUMPRIDO",
                    "Este comprovante é de um cumprimento já registrado e fica como histórico");
        }
        obrigacoes.removerComprovante(comprovanteId);
        armazenamento.remover(ComprovanteObrigacao.BUCKET, c.getCaminho());
        return detalhar(c.getObrigacaoId());
    }

    public List<FuncionarioOpcaoResponse> responsaveisPossiveis() {
        return obrigacoes.funcionariosAtivos().stream().map(f -> new FuncionarioOpcaoResponse(f.id(), f.nome())).toList();
    }

    private ObrigacaoComContexto buscar(UUID id) {
        return obrigacoes.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Obrigação"));
    }

    private ComprovanteObrigacao comprovante(UUID id) {
        return obrigacoes.comprovante(id).orElseThrow(() -> new RecursoNaoEncontradoException("Comprovante"));
    }

    private static ObrigacaoResponse resposta(ObrigacaoComContexto c, LocalDate hoje) {
        ObrigacaoInstitucional o = c.obrigacao();
        List<ComprovanteResponse> comprovantes = c.comprovantes().stream()
                .map(k -> new ComprovanteResponse(k.getId(), k.getExtensao(), k.getEnviadoEm(),
                        o.comprovantesDoCiclo(List.of(k.getEnviadoEm())) > 0))
                .toList();
        boolean podeCumprir = !o.isEncerrada() && comprovantes.stream().anyMatch(ComprovanteResponse::doCicloAtual);
        return new ObrigacaoResponse(o.getId(), o.getOrgao(), o.getOrgao().getRotulo(), o.getDescricao(),
                o.getPeriodicidadeMeses(), o.getProximoVencimento(), o.diasRestantes(hoje), o.getDiasAlerta(),
                o.situacaoEm(hoje), o.getUltimoCumprimento(), c.cumpridaPor(), o.getResponsavelId(), c.responsavel(),
                comprovantes, podeCumprir);
    }
}
