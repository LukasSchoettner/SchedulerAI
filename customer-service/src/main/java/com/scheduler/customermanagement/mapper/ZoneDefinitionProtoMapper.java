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
        def.setPriorityOverrideThreshold(proto.getPriorityOverrideThreshold());
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

        return builder.build();
    }
}
