package com.scheduler.commoncode.mappers;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.grpc.base.ZoneDefinitionProto;
import org.mapstruct.Mapper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;

@Mapper(componentModel = "spring", uses = LocalTimeMapper.class)
public interface ZoneDefinitionDtoMapperCom {

    default ZoneDefinitionDTO toDto(ZoneDefinitionProto proto) {
        if (proto == null) {
            return null;
        }
        ZoneDefinitionDTO dto = new ZoneDefinitionDTO();
        dto.setId(proto.getId());
        dto.setTitle(proto.getTitle());
        dto.setDayMask(proto.getDayMask());
        dto.setStartTime(LocalTime.parse(proto.getStartTime()));
        dto.setEndTime(LocalTime.parse(proto.getEndTime()));
        dto.setAllowedCategories(new HashSet<>(proto.getAllowedCategoriesList()));
        dto.setExcludedCategories(new HashSet<>(proto.getExcludedCategoriesList()));
        dto.setPriorityOverrideThreshold(
                proto.getPriorityOverrideThreshold() > 0
                        ? proto.getPriorityOverrideThreshold()
                        : null
        );
        dto.setPrimaryCategory(proto.getPrimaryCategory().isBlank() ? null : proto.getPrimaryCategory());
        dto.setSecondaryCategories(new HashSet<>(proto.getSecondaryCategoriesList()));
        dto.setBehaviorMode(proto.getBehaviorMode().isBlank() ? "STRICT" : proto.getBehaviorMode());
        dto.setTargetPlacementMode(proto.getTargetPlacementMode().isBlank() ? "ALLOW_ELSEWHERE" : proto.getTargetPlacementMode());
        return dto;
    }

    default ZoneDefinitionProto toProto(ZoneDefinitionDTO dto) {
        var builder = ZoneDefinitionProto.newBuilder()
                .setId(dto.getId())
                .setTitle(dto.getTitle())
                .setDayMask(dto.getDayMask())
                .setStartTime(dto.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setEndTime(dto.getEndTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setPriorityOverrideThreshold(dto.getPriorityOverrideThreshold() != null
                        ? dto.getPriorityOverrideThreshold()
                        : 0)
                .setZoneConfigId(0L);

        if (dto.getAllowedCategories() != null) {
            builder.addAllAllowedCategories(dto.getAllowedCategories());
        }
        if (dto.getExcludedCategories() != null) {
            builder.addAllExcludedCategories(dto.getExcludedCategories());
        }
        if (dto.getPrimaryCategory() != null) {
            builder.setPrimaryCategory(dto.getPrimaryCategory());
        }
        if (dto.getSecondaryCategories() != null) {
            builder.addAllSecondaryCategories(dto.getSecondaryCategories());
        }
        builder.setBehaviorMode(dto.getBehaviorMode() != null ? dto.getBehaviorMode() : "STRICT");
        builder.setTargetPlacementMode(dto.getTargetPlacementMode() != null ? dto.getTargetPlacementMode() : "ALLOW_ELSEWHERE");
        return builder.build();
    }
}
