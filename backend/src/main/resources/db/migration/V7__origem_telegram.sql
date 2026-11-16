-- Conversas que chegam pelo Telegram. A V6 criou a restrição sem nome, e o Postgres a chama de conversa_origem_check.
ALTER TABLE conversa DROP CONSTRAINT conversa_origem_check;
ALTER TABLE conversa
    ADD CONSTRAINT conversa_origem_check
        CHECK (origem IN ('DEMONSTRACAO', 'SIMULADOR', 'WHATSAPP', 'TELEGRAM'));
