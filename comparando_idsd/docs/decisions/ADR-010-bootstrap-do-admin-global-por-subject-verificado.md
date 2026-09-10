# ADR-010 — Bootstrap do admin global por subject verificado, com promoção única e auditada

- **Status:** Aceito
- **Data:** 2026-09-09
- **Supera:** ADR-007
- **Origem:** comitê de análise da `/techspec` — achado do security

---

## Contexto

ADR-007 resolveu o problema real de que não há a quem autorizar antes do primeiro
projeto existir, e a solução — flag fora do vínculo por projeto, populada no
provisionamento JIT — continua correta. Duas coisas nele não sobrevivem à revisão.

**A chave de promoção é o e-mail.** A identidade do `usuario` neste sistema é o `sub`
do token (`subject_id UNIQUE`), mas ADR-007 promove casando a claim `email` com uma
property de configuração. E-mail é mutável no provedor e, em realm com autocadastro,
federação ou IdP externo encadeado, é atribuível por quem se registra. Nada exigia
`email_verified`, nada limitava a promoção a um único uso, e o autoprovisionamento de
ADR-006 acontece na mesma chamada sem gate. Somados, o caminho da conta zero ao bypass
universal de RBAC é uma requisição.

**O ADR é texto herdado da stack anterior.** Cita rotas `/api/...`,
`UsuarioProjetoPapel`, um contrato `projetos.md` que não existe nesta árvore, e RF-008
e RN-015 com numeração que não corresponde ao PRD v1.0 — onde RN-015 trata de gestor
somente-leitura, não de projeto finalizado. Adotá-lo sem reconciliar é importar
afirmações falsas sobre este sistema.

## Decisão

**1. A promoção é por `subject_id`, não por e-mail.** A property de bootstrap passa a
conter o `sub` do provedor. Não há caminho de promoção por claim que o titular da
conta possa influenciar.

**2. A promoção é única.** Se já existe algum `usuario` com `admin_global = true`, a
promoção não ocorre — nem para o `sub` configurado. Falha explícita e registrada, não
silenciosa.

**3. Toda promoção é evento de auditoria em `WARN`**, com `sub`, instante e origem da
configuração.

**4. O bypass passa a ser especificado, não implícito.** A TechSpec Seção 8 e os
contratos declaram o que `admin_global` contorna: ele enxerga e administra qualquer
projeto, e portanto `GET /v1/projetos` devolve para ele todos os projetos, com
indicação de que o acesso é por administração global e não por participação. O que
ele **não** contorna: RN-014 (nenhuma agregação por pessoa existe para ninguém, porque
a coluna não existe) e RNF-008 (o log é imutável no nível da role de banco).

**5. `admin_global` continua sem endpoint de escrita.** Promover outra pessoa exige
alteração direta em banco, como em ADR-007 — decisão operacional deliberada.

## Motivação

Os itens 1 a 3 fecham a escalada. O item 4 é o que o comitê apontou como mais grave a
médio prazo: o mecanismo de autorização mais poderoso do sistema era o único sem
especificação, sem linha na matriz de rastreabilidade e sem teste. Um bypass que não
está escrito não é revisável, e o primeiro lugar em que ele apareceria é um incidente.

O item 4 também expõe uma lacuna que **não** se resolve aqui: não há cenário de aceite
congelado cobrindo o comportamento do admin global, e acrescentá-lo é emenda a artefato
aprovado no GATE-SPEC. Fica registrado como questão para o `/analyze --pre-tasks`, que
é a etapa que existe para achar escopo órfão.

## Alternativas descartadas

**Manter o e-mail, exigindo `email_verified` e domínio allowlisted.** Reduz o risco sem
eliminá-lo: continua dependendo de configuração correta do realm, que não é governada
por este sistema. O `sub` não depende de nada.

**Papel `admin` em tabela, com `projeto_id` nulo.** É a alternativa 2 do próprio
ADR-007, descartada lá pelo mesmo motivo que continua válido: quebra a PK composta.

**Seed em migration.** Descartada em ADR-007 porque o `sub` não seria previsível antes
do primeiro login. Com o item 1 o argumento se inverte — o `sub` passa a ser
conhecido —, mas o seed reintroduz credencial de ambiente no versionamento de schema, e
a property externa resolve igual sem isso.

## Consequências

- A property de produção muda de e-mail para `sub`, e o runbook de deploy precisa
  dizer onde obtê-lo no console do provedor. É um passo a mais na implantação, e é
  intencional: a promoção deixa de ser algo que acontece por alguém logar.
- O ambiente de desenvolvimento fixa o `sub` do usuário de teste no realm importado.
- ADR-007 fica marcado como superado e não é apagado.
