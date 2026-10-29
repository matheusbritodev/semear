/* =====================================================================
   SISTEMA SEMEAR — GESTÃO ESCOLAR
   Script completo do banco de dados (PostgreSQL 13+ / Supabase)

   COMO USAR
   1. Cole o script inteiro no SQL Editor e execute uma única vez.
   2. Em banco que já tem as tabelas, descomente o bloco "RESET" abaixo
      (apaga TUDO do schema public).
   3. Antes de testar matrículas, cadastre ano_letivo, turma e tabela_preco
      (exemplo no final do arquivo).

   ESTRUTURA
   0. Reset opcional
   1. Tipos enumerados
   2. Tabelas (pessoas, estrutura escolar, matrícula, financeiro,
      materiais, pedagógico, obrigações, controle de acesso)
   3. Índices e restrições de unicidade parcial
   4. Funções de apoio e triggers (regras de negócio)
   5. Funções de rotina (fluxos de matrícula, cobrança, notificação)
   6. Views (relatórios e portais)
   7. Dados iniciais (séries, disciplinas, permissões, obrigações)
   8. Exemplo de fluxo de teste (comentado)

   REQUISITOS COBERTOS
   RF01 matrícula/rematrícula · RF02 bolsas · RF03/RN04 limite de 30
   RF04 financeiro/Cora · RF05 notificações · RF06 lista de materiais
   RF07 RH · RF08 pedagógico · RF09 obrigações · RF10 portal
   RN01 cobrança por série · RN02 responsável N:N aluno
   RN03 cadastro único · RN04 máximo 30 alunos por turma
   ===================================================================== */


/* ---------------------------------------------------------------------
   0. RESET OPCIONAL (descomente só se quiser recriar tudo do zero)
   --------------------------------------------------------------------- */
-- DROP SCHEMA public CASCADE;
-- CREATE SCHEMA public;


/* ---------------------------------------------------------------------
   1. TIPOS ENUMERADOS
   --------------------------------------------------------------------- */
CREATE TYPE perfil_usuario       AS ENUM ('ADMIN','SECRETARIA','FINANCEIRO','PROFESSOR','RESPONSAVEL','ALUNO');
CREATE TYPE tipo_funcionario     AS ENUM ('ADMINISTRATIVO','PROFESSOR','GESTOR','PRESTADOR_SERVICO');
CREATE TYPE nivel_ensino         AS ENUM ('EDUCACAO_INFANTIL','ENSINO_FUNDAMENTAL');
CREATE TYPE status_pre_matricula AS ENUM ('PENDENTE','APROVADA','REJEITADA');
CREATE TYPE tipo_matricula       AS ENUM ('NOVA','REMATRICULA');
CREATE TYPE status_matricula     AS ENUM ('AGUARDANDO_PAGAMENTO','ATIVA','CANCELADA','ENCERRADA');
CREATE TYPE tipo_farda           AS ENUM ('UNIFORME','EDUCACAO_FISICA');
CREATE TYPE tipo_contrato        AS ENUM ('CONTRATO_MATRICULA','FICHA_SOCIO');
CREATE TYPE status_contrato      AS ENUM ('PENDENTE','ASSINADO','CANCELADO');
CREATE TYPE tipo_desconto        AS ENUM ('PERCENTUAL','VALOR_FIXO');
CREATE TYPE tipo_cobranca        AS ENUM ('MATRICULA','TAXA_ASSOCIADO','MENSALIDADE');
CREATE TYPE status_cobranca      AS ENUM ('PENDENTE','PAGO','VENCIDO','CANCELADO');
CREATE TYPE origem_pagamento     AS ENUM ('SITE','PRESENCIAL');
CREATE TYPE tipo_notificacao     AS ENUM ('VENCIMENTO_PROXIMO','INADIMPLENCIA');
CREATE TYPE canal_notificacao    AS ENUM ('EMAIL','SMS','WHATSAPP');
CREATE TYPE status_notificacao   AS ENUM ('PENDENTE','ENVIADA','FALHA');
CREATE TYPE tipo_plano           AS ENUM ('PLANO_AULA','PLANEJAMENTO_DIARIO');
CREATE TYPE tipo_calendario      AS ENUM ('AULA','FERIADO','RECESSO','EVENTO');
CREATE TYPE orgao_externo        AS ENUM ('CENSO_INEP','SMTT','SISTEMA_PRESENCA','CORA');
CREATE TYPE status_obrigacao     AS ENUM ('PENDENTE','CUMPRIDA','ATRASADA');


/* ---------------------------------------------------------------------
   2. TABELAS
   --------------------------------------------------------------------- */

/* ---- 2.1 Acesso e pessoas ------------------------------------------ */

CREATE TABLE usuario (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          varchar(255) NOT NULL UNIQUE,
    senha_hash     varchar(255) NOT NULL,
    perfil         perfil_usuario NOT NULL,
    ativo          boolean      NOT NULL DEFAULT true,
    ultimo_login   timestamptz,
    criado_em      timestamptz  NOT NULL DEFAULT now(),
    atualizado_em  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email))
);
COMMENT ON TABLE usuario IS 'Credenciais de acesso. O perfil define o que o usuário enxerga (ver perfil_permissao).';

