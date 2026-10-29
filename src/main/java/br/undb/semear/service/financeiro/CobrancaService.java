package br.undb.semear.service.financeiro;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaDetalheResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.GeracaoResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.InadimplenciaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.InadimplenteResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.MesResumoResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ResumoAnoResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ValorNaDataResponse;
import br.undb.semear.integration.cora.CoraGateway;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.financeiro.Cobranca;
import br.undb.semear.model.entity.financeiro.CobrancaFactory;
import br.undb.semear.model.entity.financeiro.Pagamento;
import br.undb.semear.model.entity.financeiro.ReferenciaCora;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.exception.SemearException;
import br.undb.semear.model.valueobject.CalendarioDiasUteis;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.repository.financeiro.CobrancaRepository;
import br.undb.semear.repository.financeiro.CobrancaRepository.CobrancaComContexto;
import br.undb.semear.repository.financeiro.CobrancaRepository.Filtro;
import br.undb.semear.repository.financeiro.CobrancaRepository.ResumoMensal;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.service.academico.AnoLetivoService;

/**
 * Cobranças (RF04/RF05). O banco cria as cobranças e aplica a bolsa; este serviço monta as
 * entidades pela CobrancaFactory e deixa o Model calcular o valor na data (RN08/RN09).
 */
@Service
public class CobrancaService {

