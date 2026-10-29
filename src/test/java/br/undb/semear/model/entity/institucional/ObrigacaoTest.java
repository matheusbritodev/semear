package br.undb.semear.model.entity.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.OrgaoExterno;
import br.undb.semear.model.enums.SituacaoObrigacao;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

class ObrigacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 29);

    private ObrigacaoInstitucional presenca(LocalDate vencimento, LocalDate ultimo) {
        return new ObrigacaoInstitucional(null, OrgaoExterno.SISTEMA_PRESENCA, "Sistema Presença", 2, vencimento, 30, ultimo,
                false, null);
    }

    @Test
    void situacaoPeloPrazoEAntecedencia() {
        assertThat(presenca(LocalDate.of(2026, 12, 15), null).situacaoEm(HOJE)).isEqualTo(SituacaoObrigacao.EM_DIA);
        assertThat(presenca(LocalDate.of(2026, 11, 20), null).situacaoEm(HOJE)).isEqualTo(SituacaoObrigacao.EM_ALERTA);
        ObrigacaoInstitucional atrasada = presenca(LocalDate.of(2026, 10, 20), null);
        assertThat(atrasada.situacaoEm(HOJE)).isEqualTo(SituacaoObrigacao.ATRASADA);
        assertThat(atrasada.diasRestantes(HOJE)).isEqualTo(-9);
    }

    @Test
    void d14_cumprirExigeComprovanteDoCicloAtual() {
        ObrigacaoInstitucional o = presenca(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 10, 5));
        // Comprovante enviado antes do último cumprimento é do ciclo anterior.
        List<OffsetDateTime> antigo = List.of(OffsetDateTime.parse("2026-10-04T10:00:00-03:00"));
        assertThatThrownBy(() -> o.cumprir(HOJE, antigo)).isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("SEM_COMPROVANTE");

        o.cumprir(HOJE, List.of(OffsetDateTime.parse("2026-10-29T09:00:00-03:00")));
        assertThat(o.getUltimoCumprimento()).isEqualTo(HOJE);
        assertThat(o.getProximoVencimento()).isEqualTo(LocalDate.of(2027, 2, 1)); // periódica: +2 meses, como fn_cumprir_obrigacao
        assertThat(o.isEncerrada()).isFalse();
    }

    @Test
    void comprovanteDeNoiteContaNoDiaLocal() {
        // 23h30 de 05/10 em São Luís já é 06/10 em UTC: continua sendo do dia do último cumprimento (ciclo anterior).
        ObrigacaoInstitucional o = presenca(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 10, 5));
        assertThat(o.comprovantesDoCiclo(List.of(OffsetDateTime.parse("2026-10-06T02:30:00Z")))).isZero();
    }

    @Test
    void obrigacaoUnicaEncerraAoCumprir() {
        ObrigacaoInstitucional alvara = ObrigacaoInstitucional.nova(OrgaoExterno.SMTT, "Vistoria do transporte escolar", null,
                LocalDate.of(2026, 11, 30), 15, null);
        List<OffsetDateTime> comprovante = List.of(OffsetDateTime.parse("2026-10-29T09:00:00-03:00"));
        alvara.cumprir(HOJE, comprovante);
        assertThat(alvara.situacaoEm(HOJE)).isEqualTo(SituacaoObrigacao.CUMPRIDA);
        assertThatThrownBy(() -> alvara.cumprir(HOJE, comprovante)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void validaCadastro() {
        assertThatThrownBy(() -> ObrigacaoInstitucional.nova(OrgaoExterno.CENSO_INEP, " ", 12, HOJE, 30, null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> ObrigacaoInstitucional.nova(OrgaoExterno.CENSO_INEP, "Censo", 0, HOJE, 30, null))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> ObrigacaoInstitucional.nova(OrgaoExterno.CENSO_INEP, "Censo", 12, HOJE, -1, null))
                .isInstanceOf(DadoInvalidoException.class);
    }
}
