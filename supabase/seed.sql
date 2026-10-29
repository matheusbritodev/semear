/* =====================================================================
   SISTEMA SEMEAR — Dados de desenvolvimento realistas (TODOS FICTÍCIOS)

   Cenário: Centro Educacional Semeando, São Luís/MA, em 28/10/2026.
   - 2025: ano encerrado (histórico que torna as matrículas de 2026 REMATRICULA)
   - 2026: ano letivo em andamento, 160 alunos, mensalidades de fev a dez,
           pagamentos com desconto de pontualidade (RN08), multa/juros (RN09),
           famílias inadimplentes, bolsas, diário/frequência (ago-out), notas (1º-3º bim)
   - 2027: período de rematrícula (outubro) e pré-matrículas de novas famílias

   CPFs gerados com dígito verificador válido, mas inventados. E-mails em
   example.com (domínio reservado). Nomes inventados.
   NUNCA executar em produção. Não cria logins: ver docs/06-supabase-conexao.md.
   ===================================================================== */

SELECT setseed(0.2026);

CREATE OR REPLACE FUNCTION pg_temp.cpf(n bigint) RETURNS char(11) LANGUAGE plpgsql AS $$
DECLARE b text := lpad((n % 1000000000)::text, 9, '0'); s int := 0; d1 int; d2 int; i int;
BEGIN
    FOR i IN 1..9 LOOP s := s + substr(b, i, 1)::int * (11 - i); END LOOP;
    d1 := 11 - s % 11; IF d1 >= 10 THEN d1 := 0; END IF;
    s := 0;
    FOR i IN 1..9 LOOP s := s + substr(b, i, 1)::int * (12 - i); END LOOP;
    s := s + d1 * 2;
    d2 := 11 - s % 11; IF d2 >= 10 THEN d2 := 0; END IF;
    RETURN b || d1 || d2;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.sortear(opcoes text[]) RETURNS text LANGUAGE sql VOLATILE AS
$$ SELECT opcoes[1 + floor(random() * array_length(opcoes, 1))::int] $$;

