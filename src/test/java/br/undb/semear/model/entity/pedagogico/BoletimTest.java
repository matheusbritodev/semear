package br.undb.semear.model.entity.pedagogico;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class BoletimTest {

    private static Boletim.Nota nota(String disciplina, int bimestre, String valor) {
        return new Boletim.Nota(disciplina, bimestre, new BigDecimal(valor));
    }

    @Test
    void mediaParcialSoDosBimestresLancados() {
        Boletim b = new Boletim(List.of(nota("Matemática", 1, "7.0"), nota("Matemática", 2, "8.5"),
                nota("Matemática", 3, "9.0"), nota("Artes", 1, "10"), nota("Artes", 2, "9")), new Boletim.Frequencia(0, 0));
        assertThat(b.mediaParcial("Matemática")).contains(new BigDecimal("8.2"));
        assertThat(b.mediaParcial("Artes")).contains(new BigDecimal("9.5"));
        assertThat(b.nota("Artes", 3)).isEmpty();
        assertThat(b.bimestresLancados()).isEqualTo(3);
        assertThat(b.mediaGeral()).contains(new BigDecimal("8.9"));
        assertThat(b.disciplinas()).containsExactly("Artes", "Matemática");
    }

    @Test
    void frequenciaComPercentualEFaltas() {
        Boletim.Frequencia f = new Boletim.Frequencia(60, 57);
        assertThat(f.faltas()).isEqualTo(3);
        assertThat(f.percentual()).contains(new BigDecimal("95.0"));
        assertThat(new Boletim.Frequencia(0, 0).percentual()).isEmpty();
    }

    @Test
    void educacaoInfantilSemNotas() {
        Boletim b = new Boletim(List.of(), new Boletim.Frequencia(40, 38));
        assertThat(b.disciplinas()).isEmpty();
        assertThat(b.mediaGeral()).isEmpty();
        assertThat(b.bimestresLancados()).isZero();
    }
}
