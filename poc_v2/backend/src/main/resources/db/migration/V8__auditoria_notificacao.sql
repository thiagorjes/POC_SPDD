-- Append-only e preservada mesmo apos a exclusao da tarefa: sem FK para tarefa.
CREATE TABLE auditoria_tarefa (
    id             UUID PRIMARY KEY,
    tarefa_id      UUID NOT NULL,
    projeto_id     UUID NOT NULL,
    autor_id       UUID NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    campo          VARCHAR(20) NOT NULL CHECK (campo IN ('RESPONSAVEL', 'TITULO', 'ETAPA', 'IMPEDIMENTO')),
    valor_anterior VARCHAR(500),
    valor_novo     VARCHAR(500),
    ocorrido_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_tarefa ON auditoria_tarefa (tarefa_id, ocorrido_em DESC);

CREATE TABLE notificacao (
    id              UUID PRIMARY KEY,
    destinatario_id UUID NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tarefa_id       UUID NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    projeto_id      UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    tipo            VARCHAR(30) NOT NULL CHECK (tipo IN (
        'ETAPA_ALTERADA', 'IMPEDIMENTO_MARCADO', 'IMPEDIMENTO_DESMARCADO')),
    mensagem        VARCHAR(500) NOT NULL,
    criada_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    lida_em         TIMESTAMPTZ
);

CREATE INDEX idx_notificacao_destinatario ON notificacao (destinatario_id, lida_em);
