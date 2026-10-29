package br.undb.semear.model.entity.matricula;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TermoAssinadoTest {

    private final List<Clausula> clausulas = List.of(new Clausula("Valores", "Mensalidade de R$ 398,00"),
            new Clausula("Atraso", "Multa de 2%"));

    @Test
    void mesmoTextoGeraMesmoCodigo() {
        TermoAssinado a = TermoAssinado.de("Contrato de Matrícula", clausulas);
        TermoAssinado b = TermoAssinado.de("Contrato de Matrícula", List.copyOf(clausulas));
        assertThat(a.hash()).hasSize(64).isEqualTo(b.hash());
        assertThat(a.integro()).isTrue();
    }

    @Test
    void reajusteDepoisDaAssinaturaMudaOCodigoEDenunciaAlteracao() {
        TermoAssinado assinado = TermoAssinado.de("Contrato de Matrícula", clausulas);
        List<Clausula> reajustado = List.of(new Clausula("Valores", "Mensalidade de R$ 420,00"), clausulas.get(1));
        assertThat(TermoAssinado.de("Contrato de Matrícula", reajustado).hash()).isNotEqualTo(assinado.hash());
        // Alguém troca o texto guardado sem trocar o código: a conferência acusa.
        assertThat(new TermoAssinado(assinado.titulo(), reajustado, assinado.hash()).integro()).isFalse();
    }

    @Test
    void evidenciaCortaCamposLongos() {
        EvidenciaAssinatura e = EvidenciaAssinatura.portal("2001:db8::1", "x".repeat(400));
        assertThat(e.navegador()).hasSize(255);
        assertThat(e.canal()).isEqualTo(EvidenciaAssinatura.Canal.PORTAL);
        assertThat(EvidenciaAssinatura.presencial(null).canal()).isEqualTo(EvidenciaAssinatura.Canal.PRESENCIAL);
    }
}
