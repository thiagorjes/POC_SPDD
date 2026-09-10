# language: pt
Funcionalidade: RF-019 — Gerir participação e permissões do projeto

  Cenário: SCN-019.1 — Concessão de participação
    Dado que tenho permissão de configuração no projeto
    Quando concedo a uma pessoa participação com permissão de escrita
    Então ela passa a ver o projeto entre os seus
    E passa a poder agir sobre as tarefas dali em diante

  Cenário: SCN-019.2 — Remoção de quem tem tarefa assumida
    Dado que uma pessoa assumiu uma tarefa na etapa Review
    Quando removo sua participação do projeto
    Então a tarefa volta a aguardar tomada na etapa Review
    E o registro de que ela havia assumido é preservado no histórico

  Cenário: SCN-019.3 — Projeto sem papel de desbloqueio
    Dado que o projeto não tem ninguém com o papel de desbloqueio
    Quando uma tarefa é declarada impedida
    Então o impedimento é registrado
    E ele se destaca para todos os participantes do projeto

  Cenário: SCN-019.4 — Acompanhamento em tempo real após remoção da participação
    Dado que uma pessoa está com o board do projeto aberto acompanhando as alterações
    Quando removo sua participação do projeto
    Então ela deixa de receber as alterações do projeto sem precisar fechar nem recarregar a página
    E uma nova consulta ao projeto lhe é recusada
