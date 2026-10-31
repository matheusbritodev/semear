package br.undb.semear.service.financeiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.AlteracaoBolsaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.BolsaRequest;
import br.undb.semear.dto.financeiro.FinanceiroDtos.BolsaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.ConcessaoResponse;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.controle.SolicitacaoAlteracaoBolsa;
import br.undb.semear.model.entity.controle.SolicitacaoConcessaoBolsa;
import br.undb.semear.model.entity.financeiro.Bolsa;
import br.undb.semear.model.entity.financeiro.ConcessaoBolsa;
import br.undb.semear.model.entity.matricula.Matricula;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.financeiro.BolsaRepository;
import br.undb.semear.repository.financeiro.BolsaRepository.ConcessaoComContexto;
import br.undb.semear.repository.matricula.MatriculaRepository;
import br.undb.semear.repository.pessoa.FuncionarioRepository;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.controle.PedidosAprovacao;

/**
 * Bolsas (RF02/RN11). Conceder ou revogar altera bolsa_matricula; o banco recalcula na hora
 * as cobranças mensais em aberto daquela matrícula (trigger), nunca as pagas (RD05).
 * <p>
 * SoD: conceder bolsa e mudar o desconto de um tipo de bolsa viram pedidos que outra pessoa aprova
 * (AprovacaoService chama os métodos "efetivar..." depois da aprovação). Revogar é imediato: só reduz desconto.
 */
@Service
public class BolsaService {

    private final BolsaRepository bolsas;
    private final MatriculaRepository matriculas;
    private final AnoLetivoService anosLetivos;
    private final PedidosAprovacao pedidos;
    private final FuncionarioRepository funcionarios;

