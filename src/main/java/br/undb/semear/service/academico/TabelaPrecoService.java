package br.undb.semear.service.academico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.AcademicoDtos.PrecoRequest;
import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.model.entity.controle.SolicitacaoCopiaPrecos;
import br.undb.semear.model.entity.controle.SolicitacaoPreco;
import br.undb.semear.dto.academico.AcademicoDtos.PrecoSerieResponse;
import br.undb.semear.model.entity.academico.TabelaPreco;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.repository.academico.TabelaPrecoRepository;
import br.undb.semear.service.controle.PedidosAprovacao;

/**
 * Tabela de preços por série e ano. Vale para cobranças e contratos novos: as cobranças já geradas
 * mantêm o valor e os contratos assinados guardam o texto congelado (D16).
 * <p>
 * SoD: toda mudança de preço é um pedido que outra pessoa aprova; os métodos "efetivar..." gravam depois da aprovação.
 */
@Service
public class TabelaPrecoService {

    private final TabelaPrecoRepository precos;
    private final SerieService series;
    private final PedidosAprovacao pedidos;

    public TabelaPrecoService(TabelaPrecoRepository precos, SerieService series, PedidosAprovacao pedidos) {
        this.precos = precos;
        this.series = series;
        this.pedidos = pedidos;
    }

    public List<PrecoSerieResponse> doAno(int ano) {
        Map<java.util.UUID, TabelaPreco> porSerie = precos.doAno(ano).stream()
                .collect(Collectors.toMap(TabelaPreco::getSerieId, Function.identity()));
        return series.listar().stream().map(s -> {
            TabelaPreco p = porSerie.get(s.getId());
            return new PrecoSerieResponse(s.getId(), s.getNome(), s.getNivel(), s.getOrdem(),
                    p == null ? null : p.getValorMatricula().getValor(), p == null ? null : p.getValorMensal().getValor(),
                    p == null ? null : p.getDiaVencimento(), p == null ? null : p.getDiasPrazoMatricula());
        }).toList();
    }

    /** Pede a mudança de preço de uma série (o pedido já valida dia de vencimento, prazo e valores). */
    @Transactional
    public SolicitacaoResponse solicitarPreco(PrecoRequest r) {
        Map<java.util.UUID, TabelaPreco> atuais = precos.doAno(r.ano()).stream()
                .collect(Collectors.toMap(TabelaPreco::getSerieId, Function.identity()));
        TabelaPreco atual = atuais.get(r.serieId());
        String antes = atual == null ? "sem preço" : "mensalidade " + atual.getValorMensal().toString();
        String resumo = pedidos.nomeSerie(r.serieId()) + " " + r.ano() + ": " + antes + " → mensalidade "
                + Dinheiro.de(r.valorMensal()).toString() + ", matrícula " + Dinheiro.de(r.valorMatricula()).toString()
                + ", vencimento dia " + r.diaVencimento();
        return pedidos.registrar(new SolicitacaoPreco(null, resumo, pedidos.solicitante(), null, null, r.serieId(), r.ano(),
                r.valorMatricula(), r.valorMensal(), r.diaVencimento(), r.diasPrazoMatricula()));
    }

    @Transactional
    public List<PrecoSerieResponse> efetivarPreco(java.util.UUID serieId, int ano, BigDecimal valorMatricula,
                                                  BigDecimal valorMensal, int diaVencimento, int diasPrazoMatricula) {
        precos.salvar(new TabelaPreco(serieId, ano, Dinheiro.de(valorMatricula), Dinheiro.de(valorMensal),
                diaVencimento, diasPrazoMatricula));
        return doAno(ano);
    }

    /** Pede a cópia dos preços de um ano para outro, com reajuste. */
    @Transactional
    public SolicitacaoResponse solicitarCopia(int deAno, int paraAno, BigDecimal reajuste) {
        if (precos.doAno(deAno).isEmpty()) {
            throw new RegraNegocioException("SEM_PRECOS_BASE", "O ano " + deAno + " não tem preços para copiar");
        }
        String resumo = "Preços de " + paraAno + " a partir de " + deAno + " com reajuste de "
                + reajuste.stripTrailingZeros().toPlainString() + "% (só séries ainda sem preço)";
        return pedidos.registrar(new SolicitacaoCopiaPrecos(null, resumo, pedidos.solicitante(), null, null, deAno, paraAno,
                reajuste));
    }

    /** Preenche as séries ainda sem preço no ano novo com os valores do ano base reajustados. Não sobrescreve. */
    @Transactional
    public List<PrecoSerieResponse> efetivarCopia(int deAno, int paraAno, BigDecimal reajuste) {
        if (deAno == paraAno) {
            throw new RegraNegocioException("MESMO_ANO", "Escolha anos diferentes para copiar");
        }
        List<TabelaPreco> base = precos.doAno(deAno);
        if (base.isEmpty()) {
            throw new RegraNegocioException("SEM_PRECOS_BASE", "O ano " + deAno + " não tem preços para copiar");
        }
        java.util.Set<java.util.UUID> jaTem = precos.doAno(paraAno).stream().map(TabelaPreco::getSerieId)
                .collect(Collectors.toSet());
        base.stream().filter(p -> !jaTem.contains(p.getSerieId()))
                .forEach(p -> precos.salvar(p.reajustada(paraAno, reajuste)));
        return doAno(paraAno);
    }
}
