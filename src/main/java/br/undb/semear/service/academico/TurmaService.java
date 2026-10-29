package br.undb.semear.service.academico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.AcademicoDtos.AlertaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.AlunoDaTurmaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.ProfessorDaTurmaResponse;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.academico.AlertaRepository;
import br.undb.semear.repository.academico.TurmaRepository;
import br.undb.semear.security.EscopoAcesso;

/** Casos de uso de turmas: ocupação (RF03/RN04), detalhes e alertas à secretaria. */
@Service
public class TurmaService {

    private final TurmaRepository turmas;
    private final AlertaRepository alertas;
    private final AnoLetivoService anosLetivos;
    private final EscopoAcesso escopo;

    public TurmaService(TurmaRepository turmas, AlertaRepository alertas, AnoLetivoService anosLetivos,
                        EscopoAcesso escopo) {
        this.turmas = turmas;
        this.alertas = alertas;
        this.anosLetivos = anosLetivos;
        this.escopo = escopo;
    }

    public List<Turma> listar(Integer ano) {
        return turmas.listarPorAno(anosLetivos.anoOuCorrente(ano), escopo.professorRestrito().orElse(null));
    }

    public Turma buscar(UUID id) {
        Turma turma = turmas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Turma"));
        Optional<UUID> professor = escopo.professorRestrito();
        if (professor.isPresent() && !turmas.professorLeciona(id, professor.get())) {
            throw new AcessoNegadoException("TURMA_DE_OUTRO_PROFESSOR", "Você só pode acessar as suas turmas");
        }
        return turma;
    }

    public List<AlunoDaTurmaResponse> alunos(UUID turmaId) {
        return turmas.listarAlunos(turmaId);
    }

    public List<ProfessorDaTurmaResponse> professores(UUID turmaId) {
        return turmas.listarProfessores(turmaId);
    }

    /** RN04: a própria Turma valida (1 a 30, nunca abaixo da ocupação); o trigger do banco confirma. */
    @Transactional
    public Turma alterarCapacidade(UUID id, int novaCapacidade) {
        Turma turma = buscar(id);
        turma.alterarCapacidade(novaCapacidade);
        return turmas.salvar(turma);
    }

    public List<AlertaResponse> alertas(boolean apenasNaoLidos) {
        return alertas.listar(apenasNaoLidos);
    }

    public void marcarAlertaComoLido(UUID id) {
        if (!alertas.marcarComoLido(id)) {
            throw new RecursoNaoEncontradoException("Alerta");
        }
    }
}
