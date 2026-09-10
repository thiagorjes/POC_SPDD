/* =============================================================
   kanban-tarefas — comportamento compartilhado dos protótipos
   Escopo: só o necessário para que os estados do brief sejam
   observáveis. Nenhuma regra de negócio real.
   ============================================================= */
(function () {
  'use strict';

  /* ---------- Tema claro / escuro (classe .dark na raiz) ---------- */
  var CHAVE = 'kanban-proto-tema';
  function aplicarTema(t) {
    document.documentElement.classList.toggle('dark', t === 'escuro');
    document.querySelectorAll('[data-alternar-tema]').forEach(function (b) {
      b.setAttribute('aria-pressed', String(t === 'escuro'));
      var r = b.querySelector('[data-rotulo-tema]');
      if (r) r.textContent = t === 'escuro' ? 'Tema escuro' : 'Tema claro';
    });
  }
  try { aplicarTema(localStorage.getItem(CHAVE) || 'claro'); } catch (e) { aplicarTema('claro'); }

  document.addEventListener('click', function (ev) {
    var alvo = ev.target.closest('[data-alternar-tema]');
    if (!alvo) return;
    var novo = document.documentElement.classList.contains('dark') ? 'claro' : 'escuro';
    aplicarTema(novo);
    try { localStorage.setItem(CHAVE, novo); } catch (e) {}
    anunciar(novo === 'escuro' ? 'Tema escuro ativado.' : 'Tema claro ativado.');
  });

  /* ---------- Região de anúncio não intrusivo (WCAG / DDR-005) ---------- */
  function regiao() {
    var r = document.getElementById('anuncio-vivo');
    if (!r) {
      r = document.createElement('div');
      r.id = 'anuncio-vivo';
      r.className = 'sr-only';
      r.setAttribute('role', 'status');
      r.setAttribute('aria-live', 'polite');
      document.body.appendChild(r);
    }
    return r;
  }
  function anunciar(t) { regiao().textContent = t; }
  window.anunciar = anunciar;

  /* ---------- Contadores contínuos (QD-03) ----------
     data-desde = minutos já decorridos. O contador nunca some, nunca
     espera limiar: a espera é a dor, escondê-la seria decidir que
     até certo ponto ela não importa.                              */
  function formatar(min) {
    var d = Math.floor(min / 1440), h = Math.floor((min % 1440) / 60), m = Math.floor(min % 60);
    if (d > 0) return d + 'd ' + h + 'h';
    if (h > 0) return h + 'h ' + String(m).padStart(2, '0') + 'min';
    return m + 'min';
  }
  function extenso(min) {
    var d = Math.floor(min / 1440), h = Math.floor((min % 1440) / 60), m = Math.floor(min % 60);
    var p = [];
    if (d) p.push(d + (d === 1 ? ' dia' : ' dias'));
    if (h) p.push(h + (h === 1 ? ' hora' : ' horas'));
    if (!d && m) p.push(m + ' minutos');
    return p.join(' e ') || 'menos de um minuto';
  }
  function tique() {
    document.querySelectorAll('[data-desde]').forEach(function (el) {
      var min = parseFloat(el.getAttribute('data-desde'));
      var passo = parseFloat(el.getAttribute('data-passo') || '0.25');
      min += passo;
      el.setAttribute('data-desde', String(min));
      var v = el.querySelector('[data-valor]') || el;
      v.textContent = formatar(min);
      var prefixo = el.getAttribute('data-prefixo-acessivel') || 'Tempo';
      el.setAttribute('title', prefixo + ': ' + extenso(min));
      var sr = el.querySelector('[data-sr]');
      if (sr) sr.textContent = prefixo + ' de ' + extenso(min) + '.';
    });
  }
  tique();
  setInterval(tique, 15000);

  /* ---------- Board: arrasto e caminho equivalente por teclado ----------
     DDR-002 + DDR-005. As duas rotas usam exatamente a mesma lista de
     etapas alcançáveis, declarada em data-alcancaveis no cartão.      */
  var arrastando = null;

  function etapasDoBoard() { return Array.prototype.slice.call(document.querySelectorAll('.etapa[data-etapa]')); }

  function realcar(cartao) {
    var ok = (cartao.getAttribute('data-alcancaveis') || '').split(',').filter(Boolean);
    etapasDoBoard().forEach(function (col) {
      var id = col.getAttribute('data-etapa');
      var atual = id === cartao.getAttribute('data-etapa-atual');
      col.classList.toggle('alvo-valido', ok.indexOf(id) > -1);
      col.classList.toggle('alvo-invalido', ok.indexOf(id) < 0 && !atual);
    });
  }
  function limparRealce() {
    etapasDoBoard().forEach(function (c) { c.classList.remove('alvo-valido', 'alvo-invalido'); });
  }

  document.addEventListener('dragstart', function (ev) {
    var c = ev.target.closest('.cartao'); if (!c) return;
    arrastando = c; realcar(c);
    anunciar('Movendo ' + c.getAttribute('data-codigo') + '. Etapas alcançáveis realçadas.');
  });
  document.addEventListener('dragend', function () { arrastando = null; limparRealce(); });
  document.addEventListener('dragover', function (ev) { if (arrastando) ev.preventDefault(); });
  document.addEventListener('drop', function (ev) {
    if (!arrastando) return;
    ev.preventDefault();
    var col = ev.target.closest('.etapa[data-etapa]');
    var ok = (arrastando.getAttribute('data-alcancaveis') || '').split(',');
    if (!col || ok.indexOf(col.getAttribute('data-etapa')) < 0) {
      recusarTransicao(arrastando, col);
    } else {
      mover(arrastando, col);
    }
    limparRealce(); arrastando = null;
  });

  function mover(cartao, col) {
    var destino = col.querySelector('[data-destino]') || col;
    destino.appendChild(cartao);
    cartao.setAttribute('data-etapa-atual', col.getAttribute('data-etapa'));
    var nome = col.getAttribute('data-nome');
    anunciar(cartao.getAttribute('data-codigo') + ' movida para a etapa ' + nome +
             '. Passa a aguardar tomada; a espera de tomada começou a contar.');
    if (window.aoMover) window.aoMover(cartao, col);
  }

  function recusarTransicao(cartao, col) {
    var nome = col ? col.getAttribute('data-nome') : 'fora de uma etapa';
    var msg = 'Transição recusada: a etapa ' + nome + ' não é alcançável a partir de ' +
              cartao.getAttribute('data-nome-etapa-atual') + ' no fluxo deste projeto. ' +
              'A tarefa permanece onde estava.';
    anunciar(msg);
    mostrarRecusa('Transição recusada', msg, null);
  }

  /* menu de movimentação — rota de teclado equivalente ao arrasto */
  document.addEventListener('click', function (ev) {
    var b = ev.target.closest('[data-abrir-mover]');
    if (b) { abrirMenu(b.closest('.cartao') || document.querySelector('[data-cartao-foco]'), b); return; }
    if (!ev.target.closest('.menu-mover')) fecharMenu();
  });
  document.addEventListener('keydown', function (ev) {
    var c = ev.target.closest('.cartao');
    if (c && (ev.key === 'm' || ev.key === 'M')) { ev.preventDefault(); abrirMenu(c, c); }
    if (ev.key === 'Escape') { fecharMenu(); limparRealce(); }
  });

  function fecharMenu() {
    var m = document.querySelector('.menu-mover'); if (m) m.remove();
  }

  function abrirMenu(cartao, ancora) {
    if (!cartao) return;
    fecharMenu();
    realcar(cartao);
    var ok = (cartao.getAttribute('data-alcancaveis') || '').split(',').filter(Boolean);
    var menu = document.createElement('div');
    menu.className = 'menu-mover';
    menu.setAttribute('role', 'menu');
    menu.setAttribute('aria-label', 'Mover ' + cartao.getAttribute('data-codigo') + ' para outra etapa');
    var html = '<p class="cabeca" id="cabeca-mover">Mover para a etapa</p>';
    etapasDoBoard().forEach(function (col) {
      var id = col.getAttribute('data-etapa'), nome = col.getAttribute('data-nome');
      if (id === cartao.getAttribute('data-etapa-atual')) return;
      var permitido = ok.indexOf(id) > -1;
      html += '<button type="button" role="menuitem" class="menu-item" data-etapa-alvo="' + id + '"' +
              (permitido ? '' : ' aria-disabled="true"') + '>' + nome +
              (permitido ? '' : '<span class="razao">não alcançável a partir de ' +
               cartao.getAttribute('data-nome-etapa-atual') + '</span>') + '</button>';
    });
    html += '<div class="menu-separador"></div>' +
            '<p class="cabeca">A raia não restringe transição — é agrupamento (Q-08)</p>';
    menu.innerHTML = html;
    document.body.appendChild(menu);
    var r = (ancora || cartao).getBoundingClientRect();
    menu.style.top = (window.scrollY + r.bottom + 6) + 'px';
    menu.style.left = (window.scrollX + Math.min(r.left, window.innerWidth - 320)) + 'px';
    var primeiro = menu.querySelector('.menu-item:not([aria-disabled])') || menu.querySelector('.menu-item');
    if (primeiro) primeiro.focus();

    menu.addEventListener('keydown', function (ev) {
      var itens = Array.prototype.slice.call(menu.querySelectorAll('.menu-item'));
      var i = itens.indexOf(document.activeElement);
      if (ev.key === 'ArrowDown') { ev.preventDefault(); itens[(i + 1) % itens.length].focus(); }
      if (ev.key === 'ArrowUp') { ev.preventDefault(); itens[(i - 1 + itens.length) % itens.length].focus(); }
    });
    menu.addEventListener('click', function (ev) {
      var it = ev.target.closest('.menu-item'); if (!it) return;
      var col = document.querySelector('.etapa[data-etapa="' + it.getAttribute('data-etapa-alvo') + '"]');
      if (it.getAttribute('aria-disabled') === 'true') { recusarTransicao(cartao, col); }
      else { mover(cartao, col); }
      fecharMenu(); limparRealce(); cartao.focus();
    });
  }

  /* ---------- Recusa por ação concorrente (B-03) ---------- */
  function mostrarRecusa(titulo, texto, estadoHtml) {
    var antigo = document.querySelector('.recusa[data-dinamica]');
    if (antigo) antigo.remove();
    var el = document.createElement('div');
    el.className = 'recusa';
    el.setAttribute('data-dinamica', '');
    el.setAttribute('role', 'alertdialog');
    el.setAttribute('aria-labelledby', 'recusa-titulo');
    el.innerHTML =
      '<h4 id="recusa-titulo">' + titulo + '</h4>' +
      '<p class="texto-sm">' + texto + '</p>' +
      (estadoHtml ? '<div class="estado-atual">' + estadoHtml + '</div>' : '<div style="height:var(--space-lg)"></div>') +
      '<div class="rodape"><button type="button" class="btn btn-sm btn-secundario" data-fechar-recusa>Entendi</button></div>';
    document.body.appendChild(el);
    el.querySelector('[data-fechar-recusa]').focus();
    el.addEventListener('click', function (ev) {
      if (ev.target.closest('[data-fechar-recusa]')) el.remove();
    });
  }
  window.mostrarRecusa = mostrarRecusa;

  document.addEventListener('click', function (ev) {
    var b = ev.target.closest('[data-simular-concorrencia]');
    if (!b) return;
    var estado = b.getAttribute('data-estado-atual') || '';
    mostrarRecusa(
      'Ação recusada — a tarefa mudou',
      b.getAttribute('data-mensagem') ||
        'Outra pessoa agiu sobre esta tarefa antes de você. Nada foi sobrescrito. Veja o estado atual e decida de novo.',
      estado
    );
    anunciar('Ação recusada por ação concorrente. ' + (b.getAttribute('data-resumo-acessivel') || ''));
  });

  document.addEventListener('click', function (ev) {
    var b = ev.target.closest('[data-fechar-recusa-estatica]');
    if (b) b.closest('.recusa').remove();
  });

  /* ---------- Recorte somente-leitura (B-07) ----------
     Demonstração em TL-07: quem tem acesso de leitura não recebe
     nenhuma ação de escrita — elas não são desabilitadas, não existem. */
  document.addEventListener('change', function (ev) {
    var s = ev.target.closest('[data-trocar-papel]');
    if (!s) return;
    var leitura = s.value === 'leitura';
    document.querySelectorAll('[data-escrita]').forEach(function (el) { el.hidden = leitura; });
    document.querySelectorAll('[data-so-leitura]').forEach(function (el) { el.hidden = !leitura; });
    anunciar(leitura
      ? 'Visão de gestor de outro time, acesso de leitura. Nenhuma ação de escrita é oferecida.'
      : 'Visão de Product Owner, com participação de escrita no projeto.');
  });
})();
