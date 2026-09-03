-- V1: catalogo fechado de RBAC (BDR-001) + usuarios provisionados via Keycloak (ADR-003).

CREATE TABLE usuario (
    id            UUID PRIMARY KEY,
    keycloak_sub  VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(320) NOT NULL UNIQUE,
    nome          VARCHAR(255) NOT NULL,
    admin_global  BOOLEAN      NOT NULL DEFAULT false,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE papel (
    id        UUID PRIMARY KEY,
    codigo    VARCHAR(64)  NOT NULL UNIQUE,
    nome      VARCHAR(128) NOT NULL,
    protegido BOOLEAN      NOT NULL DEFAULT false,
    global    BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE permissao (
    id        UUID PRIMARY KEY,
    codigo    VARCHAR(64)  NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL
);

CREATE TABLE papel_permissao (
    papel_id     UUID NOT NULL REFERENCES papel (id) ON DELETE CASCADE,
    permissao_id UUID NOT NULL REFERENCES permissao (id) ON DELETE RESTRICT,
    PRIMARY KEY (papel_id, permissao_id)
);

-- Catalogo fechado de permissoes (RF-013 / BDR-001)
INSERT INTO permissao (id, codigo, descricao) VALUES
    (gen_random_uuid(), 'projeto:administrar',  'Administrar configuracao e ciclo de vida do projeto'),
    (gen_random_uuid(), 'projeto:visualizar',   'Visualizar o projeto e seu board'),
    (gen_random_uuid(), 'workflow:gerenciar',   'Gerenciar workflows, etapas e transicoes'),
    (gen_random_uuid(), 'raia:gerenciar',       'Gerenciar raias do projeto'),
    (gen_random_uuid(), 'usuario:associar',     'Associar usuarios e papeis ao projeto'),
    (gen_random_uuid(), 'tarefa:gerenciar',     'Criar, editar e excluir tarefas'),
    (gen_random_uuid(), 'tarefa:mover',         'Mover tarefas entre etapas e raias'),
    (gen_random_uuid(), 'tarefa:finalizar',     'Mover tarefa para a etapa final e desfinalizar'),
    (gen_random_uuid(), 'tarefa:impedir',       'Marcar e desmarcar impedimento'),
    (gen_random_uuid(), 'tarefa:atribuir',      'Atribuir tarefa a terceiros'),
    (gen_random_uuid(), 'dashboard:visualizar', 'Visualizar o dashboard de lead-time');

-- Catalogo fechado de papeis (BDR-001)
INSERT INTO papel (id, codigo, nome, protegido, global) VALUES
    (gen_random_uuid(), 'admin',         'Administrador Global', true,  true),
    (gen_random_uuid(), 'project_admin', 'Administrador do Projeto', false, false),
    (gen_random_uuid(), 'product_owner', 'Product Owner', false, false),
    (gen_random_uuid(), 'dev',           'Desenvolvedor', false, false),
    (gen_random_uuid(), 'gestor',        'Gestor', false, false),
    (gen_random_uuid(), 'user',          'Usuario', false, false);

-- Mapeamento default papel -> permissoes.
-- project_admin: tudo (criacao/edicao de papeis e permissoes nao existe como operacao).
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p, permissao pe WHERE p.codigo = 'project_admin';

-- product_owner
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p, permissao pe
WHERE p.codigo = 'product_owner'
  AND pe.codigo IN ('projeto:visualizar', 'tarefa:gerenciar', 'tarefa:mover',
                    'tarefa:finalizar', 'tarefa:impedir', 'tarefa:atribuir',
                    'dashboard:visualizar');

-- dev: sem tarefa:finalizar por default (RN-011)
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p, permissao pe
WHERE p.codigo = 'dev'
  AND pe.codigo IN ('projeto:visualizar', 'tarefa:gerenciar', 'tarefa:mover',
                    'tarefa:impedir', 'dashboard:visualizar');

-- gestor: sem tarefa:impedir por default (RN-013)
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p, permissao pe
WHERE p.codigo = 'gestor'
  AND pe.codigo IN ('projeto:visualizar', 'dashboard:visualizar');

-- user: nenhuma permissao (RN-014) — intencionalmente sem INSERT.
