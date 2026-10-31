package br.undb.semear.model.entity.controle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.entity.pessoa.MatrizPermissoes;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Permissao;

/** SoD: matriz de funções incompatíveis, conflito de interesse e regra de quando a bolsa precisa de aprovação. */
class SegregacaoFuncoesTest {

    private static final String CPF_FUNCIONARIA = "529.982.247-25";

    private MatrizPermissoes matrizPadrao() {
        return new MatrizPermissoes(Map.of(
                PerfilUsuario.ADMIN, List.of(new Permissao(Modulo.CONFIGURACOES, true, true),
                        new Permissao(Modulo.USUARIOS, true, true), new Permissao(Modulo.FINANCEIRO, true, true),
                        new Permissao(Modulo.APROVACOES, true, true)),
                PerfilUsuario.SECRETARIA, List.of(new Permissao(Modulo.MATRICULA, true, true)),
                PerfilUsuario.FINANCEIRO, List.of(new Permissao(Modulo.FINANCEIRO, true, true),
                        new Permissao(Modulo.BOLSAS, true, true), new Permissao(Modulo.APROVACOES, true, true))));
    }

    @Test
    void matrizPadraoNaoTemConflitoForaDoAdmin() {
        assertThat(matrizPadrao().conflitos()).allMatch(PoliticaSegregacao.Conflito::excecaoDocumentada);
        assertThat(matrizPadrao().conflitos()).extracting(PoliticaSegregacao.Conflito::perfil)
                .containsOnly(PerfilUsuario.ADMIN);
    }

    @Test
    void secretariaQueMatriculaNaoPodeDarBaixa() {
        MatrizPermissoes m = matrizPadrao();
        assertThatThrownBy(() -> m.alterar(PerfilUsuario.SECRETARIA, Modulo.FINANCEIRO, true, true))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("CONFLITO_SOD");
        // A matriz volta ao que era: a célula recusada não fica gravada pela metade.
        assertThat(m.buscar(PerfilUsuario.SECRETARIA, Modulo.FINANCEIRO)).isEmpty();
    }

    @Test
    void consultarOutroModuloNaoEhConflito() {
        Permissao p = matrizPadrao().alterar(PerfilUsuario.SECRETARIA, Modulo.FINANCEIRO, true, false);
        assertThat(p.podeLer()).isTrue();
    }

    @Test
    void financeiroNaoPodeGanharUsuarios() {
        assertThatThrownBy(() -> matrizPadrao().alterar(PerfilUsuario.FINANCEIRO, Modulo.USUARIOS, true, true))
                .extracting("codigo").isEqualTo("CONFLITO_SOD");
    }

    @Test
    void adminTemExcecaoDocumentada() {
        Permissao p = matrizPadrao().alterar(PerfilUsuario.ADMIN, Modulo.BOLSAS, true, true);
        assertThat(p.podeGravar()).isTrue();
    }

    @Test
    void auditoriaEhSomenteLeitura() {
        assertThatThrownBy(() -> matrizPadrao().alterar(PerfilUsuario.ADMIN, Modulo.AUDITORIA, true, true))
                .extracting("codigo").isEqualTo("AUDITORIA_SOMENTE_LEITURA");
    }

    @Test
    void conflitoDeInteressePeloCpfComOuSemPontuacao() {
        assertThat(ConflitoInteresse.existe(CPF_FUNCIONARIA, List.of("52998224725"))).isTrue();
        assertThat(ConflitoInteresse.existe(CPF_FUNCIONARIA, List.of("11144477735"))).isFalse();
        assertThat(ConflitoInteresse.existe(null, List.of("52998224725"))).isFalse();
        assertThatThrownBy(() -> ConflitoInteresse.exigirAusente(CPF_FUNCIONARIA, List.of("52998224725"), "a baixa"))
                .isInstanceOf(AcessoNegadoException.class)
                .extracting("codigo").isEqualTo("CONFLITO_DE_INTERESSE");
    }

    @Test
    void bolsaSoPrecisaDeAprovacaoQuandoMexeNoDesconto() {
        Bolsa irmaos = new Bolsa(null, "Desconto Irmãos", null, TipoDesconto.PERCENTUAL, BigDecimal.TEN, true);
        assertThat(irmaos.alteracaoExigeAprovacao(TipoDesconto.PERCENTUAL, new BigDecimal("10.00"), true)).isFalse();
        assertThat(irmaos.alteracaoExigeAprovacao(TipoDesconto.PERCENTUAL, BigDecimal.TEN, false)).isFalse();
        assertThat(irmaos.alteracaoExigeAprovacao(TipoDesconto.PERCENTUAL, new BigDecimal("50"), true)).isTrue();
        assertThat(irmaos.alteracaoExigeAprovacao(TipoDesconto.VALOR_FIXO, BigDecimal.TEN, true)).isTrue();
        Bolsa inativa = new Bolsa(null, "Antiga", null, TipoDesconto.PERCENTUAL, BigDecimal.TEN, false);
        assertThat(inativa.alteracaoExigeAprovacao(TipoDesconto.PERCENTUAL, BigDecimal.TEN, true)).isTrue();
    }
}
