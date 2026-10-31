-- Controles internos (orientação do professor em 31/10/2026: SoD e SOX). Ver docs/10-controles-internos.md.
--  1. Trilha de auditoria imutável com hash encadeado (quem, quando, antes/depois).
--  2. Dupla aprovação (quem pede não aprova) para bolsas e preços.
--  3. Revisão periódica de acessos.
--  4. Novos módulos na matriz de permissões: APROVACOES e AUDITORIA.

/* ---------------------------------------------------------------- 1. Auditoria */

CREATE TABLE auditoria (
    id            bigint      PRIMARY KEY,
    ocorrido_em   timestamptz NOT NULL,
    usuario_id    uuid,                         -- login da aplicação (sem FK: o histórico sobrevive à remoção do login)
    papel_banco   text        NOT NULL,         -- usuário do PostgreSQL: mostra alterações feitas fora da aplicação
    tabela        text        NOT NULL,
    operacao      text        NOT NULL CHECK (operacao IN ('INSERT', 'UPDATE', 'DELETE')),
    registro_id   text,
    dados_antes   jsonb,
    dados_depois  jsonb,
    hash_anterior char(64)    NOT NULL,
    hash          char(64)    NOT NULL
);
CREATE SEQUENCE auditoria_id_seq OWNED BY auditoria.id;
CREATE INDEX ix_auditoria_tabela ON auditoria (tabela, ocorrido_em DESC);
CREATE INDEX ix_auditoria_usuario ON auditoria (usuario_id, ocorrido_em DESC);
CREATE INDEX ix_auditoria_registro ON auditoria (registro_id);
COMMENT ON TABLE auditoria IS
    'Trilha de auditoria (SOX): só recebe INSERT pelo trigger fn_auditar. Cada linha guarda o hash da anterior; '
    'a aplicação recalcula a cadeia (CadeiaAuditoria) para provar que nada foi alterado ou apagado.';

-- Texto exato que entra no hash. A aplicação Java monta o mesmo texto para conferir (CadeiaAuditoria).
CREATE OR REPLACE FUNCTION fn_auditoria_conteudo(p_id bigint, p_em timestamptz, p_usuario uuid, p_papel text,
                                                 p_tabela text, p_operacao text, p_registro text,
                                                 p_antes jsonb, p_depois jsonb, p_hash_anterior text)
