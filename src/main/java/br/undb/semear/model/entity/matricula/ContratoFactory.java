package br.undb.semear.model.entity.matricula;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.TipoContrato;

/** Factory: escolhe a subclasse pelo tipo gravado no banco. */
public final class ContratoFactory {

    private ContratoFactory() {
    }

    public static Contrato criar(UUID id, UUID matriculaId, TipoContrato tipo, StatusContrato status, UUID assinadoPor,
                                 OffsetDateTime assinadoEm) {
        return switch (tipo) {
            case CONTRATO_MATRICULA -> new ContratoMatricula(id, matriculaId, status, assinadoPor, assinadoEm);
            case FICHA_SOCIO -> new FichaSocio(id, matriculaId, status, assinadoPor, assinadoEm);
        };
    }
}
