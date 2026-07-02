package com.scheduler.customermanagement.mapper;

import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.models.ZoneConfiguration;
import com.scheduler.customermanagement.models.ZoneDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { ZoneDefinitionDtoMapper.class })
public interface ZoneConfigDtoMapper {
    ZoneConfigurationDTO toDto(ZoneConfiguration entity);
    ZoneConfiguration   toDomain(ZoneConfigurationDTO dto);
}
