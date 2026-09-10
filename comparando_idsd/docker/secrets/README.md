# Segredos montados

`infra/docker/architecture.md` §7 obriga credencial a entrar por **arquivo
montado**, nunca por variável de ambiente — variável vaza em `docker inspect`,
em log de crash e em qualquer dump de processo.

O arquivo `banco-senha.dev` carrega valor **descartável** e está versionado de
propósito. A mesma seção da norma permite isso em desenvolvimento com uma
condição, que é a razão de ele existir: o valor
precisa entrar **pelo mesmo mecanismo** de produção. Caminho de produção que
não é exercitado em desenvolvimento não é caminho testado, e a alternativa —
variável de ambiente só em desenvolvimento — foi recusada por ACH-02.

Fora de desenvolvimento, `BANCO_SENHA_FILE` aponta para um arquivo **fora do
repositório**. Nada muda do lado da
aplicação: ela continua lendo `/run/secrets/banco-senha`.

Nenhum arquivo sem o sufixo `.dev` é versionado — ver `.gitignore` deste
diretório.
