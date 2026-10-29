package br.undb.semear.repository.academico;

import java.util.List;

import br.undb.semear.model.entity.academico.TabelaPreco;

public interface TabelaPrecoRepository {

    List<TabelaPreco> doAno(int ano);

    /** Insere ou atualiza o preço da série no ano (erro se o ano letivo não existir). */
    void salvar(TabelaPreco preco);
}
