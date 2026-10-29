package br.undb.semear.repository.academico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.model.entity.academico.ListaMaterial;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class MaterialJdbcRepository implements MaterialRepository {

    private final JdbcClient jdbc;
    private final TradutorErroBanco tradutor;

    public MaterialJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor) {
        this.jdbc = jdbc;
        this.tradutor = tradutor;
    }

    @Override
    public Optional<ListaMaterial> buscar(UUID serieId, int ano) {
        return executar(() -> jdbc.sql("""
                        SELECT lm.id FROM lista_material lm JOIN ano_letivo al ON al.id = lm.ano_letivo_id
                         WHERE lm.serie_id = :serie AND al.ano = :ano
                        """)
                .param("serie", serieId).param("ano", ano).query(UUID.class).optional())
                .map(id -> carregar(id, serieId, ano));
    }

    @Override
    public Optional<ListaMaterial> buscarPorItem(UUID itemId) {
        record Chave(UUID lista, UUID serie, int ano) {
        }
        return executar(() -> jdbc.sql("""
                        SELECT lm.id, lm.serie_id, al.ano
                          FROM item_material i
                          JOIN lista_material lm ON lm.id = i.lista_id
                          JOIN ano_letivo al ON al.id = lm.ano_letivo_id
                         WHERE i.id = :item
                        """)
                .param("item", itemId)
                .query((rs, n) -> new Chave(Colunas.uuid(rs, "id"), Colunas.uuid(rs, "serie_id"), rs.getInt("ano")))
                .optional())
                .map(c -> carregar(c.lista(), c.serie(), c.ano()));
    }

    @Override
    public ListaMaterial criar(UUID serieId, int ano) {
        UUID id = executar(() -> jdbc.sql("""
                        INSERT INTO lista_material (serie_id, ano_letivo_id)
                        SELECT :serie, al.id FROM ano_letivo al WHERE al.ano = :ano
                        RETURNING id
                        """)
                .param("serie", serieId).param("ano", ano).query(UUID.class).optional())
                .orElseThrow(() -> new RegraNegocioException("ANO_LETIVO_INEXISTENTE", "Cadastre o ano letivo " + ano));
        return new ListaMaterial(id, serieId, ano, List.of());
    }

    @Override
    public UUID inserirItem(UUID listaId, ListaMaterial.Item item) {
        return executar(() -> jdbc.sql("""
                        INSERT INTO item_material (lista_id, descricao, quantidade) VALUES (:lista, :descricao, :quantidade)
                        RETURNING id
                        """)
                .param("lista", listaId).param("descricao", item.descricao()).param("quantidade", item.quantidade())
                .query(UUID.class).single());
    }

    @Override
    public void alterarItem(ListaMaterial.Item item) {
        executar(() -> jdbc.sql("UPDATE item_material SET descricao = :descricao, quantidade = :quantidade WHERE id = :id")
                .param("id", item.id()).param("descricao", item.descricao()).param("quantidade", item.quantidade())
                .update());
    }

    @Override
    public void removerItem(UUID itemId) {
        executar(() -> jdbc.sql("DELETE FROM item_material WHERE id = :id").param("id", itemId).update());
    }

    @Override
    public Optional<ListaDoAluno> listaDoAluno(UUID alunoId) {
        record Matricula(UUID serie, String nome, int ano) {
        }
        return executar(() -> jdbc.sql("""
                        SELECT s.id, s.nome, al.ano
                          FROM matricula m
                          JOIN turma t ON t.id = m.turma_id
                          JOIN serie s ON s.id = t.serie_id
                          JOIN ano_letivo al ON al.id = m.ano_letivo_id
                         WHERE m.aluno_id = :aluno AND m.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')
                         ORDER BY al.ano DESC LIMIT 1
                        """)
                .param("aluno", alunoId)
                .query((rs, n) -> new Matricula(Colunas.uuid(rs, "id"), rs.getString("nome"), rs.getInt("ano")))
                .optional())
                .map(m -> new ListaDoAluno(buscar(m.serie(), m.ano()).orElse(new ListaMaterial(null, m.serie(), m.ano(), List.of())),
                        m.nome(), m.ano()));
    }

    private ListaMaterial carregar(UUID listaId, UUID serieId, int ano) {
        List<ListaMaterial.Item> itens = executar(() -> jdbc.sql("""
                        SELECT id, descricao, quantidade FROM item_material WHERE lista_id = :lista ORDER BY descricao
                        """)
                .param("lista", listaId)
                .query((rs, n) -> new ListaMaterial.Item(Colunas.uuid(rs, "id"), rs.getString("descricao"), rs.getInt("quantidade")))
                .list());
        return new ListaMaterial(listaId, serieId, ano, itens);
    }

    private <R> R executar(Supplier<R> operacao) {
        try {
            return operacao.get();
        } catch (DataAccessException e) {
            throw tradutor.traduzir(e);
        }
    }
}
