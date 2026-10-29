package br.undb.semear.model.entity.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoContrato;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;

class ContratoTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-28T10:00:00-03:00");
    private final UUID mae = UUID.randomUUID();
    private final UUID pai = UUID.randomUUID();

    private final CondicoesContrato condicoes = new CondicoesContrato("Ana Souza", 42, "2º Ano", "A", 2027,
            LocalDate.of(2027, 2, 1), LocalDate.of(2027, 12, 10), Dinheiro.de("398.00"), Dinheiro.de("398.00"), 10,
            "Maria Souza");

    private Contrato pendente(TipoContrato tipo) {
        return ContratoFactory.criar(UUID.randomUUID(), UUID.randomUUID(), tipo, StatusContrato.PENDENTE, null, null);
    }

    @Test
    void rd03_responsavelVinculadoAssina() {
        Contrato c = pendente(TipoContrato.CONTRATO_MATRICULA);
        c.assinar(pai, List.of(mae, pai), StatusMatricula.AGUARDANDO_PAGAMENTO, AGORA);
        assertThat(c.getStatus()).isEqualTo(StatusContrato.ASSINADO);
        assertThat(c.getAssinadoPor()).isEqualTo(pai);
        assertThat(c.getAssinadoEm()).isEqualTo(AGORA);
    }

    @Test
    void rd03_quemNaoEhResponsavelNaoAssina() {
        Contrato c = pendente(TipoContrato.FICHA_SOCIO);
        assertThatThrownBy(() -> c.assinar(UUID.randomUUID(), List.of(mae), StatusMatricula.ATIVA, AGORA))
                .isInstanceOf(AcessoNegadoException.class).extracting("codigo").isEqualTo("ASSINANTE_INVALIDO");
        assertThat(c.getStatus()).isEqualTo(StatusContrato.PENDENTE);
    }

    @Test
    void naoAssinaDuasVezesNemDeMatriculaCancelada() {
        Contrato assinado = ContratoFactory.criar(UUID.randomUUID(), UUID.randomUUID(), TipoContrato.CONTRATO_MATRICULA,
                StatusContrato.ASSINADO, mae, AGORA);
        assertThatThrownBy(() -> assinado.assinar(mae, List.of(mae), StatusMatricula.ATIVA, AGORA))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> pendente(TipoContrato.CONTRATO_MATRICULA).assinar(mae, List.of(mae), StatusMatricula.CANCELADA, AGORA))
                .isInstanceOf(RegraNegocioException.class).extracting("codigo").isEqualTo("MATRICULA_NAO_VIGENTE");
    }

    @Test
    void rn07_cadaTipoTemSeuTextoMasAsClausulasComunsSaoAsMesmas() {
        List<Clausula> contrato = pendente(TipoContrato.CONTRATO_MATRICULA).clausulas(condicoes);
        List<Clausula> ficha = pendente(TipoContrato.FICHA_SOCIO).clausulas(condicoes);

        assertThat(contrato.get(0).titulo()).isEqualTo("Objeto");
        assertThat(ficha.get(0).titulo()).isEqualTo("Associação");
        // Só a mensalidade do Fundamental tem desconto de pontualidade (RN08).
        assertThat(contrato).extracting(Clausula::titulo).contains("Desconto de pontualidade");
        assertThat(ficha).extracting(Clausula::titulo).doesNotContain("Desconto de pontualidade");
        // Cláusulas comuns (atraso, bolsa, cancelamento) nas duas, com os valores da tabela.
        assertThat(contrato).extracting(Clausula::titulo).contains("Atraso", "Bolsa ou desconto condicional", "Cancelamento e transferência");
        assertThat(ficha).extracting(Clausula::titulo).contains("Atraso", "Bolsa ou desconto condicional", "Cancelamento e transferência");
        assertThat(contrato.get(1).texto()).contains("R$ 398,00").contains("dia 10");
        assertThat(contrato.get(contrato.size() - 1).texto()).startsWith("Maria Souza");
    }
}