CREATE TABLE responsavel (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id     uuid         UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nome           varchar(150) NOT NULL,
    cpf            char(11)     NOT NULL UNIQUE,
    telefone       varchar(20),
    email          varchar(255),
    criado_em      timestamptz  NOT NULL DEFAULT now(),
    atualizado_em  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_responsavel_cpf CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE TABLE aluno (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_cadastro  bigint       GENERATED ALWAYS AS IDENTITY UNIQUE,
    usuario_id       uuid         UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nome             varchar(150) NOT NULL,
    data_nascimento  date         NOT NULL,
    cpf              char(11)     UNIQUE,
    ativo            boolean      NOT NULL DEFAULT true,
    criado_em        timestamptz  NOT NULL DEFAULT now(),
    atualizado_em    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_aluno_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$')
);
COMMENT ON TABLE aluno IS 'RN03/RF01.6: cadastro único. A renovação anual é feita por nova linha em matricula, nunca por novo aluno.';

CREATE TABLE aluno_responsavel (
    aluno_id          uuid NOT NULL REFERENCES aluno(id)       ON DELETE CASCADE,
    responsavel_id    uuid NOT NULL REFERENCES responsavel(id) ON DELETE CASCADE,
    parentesco        varchar(50),
    resp_financeiro   boolean NOT NULL DEFAULT false,
    PRIMARY KEY (aluno_id, responsavel_id)
);
COMMENT ON TABLE aluno_responsavel IS 'RN02: relação N:N. Exatamente um responsável financeiro por aluno (índice parcial).';

CREATE TABLE funcionario (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      uuid         UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nome            varchar(150) NOT NULL,
    cpf             char(11)     NOT NULL UNIQUE,
    tipo            tipo_funcionario NOT NULL,
    telefone        varchar(20),
    email           varchar(255),
    data_admissao   date,
    ativo           boolean      NOT NULL DEFAULT true,
    criado_em       timestamptz  NOT NULL DEFAULT now(),
    atualizado_em   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_funcionario_cpf CHECK (cpf ~ '^[0-9]{11}$')
);
COMMENT ON TABLE funcionario IS 'RF07: administrativos, professores, gestores e prestadores de serviço.';

/* ---- 2.2 Estrutura escolar ----------------------------------------- */

CREATE TABLE ano_letivo (
    id            uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    ano           integer  NOT NULL UNIQUE,
    data_inicio   date     NOT NULL,
    data_fim      date     NOT NULL,
    CONSTRAINT ck_ano_letivo_ano   CHECK (ano BETWEEN 2000 AND 2100),
    CONSTRAINT ck_ano_letivo_datas CHECK (data_fim > data_inicio)
);

CREATE TABLE serie (
    id     uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome   varchar(50) NOT NULL UNIQUE,
    nivel  nivel_ensino NOT NULL,
    ordem  integer     NOT NULL UNIQUE
);
COMMENT ON COLUMN serie.nivel IS 'Define a cobrança (RN01): EDUCACAO_INFANTIL = Taxa de Associado; ENSINO_FUNDAMENTAL = Mensalidade.';

CREATE TABLE turma (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    serie_id        uuid        NOT NULL REFERENCES serie(id),
    ano_letivo_id   uuid        NOT NULL REFERENCES ano_letivo(id),
    nome            varchar(50) NOT NULL,
    capacidade_max  integer     NOT NULL DEFAULT 30,
    criado_em       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_turma UNIQUE (ano_letivo_id, serie_id, nome),
    CONSTRAINT ck_turma_capacidade CHECK (capacidade_max BETWEEN 1 AND 30)  -- RN04
);

CREATE TABLE tabela_preco (
    id                    uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    serie_id              uuid          NOT NULL REFERENCES serie(id),
    ano_letivo_id         uuid          NOT NULL REFERENCES ano_letivo(id),
    valor_matricula       numeric(10,2) NOT NULL DEFAULT 0,
    valor_mensal          numeric(10,2) NOT NULL,
    dia_vencimento        smallint      NOT NULL DEFAULT 10,
    dias_prazo_matricula  smallint      NOT NULL DEFAULT 7,
    CONSTRAINT uq_tabela_preco UNIQUE (serie_id, ano_letivo_id),
    CONSTRAINT ck_preco_valores CHECK (valor_matricula >= 0 AND valor_mensal > 0),
    CONSTRAINT ck_preco_dia     CHECK (dia_vencimento BETWEEN 1 AND 28)
);
COMMENT ON TABLE tabela_preco IS 'Valores por série e ano letivo. Base para gerar matrícula, Taxa de Associado e Mensalidade.';

/* ---- 2.3 Matrícula -------------------------------------------------- */

CREATE TABLE pre_matricula (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    ano_letivo_id       uuid         NOT NULL REFERENCES ano_letivo(id),
    serie_id            uuid         NOT NULL REFERENCES serie(id),
    responsavel_id      uuid         NOT NULL REFERENCES responsavel(id),
    parentesco          varchar(50),
    aluno_nome          varchar(150) NOT NULL,
    aluno_nascimento    date         NOT NULL,
    aluno_cpf           char(11),
    farda_uniforme_tam  varchar(10),
    farda_ed_fisica_tam varchar(10),
    status              status_pre_matricula NOT NULL DEFAULT 'PENDENTE',
    motivo_rejeicao     text,
    analisado_por       uuid         REFERENCES funcionario(id),
    analisado_em        timestamptz,
    aluno_id            uuid         REFERENCES aluno(id),
    criado_em           timestamptz  NOT NULL DEFAULT now(),
    atualizado_em       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_pre_cpf CHECK (aluno_cpf IS NULL OR aluno_cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_pre_rejeicao CHECK (status <> 'REJEITADA' OR motivo_rejeicao IS NOT NULL),
    CONSTRAINT ck_pre_analise  CHECK (status = 'PENDENTE' OR (analisado_por IS NOT NULL AND analisado_em IS NOT NULL)),
    CONSTRAINT ck_pre_aprovada CHECK (status <> 'APROVADA' OR aluno_id IS NOT NULL)
);
COMMENT ON TABLE pre_matricula IS 'RF01.1/01.2/01.3: preenchida online pelo responsável (inclui tamanhos de farda); aprovada ou rejeitada pela secretaria.';

CREATE TABLE matricula (
    id                   uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    aluno_id             uuid            NOT NULL REFERENCES aluno(id),
    turma_id             uuid            NOT NULL REFERENCES turma(id),
    ano_letivo_id        uuid            NOT NULL REFERENCES ano_letivo(id),
    pre_matricula_id     uuid            UNIQUE REFERENCES pre_matricula(id),
    tipo                 tipo_matricula  NOT NULL DEFAULT 'NOVA',
    status               status_matricula NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    data_matricula       date            NOT NULL DEFAULT current_date,
    ativada_em           timestamptz,
    motivo_cancelamento  text,
    criado_em            timestamptz     NOT NULL DEFAULT now(),
    atualizado_em        timestamptz     NOT NULL DEFAULT now(),
    CONSTRAINT uq_matricula_aluno_ano UNIQUE (aluno_id, ano_letivo_id)  -- RN03
);
COMMENT ON TABLE matricula IS 'Vínculo aluno-turma-ano. Tipo (NOVA/REMATRICULA) e ano são preenchidos por trigger.';

CREATE TABLE farda_matricula (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id  uuid        NOT NULL REFERENCES matricula(id) ON DELETE CASCADE,
    tipo          tipo_farda  NOT NULL,
    tamanho       varchar(10) NOT NULL,
    CONSTRAINT uq_farda UNIQUE (matricula_id, tipo)  -- RF01.3: uniforme e ed. física separados
);

CREATE TABLE contrato (
    id             uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id   uuid            NOT NULL REFERENCES matricula(id) ON DELETE CASCADE,
    tipo           tipo_contrato   NOT NULL,
    status         status_contrato NOT NULL DEFAULT 'PENDENTE',
    assinado_por   uuid            REFERENCES responsavel(id),
    assinado_em    timestamptz,
    arquivo_url    text,
    criado_em      timestamptz     NOT NULL DEFAULT now(),
    CONSTRAINT uq_contrato UNIQUE (matricula_id, tipo),
    CONSTRAINT ck_contrato_assinatura CHECK (status <> 'ASSINADO' OR (assinado_por IS NOT NULL AND assinado_em IS NOT NULL))
);
COMMENT ON TABLE contrato IS 'RF01.4: Contrato de Matrícula (Fundamental) ou Ficha de Sócio (Infantil), gerado automaticamente.';

CREATE TABLE documento_aluno (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    aluno_id     uuid        NOT NULL REFERENCES aluno(id) ON DELETE CASCADE,
    tipo         varchar(60) NOT NULL,
    arquivo_url  text        NOT NULL,
    enviado_em   timestamptz NOT NULL DEFAULT now()
);

/* ---- 2.4 Bolsas e financeiro --------------------------------------- */

CREATE TABLE bolsa (
    id              uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            varchar(100)  NOT NULL UNIQUE,
    descricao       text,
    tipo_desconto   tipo_desconto NOT NULL,
    valor           numeric(10,2) NOT NULL,
    ativa           boolean       NOT NULL DEFAULT true,
    criado_em       timestamptz   NOT NULL DEFAULT now(),
    atualizado_em   timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT ck_bolsa_valor CHECK (valor > 0 AND (tipo_desconto <> 'PERCENTUAL' OR valor <= 100))
);

CREATE TABLE bolsa_matricula (
    id               uuid  PRIMARY KEY DEFAULT gen_random_uuid(),
    bolsa_id         uuid  NOT NULL REFERENCES bolsa(id),
    matricula_id     uuid  NOT NULL REFERENCES matricula(id) ON DELETE CASCADE,
    vigencia_inicio  date  NOT NULL DEFAULT current_date,
    vigencia_fim     date,
    concedida_por    uuid  REFERENCES funcionario(id),
    CONSTRAINT uq_bolsa_matricula UNIQUE (bolsa_id, matricula_id, vigencia_inicio),
    CONSTRAINT ck_bolsa_vigencia CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
);

CREATE TABLE cobranca (
    id                    uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id          uuid            NOT NULL REFERENCES matricula(id),
    responsavel_id        uuid            NOT NULL REFERENCES responsavel(id),
    tipo                  tipo_cobranca   NOT NULL,
    competencia           date            NOT NULL,
    valor_bruto           numeric(10,2)   NOT NULL,
    desconto_bolsa        numeric(10,2)   NOT NULL DEFAULT 0,
    valor_final           numeric(10,2)   NOT NULL,
    vencimento            date            NOT NULL,
    status                status_cobranca NOT NULL DEFAULT 'PENDENTE',
    cora_id               varchar(100)    UNIQUE,
    cora_url              text,
    cora_sincronizado_em  timestamptz,
    pago_em               timestamptz,
    valor_pago            numeric(10,2),
    origem_pagamento      origem_pagamento,
    criado_em             timestamptz     NOT NULL DEFAULT now(),
    atualizado_em         timestamptz     NOT NULL DEFAULT now(),
    CONSTRAINT ck_cobranca_competencia CHECK (EXTRACT(day FROM competencia) = 1),
    CONSTRAINT ck_cobranca_valores CHECK (valor_bruto >= 0 AND desconto_bolsa >= 0 AND desconto_bolsa <= valor_bruto
                                          AND valor_final = valor_bruto - desconto_bolsa),
    CONSTRAINT ck_cobranca_pagamento CHECK (status <> 'PAGO' OR (pago_em IS NOT NULL AND origem_pagamento IS NOT NULL AND valor_pago IS NOT NULL))
);
COMMENT ON TABLE cobranca IS 'RF04: integração Cora (cora_id/cora_url). RN01: uma cobrança mensal por matrícula/competência. Pagamento via SITE ou PRESENCIAL.';

CREATE TABLE notificacao_cobranca (
    id              uuid                PRIMARY KEY DEFAULT gen_random_uuid(),
    cobranca_id     uuid                NOT NULL REFERENCES cobranca(id) ON DELETE CASCADE,
    responsavel_id  uuid                NOT NULL REFERENCES responsavel(id),
    tipo            tipo_notificacao    NOT NULL,
    canal           canal_notificacao   NOT NULL DEFAULT 'EMAIL',
    status          status_notificacao  NOT NULL DEFAULT 'PENDENTE',
    data_ref        date                NOT NULL DEFAULT current_date,
    criada_em       timestamptz         NOT NULL DEFAULT now(),
    enviada_em      timestamptz,
    CONSTRAINT uq_notificacao_dia UNIQUE (cobranca_id, tipo, canal, data_ref)
);
COMMENT ON TABLE notificacao_cobranca IS 'RF05: esteira de notificações. O envio real (e-mail/SMS/WhatsApp) é feito pela aplicação, que consome as linhas PENDENTE.';

CREATE TABLE alerta_secretaria (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo       varchar(40) NOT NULL,
    turma_id   uuid        REFERENCES turma(id) ON DELETE CASCADE,
    mensagem   text        NOT NULL,
    lido       boolean     NOT NULL DEFAULT false,
    criado_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_alerta_tipo CHECK (tipo IN ('TURMA_LOTADA'))
);
COMMENT ON TABLE alerta_secretaria IS 'RF03: sinalização à secretaria quando a turma atinge o limite.';

/* ---- 2.5 Lista de materiais ---------------------------------------- */

CREATE TABLE lista_material (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    serie_id       uuid NOT NULL REFERENCES serie(id),
    ano_letivo_id  uuid NOT NULL REFERENCES ano_letivo(id),
    CONSTRAINT uq_lista_material UNIQUE (serie_id, ano_letivo_id)
);

CREATE TABLE item_material (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    lista_id    uuid         NOT NULL REFERENCES lista_material(id) ON DELETE CASCADE,
    descricao   varchar(200) NOT NULL,
    quantidade  integer      NOT NULL DEFAULT 1 CHECK (quantidade > 0),
    CONSTRAINT uq_item_material UNIQUE (lista_id, descricao)
);

/* ---- 2.6 Pedagógico ------------------------------------------------- */

CREATE TABLE disciplina (
    id    uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome  varchar(80)  NOT NULL UNIQUE
);

CREATE TABLE turma_professor (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    turma_id        uuid NOT NULL REFERENCES turma(id)       ON DELETE CASCADE,
    funcionario_id  uuid NOT NULL REFERENCES funcionario(id),
    disciplina_id   uuid REFERENCES disciplina(id)
);
COMMENT ON TABLE turma_professor IS 'Define quais turmas cada professor pode acessar. disciplina_id nulo = professor polivalente.';

CREATE TABLE diario_classe (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    turma_id        uuid        NOT NULL REFERENCES turma(id),
    funcionario_id  uuid        NOT NULL REFERENCES funcionario(id),
    data_aula       date        NOT NULL,
    conteudo        text,
    criado_em       timestamptz NOT NULL DEFAULT now(),
    atualizado_em   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_diario UNIQUE (turma_id, funcionario_id, data_aula)
);

CREATE TABLE frequencia (
    diario_id     uuid    NOT NULL REFERENCES diario_classe(id) ON DELETE CASCADE,
    matricula_id  uuid    NOT NULL REFERENCES matricula(id),
    presente      boolean NOT NULL DEFAULT true,
    justificativa varchar(255),
    PRIMARY KEY (diario_id, matricula_id)
);

CREATE TABLE plano_aula (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    turma_id        uuid        NOT NULL REFERENCES turma(id),
    funcionario_id  uuid        NOT NULL REFERENCES funcionario(id),
    tipo            tipo_plano  NOT NULL,
    data_prevista   date        NOT NULL,
    objetivo        varchar(255),
    conteudo        text,
    criado_em       timestamptz NOT NULL DEFAULT now(),
    atualizado_em   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE calendario_letivo (
    id             uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    ano_letivo_id  uuid            NOT NULL REFERENCES ano_letivo(id) ON DELETE CASCADE,
    data           date            NOT NULL,
    tipo           tipo_calendario NOT NULL,
    descricao      varchar(200),
    CONSTRAINT uq_calendario UNIQUE (ano_letivo_id, data, tipo)
);

CREATE TABLE formacao_continuada (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    funcionario_id   uuid         NOT NULL REFERENCES funcionario(id),
    curso            varchar(200) NOT NULL,
    carga_horaria    integer      NOT NULL CHECK (carga_horaria > 0),
    concluida_em     date,
    certificado_url  text
);

CREATE TABLE nota (
    id            uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id  uuid          NOT NULL REFERENCES matricula(id),
    disciplina_id uuid          NOT NULL REFERENCES disciplina(id),
    bimestre      smallint      NOT NULL CHECK (bimestre BETWEEN 1 AND 4),
    valor         numeric(4,2)  NOT NULL CHECK (valor BETWEEN 0 AND 10),
    CONSTRAINT uq_nota UNIQUE (matricula_id, disciplina_id, bimestre)
);
COMMENT ON TABLE nota IS 'Não há RF dedicado, mas o RF10 exibe notas no portal. Remova se estiver fora do escopo.';

/* ---- 2.7 Obrigações institucionais --------------------------------- */

CREATE TABLE obrigacao_institucional (
    id                      uuid              PRIMARY KEY DEFAULT gen_random_uuid(),
    responsavel_id          uuid              REFERENCES funcionario(id),
    orgao                   orgao_externo     NOT NULL,
    descricao               varchar(255)      NOT NULL,
    periodicidade_meses     integer           CHECK (periodicidade_meses IS NULL OR periodicidade_meses > 0),
    proximo_vencimento      date              NOT NULL,
    dias_alerta_antecedencia integer          NOT NULL DEFAULT 30 CHECK (dias_alerta_antecedencia >= 0),
    ultimo_cumprimento      date,
    status                  status_obrigacao  NOT NULL DEFAULT 'PENDENTE',
    criado_em               timestamptz       NOT NULL DEFAULT now(),
    atualizado_em           timestamptz       NOT NULL DEFAULT now()
);
COMMENT ON TABLE obrigacao_institucional IS 'RF09: Censo/INEP (junho), SMTT (março), Sistema Presença (a cada 2 meses) e Cora.';

CREATE TABLE documento_obrigacao (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    obrigacao_id uuid        NOT NULL REFERENCES obrigacao_institucional(id) ON DELETE CASCADE,
    arquivo_url  text        NOT NULL,
    enviado_em   timestamptz NOT NULL DEFAULT now()
);

/* ---- 2.8 Controle de acesso por perfil ------------------------------ */

CREATE TABLE perfil_permissao (
    perfil     perfil_usuario NOT NULL,
    modulo     varchar(40)    NOT NULL,
    pode_ler   boolean        NOT NULL DEFAULT true,
    pode_gravar boolean       NOT NULL DEFAULT false,
    PRIMARY KEY (perfil, modulo)
);
COMMENT ON TABLE perfil_permissao IS 'Matriz perfil x módulo (seção 2 dos requisitos). A aplicação consulta esta tabela para autorizar.';


/* ---------------------------------------------------------------------
   3. ÍNDICES E UNICIDADES PARCIAIS
   --------------------------------------------------------------------- */

-- Um único responsável financeiro por aluno
CREATE UNIQUE INDEX uq_aluno_resp_financeiro
    ON aluno_responsavel (aluno_id) WHERE resp_financeiro;

-- RN01: uma única cobrança mensal (Taxa de Associado OU Mensalidade) por matrícula/competência
CREATE UNIQUE INDEX uq_cobranca_mensal
    ON cobranca (matricula_id, competencia) WHERE tipo IN ('TAXA_ASSOCIADO','MENSALIDADE');

-- Uma única cobrança de matrícula por matrícula
CREATE UNIQUE INDEX uq_cobranca_matricula
    ON cobranca (matricula_id) WHERE tipo = 'MATRICULA';

-- Evita duplicar professor/turma/disciplina (disciplina nula conta como um valor)
CREATE UNIQUE INDEX uq_turma_professor
    ON turma_professor (turma_id, funcionario_id, COALESCE(disciplina_id, '00000000-0000-0000-0000-000000000000'::uuid));

-- Índices de consulta
CREATE INDEX ix_aluno_resp_responsavel ON aluno_responsavel (responsavel_id);
CREATE INDEX ix_pre_matricula_status   ON pre_matricula (status);
CREATE INDEX ix_pre_matricula_resp     ON pre_matricula (responsavel_id);
CREATE INDEX ix_matricula_turma_status ON matricula (turma_id, status);
CREATE INDEX ix_matricula_aluno        ON matricula (aluno_id);
CREATE INDEX ix_cobranca_status_venc   ON cobranca (status, vencimento);
CREATE INDEX ix_cobranca_responsavel   ON cobranca (responsavel_id);
CREATE INDEX ix_cobranca_matricula     ON cobranca (matricula_id);
CREATE INDEX ix_notificacao_status     ON notificacao_cobranca (status);
CREATE INDEX ix_frequencia_matricula   ON frequencia (matricula_id);
CREATE INDEX ix_nota_matricula         ON nota (matricula_id);
CREATE INDEX ix_diario_turma_data      ON diario_classe (turma_id, data_aula);
CREATE INDEX ix_obrigacao_vencimento   ON obrigacao_institucional (proximo_vencimento) WHERE status <> 'CUMPRIDA';
CREATE INDEX ix_bolsa_matricula_mat    ON bolsa_matricula (matricula_id);


/* ---------------------------------------------------------------------
   4. FUNÇÕES DE APOIO E TRIGGERS (REGRAS DE NEGÓCIO)
   --------------------------------------------------------------------- */

/* ---- 4.1 Atualização automática de atualizado_em -------------------- */
CREATE OR REPLACE FUNCTION fn_set_atualizado_em() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.atualizado_em := now();
    RETURN NEW;
END $$;

DO $$
DECLARE t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['usuario','responsavel','aluno','funcionario','pre_matricula','matricula',
                             'bolsa','cobranca','diario_classe','plano_aula','obrigacao_institucional']
    LOOP
        EXECUTE format('CREATE TRIGGER trg_%s_atualizado BEFORE UPDATE ON %I
                        FOR EACH ROW EXECUTE FUNCTION fn_set_atualizado_em()', t, t);
    END LOOP;
END $$;

/* ---- 4.2 Perfil do usuário compatível com a entidade vinculada ------- */
CREATE OR REPLACE FUNCTION fn_valida_perfil_usuario() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_perfil perfil_usuario;
BEGIN
    IF NEW.usuario_id IS NULL THEN RETURN NEW; END IF;
    SELECT perfil INTO v_perfil FROM usuario WHERE id = NEW.usuario_id;

    IF TG_TABLE_NAME = 'responsavel' AND v_perfil <> 'RESPONSAVEL' THEN
        RAISE EXCEPTION 'Usuário vinculado a responsável deve ter perfil RESPONSAVEL (atual: %)', v_perfil;
    ELSIF TG_TABLE_NAME = 'aluno' AND v_perfil <> 'ALUNO' THEN
        RAISE EXCEPTION 'Usuário vinculado a aluno deve ter perfil ALUNO (atual: %)', v_perfil;
    ELSIF TG_TABLE_NAME = 'funcionario' AND v_perfil NOT IN ('ADMIN','SECRETARIA','FINANCEIRO','PROFESSOR') THEN
        RAISE EXCEPTION 'Usuário vinculado a funcionário deve ter perfil ADMIN, SECRETARIA, FINANCEIRO ou PROFESSOR (atual: %)', v_perfil;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_responsavel_perfil BEFORE INSERT OR UPDATE OF usuario_id ON responsavel
    FOR EACH ROW EXECUTE FUNCTION fn_valida_perfil_usuario();
CREATE TRIGGER trg_aluno_perfil BEFORE INSERT OR UPDATE OF usuario_id ON aluno
    FOR EACH ROW EXECUTE FUNCTION fn_valida_perfil_usuario();
CREATE TRIGGER trg_funcionario_perfil BEFORE INSERT OR UPDATE OF usuario_id ON funcionario
    FOR EACH ROW EXECUTE FUNCTION fn_valida_perfil_usuario();

/* ---- 4.3 Capacidade da turma (RF03 / RN04) --------------------------- */

-- Não permite reduzir a capacidade abaixo do número de alunos já matriculados
CREATE OR REPLACE FUNCTION fn_turma_capacidade() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_ocupadas integer;
BEGIN
    SELECT count(*) INTO v_ocupadas FROM matricula
     WHERE turma_id = NEW.id AND status IN ('AGUARDANDO_PAGAMENTO','ATIVA');
    IF NEW.capacidade_max < v_ocupadas THEN
        RAISE EXCEPTION 'Turma já possui % alunos; capacidade não pode ser menor', v_ocupadas;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_turma_capacidade BEFORE UPDATE OF capacidade_max ON turma
    FOR EACH ROW EXECUTE FUNCTION fn_turma_capacidade();

/* ---- 4.4 Matrícula: ano, tipo, capacidade, datas --------------------- */
CREATE OR REPLACE FUNCTION fn_matricula_before() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_turma    turma%ROWTYPE;
    v_ocupadas integer;
BEGIN
    -- trava a turma para evitar corrida entre duas matrículas simultâneas
    SELECT * INTO v_turma FROM turma WHERE id = NEW.turma_id FOR UPDATE;

    -- ano letivo vem da turma; se informado, deve coincidir
    IF NEW.ano_letivo_id IS NULL THEN
        NEW.ano_letivo_id := v_turma.ano_letivo_id;
    ELSIF NEW.ano_letivo_id <> v_turma.ano_letivo_id THEN
        RAISE EXCEPTION 'A turma informada pertence a outro ano letivo';
    END IF;

    -- RF01.5 / RN03: tipo definido pelo histórico do aluno
    IF TG_OP = 'INSERT' THEN
        IF EXISTS (SELECT 1 FROM matricula m
                    WHERE m.aluno_id = NEW.aluno_id AND m.ano_letivo_id <> NEW.ano_letivo_id) THEN
            NEW.tipo := 'REMATRICULA';
        ELSE
            NEW.tipo := 'NOVA';
        END IF;
    END IF;

    -- RN04: limite de alunos por turma
    IF NEW.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')
       AND (TG_OP = 'INSERT'
            OR OLD.turma_id IS DISTINCT FROM NEW.turma_id
            OR OLD.status NOT IN ('AGUARDANDO_PAGAMENTO','ATIVA')) THEN
        SELECT count(*) INTO v_ocupadas FROM matricula
         WHERE turma_id = NEW.turma_id
           AND status IN ('AGUARDANDO_PAGAMENTO','ATIVA')
           AND id <> NEW.id;
        IF v_ocupadas >= v_turma.capacidade_max THEN
            RAISE EXCEPTION 'Turma "%" lotada: limite de % alunos atingido', v_turma.nome, v_turma.capacidade_max;
        END IF;
    END IF;

    -- datas de controle
    IF NEW.status = 'ATIVA' AND (TG_OP = 'INSERT' OR OLD.status <> 'ATIVA') THEN
        NEW.ativada_em := now();
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_matricula_before BEFORE INSERT OR UPDATE ON matricula
    FOR EACH ROW EXECUTE FUNCTION fn_matricula_before();

-- Ao matricular: gera contrato (RF01.4) e cobrança de matrícula (RF04)
CREATE OR REPLACE FUNCTION fn_matricula_after_insert() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_nivel nivel_ensino;
BEGIN
    SELECT s.nivel INTO v_nivel
      FROM turma t JOIN serie s ON s.id = t.serie_id
     WHERE t.id = NEW.turma_id;

    INSERT INTO contrato (matricula_id, tipo)
    VALUES (NEW.id,
            CASE WHEN v_nivel = 'EDUCACAO_INFANTIL'
                 THEN 'FICHA_SOCIO'::tipo_contrato
                 ELSE 'CONTRATO_MATRICULA'::tipo_contrato END);

    IF NEW.status = 'AGUARDANDO_PAGAMENTO' THEN
        PERFORM fn_gerar_cobranca_matricula(NEW.id);
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_matricula_after_insert AFTER INSERT ON matricula
    FOR EACH ROW EXECUTE FUNCTION fn_matricula_after_insert();

-- RF03: sinaliza a secretaria quando a turma lota
CREATE OR REPLACE FUNCTION fn_matricula_alerta_lotacao() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_turma    turma%ROWTYPE;
    v_ocupadas integer;
BEGIN
    SELECT * INTO v_turma FROM turma WHERE id = NEW.turma_id;
    SELECT count(*) INTO v_ocupadas FROM matricula
     WHERE turma_id = NEW.turma_id AND status IN ('AGUARDANDO_PAGAMENTO','ATIVA');

    IF v_ocupadas >= v_turma.capacidade_max
       AND NOT EXISTS (SELECT 1 FROM alerta_secretaria
                        WHERE turma_id = NEW.turma_id AND tipo = 'TURMA_LOTADA' AND NOT lido) THEN
        INSERT INTO alerta_secretaria (tipo, turma_id, mensagem)
        VALUES ('TURMA_LOTADA', NEW.turma_id,
                format('A turma "%s" atingiu o limite de %s alunos.', v_turma.nome, v_turma.capacidade_max));
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER trg_matricula_alerta AFTER INSERT OR UPDATE OF turma_id, status ON matricula
    FOR EACH ROW EXECUTE FUNCTION fn_matricula_alerta_lotacao();

/* ---- 4.5 Contrato: só responsável do aluno pode assinar (RN02) ------- */
CREATE OR REPLACE FUNCTION fn_valida_assinante() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.assinado_por IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM matricula m
          JOIN aluno_responsavel ar ON ar.aluno_id = m.aluno_id
         WHERE m.id = NEW.matricula_id AND ar.responsavel_id = NEW.assinado_por) THEN
        RAISE EXCEPTION 'O assinante não é responsável vinculado a este aluno';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_contrato_assinante BEFORE INSERT OR UPDATE OF assinado_por ON contrato
    FOR EACH ROW EXECUTE FUNCTION fn_valida_assinante();

/* ---- 4.6 Pré-matrícula: decisão é definitiva ------------------------- */
CREATE OR REPLACE FUNCTION fn_pre_matricula_decisao() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status <> 'PENDENTE' AND NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'Pré-matrícula já foi % e não pode mudar de status', OLD.status;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_pre_matricula_decisao BEFORE UPDATE ON pre_matricula
    FOR EACH ROW EXECUTE FUNCTION fn_pre_matricula_decisao();

/* ---- 4.7 Bolsas (RF02) ----------------------------------------------- */

-- Desconto total aplicável a uma competência (bolsas vigentes no mês, limitado ao valor bruto)
CREATE OR REPLACE FUNCTION fn_desconto_bolsa(p_matricula_id uuid, p_competencia date, p_valor_bruto numeric)
RETURNS numeric LANGUAGE plpgsql STABLE AS $$
DECLARE v_desc numeric;
BEGIN
    SELECT COALESCE(SUM(CASE b.tipo_desconto
                          WHEN 'PERCENTUAL' THEN p_valor_bruto * b.valor / 100
                          ELSE b.valor END), 0)
      INTO v_desc
      FROM bolsa_matricula bm
      JOIN bolsa b ON b.id = bm.bolsa_id
     WHERE bm.matricula_id = p_matricula_id
       AND b.ativa
       AND bm.vigencia_inicio <= (date_trunc('month', p_competencia) + interval '1 month - 1 day')::date
       AND (bm.vigencia_fim IS NULL OR bm.vigencia_fim >= p_competencia);

    RETURN LEAST(round(v_desc, 2), p_valor_bruto);
END $$;

-- Recalcula cobranças mensais ainda em aberto quando a bolsa muda
CREATE OR REPLACE FUNCTION fn_recalcular_cobrancas_abertas(p_matricula_id uuid) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE cobranca c
       SET desconto_bolsa = d.desconto,
           valor_final    = c.valor_bruto - d.desconto
      FROM (SELECT x.id, fn_desconto_bolsa(x.matricula_id, x.competencia, x.valor_bruto) AS desconto
              FROM cobranca x
             WHERE x.matricula_id = p_matricula_id
               AND x.status IN ('PENDENTE','VENCIDO')
               AND x.tipo IN ('TAXA_ASSOCIADO','MENSALIDADE')) d
     WHERE c.id = d.id;
END $$;

CREATE OR REPLACE FUNCTION fn_bolsa_matricula_recalc() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM fn_recalcular_cobrancas_abertas(OLD.matricula_id);
        RETURN OLD;
    END IF;
    PERFORM fn_recalcular_cobrancas_abertas(NEW.matricula_id);
    RETURN NEW;
END $$;

CREATE TRIGGER trg_bolsa_matricula_recalc AFTER INSERT OR UPDATE OR DELETE ON bolsa_matricula
    FOR EACH ROW EXECUTE FUNCTION fn_bolsa_matricula_recalc();

CREATE OR REPLACE FUNCTION fn_bolsa_alterada() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE r record;
BEGIN
    FOR r IN SELECT DISTINCT matricula_id FROM bolsa_matricula WHERE bolsa_id = NEW.id LOOP
        PERFORM fn_recalcular_cobrancas_abertas(r.matricula_id);
    END LOOP;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_bolsa_alterada AFTER UPDATE OF valor, tipo_desconto, ativa ON bolsa
    FOR EACH ROW EXECUTE FUNCTION fn_bolsa_alterada();

/* ---- 4.8 Cobrança: RN01, desconto de bolsa e pagamento --------------- */
CREATE OR REPLACE FUNCTION fn_cobranca_before_insert() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE v_nivel nivel_ensino;
BEGIN
    SELECT s.nivel INTO v_nivel
      FROM matricula m
      JOIN turma t ON t.id = m.turma_id
      JOIN serie s ON s.id = t.serie_id
     WHERE m.id = NEW.matricula_id;

    -- RN01: cada nível tem o seu tipo de cobrança mensal
    IF NEW.tipo = 'TAXA_ASSOCIADO' AND v_nivel <> 'EDUCACAO_INFANTIL' THEN
        RAISE EXCEPTION 'Taxa de Associado só pode ser cobrada na Educação Infantil';
    ELSIF NEW.tipo = 'MENSALIDADE' AND v_nivel <> 'ENSINO_FUNDAMENTAL' THEN
        RAISE EXCEPTION 'Boleto de Mensalidade só pode ser cobrado no Ensino Fundamental';
    END IF;

    -- o responsável da cobrança deve estar vinculado ao aluno
    IF NOT EXISTS (SELECT 1 FROM matricula m
                     JOIN aluno_responsavel ar ON ar.aluno_id = m.aluno_id
                    WHERE m.id = NEW.matricula_id AND ar.responsavel_id = NEW.responsavel_id) THEN
        RAISE EXCEPTION 'Responsável da cobrança não está vinculado ao aluno';
    END IF;

    -- RF02: bolsa reflete automaticamente nas cobranças mensais
    IF NEW.tipo IN ('TAXA_ASSOCIADO','MENSALIDADE') THEN
        NEW.desconto_bolsa := fn_desconto_bolsa(NEW.matricula_id, NEW.competencia, NEW.valor_bruto);
    ELSE
        NEW.desconto_bolsa := 0;
    END IF;
    NEW.valor_final := NEW.valor_bruto - NEW.desconto_bolsa;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_cobranca_before_insert BEFORE INSERT ON cobranca
    FOR EACH ROW EXECUTE FUNCTION fn_cobranca_before_insert();

CREATE OR REPLACE FUNCTION fn_cobranca_before_update() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    -- cobrança paga é imutável
    IF OLD.status = 'PAGO' THEN
        IF NEW.status <> 'PAGO'
           OR NEW.valor_bruto IS DISTINCT FROM OLD.valor_bruto
           OR NEW.valor_final IS DISTINCT FROM OLD.valor_final
           OR NEW.matricula_id <> OLD.matricula_id THEN
            RAISE EXCEPTION 'Cobrança paga não pode ser alterada';
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.status = 'PAGO' THEN
        IF NEW.origem_pagamento IS NULL THEN
            RAISE EXCEPTION 'Informe a origem do pagamento (SITE ou PRESENCIAL)';
        END IF;
        NEW.pago_em    := COALESCE(NEW.pago_em, now());
        NEW.valor_pago := COALESCE(NEW.valor_pago, NEW.valor_final);
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_cobranca_before_update BEFORE UPDATE ON cobranca
    FOR EACH ROW EXECUTE FUNCTION fn_cobranca_before_update();

CREATE OR REPLACE FUNCTION fn_cobranca_before_delete() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'PAGO' THEN
        RAISE EXCEPTION 'Cobrança paga não pode ser excluída';
    END IF;
    RETURN OLD;
END $$;

CREATE TRIGGER trg_cobranca_before_delete BEFORE DELETE ON cobranca
    FOR EACH ROW EXECUTE FUNCTION fn_cobranca_before_delete();

-- Pagamento da matrícula (site ou presencial) ativa a matrícula
CREATE OR REPLACE FUNCTION fn_cobranca_after_update() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.tipo = 'MATRICULA' AND NEW.status = 'PAGO' AND OLD.status <> 'PAGO' THEN
        UPDATE matricula SET status = 'ATIVA'
         WHERE id = NEW.matricula_id AND status = 'AGUARDANDO_PAGAMENTO';
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER trg_cobranca_after_update AFTER UPDATE OF status ON cobranca
    FOR EACH ROW EXECUTE FUNCTION fn_cobranca_after_update();

/* ---- 4.9 Pedagógico: professor só atua nas suas turmas --------------- */
CREATE OR REPLACE FUNCTION fn_turma_professor_valida() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM funcionario WHERE id = NEW.funcionario_id AND tipo = 'PROFESSOR') THEN
        RAISE EXCEPTION 'Somente funcionários do tipo PROFESSOR podem ser vinculados a turmas';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_turma_professor_valida BEFORE INSERT OR UPDATE ON turma_professor
    FOR EACH ROW EXECUTE FUNCTION fn_turma_professor_valida();

CREATE OR REPLACE FUNCTION fn_valida_professor_turma() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM turma_professor
                    WHERE turma_id = NEW.turma_id AND funcionario_id = NEW.funcionario_id) THEN
        RAISE EXCEPTION 'Professor não está vinculado a esta turma';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_diario_professor BEFORE INSERT OR UPDATE OF turma_id, funcionario_id ON diario_classe
    FOR EACH ROW EXECUTE FUNCTION fn_valida_professor_turma();
CREATE TRIGGER trg_plano_professor BEFORE INSERT OR UPDATE OF turma_id, funcionario_id ON plano_aula
    FOR EACH ROW EXECUTE FUNCTION fn_valida_professor_turma();

-- Frequência: aluno precisa estar matriculado (ativo) na turma do diário
CREATE OR REPLACE FUNCTION fn_frequencia_valida() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM diario_classe d
                     JOIN matricula m ON m.turma_id = d.turma_id
                    WHERE d.id = NEW.diario_id AND m.id = NEW.matricula_id AND m.status = 'ATIVA') THEN
        RAISE EXCEPTION 'Matrícula não está ativa na turma deste diário';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_frequencia_valida BEFORE INSERT OR UPDATE OF diario_id, matricula_id ON frequencia
    FOR EACH ROW EXECUTE FUNCTION fn_frequencia_valida();

