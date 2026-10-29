package br.undb.semear.controller.matricula;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.undb.semear.dto.matricula.DocumentoDtos.ChecklistResponse;
import br.undb.semear.dto.matricula.DocumentoDtos.DocumentoResponse;
import br.undb.semear.dto.matricula.DocumentoDtos.LinkArquivoResponse;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.service.matricula.DocumentoService;

/** Documentos da matrícula (RN12) pela secretaria. */
@RestController
public class DocumentoController {

    private final DocumentoService documentos;

    public DocumentoController(DocumentoService documentos) {
        this.documentos = documentos;
    }

    @GetMapping("/api/alunos/{alunoId}/documentos")
    @PreAuthorize("@permissoes.pode('DOCUMENTOS', 'LER')")
    public ChecklistResponse checklist(@PathVariable UUID alunoId) {
        return ChecklistResponse.de(documentos.checklist(alunoId));
    }

    @PostMapping(path = "/api/alunos/{alunoId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('DOCUMENTOS', 'GRAVAR')")
    public DocumentoResponse enviar(@PathVariable UUID alunoId, @RequestParam TipoDocumentoAluno tipo,
                                    @RequestParam MultipartFile arquivo) {
        return DocumentoResponse.de(documentos.enviar(alunoId, tipo, lerBytes(arquivo)));
    }

    @GetMapping("/api/documentos/{id}/link")
    @PreAuthorize("@permissoes.pode('DOCUMENTOS', 'LER')")
    public LinkArquivoResponse link(@PathVariable UUID id) {
        return documentos.link(documentos.buscar(id));
    }

    @DeleteMapping("/api/documentos/{id}")
    @PreAuthorize("@permissoes.pode('DOCUMENTOS', 'GRAVAR')")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        documentos.remover(id);
        return ResponseEntity.noContent().build();
    }

    /** Leitura do arquivo enviado, compartilhada com o portal. */
    public static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new DadoInvalidoException("arquivo", "Não foi possível ler o arquivo enviado");
        }
    }
}
