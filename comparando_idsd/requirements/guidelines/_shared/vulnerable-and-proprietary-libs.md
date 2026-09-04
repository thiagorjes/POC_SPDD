# Bibliotecas Vulneráveis e Proprietárias (transversal)

> Política de dependências: o que é proibido, o que exige aprovação e como as libs internas
> são consumidas. Vale para qualquer stack; a ferramenta de scan concreta fica em
> `backend/<stack>/*` (ex.: SonarQube, OWASP Dependency-Check, `npm audit`).

## 1. Política de CVE

| Severidade da CVE (CVSS) | Política |
|---|---|
| **Critical** (9.0–10.0) | **Bloqueia o build/merge.** Atualizar para versão corrigida ou remover a dependência antes de seguir. |
| **High** (7.0–8.9) | **Bloqueia o merge.** Correção obrigatória; exceção só com waiver formal (ver §3) e prazo. |
| Medium (4.0–6.9) | Registrar e corrigir na próxima janela de manutenção; não bloqueia. |
| Low (< 4.0) | Backlog. |

Regras:

- Toda dependência (direta e **transitiva**) passa por SCA no pipeline. Falha de scan = falha de build.
- Vale para libs de runtime e de build/test (plugins, geradores).
- Proibido fixar versão em cima de artefato com CVE Critical/High conhecida "temporariamente"
  sem waiver.
- `log4j 1.x` e `log4j 2.x` vulnerável (Log4Shell e correlatas), `commons-collections` vulnerável a
  desserialização, e qualquer versão explicitamente marcada pelo scanner corporativo são **proibidas**.
- Algoritmos/criptografia fraca (MD5, SHA-1, DES, RC4) proibidos independentemente da lib.

## 2. Bibliotecas proprietárias / internas

- Só podem ser resolvidas pelo **repositório de artefatos corporativo (Nexus)** —
  `nexus3-cicd-tools.cloud.sfb` (releases e snapshots). Proibido baixar de repositório público
  ou copiar `.jar`/pacote solto para dentro do projeto.
- Toda lib interna é declarada com **versão fixa** (sem range, sem `latest`, sem `+`).
- `SNAPSHOT` de lib interna é permitido só em branch de desenvolvimento; **release não pode
  depender de SNAPSHOT**.
- Consumir sempre a versão recomendada/homologada pelo Nexus corporativo quando houver.

### Registro de libs proprietárias em uso

| Lib | Versão | Finalidade | Origem |
|---|---|---|---|
| `banestes-token-rhsso` | 2.1.0 | Obtenção e transcode de tokens RH-SSO (ver [`api-security.md`](api-security.md)) | Nexus |

> Manter esta tabela sincronizada com o `stack.md` de cada stack. Ao adicionar/atualizar uma
> lib interna, atualizar as duas.

## 3. Waiver (exceção temporária)

Uma dependência com CVE High só permanece com:

- Justificativa técnica (por que não dá para atualizar/remover agora)
- Avaliação de exploração no contexto da aplicação (o caminho vulnerável é alcançável?)
- Mitigação compensatória aplicada (WAF, config, isolamento)
- **Data limite** para remoção e responsável
- Aprovação do time de segurança

Waivers ficam versionados no repositório (ex.: `security/waivers.md` ou supressão explícita e
comentada do scanner) e são revisados a cada release.

## 4. Higiene contínua

- Atualização de dependências agendada (Renovate/Dependabot ou equivalente) com revisão humana.
- Remover dependência não utilizada (o scanner e o `depgraph` apontam).
- Preferir menos dependências: não adicionar lib para o que a plataforma/linguagem já resolve.
- Fixar versões (lockfile commitado) para builds reprodutíveis.

---

## Checklist

- [ ] SCA rodando no pipeline sobre deps diretas e transitivas; scan falho = build falho
- [ ] Zero CVE Critical/High sem correção ou sem waiver aprovado e datado
- [ ] Sem `log4j` vulnerável / cripto fraca
- [ ] Libs internas só do Nexus corporativo, versão fixa, release sem SNAPSHOT
- [ ] Tabela de libs proprietárias e `stack.md` sincronizados
- [ ] Lockfile commitado; dependências órfãs removidas
