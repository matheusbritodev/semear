package br.undb.semear.model.entity.controle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.StatusSolicitacao;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.exception.DadoInvalidoException;

/** Dupla aprovação (maker-checker). */
class SolicitacaoAprovacaoTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-31T10:00:00-03:00");
    private static final UUID SOLICITANTE = UUID.randomUUID();
    private static final UUID APROVADOR = UUID.randomUUID();
    private static final UUID MATRICULA = UUID.randomUUID();
    private static final UUID BOLSA = UUID.randomUUID();

    private SolicitacaoConcessaoBolsa pedido() {
        return new SolicitacaoConcessaoBolsa(UUID.randomUUID(), "Desconto Irmãos para Ana", SOLICITANTE, AGORA, null,
                MATRICULA, BOLSA, LocalDate.of(2026, 11, 1));
    }

    private static Decisor aprovador() {
        return new Decisor(APROVADOR, true, "111.444.777-35");
    }

    @Test
    void quemPediuNaoAprova() {
        assertThatThrownBy(() -> pedido().aprovar(new Decisor(SOLICITANTE, true, null), List.of(), AGORA))
                .extracting("codigo").isEqualTo("AUTOAPROVACAO");
    }

    @Test
    void precisaDoModuloAprovacoes() {
        assertThatThrownBy(() -> pedido().aprovar(new Decisor(APROVADOR, false, null), List.of(), AGORA))
                .extracting("codigo").isEqualTo("SEM_PERMISSAO_APROVAR");
    }

    @Test
    void responsavelPeloAlunoNaoDecide() {
        assertThatThrownBy(() -> pedido().aprovar(aprovador(), List.of("11144477735"), AGORA))
                .extracting("codigo").isEqualTo("CONFLITO_DE_INTERESSE");
    }

    @Test
    void aprovarRegistraQuemEQuandoEAplicaOEfeito() {
        SolicitacaoConcessaoBolsa s = pedido();
        s.aprovar(aprovador(), List.of("52998224725"), AGORA);
        assertThat(s.getDecisao().status()).isEqualTo(StatusSolicitacao.APROVADA);
        assertThat(s.getDecisao().decididaPor()).isEqualTo(APROVADOR);

        List<String> chamados = new ArrayList<>();
        s.efetivar(new EfeitosVazios() {
            @Override
            public void concederBolsa(UUID matriculaId, UUID bolsaId, LocalDate inicio, UUID solicitante) {
                chamados.add(matriculaId + "|" + bolsaId + "|" + solicitante);
            }
        });
        assertThat(chamados).containsExactly(MATRICULA + "|" + BOLSA + "|" + SOLICITANTE);
    }

    @Test
    void naoSeDecideDuasVezes() {
        SolicitacaoConcessaoBolsa s = pedido();
        s.aprovar(aprovador(), List.of(), AGORA);
        assertThatThrownBy(() -> s.rejeitar(aprovador(), List.of(), "mudei de ideia", AGORA))
                .extracting("codigo").isEqualTo("SOLICITACAO_JA_DECIDIDA");
    }

    @Test
    void recusarExigeMotivo() {
        assertThatThrownBy(() -> pedido().rejeitar(aprovador(), List.of(), " ", AGORA))
                .isInstanceOf(DadoInvalidoException.class);
        SolicitacaoConcessaoBolsa s = pedido();
        s.rejeitar(aprovador(), List.of(), "Família não comprovou a renda", AGORA);
        assertThat(s.getDecisao().status()).isEqualTo(StatusSolicitacao.REJEITADA);
    }

    @Test
    void soQuemPediuCancela() {
        assertThatThrownBy(() -> pedido().cancelar(APROVADOR, AGORA)).extracting("codigo").isEqualTo("SO_O_SOLICITANTE");
        SolicitacaoConcessaoBolsa s = pedido();
        s.cancelar(SOLICITANTE, AGORA);
        assertThat(s.getDecisao().status()).isEqualTo(StatusSolicitacao.CANCELADA);
    }

    @Test
    void fabricaReconstroiCadaTipoComOsMesmosDados() {
        List<SolicitacaoAprovacao> pedidos = List.of(pedido(),
                new SolicitacaoAlteracaoBolsa(null, "Irmãos 10% → 15%", SOLICITANTE, AGORA, null, BOLSA,
                        "Desconto Irmãos", null, TipoDesconto.PERCENTUAL, new BigDecimal("15"), true),
                new SolicitacaoPreco(null, "Maternal 2027", SOLICITANTE, AGORA, null, UUID.randomUUID(), 2027,
                        new BigDecimal("350.00"), new BigDecimal("690.00"), 10, 3),
                new SolicitacaoCopiaPrecos(null, "2027 a partir de 2026", SOLICITANTE, AGORA, null, 2026, 2027,
                        new BigDecimal("6.5")));
        for (SolicitacaoAprovacao original : pedidos) {
            SolicitacaoAprovacao copia = SolicitacaoFactory.criar(original.getId(), original.getTipo(),
                    original.getResumo(), SOLICITANTE, AGORA, null, original.dados());
            assertThat(copia.getClass()).isEqualTo(original.getClass());
            assertThat(copia.dados()).isEqualTo(original.dados());
            assertThat(copia.chave()).isEqualTo(original.chave());
        }
    }

    @Test
    void pedidoDePrecoJaValidaOsValores() {
        assertThatThrownBy(() -> new SolicitacaoPreco(null, "Maternal 2027", SOLICITANTE, AGORA, null, UUID.randomUUID(),
                2027, new BigDecimal("350.00"), new BigDecimal("690.00"), 31, 3))
                .isInstanceOf(RuntimeException.class);
    }

    /** Implementação vazia para os testes sobrescreverem só o efeito que interessa. */
    private static class EfeitosVazios implements EfeitosAprovacao {

        @Override
        public void concederBolsa(UUID matriculaId, UUID bolsaId, LocalDate vigenciaInicio, UUID solicitante) {
        }

        @Override
        public void alterarBolsa(UUID bolsaId, String nome, String descricao, TipoDesconto tipoDesconto,
                                 BigDecimal valor, boolean ativa) {
        }

        @Override
        public void salvarPreco(UUID serieId, int ano, BigDecimal valorMatricula, BigDecimal valorMensal,
                                int diaVencimento, int diasPrazoMatricula) {
        }

        @Override
        public void copiarPrecos(int deAno, int paraAno, BigDecimal reajustePercentual) {
        }
    }
}
