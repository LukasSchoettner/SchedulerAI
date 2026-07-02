package com.scheduler.commoncode.mappers;

import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.customermanagement.grpc.base.ZoneConfigurationProto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { ZoneDefinitionDtoMapperCom.class })
public interface ZoneConfigDtoMapperCom {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "zones", source = "zonesList")
    ZoneConfigurationDTO toDto(ZoneConfigurationProto proto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "zonesList", source = "zones")
    ZoneConfigurationProto toProto(ZoneConfigurationDTO dto);
}
