package br.undb.semear.model.entity.pedagogico;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Diário de classe de um dia (RF08.1): o conteúdo dado e a chamada de todos os alunos ativos da turma.
 * Um por professor, turma e dia. RD07 (professor só na sua turma) é conferida pelo serviço e pelo banco.
 */
public class DiarioClasse extends Entidade {

    /** Presença de um aluno. Justificativa só faz sentido para falta. */
    public record Presenca(UUID matriculaId, boolean presente, String justificativa) {
    }

    private final UUID turmaId;
    private final UUID professorId;
    private final LocalDate data;
    private final String conteudo;
    private final List<Presenca> presencas;

    public DiarioClasse(UUID id, UUID turmaId, UUID professorId, LocalDate data, String conteudo, List<Presenca> presencas) {
        super(id);
        this.turmaId = Objects.requireNonNull(turmaId, "turmaId");
        this.professorId = Objects.requireNonNull(professorId, "professorId");
        this.data = Objects.requireNonNull(data, "data");
        this.conteudo = conteudo;
        this.presencas = List.copyOf(presencas);
    }

    /**
     * Registra (ou refaz) o diário do dia. Regras: dia com aula no calendário e que já chegou,
     * conteúdo preenchido e chamada completa, só com alunos ativos da turma.
     */
    public static DiarioClasse registrar(UUID idExistente, UUID turmaId, UUID professorId, LocalDate data, String conteudo,
                                         Collection<Presenca> chamada, Set<UUID> matriculasAtivas,
                                         CalendarioLetivo calendario, LocalDate hoje) {
        if (data == null) {
            throw new DadoInvalidoException("data", "Informe a data da aula");
        }
        if (data.isAfter(hoje)) {
            throw new RegraNegocioException("AULA_NO_FUTURO", "Não dá para registrar a aula de um dia que ainda não chegou");
        }
        calendario.motivoSemAula(data).ifPresent(motivo -> {
            throw new RegraNegocioException("SEM_AULA_NO_DIA", "Não há aula em " + formatar(data) + " (" + motivo + ")");
        });
        String texto = conteudo == null ? "" : conteudo.trim();
        if (texto.isEmpty() || texto.length() > 2000) {
            throw new DadoInvalidoException("conteudo", "Descreva o conteúdo da aula (até 2.000 caracteres)");
        }
        if (matriculasAtivas.isEmpty()) {
            throw new RegraNegocioException("TURMA_SEM_ALUNOS", "A turma não tem alunos com matrícula ativa");
        }
        Set<UUID> vistos = new HashSet<>();
        List<Presenca> normalizadas = chamada.stream().map(p -> {
            if (!matriculasAtivas.contains(p.matriculaId())) {
                throw new RegraNegocioException("ALUNO_FORA_DA_TURMA", "A chamada tem aluno que não está ativo nesta turma");
            }
            if (!vistos.add(p.matriculaId())) {
                throw new DadoInvalidoException("presencas", "Aluno repetido na chamada");
            }
            String justificativa = p.presente() || p.justificativa() == null || p.justificativa().isBlank()
                    ? null : p.justificativa().trim();
            if (justificativa != null && justificativa.length() > 255) {
                throw new DadoInvalidoException("justificativa", "Justificativa com até 255 caracteres");
            }
            return new Presenca(p.matriculaId(), p.presente(), justificativa);
        }).toList();
        if (!vistos.containsAll(matriculasAtivas)) {
            throw new DadoInvalidoException("presencas", "Faça a chamada de todos os alunos da turma");
        }
        return new DiarioClasse(idExistente, turmaId, professorId, data, texto, normalizadas);
    }

    public long presentes() {
        return presencas.stream().filter(Presenca::presente).count();
    }

    public long faltas() {
        return presencas.size() - presentes();
    }

    public UUID getTurmaId() {
        return turmaId;
    }

    public UUID getProfessorId() {
        return professorId;
    }

    public LocalDate getData() {
        return data;
    }

    public String getConteudo() {
        return conteudo;
    }

    public List<Presenca> getPresencas() {
        return presencas;
    }

    private static String formatar(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
