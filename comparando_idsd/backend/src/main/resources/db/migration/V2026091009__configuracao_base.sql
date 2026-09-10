-- Anel de configuracao: quem existe, onde participa e com que papeis.
-- Base de autorizacao de todo o sistema (BDR-001).
--
-- Migration aplicada nunca e alterada: correcao vem em migration nova.

CREATE TABLE usuario (
    id           uuid        PRIMARY KEY,
    -- Identificador do sujeito no token. E a chave de vinculo, e nunca o
    -- e-mail: e-mail e mutavel no provedor e, em realm com autocadastro,
    -- atribuivel por quem se registra.
    subject_id   text        NOT NULL UNIQUE,
    -- Espelhados do token a cada entrada. Nao ha senha nem cadastro local
    -- (ADR-006): a conta nasce por autoprovisionamento na primeira entrada.
    nome         text        NOT NULL,
    email        text        NOT NULL,
    -- Fora do vinculo por projeto: alcance de escopo, nao participacao
    -- (ADR-010).
    admin_global boolean     NOT NULL DEFAULT false,
    criado_em    timestamptz NOT NULL
);

CREATE TABLE projeto (
    id        uuid        PRIMARY KEY,
    nome      text        NOT NULL,
    descricao text,
    criado_em timestamptz NOT NULL,
    -- Contador de sequencia de eventos do projeto (SDR-004). Nasce aqui sem
    -- uso, e de proposito: a coluna vive em `projeto` e nao em sequence do
    -- banco porque sequence nao e transacional — transacao revertida deixaria
    -- buraco permanente no `seq` e todo cliente resincronizaria para sempre.
    -- Introduzi-la agora evita alterar depois a migration que a cria.
    seq_atual bigint      NOT NULL DEFAULT 0
);

CREATE TABLE participacao (
    id         uuid        PRIMARY KEY,
    usuario_id uuid        NOT NULL REFERENCES usuario (id),
    projeto_id uuid        NOT NULL REFERENCES projeto (id),
    criada_em  timestamptz NOT NULL,
    -- Uma participacao por par. Os papeis acumulam dentro dela, nunca por
    -- linhas repetidas de participacao.
    CONSTRAINT participacao_usuario_projeto_unico UNIQUE (usuario_id, projeto_id)
);

-- Papeis acumulaveis no mesmo projeto (BDR-001). Nao ha tabela de catalogo de
-- papel: o catalogo e enumeracao em codigo, e essa e a diferenca que impede
-- alguem de compor permissao nova em runtime.
CREATE TABLE participacao_papel (
    participacao_id uuid NOT NULL REFERENCES participacao (id),
    papel           text NOT NULL,
    PRIMARY KEY (participacao_id, papel)
);
