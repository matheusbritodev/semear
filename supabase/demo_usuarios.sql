/* =====================================================================
   SISTEMA SEMEAR — Vincula os logins (Supabase Auth) aos perfis do sistema
   Rodar DEPOIS do seed.sql e de criar os usuários no Auth.
   Desenvolvimento apenas. Idempotente (pode rodar mais de uma vez).
   ===================================================================== */

-- Administrador
INSERT INTO usuario (id, email, perfil)
SELECT id, lower(email), 'ADMIN' FROM auth.users WHERE lower(email) = 'matheusbritodev1@gmail.com'
ON CONFLICT (id) DO UPDATE SET perfil = EXCLUDED.perfil;

INSERT INTO funcionario (usuario_id, nome, cpf, tipo, email, data_admissao)
SELECT u.id, 'Matheus Brito', '00000000191', 'GESTOR', u.email, '2026-02-02'
  FROM usuario u WHERE u.email = 'matheusbritodev1@gmail.com'
ON CONFLICT (cpf) DO NOTHING;

-- Contas de demonstração
INSERT INTO usuario (id, email, perfil)
SELECT a.id, lower(a.email), v.perfil::perfil_usuario
  FROM auth.users a
  JOIN (VALUES ('secretaria.semear@example.com',  'SECRETARIA'),
               ('financeiro.semear@example.com',  'FINANCEIRO'),
               ('professora.semear@example.com',  'PROFESSOR'),
               ('responsavel.semear@example.com', 'RESPONSAVEL')) AS v(email, perfil)
    ON v.email = lower(a.email)
ON CONFLICT (id) DO UPDATE SET perfil = EXCLUDED.perfil;

UPDATE funcionario f SET usuario_id = u.id
  FROM usuario u
 WHERE u.email = f.email AND u.perfil IN ('SECRETARIA', 'FINANCEIRO', 'PROFESSOR') AND f.usuario_id IS NULL;

-- Responsável de demonstração: mãe, responsável financeira de 2 filhos, sem cobranças em atraso
UPDATE responsavel r SET usuario_id = u.id, email = u.email
  FROM usuario u
 WHERE u.email = 'responsavel.semear@example.com'
   AND NOT EXISTS (SELECT 1 FROM responsavel x WHERE x.usuario_id = u.id)
   AND r.id = (SELECT ar.responsavel_id
                 FROM aluno_responsavel ar
                WHERE ar.resp_financeiro AND ar.parentesco = 'Mãe'
                  AND NOT EXISTS (SELECT 1 FROM vw_inadimplencia i
                                    JOIN aluno_responsavel y ON y.aluno_id = i.aluno_id
                                   WHERE y.responsavel_id = ar.responsavel_id)
                GROUP BY ar.responsavel_id
               HAVING count(*) = 2
                ORDER BY ar.responsavel_id
                LIMIT 1);

-- Conferência
SELECT u.email, u.perfil,
       COALESCE(f.nome, r.nome) AS pessoa,
       (SELECT count(*) FROM aluno_responsavel ar WHERE ar.responsavel_id = r.id) AS filhos
  FROM usuario u
  LEFT JOIN funcionario f ON f.usuario_id = u.id
  LEFT JOIN responsavel r ON r.usuario_id = u.id
 ORDER BY u.perfil;
