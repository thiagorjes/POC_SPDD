CREATE TABLE tarefa (
    id             UUID PRIMARY KEY,
    projeto_id     UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    workflow_id    UUID NOT NULL REFERENCES workflow (id) ON DELETE RESTRICT,
    etapa_id       UUID NOT NULL REFERENCES etapa (id) ON DELETE RESTRICT,
    raia_id        UUID NOT NULL REFERENCES raia (id) ON DELETE RESTRICT,
    responsavel_id UUID REFERENCES usuario (id) ON DELETE SET NULL,
    criador_id     UUID NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    titulo         VARCHAR(200) NOT NULL,
    descricao      VARCHAR(4000),
    tipo           VARCHAR(20) NOT NULL CHECK (tipo IN ('FEATURE', 'BUG', 'TAREFA', 'MELHORIA')),
    prioridade     VARCHAR(20) NOT NULL CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')),
    iniciada       BOOLEAN NOT NULL DEFAULT false,
    impedida       BOOLEAN NOT NULL DEFAULT false,
    criada_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tarefa_projeto_etapa ON tarefa (projeto_id, etapa_id);
CREATE INDEX idx_tarefa_projeto_raia ON tarefa (projeto_id, raia_id);

CREATE TABLE tarefa_observador (
    tarefa_id  UUID NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    origem     VARCHAR(20) NOT NULL CHECK (origem IN ('CRIADOR', 'RESPONSAVEL', 'EXPLICITO')),
    PRIMARY KEY (tarefa_id, usuario_id)
);
