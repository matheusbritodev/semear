package br.undb.semear.controller.academico;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.academico.MaterialDtos.AlterarItemRequest;
import br.undb.semear.dto.academico.MaterialDtos.CopiarListaRequest;
import br.undb.semear.dto.academico.MaterialDtos.ListaMaterialResponse;
import br.undb.semear.dto.academico.MaterialDtos.NovoItemRequest;
import br.undb.semear.service.academico.MaterialService;
import jakarta.validation.Valid;

/** Listas de materiais (RF06). Por padrão: gestão e secretaria alteram (módulo MATRICULA). */
@RestController
@RequestMapping("/api/materiais")
public class MaterialController {

    private final MaterialService materiais;

    public MaterialController(MaterialService materiais) {
        this.materiais = materiais;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER')")
    public ListaMaterialResponse lista(@RequestParam UUID serieId, @RequestParam int ano) {
        return materiais.lista(serieId, ano);
    }

    @PostMapping("/itens")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public ListaMaterialResponse adicionar(@Valid @RequestBody NovoItemRequest pedido) {
        return materiais.adicionar(pedido.serieId(), pedido.ano(), pedido.descricao(), pedido.quantidade());
    }

    @PutMapping("/itens/{id}")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public ListaMaterialResponse alterar(@PathVariable UUID id, @Valid @RequestBody AlterarItemRequest pedido) {
        return materiais.alterar(id, pedido.descricao(), pedido.quantidade());
    }

    @DeleteMapping("/itens/{id}")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public ListaMaterialResponse remover(@PathVariable UUID id) {
        return materiais.remover(id);
    }

    @PostMapping("/copiar")
    @PreAuthorize("@permissoes.pode('MATRICULA', 'GRAVAR')")
    public ListaMaterialResponse copiar(@Valid @RequestBody CopiarListaRequest pedido) {
        return materiais.copiar(pedido.serieId(), pedido.deAno(), pedido.paraAno());
    }
}
