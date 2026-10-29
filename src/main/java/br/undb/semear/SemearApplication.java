package br.undb.semear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do Sistema Semear.
 * Arquitetura MVC: Model (model, service, repository) e Controller (controller) em Java;
 * View em TypeScript (src/main/view), servida por esta mesma aplicação em produção.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SemearApplication {

    public static void main(String[] args) {
        SpringApplication.run(SemearApplication.class, args);
    }
}
