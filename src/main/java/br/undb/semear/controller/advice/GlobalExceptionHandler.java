package br.undb.semear.controller.advice;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.SemearException;

/** Converte qualquer exceção em ErroResponse com o status HTTP certo. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DadoInvalidoException.class)
    public ResponseEntity<ErroResponse> dadoInvalido(DadoInvalidoException e) {
        return responder(ErroResponse.de(e.getStatusHttp(), e.getCodigo(), e.getMessage(),
                List.of(new ErroResponse.CampoErro(e.getCampo(), e.getMessage()))));
    }

    @ExceptionHandler(SemearException.class)
    public ResponseEntity<ErroResponse> dominio(SemearException e) {
        if (e.getStatusHttp() >= 500) {
            LOG.error("Erro interno [{}]", e.getCodigo(), e);
        }
        return responder(ErroResponse.de(e.getStatusHttp(), e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> entradaInvalida(MethodArgumentNotValidException e) {
        List<ErroResponse.CampoErro> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroResponse.CampoErro(f.getField(), f.getDefaultMessage()))
                .toList();
        return responder(ErroResponse.de(400, "ENTRADA_INVALIDA", "Confira os campos destacados", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return responder(ErroResponse.de(400, "ENTRADA_INVALIDA", "Requisição em formato inválido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return responder(ErroResponse.de(400, "ENTRADA_INVALIDA", "Parâmetro inválido: " + e.getName(),
                List.of(new ErroResponse.CampoErro(e.getName(), "Valor inválido"))));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> arquivoGrande(MaxUploadSizeExceededException e) {
        return responder(ErroResponse.de(413, "ARQUIVO_GRANDE", "O arquivo passa de 5 MB",
                List.of(new ErroResponse.CampoErro("arquivo", "O arquivo passa de 5 MB"))));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErroResponse> parteAusente(Exception e) {
        return responder(ErroResponse.de(400, "ENTRADA_INVALIDA", "Faltou informar um campo obrigatório"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException e) {
        return responder(ErroResponse.de(403, "ACESSO_NEGADO", "Você não tem permissão para esta ação"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e) {
        return responder(ErroResponse.de(404, "NAO_ENCONTRADO", "Recurso não encontrado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e) {
        LOG.error("Erro inesperado", e);
        return responder(ErroResponse.de(500, "ERRO_INTERNO", "Ocorreu um erro inesperado. Tente novamente."));
    }

    private static ResponseEntity<ErroResponse> responder(ErroResponse erro) {
        return ResponseEntity.status(erro.status()).body(erro);
    }
}
