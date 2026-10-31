package br.undb.semear.model.entity.controle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.undb.semear.model.enums.TipoDesconto;

/**
 * O que acontece quando um pedido é aprovado. Cada tipo de solicitação chama o seu efeito
 * (despacho duplo): o Model decide qual, os services sabem como gravar.
 */
public interface EfeitosAprovacao {

    void concederBolsa(UUID matriculaId, UUID bolsaId, LocalDate vigenciaInicio, UUID solicitante);

    void alterarBolsa(UUID bolsaId, String nome, String descricao, TipoDesconto tipoDesconto, BigDecimal valor, boolean ativa);

    void salvarPreco(UUID serieId, int ano, BigDecimal valorMatricula, BigDecimal valorMensal, int diaVencimento,
                     int diasPrazoMatricula);

    void copiarPrecos(int deAno, int paraAno, BigDecimal reajustePercentual);
}
