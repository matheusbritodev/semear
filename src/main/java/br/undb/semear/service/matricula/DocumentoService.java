package br.undb.semear.service.matricula;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.undb.semear.dto.matricula.DocumentoDtos.LinkArquivoResponse;
import br.undb.semear.integration.ArmazenamentoArquivos;
import br.undb.semear.model.entity.matricula.ChecklistDocumentos;
import br.undb.semear.model.entity.matricula.DocumentoAluno;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.valueobject.ArquivoEnviado;
import br.undb.semear.repository.matricula.DocumentoAlunoRepository;
import br.undb.semear.repository.pessoa.AlunoRepository;

/**
 * Documentos da matrícula (RN12): o arquivo vai para o Storage privado e o caminho para o banco.
 * Se a gravação no banco falhar, o arquivo enviado é removido (compensação).
 */
@Service
public class DocumentoService {

    /** Validade do link para abrir um documento: curta, por ser documento de criança (LGPD). */
    public static final Duration VALIDADE_LINK = Duration.ofMinutes(5);

    private final DocumentoAlunoRepository documentos;
    private final AlunoRepository alunos;
    private final ArmazenamentoArquivos armazenamento;
    private final Clock relogio;

    public DocumentoService(DocumentoAlunoRepository documentos, AlunoRepository alunos,
                            ArmazenamentoArquivos armazenamento, Clock relogio) {
        this.documentos = documentos;
        this.alunos = alunos;
        this.armazenamento = armazenamento;
        this.relogio = relogio;
    }

    public ChecklistDocumentos checklist(UUID alunoId) {
        exigirAluno(alunoId);
        return new ChecklistDocumentos(documentos.listarDoAluno(alunoId));
    }

    public DocumentoAluno enviar(UUID alunoId, TipoDocumentoAluno tipo, byte[] conteudo) {
        exigirAluno(alunoId);
        ArquivoEnviado arquivo = new ArquivoEnviado(conteudo);
        DocumentoAluno novo = DocumentoAluno.novo(alunoId, tipo, arquivo);
        armazenamento.enviar(DocumentoAluno.BUCKET, novo.getCaminho(), arquivo.getConteudo(),
                arquivo.getFormato().getContentType());
        try {
            return documentos.salvar(novo);
        } catch (RuntimeException e) {
            armazenamento.remover(DocumentoAluno.BUCKET, novo.getCaminho());
            throw e;
        }
    }

    public DocumentoAluno buscar(UUID documentoId) {
        return documentos.buscarPorId(documentoId).orElseThrow(() -> new RecursoNaoEncontradoException("Documento"));
    }

    public LinkArquivoResponse link(DocumentoAluno documento) {
        String url = armazenamento.urlTemporaria(DocumentoAluno.BUCKET, documento.getCaminho(), VALIDADE_LINK);
        return new LinkArquivoResponse(url, OffsetDateTime.now(relogio).plus(VALIDADE_LINK));
    }

    /** Remove primeiro do banco (fonte da verdade) e depois do Storage. */
    public void remover(UUID documentoId) {
        DocumentoAluno documento = buscar(documentoId);
        documentos.remover(documentoId);
        armazenamento.remover(DocumentoAluno.BUCKET, documento.getCaminho());
    }

    private void exigirAluno(UUID alunoId) {
        if (alunos.buscarPorId(alunoId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Aluno");
        }
    }
}
