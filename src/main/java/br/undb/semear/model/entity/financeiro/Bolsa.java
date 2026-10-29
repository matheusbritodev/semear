package br.undb.semear.model.entity.financeiro;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Dinheiro;

/**
 * Tipo de bolsa (ex.: Desconto Irmãos 10%). Concedida a critério da escola (RN11).
 * O banco aplica o desconto nas cobranças (fn_desconto_bolsa); aqui ficam as regras de cadastro
 * e o mesmo cálculo, para a tela mostrar o efeito antes de conceder.
 */
public class Bolsa extends Entidade {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private String nome;
    private String descricao;
    private TipoDesconto tipoDesconto;
    private BigDecimal valor;
    private boolean ativa;

    public Bolsa(UUID id, String nome, String descricao, TipoDesconto tipoDesconto, BigDecimal valor, boolean ativa) {
        super(id);
        alterar(nome, descricao, tipoDesconto, valor, ativa);
    }

    /** Mesmas regras do CHECK do banco: valor positivo e percentual até 100. */
    public final void alterar(String nome, String descricao, TipoDesconto tipoDesconto, BigDecimal valor, boolean ativa) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 100) {
            throw new DadoInvalidoException("nome", "Informe o nome da bolsa (até 100 caracteres)");
        }
        Objects.requireNonNull(tipoDesconto, "tipoDesconto");
        if (valor == null || valor.signum() <= 0) {
            throw new DadoInvalidoException("valor", "O valor do desconto deve ser maior que zero");
        }
        if (tipoDesconto == TipoDesconto.PERCENTUAL && valor.compareTo(CEM) > 0) {
            throw new DadoInvalidoException("valor", "Desconto percentual não pode passar de 100%");
        }
        this.nome = nome.trim();
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        this.tipoDesconto = tipoDesconto;
        this.valor = valor;
        this.ativa = ativa;
    }

    public Dinheiro calcularDesconto(Dinheiro valorBruto) {
        return ativa ? tipoDesconto.aplicar(valorBruto, valor) : Dinheiro.ZERO;
    }

    /** RD06: as bolsas somam, mas o desconto nunca passa do valor bruto. */
    public static Dinheiro descontoTotal(Collection<Bolsa> bolsas, Dinheiro valorBruto) {
        Dinheiro soma = bolsas.stream().map(b -> b.calcularDesconto(valorBruto)).reduce(Dinheiro.ZERO, Dinheiro::somar);
        return soma.menor(valorBruto);
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoDesconto getTipoDesconto() {
        return tipoDesconto;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
