package br.com.crudao.kanban.rbac;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<Permissao, UUID> {}
