package com.scheduler.commoncode.mappers;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.grpc.base.ZoneDefinitionProto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", uses = LocalTimeMapper.class)
public interface ZoneDefinitionDtoMapperCom {

    /** proto → DTO is straightforward: copy the generated list into your Set<String>. */
    @Mapping(target="allowedCategories",  source="allowedCategoriesList")
    @Mapping(target="excludedCategories", source="excludedCategoriesList")
    ZoneDefinitionDTO toDto(ZoneDefinitionProto proto);

    /**
     * DTO → proto must use the builder’s addAll methods rather than MapStruct’s default list
     * instantiation.  We do that by hand in this default method.
     */
    default ZoneDefinitionProto toProto(ZoneDefinitionDTO dto) {
        var b = ZoneDefinitionProto.newBuilder()
                .setId(dto.getId())
                .setTitle(dto.getTitle())
                .setDayMask(dto.getDayMask())
                // format LocalTime → String:
                .setStartTime(dto.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setEndTime  (dto.getEndTime()  .format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setPriorityOverrideThreshold(dto.getPriorityOverrideThreshold())
                .setZoneConfigId(0L); // <-- wire up parent ID as you see fit

        if (dto.getAllowedCategories()   != null) b.addAllAllowedCategories(dto.getAllowedCategories());
        if (dto.getExcludedCategories() != null) b.addAllExcludedCategories(dto.getExcludedCategories());
        return b.build();
    }
}
