package br.undb.semear.integration.supabase;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.undb.semear.config.SemearProperties;
import br.undb.semear.integration.ArmazenamentoArquivos;
import br.undb.semear.model.exception.IntegracaoException;

/**
 * Supabase Storage via API REST, com a chave secreta (só no servidor).
 * Os buckets são privados: a View nunca acessa os arquivos direto, só por link temporário.
 */
@Component
public class SupabaseStorage implements ArmazenamentoArquivos {

    private static final Logger LOG = LoggerFactory.getLogger(SupabaseStorage.class);

    private final RestClient http;
    private final String baseStorage;

    public SupabaseStorage(SemearProperties propriedades, RestClient.Builder construtor) {
        this.baseStorage = propriedades.supabase().url() + "/storage/v1";
        this.http = construtor
                .baseUrl(baseStorage)
                .defaultHeader("apikey", propriedades.supabase().secretKey())
                .build();
    }

    @Override
    public void enviar(String bucket, String caminho, byte[] conteudo, String contentType) {
        try {
            http.post().uri(objeto("/object/", bucket, caminho))
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("x-upsert", "false")
                    .body(conteudo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            LOG.error("Falha ao enviar arquivo ao Storage (bucket {})", bucket, e);
            throw new IntegracaoException("Não foi possível guardar o arquivo. Tente novamente.", e);
        }
    }

    @Override
    public String urlTemporaria(String bucket, String caminho, Duration validade) {
        try {
            Map<?, ?> resposta = http.post().uri(objeto("/object/sign/", bucket, caminho))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", validade.toSeconds()))
                    .retrieve()
                    .body(Map.class);
            Object assinada = resposta == null ? null : resposta.get("signedURL");
            if (assinada == null) {
                throw new IntegracaoException("O Storage não devolveu o link do arquivo", null);
            }
            return baseStorage + assinada;
        } catch (RestClientException e) {
            LOG.error("Falha ao gerar link temporário no Storage (bucket {})", bucket, e);
            throw new IntegracaoException("Não foi possível abrir o arquivo agora. Tente novamente.", e);
        }
    }

    /** Falha ao remover não interrompe o fluxo: o arquivo órfão fica registrado no log. */
    @Override
    public void remover(String bucket, String caminho) {
        try {
            http.delete().uri(objeto("/object/", bucket, caminho)).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            LOG.warn("Arquivo não removido do Storage (bucket {}): ficará órfão", bucket, e);
        }
    }

    /**
     * Monta a URL do objeto mantendo as "/" do caminho como separadores de pasta.
     * (Passar o caminho como variável de template codificaria a barra como %2F e quebraria o link assinado.)
     */
    private URI objeto(String operacao, String bucket, String caminho) {
        if (!caminho.matches("[A-Za-z0-9._/-]+") || caminho.contains("..")) {
            throw new IllegalArgumentException("Caminho de arquivo inválido");
        }
        return URI.create(baseStorage + operacao + bucket + "/" + caminho);
    }
}
