package br.undb.semear.controller.matricula;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.matricula.ContratoDtos.AssinaturaPresencialRequest;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoTermoResponse;
import br.undb.semear.model.entity.matricula.EvidenciaAssinatura;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.service.matricula.ContratoService;
import jakarta.validation.Valid;

/** Contratos na área da escola: termo para imprimir e registro de assinatura feita na secretaria. */
@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService contratos;
    private final EscopoAcesso escopo;

    public ContratoController(ContratoService contratos, EscopoAcesso escopo) {
        this.contratos = contratos;
        this.escopo = escopo;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public ContratoTermoResponse termo(@PathVariable UUID id) {
        return contratos.termo(id);
    }

    @PostMapping("/{id}/assinatura")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public ContratoTermoResponse registrarAssinatura(@PathVariable UUID id,
                                                     @Valid @RequestBody AssinaturaPresencialRequest pedido) {
        return contratos.assinar(id, pedido.responsavelId(),
                EvidenciaAssinatura.presencial(escopo.funcionarioOpcional().orElse(null)));
    }
}
