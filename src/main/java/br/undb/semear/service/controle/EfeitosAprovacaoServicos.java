package br.undb.semear.service.controle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.undb.semear.model.entity.controle.EfeitosAprovacao;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.service.academico.TabelaPrecoService;
import br.undb.semear.service.financeiro.BolsaService;

/** Liga cada efeito de pedido aprovado ao service que sabe gravá-lo. */
@Component
public class EfeitosAprovacaoServicos implements EfeitosAprovacao {

    private final BolsaService bolsas;
    private final TabelaPrecoService precos;

    public EfeitosAprovacaoServicos(BolsaService bolsas, TabelaPrecoService precos) {
        this.bolsas = bolsas;
        this.precos = precos;
    }

    @Override
    public void concederBolsa(UUID matriculaId, UUID bolsaId, LocalDate vigenciaInicio, UUID solicitante) {
        bolsas.efetivarConcessao(matriculaId, bolsaId, vigenciaInicio, solicitante);
    }

    @Override
    public void alterarBolsa(UUID bolsaId, String nome, String descricao, TipoDesconto tipoDesconto, BigDecimal valor,
                             boolean ativa) {
        bolsas.efetivarAlteracao(bolsaId, nome, descricao, tipoDesconto, valor, ativa);
    }

    @Override
    public void salvarPreco(UUID serieId, int ano, BigDecimal valorMatricula, BigDecimal valorMensal, int diaVencimento,
                            int diasPrazoMatricula) {
        precos.efetivarPreco(serieId, ano, valorMatricula, valorMensal, diaVencimento, diasPrazoMatricula);
    }

    @Override
    public void copiarPrecos(int deAno, int paraAno, BigDecimal reajustePercentual) {
        precos.efetivarCopia(deAno, paraAno, reajustePercentual);
    }
}
