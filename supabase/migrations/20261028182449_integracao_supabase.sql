/* =====================================================================
   SISTEMA SEMEAR — Migration 0100: integração com o Supabase
   Ver docs/03-banco-de-dados.md §6 e §7.

   1. usuario passa a espelhar auth.users (mesmo id)
      (a remoção de senha_hash está na migration 20261028190000)
   2. RLS ligado em todas as tabelas do schema public (sem políticas)
   3. Views passam a respeitar as permissões de quem consulta
   4. API pública do Supabase (roles anon/authenticated) sem acesso ao public

   O backend conecta como "postgres" (dono das tabelas) e não é afetado
   pelo RLS nem pelos REVOKE abaixo.
   ===================================================================== */


/* ---- 1. usuario = auth.users ----------------------------------------- */

-- O id passa a ser definido pelo Supabase Auth, não gerado aqui
ALTER TABLE public.usuario ALTER COLUMN id DROP DEFAULT;

ALTER TABLE public.usuario
    ADD CONSTRAINT fk_usuario_auth_users
    FOREIGN KEY (id) REFERENCES auth.users (id) ON DELETE CASCADE;

COMMENT ON TABLE public.usuario IS
    'Perfil de acesso do usuário. id = auth.users.id (Supabase Auth). Credenciais ficam no Auth; o perfil define o que o usuário enxerga (ver perfil_permissao).';


/* ---- 2. RLS em todas as tabelas (sem políticas = nega tudo via API) --- */

DO $$
DECLARE t record;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;
END $$;


/* ---- 3. Views executam com as permissões de quem consulta ------------ */
-- Sem isso, a view roda com as permissões do dono e ignora o RLS (PG 15+).

DO $$
DECLARE v record;
BEGIN
    FOR v IN SELECT viewname FROM pg_views WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER VIEW public.%I SET (security_invoker = true)', v.viewname);
    END LOOP;
END $$;


/* ---- 3b. search_path fixo nas funções (recomendação do Supabase) ------ */

DO $$
DECLARE f record;
BEGIN
    FOR f IN SELECT p.oid::regprocedure AS assinatura
               FROM pg_proc p
              WHERE p.pronamespace = 'public'::regnamespace AND p.prokind = 'f' LOOP
        EXECUTE format('ALTER FUNCTION %s SET search_path = public, pg_temp', f.assinatura);
    END LOOP;
END $$;


/* ---- 4. Fecha o schema public para a API pública do Supabase ---------- */

REVOKE ALL     ON ALL TABLES    IN SCHEMA public FROM anon, authenticated;
REVOKE ALL     ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;
REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA public FROM PUBLIC, anon, authenticated;

-- Vale também para objetos criados por migrations futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL     ON TABLES    FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL     ON SEQUENCES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC, anon, authenticated;
