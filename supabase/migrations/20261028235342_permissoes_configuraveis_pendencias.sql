/* =====================================================================
   SISTEMA SEMEAR — Perfis de acesso configuráveis e módulo PENDENCIAS
   Orientação do professor (28/10/2026): pendências de documentos visíveis por padrão a
   gestor, secretaria e responsável; o gestor pode ligar/desligar permissões, com volta ao padrão.
   ===================================================================== */

-- Novo módulo: painel de pendências dos alunos (padrão: ADMIN e SECRETARIA).
-- O responsável vê as pendências dos próprios filhos pelo módulo PORTAL.
INSERT INTO perfil_permissao (perfil, modulo, pode_ler, pode_gravar) VALUES
    ('ADMIN',      'PENDENCIAS', true, true),
    ('SECRETARIA', 'PENDENCIAS', true, true)
ON CONFLICT (perfil, modulo) DO NOTHING;

-- Matriz padrão, usada pelo botão "Restaurar padrão" da tela de perfis de acesso.
CREATE TABLE perfil_permissao_padrao (LIKE perfil_permissao INCLUDING ALL);
INSERT INTO perfil_permissao_padrao SELECT * FROM perfil_permissao;
COMMENT ON TABLE perfil_permissao_padrao IS
    'Cópia da matriz perfil x módulo original. A tela de perfis de acesso restaura perfil_permissao a partir daqui.';

ALTER TABLE perfil_permissao_padrao ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON perfil_permissao_padrao FROM anon, authenticated;
