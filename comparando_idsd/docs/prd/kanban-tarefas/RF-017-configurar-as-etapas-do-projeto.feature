# language: pt
Funcionalidade: RF-017 — Configurar as etapas do projeto

  Cenário: SCN-017.1 — Fluxo configurado
    Dado que tenho permissão de configuração no projeto
    Quando defino as etapas do projeto, sua ordem e quais são terminais
    Então o fluxo passa a valer para as transições seguintes
    E o histórico já acumulado permanece inalterado

  Cenário: SCN-017.2 — Fluxo sem etapa terminal
    Dado que tenho permissão de configuração no projeto
    Quando tento salvar um fluxo em que nenhuma etapa é terminal
    Então a configuração é recusada
    E o fluxo vigente não é alterado

  Cenário: SCN-017.3 — Remoção de etapa ocupada
    Dado que a etapa Review tem três tarefas
    Quando tento removê-la do fluxo
    Então a remoção é recusada
    E sou informado de que a etapa precisa ser esvaziada antes
    E as três tarefas permanecem em Review
