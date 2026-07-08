package com.scheduler.scheduling.mapper;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.grpc.base.ZoneDefinitionProto;
import org.mapstruct.Mapper;

import java.time.LocalTime;
import java.util.HashSet;

@Mapper(componentModel="spring")
public interface ZoneGrpcMapper {
    default ZoneDefinitionDTO fromProto(ZoneDefinitionProto proto) {
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
        dto.setTargetPlacementMode(proto.getTargetPlacementMode().isBlank()
                ? "ALLOW_ELSEWHERE"
                : proto.getTargetPlacementMode());
        return dto;
    }
}
