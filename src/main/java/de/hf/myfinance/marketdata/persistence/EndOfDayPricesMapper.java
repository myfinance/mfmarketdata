package de.hf.myfinance.marketdata.persistence;

import de.hf.myfinance.marketdata.persistence.entities.EndOfDayPricesEntity;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EndOfDayPricesMapper {

    EndOfDayPrices entityToApi(EndOfDayPricesEntity entity);

    @Mappings({
            @Mapping(target = "priceid", ignore = true)
    })
    EndOfDayPricesEntity apiToEntity(EndOfDayPrices api);

    List<EndOfDayPrices> entityListToApiList(List<EndOfDayPricesEntity> entity);

    List<EndOfDayPricesEntity> apiListToEntityList(List<EndOfDayPrices> api);

    default EndOfDayPrices createInstrument() {
        return new EndOfDayPrices();
    }
}
