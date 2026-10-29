package br.undb.semear.model.exception;

/** Regra de negócio violada (HTTP 422). */
public class RegraNegocioException extends SemearException {

    public RegraNegocioException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public RegraNegocioException(String codigo, String mensagem, Throwable causa) {
        super(codigo, mensagem, causa);
    }

    @Override
    public int getStatusHttp() {
        return 422;
    }
}
