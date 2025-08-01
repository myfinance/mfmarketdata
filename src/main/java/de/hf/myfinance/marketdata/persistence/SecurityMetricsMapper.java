package de.hf.myfinance.marketdata.persistence;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import de.hf.myfinance.marketdata.persistence.entities.SecurityMetricsEntity;
import de.hf.myfinance.restmodel.SecurityMetrics;

@Mapper(componentModel = "spring")
public interface SecurityMetricsMapper {
        @Mappings({
            @Mapping(target = "rankByPE", ignore = true),
            @Mapping(target = "rankByRoA", ignore = true),
            @Mapping(target = "rankByRoAAndPE", ignore = true),
            @Mapping(target = "rankByIntrinsicValueMargin", ignore = true),
            @Mapping(target = "rankByLynchScore", ignore = true),
            @Mapping(target = "rankByLynchAndIntrinsicValueMargin", ignore = true)
            })
    SecurityMetrics entityToApi(SecurityMetricsEntity entity);

    @Mappings({
            @Mapping(target = "securityMetricsid", ignore = true)
            })
    SecurityMetricsEntity apiToEntity(SecurityMetrics api);

    List<SecurityMetrics> entityListToApiList(List<SecurityMetricsEntity> entity);

    List<SecurityMetricsEntity> apiListToEntityList(List<SecurityMetrics> api);

    default SecurityMetrics createSecurityMetrics() {
        return new SecurityMetrics();
    }
}
