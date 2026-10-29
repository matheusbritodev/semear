package br.undb.semear.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.exception.DadoInvalidoException;

class EmailTelefoneTest {

    @Test
    void emailFicaEmMinusculasComoOBancoExige() {
        assertThat(new Email("  Secretaria@Escola.Exemplo ").getEndereco()).isEqualTo("secretaria@escola.exemplo");
    }

    @Test
    void rejeitaEmailInvalido() {
        assertThatThrownBy(() -> new Email("sem-arroba")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void telefoneCelularEFixo() {
        assertThat(new Telefone("(98) 99999-1234").formatado()).isEqualTo("(98) 99999-1234");
        assertThat(new Telefone("9833334444").formatado()).isEqualTo("(98) 3333-4444");
        assertThat(new Telefone("98999991234").ehCelular()).isTrue();
    }

    @Test
    void rejeitaTelefoneSemDdd() {
        assertThatThrownBy(() -> new Telefone("99999-1234")).isInstanceOf(DadoInvalidoException.class);
    }
}
