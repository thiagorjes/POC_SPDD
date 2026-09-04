package br.com.crudao.kanban.api;

import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.dto.UsuarioResponse;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Identidade do usuario autenticado (BDR-001). */
@RestController
@RequestMapping("/api/usuarios")
@Validated
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioAtualProvider usuarioAtualProvider;

  @GetMapping("/me")
  public ResponseEntity<UsuarioResponse> eu() {
    Usuario usuario = usuarioAtualProvider.atual();
    return ResponseEntity.ok(
        new UsuarioResponse(
            usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isAdminGlobal()));
  }
}
