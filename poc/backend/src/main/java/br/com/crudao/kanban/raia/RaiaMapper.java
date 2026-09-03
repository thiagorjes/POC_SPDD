package br.com.crudao.kanban.raia;

import br.com.crudao.kanban.raia.dto.RaiaResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RaiaMapper {

  RaiaResponse paraResponse(Raia raia);

  List<RaiaResponse> paraResponse(List<Raia> raias);
}
