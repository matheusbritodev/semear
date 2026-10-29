package br.undb.semear.model.entity.pedagogico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Notas de uma matrícula por disciplina e bimestre (1 a 4) e a frequência no ano.
 * A média é parcial (dos bimestres já lançados). Não há regra de aprovação nos requisitos, então o boletim não decide isso.
 */
public final class Boletim {

    public static final int BIMESTRES = 4;

    /** Nota lançada. */
    public record Nota(String disciplina, int bimestre, BigDecimal valor) {

        public Nota {
            if (bimestre < 1 || bimestre > BIMESTRES) {
                throw new IllegalArgumentException("Bimestre deve ser de 1 a 4");
            }
        }
    }

    /** Registros de chamada do ano. */
    public record Frequencia(int aulas, int presencas) {

        public int faltas() {
            return aulas - presencas;
        }

        /** Frequência mínima exigida pela LDB (Lei 9.394/96, art. 24, VI). */
        public static final BigDecimal MINIMO = BigDecimal.valueOf(75);

        /** Abaixo de 75% de presença (só com chamada registrada). */
        public boolean abaixoDoMinimo() {
            return percentual().map(p -> p.compareTo(MINIMO) < 0).orElse(false);
        }

        /** Percentual de presença com uma casa, ou vazio se ainda não houve chamada. */
        public Optional<BigDecimal> percentual() {
            if (aulas == 0) {
                return Optional.empty();
            }
            return Optional.of(BigDecimal.valueOf(presencas * 100L).divide(BigDecimal.valueOf(aulas), 1, RoundingMode.HALF_UP));
        }
    }

    /**
     * Bimestre de uma data: o ano letivo dividido em 4 partes iguais (a escola não tem datas
     * oficiais de bimestre cadastradas). Antes do início = 1; depois do fim = 4.
     */
    public static int bimestreDe(br.undb.semear.model.entity.academico.AnoLetivo ano, java.time.LocalDate data) {
        long total = java.time.temporal.ChronoUnit.DAYS.between(ano.getDataInicio(), ano.getDataFim()) + 1;
        long decorridos = java.time.temporal.ChronoUnit.DAYS.between(ano.getDataInicio(), data);
        if (decorridos < 0) {
            return 1;
        }
        return (int) Math.min(BIMESTRES, 1 + decorridos * BIMESTRES / total);
    }

    private final Map<String, Map<Integer, BigDecimal>> notas = new TreeMap<>();
    private final Frequencia frequencia;

    public Boletim(Collection<Nota> lancadas, Frequencia frequencia) {
        lancadas.forEach(n -> notas.computeIfAbsent(n.disciplina(), d -> new TreeMap<>()).put(n.bimestre(), n.valor()));
        this.frequencia = frequencia;
    }

    public List<String> disciplinas() {
        return List.copyOf(notas.keySet());
    }

    public Optional<BigDecimal> nota(String disciplina, int bimestre) {
        return Optional.ofNullable(notas.getOrDefault(disciplina, Map.of()).get(bimestre));
    }

    /** Média dos bimestres lançados, com uma casa (ex.: 7,5). */
    public Optional<BigDecimal> mediaParcial(String disciplina) {
        Collection<BigDecimal> valores = notas.getOrDefault(disciplina, Map.of()).values();
        return media(valores);
    }

    /** Média das médias parciais das disciplinas. */
    public Optional<BigDecimal> mediaGeral() {
        return media(notas.keySet().stream().map(this::mediaParcial).flatMap(Optional::stream).toList());
    }

    /** Último bimestre com alguma nota lançada (0 se nenhum). */
    public int bimestresLancados() {
        return notas.values().stream().flatMap(m -> m.keySet().stream()).mapToInt(Integer::intValue).max().orElse(0);
    }

    public Frequencia getFrequencia() {
        return frequencia;
    }

    private static Optional<BigDecimal> media(Collection<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(soma.divide(BigDecimal.valueOf(valores.size()), 1, RoundingMode.HALF_UP));
    }
}
