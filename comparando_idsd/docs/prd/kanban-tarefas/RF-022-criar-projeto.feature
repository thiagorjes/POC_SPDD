# language: pt
Funcionalidade: RF-022 — Criar projeto

  Cenário: SCN-022.1 — Criação com a primeira participação nomeada
    Dado que sou administrador global
    E que existe uma conta corporativa que ainda não participa de projeto nenhum
    Quando crio um projeto informando o nome e essa pessoa como primeira administradora do projeto
    Então o projeto passa a existir com esse nome
    E essa pessoa consta como participante com o papel de administradora do projeto
    E eu não consto como participante do projeto que criei

  Cenário: SCN-022.2 — Criação recusada a quem não tem o alcance global
    Dado que participo de projetos como administrador de projeto
    E que não sou administrador global
    Quando tento criar um projeto
    Então a criação é recusada com razão explícita
    E nenhum projeto é criado

  Cenário: SCN-022.3 — Projeto nasce sem fluxo e recusa tarefa até ser configurado
    Dado que um projeto acabou de ser criado
    E que seu fluxo ainda não tem etapa nenhuma
    Quando a administradora do projeto tenta criar uma tarefa
    Então a criação é recusada, indicando que o fluxo precisa ser configurado antes
    E depois que ela configura o fluxo a criação da tarefa é aceita
