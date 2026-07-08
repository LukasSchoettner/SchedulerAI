package com.scheduler.customermanagement.mapper;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.models.ZoneDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ZoneDefinitionDtoMapper {
    @Mapping(target="id",                    source="id")
    @Mapping(target="title",                 source="title")
    @Mapping(target="dayMask",               source="dayMask")
    @Mapping(target="startTime",             source="startTime")
    @Mapping(target="endTime",               source="endTime")
    @Mapping(target="allowedCategories",     source="allowedCategories")
    @Mapping(target="excludedCategories",    source="excludedCategories")
    @Mapping(target="priorityOverrideThreshold", source="priorityOverrideThreshold")
    @Mapping(target="primaryCategory",       source="primaryCategory")
    @Mapping(target="secondaryCategories",   source="secondaryCategories")
    @Mapping(target="behaviorMode",          source="behaviorMode")
    @Mapping(target="targetPlacementMode",   source="targetPlacementMode")
    ZoneDefinitionDTO toDto(ZoneDefinition entity);

    ZoneDefinition toDomain(ZoneDefinitionDTO dto);
}
