package br.undb.semear.repository.academico;

import java.util.UUID;

import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.repository.base.Repository;

/** Séries são carga fixa do script (Maternal 1 … 5º Ano), listadas pela ordem. */
public interface SerieRepository extends Repository<Serie, UUID> {
}
