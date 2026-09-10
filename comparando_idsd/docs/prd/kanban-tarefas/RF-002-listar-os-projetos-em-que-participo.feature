# language: pt
Funcionalidade: RF-002 — Listar os projetos em que participo

  Cenário: SCN-002.1 — Projetos com a permissão de cada um
    Dado que participo de três projetos com permissões diferentes
    Quando consulto meus projetos
    Então vejo os três
    E em cada um vejo qual permissão eu tenho ali

  Cenário: SCN-002.2 — Nenhum projeto
    Dado que não participo de nenhum projeto
    Quando consulto meus projetos
    Então vejo que não participo de nenhum projeto
    E nenhum projeto do sistema me é exibido

  Cenário: SCN-002.3 — Projeto sem participação
    Dado que existe um projeto do qual não participo
    Quando tento acessá-lo diretamente
    Então o acesso é recusado
    E nenhum dado daquele projeto me é revelado

  Cenário: SCN-002.4 — Projeto ainda sem fluxo aparece marcado
    Dado que participo de dois projetos
    E que um deles ainda não tem etapa alguma configurada
    Quando consulto meus projetos
    Então vejo os dois
    E o que ainda não tem etapa alguma vem marcado como fluxo não configurado
    E o outro não vem marcado
