package br.com.crudao.kanban.raia;

import br.com.crudao.kanban.raia.dto.RaiaResponse;
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
public class RaiaMapperImpl implements RaiaMapper {

    @Override
    public RaiaResponse paraResponse(Raia raia) {
        if ( raia == null ) {
            return null;
        }

        UUID id = null;
        String nome = null;
        int ordem = 0;
        boolean padrao = false;

        id = raia.getId();
        nome = raia.getNome();
        ordem = raia.getOrdem();
        padrao = raia.isPadrao();

        RaiaResponse raiaResponse = new RaiaResponse( id, nome, ordem, padrao );

        return raiaResponse;
    }

    @Override
    public List<RaiaResponse> paraResponse(List<Raia> raias) {
        if ( raias == null ) {
            return null;
        }

        List<RaiaResponse> list = new ArrayList<RaiaResponse>( raias.size() );
        for ( Raia raia : raias ) {
            list.add( paraResponse( raia ) );
        }

        return list;
    }
}
