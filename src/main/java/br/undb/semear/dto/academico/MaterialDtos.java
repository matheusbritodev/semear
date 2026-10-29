package br.undb.semear.dto.academico;

import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.academico.ListaMaterial;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Listas de materiais por série e ano (RF06). */
public final class MaterialDtos {

    private MaterialDtos() {
    }

    public record ItemResponse(UUID id, String descricao, int quantidade) {
    }

    /** {@code serie} vem preenchida no portal. Lista sem itens quando a escola ainda não cadastrou. */
    public record ListaMaterialResponse(UUID serieId, String serie, int ano, List<ItemResponse> itens) {

        public static ListaMaterialResponse de(ListaMaterial lista, String serie) {
            return new ListaMaterialResponse(lista.getSerieId(), serie, lista.getAno(),
                    lista.getItens().stream().map(i -> new ItemResponse(i.id(), i.descricao(), i.quantidade())).toList());
        }
    }

    public record NovoItemRequest(@NotNull UUID serieId, @NotNull Integer ano, @NotBlank @Size(max = 200) String descricao,
                                  @NotNull @Min(1) @Max(999) Integer quantidade) {
    }

    public record AlterarItemRequest(@NotBlank @Size(max = 200) String descricao, @NotNull @Min(1) @Max(999) Integer quantidade) {
    }

    public record CopiarListaRequest(@NotNull UUID serieId, @NotNull Integer deAno, @NotNull Integer paraAno) {
    }
}
