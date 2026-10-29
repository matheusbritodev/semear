package br.undb.semear.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da aplicação no fuso de São Luís (America/Fortaleza, UTC-3, sem horário de verão).
 * Injetar Clock em vez de chamar LocalDate.now() direto permite testar regras com datas fixas.
 */
@Configuration
public class RelogioConfig {

    public static final ZoneId FUSO_ESCOLA = ZoneId.of("America/Fortaleza");

    @Bean
    public Clock relogio() {
        return Clock.system(FUSO_ESCOLA);
    }
}
