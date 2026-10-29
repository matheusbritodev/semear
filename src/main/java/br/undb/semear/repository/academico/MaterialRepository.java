package br.undb.semear.repository.academico;

import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.academico.ListaMaterial;

public interface MaterialRepository {

    /** Lista da série no ano do aluno (matrícula vigente mais recente), com o nome da série. */
    record ListaDoAluno(ListaMaterial lista, String serie, int ano) {
    }

    Optional<ListaMaterial> buscar(UUID serieId, int ano);

    Optional<ListaMaterial> buscarPorItem(UUID itemId);

    /** Cria a lista vazia; erro se o ano letivo não existir. */
    ListaMaterial criar(UUID serieId, int ano);

    UUID inserirItem(UUID listaId, ListaMaterial.Item item);

    void alterarItem(ListaMaterial.Item item);

    void removerItem(UUID itemId);

    /** Vazio se o aluno não tem matrícula vigente; lista sem itens se a escola ainda não cadastrou. */
    Optional<ListaDoAluno> listaDoAluno(UUID alunoId);
}
