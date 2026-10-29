package br.undb.semear.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrega a View (aplicação TypeScript compilada em static/index.html) para as rotas de tela.
 * O roteamento dentro de cada área é feito pela própria View (React Router).
 */
@Controller
public class ViewController {

    @GetMapping({"/", "/login", "/cadastro", "/recuperar-senha", "/redefinir-senha", "/sem-acesso",
                 "/escola", "/escola/**", "/portal", "/portal/**"})
    public String view() {
        return "forward:/index.html";
    }
}
