ALTER TABLE conversa ADD COLUMN resumo_encerramento VARCHAR(1000);

CREATE INDEX idx_conversa_historico_encerrada
    ON conversa (encerrada_em DESC, id DESC)
    WHERE status = 'ENCERRADA';
