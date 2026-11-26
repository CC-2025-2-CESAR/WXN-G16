-- Data de início do atendimento humano; os atendimentos anteriores à migração não têm horário conhecido.
ALTER TABLE conversa ADD COLUMN assumida_em TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX idx_conversa_atendente_status ON conversa (atendente_id, status);
