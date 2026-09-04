package br.com.crudao.kanban.notificacao;

import br.com.crudao.kanban.notificacao.dto.NotificacaoResponse;
import org.mapstruct.Mapper;

/** Conversao entidade/DTO de notificacao. */
@Mapper(componentModel = "spring")
public interface NotificacaoMapper {

  NotificacaoResponse paraResponse(Notificacao notificacao);
}
