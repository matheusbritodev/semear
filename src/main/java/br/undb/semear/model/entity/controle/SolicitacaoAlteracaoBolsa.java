package br.undb.semear.model.entity.controle;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.enums.TipoSolicitacao;

/**
 * Mudar o desconto de um tipo de bolsa (ou reativá-la). Afeta todos os bolsistas de uma vez,
 * por isso passa pela aprovação. Só nome e descrição mudam direto.
 */
public class SolicitacaoAlteracaoBolsa extends SolicitacaoAprovacao {

    private final UUID bolsaId;
    private final String nome;
    private final String descricao;
    private final TipoDesconto tipoDesconto;
    private final BigDecimal valor;
    private final boolean ativa;

    public SolicitacaoAlteracaoBolsa(UUID id, String resumo, UUID solicitadaPor, OffsetDateTime solicitadaEm,
                                     Decisao decisao, UUID bolsaId, String nome, String descricao,
                                     TipoDesconto tipoDesconto, BigDecimal valor, boolean ativa) {
        super(id, TipoSolicitacao.ALTERACAO_BOLSA, resumo, solicitadaPor, solicitadaEm, decisao);
        this.bolsaId = Objects.requireNonNull(bolsaId, "bolsaId");
        // Valida já no pedido (mesmas regras da Bolsa), para não aprovar algo impossível de gravar.
        Bolsa conferida = new Bolsa(bolsaId, nome, descricao, tipoDesconto, valor, ativa);
        this.nome = conferida.getNome();
        this.descricao = conferida.getDescricao();
        this.tipoDesconto = tipoDesconto;
        this.valor = valor;
        this.ativa = ativa;
    }

    @Override
    public void efetivar(EfeitosAprovacao efeitos) {
        efeitos.alterarBolsa(bolsaId, nome, descricao, tipoDesconto, valor, ativa);
    }

    @Override
    public Map<String, String> dados() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("bolsaId", bolsaId.toString());
        d.put("nome", nome);
        if (descricao != null) {
            d.put("descricao", descricao);
        }
        d.put("tipoDesconto", tipoDesconto.name());
        d.put("valor", valor.toPlainString());
        d.put("ativa", String.valueOf(ativa));
        return d;
    }

    @Override
    public String chave() {
        return bolsaId.toString();
    }
}
