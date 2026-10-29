package br.undb.semear.controller.portal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.undb.semear.dto.academico.MaterialDtos.ListaMaterialResponse;
import br.undb.semear.dto.matricula.ContratoDtos.AceiteRequest;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoResumoResponse;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoTermoResponse;
import br.undb.semear.dto.pedagogico.BoletimDtos.BoletimResponse;
import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaDetalheResponse;
import br.undb.semear.dto.portal.PortalDtos.PortalCobrancasResponse;
import br.undb.semear.controller.matricula.DocumentoController;
import br.undb.semear.dto.matricula.DocumentoDtos.ChecklistResponse;
import br.undb.semear.dto.matricula.DocumentoDtos.DocumentoResponse;
import br.undb.semear.dto.matricula.DocumentoDtos.LinkArquivoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.NovaPreMatriculaRequest;
import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RematriculaOpcaoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RematricularRequest;
import br.undb.semear.dto.portal.PortalDtos.CompletarCadastroRequest;
import br.undb.semear.dto.portal.PortalDtos.OpcoesMatriculaResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.AlunoDoResponsavelResponse;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.service.portal.PortalService;
import jakarta.validation.Valid;

/** Portal do responsável (RF10). Os dados são sempre filtrados pelo login, no service. */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalService portal;

    public PortalController(PortalService portal) {
        this.portal = portal;
    }

    /** Primeiro acesso: qualquer login válido pode completar o cadastro (ainda não tem perfil). */
    @PostMapping("/cadastro")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> completarCadastro(@Valid @RequestBody CompletarCadastroRequest pedido) {
        portal.completarCadastro(pedido.nome(), pedido.cpf(), pedido.telefone());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alunos")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public List<AlunoDoResponsavelResponse> meusAlunos() {
        return portal.meusAlunos();
    }

    @GetMapping("/opcoes-matricula")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public OpcoesMatriculaResponse opcoesMatricula() {
        return portal.opcoesMatricula();
    }

    @GetMapping("/pre-matriculas")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public List<PreMatriculaResumoResponse> minhasPreMatriculas() {
        return portal.minhasPreMatriculas();
    }

    @PostMapping("/pre-matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('PORTAL', 'GRAVAR')")
    public PreMatriculaResumoResponse novaPreMatricula(@Valid @RequestBody NovaPreMatriculaRequest pedido) {
        return portal.novaPreMatricula(pedido);
    }

    @GetMapping("/rematricula")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public List<RematriculaOpcaoResponse> opcoesRematricula() {
        return portal.opcoesRematricula();
    }

    @GetMapping("/alunos/{alunoId}/documentos")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public ChecklistResponse documentos(@PathVariable UUID alunoId) {
        return ChecklistResponse.de(portal.documentosDoFilho(alunoId));
    }

    @PostMapping(path = "/alunos/{alunoId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('PORTAL', 'GRAVAR')")
    public DocumentoResponse enviarDocumento(@PathVariable UUID alunoId, @RequestParam TipoDocumentoAluno tipo,
                                             @RequestParam MultipartFile arquivo) {
        return DocumentoResponse.de(portal.enviarDocumentoDoFilho(alunoId, tipo, DocumentoController.lerBytes(arquivo)));
    }

    @GetMapping("/documentos/{id}/link")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public LinkArquivoResponse linkDocumento(@PathVariable UUID id) {
        return portal.linkDocumento(id);
    }

    @PostMapping("/rematricula")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('PORTAL', 'GRAVAR')")
    public Map<String, UUID> rematricular(@Valid @RequestBody RematricularRequest pedido) {
        return Map.of("matriculaId", portal.rematricular(pedido.alunoId(), pedido.fardaUniforme(), pedido.fardaEdFisica()));
    }

    /* ---- Financeiro ---- */

    @GetMapping("/cobrancas")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public PortalCobrancasResponse cobrancas() {
        return portal.minhasCobrancas();
    }

    @GetMapping("/cobrancas/{id}")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public CobrancaDetalheResponse cobranca(@PathVariable UUID id) {
        return portal.cobranca(id);
    }

    /** Só com o Cora simulado: confirma o pagamento como se o banco tivesse avisado. */
    @PostMapping("/cobrancas/{id}/pagamento-simulado")
    @PreAuthorize("@permissoes.pode('PORTAL', 'GRAVAR')")
    public CobrancaDetalheResponse simularPagamento(@PathVariable UUID id) {
        return portal.simularPagamento(id);
    }

    /* ---- Contratos, materiais e boletim ---- */

    @GetMapping("/alunos/{alunoId}/contratos")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public List<ContratoResumoResponse> contratos(@PathVariable UUID alunoId) {
        return portal.contratosDoFilho(alunoId);
    }

    @GetMapping("/contratos/{id}")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public ContratoTermoResponse contrato(@PathVariable UUID id) {
        return portal.contrato(id);
    }

    @PostMapping("/contratos/{id}/assinar")
    @PreAuthorize("@permissoes.pode('PORTAL', 'GRAVAR')")
    public ContratoTermoResponse assinar(@PathVariable UUID id, @Valid @RequestBody AceiteRequest pedido,
                                         jakarta.servlet.http.HttpServletRequest requisicao) {
        // Evidência do aceite eletrônico: endereço de rede e navegador de quem assinou.
        return portal.assinarContrato(id, requisicao.getRemoteAddr(), requisicao.getHeader("User-Agent"));
    }

    @GetMapping("/alunos/{alunoId}/materiais")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public ListaMaterialResponse materiais(@PathVariable UUID alunoId) {
        return portal.materiaisDoFilho(alunoId);
    }

    @GetMapping("/alunos/{alunoId}/boletim")
    @PreAuthorize("@permissoes.pode('PORTAL', 'LER')")
    public BoletimResponse boletim(@PathVariable UUID alunoId, @RequestParam(required = false) Integer ano) {
        return portal.boletimDoFilho(alunoId, ano);
    }
}
