package br.undb.semear.repository.matricula;

import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.matricula.DocumentoAluno;
import br.undb.semear.repository.base.Repository;

public interface DocumentoAlunoRepository extends Repository<DocumentoAluno, UUID> {

    List<DocumentoAluno> listarDoAluno(UUID alunoId);
}
