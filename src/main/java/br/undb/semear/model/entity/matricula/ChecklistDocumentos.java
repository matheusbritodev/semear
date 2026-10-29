package br.undb.semear.model.entity.matricula;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import br.undb.semear.model.enums.TipoDocumentoAluno;

/**
 * Situação dos documentos de um aluno frente à lista obrigatória (RN12).
 * Recebe os documentos enviados e responde o que falta. Imutável.
 */
public final class ChecklistDocumentos {

    /** Um item da lista: o tipo e os arquivos enviados para ele (pode haver mais de um, ex.: frente e verso). */
    public record Item(TipoDocumentoAluno tipo, List<DocumentoAluno> documentos) {

        public boolean enviado() {
            return !documentos.isEmpty();
        }

        public boolean pendente() {
            return tipo.isObrigatorio() && !enviado();
        }
    }

    private final List<Item> itens;

    public ChecklistDocumentos(List<DocumentoAluno> enviados) {
        this.itens = Arrays.stream(TipoDocumentoAluno.values())
                .map(tipo -> new Item(tipo, enviados.stream()
                        .filter(d -> d.getTipo() == tipo)
                        .sorted(Comparator.comparing(DocumentoAluno::getEnviadoEm,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList()))
                .toList();
    }

    public List<Item> getItens() {
        return itens;
    }

    public Set<TipoDocumentoAluno> pendentes() {
        return itens.stream().filter(Item::pendente).map(Item::tipo).collect(Collectors.toUnmodifiableSet());
    }

    /** RN12: todos os obrigatórios entregues. */
    public boolean estaCompleto() {
        return pendentes().isEmpty();
    }
}
