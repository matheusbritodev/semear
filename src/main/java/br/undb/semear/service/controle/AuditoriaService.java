package br.undb.semear.service.controle;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.controle.ControleDtos.AutorResponse;
import br.undb.semear.dto.controle.ControleDtos.RegistroAuditoriaResponse;
import br.undb.semear.dto.controle.ControleDtos.TabelaAuditadaResponse;
import br.undb.semear.dto.controle.ControleDtos.VerificacaoResponse;
import br.undb.semear.model.entity.controle.AlteracaoCampo;
import br.undb.semear.model.entity.controle.CadeiaAuditoria;
import br.undb.semear.model.entity.controle.RegistroAuditoria;
import br.undb.semear.repository.controle.AuditoriaRepository;
import br.undb.semear.repository.controle.AuditoriaRepository.Filtro;
import br.undb.semear.repository.controle.AuditoriaRepository.LinhaAuditoria;
import br.undb.semear.repository.controle.RevisaoAcessoRepository;

/** Trilha de auditoria (SOX): consulta com filtros e verificação de integridade da cadeia de hashes. */
@Service
public class AuditoriaService {

    private static final int LOTE = 1000;
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    /** Tabelas com trigger de auditoria (migração 20261031120000) e o nome para a tela. */
    public static final Map<String, String> TABELAS = tabelas();

    private final AuditoriaRepository auditoria;
    private final RevisaoAcessoRepository revisoes;
    private final ObjectMapper json;
    private final Clock relogio;

    public AuditoriaService(AuditoriaRepository auditoria, RevisaoAcessoRepository revisoes, ObjectMapper json,
                            Clock relogio) {
        this.auditoria = auditoria;
        this.revisoes = revisoes;
        this.json = json;
        this.relogio = relogio;
    }

    private static Map<String, String> tabelas() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("cobranca", "Cobranças");
        t.put("bolsa", "Tipos de bolsa");
        t.put("bolsa_matricula", "Bolsas concedidas");
        t.put("tabela_preco", "Preços");
        t.put("solicitacao_aprovacao", "Pedidos de aprovação");
        t.put("matricula", "Matrículas");
        t.put("contrato", "Contratos");
        t.put("usuario", "Logins");
        t.put("perfil_permissao", "Perfis de acesso");
        t.put("revisao_acesso", "Revisões de acesso");
        t.put("ano_letivo", "Anos letivos");
        return java.util.Collections.unmodifiableMap(t);
    }

    public PaginaResponse<RegistroAuditoriaResponse> pesquisar(Filtro filtro) {
        return auditoria.pesquisar(filtro).mapear(this::resposta);
    }

    public List<AutorResponse> autores() {
        return auditoria.autores().stream().map(a -> new AutorResponse(a.usuarioId(), a.nome())).toList();
    }

    public List<TabelaAuditadaResponse> tabelasAuditadas() {
        return TABELAS.entrySet().stream().map(e -> new TabelaAuditadaResponse(e.getKey(), e.getValue())).toList();
    }

    /**
     * Recalcula a cadeia inteira, em lotes, e confere as âncoras guardadas nas revisões de acesso
     * (detecta também o corte do fim da trilha).
     */
    public VerificacaoResponse verificar() {
        CadeiaAuditoria cadeia = new CadeiaAuditoria();
        long ultimoId = 0;
        boolean continuar = true;
        while (continuar) {
            List<RegistroAuditoria> lote = auditoria.lote(ultimoId, LOTE);
            for (RegistroAuditoria r : lote) {
                if (!cadeia.conferir(r)) {
                    continuar = false;
                    break;
                }
                ultimoId = r.id();
            }
            continuar = continuar && lote.size() == LOTE;
        }
        int ancoras = 0;
        for (var revisao : revisoes.listar()) {
            if (revisao.ancoraId() != null) {
                ancoras++;
                cadeia.conferirAncora(revisao.ancoraId(), revisao.ancoraHash(),
                        auditoria.hashDe(revisao.ancoraId()).orElse(null));
            }
        }
        CadeiaAuditoria.Resultado r = cadeia.resultado();
        String mensagem = r.integra()
                ? r.verificados() + " registros conferidos: nenhum foi alterado ou apagado"
                : r.falha().mensagem();
        return new VerificacaoResponse(r.integra(), r.verificados(), r.ultimoId(), r.ultimoHash(), ancoras,
                r.falha() == null ? null : r.falha().registroId(), r.falha() == null ? null : r.falha().tipo(),
                mensagem, OffsetDateTime.now(relogio));
    }

    private RegistroAuditoriaResponse resposta(LinhaAuditoria linha) {
        RegistroAuditoria r = linha.registro();
        String autor = linha.autor() != null ? linha.autor()
                : r.usuarioId() != null ? "Login removido"
                : "postgres".equals(r.papelBanco()) ? "Sistema (rotina automática ou acesso direto ao banco)" : "Banco: " + r.papelBanco();
        List<AlteracaoCampo> alteracoes = AlteracaoCampo.entre(mapa(r.dadosAntes()), mapa(r.dadosDepois()));
        return new RegistroAuditoriaResponse(r.id(), linha.ocorridoEm(), autor, r.papelBanco(), r.tabela(),
                TABELAS.getOrDefault(r.tabela(), r.tabela()), r.operacao(), r.registroId(), alteracoes);
    }

    private Map<String, Object> mapa(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return json.readValue(texto, MAPA);
        } catch (JsonProcessingException e) {
            return Map.of("erro", "conteúdo ilegível");
        }
    }
}