-- Calendário: data deve estar dentro do ano letivo
CREATE OR REPLACE FUNCTION fn_calendario_valida() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ano_letivo
                    WHERE id = NEW.ano_letivo_id AND NEW.data BETWEEN data_inicio AND data_fim) THEN
        RAISE EXCEPTION 'Data % fora do período do ano letivo', NEW.data;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_calendario_valida BEFORE INSERT OR UPDATE OF data, ano_letivo_id ON calendario_letivo
    FOR EACH ROW EXECUTE FUNCTION fn_calendario_valida();


/* ---------------------------------------------------------------------
   5. FUNÇÕES DE ROTINA (FLUXOS DO SISTEMA)
   --------------------------------------------------------------------- */

/* ---- 5.1 Apoio ------------------------------------------------------- */

-- Responsável financeiro do aluno (erro se não houver)
CREATE OR REPLACE FUNCTION fn_responsavel_financeiro(p_aluno_id uuid) RETURNS uuid
LANGUAGE plpgsql STABLE AS $$
DECLARE v_id uuid;
BEGIN
    SELECT responsavel_id INTO v_id
      FROM aluno_responsavel WHERE aluno_id = p_aluno_id AND resp_financeiro;
    IF v_id IS NULL THEN
        RAISE EXCEPTION 'Aluno % não possui responsável financeiro definido', p_aluno_id;
    END IF;
    RETURN v_id;