RETURNS text LANGUAGE sql IMMUTABLE AS $$
    SELECT concat_ws('|', p_id::text,
                     to_char(p_em AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US'),
                     coalesce(p_usuario::text, ''), p_papel, p_tabela, p_operacao, coalesce(p_registro, ''),
                     coalesce(p_antes::text, ''), coalesce(p_depois::text, ''), p_hash_anterior)
$$;

CREATE OR REPLACE FUNCTION fn_auditar() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_antes    jsonb;
    v_depois   jsonb;
    v_linha    jsonb;
    v_registro text;
    v_usuario  uuid;
    v_anterior text;
    v_id       bigint;
    v_em       timestamptz := clock_timestamp();
BEGIN
    IF tg_op IN ('UPDATE', 'DELETE') THEN v_antes := to_jsonb(OLD); END IF;
    IF tg_op IN ('INSERT', 'UPDATE') THEN v_depois := to_jsonb(NEW); END IF;
    -- UPDATE que não muda nada (só o atualizado_em) não vira registro
    IF tg_op = 'UPDATE' AND (v_antes - 'atualizado_em') = (v_depois - 'atualizado_em') THEN
        RETURN NULL;
    END IF;

    v_linha := coalesce(v_depois, v_antes);
    v_registro := coalesce(v_linha ->> 'id', (SELECT string_agg(v_linha ->> k, ':') FROM unnest(tg_argv) AS k));
    -- A aplicação informa o login a cada conexão (DataSourceComUsuario). Vazio = rotina automática ou acesso direto.
    v_usuario := nullif(current_setting('semear.usuario_id', true), '')::uuid;

    -- Uma gravação por vez na cadeia: o lock dura até o fim da transação.
    PERFORM pg_advisory_xact_lock(hashtext('semear.auditoria'));
    SELECT a.hash INTO v_anterior FROM auditoria a ORDER BY a.id DESC LIMIT 1;
    v_anterior := coalesce(v_anterior, repeat('0', 64));
    v_id := nextval('auditoria_id_seq');

    INSERT INTO auditoria (id, ocorrido_em, usuario_id, papel_banco, tabela, operacao, registro_id,
                           dados_antes, dados_depois, hash_anterior, hash)
    VALUES (v_id, v_em, v_usuario, current_user, tg_table_name, tg_op, v_registro, v_antes, v_depois, v_anterior,
            encode(sha256(convert_to(fn_auditoria_conteudo(v_id, v_em, v_usuario, current_user, tg_table_name, tg_op,
                                                           v_registro, v_antes, v_depois, v_anterior), 'UTF8')), 'hex'));
    RETURN NULL;
END;
$$;

-- Imutável: nem a aplicação nem o painel conseguem alterar ou apagar a trilha.
CREATE OR REPLACE FUNCTION fn_auditoria_imutavel() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'A trilha de auditoria não pode ser alterada nem apagada';
END;
$$;
CREATE TRIGGER trg_auditoria_imutavel BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_imutavel();
CREATE TRIGGER trg_auditoria_sem_truncate BEFORE TRUNCATE ON auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_imutavel();

/* ---------------------------------------------------------------- 2. Dupla aprovação */

CREATE TYPE tipo_solicitacao AS ENUM ('CONCESSAO_BOLSA', 'ALTERACAO_BOLSA', 'ALTERACAO_PRECO', 'COPIA_PRECOS');
CREATE TYPE status_solicitacao AS ENUM ('PENDENTE', 'APROVADA', 'REJEITADA', 'CANCELADA');

CREATE TABLE solicitacao_aprovacao (
    id             uuid               PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo           tipo_solicitacao   NOT NULL,
    status         status_solicitacao NOT NULL DEFAULT 'PENDENTE',
    resumo         varchar(300)       NOT NULL,
    dados          jsonb              NOT NULL,
    chave          varchar(200)       NOT NULL,   -- o que está sendo pedido (evita dois pedidos iguais em aberto)
    matricula_id   uuid               REFERENCES matricula (id),
    solicitada_por uuid               NOT NULL REFERENCES usuario (id),
    solicitada_em  timestamptz        NOT NULL DEFAULT now(),
    decidida_por   uuid               REFERENCES usuario (id),
    decidida_em    timestamptz,
    motivo         varchar(500),
    CONSTRAINT ck_solicitacao_decisao CHECK ((status = 'PENDENTE') = (decidida_em IS NULL)),
    -- SoD também no banco: quem pediu não aprova nem rejeita (só pode cancelar o próprio pedido)
    CONSTRAINT ck_solicitacao_sod CHECK (status = 'CANCELADA' OR decidida_por IS NULL OR decidida_por <> solicitada_por),
    CONSTRAINT ck_solicitacao_motivo CHECK (status <> 'REJEITADA' OR length(trim(motivo)) > 0)
);
CREATE UNIQUE INDEX uq_solicitacao_pendente ON solicitacao_aprovacao (tipo, chave) WHERE status = 'PENDENTE';
CREATE INDEX ix_solicitacao_status ON solicitacao_aprovacao (status, solicitada_em DESC);
COMMENT ON TABLE solicitacao_aprovacao IS
    'Pedidos que precisam de uma segunda pessoa (maker-checker): conceder bolsa, mudar desconto de bolsa, mudar preços.';

/* ---------------------------------------------------------------- 3. Revisão de acessos */

CREATE TABLE revisao_acesso (
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    realizada_em     timestamptz NOT NULL DEFAULT now(),
    realizada_por    uuid        NOT NULL REFERENCES usuario (id),
    contas_revisadas integer     NOT NULL CHECK (contas_revisadas >= 0),
    alertas          integer     NOT NULL CHECK (alertas >= 0),
    observacao       varchar(1000),
    resultado        jsonb       NOT NULL
);
COMMENT ON TABLE revisao_acesso IS
    'Revisão trimestral de acessos (SOX): quem revisou, quando e a foto das contas e alertas naquele momento.';

/* ---------------------------------------------------------------- 4. Permissões */

INSERT INTO perfil_permissao (perfil, modulo, pode_ler, pode_gravar) VALUES
    ('ADMIN', 'APROVACOES', true, true),
    ('FINANCEIRO', 'APROVACOES', true, true),
    ('ADMIN', 'AUDITORIA', true, false)
ON CONFLICT (perfil, modulo) DO NOTHING;
INSERT INTO perfil_permissao_padrao (perfil, modulo, pode_ler, pode_gravar) VALUES
    ('ADMIN', 'APROVACOES', true, true),
    ('FINANCEIRO', 'APROVACOES', true, true),
    ('ADMIN', 'AUDITORIA', true, false)
ON CONFLICT (perfil, modulo) DO NOTHING;

/* ---------------------------------------------------------------- RLS e triggers de auditoria */

ALTER TABLE auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE solicitacao_aprovacao ENABLE ROW LEVEL SECURITY;
ALTER TABLE revisao_acesso ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON auditoria, solicitacao_aprovacao, revisao_acesso FROM anon, authenticated;
REVOKE ALL ON SEQUENCE auditoria_id_seq FROM anon, authenticated;
REVOKE ALL ON FUNCTION fn_auditar() FROM PUBLIC, anon, authenticated;

-- Tabelas com dinheiro, acesso ou compromisso com a família.
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON usuario FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON perfil_permissao
    FOR EACH ROW EXECUTE FUNCTION fn_auditar('perfil', 'modulo');
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON bolsa FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON bolsa_matricula FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON tabela_preco FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON cobranca FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON contrato FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON matricula FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON ano_letivo FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON solicitacao_aprovacao
    FOR EACH ROW EXECUTE FUNCTION fn_auditar();
CREATE TRIGGER trg_auditar AFTER INSERT OR UPDATE OR DELETE ON revisao_acesso FOR EACH ROW EXECUTE FUNCTION fn_auditar();
