package br.undb.semear.model.exception;

/** Muitas tentativas seguidas da mesma origem. HTTP 429. */
public class LimiteTentativasException extends SemearException {

    public LimiteTentativasException(String mensagem) {
        super("MUITAS_TENTATIVAS", mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 429;
    }
}
