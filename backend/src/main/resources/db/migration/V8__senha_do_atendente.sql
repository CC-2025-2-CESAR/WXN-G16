-- Atendentes de demonstração permanecem sem senha até o provisionamento explícito.
ALTER TABLE atendente ADD COLUMN senha_hash VARCHAR(100);

-- Login por e-mail deve identificar uma única conta, sem variar com maiúsculas.
CREATE UNIQUE INDEX uq_atendente_email_normalizado ON atendente (lower(email));
