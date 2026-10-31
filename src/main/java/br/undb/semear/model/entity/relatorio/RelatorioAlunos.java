package br.undb.semear.model.entity.relatorio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import br.undb.semear.model.enums.StatusMatricula;

/**
 * Relatório de alunos matriculados num ano: as linhas (uma por matrícula) e os totais
 * por situação e por turma. Monta só com os dados que recebe; quem busca no banco é o service.
 * Não leva CPF nem contatos (LGPD: o relatório pode ser impresso).
 */
public final class RelatorioAlunos {

    /** Uma matrícula no relatório. */
    public record Linha(long numeroCadastro, String aluno, LocalDate dataNascimento, int idade, String turma,
                        StatusMatricula situacao, String responsavelFinanceiro, int documentosPendentes) {

        public Linha {
            Objects.requireNonNull(aluno, "aluno");
            Objects.requireNonNull(situacao, "situacao");
        }
    }

    public record TotalTurma(String turma, int alunos, int vigentes) {
    }

    private final int ano;
    private final List<Linha> linhas;

    public RelatorioAlunos(int ano, List<Linha> linhas) {
        this.ano = ano;
        this.linhas = linhas.stream()
                .sorted(Comparator.comparing(Linha::turma).thenComparing(Linha::aluno, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Matrícula que ocupa vaga: ativa ou aguardando o pagamento da taxa. */
    public static boolean vigente(StatusMatricula situacao) {
        return situacao == StatusMatricula.ATIVA || situacao == StatusMatricula.AGUARDANDO_PAGAMENTO;
    }

    public Map<StatusMatricula, Integer> porSituacao() {
        Map<StatusMatricula, Integer> totais = new EnumMap<>(StatusMatricula.class);
        for (StatusMatricula s : StatusMatricula.values()) {
            totais.put(s, 0);
        }
        linhas.forEach(l -> totais.merge(l.situacao(), 1, Integer::sum));
        return totais;
    }

    public List<TotalTurma> porTurma() {
        Map<String, int[]> contagem = new LinkedHashMap<>();
        for (Linha l : linhas) {
            int[] c = contagem.computeIfAbsent(l.turma(), t -> new int[2]);
            c[0]++;
            if (vigente(l.situacao())) {
                c[1]++;
            }
        }
        return contagem.entrySet().stream().map(e -> new TotalTurma(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    public long vigentes() {
        return linhas.stream().filter(l -> vigente(l.situacao())).count();
    }

    /** Matrículas vigentes com algum documento obrigatório faltando. */
    public long comDocumentosPendentes() {
        return linhas.stream().filter(l -> vigente(l.situacao()) && l.documentosPendentes() > 0).count();
    }

    public int getAno() {
        return ano;
    }

    public List<Linha> getLinhas() {
        return linhas;
    }
}
