package br.undb.semear.model.exception;

/** Usuário autenticado sem permissão para a ação. HTTP 403. */
public class AcessoNegadoException extends SemearException {

    public AcessoNegadoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public AcessoNegadoException(String codigo, String mensagem, Throwable causa) {
        super(codigo, mensagem, causa);
    }

    @Override
    public int getStatusHttp() {
        return 403;
    }
}
