# language: pt
Funcionalidade: RF-020 — Refletir no board as alterações feitas por outras pessoas

  Cenário: SCN-020.1 — Movimentação feita por outra pessoa
    Dado que estou com o board do projeto aberto
    Quando outra pessoa move uma tarefa de Desenvolvimento para Review
    Então a tarefa passa a aparecer em Review no meu board
    E eu não precisei recarregar a página

  Cenário: SCN-020.2 — Chegada à minha fila
    Dado que estou com minha fila aberta
    E que nada aguardava tomada por mim
    Quando uma tarefa é movida para uma etapa em que sou participante
    Então ela passa a aparecer na minha fila com a espera correndo
    E eu não precisei recarregar a página

  Cenário: SCN-020.3 — Perda em ação concorrente
    Dado que estou com o board aberto e vejo uma tarefa aguardando tomada
    E que outra pessoa a assumiu um instante antes de mim
    Quando tento assumi-la
    Então minha ação é recusada
    E vejo quem assumiu e desde quando
    E o board passa a exibir a tarefa em curso com o responsável correto
