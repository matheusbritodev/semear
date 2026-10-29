package br.undb.semear.repository.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;

/**
 * Leitura de colunas do ResultSet para tipos do Model.
 * Contatos gravados fora do padrão (dados antigos) viram null com aviso no log, em vez de derrubar a tela.
 */
public final class Colunas {

    private static final Logger LOG = LoggerFactory.getLogger(Colunas.class);

    private Colunas() {
    }

    public static UUID uuid(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, UUID.class);
    }

    public static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, LocalDate.class);
    }

    public static Cpf cpf(ResultSet rs, String coluna) throws SQLException {
        String valor = rs.getString(coluna);
        return valor == null ? null : new Cpf(valor);
    }

    public static Telefone telefone(ResultSet rs, String coluna) throws SQLException {
        try {
            return Telefone.opcional(rs.getString(coluna));
        } catch (DadoInvalidoException e) {
            LOG.warn("Telefone fora do padrão ignorado na coluna {}", coluna);
            return null;
        }
    }

    public static Email email(ResultSet rs, String coluna) throws SQLException {
        try {
            return Email.opcional(rs.getString(coluna));
        } catch (DadoInvalidoException e) {
            LOG.warn("E-mail fora do padrão ignorado na coluna {}", coluna);
            return null;
        }
    }

    /** Termo de busca: vazio vira null (o SQL usa "CAST(:busca AS text) IS NULL"). */
    public static String termo(String busca) {
        return busca == null || busca.isBlank() ? null : busca.trim();
    }
}
