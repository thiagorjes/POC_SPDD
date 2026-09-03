package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.projeto.dto.ProjetoResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-03T16:34:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class ProjetoMapperImpl implements ProjetoMapper {

    @Override
    public ProjetoResponse paraResponse(Projeto projeto) {
        if ( projeto == null ) {
            return null;
        }

        UUID id = null;
        String nome = null;
        String descricao = null;
        StatusProjeto status = null;
        Instant finalizadoEm = null;
        Instant criadoEm = null;
        long versao = 0L;

        id = projeto.getId();
        nome = projeto.getNome();
        descricao = projeto.getDescricao();
        status = projeto.getStatus();
        finalizadoEm = projeto.getFinalizadoEm();
        criadoEm = projeto.getCriadoEm();
        versao = projeto.getVersao();

        ProjetoResponse projetoResponse = new ProjetoResponse( id, nome, descricao, status, finalizadoEm, criadoEm, versao );

        return projetoResponse;
    }

    @Override
    public List<ProjetoResponse> paraResponse(List<Projeto> projetos) {
        if ( projetos == null ) {
            return null;
        }

        List<ProjetoResponse> list = new ArrayList<ProjetoResponse>( projetos.size() );
        for ( Projeto projeto : projetos ) {
            list.add( paraResponse( projeto ) );
        }

        return list;
    }
}
