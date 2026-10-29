package br.undb.semear.model.entity.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;

class ListaMaterialTest {

    private final UUID lapis = UUID.randomUUID();

    private ListaMaterial lista() {
        return new ListaMaterial(UUID.randomUUID(), UUID.randomUUID(), 2027,
                List.of(new ListaMaterial.Item(lapis, "Lápis preto nº 2", 4)));
    }

    @Test
    void naoAceitaItemRepetidoMesmoComAcentoOuMaiusculas() {
        assertThatThrownBy(() -> lista().adicionar("  LAPIS preto  nº 2 ", 2)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void validaDescricaoEQuantidade() {
        assertThatThrownBy(() -> lista().adicionar("  ", 1)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> lista().adicionar("Caderno", 0)).isInstanceOf(DadoInvalidoException.class);
        assertThat(lista().adicionar("  Caderno   brochura ", 3).descricao()).isEqualTo("Caderno brochura");
    }

    @Test
    void alterarOProprioItemNaoContaComoRepetido() {
        ListaMaterial l = lista();
        assertThat(l.alterar(lapis, "Lápis preto nº 2", 6).quantidade()).isEqualTo(6);
        l.remover(lapis);
        assertThat(l.getItens()).isEmpty();
    }
}
