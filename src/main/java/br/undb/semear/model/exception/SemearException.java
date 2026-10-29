package br.undb.semear.model.exception;

/**
 * Raiz das exceções do domínio. Cada subclasse define o status HTTP correspondente,
 * e o código estável é o que a View usa para escolher a mensagem.
 */
public abstract class SemearException extends RuntimeException {

    private final String codigo;

    protected SemearException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    protected SemearException(String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public abstract int getStatusHttp();
}
