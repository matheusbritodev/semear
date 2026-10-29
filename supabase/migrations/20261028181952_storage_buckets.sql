/* =====================================================================
   SISTEMA SEMEAR — Migration 0200: buckets de arquivos
   Ver docs/03-banco-de-dados.md §8.

   Todos privados e sem políticas em storage.objects: só o backend
   (com a chave secreta) faz upload e gera URLs assinadas.
   As colunas *_url do banco guardam o caminho no bucket.
   ===================================================================== */

INSERT INTO storage.buckets (id, name, public) VALUES
    ('documentos-alunos', 'documentos-alunos', false),  -- documento_aluno.arquivo_url
    ('contratos',         'contratos',         false),  -- contrato.arquivo_url
    ('obrigacoes',        'obrigacoes',        false),  -- documento_obrigacao.arquivo_url
    ('formacao',          'formacao',          false)   -- formacao_continuada.certificado_url
ON CONFLICT (id) DO NOTHING;
