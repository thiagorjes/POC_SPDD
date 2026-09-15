-- A role de LOGIN da aplicacao, que faltava.
--
-- Fecha ACH-01 da revisao de TASK-02.3 e as pendencias 21 e 22. Ate aqui o
-- grupo `aplicacao_kanban` existia com o conjunto certo de privilegios e
-- ninguem conectava por ele: a aplicacao usava o `POSTGRES_USER` da imagem,
-- superusuario e dono do schema, contra quem toda revogacao e inerte. RNF-008
-- ficava garantido so pela ausencia de metodo nos repositorios, e o contrato
-- `sessao-e-projetos.md` prometia a garantia de banco que nao existia.
--
-- O que torna a garantia real nao e o GRANT — ele ja estava certo. E esta
-- role: sem SUPERUSER, sem posse de objeto nenhum, recebendo tudo o que pode
-- fazer por pertencimento ao grupo.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kanban_app') THEN
        -- Sem senha aqui, de proposito: segredo nao entra em arquivo
        -- versionado. A senha e atribuida pelo callback `afterMigrate`, que
        -- recebe o valor do arquivo montado — mesmo caminho que a senha do
        -- proprio Flyway ja percorre.
        --
        -- NOSUPERUSER e explicito, e nao herdado do padrao: e a propriedade da
        -- qual a garantia inteira depende, e propriedade da qual algo depende
        -- se escreve.
        CREATE ROLE kanban_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT;
    END IF;
END
$$;

-- INHERIT e o que faz o pertencimento bastar: sem ele, a aplicacao precisaria
-- de SET ROLE a cada conexao, e uma conexao que esquecesse o SET ROLE ficaria
-- sem privilegio nenhum — falha de configuracao aparecendo como defeito de
-- produto.
GRANT aplicacao_kanban TO kanban_app;

-- A role nao e dona de nada, e nao deve poder criar nada: objeto criado por
-- ela nasceria com ela como dona, e dono reconcede a si mesmo o que foi
-- revogado — que e exatamente o buraco que esta migration fecha.
REVOKE CREATE ON SCHEMA public FROM kanban_app;
REVOKE CREATE ON SCHEMA public FROM aplicacao_kanban;

-- Toda tabela nova precisa conceder explicitamente ao grupo, como as
-- migrations anteriores ja fazem tabela a tabela. `ALTER DEFAULT PRIVILEGES`
-- ficou fora de proposito: concessao automatica e o oposto do que este desenho
-- quer — o conjunto do que a aplicacao pode fazer e declarado, tabela a
-- tabela, e revisavel num lugar so. O preco e que uma migration futura que
-- esqueca o GRANT quebra a aplicacao; e o preco certo, porque quebra na hora e
-- nao em silencio.

-- A ressalva que ACH-02 mandou levar ao catalogo deixa de valer: a segunda
-- perna da garantia entra em vigor com esta role.
COMMENT ON TABLE evento_tarefa IS
    'Anel de verdade: log somente de insercao (SDR-001). Sem UPDATE e sem '
    'DELETE, e a garantia e dupla: nenhum metodo de repositorio os expoe, e a '
    'role de aplicacao (kanban_app, membro de aplicacao_kanban, sem SUPERUSER '
    'e sem posse de objeto) recebe apenas SELECT, INSERT. Nao ha FK para '
    'tarefa: o log sobrevive a projecao, que e descartavel e refeita do zero.';

-- `COMMENT ON ROLE` continua fora, pela razao que a migration de ordem 4 ja
-- registrou: exige superusuario, e migration que so aplica sob superusuario
-- contradiz o desenho que ela instala.
