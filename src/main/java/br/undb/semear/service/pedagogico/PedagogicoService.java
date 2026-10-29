package br.undb.semear.service.pedagogico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.pedagogico.PedagogicoDtos.ChamadaResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.DiarioRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.DiarioResumoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.DisciplinaOpcaoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.FrequenciaAlunoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.ItemChamadaResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.LancarNotasRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.NotaAlunoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.NotaRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.NotasTurmaResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.PlanoRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.PlanoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.TurmaPedagogicaResponse;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.entity.pedagogico.Boletim;
import br.undb.semear.model.entity.pedagogico.CalendarioLetivo;
import br.undb.semear.model.entity.pedagogico.DiarioClasse;
import br.undb.semear.model.entity.pedagogico.LancamentoNotas;
import br.undb.semear.model.entity.pedagogico.PlanoAula;
import br.undb.semear.model.entity.pedagogico.ProfessoresDaTurma;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.pedagogico.PedagogicoRepository;
import br.undb.semear.repository.pedagogico.PedagogicoRepository.AlunoAtivo;
import br.undb.semear.repository.pedagogico.PedagogicoRepository.DiarioResumo;
import br.undb.semear.repository.pedagogico.PedagogicoRepository.Disciplina;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.academico.TurmaService;

/**
 * Pedagógico (RF08). O acesso à turma passa pelo TurmaService (professor só vê as suas);
 * escrever exige estar vinculado à turma (RD07) — o Model confere e o banco confere de novo.
 */
@Service
public class PedagogicoService {

    private final PedagogicoRepository pedagogico;
    private final TurmaService turmas;
    private final AnoLetivoService anosLetivos;
    private final EscopoAcesso escopo;

    public PedagogicoService(PedagogicoRepository pedagogico, TurmaService turmas, AnoLetivoService anosLetivos,
                             EscopoAcesso escopo) {
        this.pedagogico = pedagogico;
        this.turmas = turmas;
        this.anosLetivos = anosLetivos;
        this.escopo = escopo;
    }

    /* ---- Turmas ---- */

    public List<TurmaPedagogicaResponse> turmas(Integer ano) {
        Optional<UUID> eu = funcionarioOpcional();
        Map<UUID, String> nomes = nomesDisciplinas();
        LocalDate hoje = anosLetivos.hoje();
        List<TurmaPedagogicaResponse> lista = new ArrayList<>();
        for (Turma t : turmas.listar(ano)) {
            AnoLetivo anoLetivo = anoLetivo(t);
            ProfessoresDaTurma professores = pedagogico.professores(t.getId());
            List<String> minhas = new ArrayList<>();
            eu.ifPresent(id -> {
                if (professores.polivalente(id)) {
                    minhas.add("Polivalente");
                }
                professores.especialidadesDe(id).stream().map(nomes::get).sorted().forEach(minhas::add);
            });
            List<DiarioResumo> ultimos = pedagogico.diariosDaTurma(t.getId(), anoLetivo.getDataInicio(), anoLetivo.getDataFim());
            lista.add(new TurmaPedagogicaResponse(t.getId(), t.getAnoLetivo(), t.getNomeCompleto(), t.getSerie().getNivel(),
                    pedagogico.alunosAtivos(t.getId()).size(), List.copyOf(minhas),
                    ultimos.isEmpty() ? null : ultimos.get(0).data(), Boletim.bimestreDe(anoLetivo, hoje)));
        }
        return lista;
    }

    /* ---- Diário e chamada ---- */

    public ChamadaResponse chamada(UUID turmaId, LocalDate data) {
        Turma turma = turmas.buscar(turmaId);
        LocalDate hoje = anosLetivos.hoje();
        LocalDate dia = data == null ? hoje : data;
        CalendarioLetivo calendario = calendario(turma);
        Optional<UUID> eu = funcionarioOpcional();
        ProfessoresDaTurma professores = pedagogico.professores(turmaId);
        boolean leciono = eu.isPresent() && professores.leciona(eu.get());

        Optional<DiarioClasse> diario = leciono ? pedagogico.diario(turmaId, eu.get(), dia)
                : pedagogico.diariosDaTurma(turmaId, dia, dia).stream().findFirst().flatMap(d -> pedagogico.diario(d.id()));
        String professor = diario.flatMap(d -> pedagogico.diariosDaTurma(turmaId, dia, dia).stream()
                .filter(r -> r.id().equals(d.getId())).findFirst().map(DiarioResumo::professor)).orElse(null);
        Map<UUID, DiarioClasse.Presenca> marcadas = diario.map(d -> d.getPresencas().stream()
                .collect(Collectors.toMap(DiarioClasse.Presenca::matriculaId, p -> p))).orElse(Map.of());

        List<ItemChamadaResponse> alunos = pedagogico.alunosAtivos(turmaId).stream().map(a -> {
            DiarioClasse.Presenca p = marcadas.get(a.matriculaId());
            return new ItemChamadaResponse(a.matriculaId(), a.alunoId(), a.numeroCadastro(), a.nome(),
                    p == null || p.presente(), p == null ? null : p.justificativa());
        }).toList();
        String motivo = dia.isAfter(hoje) ? "Este dia ainda não chegou" : calendario.motivoSemAula(dia).orElse(null);
        return new ChamadaResponse(turmaId, turma.getNomeCompleto(), dia, motivo, diario.map(DiarioClasse::getId).orElse(null),
                professor, diario.map(DiarioClasse::getConteudo).orElse(null), leciono && motivo == null, alunos);
    }