END $$;

-- Próxima data (a partir de hoje) para um dia/mês fixo do ano
CREATE OR REPLACE FUNCTION fn_proxima_ocorrencia(p_mes integer, p_dia integer) RETURNS date
LANGUAGE plpgsql STABLE AS $$
DECLARE
    v_ano  integer := extract(year FROM current_date)::integer;
    v_data date    := make_date(v_ano, p_mes, p_dia);
BEGIN
    IF v_data < current_date THEN
        v_data := make_date(v_ano + 1, p_mes, p_dia);
    END IF;
    RETURN v_data;
END $$;

/* ---- 5.2 Cobrança da matrícula (RF04) -------------------------------- */
CREATE OR REPLACE FUNCTION fn_gerar_cobranca_matricula(p_matricula_id uuid) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE
    v_mat    matricula%ROWTYPE;
    v_serie  uuid;
    v_preco  tabela_preco%ROWTYPE;
    v_id     uuid;
BEGIN
    SELECT * INTO v_mat FROM matricula WHERE id = p_matricula_id;
    SELECT serie_id INTO v_serie FROM turma WHERE id = v_mat.turma_id;

    SELECT * INTO v_preco FROM tabela_preco
     WHERE serie_id = v_serie AND ano_letivo_id = v_mat.ano_letivo_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Cadastre a tabela_preco da série/ano letivo antes de matricular';
    END IF;

    -- matrícula gratuita: ativa direto, sem cobrança
    IF v_preco.valor_matricula = 0 THEN
        UPDATE matricula SET status = 'ATIVA'
         WHERE id = p_matricula_id AND status = 'AGUARDANDO_PAGAMENTO';
        RETURN NULL;
    END IF;

    INSERT INTO cobranca (matricula_id, responsavel_id, tipo, competencia,
                          valor_bruto, valor_final, vencimento)
    VALUES (p_matricula_id,
            fn_responsavel_financeiro(v_mat.aluno_id),
            'MATRICULA',
            date_trunc('month', current_date)::date,
            v_preco.valor_matricula,
            v_preco.valor_matricula,
            current_date + v_preco.dias_prazo_matricula)
    RETURNING id INTO v_id;
    RETURN v_id;
