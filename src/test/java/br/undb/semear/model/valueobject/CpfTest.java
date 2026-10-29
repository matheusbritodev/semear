package br.undb.semear.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.undb.semear.model.exception.DadoInvalidoException;

class CpfTest {

    @Test
    void aceitaCpfValidoComOuSemMascara() {
        assertThat(new Cpf("000.000.001-91").getDigitos()).isEqualTo("00000000191");
        assertThat(new Cpf("00000000272").formatado()).isEqualTo("000.000.002-72");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "00000000192", "11111111111", "abc.def.ghi-jk"})
    void rejeitaCpfInvalido(String entrada) {
        assertThatThrownBy(() -> new Cpf(entrada))
                .isInstanceOf(DadoInvalidoException.class)
                .extracting("campo").isEqualTo("cpf");
    }

    @Test
    void toStringNaoExpoeOCpf() {
        assertThat(new Cpf("00000000353").toString()).isEqualTo("***.***.***-53");
    }

    @Test
    void igualdadePorValor() {
        assertThat(new Cpf("000.000.001-91")).isEqualTo(new Cpf("00000000191"));
    }
}
