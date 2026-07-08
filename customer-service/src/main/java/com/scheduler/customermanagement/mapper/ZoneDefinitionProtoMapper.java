package com.scheduler.customermanagement.mapper;

import com.scheduler.customermanagement.grpc.base.ZoneDefinitionProto;
import com.scheduler.customermanagement.models.ZoneDefinition;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashSet;

@Component
public class ZoneDefinitionProtoMapper {

    public ZoneDefinition toDomain(ZoneDefinitionProto proto) {
        ZoneDefinition def = new ZoneDefinition();
        def.setId(proto.getId());
        def.setTitle(proto.getTitle());
        def.setDayMask(proto.getDayMask());
        def.setStartTime(LocalTime.parse(proto.getStartTime()));
        def.setEndTime(LocalTime.parse(proto.getEndTime()));
        def.setAllowedCategories(new HashSet<>(proto.getAllowedCategoriesList()));
        def.setExcludedCategories(new HashSet<>(proto.getExcludedCategoriesList()));
        def.setPriorityOverrideThreshold(proto.getPriorityOverrideThreshold() > 0 ? proto.getPriorityOverrideThreshold() : null);
        def.setPrimaryCategory(proto.getPrimaryCategory().isBlank() ? null : proto.getPrimaryCategory());
        def.setSecondaryCategories(new HashSet<>(proto.getSecondaryCategoriesList()));
        def.setBehaviorMode(proto.getBehaviorMode().isBlank() ? "STRICT" : proto.getBehaviorMode());
        def.setTargetPlacementMode(proto.getTargetPlacementMode().isBlank() ? "ALLOW_ELSEWHERE" : proto.getTargetPlacementMode());
        def.setZoneConfigId(proto.getZoneConfigId());
        return def;
    }

    public ZoneDefinitionProto toDto(ZoneDefinition entity) {
        ZoneDefinitionProto.Builder builder = ZoneDefinitionProto.newBuilder()
                .setId(entity.getId())
                .setTitle(entity.getTitle())
                .setDayMask(entity.getDayMask())
                .setStartTime(entity.getStartTime().toString())
                .setEndTime(entity.getEndTime().toString())
                .setPriorityOverrideThreshold(entity.getPriorityOverrideThreshold() == null ? 0 : entity.getPriorityOverrideThreshold())
                .setZoneConfigId(entity.getZoneConfigId());

        if (entity.getAllowedCategories() != null) {
            builder.addAllAllowedCategories(entity.getAllowedCategories());
        }
        if (entity.getExcludedCategories() != null) {
            builder.addAllExcludedCategories(entity.getExcludedCategories());
        }
        if (entity.getPrimaryCategory() != null) {
            builder.setPrimaryCategory(entity.getPrimaryCategory());
        }
        if (entity.getSecondaryCategories() != null) {
            builder.addAllSecondaryCategories(entity.getSecondaryCategories());
        }
        builder.setBehaviorMode(entity.getBehaviorMode() != null ? entity.getBehaviorMode() : "STRICT");
        builder.setTargetPlacementMode(entity.getTargetPlacementMode() != null ? entity.getTargetPlacementMode() : "ALLOW_ELSEWHERE");

        return builder.build();
    }
}
