package br.undb.semear.service.controle;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.undb.semear.dto.controle.ControleDtos.ConflitoSodResponse;
import br.undb.semear.dto.controle.ControleDtos.ContaRevisadaResponse;
import br.undb.semear.dto.controle.ControleDtos.RelatorioRevisaoResponse;
import br.undb.semear.dto.controle.ControleDtos.RevisaoRealizadaResponse;
import br.undb.semear.model.entity.controle.RevisaoAcesso;
import br.undb.semear.model.entity.pessoa.MatrizPermissoes;
import br.undb.semear.repository.controle.AuditoriaRepository;
import br.undb.semear.repository.controle.RevisaoAcessoRepository;
import br.undb.semear.repository.controle.RevisaoAcessoRepository.RevisaoRealizada;
import br.undb.semear.security.UsuarioLogado;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.pessoa.PermissaoService;

/**
 * Revisão trimestral de acessos (SOX). O relatório aponta contas que merecem atenção; o gestor corrige o que
 * for preciso na tela de Usuários e registra a revisão. O registro guarda a foto das contas e a âncora da
 * trilha de auditoria (último id e hash), usada depois para provar que a trilha não foi cortada.
 */
@Service
public class RevisaoAcessoService {

    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

    private final RevisaoAcessoRepository revisoes;
    private final AuditoriaRepository auditoria;
    private final PermissaoService permissoes;
    private final AnoLetivoService anosLetivos;
    private final UsuarioLogado usuarioLogado;
    private final ObjectMapper json;

    public RevisaoAcessoService(RevisaoAcessoRepository revisoes, AuditoriaRepository auditoria,
                                PermissaoService permissoes, AnoLetivoService anosLetivos, UsuarioLogado usuarioLogado,
                                ObjectMapper json) {
        this.revisoes = revisoes;
        this.auditoria = auditoria;
        this.permissoes = permissoes;
        this.anosLetivos = anosLetivos;
        this.usuarioLogado = usuarioLogado;
        this.json = json;
    }

    public RelatorioRevisaoResponse relatorio() {
        LocalDate hoje = anosLetivos.hoje();
        MatrizPermissoes matriz = permissoes.matriz();
        RevisaoAcesso revisao = new RevisaoAcesso(revisoes.contasDaEquipe(), matriz, hoje);
        List<RevisaoRealizada> historico = revisoes.listar();
        LocalDate ultima = historico.isEmpty() ? null
                : historico.getFirst().realizadaEm().atZoneSameInstant(FUSO).toLocalDate();
        LocalDate proxima = RevisaoAcesso.proximaRevisao(ultima, hoje);
        return new RelatorioRevisaoResponse(
                revisao.getContas().stream().map(ContaRevisadaResponse::de).toList(),
                revisao.totalAlertas(), revisao.contasComAlerta(), revisoes.contasDoPortalAtivas(),
                matriz.conflitos().stream().map(ConflitoSodResponse::de).toList(),
                ultima, proxima, !proxima.isAfter(hoje),
                historico.stream().map(h -> new RevisaoRealizadaResponse(h.id(), h.realizadaEm(), h.realizadaPor(),
                        h.contasRevisadas(), h.alertas(), h.observacao())).toList());
    }

    @Transactional
    public RelatorioRevisaoResponse registrar(String observacao) {
        RevisaoAcesso revisao = new RevisaoAcesso(revisoes.contasDaEquipe(), permissoes.matriz(), anosLetivos.hoje());
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("contas", revisao.getContas().stream().map(c -> Map.of(
                "usuarioId", c.conta().usuarioId().toString(),
                "perfil", c.conta().perfil().name(),
                "ativo", c.conta().ativo(),
                "alertas", c.alertas().stream().map(a -> a.tipo().name()).toList())).toList());
        auditoria.ultimo().ifPresent(u -> resultado.put("ancora", Map.of("id", u.id(), "hash", u.hash())));
        try {
            revisoes.salvar(usuarioLogado.exigir().getId(), revisao.getContas().size(), revisao.totalAlertas(),
                    observacao == null || observacao.isBlank() ? null : observacao.trim(), json.writeValueAsString(resultado));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resultado da revisão inválido", e);
        }
        return relatorio();
    }
}