    private static final Logger LOG = LoggerFactory.getLogger(CobrancaService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final CobrancaRepository cobrancas;
    private final AnoLetivoService anosLetivos;
    private final CoraGateway cora;
    private final Clock relogio;
    private final EscopoAcesso escopo;

    public CobrancaService(CobrancaRepository cobrancas, AnoLetivoService anosLetivos, CoraGateway cora, Clock relogio,
                           EscopoAcesso escopo) {
        this.cobrancas = cobrancas;
        this.anosLetivos = anosLetivos;
        this.cora = cora;
        this.relogio = relogio;
        this.escopo = escopo;
    }

    /* ---- Consulta ---- */

    public PaginaResponse<CobrancaResponse> pesquisar(Filtro filtro) {
        LocalDate hoje = anosLetivos.hoje();
        Function<CobrancaComContexto, CobrancaResponse> conversor = conversor(hoje);
        return cobrancas.pesquisar(filtro, hoje).mapear(conversor);
    }

    public CobrancaDetalheResponse detalhar(UUID id, LocalDate data) {
        LocalDate hoje = anosLetivos.hoje();
        CobrancaComContexto linha = buscar(id);
        Cobranca cobranca = fabrica().criar(linha.dados());
        return new CobrancaDetalheResponse(conversor(hoje).apply(linha),
                ValorNaDataResponse.de(cobranca.calcular(data == null ? hoje : data)));
    }

    public boolean pagamentoSimulado() {
        return cora.simulado();
    }

    /** O responsável é o dono da cobrança (pagador)? Usado pelo portal. */
    public boolean pertenceAoResponsavel(UUID cobrancaId, UUID responsavelId) {
        return buscar(cobrancaId).dados().responsavelId().equals(responsavelId);
    }

    /* ---- Baixa ---- */

    /** Baixa presencial na escola, pelo valor do dia (com desconto ou encargos). */
    @Transactional
    public CobrancaDetalheResponse registrarPagamento(UUID id, OrigemPagamento origem) {
        if (origem != OrigemPagamento.PRESENCIAL) {
            throw new RegraNegocioException("ORIGEM_INVALIDA", "Pagamento pelo site é confirmado pelo banco Cora, não pela escola");
        }
        pagarPeloValorDoDia(id, origem);
        return detalhar(id, null);
    }

    /**
     * Pagamento pelo site (webhook do Cora ou simulação no portal). Reentrega é sucesso:
     * se já estiver paga, não faz nada (devolve false).
     */
    @Transactional
    public boolean confirmarPagamentoSite(String coraId) {
        UUID id = cobrancas.buscarIdPorCora(coraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Boleto " + coraId));
        return confirmarPagamentoSite(id);
    }

    @Transactional
    public boolean confirmarPagamentoSite(UUID cobrancaId) {
        Cobranca cobranca = fabrica().criar(buscar(cobrancaId).dados());
        if (cobranca.getPagamento() != null) {
            return false;
        }
        try {
            pagarPeloValorDoDia(cobrancaId, OrigemPagamento.SITE);
            return true;
        } catch (SemearException e) {
            // Dois avisos ao mesmo tempo: o banco trava a linha e o segundo vê "já está paga".
            if ("COBRANCA_JA_PAGA".equals(e.getCodigo())) {
                return false;
            }
            throw e;
        }
    }

    private void pagarPeloValorDoDia(UUID id, OrigemPagamento origem) {
        Cobranca cobranca = fabrica().criar(buscar(id).dados());
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        Pagamento pagamento = new Pagamento(agora, cobranca.valorAPagar(anosLetivos.hoje()), origem);
        cobranca.pagar(pagamento);
        // Presencial: guarda quem recebeu. Site (Cora): não há funcionário envolvido.
        UUID quem = origem == OrigemPagamento.PRESENCIAL ? escopo.funcionarioOpcional().orElse(null) : null;
        cobrancas.registrarPagamento(id, origem, pagamento.valorPago(), agora, quem);
        LOG.info("Cobrança {} paga ({}, {})", id, origem, cobranca.getTipo());
    }

    /* ---- Geração e boletos ---- */

    @Transactional
    public GeracaoResponse gerar(int ano, int mesInicio, int mesFim) {
        if (mesInicio > mesFim) {
            throw new DadoInvalidoException("mesFim", "O mês final deve ser igual ou depois do inicial");
        }
        AnoLetivo anoLetivo = anosLetivos.listar().stream().filter(a -> a.getAno() == ano).findFirst()
                .orElseThrow(() -> new RegraNegocioException("ANO_LETIVO_INEXISTENTE", "Cadastre o ano letivo " + ano));
        if (mesFim < anoLetivo.getDataInicio().getMonthValue() || mesInicio > anoLetivo.getDataFim().getMonthValue()) {
            throw new RegraNegocioException("FORA_DO_ANO_LETIVO", "Os meses escolhidos estão fora do ano letivo " + ano);
        }
        int criadas = cobrancas.gerarPeriodo(ano, mesInicio, mesFim);
        LOG.info("Geradas {} cobranças de {}/{} a {}/{}", criadas, mesInicio, ano, mesFim, ano);
        return new GeracaoResponse(criadas, emitirBoletosPendentes(500));
    }

    /**
     * Geração automática (dia 25): cobranças do mês seguinte, se ele estiver dentro de um ano letivo.
     * Fora do ano letivo (ex.: janeiro) não faz nada. Substitui o agendamento no pg_cron.
     */
    @Transactional
    public GeracaoResponse gerarMesSeguinte() {
        java.time.YearMonth proximo = java.time.YearMonth.from(anosLetivos.hoje()).plusMonths(1);
        boolean letivo = anosLetivos.listar().stream().anyMatch(a -> a.getAno() == proximo.getYear()
                && !proximo.atEndOfMonth().isBefore(a.getDataInicio()) && !proximo.atDay(1).isAfter(a.getDataFim()));
        if (!letivo) {
            return new GeracaoResponse(0, 0);
        }
        return gerar(proximo.getYear(), proximo.getMonthValue(), proximo.getMonthValue());
    }

    /** Emite no Cora as cobranças em aberto sem boleto. Devolve quantos boletos foram emitidos. */
    @Transactional
    public int emitirBoletosPendentes(int limite) {
        int emitidos = 0;
        CobrancaFactory fabrica = fabrica();
        for (CobrancaComContexto linha : cobrancas.travarSemBoleto(limite)) {
            Cobranca cobranca = fabrica.criar(linha.dados());
            if (!cobranca.precisaEmitirBoleto()) {
                continue;
            }
            ReferenciaCora boleto = cora.emitir(cobranca, descricao(cobranca, linha));
            cobranca.vincularBoleto(boleto);
            if (cobrancas.vincularBoleto(cobranca.getId(), boleto)) {
                emitidos++;
            }
        }
        return emitidos;
    }

    /* ---- Inadimplência e resumo ---- */

    public InadimplenciaResponse inadimplencia() {
        LocalDate hoje = anosLetivos.hoje();
        Function<CobrancaComContexto, CobrancaResponse> conversor = conversor(hoje);
        Map<UUID, List<CobrancaResponse>> porAluno = cobrancas.vencidas(hoje).stream().map(conversor)
                .collect(Collectors.groupingBy(CobrancaResponse::alunoId, LinkedHashMap::new, Collectors.toList()));
        List<InadimplenteResponse> lista = new ArrayList<>();
        porAluno.forEach((alunoId, itens) -> {
            CobrancaResponse primeira = itens.get(0);
            lista.add(new InadimplenteResponse(alunoId, primeira.numeroCadastro(), primeira.aluno(), primeira.turma(),
                    primeira.responsavel(), primeira.telefone(), itens.size(),
                    itens.stream().mapToLong(CobrancaResponse::diasDeAtraso).max().orElse(0),
                    soma(itens, CobrancaResponse::valorFinal), soma(itens, CobrancaResponse::valorHoje), itens));
        });
        lista.sort(Comparator.comparingLong(InadimplenteResponse::maiorAtraso).reversed()
                .thenComparing(InadimplenteResponse::aluno));
        return new InadimplenciaResponse(lista.size(), lista.stream().mapToInt(InadimplenteResponse::cobrancas).sum(),
                lista.stream().map(InadimplenteResponse::valorOriginal).reduce(BigDecimal.ZERO, BigDecimal::add),
                lista.stream().map(InadimplenteResponse::valorAtualizado).reduce(BigDecimal.ZERO, BigDecimal::add),
                lista);
    }

    /** Previsto × recebido × em atraso por mês (soma os tipos de cobrança). */
    public ResumoAnoResponse resumo(Integer ano) {
        int alvo = anosLetivos.anoOuCorrente(ano);
        Map<LocalDate, List<ResumoMensal>> porMes = cobrancas.resumo(alvo).stream()
                .collect(Collectors.groupingBy(ResumoMensal::competencia, java.util.TreeMap::new, Collectors.toList()));
        List<MesResumoResponse> meses = porMes.entrySet().stream().map(e -> somarMes(e.getKey(), e.getValue())).toList();
        return new ResumoAnoResponse(alvo, meses, somarMes(null,
                porMes.values().stream().flatMap(List::stream).toList()));
    }

    /* ---- Apoio ---- */

    /** Calendário de dias úteis com os feriados do banco (lidos a cada uso: são poucos e mudam raramente). */
    CobrancaFactory fabrica() {
        return new CobrancaFactory(new CalendarioDiasUteis(cobrancas.feriados()));
    }

    private Function<CobrancaComContexto, CobrancaResponse> conversor(LocalDate hoje) {
        CobrancaFactory fabrica = fabrica();
        return linha -> {
            Cobranca c = fabrica.criar(linha.dados());
            Pagamento p = c.getPagamento();
            String link = c.getCora() == null || !c.getStatus().emAberto() ? null : cora.linkPagamento(c.getCora(), c.getId());
            return new CobrancaResponse(c.getId(), c.getTipo(), c.getCompetencia().atDay(1), c.getMatriculaId(),
                    linha.alunoId(), linha.numeroCadastro(), linha.aluno(), linha.ano(), linha.turma(),
                    linha.responsavel(), linha.telefone(), c.getValorBruto().getValor(), c.getDescontoBolsa().getValor(),
                    c.getValorFinal().getValor(), c.getVencimento(), c.statusEm(hoje), c.diasDeAtraso(hoje),
                    c.valorAPagar(hoje).getValor(), p == null ? null : p.pagoEm(),
                    p == null ? null : p.valorPago().getValor(), p == null ? null : p.origem(), c.getCora() != null, link,
                    linha.baixaRegistradaPor());
        };
    }

    private CobrancaComContexto buscar(UUID id) {
        return cobrancas.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança"));
    }

    private static String descricao(Cobranca c, CobrancaComContexto linha) {
        String tipo = switch (c.getTipo()) {
            case MENSALIDADE -> "Mensalidade";
            case TAXA_ASSOCIADO -> "Taxa de Associado";
            case MATRICULA -> "Matrícula";
        };
        String mes = c.getCompetencia().getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return tipo + (c.getTipo() == TipoCobranca.MATRICULA ? " " + linha.ano() : " de " + mes + "/" + c.getCompetencia().getYear())
                + " — aluno nº " + linha.numeroCadastro();
    }

    private static BigDecimal soma(List<CobrancaResponse> itens, Function<CobrancaResponse, BigDecimal> campo) {
        return itens.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static MesResumoResponse somarMes(LocalDate competencia, List<ResumoMensal> linhas) {
        Dinheiro previsto = Dinheiro.ZERO, descontos = Dinheiro.ZERO, recebido = Dinheiro.ZERO, atraso = Dinheiro.ZERO;
        int quantidade = 0;
        for (ResumoMensal l : linhas) {
            quantidade += l.quantidade();
            previsto = previsto.somar(l.previsto());
            descontos = descontos.somar(l.descontos());
            recebido = recebido.somar(l.recebido());
            atraso = atraso.somar(l.emAtraso());
        }
        return new MesResumoResponse(competencia, quantidade, previsto.getValor(), descontos.getValor(),
                recebido.getValor(), atraso.getValor());
    }
}
