CREATE TABLE empresa (
    id   UUID         PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);

CREATE TABLE chatbot (
    id                   UUID         PRIMARY KEY,
    nome                 VARCHAR(255) NOT NULL,
    mensagem_boas_vindas VARCHAR(255) NOT NULL,
    ativo                BOOLEAN      NOT NULL
);

CREATE TABLE cliente (
    id         UUID         PRIMARY KEY,
    empresa_id UUID         NOT NULL REFERENCES empresa (id),
    nome       VARCHAR(255) NOT NULL,
    telefone   VARCHAR(255) NOT NULL
);

CREATE TABLE atendente (
    id         UUID         PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    disponivel BOOLEAN      NOT NULL
);

CREATE TABLE conversa (
    id           UUID         PRIMARY KEY,
    empresa_id   UUID         NOT NULL REFERENCES empresa (id),
    cliente_id   UUID         NOT NULL REFERENCES cliente (id),
    chatbot_id   UUID         NOT NULL REFERENCES chatbot (id),
    atendente_id UUID         REFERENCES atendente (id),
    status       VARCHAR(255) NOT NULL
        CHECK (status IN ('COM_CHATBOT', 'AGUARDANDO_ATENDENTE', 'COM_ATENDENTE', 'ENCERRADA')),
    iniciada_em  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    encerrada_em TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE mensagem (
    id             UUID         PRIMARY KEY,
    conversa_id    UUID         NOT NULL REFERENCES conversa (id),
    remetente_id   UUID         NOT NULL,
    tipo_remetente VARCHAR(255) NOT NULL
        CHECK (tipo_remetente IN ('CLIENTE', 'CHATBOT', 'ATENDENTE')),
    conteudo       TEXT         NOT NULL,
    enviada_em     TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE ticket (
    id           UUID         PRIMARY KEY,
    conversa_id  UUID         NOT NULL UNIQUE REFERENCES conversa (id),
    motivo       VARCHAR(255) NOT NULL,
    status       VARCHAR(255) NOT NULL
        CHECK (status IN ('ABERTO', 'EM_ATENDIMENTO', 'RESOLVIDO')),
    atendente_id UUID         REFERENCES atendente (id),
    aberto_em    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolvido_em TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX idx_mensagem_conversa_enviada ON mensagem (conversa_id, enviada_em);
CREATE INDEX idx_conversa_status ON conversa (status);
CREATE INDEX idx_cliente_empresa ON cliente (empresa_id);
CREATE INDEX idx_conversa_empresa ON conversa (empresa_id);