package br.com.crudao.kanban.board;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projetos/{projetoId}/board")
@Validated
@RequiredArgsConstructor
public class BoardController {

  private final BoardQueryService boardQueryService;

  /** Snapshot completo; tambem e o endpoint de resync apos gap de {@code seq} (ADR-004). */
  @GetMapping
  public ResponseEntity<BoardSnapshotResponse> snapshot(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(boardQueryService.snapshot(projetoId));
  }
}
