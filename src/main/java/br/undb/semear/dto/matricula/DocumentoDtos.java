package br.undb.semear.dto.matricula;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.matricula.ChecklistDocumentos;
import br.undb.semear.model.entity.matricula.DocumentoAluno;
import br.undb.semear.model.enums.TipoDocumentoAluno;

/** Saída dos endpoints de documentos do aluno (RN12). */
public final class DocumentoDtos {

    private DocumentoDtos() {
    }

    public record DocumentoResponse(UUID id, TipoDocumentoAluno tipo, String extensao, OffsetDateTime enviadoEm) {

        public static DocumentoResponse de(DocumentoAluno d) {
            return new DocumentoResponse(d.getId(), d.getTipo(), d.getExtensao(), d.getEnviadoEm());
        }
    }

    public record ItemChecklistResponse(TipoDocumentoAluno tipo, String rotulo, boolean obrigatorio, boolean enviado,
                                        List<DocumentoResponse> documentos) {
    }

    public record ChecklistResponse(boolean completo, int pendentes, List<ItemChecklistResponse> itens) {

        public static ChecklistResponse de(ChecklistDocumentos checklist) {
            List<ItemChecklistResponse> itens = checklist.getItens().stream()
                    .map(i -> new ItemChecklistResponse(i.tipo(), i.tipo().getRotulo(), i.tipo().isObrigatorio(),
                            i.enviado(), i.documentos().stream().map(DocumentoResponse::de).toList()))
                    .toList();
            return new ChecklistResponse(checklist.estaCompleto(), checklist.pendentes().size(), itens);
        }
    }

    public record LinkArquivoResponse(String url, OffsetDateTime expiraEm) {
    }
}