    @Transactional
    public ChamadaResponse registrarDiario(UUID turmaId, LocalDate data, DiarioRequest pedido) {
        Turma turma = turmas.buscar(turmaId);
        UUID eu = exigirProfessorDaTurma(turmaId);
        Set<UUID> ativos = pedagogico.alunosAtivos(turmaId).stream().map(AlunoAtivo::matriculaId).collect(Collectors.toSet());
        UUID existente = pedagogico.diario(turmaId, eu, data).map(DiarioClasse::getId).orElse(null);
        DiarioClasse diario = DiarioClasse.registrar(existente, turmaId, eu, data, pedido.conteudo(),
                pedido.presencas().stream()
                        .map(p -> new DiarioClasse.Presenca(p.matriculaId(), p.presente(), p.justificativa())).toList(),
                ativos, calendario(turma), anosLetivos.hoje());
        pedagogico.salvarDiario(diario);
        return chamada(turmaId, data);
    }

    public List<DiarioResumoResponse> diarios(UUID turmaId, YearMonth mes) {
        turmas.buscar(turmaId);
        YearMonth alvo = mes == null ? YearMonth.from(anosLetivos.hoje()) : mes;
        Optional<UUID> eu = funcionarioOpcional();
        return pedagogico.diariosDaTurma(turmaId, alvo.atDay(1), alvo.atEndOfMonth()).stream()
                .map(d -> new DiarioResumoResponse(d.id(), d.data(), d.professor(), d.conteudo(), d.presentes(), d.faltas(),
                        eu.map(d.professorId()::equals).orElse(false)))
                .toList();
    }

    public List<FrequenciaAlunoResponse> frequencia(UUID turmaId) {
        turmas.buscar(turmaId);
        return pedagogico.frequenciaDaTurma(turmaId).stream().map(f -> {
            Boletim.Frequencia freq = new Boletim.Frequencia(f.aulas(), f.presencas());
            return new FrequenciaAlunoResponse(f.matriculaId(), f.alunoId(), f.nome(), f.aulas(), f.presencas(), freq.faltas(),
                    freq.percentual().orElse(null), freq.abaixoDoMinimo());
        }).toList();
    }

    /* ---- Planos ---- */

    public List<PlanoResponse> planos(UUID turmaId, LocalDate de, LocalDate ate) {
        turmas.buscar(turmaId);
        LocalDate inicio = de == null ? anosLetivos.hoje().withDayOfMonth(1) : de;
        LocalDate fim = ate == null ? inicio.plusMonths(2).minusDays(1) : ate;
        Optional<UUID> eu = funcionarioOpcional();
        return pedagogico.planosDaTurma(turmaId, inicio, fim).stream().map(p -> new PlanoResponse(p.plano().getId(),
                p.plano().getTipo(), p.plano().getDataPrevista(), p.plano().getObjetivo(), p.plano().getConteudo(), p.professor(),
                eu.map(p.plano().getProfessorId()::equals).orElse(false))).toList();
    }

    @Transactional
    public UUID criarPlano(UUID turmaId, PlanoRequest r) {
        Turma turma = turmas.buscar(turmaId);
        UUID eu = exigirProfessorDaTurma(turmaId);
        return pedagogico.salvarPlano(PlanoAula.novo(turmaId, eu, r.tipo(), r.dataPrevista(), r.objetivo(), r.conteudo(),
                anoLetivo(turma)));
    }

    @Transactional
    public void alterarPlano(UUID planoId, PlanoRequest r) {
        PlanoAula plano = buscarPlano(planoId);
        Turma turma = turmas.buscar(plano.getTurmaId());
        plano.alterar(exigirProfessorDaTurma(turma.getId()), r.tipo(), r.dataPrevista(), r.objetivo(), r.conteudo(),
                anoLetivo(turma));
        pedagogico.salvarPlano(plano);
    }

