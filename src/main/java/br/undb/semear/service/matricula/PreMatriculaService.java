package br.undb.semear.service.matricula;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.AcademicoDtos.TurmaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.NovaPreMatriculaRequest;
import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaDetalheResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaResumoResponse;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.entity.matricula.DadosCrianca;
import br.undb.semear.model.entity.matricula.PreMatricula;
import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.model.enums.TipoFarda;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.repository.academico.AnoLetivoRepository;
import br.undb.semear.repository.academico.SerieRepository;
import br.undb.semear.repository.academico.TurmaRepository;
import br.undb.semear.repository.matricula.FluxoMatriculaRepository;
import br.undb.semear.repository.matricula.PreMatriculaRepository;
import br.undb.semear.repository.pessoa.ResponsavelRepository;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.service.academico.AnoLetivoService;

/** RF01.1 e RF01.2: pedido do responsável e análise da secretaria. */
@Service
public class PreMatriculaService {

    private final PreMatriculaRepository preMatriculas;
    private final TurmaRepository turmas;
    private final SerieRepository series;
    private final AnoLetivoRepository anos;
    private final ResponsavelRepository responsaveis;
    private final FluxoMatriculaRepository fluxo;
    private final AnoLetivoService anosLetivos;
    private final EscopoAcesso escopo;

    public PreMatriculaService(PreMatriculaRepository preMatriculas, TurmaRepository turmas, SerieRepository series,
                               AnoLetivoRepository anos, ResponsavelRepository responsaveis,
                               FluxoMatriculaRepository fluxo, AnoLetivoService anosLetivos, EscopoAcesso escopo) {
        this.preMatriculas = preMatriculas;
        this.turmas = turmas;
        this.series = series;
        this.anos = anos;
        this.responsaveis = responsaveis;
        this.fluxo = fluxo;
        this.anosLetivos = anosLetivos;
        this.escopo = escopo;
    }

    /** Anos ainda não encerrados aceitam pedidos (inclusive o atual, para quem chega no meio do ano). */
    public List<Integer> anosAbertos() {
        return anos.listar().stream()
                .filter(a -> !a.getDataFim().isBefore(anosLetivos.hoje()))
                .map(AnoLetivo::getAno)
                .toList();
    }

    @Transactional
    public PreMatriculaResumoResponse criar(UUID responsavelId, NovaPreMatriculaRequest pedido) {
        AnoLetivo ano = anos.buscarPorAno(pedido.ano())
                .filter(a -> anosAbertos().contains(a.getAno()))
                .orElseThrow(() -> new DadoInvalidoException("ano", "Este ano letivo não está aberto para matrículas"));
        Serie serie = series.buscarPorId(pedido.serieId())
                .orElseThrow(() -> new DadoInvalidoException("serieId", "Série inválida"));

        Cpf cpf = pedido.alunoCpf() == null || pedido.alunoCpf().isBlank() ? null : new Cpf(pedido.alunoCpf());
        DadosCrianca crianca = new DadosCrianca(pedido.alunoNome(), pedido.alunoNascimento(), cpf);
        crianca.validarIdadePara(ano.getDataInicio());

        boolean duplicada = preMatriculas.listarDoResponsavel(responsavelId).stream()
                .anyMatch(p -> p.status() == StatusPreMatricula.PENDENTE && p.ano() == ano.getAno()
                        && p.alunoNome().equalsIgnoreCase(crianca.nome())
                        && p.alunoNascimento().equals(crianca.dataNascimento()));
        if (duplicada) {
            throw new ConflitoException("PRE_MATRICULA_DUPLICADA", "Já existe uma pré-matrícula em análise para esta criança");
        }

        Map<TipoFarda, String> fardas = new EnumMap<>(TipoFarda.class);
        fardas.put(TipoFarda.UNIFORME, pedido.fardaUniforme());
        fardas.put(TipoFarda.EDUCACAO_FISICA, pedido.fardaEdFisica());

        PreMatricula nova = preMatriculas.salvar(
                PreMatricula.nova(ano.getAno(), serie, responsavelId, pedido.parentesco().trim(), crianca, fardas));
        return preMatriculas.buscarResumo(nova.getId()).orElseThrow();
    }

    public List<PreMatriculaResumoResponse> listar(StatusPreMatricula status, Integer ano) {
        return preMatriculas.listar(status, ano);
    }

    public List<PreMatriculaResumoResponse> listarDoResponsavel(UUID responsavelId) {
        return preMatriculas.listarDoResponsavel(responsavelId);
    }

    public PreMatriculaDetalheResponse detalhar(UUID id) {
        PreMatricula pre = buscar(id);
        PreMatriculaResumoResponse resumo = preMatriculas.buscarResumo(id).orElseThrow();
        Responsavel responsavel = responsaveis.buscarPorId(pre.getResponsavelId()).orElseThrow();
        List<TurmaResponse> opcoes = pre.getStatus() == StatusPreMatricula.PENDENTE
                ? turmasDaSerie(pre).stream().map(TurmaResponse::de).toList()
                : List.of();
        return new PreMatriculaDetalheResponse(resumo, pre.getSerie().getId(), pre.getParentesco(),
                pre.getCrianca().cpf() == null ? null : pre.getCrianca().cpf().formatado(),
                pre.getFardas().get(TipoFarda.UNIFORME), pre.getFardas().get(TipoFarda.EDUCACAO_FISICA),
                responsavel.getId(), responsavel.getEmail().map(e -> e.getEndereco()).orElse(null), opcoes);
    }

    /** RF01.2 + RN04: o Model valida; a função do banco cria aluno, vínculo, matrícula, contrato e cobrança. */
    @Transactional
    public UUID aprovar(UUID id, UUID turmaId) {
        PreMatricula pre = buscar(id);
        Turma turma = turmas.buscarPorId(turmaId).orElseThrow(() -> new RecursoNaoEncontradoException("Turma"));
        pre.validarAprovacao(turma);
        return fluxo.aprovarPreMatricula(id, escopo.funcionarioLogado(), turmaId);
    }

    @Transactional
    public void rejeitar(UUID id, String motivo) {
        PreMatricula pre = buscar(id);
        String motivoValidado = pre.validarRejeicao(motivo);
        fluxo.rejeitarPreMatricula(id, escopo.funcionarioLogado(), motivoValidado);
    }

    private PreMatricula buscar(UUID id) {
        return preMatriculas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pré-matrícula"));
    }

    private List<Turma> turmasDaSerie(PreMatricula pre) {
        return turmas.listarPorAno(pre.getAnoLetivo(), null).stream()
                .filter(t -> t.getSerie().getId().equals(pre.getSerie().getId()))
                .toList();
    }
}
