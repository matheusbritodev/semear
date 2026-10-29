package br.undb.semear.service.conta;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.LimiteTentativasException;

class ContaTest {

    /** Relógio que dá para avançar no teste. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-10-28T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }
    }

    @Test
    void limitadorBloqueiaDepoisDoMaximoELiberaQuandoAJanelaPassa() {
        RelogioAjustavel relogio = new RelogioAjustavel();
        LimitadorTentativas limitador = new LimitadorTentativas(3, Duration.ofHours(1), relogio);
        for (int i = 0; i < 3; i++) {
            limitador.registrar("10.0.0.1");
        }
        assertThatThrownBy(() -> limitador.registrar("10.0.0.1")).isInstanceOf(LimiteTentativasException.class);
        assertThatCode(() -> limitador.registrar("10.0.0.2")).doesNotThrowAnyException();

        relogio.avancar(Duration.ofMinutes(61));
        assertThatCode(() -> limitador.registrar("10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void senhaPrecisaDeTamanhoLetraENumero() {
        assertThatThrownBy(() -> ContaService.validarSenha("abc123")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> ContaService.validarSenha("somenteletras")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> ContaService.validarSenha("1234567890")).isInstanceOf(DadoInvalidoException.class);
        assertThatCode(() -> ContaService.validarSenha("semear2026")).doesNotThrowAnyException();
    }
}
