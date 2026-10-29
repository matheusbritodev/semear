package br.undb.semear.model.exception;

/** Registro não encontrado. HTTP 404. */
public class RecursoNaoEncontradoException extends SemearException {

    public RecursoNaoEncontradoException(String recurso) {
        super("NAO_ENCONTRADO", recurso + " não encontrado(a)");
    }

    public RecursoNaoEncontradoException(String codigo, String mensagem, Throwable causa) {
        super(codigo, mensagem, causa);
    }

    @Override
    public int getStatusHttp() {
        return 404;
    }
}
