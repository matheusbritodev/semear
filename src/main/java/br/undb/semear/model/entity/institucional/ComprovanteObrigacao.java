package br.undb.semear.model.entity.institucional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.valueobject.ArquivoEnviado;

/** Comprovante de uma obrigação (tabela documento_obrigacao), guardado no bucket privado "obrigacoes". */
public class ComprovanteObrigacao extends Entidade {

    public static final String BUCKET = "obrigacoes";

    private final UUID obrigacaoId;
    private final String caminho;
    private final OffsetDateTime enviadoEm;

    public ComprovanteObrigacao(UUID id, UUID obrigacaoId, String caminho, OffsetDateTime enviadoEm) {
        super(id);
        this.obrigacaoId = Objects.requireNonNull(obrigacaoId, "obrigacaoId");
        this.caminho = Objects.requireNonNull(caminho, "caminho");
        this.enviadoEm = enviadoEm;
    }

    /** Caminho único "<obrigacao>/<uuid>.<ext>", sem o nome original do arquivo. */
    public static ComprovanteObrigacao novo(UUID obrigacaoId, ArquivoEnviado arquivo) {
        return new ComprovanteObrigacao(null, obrigacaoId,
                obrigacaoId + "/" + UUID.randomUUID() + "." + arquivo.getFormato().getExtensao(), null);
    }

    public String getExtensao() {
        return caminho.substring(caminho.lastIndexOf('.') + 1);
    }

    public UUID getObrigacaoId() {
        return obrigacaoId;
    }

    public String getCaminho() {
        return caminho;
    }

    public OffsetDateTime getEnviadoEm() {
        return enviadoEm;
    }
}
