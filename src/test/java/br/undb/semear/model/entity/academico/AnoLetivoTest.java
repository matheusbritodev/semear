package br.undb.semear.model.entity.academico;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.exception.RegraNegocioException;

class AnoLetivoTest {

    private final AnoLetivo ano2026 = new AnoLetivo(UUID.randomUUID(), 2026, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 12, 11));

    @Test
    void soEncerraDepoisDoUltimoDiaLetivo() {
        assertThatThrownBy(() -> ano2026.exigirEncerravel(LocalDate.of(2026, 10, 29)))
                .isInstanceOf(RegraNegocioException.class).extracting("codigo").isEqualTo("ANO_LETIVO_EM_ANDAMENTO");
        assertThatThrownBy(() -> ano2026.exigirEncerravel(LocalDate.of(2026, 12, 11))).isInstanceOf(RegraNegocioException.class);
        assertThatCode(() -> ano2026.exigirEncerravel(LocalDate.of(2026, 12, 12))).doesNotThrowAnyException();
    }
}
