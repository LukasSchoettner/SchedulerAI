package com.scheduler.customermanagement.mapper;

import com.scheduler.customermanagement.grpc.base.ZoneConfigurationProto;
import com.scheduler.customermanagement.mapper.ZoneDefinitionProtoMapper;
import com.scheduler.customermanagement.models.ZoneConfiguration;
import com.scheduler.customermanagement.models.ZoneDefinition;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = { ZoneDefinitionProtoMapper.class })
public abstract class ZoneConfigProtoMapper {

    @Autowired
    protected ZoneDefinitionProtoMapper zoneDefinitionProtoMapper;

    public ZoneConfiguration toDomain(ZoneConfigurationProto proto) {
        if (proto == null) return null;

        ZoneConfiguration zc = new ZoneConfiguration();
        zc.setId(proto.getId());
        zc.setName(proto.getName());
        zc.setActive(proto.getActive());
        zc.setStartTime(LocalTime.parse(proto.getStartTime()));
        zc.setEndTime(LocalTime.parse(proto.getEndTime()));

        Set<ZoneDefinition> zones = proto.getZonesList().stream()
                .map(zoneDefinitionProtoMapper::toDomain)
                .peek(z -> z.setZoneConfigId(zc.getId()))  // maintain bidirectional link
                .collect(Collectors.toSet());

        zc.setZones(zones);
        return zc;
    }

    public abstract ZoneConfigurationProto toProto(ZoneConfiguration entity);
}
