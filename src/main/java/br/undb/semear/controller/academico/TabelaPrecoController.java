package br.undb.semear.controller.academico;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.academico.AcademicoDtos.CopiarPrecosRequest;
import br.undb.semear.dto.academico.AcademicoDtos.PrecoRequest;
import br.undb.semear.dto.academico.AcademicoDtos.PrecoSerieResponse;
import br.undb.semear.dto.controle.ControleDtos.SolicitacaoResponse;
import br.undb.semear.service.academico.TabelaPrecoService;
import jakarta.validation.Valid;

/** Tabela de preços por série e ano (configuração da gestão; o financeiro consulta). */
@RestController
@RequestMapping("/api/tabela-precos")
public class TabelaPrecoController {

    private final TabelaPrecoService precos;

    public TabelaPrecoController(TabelaPrecoService precos) {
        this.precos = precos;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'LER') or @permissoes.pode('FINANCEIRO', 'LER')")
    public List<PrecoSerieResponse> doAno(@RequestParam int ano) {
        return precos.doAno(ano);
    }

    /** Pede a mudança: o preço só vale depois que outra pessoa aprovar (202 + pedido). */
    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public SolicitacaoResponse salvar(@Valid @RequestBody PrecoRequest pedido) {
        return precos.solicitarPreco(pedido);
    }

    @PostMapping("/copiar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public SolicitacaoResponse copiar(@Valid @RequestBody CopiarPrecosRequest pedido) {
        return precos.solicitarCopia(pedido.deAno(), pedido.paraAno(), pedido.reajustePercentual());
    }
}
