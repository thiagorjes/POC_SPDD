package br.com.crudao.kanban.raia;

import br.com.crudao.kanban.raia.dto.RaiaResponse;
import java.util.List;
import org.mapstruct.Mapper;

/** Conversao entidade/DTO de raia. */
@Mapper(componentModel = "spring")
public interface RaiaMapper {

  RaiaResponse paraResponse(Raia raia);

  List<RaiaResponse> paraResponses(List<Raia> raias);
}