    @Transactional
    public void removerPlano(UUID planoId) {
        PlanoAula plano = buscarPlano(planoId);
        turmas.buscar(plano.getTurmaId());
        plano.exigirAutor(exigirProfessorDaTurma(plano.getTurmaId()));
        pedagogico.removerPlano(planoId);
    }

    /* ---- Notas ---- */

    public NotasTurmaResponse notas(UUID turmaId, UUID disciplinaId, Integer bimestre) {
        Turma turma = turmas.buscar(turmaId);
        List<Disciplina> todas = pedagogico.disciplinas();
        ProfessoresDaTurma professores = pedagogico.professores(turmaId);
        Optional<UUID> eu = funcionarioOpcional();
        List<DisciplinaOpcaoResponse> opcoes = todas.stream()
                .map(d -> new DisciplinaOpcaoResponse(d.id(), d.nome(), eu.map(id -> professores.podeLancarNotas(id, d.id())).orElse(false)))
                .toList();
        DisciplinaOpcaoResponse escolhida = opcoes.stream().filter(o -> o.id().equals(disciplinaId)).findFirst()
                .or(() -> opcoes.stream().filter(DisciplinaOpcaoResponse::podeLancar).findFirst())
                .or(() -> opcoes.stream().findFirst())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina"));
        int bim = bimestre != null ? bimestre : Boletim.bimestreDe(anoLetivo(turma), anosLetivos.hoje());
        Map<UUID, BigDecimal> lancadas = pedagogico.notas(turmaId, escolhida.id(), bim);
        List<NotaAlunoResponse> alunos = pedagogico.alunosAtivos(turmaId).stream()
                .map(a -> new NotaAlunoResponse(a.matriculaId(), a.alunoId(), a.numeroCadastro(), a.nome(), lancadas.get(a.matriculaId())))
                .toList();
        boolean fundamental = turma.getSerie().getNivel() == NivelEnsino.ENSINO_FUNDAMENTAL;
        return new NotasTurmaResponse(turmaId, turma.getNomeCompleto(), turma.getSerie().getNivel(), escolhida.id(),
                escolhida.nome(), bim, fundamental && escolhida.podeLancar(), opcoes, alunos);
    }

    @Transactional
    public NotasTurmaResponse lancarNotas(UUID turmaId, LancarNotasRequest r) {
        Turma turma = turmas.buscar(turmaId);
        UUID eu = escopo.funcionarioLogado();
        Set<UUID> ativos = pedagogico.alunosAtivos(turmaId).stream().map(AlunoAtivo::matriculaId).collect(Collectors.toSet());
        Map<UUID, BigDecimal> informadas = new LinkedHashMap<>();
        for (NotaRequest n : r.notas()) {
            informadas.put(n.matriculaId(), n.valor());
        }
        pedagogico.salvarNotas(new LancamentoNotas(turma.getSerie().getNivel(), pedagogico.professores(turmaId), eu,
                r.disciplinaId(), r.bimestre(), informadas, ativos));
        return notas(turmaId, r.disciplinaId(), r.bimestre());
    }

    /* ---- Apoio ---- */

    private UUID exigirProfessorDaTurma(UUID turmaId) {
        UUID eu = escopo.funcionarioLogado();
        if (!pedagogico.professores(turmaId).leciona(eu)) {
            throw new AcessoNegadoException("PROFESSOR_FORA_DA_TURMA", "Só o professor da turma registra diário e planos");
        }
        return eu;
    }

    /** Funcionário do login, se houver (gestor sem cadastro de funcionário só consulta). */
    private Optional<UUID> funcionarioOpcional() {
        try {
            return Optional.of(escopo.funcionarioLogado());
        } catch (AcessoNegadoException e) {
            return Optional.empty();
        }
    }

    private CalendarioLetivo calendario(Turma turma) {
        AnoLetivo ano = anoLetivo(turma);
        return new CalendarioLetivo(ano, pedagogico.eventos(ano.getAno()));
    }

    private AnoLetivo anoLetivo(Turma turma) {
        return anosLetivos.listar().stream().filter(a -> a.getAno() == turma.getAnoLetivo()).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ano letivo"));
    }

    private PlanoAula buscarPlano(UUID id) {
        return pedagogico.plano(id).orElseThrow(() -> new RecursoNaoEncontradoException("Plano"));
    }

    private Map<UUID, String> nomesDisciplinas() {
        Map<UUID, String> mapa = new LinkedHashMap<>();
        pedagogico.disciplinas().forEach(d -> mapa.put(d.id(), d.nome()));
        return mapa;
    }
}
