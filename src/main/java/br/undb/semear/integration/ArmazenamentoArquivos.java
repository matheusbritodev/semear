package br.undb.semear.integration;

import java.time.Duration;

/**
 * Onde os arquivos ficam guardados (padrão Adapter / Gateway).
 * Os services dependem desta interface; a implementação atual é o Supabase Storage,
 * e trocar de provedor é escrever outra classe, sem mudar regra de negócio.
 */
public interface ArmazenamentoArquivos {

    void enviar(String bucket, String caminho, byte[] conteudo, String contentType);

    /** Link temporário para baixar um arquivo de bucket privado. */
    String urlTemporaria(String bucket, String caminho, Duration validade);

    void remover(String bucket, String caminho);
}
