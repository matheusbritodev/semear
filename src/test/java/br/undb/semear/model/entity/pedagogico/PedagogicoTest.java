package br.undb.semear.model.entity.pedagogico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.TipoCalendario;
import br.undb.semear.model.enums.TipoPlano;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.ValorNota;

class PedagogicoTest {

    private static final AnoLetivo ANO = new AnoLetivo(UUID.randomUUID(), 2026, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 12, 11));
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);
    private final CalendarioLetivo calendario = new CalendarioLetivo(ANO, List.of(
            new EventoCalendario(null, 2026, LocalDate.of(2026, 10, 12), TipoCalendario.FERIADO, "Nossa Senhora Aparecida"),
            new EventoCalendario(null, 2026, LocalDate.of(2026, 10, 17), TipoCalendario.AULA, "Sábado letivo")));

    private final UUID regente = UUID.randomUUID();
    private final UUID ingles = UUID.randomUUID();
    private final UUID discIngles = UUID.randomUUID();
    private final UUID discMatematica = UUID.randomUUID();
    private final ProfessoresDaTurma professores = new ProfessoresDaTurma(List.of(
            new ProfessoresDaTurma.Atribuicao(regente, null), new ProfessoresDaTurma.Atribuicao(ingles, discIngles)));

    private final UUID ana = UUID.randomUUID();
    private final UUID bia = UUID.randomUUID();
    private final Set<UUID> ativos = Set.of(ana, bia);

    private DiarioClasse registrar(LocalDate dia, List<DiarioClasse.Presenca> chamada) {
        return DiarioClasse.registrar(null, UUID.randomUUID(), regente, dia, "Frações", chamada, ativos, calendario, HOJE);
    }

    @Test
    void chamadaCompletaComFaltaJustificada() {
        DiarioClasse d = registrar(LocalDate.of(2026, 10, 27), List.of(
                new DiarioClasse.Presenca(ana, true, "ignorada"), new DiarioClasse.Presenca(bia, false, " Consulta médica ")));
        assertThat(d.presentes()).isEqualTo(1);
        assertThat(d.faltas()).isEqualTo(1);
        assertThat(d.getPresencas()).extracting(DiarioClasse.Presenca::justificativa).containsExactly(null, "Consulta médica");
    }

    @Test
    void naoAceitaFeriadoFimDeSemanaNemFuturo() {
        List<DiarioClasse.Presenca> todos = List.of(new DiarioClasse.Presenca(ana, true, null), new DiarioClasse.Presenca(bia, true, null));
        assertThatThrownBy(() -> registrar(LocalDate.of(2026, 10, 12), todos)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Nossa Senhora Aparecida");
        assertThatThrownBy(() -> registrar(LocalDate.of(2026, 10, 25), todos)).hasMessageContaining("Fim de semana");
        assertThatThrownBy(() -> registrar(LocalDate.of(2026, 10, 29), todos)).extracting("codigo").isEqualTo("AULA_NO_FUTURO");
        // Sábado letivo marcado no calendário pode.
        assertThat(registrar(LocalDate.of(2026, 10, 17), todos).presentes()).isEqualTo(2);
    }

    @Test
    void chamadaPrecisaTerTodosOsAlunosAtivosESoEles() {
        assertThatThrownBy(() -> registrar(LocalDate.of(2026, 10, 27), List.of(new DiarioClasse.Presenca(ana, true, null))))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> registrar(LocalDate.of(2026, 10, 27), List.of(new DiarioClasse.Presenca(ana, true, null),
                new DiarioClasse.Presenca(bia, true, null), new DiarioClasse.Presenca(UUID.randomUUID(), true, null))))
                .extracting("codigo").isEqualTo("ALUNO_FORA_DA_TURMA");
    }

    @Test
    void rd07_soQuemLecionaEOEspecialistaFicaComSuaDisciplina() {
        assertThat(professores.leciona(regente)).isTrue();
        assertThat(professores.leciona(UUID.randomUUID())).isFalse();
        assertThat(professores.podeLancarNotas(regente, discMatematica)).isTrue();
        assertThat(professores.podeLancarNotas(regente, discIngles)).isFalse();
        assertThat(professores.podeLancarNotas(ingles, discIngles)).isTrue();
        assertThat(professores.podeLancarNotas(ingles, discMatematica)).isFalse();
        assertThat(professores.polivalente(regente)).isTrue();
        assertThat(professores.especialidadesDe(ingles)).containsExactly(discIngles);
    }

    @Test
    void notasValidamFaixaProfessorENivel() {
        assertThatThrownBy(() -> new ValorNota(new BigDecimal("10.5"))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new ValorNota(new BigDecimal("7.333"))).isInstanceOf(DadoInvalidoException.class);
        assertThat(new ValorNota(new BigDecimal("7.5")).getValor()).isEqualByComparingTo("7.50");

        java.util.HashMap<UUID, BigDecimal> notas = new java.util.HashMap<>();
        notas.put(ana, new BigDecimal("8.5"));
        notas.put(bia, null);
        LancamentoNotas l = new LancamentoNotas(NivelEnsino.ENSINO_FUNDAMENTAL, professores, regente, discMatematica, 4, notas, ativos);
        assertThat(l.getNotas().get(ana)).isPresent();
        assertThat(l.getNotas().get(bia)).isEmpty();

        assertThatThrownBy(() -> new LancamentoNotas(NivelEnsino.ENSINO_FUNDAMENTAL, professores, regente, discIngles, 4, Map.of(), ativos))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> new LancamentoNotas(NivelEnsino.EDUCACAO_INFANTIL, professores, regente, discMatematica, 1, Map.of(), ativos))
                .extracting("codigo").isEqualTo("INFANTIL_SEM_NOTAS");
    }

    @Test
    void bimestreDivideOAnoEmQuatroPartes() {
        assertThat(Boletim.bimestreDe(ANO, LocalDate.of(2026, 1, 15))).isEqualTo(1);
        assertThat(Boletim.bimestreDe(ANO, LocalDate.of(2026, 3, 10))).isEqualTo(1);
        assertThat(Boletim.bimestreDe(ANO, LocalDate.of(2026, 6, 1))).isEqualTo(2);
        assertThat(Boletim.bimestreDe(ANO, LocalDate.of(2026, 9, 1))).isEqualTo(3);
        assertThat(Boletim.bimestreDe(ANO, HOJE)).isEqualTo(4);
        assertThat(Boletim.bimestreDe(ANO, LocalDate.of(2027, 1, 5))).isEqualTo(4);
    }

    @Test
    void frequenciaAbaixoDe75PorCento() {
        assertThat(new Boletim.Frequencia(40, 29).abaixoDoMinimo()).isTrue();
        assertThat(new Boletim.Frequencia(40, 30).abaixoDoMinimo()).isFalse();
        assertThat(new Boletim.Frequencia(0, 0).abaixoDoMinimo()).isFalse();
    }

    @Test
    void soOAutorAlteraOPlano() {
        PlanoAula plano = PlanoAula.novo(UUID.randomUUID(), regente, TipoPlano.PLANO_AULA, LocalDate.of(2026, 11, 3),
                "Somar frações", "Material dourado", ANO);
        assertThatThrownBy(() -> plano.alterar(ingles, TipoPlano.PLANO_AULA, LocalDate.of(2026, 11, 4), "x", "y", ANO))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> PlanoAula.novo(UUID.randomUUID(), regente, TipoPlano.PLANO_AULA, LocalDate.of(2027, 3, 1),
                "x", "y", ANO)).isInstanceOf(DadoInvalidoException.class);
    }
}
