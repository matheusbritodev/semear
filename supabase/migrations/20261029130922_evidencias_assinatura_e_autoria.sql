-- Contrato: texto e valores congelados na assinatura + evidência do aceite eletrônico
ALTER TABLE contrato
    ADD COLUMN termo_assinado            jsonb,
    ADD COLUMN termo_hash                char(64),
    ADD COLUMN assinatura_canal          varchar(20) CHECK (assinatura_canal IN ('PORTAL','PRESENCIAL')),
    ADD COLUMN assinatura_ip             varchar(45),
    ADD COLUMN assinatura_navegador      varchar(255),
    ADD COLUMN assinatura_registrada_por uuid REFERENCES funcionario(id);
COMMENT ON COLUMN contrato.termo_assinado IS 'Cláusulas e valores exatamente como estavam no momento da assinatura (não mudam com reajuste de preço).';
COMMENT ON COLUMN contrato.termo_hash IS 'SHA-256 do texto assinado: prova de que o termo exibido é o mesmo que foi aceito.';
COMMENT ON COLUMN contrato.assinatura_registrada_por IS 'Funcionário que registrou a assinatura presencial (nulo quando o aceite foi pelo portal).';

-- Quem registrou: baixa presencial e cumprimento de obrigação
ALTER TABLE cobranca ADD COLUMN baixa_registrada_por uuid REFERENCES funcionario(id);
COMMENT ON COLUMN cobranca.baixa_registrada_por IS 'Funcionário que registrou a baixa presencial (nulo para pagamento pelo site/Cora).';

ALTER TABLE obrigacao_institucional ADD COLUMN ultimo_cumprimento_por uuid REFERENCES funcionario(id);
COMMENT ON COLUMN obrigacao_institucional.ultimo_cumprimento_por IS 'Funcionário que marcou o último cumprimento.';
