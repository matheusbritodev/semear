package br.undb.semear.service.portal;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.academico.MaterialDtos.ListaMaterialResponse;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoResumoResponse;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoTermoResponse;
import br.undb.semear.dto.pedagogico.BoletimDtos.BoletimResponse;
import br.undb.semear.service.academico.MaterialService;
import br.undb.semear.service.matricula.ContratoService;
import br.undb.semear.service.pedagogico.BoletimService;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaDetalheResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaResponse;
import br.undb.semear.dto.portal.PortalDtos.PortalCobrancasResponse;
import br.undb.semear.repository.financeiro.CobrancaRepository.Filtro;
import br.undb.semear.service.financeiro.CobrancaService;
import br.undb.semear.dto.matricula.DocumentoDtos.LinkArquivoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.NovaPreMatriculaRequest;
import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RematriculaOpcaoResponse;
import br.undb.semear.dto.portal.PortalDtos.OpcoesMatriculaResponse;
import br.undb.semear.dto.portal.PortalDtos.SerieResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.AlunoDoResponsavelResponse;
import br.undb.semear.model.entity.matricula.ChecklistDocumentos;
import br.undb.semear.model.entity.matricula.DocumentoAluno;
import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.academico.SerieRepository;
import br.undb.semear.repository.pessoa.ResponsavelRepository;
import br.undb.semear.repository.pessoa.UsuarioRepository;
import br.undb.semear.security.UsuarioLogado;
import br.undb.semear.service.matricula.DocumentoService;
import br.undb.semear.service.matricula.PreMatriculaService;
import br.undb.semear.service.matricula.RematriculaService;

/**
 * Portal do responsável (RF10). Tudo é filtrado pelo responsável do login (docs/04 §8.4):
 * nunca se confia em id de responsável vindo da View.
 */
@Service
public class PortalService {

    private final UsuarioLogado usuarioLogado;
    private final UsuarioRepository usuarios;
    private final ResponsavelRepository responsaveis;
    private final SerieRepository series;
    private final PreMatriculaService preMatriculas;
    private final RematriculaService rematriculas;
    private final DocumentoService documentos;
    private final CobrancaService cobrancas;
    private final ContratoService contratos;
    private final MaterialService materiais;
    private final BoletimService boletins;

    public PortalService(UsuarioLogado usuarioLogado, UsuarioRepository usuarios, ResponsavelRepository responsaveis,
                         SerieRepository series, PreMatriculaService preMatriculas, RematriculaService rematriculas,
                         DocumentoService documentos, CobrancaService cobrancas, ContratoService contratos,
                         MaterialService materiais, BoletimService boletins) {
        this.usuarioLogado = usuarioLogado;
        this.usuarios = usuarios;
        this.responsaveis = responsaveis;
        this.series = series;
        this.preMatriculas = preMatriculas;
        this.rematriculas = rematriculas;
        this.documentos = documentos;
        this.cobrancas = cobrancas;
        this.contratos = contratos;
        this.materiais = materiais;
        this.boletins = boletins;
    }

    /**
     * Primeiro acesso: cria o usuário RESPONSAVEL (id = login do Supabase) e liga a um cadastro.
     * Se a escola já tem o responsável pelo CPF (ex.: pai de aluno antigo), reaproveita; senão, cria.
     */
    @Transactional
    public void completarCadastro(String nome, String cpfInformado, String telefoneInformado) {
        UUID id = usuarioLogado.id();
        if (usuarios.buscarPorId(id).isPresent()) {
            throw new ConflitoException("CADASTRO_JA_COMPLETO", "Seu cadastro já está completo");
        }
        Email email = usuarioLogado.email().map(Email::new)
                .orElseThrow(() -> new DadoInvalidoException("email", "Seu login não tem e-mail"));
        Cpf cpf = new Cpf(cpfInformado);
        Telefone telefone = new Telefone(telefoneInformado);

        Responsavel responsavel = responsaveis.buscarPorCpf(cpf.getDigitos()).orElse(null);
        if (responsavel != null && responsavel.possuiLogin()) {
            throw new ConflitoException("CPF_JA_VINCULADO",
                    "Este CPF já está ligado a outro acesso. Procure a secretaria da escola.");
        }

        usuarios.salvar(new Usuario(id, email, PerfilUsuario.RESPONSAVEL, true, null, List.of()));
        if (responsavel == null) {
            responsavel = responsaveis.salvar(new Responsavel(null, nome, cpf, telefone, email, null));
        }
        responsaveis.vincularUsuario(responsavel.getId(), id);
    }

    public List<AlunoDoResponsavelResponse> meusAlunos() {
        return responsaveis.listarAlunos(responsavelLogado().getId());
    }

    public List<PreMatriculaResumoResponse> minhasPreMatriculas() {
        return preMatriculas.listarDoResponsavel(responsavelLogado().getId());
    }

