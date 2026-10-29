package br.undb.semear.repository.base;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.exception.SemearException;

/**
 * Traduz os erros do PostgreSQL em exceções do domínio com código estável.
 * As regras do banco lançam RAISE EXCEPTION (SQLSTATE P0001) com mensagem em português;
 * a tradução é feita pelo trecho da mensagem (docs/03-banco-de-dados.md §11).
 */
@Component
public class TradutorErroBanco {

    private static final Logger LOG = LoggerFactory.getLogger(TradutorErroBanco.class);

    private record Regra(String trecho, BiFunction<String, Throwable, SemearException> criar) {
    }

    private static final List<Regra> REGRAS = List.of(
            new Regra("lotada: limite de", (m, c) -> new ConflitoException("TURMA_LOTADA", m, c)),
            new Regra("não possui responsável financeiro", (m, c) -> new RegraNegocioException("SEM_RESPONSAVEL_FINANCEIRO", m, c)),
            new Regra("Cadastre a tabela_preco", (m, c) -> new RegraNegocioException("TABELA_PRECO_AUSENTE", m, c)),
            new Regra("Pré-matrícula já", (m, c) -> new ConflitoException("PRE_MATRICULA_JA_ANALISADA", m, c)),
            new Regra("já analisada", (m, c) -> new ConflitoException("PRE_MATRICULA_JA_ANALISADA", m, c)),
            new Regra("Informe o motivo da rejeição", (m, c) -> new RegraNegocioException("MOTIVO_OBRIGATORIO", m, c)),
            new Regra("não corresponde à série/ano letivo", (m, c) -> new RegraNegocioException("TURMA_INCOMPATIVEL", m, c)),
            new Regra("Aluno sem matrícula anterior", (m, c) -> new RegraNegocioException("SEM_MATRICULA_ANTERIOR", m, c)),
            new Regra("Cobrança já está paga", (m, c) -> new ConflitoException("COBRANCA_JA_PAGA", m, c)),
            new Regra("Cobrança cancelada não pode ser paga", (m, c) -> new ConflitoException("COBRANCA_CANCELADA", m, c)),
            new Regra("Cobrança paga não pode ser", (m, c) -> new ConflitoException("COBRANCA_IMUTAVEL", m, c)),
            new Regra("Informe a origem do pagamento", (m, c) -> new RegraNegocioException("ORIGEM_PAGAMENTO_OBRIGATORIA", m, c)),
            new Regra("só pode ser cobrad", (m, c) -> new RegraNegocioException("TIPO_COBRANCA_INCOMPATIVEL", m, c)),
            new Regra("não está vinculado ao aluno", (m, c) -> new RegraNegocioException("RESPONSAVEL_NAO_VINCULADO", m, c)),
            new Regra("assinante não é responsável", (m, c) -> new AcessoNegadoException("ASSINANTE_INVALIDO", m, c)),
            new Regra("Professor não está vinculado", (m, c) -> new AcessoNegadoException("PROFESSOR_FORA_DA_TURMA", m, c)),
            new Regra("não está ativa na turma deste diário", (m, c) -> new RegraNegocioException("MATRICULA_INATIVA_NA_TURMA", m, c)),
            new Regra("fora do período do ano letivo", (m, c) -> new RegraNegocioException("DATA_FORA_ANO_LETIVO", m, c)),
            new Regra("não encontrad", (m, c) -> new RecursoNaoEncontradoException("NAO_ENCONTRADO", m, c)));

    public SemearException traduzir(DataAccessException erro) {
        SQLException sql = encontrarSqlException(erro);
        String estado = sql != null ? sql.getSQLState() : null;
        String mensagem = mensagemLimpa(sql != null ? sql.getMessage() : erro.getMessage());

        if ("P0001".equals(estado)) {
            for (Regra regra : REGRAS) {
                if (mensagem.contains(regra.trecho())) {
                    return regra.criar().apply(mensagem, erro);
                }
            }
            return new RegraNegocioException("REGRA_DO_BANCO", mensagem, erro);
        }
        if ("23505".equals(estado)) {
            return new ConflitoException("DUPLICADO", "Já existe um registro com esses dados", erro);
        }
        if ("23514".equals(estado) || "23503".equals(estado) || "23502".equals(estado) || "22P02".equals(estado)) {
            return new RegraNegocioException("DADO_INVALIDO", "Dados inválidos ou incompletos", erro);
        }
        LOG.error("Erro inesperado no banco (SQLSTATE {})", estado, erro);
        return new ErroInternoBancoException(erro);
    }

    private static SQLException encontrarSqlException(Throwable erro) {
        Throwable atual = erro;
        while (atual != null) {
            if (atual instanceof SQLException sql) {
                return sql;
            }
            atual = atual.getCause();
        }
        return null;
    }

    /** Remove o prefixo "ERROR: " e o contexto (linhas "Where:") que o PostgreSQL acrescenta. */
    private static String mensagemLimpa(String mensagem) {
        if (mensagem == null) {
            return "";
        }
        String primeiraLinha = mensagem.lines().findFirst().orElse("");
        return primeiraLinha.replaceFirst("^(ERROR|ERRO):\\s*", "").trim();
    }

    /** Falha técnica do banco (conexão, SQL inválido...). Vira HTTP 500 sem expor detalhes. */
    public static class ErroInternoBancoException extends SemearException {

        public ErroInternoBancoException(Throwable causa) {
            super("ERRO_BANCO", "Não foi possível concluir a operação no banco de dados", causa);
        }

        @Override
        public int getStatusHttp() {
            return 500;
        }
    }
}