    public BolsaService(BolsaRepository bolsas, MatriculaRepository matriculas, AnoLetivoService anosLetivos,
                        PedidosAprovacao pedidos, FuncionarioRepository funcionarios) {
        this.bolsas = bolsas;
        this.matriculas = matriculas;
        this.anosLetivos = anosLetivos;
        this.pedidos = pedidos;
        this.funcionarios = funcionarios;
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

    /**
     * Nome, descrição e desativar valem na hora. Mudar o tipo ou o valor do desconto, ou reativar,
     * afeta todos os bolsistas: vira pedido de aprovação.
     */
    @Transactional
    public AlteracaoBolsaResponse alterar(UUID id, BolsaRequest r) {
        Bolsa bolsa = buscarBolsa(id);
        if (bolsa.alteracaoExigeAprovacao(r.tipoDesconto(), r.valor(), r.ativa())) {
            String resumo = "Bolsa \"" + bolsa.getNome() + "\": " + descreverDesconto(bolsa.getTipoDesconto(), bolsa.getValor())
                    + " → " + descreverDesconto(r.tipoDesconto(), r.valor())
                    + (r.ativa() && !bolsa.isAtiva() ? " (reativar)" : "");
            SolicitacaoResponse pedido = pedidos.registrar(new SolicitacaoAlteracaoBolsa(null, resumo,
                    pedidos.solicitante(), null, null, id, r.nome(), r.descricao(), r.tipoDesconto(), r.valor(), r.ativa()));
            return new AlteracaoBolsaResponse(BolsaResponse.de(bolsa, bolsas.bolsistasVigentes(id, anosLetivos.hoje())), pedido);
        }
        return new AlteracaoBolsaResponse(efetivarAlteracao(id, r.nome(), r.descricao(), r.tipoDesconto(), r.valor(),
                r.ativa()), null);
    }

    /** Grava a alteração (direta ou já aprovada). Mudar valor ou desativar recalcula as cobranças em aberto (trigger). */
    @Transactional
    public BolsaResponse efetivarAlteracao(UUID id, String nome, String descricao, TipoDesconto tipo, BigDecimal valor,
                                           boolean ativa) {
        Bolsa bolsa = buscarBolsa(id);
        bolsa.alterar(nome, descricao, tipo, valor, ativa);
        Bolsa salva = bolsas.salvar(bolsa);
        return BolsaResponse.de(salva, bolsas.bolsistasVigentes(id, anosLetivos.hoje()));
    }

    private static String descreverDesconto(TipoDesconto tipo, BigDecimal valor) {
        return tipo == TipoDesconto.PERCENTUAL
                ? valor.stripTrailingZeros().toPlainString() + "%"
                : "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
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
     * Pede a concessão (SoD: outra pessoa aprova). As regras são conferidas já no pedido, para não
     * acumular pedidos impossíveis, e de novo na aprovação, porque a situação pode ter mudado.
     * Quem é responsável pelo aluno não pode pedir bolsa para ele (conflito de interesse).
     */
    @Transactional
    public SolicitacaoResponse solicitarConcessao(UUID matriculaId, UUID bolsaId, LocalDate inicioInformado) {
        pedidos.exigirSemConflitoNaMatricula(matriculaId, "a solicitação de bolsa");
        Bolsa bolsa = buscarBolsa(bolsaId);
        LocalDate inicio = validarConcessao(matriculaId, bolsa, inicioInformado);
        String resumo = bolsa.getNome() + " (" + descreverDesconto(bolsa.getTipoDesconto(), bolsa.getValor()) + ") para "
                + pedidos.descricaoMatricula(matriculaId) + ", a partir de "
                + inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return pedidos.registrar(new SolicitacaoConcessaoBolsa(null, resumo, pedidos.solicitante(), null, null,
                matriculaId, bolsaId, inicioInformado));
    }

    /**
     * Concede de fato (pedido aprovado), a partir da data informada (padrão: hoje, ou o início do ano letivo
     * se a matrícula for do ano seguinte). Só para matrícula vigente, bolsa ativa e sem a mesma bolsa já valendo.
     *
     * @param solicitante login de quem pediu: o funcionário dele fica registrado como quem concedeu
     */
    @Transactional
    public List<ConcessaoResponse> efetivarConcessao(UUID matriculaId, UUID bolsaId, LocalDate inicioInformado,
                                                     UUID solicitante) {
        Bolsa bolsa = buscarBolsa(bolsaId);
        LocalDate inicio = validarConcessao(matriculaId, bolsa, inicioInformado);
        AnoLetivo ano = anoDa(matriculas.buscarPorId(matriculaId).orElseThrow());
        UUID funcionarioId = funcionarios.buscarIdPorUsuario(solicitante).orElse(null);
        // Vigência até o fim do ano letivo: RN11, a bolsa é revista a cada ano.
        bolsas.conceder(new ConcessaoBolsa(null, bolsaId, matriculaId, inicio, ano.getDataFim(), funcionarioId));
        return daMatricula(matriculaId);
    }

    /** Confere matrícula, bolsa e data; devolve a data de início que vai valer. */
    private LocalDate validarConcessao(UUID matriculaId, Bolsa bolsa, LocalDate inicioInformado) {
        Matricula matricula = matriculas.buscarPorId(matriculaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula"));
        if (matricula.getStatus() != StatusMatricula.ATIVA && matricula.getStatus() != StatusMatricula.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("MATRICULA_NAO_VIGENTE", "Só é possível conceder bolsa a matrícula vigente");
        }
        if (!bolsa.isAtiva()) {
            throw new RegraNegocioException("BOLSA_INATIVA", "Esta bolsa está desativada");
        }
        LocalDate hoje = anosLetivos.hoje();
        AnoLetivo ano = anoDa(matricula);
        LocalDate inicio = inicioInformado != null ? inicioInformado
                : (ano.getDataInicio().isAfter(hoje) ? ano.getDataInicio() : hoje);
        if (inicio.isAfter(ano.getDataFim())) {
            throw new RegraNegocioException("FORA_DO_ANO_LETIVO", "A bolsa precisa começar dentro do ano letivo " + ano.getAno());
        }
        boolean jaTem = bolsas.concessoesDaMatricula(matriculaId).stream()
                .anyMatch(c -> c.bolsa().getId().equals(bolsa.getId()) && c.concessao().ativaOuFutura(hoje));
        if (jaTem) {
            throw new ConflitoException("BOLSA_JA_CONCEDIDA", "Esta matrícula já tem essa bolsa");
        }
        return inicio;
    }

    private AnoLetivo anoDa(Matricula matricula) {
        return anosLetivos.listar().stream().filter(a -> a.getAno() == matricula.getAnoLetivo()).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ano letivo"));
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
