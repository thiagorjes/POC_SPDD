"""
Verificacao da biblioteca de guidelines — skill /guidelines.

A biblioteca e compartilhada entre sistemas e organizada por stack:

    guidelines/
      _shared/            transversais, agnosticas de stack
      _templates/         molde de colecao nova
      <camada>/
        _shared/          transversais da camada (opcional)
        <stack>/          a colecao concreta

A linha que separa falha de aviso e o que este script consegue decidir:

  FALHA  invariante provavel a partir do disco — arquivo obrigatorio ausente,
         colecao sem cross-link para o transversal, indice fora de sincronia,
         colecao declarada e inexistente, DR citado e ausente, design-system.md
         sem design-tokens.json, versao nao fixada.

  AVISO  cheiro na prosa — hedge, stub declarado. Sao sinais para quem revisa,
         nao vereditos.

A regra editorial do template ("todo arquivo termina com checklist acionavel",
"nao escreva recomenda-se") nao virou check de reprovacao de proposito. Uma
tentativa anterior exigia a sintaxe `- [ ]` e reprovava os dois `testing.md` da
biblioteca, que terminam em lista numerada imperativa — cumprem a intencao com
outra sintaxe. Check que reprova arquivo bem escrito e pior que check nenhum:
ensina a ignorar o validador. Reviewability nao e decidivel por regex; o que e
decidivel esta na lista de FALHA acima.

Uso:
    python check_guidelines.py [--raiz guidelines] [--sistema .]
    python check_guidelines.py --colecao frontend/nextjs
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
from md_tables import corpo_util  # noqa: E402

OBRIGATORIOS = [
    "stack.md", "architecture.md", "coding-standards.md",
    "testing.md", "definition-of-done.md",
]

# Nao e lista fechada de arquivos permitidos: e o que o template reconhece como
# condicional. Arquivo fora dela nao reprova — a biblioteca precisa aceitar
# convencao de stack que ainda nao existe.
CONDICIONAIS = {
    "database.md", "integrations.md", "openapi-swagger.md", "sonarqube.md",
    "security.md", "design-system.md", "README.md",
}

# Formulacoes que transformam norma em preferencia. Aviso, nao falha: a varredura
# e por linha e nao distingue a oracao principal da subordinada — em
# `frontend/_shared/design-principles.md` o "idealmente" qualifica o mecanismo de
# reforco, nao a norma, que e dura. Quem le o aviso decide; o script nao tem como.
#
# A secao "Recomendados" e cortada antes da varredura: ali a palavra nomeia um
# nivel declarado no definition-of-done, nao uma vaguidade.
HEDGE = re.compile(
    r"\b(recomenda-se|sempre que poss[íi]vel|de prefer[êe]ncia|idealmente|"
    r"evite se poss[íi]vel|se poss[íi]vel|quando fizer sentido)\b",
    re.IGNORECASE,
)

DR = re.compile(r"\b(ADR|BDR|SDR|DDR)-(\d{3})\b")
STUB = re.compile(r"status:\s*\**\s*n[ãa]o\s+elaborada", re.IGNORECASE)
LINK_SHARED = re.compile(r"_shared/[\w-]+\.md")
LATEST = re.compile(r"\blatest\b", re.IGNORECASE)
NEGACAO = re.compile(r"\b(sem|n[ãa]o|proib|nunca|evitar)\b", re.IGNORECASE)


def sem_recomendados(texto: str) -> str:
    """Corta a secao 'Recomendados', onde a palavra nomeia um nivel declarado."""
    return re.sub(r"^##+\s*Recomendados.*?(?=^##\s|\Z)", "", texto,
                  flags=re.MULTILINE | re.DOTALL | re.IGNORECASE)


def verificar_arquivo(arquivo: Path, rotulo: str,
                      decisoes: set[str]) -> tuple[list[str], list[str]]:
    texto = arquivo.read_text(encoding="utf-8")
    util = sem_recomendados(corpo_util(texto))
    erros: list[str] = []
    avisos: list[str] = []

    for n, linha in enumerate(util.splitlines(), 1):
        if HEDGE.search(linha):
            avisos.append(
                f"{rotulo}:{n}: norma possivelmente escrita como sugestao. Se a "
                f"ressalva vale para a norma, declare a excecao e quem autoriza; "
                f"se qualifica so um detalhe, ignore."
            )

    # "Versoes sempre fixas; sem latest" (template). A negacao e excluida porque
    # o transversal cita 'latest' justamente para proibi-lo.
    if arquivo.name == "stack.md":
        for n, linha in enumerate(util.splitlines(), 1):
            if LATEST.search(linha) and not NEGACAO.search(linha):
                erros.append(f"{rotulo}:{n}: versao nao fixa ('latest').")

    for tipo, num in DR.findall(util):
        if f"{tipo}-{num}" not in decisoes:
            erros.append(f"{rotulo}: cita {tipo}-{num}, inexistente em docs/decisions/.")

    if "{{" in util:
        erros.append(f"{rotulo}: placeholder do template nao substituido.")

    return erros, avisos


def verificar_colecao(pasta: Path, raiz: Path, decisoes: set[str]) -> tuple[list[str], list[str]]:
    rel = pasta.relative_to(raiz).as_posix()
    erros: list[str] = []
    avisos: list[str] = []

    readme = pasta / "README.md"
    if readme.exists() and STUB.search(readme.read_text(encoding="utf-8")):
        avisos.append(f"{rel}: colecao declarada como nao elaborada (stub).")
        return erros, avisos

    for nome in OBRIGATORIOS:
        if not (pasta / nome).exists():
            erros.append(f"{rel}: arquivo obrigatorio ausente: {nome}")

    # design-system.md sem os tokens legiveis por maquina deixa o /design sem
    # fonte utilizavel: ele passaria a inferir cor e espacamento do texto.
    if (pasta / "design-system.md").exists() and not (pasta / "design-tokens.json").exists():
        erros.append(f"{rel}: design-system.md sem design-tokens.json ao lado.")

    arquivos = sorted(pasta.glob("*.md"))
    ligacoes = 0
    for arquivo in arquivos:
        rotulo = f"{rel}/{arquivo.name}"
        e, a = verificar_arquivo(arquivo, rotulo, decisoes)
        erros += e
        avisos += a
        ligacoes += len(LINK_SHARED.findall(arquivo.read_text(encoding="utf-8")))

    # Colecao que nunca referencia o transversal ou o ignora ou o reescreveu. Os
    # dois casos sao o mesmo defeito visto de angulos diferentes.
    if arquivos and ligacoes == 0:
        erros.append(
            f"{rel}: nenhum cross-link para _shared/ — a colecao materializa o "
            f"transversal, nao o reescreve."
        )

    return erros, avisos


def colecoes(raiz: Path) -> list[Path]:
    """Toda pasta <camada>/<stack>, com <camada> aberto e `_shared` fora."""
    achadas = []
    for camada in sorted(raiz.iterdir()):
        if not camada.is_dir() or camada.name.startswith("_"):
            continue
        for stack in sorted(camada.iterdir()):
            if stack.is_dir() and not stack.name.startswith("_"):
                achadas.append(stack)
    return achadas


def verificar_indice(raiz: Path, encontradas: list[Path]) -> list[str]:
    readme = raiz / "README.md"
    if not readme.exists():
        return [f"{raiz.name}/README.md ausente — a biblioteca precisa de indice."]
    texto = readme.read_text(encoding="utf-8")
    erros = []
    for pasta in encontradas:
        rel = pasta.relative_to(raiz).as_posix()
        if rel not in texto:
            erros.append(f"README.md nao indexa a colecao {rel}.")
    for rel in set(re.findall(r"\(([\w-]+/[\w-]+)/\)", texto)):
        if not (raiz / rel).is_dir():
            erros.append(f"README.md indexa {rel}, que nao existe.")
    return erros


def verificar_sistema(sistema: Path, raiz: Path) -> tuple[list[str], Path | None]:
    """Le guidelines.yaml do sistema e confere as colecoes declaradas."""
    manifesto = sistema / "guidelines.yaml"
    if not manifesto.exists():
        return ([f"{sistema.name}: guidelines.yaml ausente — o sistema nao "
                 f"declara quais colecoes o governam."], None)

    import yaml
    dados = yaml.safe_load(manifesto.read_text(encoding="utf-8")) or {}
    declarada = dados.get("raiz")
    alvo = (sistema / declarada).resolve() if declarada else raiz

    erros = []
    if not alvo.is_dir():
        erros.append(f"guidelines.yaml aponta raiz inexistente: {declarada}")
        return erros, None

    lista = dados.get("colecoes") or []
    if not lista and not dados.get("motivo"):
        # Sistema sem colecao e situacao legitima (ferramental, infra), mas
        # precisa dizer por que — senao nao se distingue de esquecimento.
        erros.append(
            "guidelines.yaml nao declara colecao nem 'motivo' — declare a "
            "razao de o sistema nao ter stack governada."
        )
    for rel in lista:
        if not (alvo / rel).is_dir():
            erros.append(f"guidelines.yaml declara '{rel}', que nao existe na biblioteca.")
    return erros, alvo


def descobrir(inicio: Path) -> Path | None:
    """Sobe a arvore procurando uma pasta guidelines/ com _shared/ dentro."""
    for pasta in [inicio, *inicio.parents]:
        alvo = pasta / "guidelines"
        if (alvo / "_shared").is_dir():
            return alvo
    return None


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--raiz", default=None, help="Raiz da biblioteca de guidelines")
    p.add_argument("--sistema", default=None, help="Raiz do sistema que declara guidelines.yaml")
    p.add_argument("--colecao", default=None, help="Verifica apenas <camada>/<stack>")
    args = p.parse_args()

    erros: list[str] = []
    avisos: list[str] = []

    sistema = Path(args.sistema).resolve() if args.sistema else None
    raiz = Path(args.raiz).resolve() if args.raiz else None

    if sistema:
        errs, alvo = verificar_sistema(sistema, raiz or Path.cwd())
        erros += errs
        raiz = raiz or alvo

    raiz = raiz or descobrir(Path.cwd().resolve())
    if raiz is None or not raiz.is_dir():
        print("ERRO: biblioteca de guidelines nao encontrada. Use --raiz.", file=sys.stderr)
        return 1

    decisoes = set()
    base = sistema or raiz.parent
    for f in (base / "docs" / "decisions").glob("*.md"):
        if m := DR.search(f.name):
            decisoes.add(m.group(0))

    if not (raiz / "_shared").is_dir():
        erros.append("_shared/ ausente — sem transversal, toda colecao duplica.")
    if not (raiz / "_templates" / "stack-guidelines-template.md").exists():
        erros.append("_templates/stack-guidelines-template.md ausente.")

    # O transversal e lido por toda colecao: defeito ali se propaga por inteiro.
    if not args.colecao:
        for pasta in sorted(raiz.rglob("_shared")):
            for arquivo in sorted(pasta.glob("*.md")):
                rotulo = arquivo.relative_to(raiz).as_posix()
                e, a = verificar_arquivo(arquivo, rotulo, decisoes)
                erros += e
                avisos += a

    encontradas = colecoes(raiz)
    if args.colecao:
        encontradas = [c for c in encontradas
                       if c.relative_to(raiz).as_posix() == args.colecao]
        if not encontradas:
            print(f"ERRO: colecao '{args.colecao}' nao encontrada em {raiz}.", file=sys.stderr)
            return 1
    else:
        erros += verificar_indice(raiz, encontradas)

    for pasta in encontradas:
        e, a = verificar_colecao(pasta, raiz, decisoes)
        erros += e
        avisos += a

    for a in avisos:
        print(f"AVISO {a}")
    if erros:
        for e in erros:
            print(f"FALHA {e}", file=sys.stderr)
        print(f"\n{len(erros)} falha(s), {len(avisos)} aviso(s).", file=sys.stderr)
        return 1
    print(f"OK - {len(encontradas)} colecao(oes) conforme(s), {len(avisos)} aviso(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
