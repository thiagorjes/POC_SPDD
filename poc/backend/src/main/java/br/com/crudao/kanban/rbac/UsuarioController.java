package br.com.crudao.kanban.rbac;

import br.com.crudao.kanban.rbac.dto.UsuarioResponse;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Validated
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioAtualProvider usuarioAtualProvider;
  private final UsuarioRepository usuarioRepository;

  @GetMapping("/me")
  public ResponseEntity<UsuarioResponse> eu() {
    Usuario usuario = usuarioAtualProvider.obrigatorio();
    return ResponseEntity.ok(
        new UsuarioResponse(
            usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isAdminGlobal()));
  }

  /** Lista para o seletor de responsavel e de associacao de papel. Nao expoe dado sensivel. */
  @GetMapping
  public ResponseEntity<List<UsuarioResponse>> listar() {
    return ResponseEntity.ok(
        usuarioRepository.findAllByOrderByNomeAsc().stream()
            .map(u -> new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.isAdminGlobal()))
            .toList());
  }
}
