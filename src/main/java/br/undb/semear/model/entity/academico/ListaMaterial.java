package br.undb.semear.model.entity.academico;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;

/** Lista de materiais de uma série num ano letivo (RF06). Não aceita item repetido (mesma descrição). */
public class ListaMaterial extends Entidade {

    /** Item da lista. */
    public record Item(UUID id, String descricao, int quantidade) {
    }

    private final UUID serieId;
    private final int ano;
    private final List<Item> itens;

    public ListaMaterial(UUID id, UUID serieId, int ano, List<Item> itens) {
        super(id);
        this.serieId = Objects.requireNonNull(serieId, "serieId");
        this.ano = ano;
        this.itens = new ArrayList<>(itens);
    }

    /** Valida e devolve o item novo (sem id; o banco gera). */
    public Item adicionar(String descricao, int quantidade) {
        Item novo = validar(null, descricao, quantidade);
        itens.add(novo);
        return novo;
    }

    public Item alterar(UUID itemId, String descricao, int quantidade) {
        int posicao = posicao(itemId);
        Item alterado = validar(itemId, descricao, quantidade);
        itens.set(posicao, alterado);
        return alterado;
    }

    public void remover(UUID itemId) {
        itens.remove(posicao(itemId));
    }

    private Item validar(UUID itemId, String descricao, int quantidade) {
        String texto = descricao == null ? "" : descricao.trim().replaceAll("\\s+", " ");
        if (texto.isEmpty() || texto.length() > 200) {
            throw new DadoInvalidoException("descricao", "Descreva o item (até 200 caracteres)");
        }
        if (quantidade < 1 || quantidade > 999) {
            throw new DadoInvalidoException("quantidade", "A quantidade deve ser de 1 a 999");
        }
        String chave = chave(texto);
        boolean repetido = itens.stream().anyMatch(i -> !i.id().equals(itemId) && chave(i.descricao()).equals(chave));
        if (repetido) {
            throw new ConflitoException("ITEM_REPETIDO", "Este item já está na lista");
        }
        return new Item(itemId, texto, quantidade);
    }

    private int posicao(UUID itemId) {
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).id() != null && itens.get(i).id().equals(itemId)) {
                return i;
            }
        }
        throw new RecursoNaoEncontradoException("Item da lista");
    }

    /** Compara sem acento e sem diferença de maiúsculas ("Lápis" = "lapis"). */
    private static String chave(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public UUID getSerieId() {
        return serieId;
    }

    public int getAno() {
        return ano;
    }

    public List<Item> getItens() {
        return List.copyOf(itens);
    }
}
