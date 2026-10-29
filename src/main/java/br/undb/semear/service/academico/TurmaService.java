package br.undb.semear.service.academico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.AcademicoDtos.AlertaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.AlunoDaTurmaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.ProfessorDaTurmaResponse;
import br.undb.semear.dto.academico.AcademicoDtos.OpcaoResponse;
import br.undb.semear.dto.academico.AcademicoDtos.OpcoesProfessoresResponse;
import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.entity.pessoa.Funcionario;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.service.pessoa.FuncionarioService;
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
    private final SerieService series;
    private final FuncionarioService funcionarios;

    public TurmaService(TurmaRepository turmas, AlertaRepository alertas, AnoLetivoService anosLetivos,
                        EscopoAcesso escopo, SerieService series, FuncionarioService funcionarios) {
        this.turmas = turmas;
        this.alertas = alertas;
        this.anosLetivos = anosLetivos;
        this.escopo = escopo;
        this.series = series;
        this.funcionarios = funcionarios;
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

    /* ---- Cadastro (configuração do ano) ---- */

    @Transactional
    public Turma criar(int ano, UUID serieId, String nome, int capacidade) {
        Serie serie = series.listar().stream().filter(s -> s.getId().equals(serieId)).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Série"));
        return turmas.salvar(Turma.nova(serie, ano, nome, capacidade));
    }

    /** Só turma sem alunos; os vínculos de professor saem junto (ON DELETE CASCADE). */
    @Transactional
    public void remover(UUID id) {
        Turma turma = buscar(id);
        turma.exigirRemovivel();
        turmas.remover(id);
    }

    /** Regente (sem disciplina) ou especialista; só funcionário professor e ativo (o banco confere o tipo). */
    @Transactional
    public List<ProfessorDaTurmaResponse> vincularProfessor(UUID turmaId, UUID funcionarioId, UUID disciplinaId) {
        buscar(turmaId);
        Funcionario funcionario = funcionarios.buscar(funcionarioId);
        if (!funcionario.podeLecionar() || !funcionario.isAtivo()) {
            throw new RegraNegocioException("NAO_E_PROFESSOR", "Só funcionário do tipo professor e ativo pode ser vinculado à turma");
        }
        turmas.atribuicoes(turmaId).exigirPodeVincular(funcionarioId, disciplinaId);
        turmas.vincularProfessor(turmaId, funcionarioId, disciplinaId);
        return turmas.listarProfessores(turmaId);
    }

    @Transactional
    public List<ProfessorDaTurmaResponse> removerProfessor(UUID turmaId, UUID vinculoId) {
        buscar(turmaId);
        if (!turmas.removerVinculoProfessor(turmaId, vinculoId)) {
            throw new RecursoNaoEncontradoException("Professor da turma");
        }
        return turmas.listarProfessores(turmaId);
    }

    public OpcoesProfessoresResponse opcoesProfessores() {
        return new OpcoesProfessoresResponse(
                funcionarios.pesquisar(null, TipoFuncionario.PROFESSOR).stream().filter(Funcionario::isAtivo)
                        .map(f -> new OpcaoResponse(f.getId(), f.getNome())).toList(),
                turmas.disciplinas().stream().map(d -> new OpcaoResponse(d.id(), d.nome())).toList());
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
