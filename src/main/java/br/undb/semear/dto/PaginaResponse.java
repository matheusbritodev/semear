package br.undb.semear.dto;

import java.util.List;
import java.util.function.Function;

/** Página de resultados de uma listagem. */
public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long total) {

    public <R> PaginaResponse<R> mapear(Function<T, R> conversor) {
        return new PaginaResponse<>(itens.stream().map(conversor).toList(), pagina, tamanho, total);
    }
}
