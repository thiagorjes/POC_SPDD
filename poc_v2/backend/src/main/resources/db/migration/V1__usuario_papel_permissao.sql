-- Bloco 1/2 das Operations: catalogo fechado de papeis e permissoes (BDR-001, RN-006, RN-014).

CREATE TABLE usuario (
    id            UUID PRIMARY KEY,
    keycloak_sub  VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(320) NOT NULL UNIQUE,
    nome          VARCHAR(255) NOT NULL,
    admin_global  BOOLEAN NOT NULL DEFAULT false,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE papel (
    id        UUID PRIMARY KEY,
    codigo    VARCHAR(60) NOT NULL UNIQUE,
    nome      VARCHAR(120) NOT NULL,
    protegido BOOLEAN NOT NULL DEFAULT false,
    global    BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE permissao (
    id        UUID PRIMARY KEY,
    codigo    VARCHAR(60) NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL
);

CREATE TABLE papel_permissao (
    papel_id     UUID NOT NULL REFERENCES papel (id) ON DELETE CASCADE,
    permissao_id UUID NOT NULL REFERENCES permissao (id) ON DELETE RESTRICT,
    PRIMARY KEY (papel_id, permissao_id)
);

-- Seed: catalogo fechado de papeis (BDR-001).
INSERT INTO papel (id, codigo, nome, protegido, global) VALUES
    (gen_random_uuid(), 'admin',         'Administrador Global', true,  true),
    (gen_random_uuid(), 'project_admin', 'Administrador do Projeto', false, false),
    (gen_random_uuid(), 'product_owner', 'Product Owner', false, false),
    (gen_random_uuid(), 'dev',           'Desenvolvedor', false, false),
    (gen_random_uuid(), 'gestor',        'Gestor', false, false),
    (gen_random_uuid(), 'user',          'Usuario', false, false);

-- Seed: catalogo fechado de permissoes (RF-013, BDR-001).
INSERT INTO permissao (id, codigo, descricao) VALUES
    (gen_random_uuid(), 'projeto:administrar',  'Administrar o projeto'),
    (gen_random_uuid(), 'projeto:visualizar',   'Visualizar o projeto e seu board'),
    (gen_random_uuid(), 'workflow:gerenciar',   'Gerenciar workflows, etapas e transicoes'),
    (gen_random_uuid(), 'raia:gerenciar',       'Gerenciar raias'),
    (gen_random_uuid(), 'usuario:associar',     'Associar usuarios e papeis ao projeto'),
    (gen_random_uuid(), 'tarefa:gerenciar',     'Criar, editar e excluir tarefas'),
    (gen_random_uuid(), 'tarefa:mover',         'Mover tarefas entre etapas'),
    (gen_random_uuid(), 'tarefa:finalizar',     'Mover para a etapa final e desfinalizar'),
    (gen_random_uuid(), 'tarefa:impedir',       'Marcar e desmarcar impedimento'),
    (gen_random_uuid(), 'tarefa:atribuir',      'Atribuir tarefa a terceiros'),
    (gen_random_uuid(), 'dashboard:visualizar', 'Visualizar o dashboard de lead-time');

-- Seed: mapeamento default papel -> permissoes (bloco 2 das Operations).
-- project_admin: todas as permissoes do catalogo (criacao de papeis/permissoes nao existe como operacao).
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p CROSS JOIN permissao pe WHERE p.codigo = 'project_admin';

-- product_owner
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p JOIN permissao pe ON pe.codigo IN (
    'projeto:visualizar', 'tarefa:gerenciar', 'tarefa:mover', 'tarefa:finalizar',
    'tarefa:impedir', 'tarefa:atribuir', 'dashboard:visualizar')
WHERE p.codigo = 'product_owner';

-- dev: sem tarefa:finalizar por default (RN-011).
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p JOIN permissao pe ON pe.codigo IN (
    'projeto:visualizar', 'tarefa:gerenciar', 'tarefa:mover',
    'tarefa:impedir', 'dashboard:visualizar')
WHERE p.codigo = 'dev';

-- gestor: sem tarefa:impedir por default (RN-013).
INSERT INTO papel_permissao (papel_id, permissao_id)
SELECT p.id, pe.id FROM papel p JOIN permissao pe ON pe.codigo IN (
    'projeto:visualizar', 'dashboard:visualizar')
WHERE p.codigo = 'gestor';

-- user: nenhuma permissao (RN-014).
