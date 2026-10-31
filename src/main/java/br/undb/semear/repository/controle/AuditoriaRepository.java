package br.undb.semear.repository.controle;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.model.entity.controle.RegistroAuditoria;

/** Leitura da trilha de auditoria. Não há escrita: só o trigger do banco grava. */
public interface AuditoriaRepository {

    record Filtro(String tabela, UUID usuarioId, String registroId, LocalDate de, LocalDate ate, int pagina, int tamanho) {
    }

    /** Linha para a tela: o registro mais o horário e o nome de quem fez. */
    record LinhaAuditoria(RegistroAuditoria registro, OffsetDateTime ocorridoEm, String autor) {
    }

    record Autor(UUID usuarioId, String nome) {
    }

    PaginaResponse<LinhaAuditoria> pesquisar(Filtro filtro);

    /** Próximos registros depois do id informado, em ordem (para conferir a cadeia em páginas). */
    List<RegistroAuditoria> lote(long depoisDoId, int limite);

    Optional<String> hashDe(long id);

    Optional<RegistroAuditoria> ultimo();

    /** Logins que aparecem na trilha, para o filtro da tela. */
    List<Autor> autores();
}
