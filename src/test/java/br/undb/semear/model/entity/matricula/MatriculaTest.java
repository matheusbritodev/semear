package br.undb.semear.model.entity.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoMatricula;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

class MatriculaTest {

    private Matricula matricula(StatusMatricula status) {
        return new Matricula(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2026, TipoMatricula.NOVA,
                status, LocalDate.of(2026, 1, 10), null);
    }

    @Test
    void cancelaMatriculaQuitadaComMotivo() {
        Matricula m = matricula(StatusMatricula.ATIVA);
        m.cancelar("Mudança da família para outra cidade", false);
        assertThat(m.getStatus()).isEqualTo(StatusMatricula.CANCELADA);
        assertThat(m.ocupaVaga()).isFalse();
        assertThat(m.getMotivoCancelamento()).contains("Mudança da família para outra cidade");
    }

    @Test
    void rn13_cancelamentoExigeQuitacao() {
        assertThatThrownBy(() -> matricula(StatusMatricula.ATIVA).cancelar("Transferência para outra escola", true))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("DEBITOS_EM_ABERTO");
    }

    @Test
    void motivoObrigatorio() {
        assertThatThrownBy(() -> matricula(StatusMatricula.AGUARDANDO_PAGAMENTO).cancelar("", false))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void transicoesInvalidasSaoRecusadas() {
        assertThatThrownBy(() -> matricula(StatusMatricula.ENCERRADA).cancelar("Pedido da família", false))
                .isInstanceOf(ConflitoException.class);
        assertThat(StatusMatricula.AGUARDANDO_PAGAMENTO.podeIrPara(StatusMatricula.ATIVA)).isTrue();
        assertThat(StatusMatricula.CANCELADA.podeIrPara(StatusMatricula.ATIVA)).isFalse();
    }
}