CREATE OR REPLACE FUNCTION pg_temp.slug(t text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT lower(translate(split_part(t, ' ', 1), 'ÁÀÂÃÉÊÍÓÔÕÚÇáàâãéêíóôõúç', 'AAAAEEIOOOUCaaaaeeiooouc')) $$;

CREATE OR REPLACE FUNCTION pg_temp.telefone() RETURNS text LANGUAGE sql VOLATILE AS
$$ SELECT '989' || lpad(floor(random() * 100000000)::text, 8, '0') $$;

CREATE TEMP SEQUENCE seq_cpf START 300000017 INCREMENT 7919;
CREATE TEMP TABLE tmp_familia (
    id serial PRIMARY KEY, resp_fin uuid, parentesco_fin text, resp_outro uuid, parentesco_outro text,
    sobrenome text, filhos int DEFAULT 1, ultima_ordem int, perfil text, mes_inad int);
CREATE TEMP TABLE tmp_aluno (aluno_id uuid, matricula26 uuid, ordem int, familia int, base numeric);

DO $seed$
DECLARE
    fem text[] := ARRAY['Ana','Maria','Júlia','Beatriz','Larissa','Camila','Fernanda','Juliana','Patrícia','Aline',
                        'Gabriela','Letícia','Mariana','Raimunda','Francisca','Antônia','Luana','Thaís','Vanessa','Débora',
                        'Priscila','Elaine','Cláudia','Rosana','Kátia','Simone','Adriana','Lorena','Natália','Sâmia'];
    masc text[] := ARRAY['José','João','Francisco','Antônio','Carlos','Paulo','Pedro','Lucas','Marcos','Rafael',
                         'José Ribamar','Raimundo','Daniel','Bruno','Thiago','Felipe','Rodrigo','Diego','Márcio','Leonardo',
                         'André','Fábio','Gustavo','Ricardo','Sérgio','Wellington','Jefferson','Edson','Gilson','Alexandre'];
    fem_cri text[] := ARRAY['Alice','Helena','Laura','Manuela','Valentina','Sophia','Isabella','Heloísa','Luíza','Cecília',
                            'Maitê','Lívia','Giovanna','Maria Clara','Maria Eduarda','Ana Clara','Ana Luíza','Lara','Melissa','Yasmin',
                            'Lorena','Esther','Rebeca','Clara','Ísis','Ágatha','Elisa','Mirella','Antonella','Beatriz'];
    masc_cri text[] := ARRAY['Miguel','Arthur','Gael','Heitor','Theo','Davi','Gabriel','Bernardo','Samuel','João Miguel',
                             'Pedro Henrique','Enzo','Lorenzo','Benjamin','Matheus','Lucas','Rafael','Nicolas','Guilherme','Gustavo',
                             'Isaac','Bento','Ravi','Joaquim','Benício','Murilo','Anthony','Caio','Davi Lucca','Henrique'];
    sobrenomes text[] := ARRAY['Silva','Santos','Sousa','Oliveira','Pereira','Costa','Ferreira','Rodrigues','Almeida','Lima',
                               'Carvalho','Araújo','Ribeiro','Gomes','Martins','Barbosa','Rocha','Mendes','Nascimento','Cardoso',
                               'Moraes','Castro','Pinheiro','Nogueira','Serra','Campos','Belfort','Muniz','Viana','Coelho',
                               'Ramos','Lobato','Frazão','Sá','Brandão','Furtado','Bogéa','Pacheco','Teixeira','Diniz'];
    conteudo_inf text[] := ARRAY[
        'Acolhida, roda de conversa e calendário; contação de história "A Lagarta Comilona"',
        'Coordenação motora fina: recorte e colagem com papéis coloridos',
        'Cores primárias: pintura com tinta guache',
        'Músicas e brincadeiras cantadas; psicomotricidade no pátio',
        'Reconhecimento das letras do próprio nome com fichas',
        'Números de 1 a 10: contagem com tampinhas',
        'Modelagem com massinha: animais da fazenda',
        'Hábitos de higiene: lavar as mãos e escovar os dentes',
        'Cantigas populares maranhenses e roda de ciranda',
        'Leitura de imagens e reconto da história do dia'];
    conteudo_fund text[] := ARRAY[
        'Português: leitura e interpretação de texto; Matemática: adição com reagrupamento',
        'Matemática: frações no cotidiano; Ciências: ciclo da água',
        'História: povos indígenas do Maranhão; Português: ortografia (S, SS, Ç)',
        'Geografia: os bairros de São Luís e o centro histórico',
        'Produção textual: carta pessoal; revisão em duplas',
        'Matemática: tabuada do 7 e situações-problema',
        'Ciências: sistema digestório e alimentação saudável',
        'Português: substantivos próprios e comuns; Artes: azulejos de São Luís',
        'Matemática: medidas de comprimento com régua e fita métrica',
        'Leitura deleite e roda de leitura do livro paradidático'];
    capacidade int[]   := ARRAY[15, 18, 20, 20, 30, 30, 30, 30, 30];
    quantidade int[]   := ARRAY[11, 14, 18, 20, 24, 22, 19, 17, 15];
    farda_tam text[]   := ARRAY['2', '4', '4', '6', '6', '8', '10', '12', '14'];
    preco25 numeric[]  := ARRAY[118, 118, 118, 118, 295, 356, 356, 356, 356];
    preco26 numeric[]  := ARRAY[125, 125, 125, 125, 310, 375, 375, 375, 375];
    preco27 numeric[]  := ARRAY[132.50, 132.50, 132.50, 132.50, 329, 398, 398, 398, 398];
    prof_nomes text[]  := ARRAY['Kelly Cristina Mendes','Joana Darc Pereira','Silvana Ribeiro Lima','Elisângela Sousa Rocha',
                                'Márcia Regina Castro','Luciana Almeida Serra','Tatiane Gomes Viana','Raquel Nogueira Pinheiro',
                                'Sandro Luís Frazão'];
    v_ano25 uuid; v_ano26 uuid; v_ano27 uuid; v_ano uuid;
    v_serie record; v_turma25 uuid; v_turma26 uuid;
    v_diretora uuid; v_secretaria uuid; v_financeiro uuid; v_edfis uuid; v_ingles uuid; v_prof uuid[] := '{}';
    v_fam record; v_fam_id int; v_sobrenome text; v_nome_mae text; v_nome_pai text; v_nome text;
    v_par_fin text; v_par_outro text; v_resp_fin uuid; v_resp_outro uuid; v_r float; v_perfil text;
    v_aluno uuid; v_mat uuid; v_dm date; v_nasc date; v_pago date; v_valor numeric; v_k int;
    c record; d date; v_diario uuid; v_lista uuid; v_pre uuid; v_rematriculas int := 0;
    pre_ordens int[] := ARRAY[1, 1, 1, 2, 2, 3, 5, 5, 6, 1];
BEGIN
    /* ---- Anos letivos, turmas e preços ---------------------------------- */
    INSERT INTO ano_letivo (ano, data_inicio, data_fim) VALUES (2025, '2025-02-03', '2025-12-12') RETURNING id INTO v_ano25;
    INSERT INTO ano_letivo (ano, data_inicio, data_fim) VALUES (2026, '2026-02-02', '2026-12-11') RETURNING id INTO v_ano26;
    INSERT INTO ano_letivo (ano, data_inicio, data_fim) VALUES (2027, '2027-02-01', '2027-12-10') RETURNING id INTO v_ano27;

    FOR v_serie IN SELECT id, ordem FROM serie ORDER BY ordem LOOP
        INSERT INTO turma (serie_id, ano_letivo_id, nome, capacidade_max) VALUES
            (v_serie.id, v_ano25, 'A', capacidade[v_serie.ordem]),
            (v_serie.id, v_ano26, 'A', capacidade[v_serie.ordem]),
            (v_serie.id, v_ano27, 'A', capacidade[v_serie.ordem]);
        -- valor da matrícula = 1ª parcela (RN12; decisão pendente P05)
        INSERT INTO tabela_preco (serie_id, ano_letivo_id, valor_matricula, valor_mensal, dia_vencimento) VALUES
            (v_serie.id, v_ano25, preco25[v_serie.ordem], preco25[v_serie.ordem], 10),
            (v_serie.id, v_ano26, preco26[v_serie.ordem], preco26[v_serie.ordem], 10),
            (v_serie.id, v_ano27, preco27[v_serie.ordem], preco27[v_serie.ordem], 10);
    END LOOP;

    /* ---- Funcionários (RF07) -------------------------------------------- */
    INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao)
    VALUES ('Lúcia Helena Ferreira Campos', pg_temp.cpf(nextval('seq_cpf')), 'GESTOR', pg_temp.telefone(), 'lucia.campos@example.com', '2012-01-16')
    RETURNING id INTO v_diretora;
    INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao)
    VALUES ('Ana Paula Costa Diniz', pg_temp.cpf(nextval('seq_cpf')), 'ADMINISTRATIVO', pg_temp.telefone(), 'secretaria.semear@example.com', '2016-02-01')
    RETURNING id INTO v_secretaria;
    INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao)
    VALUES ('Rosângela Martins Bogéa', pg_temp.cpf(nextval('seq_cpf')), 'ADMINISTRATIVO', pg_temp.telefone(), 'financeiro.semear@example.com', '2018-07-02')
    RETURNING id INTO v_financeiro;
    FOR v_k IN 1..9 LOOP
        INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao)
        VALUES (prof_nomes[v_k], pg_temp.cpf(nextval('seq_cpf')), 'PROFESSOR', pg_temp.telefone(),
                CASE WHEN v_k = 5 THEN 'professora.semear@example.com'
                     ELSE pg_temp.slug(prof_nomes[v_k]) || '.prof' || v_k || '@example.com' END,
                date '2014-02-03' + (v_k * 211))
        RETURNING id INTO v_aluno;
        v_prof := v_prof || v_aluno;
    END LOOP;
    INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao)
    VALUES ('Wellington Belfort Ramos', pg_temp.cpf(nextval('seq_cpf')), 'PROFESSOR', pg_temp.telefone(), 'wellington.edfisica@example.com', '2019-02-04')
    RETURNING id INTO v_edfis;
    INSERT INTO funcionario (nome, cpf, tipo, telefone, email, data_admissao)
    VALUES ('Carolina Muniz Coelho', pg_temp.cpf(nextval('seq_cpf')), 'PROFESSOR', pg_temp.telefone(), 'carolina.ingles@example.com', '2022-02-07')
    RETURNING id INTO v_ingles;
    INSERT INTO funcionario (nome, cpf, tipo, telefone, data_admissao) VALUES
        ('José Ribamar Sousa', pg_temp.cpf(nextval('seq_cpf')), 'PRESTADOR_SERVICO', pg_temp.telefone(), '2015-03-02'),
        ('Raimunda Nonata Silva', pg_temp.cpf(nextval('seq_cpf')), 'PRESTADOR_SERVICO', pg_temp.telefone(), '2017-01-09');

    /* ---- Professores nas turmas (polivalente + especialistas) ----------- */
    FOR v_serie IN SELECT id, ordem, nivel FROM serie ORDER BY ordem LOOP
        INSERT INTO turma_professor (turma_id, funcionario_id)
        SELECT t.id, v_prof[v_serie.ordem] FROM turma t
         WHERE t.serie_id = v_serie.id AND t.ano_letivo_id IN (v_ano26, v_ano27);
        IF v_serie.nivel = 'ENSINO_FUNDAMENTAL' THEN
            INSERT INTO turma_professor (turma_id, funcionario_id, disciplina_id)
            SELECT t.id, v_edfis, (SELECT id FROM disciplina WHERE nome = 'Educação Física') FROM turma t
             WHERE t.serie_id = v_serie.id AND t.ano_letivo_id IN (v_ano26, v_ano27);
            INSERT INTO turma_professor (turma_id, funcionario_id, disciplina_id)
            SELECT t.id, v_ingles, (SELECT id FROM disciplina WHERE nome = 'Língua Inglesa') FROM turma t
             WHERE t.serie_id = v_serie.id AND t.ano_letivo_id IN (v_ano26, v_ano27);
        END IF;
    END LOOP;

    /* ---- Bolsas (RF02) --------------------------------------------------- */
    INSERT INTO bolsa (nome, descricao, tipo_desconto, valor) VALUES
        ('Desconto Irmãos', '10% para cada irmão matriculado', 'PERCENTUAL', 10),
        ('Bolsa Social', 'Concedida após análise socioeconômica; revisada a cada ano letivo', 'PERCENTUAL', 30),
        ('Bolsa Funcionário', 'Filhos de colaboradores da escola', 'PERCENTUAL', 50),
        ('Convênio Empresarial', 'Convênio com empresas parceiras do bairro', 'VALOR_FIXO', 40);

    /* ---- Famílias, alunos e matrículas ----------------------------------- */
    FOR v_serie IN SELECT id, ordem, nivel FROM serie ORDER BY ordem LOOP
        SELECT id INTO v_turma26 FROM turma WHERE serie_id = v_serie.id AND ano_letivo_id = v_ano26;
        v_turma25 := NULL;
        SELECT t.id INTO v_turma25 FROM turma t JOIN serie s ON s.id = t.serie_id
         WHERE s.ordem = v_serie.ordem - 1 AND t.ano_letivo_id = v_ano25;

        FOR v_k IN 1..quantidade[v_serie.ordem] LOOP
            -- irmão de uma família já cadastrada (~13%) ou família nova
            v_fam_id := NULL;
            IF random() < 0.13 THEN
                SELECT id INTO v_fam_id FROM tmp_familia
                 WHERE filhos = 1 AND ultima_ordem <> v_serie.ordem ORDER BY random() LIMIT 1;
            END IF;

            IF v_fam_id IS NOT NULL THEN
                UPDATE tmp_familia SET filhos = 2, ultima_ordem = v_serie.ordem WHERE id = v_fam_id;
            ELSE
                v_sobrenome := pg_temp.sortear(sobrenomes);
                v_nome_mae := pg_temp.sortear(fem) || ' ' || pg_temp.sortear(sobrenomes) || ' ' || v_sobrenome;
                v_nome_pai := pg_temp.sortear(masc) || ' ' || pg_temp.sortear(sobrenomes) || ' ' || v_sobrenome;
                v_r := random();
                IF v_r < 0.80 THEN
                    v_par_fin := 'Mãe'; v_par_outro := CASE WHEN random() < 0.55 THEN 'Pai' END;
                ELSIF v_r < 0.95 THEN
                    v_par_fin := 'Pai'; v_par_outro := CASE WHEN random() < 0.80 THEN 'Mãe' END;
                ELSE
                    v_par_fin := 'Avó'; v_par_outro := CASE WHEN random() < 0.50 THEN 'Mãe' END;
                END IF;

                v_nome := CASE v_par_fin
                            WHEN 'Pai' THEN v_nome_pai
                            WHEN 'Avó' THEN pg_temp.sortear(ARRAY['Maria','Raimunda','Francisca','Antônia','Teresinha','Conceição'])
                                            || ' ' || pg_temp.sortear(sobrenomes) || ' ' || v_sobrenome
                            ELSE v_nome_mae END;
                INSERT INTO responsavel (nome, cpf, telefone, email)
                VALUES (v_nome, pg_temp.cpf(nextval('seq_cpf')), pg_temp.telefone(),
                        pg_temp.slug(v_nome) || '.' || pg_temp.slug(v_sobrenome) || (10 + floor(random() * 89))::int || '@example.com')
                RETURNING id INTO v_resp_fin;

                v_resp_outro := NULL;
                IF v_par_outro IS NOT NULL THEN
                    v_nome := CASE v_par_outro WHEN 'Pai' THEN v_nome_pai ELSE v_nome_mae END;
                    INSERT INTO responsavel (nome, cpf, telefone, email)
                    VALUES (v_nome, pg_temp.cpf(nextval('seq_cpf')), pg_temp.telefone(),
                            pg_temp.slug(v_nome) || '.' || pg_temp.slug(v_sobrenome) || (10 + floor(random() * 89))::int || '@example.com')
                    RETURNING id INTO v_resp_outro;
                END IF;

                -- comportamento de pagamento da família
                v_r := random();
                v_perfil := CASE WHEN v_r < 0.72 THEN 'PONTUAL' WHEN v_r < 0.92 THEN 'REGULAR' ELSE 'INADIMPLENTE' END;
                INSERT INTO tmp_familia (resp_fin, parentesco_fin, resp_outro, parentesco_outro, sobrenome, ultima_ordem, perfil, mes_inad)
                VALUES (v_resp_fin, v_par_fin, v_resp_outro, v_par_outro, v_sobrenome, v_serie.ordem, v_perfil, 7 + floor(random() * 4)::int)
                RETURNING id INTO v_fam_id;
            END IF;
            SELECT * INTO v_fam FROM tmp_familia WHERE id = v_fam_id;

            -- criança (idade compatível com a série; corte em 31/03)
            v_nome := CASE WHEN random() < 0.5 THEN pg_temp.sortear(fem_cri) ELSE pg_temp.sortear(masc_cri) END
                      || ' ' || pg_temp.sortear(sobrenomes) || ' ' || v_fam.sobrenome;
            v_nasc := make_date(2023 - (v_serie.ordem - 1), 4, 1) + floor(random() * 365)::int;
            INSERT INTO aluno (nome, data_nascimento) VALUES (v_nome, v_nasc) RETURNING id INTO v_aluno;
            INSERT INTO aluno_responsavel VALUES (v_aluno, v_fam.resp_fin, v_fam.parentesco_fin, true);
            IF v_fam.resp_outro IS NOT NULL THEN
                INSERT INTO aluno_responsavel VALUES (v_aluno, v_fam.resp_outro, v_fam.parentesco_outro, false);
            END IF;

            -- histórico 2025 (veteranos): faz a matrícula de 2026 nascer como REMATRICULA
            IF v_turma25 IS NOT NULL AND random() < 0.80 THEN
                v_dm := date '2024-11-04' + floor(random() * 85)::int;
                INSERT INTO matricula (aluno_id, turma_id, status, data_matricula)
                VALUES (v_aluno, v_turma25, 'ENCERRADA', v_dm) RETURNING id INTO v_mat;
                UPDATE matricula SET ativada_em = (v_dm + 2)::timestamptz WHERE id = v_mat;
                UPDATE contrato SET status = 'ASSINADO', assinado_por = v_fam.resp_fin, assinado_em = v_dm::timestamptz
                 WHERE matricula_id = v_mat;
            END IF;

            -- matrícula 2026 (feita entre nov/2025 e jan/2026, já paga e ativa)
            v_dm := date '2025-11-03' + floor(random() * 88)::int;
            INSERT INTO matricula (aluno_id, turma_id, status, data_matricula)
            VALUES (v_aluno, v_turma26, 'ATIVA', v_dm) RETURNING id INTO v_mat;
            UPDATE matricula SET ativada_em = (v_dm + 1 + floor(random() * 4)::int)::timestamptz WHERE id = v_mat;
            INSERT INTO farda_matricula (matricula_id, tipo, tamanho) VALUES
                (v_mat, 'UNIFORME', farda_tam[v_serie.ordem]), (v_mat, 'EDUCACAO_FISICA', farda_tam[v_serie.ordem]);

            v_valor := preco26[v_serie.ordem];
            v_pago := v_dm + floor(random() * 5)::int;
            INSERT INTO cobranca (matricula_id, responsavel_id, tipo, competencia, valor_bruto, valor_final, vencimento,
                                  status, pago_em, valor_pago, origem_pagamento)
            VALUES (v_mat, v_fam.resp_fin, 'MATRICULA', date_trunc('month', v_dm)::date, v_valor, v_valor, v_dm + 7,
                    'PAGO', (v_pago + time '10:30')::timestamptz, v_valor,
                    (CASE WHEN random() < 0.4 THEN 'SITE' ELSE 'PRESENCIAL' END)::origem_pagamento);

            IF random() < 0.97 THEN
                UPDATE contrato SET status = 'ASSINADO', assinado_por = v_fam.resp_fin, assinado_em = (v_dm + 1)::timestamptz
                 WHERE matricula_id = v_mat;
            END IF;

            INSERT INTO tmp_aluno VALUES (v_aluno, v_mat, v_serie.ordem, v_fam_id, 5.8 + random() * 3.8);
        END LOOP;
    END LOOP;

    /* ---- Bolsas concedidas em 2026 (antes de gerar as mensalidades) ------ */
    INSERT INTO bolsa_matricula (bolsa_id, matricula_id, vigencia_inicio, vigencia_fim, concedida_por)
    SELECT (SELECT id FROM bolsa WHERE nome = 'Desconto Irmãos'), ta.matricula26, '2026-02-01', '2026-12-31', v_diretora
      FROM tmp_aluno ta JOIN tmp_familia f ON f.id = ta.familia WHERE f.filhos = 2;

    INSERT INTO bolsa_matricula (bolsa_id, matricula_id, vigencia_inicio, vigencia_fim, concedida_por)
    SELECT b.id, x.matricula26, '2026-02-01', '2026-12-31', v_diretora
      FROM (SELECT ta.matricula26, row_number() OVER (ORDER BY random()) AS n
              FROM tmp_aluno ta JOIN tmp_familia f ON f.id = ta.familia
             WHERE f.filhos = 1 AND f.perfil <> 'INADIMPLENTE') x
      JOIN bolsa b ON b.nome = CASE WHEN x.n <= 7 THEN 'Bolsa Social'
                                    WHEN x.n <= 9 THEN 'Bolsa Funcionário'
                                    ELSE 'Convênio Empresarial' END
     WHERE x.n <= 13;

    /* ---- Mensalidades / Taxas de Associado de fev a dez/2026 (RN01) ------ */
    PERFORM fn_gerar_cobrancas_periodo(2026, 2, 12);

    -- boletos já emitidos no Cora (simulado) até o mês seguinte
    UPDATE cobranca
       SET cora_id = 'cora_' || substr(md5(id::text), 1, 20),
           cora_url = 'https://pagamentos.example.com/boleto/' || substr(md5(id::text), 1, 20),
           cora_sincronizado_em = (vencimento - 25)::timestamptz
     WHERE tipo <> 'MATRICULA' AND vencimento <= current_date + 20;

    /* ---- Pagamentos (RN08 desconto, RN09 multa e juros) ------------------ */
    FOR c IN SELECT co.id, co.tipo, co.valor_final, co.vencimento, f.perfil, f.mes_inad
               FROM cobranca co
               JOIN tmp_aluno ta ON ta.matricula26 = co.matricula_id
               JOIN tmp_familia f ON f.id = ta.familia
              WHERE co.tipo <> 'MATRICULA' AND co.vencimento < current_date
              ORDER BY co.vencimento LOOP
        IF c.perfil = 'INADIMPLENTE' AND extract(month FROM c.vencimento) >= c.mes_inad THEN CONTINUE; END IF;
        IF c.perfil = 'REGULAR' AND c.vencimento >= current_date - 20 AND random() < 0.35 THEN CONTINUE; END IF;

        IF c.perfil = 'PONTUAL' OR random() < 0.45 THEN
            v_pago := c.vencimento - floor(random() * 8)::int;
        ELSE
            v_pago := LEAST(c.vencimento + 1 + floor(random() * 17)::int, current_date - 1);
        END IF;

        IF v_pago <= c.vencimento THEN
            v_valor := CASE WHEN c.tipo = 'MENSALIDADE' THEN round(c.valor_final * 0.90, 2) ELSE c.valor_final END;
        ELSE
            v_valor := round(c.valor_final * (1.02 + 0.00033 * (v_pago - c.vencimento)), 2);
        END IF;

        PERFORM fn_registrar_pagamento(c.id,
            (CASE WHEN random() < 0.65 THEN 'SITE' ELSE 'PRESENCIAL' END)::origem_pagamento,
            v_valor,
            (v_pago + time '08:00' + make_interval(mins => floor(random() * 600)::int))::timestamptz);
    END LOOP;
    PERFORM fn_atualizar_cobrancas_vencidas();

    /* ---- Calendário letivo 2026 (RF08.3) — São Luís/MA ------------------- */
    INSERT INTO calendario_letivo (ano_letivo_id, data, tipo, descricao) VALUES
        (v_ano26, '2026-02-02', 'AULA',    'Início do ano letivo'),
        (v_ano26, '2026-02-16', 'FERIADO', 'Carnaval'),
        (v_ano26, '2026-02-17', 'FERIADO', 'Carnaval'),
        (v_ano26, '2026-02-18', 'RECESSO', 'Quarta-feira de Cinzas'),
        (v_ano26, '2026-03-21', 'EVENTO',  'Reunião de pais e mestres (1º bimestre)'),
        (v_ano26, '2026-04-03', 'FERIADO', 'Sexta-feira da Paixão'),
        (v_ano26, '2026-04-21', 'FERIADO', 'Tiradentes'),
        (v_ano26, '2026-05-01', 'FERIADO', 'Dia do Trabalhador'),
        (v_ano26, '2026-05-08', 'EVENTO',  'Homenagem ao Dia das Mães'),
        (v_ano26, '2026-06-04', 'FERIADO', 'Corpus Christi'),
        (v_ano26, '2026-06-19', 'EVENTO',  'Arraial do Semear (festa junina)'),
        (v_ano26, '2026-07-28', 'FERIADO', 'Adesão do Maranhão à Independência'),
        (v_ano26, '2026-08-14', 'EVENTO',  'Homenagem ao Dia dos Pais'),
        (v_ano26, '2026-09-07', 'FERIADO', 'Independência do Brasil'),
        (v_ano26, '2026-09-08', 'FERIADO', 'Aniversário de São Luís'),
        (v_ano26, '2026-10-09', 'EVENTO',  'Festa do Dia das Crianças'),
        (v_ano26, '2026-10-12', 'FERIADO', 'Nossa Senhora Aparecida'),
        (v_ano26, '2026-10-15', 'RECESSO', 'Dia do Professor'),
        (v_ano26, '2026-11-02', 'FERIADO', 'Finados'),
        (v_ano26, '2026-11-20', 'FERIADO', 'Dia da Consciência Negra'),
        (v_ano26, '2026-12-08', 'FERIADO', 'Nossa Senhora da Conceição (municipal)'),
        (v_ano26, '2026-12-10', 'EVENTO',  'Formatura do 5º ano e da Educação Infantil'),
        (v_ano26, '2026-12-11', 'EVENTO',  'Encerramento do ano letivo'),
        (v_ano27, '2027-02-01', 'AULA',    'Início do ano letivo');
    INSERT INTO calendario_letivo (ano_letivo_id, data, tipo, descricao)
    SELECT v_ano26, g::date, 'RECESSO', 'Recesso escolar de julho'
      FROM generate_series(date '2026-07-13', date '2026-07-24', interval '1 day') g
     WHERE extract(isodow FROM g) < 6;

    /* ---- Diário de classe e frequência (ago a out/2026) — RF08.1 --------- */
    FOR v_serie IN SELECT s.ordem, s.nivel, t.id AS turma_id FROM turma t JOIN serie s ON s.id = t.serie_id
                    WHERE t.ano_letivo_id = v_ano26 ORDER BY s.ordem LOOP
        FOR d IN SELECT g::date FROM generate_series(date '2026-08-03', date '2026-10-27', interval '1 day') g
                  WHERE extract(isodow FROM g) < 6
                    AND NOT EXISTS (SELECT 1 FROM calendario_letivo cl
                                     WHERE cl.ano_letivo_id = v_ano26 AND cl.data = g::date AND cl.tipo IN ('FERIADO', 'RECESSO')) LOOP
            INSERT INTO diario_classe (turma_id, funcionario_id, data_aula, conteudo)
            VALUES (v_serie.turma_id, v_prof[v_serie.ordem], d,
                    CASE WHEN v_serie.nivel = 'EDUCACAO_INFANTIL' THEN pg_temp.sortear(conteudo_inf) ELSE pg_temp.sortear(conteudo_fund) END)
            RETURNING id INTO v_diario;

            INSERT INTO frequencia (diario_id, matricula_id, presente, justificativa)
            SELECT v_diario, x.id, x.presente,
                   CASE WHEN NOT x.presente AND random() < 0.35
                        THEN pg_temp.sortear(ARRAY['Atestado médico', 'Consulta odontológica', 'Viagem da família', 'Febre (aviso da família)']) END
              FROM (SELECT m.id, random() < 0.94 AS presente FROM matricula m
                     WHERE m.turma_id = v_serie.turma_id AND m.status = 'ATIVA') x;
        END LOOP;
    END LOOP;

    /* ---- Notas do 1º ao 3º bimestre (Ensino Fundamental) ----------------- */
    INSERT INTO nota (matricula_id, disciplina_id, bimestre, valor)
    SELECT ta.matricula26, di.id, b,
           LEAST(10, GREATEST(0, round((ta.base + (random() - 0.5) * 2.4
                 + CASE di.nome WHEN 'Matemática' THEN -0.4 WHEN 'Artes' THEN 0.6 WHEN 'Educação Física' THEN 0.8 ELSE 0 END)::numeric, 1)))
      FROM tmp_aluno ta
      CROSS JOIN disciplina di
      CROSS JOIN generate_series(1, 3) b
     WHERE ta.ordem >= 5 AND di.nome <> 'Ensino Religioso';

    /* ---- Planos de aula de novembro (RF08.2) ----------------------------- */
    INSERT INTO plano_aula (turma_id, funcionario_id, tipo, data_prevista, objetivo, conteudo)
    SELECT t.id, v_prof[s.ordem], p.tipo::tipo_plano, p.data::date,
           CASE WHEN s.nivel = 'EDUCACAO_INFANTIL' THEN p.obj_inf ELSE p.obj_fund END,
           CASE WHEN s.nivel = 'EDUCACAO_INFANTIL' THEN p.cont_inf ELSE p.cont_fund END
      FROM turma t JOIN serie s ON s.id = t.serie_id
     CROSS JOIN (VALUES
        ('PLANO_AULA', '2026-11-03',
         'Explorar texturas e cores da natureza', 'Passeio no pátio, coleta de folhas e colagem coletiva',
         'Resolver problemas com as quatro operações', 'Situações-problema do cotidiano (feira, troco); trabalho em duplas'),
        ('PLANEJAMENTO_DIARIO', '2026-11-04',
         'Rotina: acolhida, roda de conversa, lanche e parque', 'Música "Cabeça, ombro, joelho e pé"; jogos de encaixe',
         'Rotina do dia', 'Correção do dever; leitura compartilhada; Ciências: plantas medicinais do Maranhão'),
        ('PLANO_AULA', '2026-11-10',
         'Reconhecer o próprio nome', 'Fichas com os nomes da turma e jogo da memória',
         'Produzir texto narrativo', 'Leitura da lenda de Ana Jansen e reescrita em grupos'),
        ('PLANO_AULA', '2026-11-17',
         'Cultura popular: Bumba meu boi', 'Apreciação de toadas e confecção de chapéu de boi com papel',
         'Consciência Negra: personalidades negras do Maranhão', 'Pesquisa orientada e cartaz coletivo')
     ) AS p(tipo, data, obj_inf, cont_inf, obj_fund, cont_fund)
     WHERE t.ano_letivo_id = v_ano26;

    /* ---- Formação continuada (RF08.4) ------------------------------------ */
    INSERT INTO formacao_continuada (funcionario_id, curso, carga_horaria, concluida_em) VALUES
        (v_prof[1], 'Ludicidade e desenvolvimento na Educação Infantil', 20, '2026-03-14'),
        (v_prof[2], 'Primeiros socorros no ambiente escolar', 8, '2026-02-07'),
        (v_prof[5], 'Alfabetização e letramento na perspectiva da BNCC', 40, '2025-11-29'),
        (v_prof[7], 'Educação inclusiva: estratégias para alunos com TEA', 30, '2026-06-27'),
        (v_ingles,  'Teaching English to Young Learners', 60, NULL),
        (v_edfis,   'Jogos cooperativos e psicomotricidade', 20, '2026-08-22'),
        (v_prof[9], 'Avaliação formativa em sala de aula', 20, NULL);

    /* ---- Listas de materiais 2026 e 2027 (RF06) -------------------------- */
    FOR v_ano IN SELECT unnest(ARRAY[v_ano26, v_ano27]) LOOP
        FOR v_serie IN SELECT id, ordem FROM serie ORDER BY ordem LOOP
            INSERT INTO lista_material (serie_id, ano_letivo_id) VALUES (v_serie.id, v_ano) RETURNING id INTO v_lista;
            INSERT INTO item_material (lista_id, descricao, quantidade)
            SELECT v_lista, i.descricao, i.qtd
              FROM (VALUES
                ('Caderno de desenho grande (96 folhas)', 2, 1, 4),
                ('Massinha de modelar atóxica (12 cores)', 2, 1, 4),
                ('Giz de cera grosso (12 cores)', 2, 1, 4),
                ('Tinta guache 15 ml (6 cores)', 1, 1, 4),
                ('Pincel chato nº 12', 1, 1, 4),
                ('Lenço umedecido (pacote)', 4, 1, 2),
                ('Muda de roupa identificada (na mochila)', 1, 1, 3),
                ('Toalhinha de mão com nome', 2, 1, 4),
                ('Escova de dentes com protetor', 2, 1, 9),
                ('Cola branca 90 g', 2, 1, 9),
                ('Tesoura sem ponta com nome gravado', 1, 1, 9),
                ('Pasta com elástico', 2, 1, 9),
                ('Caderno brochura capa dura (96 folhas)', 4, 5, 6),
                ('Caderno de caligrafia', 1, 5, 6),
                ('Caderno universitário (10 matérias)', 1, 7, 9),
                ('Lápis grafite nº 2', 6, 5, 9),
                ('Borracha branca macia', 3, 5, 9),
                ('Apontador com depósito', 2, 5, 9),
                ('Caixa de lápis de cor (24 cores)', 1, 4, 9),
                ('Régua 30 cm', 1, 6, 9),
                ('Caneta esferográfica azul', 3, 8, 9),
                ('Dicionário escolar de Língua Portuguesa', 1, 6, 9),
                ('Minidicionário Inglês–Português', 1, 8, 9),
                ('Livro paradidático (indicado na 1ª reunião)', 2, 4, 9),
                ('Resma de papel A4', 1, 1, 9)
              ) AS i(descricao, qtd, min_ordem, max_ordem)
             WHERE v_serie.ordem BETWEEN i.min_ordem AND i.max_ordem;
        END LOOP;
    END LOOP;

    /* ---- Obrigações institucionais (RF09) -------------------------------- */
    UPDATE obrigacao_institucional SET responsavel_id = v_secretaria;
    UPDATE obrigacao_institucional SET responsavel_id = v_financeiro WHERE orgao = 'CORA';
    UPDATE obrigacao_institucional SET ultimo_cumprimento = '2026-06-17' WHERE orgao = 'CENSO_INEP';
    UPDATE obrigacao_institucional SET ultimo_cumprimento = '2026-03-12' WHERE orgao = 'SMTT';
    UPDATE obrigacao_institucional SET ultimo_cumprimento = '2026-10-05' WHERE orgao = 'SISTEMA_PRESENCA';

    /* ---- Rematrículas para 2027 (RF01.5) — só adimplentes (RN13) --------- */
    FOR c IN SELECT ta.aluno_id, ta.ordem, f.resp_fin
               FROM tmp_aluno ta JOIN tmp_familia f ON f.id = ta.familia
              WHERE ta.ordem < 9 AND f.perfil <> 'INADIMPLENTE'
                AND NOT EXISTS (SELECT 1 FROM vw_inadimplencia i WHERE i.aluno_id = ta.aluno_id)
              ORDER BY random() LOOP
        EXIT WHEN v_rematriculas >= 52;
        v_mat := fn_rematricular(c.aluno_id,
                    (SELECT t.id FROM turma t JOIN serie s ON s.id = t.serie_id
                      WHERE s.ordem = c.ordem + 1 AND t.ano_letivo_id = v_ano27),
                    farda_tam[c.ordem + 1], farda_tam[c.ordem + 1]);
        v_dm := date '2026-10-01' + floor(random() * 27)::int;
        UPDATE matricula SET data_matricula = v_dm WHERE id = v_mat;
        UPDATE cobranca SET competencia = date_trunc('month', v_dm)::date, vencimento = v_dm + 7
         WHERE matricula_id = v_mat AND tipo = 'MATRICULA';
        IF random() < 0.62 THEN
            PERFORM fn_registrar_pagamento(
                (SELECT id FROM cobranca WHERE matricula_id = v_mat AND tipo = 'MATRICULA'),
                (CASE WHEN random() < 0.7 THEN 'SITE' ELSE 'PRESENCIAL' END)::origem_pagamento,
                NULL,
                (LEAST(v_dm + floor(random() * 5)::int, current_date) + time '11:00')::timestamptz);
            UPDATE contrato SET status = 'ASSINADO', assinado_por = c.resp_fin, assinado_em = (v_dm + 1)::timestamptz
             WHERE matricula_id = v_mat;
        END IF;
        v_rematriculas := v_rematriculas + 1;
    END LOOP;

    /* ---- Pré-matrículas 2027 de novas famílias (RF01.1 / RF01.2) --------- */
    FOR v_k IN 1..10 LOOP
        IF v_k = 10 THEN
            -- irmão caçula de uma família que já estuda na escola
            SELECT resp_fin, sobrenome INTO v_resp_fin, v_sobrenome FROM tmp_familia
             WHERE perfil = 'PONTUAL' AND parentesco_fin = 'Mãe' ORDER BY random() LIMIT 1;
        ELSE
            v_sobrenome := pg_temp.sortear(sobrenomes);
            v_nome := pg_temp.sortear(fem) || ' ' || pg_temp.sortear(sobrenomes) || ' ' || v_sobrenome;
            INSERT INTO responsavel (nome, cpf, telefone, email)
            VALUES (v_nome, pg_temp.cpf(nextval('seq_cpf')), pg_temp.telefone(),
                    pg_temp.slug(v_nome) || '.' || pg_temp.slug(v_sobrenome) || (10 + floor(random() * 89))::int || '@example.com')
            RETURNING id INTO v_resp_fin;
        END IF;

        v_nome := CASE WHEN random() < 0.5 THEN pg_temp.sortear(fem_cri) ELSE pg_temp.sortear(masc_cri) END
                  || ' ' || pg_temp.sortear(sobrenomes) || ' ' || v_sobrenome;
        INSERT INTO pre_matricula (ano_letivo_id, serie_id, responsavel_id, parentesco, aluno_nome, aluno_nascimento,
                                   farda_uniforme_tam, farda_ed_fisica_tam)
        VALUES (v_ano27, (SELECT id FROM serie WHERE ordem = pre_ordens[v_k]), v_resp_fin, 'Mãe', v_nome,
                make_date(2024 - (pre_ordens[v_k] - 1), 4, 1) + floor(random() * 365)::int,
                farda_tam[pre_ordens[v_k]], farda_tam[pre_ordens[v_k]])
        RETURNING id INTO v_pre;
        UPDATE pre_matricula SET criado_em = timestamptz '2026-10-01 08:00-03' + make_interval(hours => floor(random() * 26 * 24)::int)
         WHERE id = v_pre;

        IF v_k IN (1, 7) THEN
            PERFORM fn_aprovar_pre_matricula(v_pre, v_secretaria,
                (SELECT t.id FROM turma t JOIN serie s ON s.id = t.serie_id
                  WHERE s.ordem = pre_ordens[v_k] AND t.ano_letivo_id = v_ano27));
        ELSIF v_k = 4 THEN
            PERFORM fn_rejeitar_pre_matricula(v_pre, v_secretaria,
                'Documentação incompleta: certidão de nascimento e comprovante de residência não enviados após três contatos.');
        END IF;
    END LOOP;

    /* ---- Fila de notificações do dia (RF05) ------------------------------ */
    PERFORM fn_gerar_notificacoes_cobranca(3, 'EMAIL');
END $seed$;

/* Resumo do que foi criado */
SELECT
    (SELECT count(*) FROM aluno)                                                     AS alunos,
    (SELECT count(*) FROM responsavel)                                               AS responsaveis,
    (SELECT count(*) FROM funcionario)                                               AS funcionarios,
    (SELECT count(*) FROM matricula m JOIN ano_letivo a ON a.id = m.ano_letivo_id WHERE a.ano = 2026) AS matriculas_2026,
    (SELECT count(*) FROM matricula m JOIN ano_letivo a ON a.id = m.ano_letivo_id WHERE a.ano = 2027) AS matriculas_2027,
    (SELECT count(*) FROM cobranca)                                                  AS cobrancas,
    (SELECT count(*) FROM cobranca WHERE status = 'PAGO')                            AS pagas,
    (SELECT count(*) FROM vw_inadimplencia)                                          AS em_atraso,
    (SELECT count(*) FROM pre_matricula WHERE status = 'PENDENTE')                   AS pre_pendentes,
    (SELECT count(*) FROM frequencia)                                                AS registros_frequencia,
    (SELECT count(*) FROM nota)                                                      AS notas,
    (SELECT count(*) FROM alerta_secretaria)                                         AS alertas;
