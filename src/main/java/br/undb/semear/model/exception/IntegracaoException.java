package br.undb.semear.model.exception;

/** Falha em serviço externo (Storage, Cora, e-mail). HTTP 502, sem expor detalhes ao usuário. */
public class IntegracaoException extends SemearException {

    public IntegracaoException(String mensagem, Throwable causa) {
        super("ERRO_INTEGRACAO", mensagem, causa);
    }

    @Override
    public int getStatusHttp() {
        return 502;
    }
}
