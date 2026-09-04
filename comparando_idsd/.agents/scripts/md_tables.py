"""
Leitura de secoes e tabelas markdown — utilitario compartilhado pelos scripts
de verificacao das skills.

Existe para que a definicao de "linha de dados de uma tabela" seja uma so.
Quando cada script reimplementa isso, eles divergem em silencio e passam a
aceitar artefatos diferentes.

Uso a partir de um script de skill:

    import sys; from pathlib import Path
    sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts"))
    from md_tables import secao, tabela, campo, preenchido
"""

from __future__ import annotations

import re

VAZIOS = {"", "-", "—", "tbd", "a definir", "n/a", "nenhum", "?"}


def secao(texto: str, titulo: str, nivel: int = 2) -> str:
    """Corpo da secao, ate o proximo cabecalho de nivel igual ou superior.

    Parar apenas no mesmo nivel faria uma subsecao final engolir todo o resto
    do documento, incluindo as tabelas das secoes seguintes.
    """
    marca = "#" * nivel
    limite = "|".join("#" * n + r"\s" for n in range(1, nivel + 1))
    m = re.search(
        rf"^{marca}\s+{re.escape(titulo)}\s*$(.*?)(?=^(?:{limite})|\Z)",
        texto, re.MULTILINE | re.DOTALL,
    )
    return m.group(1) if m else ""


def sem_citacoes(bloco: str) -> str:
    """Remove linhas de citacao, que no template carregam instrucoes."""
    return "\n".join(
        linha for linha in bloco.splitlines() if not linha.lstrip().startswith(">")
    )


def tabela(bloco: str) -> list[list[str]]:
    """Linhas de dados da primeira tabela do bloco, sem cabecalho nem separador."""
    linhas: list[list[str]] = []
    cabecalho_visto = False
    for linha in bloco.splitlines():
        linha = linha.strip()
        if not linha.startswith("|"):
            continue
        # `\|` e pipe literal escapado dentro da celula, nao separador.
        celulas = [
            c.strip().replace("\0", "|")
            for c in linha.strip("|").replace("\\|", "\0").split("|")
        ]
        if all(set(c) <= set("- :") for c in celulas if c):
            continue  # separador
        if not cabecalho_visto:
            cabecalho_visto = True
            continue
        if any(celulas):
            linhas.append(celulas)
    return linhas


def tabela_da_secao(texto: str, titulo: str, nivel: int = 2) -> list[list[str]]:
    return tabela(secao(texto, titulo, nivel))


def campo(texto: str, rotulo: str) -> str:
    """Valor de um campo no formato `- **Rotulo:** valor`.

    O espacamento apos o rotulo e `[ \\t]*` e nao `\\s*`: `\\s` engole a quebra de
    linha e faz um campo vazio capturar a linha seguinte.
    """
    m = re.search(rf"[-*][ \t]*\*\*{re.escape(rotulo)}:?\*\*[ \t]*(.*)", texto)
    return m.group(1).split("<!--")[0].strip() if m else ""


def preenchido(valor: str) -> bool:
    v = valor.strip()
    return bool(v) and v.lower() not in VAZIOS and not v.startswith("{{")


def celula(linha: list[str], i: int) -> str:
    return linha[i] if len(linha) > i else ""


def corpo_util(texto: str) -> str:
    """Texto sem o bloco final de regras negativas, que cita termos proibidos."""
    corte = texto.find("## Fora deste artefato")
    return texto[:corte] if corte != -1 else texto
