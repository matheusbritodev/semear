package br.undb.semear.model.entity.controle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import br.undb.semear.model.entity.controle.SolicitacaoAprovacao.Decisao;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.enums.TipoSolicitacao;

/** Reconstrói a subclasse certa a partir do tipo e dos dados guardados em jsonb. */
public final class SolicitacaoFactory {

    private SolicitacaoFactory() {
    }

    public static SolicitacaoAprovacao criar(UUID id, TipoSolicitacao tipo, String resumo, UUID solicitadaPor,
                                             OffsetDateTime solicitadaEm, Decisao decisao, Map<String, String> d) {
        return switch (tipo) {
            case CONCESSAO_BOLSA -> new SolicitacaoConcessaoBolsa(id, resumo, solicitadaPor, solicitadaEm, decisao,
                    UUID.fromString(d.get("matriculaId")), UUID.fromString(d.get("bolsaId")),
                    d.get("vigenciaInicio") == null ? null : LocalDate.parse(d.get("vigenciaInicio")));
            case ALTERACAO_BOLSA -> new SolicitacaoAlteracaoBolsa(id, resumo, solicitadaPor, solicitadaEm, decisao,
                    UUID.fromString(d.get("bolsaId")), d.get("nome"), d.get("descricao"),
                    TipoDesconto.valueOf(d.get("tipoDesconto")), new BigDecimal(d.get("valor")),
                    Boolean.parseBoolean(d.get("ativa")));
            case ALTERACAO_PRECO -> new SolicitacaoPreco(id, resumo, solicitadaPor, solicitadaEm, decisao,
                    UUID.fromString(d.get("serieId")), Integer.parseInt(d.get("ano")),
                    new BigDecimal(d.get("valorMatricula")), new BigDecimal(d.get("valorMensal")),
                    Integer.parseInt(d.get("diaVencimento")), Integer.parseInt(d.get("diasPrazoMatricula")));
            case COPIA_PRECOS -> new SolicitacaoCopiaPrecos(id, resumo, solicitadaPor, solicitadaEm, decisao,
                    Integer.parseInt(d.get("deAno")), Integer.parseInt(d.get("paraAno")),
                    new BigDecimal(d.get("reajustePercentual")));
        };
    }
}
