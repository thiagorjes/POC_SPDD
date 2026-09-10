# language: pt
Funcionalidade: RF-004 — Criar tarefa no projeto

  Cenário: SCN-004.1 — Tarefa nasce no início do fluxo
    Dado que participo de um projeto com fluxo configurado
    Quando crio uma tarefa informando seu título
    Então ela passa a existir na primeira etapa do fluxo
    E sua condição é aguardando tomada
    E ela não tem responsável individual

  Cenário: SCN-004.2 — Criação sem título
    Dado que participo de um projeto com fluxo configurado
    Quando tento criar uma tarefa sem informar o título
    Então a criação é recusada
    E a razão da recusa me é informada

  Cenário: SCN-004.3 — Projeto sem fluxo configurado
    Dado que participo de um projeto cujo fluxo ainda não foi configurado
    Quando tento criar uma tarefa
    Então a criação é recusada
    E sou informado de que o fluxo do projeto precisa ser configurado antes
