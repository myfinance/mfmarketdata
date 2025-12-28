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
