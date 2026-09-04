package br.com.crudao.kanban.api;

import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoMapper;
import br.com.crudao.kanban.projeto.ProjetoService;
import br.com.crudao.kanban.projeto.dto.AtualizarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.AtualizarTogglesRequest;
import br.com.crudao.kanban.projeto.dto.CriarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.PermissoesEfetivasResponse;
import br.com.crudao.kanban.projeto.dto.ProjetoResponse;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ciclo de vida do projeto, permissoes efetivas e toggles (RF-008, RF-016, RNF-003). */
@RestController
@RequestMapping("/api/projetos")
@Validated
@RequiredArgsConstructor
public class ProjetoController {

  private final ProjetoService projetoService;
  private final ProjetoMapper projetoMapper;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @GetMapping
  public ResponseEntity<List<ProjetoResponse>> listar() {
    return ResponseEntity.ok(
        projetoMapper.paraResponses(projetoService.listarDoUsuario(usuarioAtualProvider.atual())));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProjetoResponse> buscar(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.buscar(id)));
  }

  @PostMapping
  public ResponseEntity<ProjetoResponse> criar(@RequestBody @Valid CriarProjetoRequest request) {
    Projeto projeto = projetoService.criar(request, usuarioAtualProvider.atual());
    return ResponseEntity.created(URI.create("/api/projetos/" + projeto.getId()))
        .body(projetoMapper.paraResponse(projeto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProjetoResponse> atualizar(
      @PathVariable UUID id, @RequestBody @Valid AtualizarProjetoRequest request) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.atualizar(id, request)));
  }

  @PostMapping("/{id}/finalizar")
  public ResponseEntity<ProjetoResponse> finalizar(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.finalizar(id)));
  }

  @PostMapping("/{id}/reabrir")
  public ResponseEntity<ProjetoResponse> reabrir(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.reabrir(id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    projetoService.excluir(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/permissoes")
  public ResponseEntity<PermissoesEfetivasResponse> permissoes(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoService.permissoesEfetivas(id, usuarioAtualProvider.atual()));
  }

  @GetMapping("/{id}/toggles")
  public ResponseEntity<Map<ChaveToggle, Boolean>> toggles(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoService.toggles(id));
  }

  @PutMapping("/{id}/toggles")
  public ResponseEntity<Map<ChaveToggle, Boolean>> atualizarToggles(
      @PathVariable UUID id, @RequestBody @Valid AtualizarTogglesRequest request) {
    return ResponseEntity.ok(projetoService.atualizarToggles(id, request.toggles()));
  }
}
