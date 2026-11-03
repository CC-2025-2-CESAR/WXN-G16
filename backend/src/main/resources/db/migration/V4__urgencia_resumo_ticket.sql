ALTER TABLE ticket
    ADD COLUMN urgencia VARCHAR(255) NOT NULL DEFAULT 'BAIXA'
        CHECK (urgencia IN ('BAIXA', 'MEDIA', 'ALTA')),
    ADD COLUMN resumo TEXT;