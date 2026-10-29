package br.undb.semear.model.entity.pedagogico;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quem leciona o quê numa turma (tabela turma_professor). Professor especialista (com disciplina)
 * lança só a sua; o polivalente (sem disciplina) lança as demais — nunca a de um especialista da turma.
 */
public final class ProfessoresDaTurma {

    /** Vínculo do professor com a turma. disciplinaId nulo = polivalente. */
    public record Atribuicao(UUID professorId, UUID disciplinaId) {
    }

    private final List<Atribuicao> atribuicoes;
    private final Set<UUID> disciplinasDeEspecialistas;

    public ProfessoresDaTurma(Collection<Atribuicao> atribuicoes) {
        this.atribuicoes = List.copyOf(atribuicoes);
        this.disciplinasDeEspecialistas = this.atribuicoes.stream().map(Atribuicao::disciplinaId)
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    /** RD07: está vinculado à turma (diário e planos). */
    public boolean leciona(UUID professorId) {
        return atribuicoes.stream().anyMatch(a -> a.professorId().equals(professorId));
    }

    public boolean podeLancarNotas(UUID professorId, UUID disciplinaId) {
        return atribuicoes.stream().filter(a -> a.professorId().equals(professorId)).anyMatch(a ->
                a.disciplinaId() == null ? !disciplinasDeEspecialistas.contains(disciplinaId) : a.disciplinaId().equals(disciplinaId));
    }

    /** Vinculado sem disciplina: o professor regente, que dá as disciplinas sem especialista. */
    public boolean polivalente(UUID professorId) {
        return atribuicoes.stream().anyMatch(a -> a.professorId().equals(professorId) && a.disciplinaId() == null);
    }

    /** Disciplinas em que o professor é o especialista da turma. */
    public List<UUID> especialidadesDe(UUID professorId) {
        return atribuicoes.stream().filter(a -> a.professorId().equals(professorId) && a.disciplinaId() != null)
                .map(Atribuicao::disciplinaId).toList();
    }

    /**
     * Novo vínculo: um único regente (sem disciplina) e um único especialista por disciplina.
     * Repetir o mesmo vínculo também é recusado.
     */
    public void exigirPodeVincular(UUID professorId, UUID disciplinaId) {
        if (disciplinaId == null && atribuicoes.stream().anyMatch(a -> a.disciplinaId() == null)) {
            throw new br.undb.semear.model.exception.ConflitoException("TURMA_COM_REGENTE",
                    "A turma já tem professor regente (polivalente). Remova o atual antes de trocar");
        }
        if (disciplinaId != null && disciplinasDeEspecialistas.contains(disciplinaId)) {
            throw new br.undb.semear.model.exception.ConflitoException("DISCIPLINA_COM_PROFESSOR",
                    "Esta disciplina já tem professor nesta turma");
        }
        Objects.requireNonNull(professorId, "professorId");
    }

    /** Das disciplinas informadas, as que o professor lança nesta turma. */
    public List<UUID> disciplinasDe(UUID professorId, Collection<UUID> todas) {
        return todas.stream().filter(d -> podeLancarNotas(professorId, d)).toList();
    }
}
