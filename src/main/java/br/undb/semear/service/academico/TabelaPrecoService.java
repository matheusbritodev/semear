package br.undb.semear.service.academico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.AcademicoDtos.PrecoRequest;
import br.undb.semear.dto.academico.AcademicoDtos.PrecoSerieResponse;
import br.undb.semear.model.entity.academico.TabelaPreco;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;
import br.undb.semear.repository.academico.TabelaPrecoRepository;

/**
 * Tabela de preços por série e ano. Vale para cobranças e contratos novos: as cobranças já geradas
 * mantêm o valor e os contratos assinados guardam o texto congelado (D16).
 */
@Service
public class TabelaPrecoService {

    private final TabelaPrecoRepository precos;
    private final SerieService series;

    public TabelaPrecoService(TabelaPrecoRepository precos, SerieService series) {
        this.precos = precos;
        this.series = series;
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

    @Transactional
    public List<PrecoSerieResponse> salvar(PrecoRequest r) {
        precos.salvar(new TabelaPreco(r.serieId(), r.ano(), Dinheiro.de(r.valorMatricula()), Dinheiro.de(r.valorMensal()),
                r.diaVencimento(), r.diasPrazoMatricula()));
        return doAno(r.ano());
    }

    /** Preenche as séries ainda sem preço no ano novo com os valores do ano base reajustados. Não sobrescreve. */
    @Transactional
    public List<PrecoSerieResponse> copiar(int deAno, int paraAno, BigDecimal reajuste) {
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
