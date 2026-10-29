package br.undb.semear.model.entity.matricula;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.valueobject.ArquivoEnviado;

/**
 * Documento do aluno guardado no bucket privado "documentos-alunos".
 * O banco guarda o caminho dentro do bucket (coluna arquivo_url), nunca uma URL pública (docs/03 §8).
 */
public class DocumentoAluno extends Entidade {

    public static final String BUCKET = "documentos-alunos";

    private final UUID alunoId;
    private final TipoDocumentoAluno tipo;
    private final String caminho;
    private final OffsetDateTime enviadoEm;

    public DocumentoAluno(UUID id, UUID alunoId, TipoDocumentoAluno tipo, String caminho, OffsetDateTime enviadoEm) {
        super(id);
        this.alunoId = Objects.requireNonNull(alunoId, "alunoId");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.caminho = Objects.requireNonNull(caminho, "caminho");
        this.enviadoEm = enviadoEm;
    }

    /** Novo documento: caminho único "<aluno>/<tipo>-<uuid>.<ext>", sem nome original (que pode ter dados pessoais). */
    public static DocumentoAluno novo(UUID alunoId, TipoDocumentoAluno tipo, ArquivoEnviado arquivo) {
        String caminho = alunoId + "/" + tipo.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-"
                + UUID.randomUUID() + "." + arquivo.getFormato().getExtensao();
        return new DocumentoAluno(null, alunoId, tipo, caminho, null);
    }

    public boolean pertenceAo(UUID idAluno) {
        return alunoId.equals(idAluno);
    }

    public String getExtensao() {
        int ponto = caminho.lastIndexOf('.');
        return ponto < 0 ? "" : caminho.substring(ponto + 1);
    }

    public UUID getAlunoId() {
        return alunoId;
    }

    public TipoDocumentoAluno getTipo() {
        return tipo;
    }

    public String getCaminho() {
        return caminho;
    }

    public OffsetDateTime getEnviadoEm() {
        return enviadoEm;
    }
}
