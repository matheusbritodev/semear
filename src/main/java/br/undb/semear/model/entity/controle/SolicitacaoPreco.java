package br.undb.semear.model.entity.controle;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.academico.TabelaPreco;
import br.undb.semear.model.enums.TipoSolicitacao;
import br.undb.semear.model.valueobject.Dinheiro;

/** Definir ou mudar o preço de uma série em um ano. */
public class SolicitacaoPreco extends SolicitacaoAprovacao {

    private final TabelaPreco preco;

    public SolicitacaoPreco(UUID id, String resumo, UUID solicitadaPor, OffsetDateTime solicitadaEm, Decisao decisao,
                            UUID serieId, int ano, BigDecimal valorMatricula, BigDecimal valorMensal, int diaVencimento,
                            int diasPrazoMatricula) {
        super(id, TipoSolicitacao.ALTERACAO_PRECO, resumo, solicitadaPor, solicitadaEm, decisao);
        // TabelaPreco valida dia de vencimento, prazo e valores já no pedido.
        this.preco = new TabelaPreco(Objects.requireNonNull(serieId, "serieId"), ano, Dinheiro.de(valorMatricula),
                Dinheiro.de(valorMensal), diaVencimento, diasPrazoMatricula);
    }

    @Override
    public void efetivar(EfeitosAprovacao efeitos) {
        efeitos.salvarPreco(preco.getSerieId(), preco.getAno(), preco.getValorMatricula().getValor(),
                preco.getValorMensal().getValor(), preco.getDiaVencimento(), preco.getDiasPrazoMatricula());
    }

    @Override
    public Map<String, String> dados() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("serieId", preco.getSerieId().toString());
        d.put("ano", String.valueOf(preco.getAno()));
        d.put("valorMatricula", preco.getValorMatricula().getValor().toPlainString());
        d.put("valorMensal", preco.getValorMensal().getValor().toPlainString());
        d.put("diaVencimento", String.valueOf(preco.getDiaVencimento()));
        d.put("diasPrazoMatricula", String.valueOf(preco.getDiasPrazoMatricula()));
        return d;
    }

    @Override
    public String chave() {
        return preco.getSerieId() + ":" + preco.getAno();
    }
}
