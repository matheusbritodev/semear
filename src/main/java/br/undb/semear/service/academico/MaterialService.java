package br.undb.semear.service.academico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.MaterialDtos.ListaMaterialResponse;
import br.undb.semear.model.entity.academico.ListaMaterial;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.academico.MaterialRepository;

/** Listas de materiais (RF06): a escola mantém por série e ano; o portal mostra a da série do filho. */
@Service
public class MaterialService {

    private final MaterialRepository materiais;

    public MaterialService(MaterialRepository materiais) {
        this.materiais = materiais;
    }

    /** Sem lista cadastrada, devolve a lista vazia (a escola começa a preencher). */
    public ListaMaterialResponse lista(UUID serieId, int ano) {
        return ListaMaterialResponse.de(materiais.buscar(serieId, ano).orElse(new ListaMaterial(null, serieId, ano, List.of())), null);
    }

    @Transactional
    public ListaMaterialResponse adicionar(UUID serieId, int ano, String descricao, int quantidade) {
        ListaMaterial lista = materiais.buscar(serieId, ano).orElseGet(() -> materiais.criar(serieId, ano));
        ListaMaterial.Item novo = lista.adicionar(descricao, quantidade);
        materiais.inserirItem(lista.getId(), novo);
        return lista(serieId, ano);
    }

    @Transactional
    public ListaMaterialResponse alterar(UUID itemId, String descricao, int quantidade) {
        ListaMaterial lista = listaDoItem(itemId);
        materiais.alterarItem(lista.alterar(itemId, descricao, quantidade));
        return lista(lista.getSerieId(), lista.getAno());
    }

    @Transactional
    public ListaMaterialResponse remover(UUID itemId) {
        ListaMaterial lista = listaDoItem(itemId);
        lista.remover(itemId);
        materiais.removerItem(itemId);
        return lista(lista.getSerieId(), lista.getAno());
    }

    /** Começa a lista de um ano a partir da de outro (ex.: preparar 2027 com a de 2026). Só para lista vazia. */
    @Transactional
    public ListaMaterialResponse copiar(UUID serieId, int deAno, int paraAno) {
        ListaMaterial origem = materiais.buscar(serieId, deAno)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de " + deAno));
        ListaMaterial destino = materiais.buscar(serieId, paraAno).orElseGet(() -> materiais.criar(serieId, paraAno));
        if (!destino.getItens().isEmpty()) {
            throw new RegraNegocioException("LISTA_JA_PREENCHIDA", "A lista de " + paraAno + " já tem itens");
        }
        for (ListaMaterial.Item item : origem.getItens()) {
            materiais.inserirItem(destino.getId(), destino.adicionar(item.descricao(), item.quantidade()));
        }
        return lista(serieId, paraAno);
    }

    /** Lista da matrícula vigente do aluno (portal). */
    public Optional<ListaMaterialResponse> doAluno(UUID alunoId) {
        return materiais.listaDoAluno(alunoId).map(l -> ListaMaterialResponse.de(l.lista(), l.serie()));
    }

    private ListaMaterial listaDoItem(UUID itemId) {
        return materiais.buscarPorItem(itemId).orElseThrow(() -> new RecursoNaoEncontradoException("Item da lista"));
    }
}
