package br.undb.semear.model.valueobject;

import java.util.Arrays;
import java.util.Objects;

import br.undb.semear.model.exception.DadoInvalidoException;

/**
 * Arquivo recebido em um upload. Aceita só PDF, JPEG e PNG de até 5 MB e confere o
 * conteúdo real (assinatura dos primeiros bytes), não apenas a extensão informada.
 */
public final class ArquivoEnviado {

    public static final int TAMANHO_MAXIMO = 5 * 1024 * 1024;

    /** Formatos aceitos, reconhecidos pelos primeiros bytes do arquivo. */
    public enum Formato {
        PDF("pdf", "application/pdf", new byte[] {'%', 'P', 'D', 'F'}),
        JPEG("jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG("png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        private final String extensao;
        private final String contentType;
        private final byte[] assinatura;

        Formato(String extensao, String contentType, byte[] assinatura) {
            this.extensao = extensao;
            this.contentType = contentType;
            this.assinatura = assinatura;
        }

        public String getExtensao() {
            return extensao;
        }

        public String getContentType() {
            return contentType;
        }

        boolean reconhece(byte[] conteudo) {
            return conteudo.length >= assinatura.length
                    && Arrays.equals(Arrays.copyOf(conteudo, assinatura.length), assinatura);
        }
    }

    private final byte[] conteudo;
    private final Formato formato;

    public ArquivoEnviado(byte[] conteudo) {
        Objects.requireNonNull(conteudo, "conteudo");
        if (conteudo.length == 0) {
            throw new DadoInvalidoException("arquivo", "O arquivo está vazio");
        }
        if (conteudo.length > TAMANHO_MAXIMO) {
            throw new DadoInvalidoException("arquivo", "O arquivo passa de 5 MB");
        }
        this.formato = Arrays.stream(Formato.values()).filter(f -> f.reconhece(conteudo)).findFirst()
                .orElseThrow(() -> new DadoInvalidoException("arquivo", "Envie um PDF ou uma foto (JPG ou PNG)"));
        this.conteudo = conteudo.clone();
    }

    public byte[] getConteudo() {
        return conteudo.clone();
    }

    public Formato getFormato() {
        return formato;
    }

    public int getTamanho() {
        return conteudo.length;
    }
}
