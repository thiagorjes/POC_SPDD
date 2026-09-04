package br.com.crudao.kanban.api;

import br.com.crudao.kanban.board.BoardQueryService;
import br.com.crudao.kanban.board.dto.BoardSnapshotResponse;
import br.com.crudao.kanban.dashboard.DashboardService;
import br.com.crudao.kanban.dashboard.dto.DashboardResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Leitura do board e dos indicadores do projeto (RF-001, RF-007, RF-011). */
@RestController
@RequestMapping("/api/projetos/{projetoId}")
@Validated
@RequiredArgsConstructor
public class BoardController {

  private final BoardQueryService boardQueryService;
  private final DashboardService dashboardService;

  @GetMapping("/board")
  public ResponseEntity<BoardSnapshotResponse> board(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(boardQueryService.snapshot(projetoId));
  }

  @GetMapping("/dashboard")
  public ResponseEntity<DashboardResponse> dashboard(
      @PathVariable UUID projetoId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant inicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant fim) {
    return ResponseEntity.ok(dashboardService.obter(projetoId, inicio, fim));
  }
}
