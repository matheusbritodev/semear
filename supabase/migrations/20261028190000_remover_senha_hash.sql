/* =====================================================================
   SISTEMA SEMEAR — Remove usuario.senha_hash
   Senha, recuperação e sessão ficam com o Supabase Auth (decisão D02).
   Separada da migration de integração porque exige confirmação manual.
   IF EXISTS: pode ser executada mais de uma vez sem erro.
   ===================================================================== */

ALTER TABLE public.usuario DROP COLUMN IF EXISTS senha_hash;
