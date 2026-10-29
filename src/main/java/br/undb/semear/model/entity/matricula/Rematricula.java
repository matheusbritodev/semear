package br.undb.semear.model.entity.matricula;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Regras da rematrícula (RF01.5, RN03, RN13), sem depender de banco:
 * recebe os fatos sobre o aluno e decide se ele pode ser rematriculado e em qual turma.
 */
public final class Rematricula {

    /** Resultado da avaliação, com a mensagem que a família ou a secretaria verá. */
    public enum Situacao {
        DISPONIVEL("Pode ser rematriculado(a)"),
        JA_REMATRICULADO("Já tem matrícula no próximo ano"),
        PENDENCIA_FINANCEIRA("Há cobranças vencidas: regularize para rematricular (RN13)"),
        SEM_MATRICULA_ATIVA("Não tem matrícula ativa no ano atual"),
        CONCLUIU_ESCOLA("Concluiu o 5º ano: não há série seguinte na escola"),
        SEM_TURMA("A turma do próximo ano ainda não foi aberta"),
        SEM_VAGAS("Não há vagas na série seguinte (RN04)");

        private final String mensagem;

        Situacao(String mensagem) {
            this.mensagem = mensagem;
        }

        public String getMensagem() {
            return mensagem;
        }

        public boolean permite() {
            return this == DISPONIVEL;
        }
    }

    /**
     * Fatos necessários para decidir.
     * @param statusAtual        status da matrícula no ano corrente (vazio se não houver)
     * @param jaTemProximoAno    já existe matrícula no ano seguinte
     * @param possuiDebitos      há cobranças vencidas em aberto (RN13)
     * @param existeSerieSeguinte o aluno não está na última série
     * @param turmasDestino      turmas da série seguinte no ano seguinte
     */
    public record Fatos(Optional<StatusMatricula> statusAtual, boolean jaTemProximoAno, boolean possuiDebitos,
                        boolean existeSerieSeguinte, List<Turma> turmasDestino) {
    }

    /** Avaliação: situação e, se disponível, a turma sugerida. */
    public record Avaliacao(Situacao situacao, Optional<Turma> turmaSugerida) {
    }

    private Rematricula() {
    }

    /** A ordem das verificações importa: a primeira que falhar define a situação. */
    public static Avaliacao avaliar(Fatos fatos) {
        if (fatos.jaTemProximoAno()) {
            return new Avaliacao(Situacao.JA_REMATRICULADO, Optional.empty());
        }
        if (fatos.statusAtual().filter(s -> s == StatusMatricula.ATIVA).isEmpty()) {
            return new Avaliacao(Situacao.SEM_MATRICULA_ATIVA, Optional.empty());
        }
        if (!fatos.existeSerieSeguinte()) {
            return new Avaliacao(Situacao.CONCLUIU_ESCOLA, Optional.empty());
        }
        if (fatos.possuiDebitos()) {
            return new Avaliacao(Situacao.PENDENCIA_FINANCEIRA, Optional.empty());
        }
        if (fatos.turmasDestino().isEmpty()) {
            return new Avaliacao(Situacao.SEM_TURMA, Optional.empty());
        }
        Optional<Turma> comMaisVagas = fatos.turmasDestino().stream()
                .filter(Turma::temVaga)
                .max(Comparator.comparingInt(Turma::vagasRestantes));
        return comMaisVagas
                .map(t -> new Avaliacao(Situacao.DISPONIVEL, Optional.of(t)))
                .orElseGet(() -> new Avaliacao(Situacao.SEM_VAGAS, Optional.empty()));
    }

    /** Lança a exceção correspondente se a avaliação não permitir rematricular. */
    public static Turma exigirTurma(Avaliacao avaliacao) {
        if (!avaliacao.situacao().permite()) {
            throw new RegraNegocioException("REMATRICULA_" + avaliacao.situacao().name(), avaliacao.situacao().getMensagem());
        }
        return avaliacao.turmaSugerida().orElseThrow();
    }
}
