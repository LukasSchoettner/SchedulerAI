package com.scheduler.scheduling.mapper;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.grpc.base.ZoneDefinitionProto;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface ZoneGrpcMapper {
    ZoneDefinitionDTO fromProto(ZoneDefinitionProto proto);
}

