# language: pt
Funcionalidade: RF-015 — Consultar o andamento do projeto

  Cenário: SCN-015.1 — Andamento do projeto
    Dado que participo de um projeto com tarefas em várias etapas
    E que duas delas estão impedidas
    Quando consulto o andamento do projeto
    Então vejo quantas tarefas há em cada etapa
    E vejo as duas tarefas impedidas e há quanto tempo cada uma está assim

  Cenário: SCN-015.2 — Consulta por quem só tem leitura
    Dado que tenho acesso somente-leitura ao projeto
    Quando consulto o andamento do projeto
    Então vejo o andamento
    E nenhuma ação de escrita me é apresentada
    E qualquer escrita que eu solicite por outro caminho é recusada

  Cenário: SCN-015.3 — Impedimento parado há muito tempo
    Dado que uma tarefa está impedida há cinco dias
    Quando consulto o andamento do projeto
    Então vejo a tarefa impedida com os cinco dias acumulados à mostra
    E nenhuma ação automática foi disparada por causa do tempo decorrido
