package br.undb.semear.repository.base;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Verificações gerais do banco (usada pelo endpoint de saúde). */
@Component
public class BancoRepository {

    private final JdbcClient jdbc;

    public BancoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean estaDisponivel() {
        try {
            return jdbc.sql("SELECT 1").query(Integer.class).single() == 1;
        } catch (DataAccessException e) {
            return false;
        }
    }

    /** Fuso da sessão do banco (deve ser America/Fortaleza: ver connection-init-sql no application.yml). */
    public String fuso() {
        try {
            return jdbc.sql("SELECT current_setting('TimeZone')").query(String.class).single();
        } catch (DataAccessException e) {
            return null;
        }
    }
}
