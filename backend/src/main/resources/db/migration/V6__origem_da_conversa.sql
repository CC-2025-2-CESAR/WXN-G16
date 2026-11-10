-- De onde a conversa veio. As que já existem são todas dos dados de demonstração: o canal de chat só passou a criar
-- conversas depois desta coluna ser pensada.
ALTER TABLE conversa
    ADD COLUMN origem VARCHAR(255) NOT NULL DEFAULT 'DEMONSTRACAO'
        CHECK (origem IN ('DEMONSTRACAO', 'SIMULADOR', 'WHATSAPP'));
