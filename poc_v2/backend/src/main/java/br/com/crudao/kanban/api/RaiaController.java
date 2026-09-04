package br.com.crudao.kanban.api;

import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaMapper;
import br.com.crudao.kanban.raia.RaiaService;
import br.com.crudao.kanban.raia.dto.CriarRaiaRequest;
import br.com.crudao.kanban.raia.dto.RaiaResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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

/** Raias do projeto (RF-011). Raia agrupa visualmente e nao participa do grafo de transicoes. */
@RestController
@RequestMapping("/api/projetos/{projetoId}/raias")
@Validated
@RequiredArgsConstructor
public class RaiaController {

  private final RaiaService raiaService;
  private final RaiaMapper raiaMapper;

  @GetMapping
  public ResponseEntity<List<RaiaResponse>> listar(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(raiaMapper.paraResponses(raiaService.listar(projetoId)));
  }

  @PostMapping
  public ResponseEntity<RaiaResponse> criar(
      @PathVariable UUID projetoId, @RequestBody @Valid CriarRaiaRequest request) {
    Raia raia = raiaService.criar(projetoId, request.nome());
    return ResponseEntity.created(
            URI.create("/api/projetos/" + projetoId + "/raias/" + raia.getId()))
        .body(raiaMapper.paraResponse(raia));
  }

  @PutMapping("/{raiaId}")
  public ResponseEntity<RaiaResponse> atualizar(
      @PathVariable UUID projetoId,
      @PathVariable UUID raiaId,
      @RequestBody @Valid CriarRaiaRequest request) {
    return ResponseEntity.ok(
        raiaMapper.paraResponse(raiaService.atualizar(raiaId, request.nome())));
  }

  @PutMapping("/{raiaId}/padrao")
  public ResponseEntity<RaiaResponse> definirPadrao(
      @PathVariable UUID projetoId, @PathVariable UUID raiaId) {
    return ResponseEntity.ok(raiaMapper.paraResponse(raiaService.definirPadrao(raiaId)));
  }

  @DeleteMapping("/{raiaId}")
  public ResponseEntity<Void> excluir(@PathVariable UUID projetoId, @PathVariable UUID raiaId) {
    raiaService.excluir(raiaId);
    return ResponseEntity.noContent().build();
  }
}
