package br.undb.semear.controller.publico;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.publico.ContaDtos.ContaCriadaResponse;
import br.undb.semear.dto.publico.ContaDtos.CriarContaRequest;
import br.undb.semear.service.conta.ContaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Rotas sem login: criação de conta do responsável (limitada por IP). */
@RestController
@RequestMapping("/api/publico")
public class ContaController {

    private final ContaService contas;

    public ContaController(ContaService contas) {
        this.contas = contas;
    }

    @PostMapping("/contas")
    @ResponseStatus(HttpStatus.CREATED)
    public ContaCriadaResponse criar(@Valid @RequestBody CriarContaRequest pedido, HttpServletRequest requisicao) {
        return new ContaCriadaResponse(contas.criar(pedido.email(), pedido.senha(), requisicao.getRemoteAddr()));
    }
}
