package br.com.crudao.kanban.raia;

import br.com.crudao.kanban.raia.dto.CriarRaiaRequest;
import br.com.crudao.kanban.raia.dto.RaiaResponse;
import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.security.ExigePermissao;
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

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class RaiaController {

  private final RaiaService raiaService;
  private final RaiaMapper raiaMapper;

  @GetMapping("/projetos/{projetoId}/raias")
  @ExigePermissao(
      valor = Permissoes.PROJETO_VISUALIZAR,
      escopoProjeto = "#resolvedor.deProjeto(#projetoId)",
      escrita = false)
  public ResponseEntity<List<RaiaResponse>> listar(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(raiaMapper.paraResponse(raiaService.listar(projetoId)));
  }

  @PostMapping("/projetos/{projetoId}/raias")
  @ExigePermissao(
      valor = Permissoes.RAIA_GERENCIAR, escopoProjeto = "#resolvedor.deProjeto(#projetoId)")
  public ResponseEntity<RaiaResponse> criar(
      @PathVariable UUID projetoId, @RequestBody @Valid CriarRaiaRequest request) {
    Raia raia = raiaService.criar(projetoId, request);
    return ResponseEntity.created(
            URI.create("/api/projetos/" + projetoId + "/raias/" + raia.getId()))
        .body(raiaMapper.paraResponse(raia));
  }

  @PutMapping("/projetos/{projetoId}/raias/{raiaId}")
  @ExigePermissao(valor = Permissoes.RAIA_GERENCIAR, escopoProjeto = "#resolvedor.deRaia(#raiaId)")
  public ResponseEntity<RaiaResponse> renomear(
      @PathVariable UUID projetoId,
      @PathVariable UUID raiaId,
      @RequestBody @Valid CriarRaiaRequest request) {
    return ResponseEntity.ok(raiaMapper.paraResponse(raiaService.renomear(raiaId, request.nome())));
  }

  @PutMapping("/projetos/{projetoId}/raias/{raiaId}/padrao")
  @ExigePermissao(valor = Permissoes.RAIA_GERENCIAR, escopoProjeto = "#resolvedor.deRaia(#raiaId)")
  public ResponseEntity<RaiaResponse> definirPadrao(
      @PathVariable UUID projetoId, @PathVariable UUID raiaId) {
    return ResponseEntity.ok(raiaMapper.paraResponse(raiaService.definirPadrao(raiaId)));
  }

  @DeleteMapping("/projetos/{projetoId}/raias/{raiaId}")
  @ExigePermissao(valor = Permissoes.RAIA_GERENCIAR, escopoProjeto = "#resolvedor.deRaia(#raiaId)")
  public ResponseEntity<Void> excluir(@PathVariable UUID projetoId, @PathVariable UUID raiaId) {
    raiaService.excluir(raiaId);
    return ResponseEntity.noContent().build();
  }
}