    public OpcoesMatriculaResponse opcoesMatricula() {
        List<SerieResponse> lista = series.listar().stream()
                .map(s -> new SerieResponse(s.getId(), s.getNome(), s.getNivel(), s.getNivel().getRotulo(), s.getOrdem()))
                .toList();
        return new OpcoesMatriculaResponse(preMatriculas.anosAbertos(), lista);
    }

    public PreMatriculaResumoResponse novaPreMatricula(NovaPreMatriculaRequest pedido) {
        return preMatriculas.criar(responsavelLogado().getId(), pedido);
    }

    public List<RematriculaOpcaoResponse> opcoesRematricula() {
        return meusAlunos().stream().map(a -> rematriculas.opcao(a.alunoId())).toList();
    }

    public UUID rematricular(UUID alunoId, String fardaUniforme, String fardaEdFisica) {
        exigirFilho(alunoId);
        return rematriculas.rematricular(alunoId, fardaUniforme, fardaEdFisica);
    }

    /* ---- Documentos dos filhos (RN12, RF10) ---- */

    public ChecklistDocumentos documentosDoFilho(UUID alunoId) {
        exigirFilho(alunoId);
        return documentos.checklist(alunoId);
    }

    public DocumentoAluno enviarDocumentoDoFilho(UUID alunoId, TipoDocumentoAluno tipo, byte[] conteudo) {
        exigirFilho(alunoId);
        return documentos.enviar(alunoId, tipo, conteudo);
    }

    public LinkArquivoResponse linkDocumento(UUID documentoId) {
        DocumentoAluno documento = documentos.buscar(documentoId);
        exigirFilho(documento.getAlunoId());
        return documentos.link(documento);
    }

    /* ---- Contratos, materiais e boletim dos filhos (RF10) ---- */

    public List<ContratoResumoResponse> contratosDoFilho(UUID alunoId) {
        exigirFilho(alunoId);
        return contratos.doAluno(alunoId);
    }

    public ContratoTermoResponse contrato(UUID contratoId) {
        exigirFilho(contratos.alunoDoContrato(contratoId));
        return contratos.termo(contratoId);
    }

    /** Aceite eletrônico: quem assina é sempre o responsável do login (RD03). */
    public ContratoTermoResponse assinarContrato(UUID contratoId) {
        exigirFilho(contratos.alunoDoContrato(contratoId));
        return contratos.assinar(contratoId, responsavelLogado().getId(), "portal");
    }

    public ListaMaterialResponse materiaisDoFilho(UUID alunoId) {
        exigirFilho(alunoId);
        return materiais.doAluno(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula vigente"));
    }

    public BoletimResponse boletimDoFilho(UUID alunoId, Integer ano) {
        exigirFilho(alunoId);
        return boletins.doAluno(alunoId, ano);
    }

    /* ---- Financeiro: só as cobranças em que o logado é o pagador ---- */

    public PortalCobrancasResponse minhasCobrancas() {
        UUID responsavel = responsavelLogado().getId();
        PaginaResponse<CobrancaResponse> pagina = cobrancas.pesquisar(
                new Filtro(null, null, null, null, null, null, responsavel, 0, 100));
        return new PortalCobrancasResponse(cobrancas.pagamentoSimulado(), pagina.itens());
    }

    public CobrancaDetalheResponse cobranca(UUID cobrancaId) {
        exigirPagador(cobrancaId);
        return cobrancas.detalhar(cobrancaId, null);
    }

    /** Só no Cora simulado: faz o papel do banco confirmando o pagamento do boleto. */
    public CobrancaDetalheResponse simularPagamento(UUID cobrancaId) {
        exigirPagador(cobrancaId);
        if (!cobrancas.pagamentoSimulado()) {
            throw new AcessoNegadoException("PAGAMENTO_REAL", "Pague pelo link do boleto");
        }
        cobrancas.confirmarPagamentoSite(cobrancaId);
        return cobrancas.detalhar(cobrancaId, null);
    }

    private void exigirPagador(UUID cobrancaId) {
        if (!cobrancas.pertenceAoResponsavel(cobrancaId, responsavelLogado().getId())) {
            throw new AcessoNegadoException("COBRANCA_DE_OUTRO_RESPONSAVEL", "Esta cobrança não está no seu nome");
        }
    }

    private void exigirFilho(UUID alunoId) {
        if (!responsaveis.estaVinculadoAoAluno(responsavelLogado().getId(), alunoId)) {
            throw new AcessoNegadoException("ALUNO_DE_OUTRA_FAMILIA", "Você só pode acessar os dados dos seus filhos");
        }
    }

    private Responsavel responsavelLogado() {
        return responsaveis.buscarPorUsuario(usuarioLogado.exigir().getId())
                .orElseThrow(() -> new AcessoNegadoException("CADASTRO_INCOMPLETO",
                        "Complete seu cadastro de responsável para usar o portal"));
    }
}
