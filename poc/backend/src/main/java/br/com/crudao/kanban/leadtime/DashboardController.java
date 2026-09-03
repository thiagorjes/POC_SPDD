package br.com.crudao.kanban.leadtime;

import br.com.crudao.kanban.leadtime.dto.DashboardResponse;
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

@RestController
@RequestMapping("/api/projetos/{projetoId}/dashboard")
@Validated
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  /** Leitura permitida mesmo com o projeto finalizado (UC-002). */
  @GetMapping
  public ResponseEntity<DashboardResponse> obter(
      @PathVariable UUID projetoId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant inicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant fim) {
    return ResponseEntity.ok(dashboardService.obter(projetoId, inicio, fim));
  }
}