END $$;

/* ---- 5.3 Pré-matrícula: aprovar / rejeitar (RF01.2) ------------------ */

-- Aprova, cria o cadastro principal do aluno, vincula o responsável,
-- matricula na turma e copia os tamanhos de farda. Retorna o id da matrícula.
CREATE OR REPLACE FUNCTION fn_aprovar_pre_matricula(
    p_pre_matricula_id uuid,
    p_funcionario_id   uuid,
    p_turma_id         uuid
) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE
    v_pre        pre_matricula%ROWTYPE;
    v_turma      turma%ROWTYPE;
    v_aluno_id   uuid;
    v_matricula  uuid;
BEGIN
    SELECT * INTO v_pre FROM pre_matricula WHERE id = p_pre_matricula_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Pré-matrícula não encontrada'; END IF;
    IF v_pre.status <> 'PENDENTE' THEN
        RAISE EXCEPTION 'Pré-matrícula já está %', v_pre.status;
    END IF;

    SELECT * INTO v_turma FROM turma WHERE id = p_turma_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Turma não encontrada'; END IF;
    IF v_turma.serie_id <> v_pre.serie_id OR v_turma.ano_letivo_id <> v_pre.ano_letivo_id THEN
        RAISE EXCEPTION 'A turma não corresponde à série/ano letivo da pré-matrícula';
    END IF;

    -- cadastro principal (RN03: único)
    INSERT INTO aluno (nome, data_nascimento, cpf)
    VALUES (v_pre.aluno_nome, v_pre.aluno_nascimento, v_pre.aluno_cpf)
    RETURNING id INTO v_aluno_id;

    -- RN02: vínculo com o responsável que preencheu (financeiro por padrão)
    INSERT INTO aluno_responsavel (aluno_id, responsavel_id, parentesco, resp_financeiro)
    VALUES (v_aluno_id, v_pre.responsavel_id, v_pre.parentesco, true);

    -- matrícula (contrato e cobrança nascem por trigger)
    INSERT INTO matricula (aluno_id, turma_id, pre_matricula_id)
    VALUES (v_aluno_id, p_turma_id, p_pre_matricula_id)
    RETURNING id INTO v_matricula;

    -- RF01.3: farda
    IF v_pre.farda_uniforme_tam IS NOT NULL THEN
        INSERT INTO farda_matricula (matricula_id, tipo, tamanho)
        VALUES (v_matricula, 'UNIFORME', v_pre.farda_uniforme_tam);
    END IF;
    IF v_pre.farda_ed_fisica_tam IS NOT NULL THEN
        INSERT INTO farda_matricula (matricula_id, tipo, tamanho)
        VALUES (v_matricula, 'EDUCACAO_FISICA', v_pre.farda_ed_fisica_tam);
    END IF;

    UPDATE pre_matricula
       SET status = 'APROVADA', analisado_por = p_funcionario_id,
           analisado_em = now(), aluno_id = v_aluno_id
     WHERE id = p_pre_matricula_id;

    RETURN v_matricula;
