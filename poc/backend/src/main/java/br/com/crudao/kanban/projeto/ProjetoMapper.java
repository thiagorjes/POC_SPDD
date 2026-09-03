package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.projeto.dto.ProjetoResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjetoMapper {

  ProjetoResponse paraResponse(Projeto projeto);

  List<ProjetoResponse> paraResponse(List<Projeto> projetos);
}
