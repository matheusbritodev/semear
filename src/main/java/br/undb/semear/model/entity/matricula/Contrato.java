package br.undb.semear.model.entity.matricula;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoContrato;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Documento de formalização da matrícula (RF01.4, RN07): Contrato de Matrícula no Fundamental
 * e Ficha de Sócio na Educação Infantil. O banco cria um por matrícula; aqui ficam o texto e a assinatura.
 *
 * Template Method: {@link #clausulas} monta sempre na mesma ordem — objeto, valores (da subclasse),
 * cláusulas comuns do contrato e responsável financeiro.
 */
public abstract class Contrato extends Entidade {

    protected static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UUID matriculaId;
    private StatusContrato status;
    private UUID assinadoPor;
    private OffsetDateTime assinadoEm;

    protected Contrato(UUID id, UUID matriculaId, StatusContrato status, UUID assinadoPor, OffsetDateTime assinadoEm) {
        super(id);
        this.matriculaId = Objects.requireNonNull(matriculaId, "matriculaId");
        this.status = Objects.requireNonNull(status, "status");
        this.assinadoPor = assinadoPor;
        this.assinadoEm = assinadoEm;
    }

    public abstract TipoContrato getTipo();

    public abstract String getTitulo();

    /** O que o documento formaliza (primeira cláusula). */
    protected abstract Clausula objeto(CondicoesContrato c);

    /** Valores e forma de pagamento: é o que muda entre Taxa de Associado e Mensalidade (RN01). */
    protected abstract List<Clausula> valores(CondicoesContrato c);

    public final List<Clausula> clausulas(CondicoesContrato c) {
        List<Clausula> lista = new ArrayList<>();
        lista.add(objeto(c));
        lista.addAll(valores(c));
        lista.add(new Clausula("Efetivação",
                "A matrícula é efetivada com o pagamento do valor de matrícula (arras). Os documentos listados pela escola "
                        + "devem ser entregues; enquanto faltarem, a matrícula vale, mas fica com pendência registrada (cl. 5)."));
        lista.add(new Clausula("Atraso",
                "Pago depois do vencimento, o valor tem multa de 2% e juros de 0,033% ao dia, contados do vencimento. "
                        + "Não há acréscimo se o pagamento ocorrer até o 5º dia útil do mês do vencimento (cl. 6)."));
        lista.add(new Clausula("Bolsa ou desconto condicional",
                "Bolsas são concedidas a critério da escola e podem ser revistas a cada ano letivo. O desconto vale apenas "
                        + "para as cobranças mensais (cl. 4, §2)."));
        lista.add(new Clausula("Cancelamento e transferência",
                "O cancelamento ou a transferência exige a quitação dos débitos vencidos. A renovação para o ano seguinte "
                        + "só é aceita para aluno adimplente (cl. 9 e 11)."));
        lista.add(new Clausula("Responsável financeiro",
                c.responsavelFinanceiro() == null
                        ? "O responsável financeiro ainda não foi definido; a escola deve registrá-lo antes da assinatura."
                        : c.responsavelFinanceiro() + " responde pelos pagamentos deste " + getTitulo().toLowerCase() + "."));
        return List.copyOf(lista);
    }

    /**
     * Assinatura (RD03): só um responsável vinculado ao aluno, só de contrato pendente e de matrícula vigente.
     * O banco confere de novo (fn_valida_assinante).
     */
    public void assinar(UUID responsavelId, Collection<UUID> responsaveisDoAluno, StatusMatricula statusMatricula,
                        OffsetDateTime agora) {
        if (status == StatusContrato.ASSINADO) {
            throw new ConflitoException("CONTRATO_JA_ASSINADO", "Este documento já foi assinado");
        }
        if (status == StatusContrato.CANCELADO) {
            throw new RegraNegocioException("CONTRATO_CANCELADO", "Documento cancelado não pode ser assinado");
        }
        if (statusMatricula != StatusMatricula.ATIVA && statusMatricula != StatusMatricula.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("MATRICULA_NAO_VIGENTE", "A matrícula deste documento não está vigente");
        }
        if (responsavelId == null || !responsaveisDoAluno.contains(responsavelId)) {
            throw new AcessoNegadoException("ASSINANTE_INVALIDO", "Só um responsável vinculado ao aluno pode assinar");
        }
        this.status = StatusContrato.ASSINADO;
        this.assinadoPor = responsavelId;
        this.assinadoEm = Objects.requireNonNull(agora, "agora");
    }

    public UUID getMatriculaId() {
        return matriculaId;
    }

    public StatusContrato getStatus() {
        return status;
    }

    public UUID getAssinadoPor() {
        return assinadoPor;
    }

    public OffsetDateTime getAssinadoEm() {
        return assinadoEm;
    }

    protected static String periodo(CondicoesContrato c) {
        return c.inicioAno().format(DATA) + " a " + c.fimAno().format(DATA);
    }
}