END $$;

CREATE OR REPLACE FUNCTION fn_rejeitar_pre_matricula(
    p_pre_matricula_id uuid,
    p_funcionario_id   uuid,
    p_motivo           text
) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF p_motivo IS NULL OR btrim(p_motivo) = '' THEN
        RAISE EXCEPTION 'Informe o motivo da rejeição';
    END IF;
    UPDATE pre_matricula
       SET status = 'REJEITADA', motivo_rejeicao = p_motivo,
           analisado_por = p_funcionario_id, analisado_em = now()
     WHERE id = p_pre_matricula_id AND status = 'PENDENTE';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pré-matrícula inexistente ou já analisada';
    END IF;
END $$;

/* ---- 5.4 Rematrícula (RF01.5 / RN03) --------------------------------- */
CREATE OR REPLACE FUNCTION fn_rematricular(
    p_aluno_id         uuid,
    p_turma_id         uuid,
    p_farda_uniforme   varchar DEFAULT NULL,
    p_farda_ed_fisica  varchar DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE
    v_ano_destino uuid;
    v_matricula   uuid;
BEGIN
    SELECT ano_letivo_id INTO v_ano_destino FROM turma WHERE id = p_turma_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Turma não encontrada'; END IF;

    IF NOT EXISTS (SELECT 1 FROM matricula
                    WHERE aluno_id = p_aluno_id AND ano_letivo_id <> v_ano_destino) THEN
        RAISE EXCEPTION 'Aluno sem matrícula anterior: use a pré-matrícula';
    END IF;

    -- o cadastro do aluno é reaproveitado; o trigger marca tipo = REMATRICULA
    INSERT INTO matricula (aluno_id, turma_id) VALUES (p_aluno_id, p_turma_id)
    RETURNING id INTO v_matricula;

    IF p_farda_uniforme IS NOT NULL THEN
        INSERT INTO farda_matricula (matricula_id, tipo, tamanho)
        VALUES (v_matricula, 'UNIFORME', p_farda_uniforme);
    END IF;
    IF p_farda_ed_fisica IS NOT NULL THEN
        INSERT INTO farda_matricula (matricula_id, tipo, tamanho)
        VALUES (v_matricula, 'EDUCACAO_FISICA', p_farda_ed_fisica);
    END IF;
    RETURN v_matricula;
END $$;

-- Encerra as matrículas ativas de um ano letivo (virada de ano)
CREATE OR REPLACE FUNCTION fn_encerrar_ano_letivo(p_ano_letivo_id uuid) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE v_qtd integer;
BEGIN
    UPDATE matricula SET status = 'ENCERRADA'
     WHERE ano_letivo_id = p_ano_letivo_id AND status = 'ATIVA';
    GET DIAGNOSTICS v_qtd = ROW_COUNT;
    RETURN v_qtd;
END $$;

/* ---- 5.5 Cobranças mensais (RF04 / RN01 / RF02) ---------------------- */

-- Gera a cobrança do mês para toda matrícula ATIVA (idempotente).
-- Infantil -> TAXA_ASSOCIADO; Fundamental -> MENSALIDADE. Bolsa aplicada por trigger.
CREATE OR REPLACE FUNCTION fn_gerar_cobrancas_mensais(p_ano integer, p_mes integer) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE v_qtd integer;
BEGIN
    IF p_mes NOT BETWEEN 1 AND 12 THEN
        RAISE EXCEPTION 'Mês inválido: %', p_mes;
    END IF;

    INSERT INTO cobranca (matricula_id, responsavel_id, tipo, competencia,
                          valor_bruto, valor_final, vencimento)
    SELECT m.id,
           (SELECT ar.responsavel_id FROM aluno_responsavel ar
             WHERE ar.aluno_id = m.aluno_id AND ar.resp_financeiro),
           CASE s.nivel WHEN 'EDUCACAO_INFANTIL' THEN 'TAXA_ASSOCIADO'::tipo_cobranca
                        ELSE 'MENSALIDADE'::tipo_cobranca END,
           make_date(p_ano, p_mes, 1),
           tp.valor_mensal,
           tp.valor_mensal,
           make_date(p_ano, p_mes, tp.dia_vencimento)
      FROM matricula m
      JOIN ano_letivo al    ON al.id = m.ano_letivo_id AND al.ano = p_ano
      JOIN turma t          ON t.id = m.turma_id
      JOIN serie s          ON s.id = t.serie_id
      JOIN tabela_preco tp  ON tp.serie_id = s.id AND tp.ano_letivo_id = m.ano_letivo_id
     WHERE m.status = 'ATIVA'
       AND make_date(p_ano, p_mes, 1) BETWEEN date_trunc('month', al.data_inicio)::date AND al.data_fim
       AND EXISTS (SELECT 1 FROM aluno_responsavel ar
                    WHERE ar.aluno_id = m.aluno_id AND ar.resp_financeiro)
    ON CONFLICT DO NOTHING;

    GET DIAGNOSTICS v_qtd = ROW_COUNT;
    RETURN v_qtd;
END $$;

-- Gera um intervalo de meses (ex.: fevereiro a dezembro)
CREATE OR REPLACE FUNCTION fn_gerar_cobrancas_periodo(p_ano integer, p_mes_ini integer, p_mes_fim integer)
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE
    v_total integer := 0;
    v_mes   integer;
BEGIN
    FOR v_mes IN p_mes_ini..p_mes_fim LOOP
        v_total := v_total + fn_gerar_cobrancas_mensais(p_ano, v_mes);
    END LOOP;
    RETURN v_total;
END $$;

/* ---- 5.6 Pagamentos -------------------------------------------------- */

-- Baixa manual (ex.: pagamento presencial na secretaria ou confirmação no site)
CREATE OR REPLACE FUNCTION fn_registrar_pagamento(
    p_cobranca_id uuid,
    p_origem      origem_pagamento,
    p_valor_pago  numeric     DEFAULT NULL,
    p_pago_em     timestamptz DEFAULT now()
) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE v_status status_cobranca;
BEGIN
    SELECT status INTO v_status FROM cobranca WHERE id = p_cobranca_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Cobrança não encontrada'; END IF;
    IF v_status = 'PAGO' THEN RAISE EXCEPTION 'Cobrança já está paga'; END IF;
    IF v_status = 'CANCELADO' THEN RAISE EXCEPTION 'Cobrança cancelada não pode ser paga'; END IF;

    UPDATE cobranca
       SET status = 'PAGO', origem_pagamento = p_origem,
           valor_pago = p_valor_pago, pago_em = p_pago_em
     WHERE id = p_cobranca_id;
END $$;

-- Para o webhook do Cora: localiza a cobrança pelo identificador do banco
CREATE OR REPLACE FUNCTION fn_registrar_pagamento_cora(
    p_cora_id     varchar,
    p_valor_pago  numeric,
    p_pago_em     timestamptz DEFAULT now()
) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE v_id uuid;
BEGIN
    SELECT id INTO v_id FROM cobranca WHERE cora_id = p_cora_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'Cobrança com cora_id % não encontrada', p_cora_id; END IF;

    PERFORM fn_registrar_pagamento(v_id, 'SITE', p_valor_pago, p_pago_em);
    UPDATE cobranca SET cora_sincronizado_em = now() WHERE id = v_id;
    RETURN v_id;
END $$;

/* ---- 5.7 Inadimplência e notificações (RF05) ------------------------- */
CREATE OR REPLACE FUNCTION fn_atualizar_cobrancas_vencidas() RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE v_qtd integer;
BEGIN
    UPDATE cobranca SET status = 'VENCIDO'
     WHERE status = 'PENDENTE' AND vencimento < current_date;
    GET DIAGNOSTICS v_qtd = ROW_COUNT;
    RETURN v_qtd;
END $$;

-- Cria notificações (uma por cobrança/tipo/canal/dia) para quem vence nos próximos
-- p_dias_antecedencia dias ou já está em atraso. A aplicação envia as PENDENTE.
CREATE OR REPLACE FUNCTION fn_gerar_notificacoes_cobranca(
    p_dias_antecedencia integer DEFAULT 3,
    p_canal             canal_notificacao DEFAULT 'EMAIL'
) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE v_qtd integer;
BEGIN
    INSERT INTO notificacao_cobranca (cobranca_id, responsavel_id, tipo, canal)
    SELECT c.id, c.responsavel_id,
           CASE WHEN c.vencimento < current_date
                THEN 'INADIMPLENCIA'::tipo_notificacao
                ELSE 'VENCIMENTO_PROXIMO'::tipo_notificacao END,
           p_canal
      FROM cobranca c
     WHERE c.status IN ('PENDENTE','VENCIDO')
       AND c.vencimento <= current_date + p_dias_antecedencia
    ON CONFLICT DO NOTHING;

    GET DIAGNOSTICS v_qtd = ROW_COUNT;
    RETURN v_qtd;
END $$;

/* ---- 5.8 Obrigações institucionais (RF09) ---------------------------- */
CREATE OR REPLACE FUNCTION fn_atualizar_obrigacoes() RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE v_qtd integer;
BEGIN
    UPDATE obrigacao_institucional SET status = 'ATRASADA'
     WHERE status = 'PENDENTE' AND proximo_vencimento < current_date;
    GET DIAGNOSTICS v_qtd = ROW_COUNT;
    RETURN v_qtd;
END $$;

-- Marca como cumprida e já programa o próximo vencimento quando houver periodicidade
CREATE OR REPLACE FUNCTION fn_cumprir_obrigacao(p_obrigacao_id uuid) RETURNS date
LANGUAGE plpgsql AS $$
DECLARE v_prox date;
BEGIN
    UPDATE obrigacao_institucional
       SET ultimo_cumprimento = current_date,
           proximo_vencimento = CASE WHEN periodicidade_meses IS NULL THEN proximo_vencimento
                                     ELSE (proximo_vencimento + make_interval(months => periodicidade_meses))::date END,
           status = CASE WHEN periodicidade_meses IS NULL THEN 'CUMPRIDA'::status_obrigacao
                         ELSE 'PENDENTE'::status_obrigacao END
     WHERE id = p_obrigacao_id
    RETURNING proximo_vencimento INTO v_prox;

    IF NOT FOUND THEN RAISE EXCEPTION 'Obrigação não encontrada'; END IF;
    RETURN v_prox;
END $$;

/* ---- 5.9 Rotina diária ----------------------------------------------- */
-- Agende uma vez por dia. No Supabase: Database > Extensions > pg_cron, e então:
--   SELECT cron.schedule('semear-rotina-diaria', '0 6 * * *', 'SELECT fn_rotina_diaria()');
--   SELECT cron.schedule('semear-cobrancas-mes', '0 5 25 * *',
--     $cron$ SELECT fn_gerar_cobrancas_mensais(
--              extract(year  FROM current_date + interval '1 month')::int,
--              extract(month FROM current_date + interval '1 month')::int) $cron$);
CREATE OR REPLACE FUNCTION fn_rotina_diaria() RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM fn_atualizar_cobrancas_vencidas();
    PERFORM fn_gerar_notificacoes_cobranca(3, 'EMAIL');
    PERFORM fn_atualizar_obrigacoes();
END $$;


/* ---------------------------------------------------------------------
   6. VIEWS
   --------------------------------------------------------------------- */

/* ---- 6.1 Gestão (secretaria, financeiro, gestor) --------------------- */

-- RF03: ocupação das turmas
CREATE OR REPLACE VIEW vw_vagas_turma AS
SELECT t.id AS turma_id, al.ano, s.nome AS serie, t.nome AS turma,
       t.capacidade_max,
       count(m.id) FILTER (WHERE m.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')) AS ocupadas,
       t.capacidade_max - count(m.id) FILTER (WHERE m.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')) AS vagas,
       (count(m.id) FILTER (WHERE m.status IN ('AGUARDANDO_PAGAMENTO','ATIVA')) >= t.capacidade_max) AS lotada
  FROM turma t
  JOIN serie s       ON s.id = t.serie_id
  JOIN ano_letivo al ON al.id = t.ano_letivo_id
  LEFT JOIN matricula m ON m.turma_id = t.id
 GROUP BY t.id, al.ano, s.nome, s.ordem, t.nome, t.capacidade_max;

CREATE OR REPLACE VIEW vw_pre_matriculas_pendentes AS
SELECT p.id, p.criado_em, p.aluno_nome, p.aluno_nascimento, s.nome AS serie, al.ano,
       r.nome AS responsavel, r.telefone, r.email
  FROM pre_matricula p
  JOIN serie s       ON s.id = p.serie_id
  JOIN ano_letivo al ON al.id = p.ano_letivo_id
  JOIN responsavel r ON r.id = p.responsavel_id
 WHERE p.status = 'PENDENTE'
 ORDER BY p.criado_em;

-- RF04/RF05: inadimplentes (status calculado pela data, sem depender da rotina)
CREATE OR REPLACE VIEW vw_inadimplencia AS
SELECT c.id AS cobranca_id, a.id AS aluno_id, a.nome AS aluno,
       r.nome AS responsavel, r.telefone, r.email,
       c.tipo, c.competencia, c.vencimento, c.valor_final,
       (current_date - c.vencimento) AS dias_atraso
  FROM cobranca c
  JOIN matricula m   ON m.id = c.matricula_id
  JOIN aluno a       ON a.id = m.aluno_id
  JOIN responsavel r ON r.id = c.responsavel_id
 WHERE c.status IN ('PENDENTE','VENCIDO') AND c.vencimento < current_date;

CREATE OR REPLACE VIEW vw_resumo_financeiro_mensal AS
SELECT c.competencia, c.tipo,
       count(*)                                              AS qtd_cobrancas,
       sum(c.valor_final)                                    AS total_previsto,
       sum(c.desconto_bolsa)                                 AS total_descontos,
       sum(c.valor_pago)  FILTER (WHERE c.status = 'PAGO')   AS total_recebido,
       sum(c.valor_final) FILTER (WHERE c.status IN ('PENDENTE','VENCIDO')
                                    AND c.vencimento < current_date) AS total_em_atraso
  FROM cobranca c
 WHERE c.status <> 'CANCELADO'
 GROUP BY c.competencia, c.tipo
 ORDER BY c.competencia, c.tipo;

CREATE OR REPLACE VIEW vw_bolsistas AS
SELECT m.id AS matricula_id, a.nome AS aluno, al.ano, b.nome AS bolsa,
       b.tipo_desconto, b.valor, bm.vigencia_inicio, bm.vigencia_fim, b.ativa
  FROM bolsa_matricula bm
  JOIN bolsa b       ON b.id = bm.bolsa_id
  JOIN matricula m   ON m.id = bm.matricula_id
  JOIN aluno a       ON a.id = m.aluno_id
  JOIN ano_letivo al ON al.id = m.ano_letivo_id;

-- RF09: obrigações vencendo ou atrasadas
CREATE OR REPLACE VIEW vw_alertas_obrigacoes AS
SELECT o.id, o.orgao, o.descricao, o.proximo_vencimento,
       (o.proximo_vencimento - current_date) AS dias_restantes,
       CASE WHEN o.proximo_vencimento < current_date THEN 'ATRASADA' ELSE 'A_VENCER' END AS situacao,
       f.nome AS responsavel
  FROM obrigacao_institucional o
  LEFT JOIN funcionario f ON f.id = o.responsavel_id
 WHERE o.status <> 'CUMPRIDA'
   AND o.proximo_vencimento <= current_date + o.dias_alerta_antecedencia
 ORDER BY o.proximo_vencimento;

/* ---- 6.2 Pedagógico -------------------------------------------------- */

-- Turmas de cada professor (filtre por usuario_id do login)
CREATE OR REPLACE VIEW vw_turmas_do_professor AS
SELECT f.usuario_id, f.id AS funcionario_id, f.nome AS professor,
       t.id AS turma_id, al.ano, s.nome AS serie, t.nome AS turma,
       d.nome AS disciplina,
       (SELECT count(*) FROM matricula m WHERE m.turma_id = t.id AND m.status = 'ATIVA') AS alunos_ativos
  FROM turma_professor tp
  JOIN funcionario f ON f.id = tp.funcionario_id
  JOIN turma t       ON t.id = tp.turma_id
  JOIN serie s       ON s.id = t.serie_id
  JOIN ano_letivo al ON al.id = t.ano_letivo_id
  LEFT JOIN disciplina d ON d.id = tp.disciplina_id;

CREATE OR REPLACE VIEW vw_frequencia_aluno AS
SELECT m.id AS matricula_id, a.id AS aluno_id, a.nome AS aluno, t.nome AS turma,
       count(fr.*)                                  AS total_registros,
       count(fr.*) FILTER (WHERE fr.presente)       AS presencas,
       count(fr.*) FILTER (WHERE NOT fr.presente)   AS faltas,
       round(100.0 * count(fr.*) FILTER (WHERE fr.presente) / NULLIF(count(fr.*), 0), 1) AS percentual_presenca
  FROM matricula m
  JOIN aluno a ON a.id = m.aluno_id
  JOIN turma t ON t.id = m.turma_id
  LEFT JOIN frequencia fr ON fr.matricula_id = m.id
 GROUP BY m.id, a.id, a.nome, t.nome;

/* ---- 6.3 Portal do responsável / aluno (RF10) ------------------------ */
-- Mapeia o login ao(s) aluno(s) que ele pode ver. Nas demais views do portal,
-- filtre por: aluno_id IN (SELECT aluno_id FROM vw_portal_acesso WHERE usuario_id = :login)
CREATE OR REPLACE VIEW vw_portal_acesso AS
SELECT r.usuario_id, ar.aluno_id, 'RESPONSAVEL'::text AS vinculo
  FROM aluno_responsavel ar
  JOIN responsavel r ON r.id = ar.responsavel_id
 WHERE r.usuario_id IS NOT NULL
UNION ALL
SELECT a.usuario_id, a.id, 'ALUNO'::text
  FROM aluno a
 WHERE a.usuario_id IS NOT NULL;

CREATE OR REPLACE VIEW vw_portal_alunos AS
SELECT a.id AS aluno_id, a.nome AS aluno, m.id AS matricula_id, m.tipo, m.status,
       al.ano, s.nome AS serie, s.nivel, t.nome AS turma
  FROM aluno a
  JOIN matricula m   ON m.aluno_id = a.id
  JOIN turma t       ON t.id = m.turma_id
  JOIN serie s       ON s.id = t.serie_id
  JOIN ano_letivo al ON al.id = m.ano_letivo_id;

CREATE OR REPLACE VIEW vw_portal_cobrancas AS
SELECT m.aluno_id, c.id AS cobranca_id, c.responsavel_id, c.tipo, c.competencia,
       c.valor_bruto, c.desconto_bolsa, c.valor_final, c.vencimento,
       CASE WHEN c.status IN ('PENDENTE','VENCIDO') AND c.vencimento < current_date
            THEN 'VENCIDO'::status_cobranca ELSE c.status END AS status,
       c.cora_url, c.pago_em
  FROM cobranca c
  JOIN matricula m ON m.id = c.matricula_id;

CREATE OR REPLACE VIEW vw_portal_notas AS
SELECT m.aluno_id, al.ano, d.nome AS disciplina, n.bimestre, n.valor
  FROM nota n
  JOIN matricula m   ON m.id = n.matricula_id
  JOIN ano_letivo al ON al.id = m.ano_letivo_id
  JOIN disciplina d  ON d.id = n.disciplina_id;

-- RF06: lista de materiais da série da turma do aluno
CREATE OR REPLACE VIEW vw_portal_materiais AS
SELECT m.aluno_id, al.ano, s.nome AS serie, i.descricao, i.quantidade
  FROM matricula m
  JOIN turma t           ON t.id = m.turma_id
  JOIN serie s           ON s.id = t.serie_id
  JOIN ano_letivo al     ON al.id = m.ano_letivo_id
  JOIN lista_material lm ON lm.serie_id = s.id AND lm.ano_letivo_id = m.ano_letivo_id
  JOIN item_material i   ON i.lista_id = lm.id;

CREATE OR REPLACE VIEW vw_portal_contratos AS
SELECT m.aluno_id, ct.id AS contrato_id, ct.tipo, ct.status, ct.assinado_em, ct.arquivo_url
  FROM contrato ct
  JOIN matricula m ON m.id = ct.matricula_id;

CREATE OR REPLACE VIEW vw_portal_documentos AS
SELECT aluno_id, id AS documento_id, tipo, arquivo_url, enviado_em
  FROM documento_aluno;


/* ---------------------------------------------------------------------
   7. DADOS INICIAIS
   --------------------------------------------------------------------- */

-- Séries (Educação Infantil e 1º ao 5º ano)
INSERT INTO serie (nome, nivel, ordem) VALUES
    ('Maternal 1', 'EDUCACAO_INFANTIL', 1),
    ('Maternal 2', 'EDUCACAO_INFANTIL', 2),
    ('Infantil 1', 'EDUCACAO_INFANTIL', 3),
    ('Infantil 2', 'EDUCACAO_INFANTIL', 4),
    ('1º Ano',     'ENSINO_FUNDAMENTAL', 5),
    ('2º Ano',     'ENSINO_FUNDAMENTAL', 6),
    ('3º Ano',     'ENSINO_FUNDAMENTAL', 7),
    ('4º Ano',     'ENSINO_FUNDAMENTAL', 8),
    ('5º Ano',     'ENSINO_FUNDAMENTAL', 9);

-- Disciplinas (ajuste conforme a grade da escola)
INSERT INTO disciplina (nome) VALUES
    ('Língua Portuguesa'), ('Matemática'), ('Ciências'), ('História'),
    ('Geografia'), ('Artes'), ('Educação Física'), ('Ensino Religioso'), ('Língua Inglesa');

-- Permissões por perfil (seção 2 dos requisitos)
INSERT INTO perfil_permissao (perfil, modulo, pode_ler, pode_gravar)
SELECT 'ADMIN'::perfil_usuario, m, true, true
  FROM unnest(ARRAY['CONFIGURACOES','USUARIOS','MATRICULA','ALUNOS','TURMAS','DOCUMENTOS',
                    'FINANCEIRO','BOLSAS','RH','PEDAGOGICO','OBRIGACOES','PORTAL']) AS m;

INSERT INTO perfil_permissao (perfil, modulo, pode_ler, pode_gravar) VALUES
    ('SECRETARIA',  'MATRICULA',   true, true),
    ('SECRETARIA',  'ALUNOS',      true, true),
    ('SECRETARIA',  'TURMAS',      true, true),
    ('SECRETARIA',  'DOCUMENTOS',  true, true),
    ('SECRETARIA',  'PEDAGOGICO',  true, false),
    ('SECRETARIA',  'OBRIGACOES',  true, true),
    ('FINANCEIRO',  'FINANCEIRO',  true, true),
    ('FINANCEIRO',  'BOLSAS',      true, true),
    ('FINANCEIRO',  'MATRICULA',   true, false),
    ('FINANCEIRO',  'ALUNOS',      true, false),
    ('PROFESSOR',   'PEDAGOGICO',  true, true),
    ('PROFESSOR',   'TURMAS',      true, false),
    ('PROFESSOR',   'ALUNOS',      true, false),
    ('RESPONSAVEL', 'PORTAL',      true, true),   -- paga taxas, assina contratos, rematrícula
    ('ALUNO',       'PORTAL',      true, false);

-- Obrigações institucionais (RF09). AJUSTE os dias e a periodicidade do Cora.
INSERT INTO obrigacao_institucional (orgao, descricao, periodicidade_meses, proximo_vencimento) VALUES
    ('CENSO_INEP',       'Censo Escolar (INEP) — junho',                    12, fn_proxima_ocorrencia(6, 1)),
    ('SMTT',             'Obrigações SMTT — março',                         12, fn_proxima_ocorrencia(3, 1)),
    ('SISTEMA_PRESENCA', 'Sistema Presença — a cada 2 meses',                2, (date_trunc('month', current_date) + interval '2 months')::date),
    ('CORA',             'Obrigações junto ao Cora (definir prazo real)',   12, (current_date + interval '12 months')::date);


/* ---------------------------------------------------------------------
   8. EXEMPLO DE FLUXO DE TESTE (descomente e execute por partes)
   --------------------------------------------------------------------- */
/*
-- 8.1 Estrutura do ano
INSERT INTO ano_letivo (ano, data_inicio, data_fim) VALUES (2027, '2027-02-01', '2027-12-15');

INSERT INTO turma (serie_id, ano_letivo_id, nome)
SELECT s.id, a.id, 'A' FROM serie s, ano_letivo a WHERE a.ano = 2027 AND s.nome IN ('Maternal 1','1º Ano');

INSERT INTO tabela_preco (serie_id, ano_letivo_id, valor_matricula, valor_mensal, dia_vencimento)
SELECT s.id, a.id, 200.00, 450.00, 10 FROM serie s, ano_letivo a WHERE a.ano = 2027;

-- 8.2 Funcionário (secretaria) e responsável
INSERT INTO funcionario (nome, cpf, tipo) VALUES ('Secretaria Teste', '11111111111', 'ADMINISTRATIVO');
INSERT INTO responsavel (nome, cpf, telefone, email) VALUES ('Responsável Teste', '22222222222', '98999999999', 'resp@teste.com');

-- 8.3 Pré-matrícula -> aprovação (gera aluno, matrícula, contrato e cobrança da matrícula)
INSERT INTO pre_matricula (ano_letivo_id, serie_id, responsavel_id, parentesco, aluno_nome, aluno_nascimento,
                           farda_uniforme_tam, farda_ed_fisica_tam)
SELECT a.id, s.id, r.id, 'Mãe', 'Aluno Teste', '2020-05-10', 'M', 'M'
  FROM ano_letivo a, serie s, responsavel r
 WHERE a.ano = 2027 AND s.nome = '1º Ano' AND r.cpf = '22222222222';

SELECT fn_aprovar_pre_matricula(
         (SELECT id FROM pre_matricula LIMIT 1),
         (SELECT id FROM funcionario LIMIT 1),
         (SELECT t.id FROM turma t JOIN serie s ON s.id = t.serie_id WHERE s.nome = '1º Ano'));

-- 8.4 Pagamento presencial da matrícula -> matrícula vira ATIVA
SELECT fn_registrar_pagamento((SELECT id FROM cobranca WHERE tipo = 'MATRICULA' LIMIT 1), 'PRESENCIAL');

-- 8.5 Bolsa de 20% e cobranças de fevereiro a dezembro (desconto aplicado sozinho)
INSERT INTO bolsa (nome, tipo_desconto, valor) VALUES ('Bolsa 20%', 'PERCENTUAL', 20);
INSERT INTO bolsa_matricula (bolsa_id, matricula_id)
SELECT b.id, m.id FROM bolsa b, matricula m LIMIT 1;
SELECT fn_gerar_cobrancas_periodo(2027, 2, 12);

-- 8.6 Conferências
SELECT * FROM vw_vagas_turma;
SELECT * FROM vw_resumo_financeiro_mensal;
SELECT * FROM vw_inadimplencia;
SELECT * FROM vw_alertas_obrigacoes;
SELECT fn_rotina_diaria();
*/

/* ======================= FIM DO SCRIPT ======================= */
