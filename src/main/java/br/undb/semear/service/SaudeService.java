package br.undb.semear.service;

import org.springframework.stereotype.Service;

import br.undb.semear.repository.base.BancoRepository;

/** Verifica se a aplicação e o banco estão respondendo. */
@Service
public class SaudeService {

    private final BancoRepository banco;

    public SaudeService(BancoRepository banco) {
        this.banco = banco;
    }

    public boolean bancoDisponivel() {
        return banco.estaDisponivel();
    }

    public String fusoBanco() {
        return banco.fuso();
    }
}
