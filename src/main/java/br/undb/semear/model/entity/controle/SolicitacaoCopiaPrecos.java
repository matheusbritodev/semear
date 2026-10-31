package br.undb.semear.model.entity.controle;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.enums.TipoSolicitacao;
import br.undb.semear.model.exception.RegraNegocioException;

/** Preencher os preços do ano novo a partir de outro ano, com reajuste. */
public class SolicitacaoCopiaPrecos extends SolicitacaoAprovacao {

    private final int deAno;
    private final int paraAno;
    private final BigDecimal reajustePercentual;

    public SolicitacaoCopiaPrecos(UUID id, String resumo, UUID solicitadaPor, OffsetDateTime solicitadaEm,
                                  Decisao decisao, int deAno, int paraAno, BigDecimal reajustePercentual) {
        super(id, TipoSolicitacao.COPIA_PRECOS, resumo, solicitadaPor, solicitadaEm, decisao);
        if (deAno == paraAno) {
            throw new RegraNegocioException("MESMO_ANO", "Escolha anos diferentes para copiar");
        }
        this.deAno = deAno;
        this.paraAno = paraAno;
        this.reajustePercentual = Objects.requireNonNull(reajustePercentual, "reajustePercentual");
    }

    @Override
    public void efetivar(EfeitosAprovacao efeitos) {
        efeitos.copiarPrecos(deAno, paraAno, reajustePercentual);
    }

    @Override
    public Map<String, String> dados() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("deAno", String.valueOf(deAno));
        d.put("paraAno", String.valueOf(paraAno));
        d.put("reajustePercentual", reajustePercentual.toPlainString());
        return d;
    }

    @Override
    public String chave() {
        return String.valueOf(paraAno);
    }
}
