package br.undb.semear.service.financeiro;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.financeiro.FinanceiroDtos.BolsaRequest;
import br.undb.semear.dto.financeiro.FinanceiroDtos.BolsaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ConcessaoResponse;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.entity.financeiro.ConcessaoBolsa;
import br.undb.semear.model.entity.matricula.Matricula;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.financeiro.BolsaRepository;
import br.undb.semear.repository.financeiro.BolsaRepository.ConcessaoComContexto;
import br.undb.semear.repository.matricula.MatriculaRepository;
import br.undb.semear.service.academico.AnoLetivoService;

/**
 * Bolsas (RF02/RN11). Conceder ou revogar altera bolsa_matricula; o banco recalcula na hora
 * as cobranças mensais em aberto daquela matrícula (trigger), nunca as pagas (RD05).
 */
@Service
public class BolsaService {

    private final BolsaRepository bolsas;
    private final MatriculaRepository matriculas;
    private final AnoLetivoService anosLetivos;

    public BolsaService(BolsaRepository bolsas, MatriculaRepository matriculas, AnoLetivoService anosLetivos) {
        this.bolsas = bolsas;
        this.matriculas = matriculas;
        this.anosLetivos = anosLetivos;
    }

    public List<BolsaResponse> listar() {
        LocalDate hoje = anosLetivos.hoje();
        return bolsas.listar().stream().map(b -> BolsaResponse.de(b, bolsas.bolsistasVigentes(b.getId(), hoje))).toList();
    }

    @Transactional
    public BolsaResponse criar(BolsaRequest r) {
        Bolsa nova = bolsas.salvar(new Bolsa(null, r.nome(), r.descricao(), r.tipoDesconto(), r.valor(), r.ativa()));
        return BolsaResponse.de(nova, 0);
    }

    /** Mudar valor ou desativar recalcula as cobranças em aberto de todos os bolsistas (trigger). */
    @Transactional
    public BolsaResponse alterar(UUID id, BolsaRequest r) {
        Bolsa bolsa = buscarBolsa(id);
        bolsa.alterar(r.nome(), r.descricao(), r.tipoDesconto(), r.valor(), r.ativa());
        Bolsa salva = bolsas.salvar(bolsa);
        return BolsaResponse.de(salva, bolsas.bolsistasVigentes(id, anosLetivos.hoje()));
    }

    public List<ConcessaoResponse> bolsistas(Integer ano) {
        LocalDate hoje = anosLetivos.hoje();
        return bolsas.bolsistas(anosLetivos.anoOuCorrente(ano)).stream().map(c -> resposta(c, hoje)).toList();
    }

    public List<ConcessaoResponse> daMatricula(UUID matriculaId) {
        LocalDate hoje = anosLetivos.hoje();
        return bolsas.concessoesDaMatricula(matriculaId).stream().map(c -> resposta(c, hoje)).toList();
    }

    /**
     * Concede a partir da data informada (padrão: hoje, ou o início do ano letivo se a matrícula for do ano seguinte).
     * Só para matrícula vigente, bolsa ativa e sem a mesma bolsa já valendo.
     */
    @Transactional
    public List<ConcessaoResponse> conceder(UUID matriculaId, UUID bolsaId, LocalDate inicioInformado, UUID funcionarioId) {
        Matricula matricula = matriculas.buscarPorId(matriculaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula"));
        if (matricula.getStatus() != StatusMatricula.ATIVA && matricula.getStatus() != StatusMatricula.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("MATRICULA_NAO_VIGENTE", "Só é possível conceder bolsa a matrícula vigente");
        }
        Bolsa bolsa = buscarBolsa(bolsaId);
        if (!bolsa.isAtiva()) {
            throw new RegraNegocioException("BOLSA_INATIVA", "Esta bolsa está desativada");
        }
        LocalDate hoje = anosLetivos.hoje();
        AnoLetivo ano = anosLetivos.listar().stream().filter(a -> a.getAno() == matricula.getAnoLetivo()).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ano letivo"));
        LocalDate inicio = inicioInformado != null ? inicioInformado
                : (ano.getDataInicio().isAfter(hoje) ? ano.getDataInicio() : hoje);
        if (inicio.isAfter(ano.getDataFim())) {
            throw new RegraNegocioException("FORA_DO_ANO_LETIVO", "A bolsa precisa começar dentro do ano letivo " + ano.getAno());
        }
        boolean jaTem = bolsas.concessoesDaMatricula(matriculaId).stream()
                .anyMatch(c -> c.bolsa().getId().equals(bolsaId) && c.concessao().ativaOuFutura(hoje));
        if (jaTem) {
            throw new ConflitoException("BOLSA_JA_CONCEDIDA", "Esta matrícula já tem essa bolsa");
        }
        // Vigência até o fim do ano letivo: RN11, a bolsa é revista a cada ano.
        bolsas.conceder(new ConcessaoBolsa(null, bolsaId, matriculaId, inicio, ano.getDataFim(), funcionarioId));
        return daMatricula(matriculaId);
    }

    @Transactional
    public List<ConcessaoResponse> revogar(UUID matriculaId, UUID concessaoId) {
        ConcessaoBolsa concessao = bolsas.buscarConcessao(concessaoId)
                .filter(c -> c.getMatriculaId().equals(matriculaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bolsa da matrícula"));
        LocalDate hoje = anosLetivos.hoje();
        if (concessao.revogar(hoje)) {
            bolsas.apagarConcessao(concessaoId);
        } else {
            bolsas.encerrarConcessao(concessaoId, concessao.getVigenciaFim());
        }
        return daMatricula(matriculaId);
    }

    private Bolsa buscarBolsa(UUID id) {
        return bolsas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Bolsa"));
    }

    private static ConcessaoResponse resposta(ConcessaoComContexto c, LocalDate hoje) {
        ConcessaoBolsa k = c.concessao();
        Bolsa b = c.bolsa();
        return new ConcessaoResponse(k.getId(), k.getMatriculaId(), b.getId(), b.getNome(), b.getTipoDesconto(),
                b.getValor(), b.isAtiva(), c.alunoId(), c.aluno(), c.ano(), c.turma(), k.getVigenciaInicio(),
                k.getVigenciaFim(), b.isAtiva() && k.vigenteEm(hoje));
    }
}
