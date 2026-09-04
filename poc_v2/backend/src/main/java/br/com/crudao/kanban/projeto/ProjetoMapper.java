package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.projeto.dto.ProjetoResponse;
import java.util.List;
import org.mapstruct.Mapper;

/** Conversao entidade/DTO de projeto. */
@Mapper(componentModel = "spring")
public interface ProjetoMapper {

  ProjetoResponse paraResponse(Projeto projeto);

  List<ProjetoResponse> paraResponses(List<Projeto> projetos);
}
