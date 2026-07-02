package com.scheduler.commoncode.mappers;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.customermanagement.grpc.base.CustomerProto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

@Mapper(componentModel = "spring")
public interface CustomerDtoMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "customername", source = "customername")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "membershipLevel", source = "membershipLevel")
    CustomerDTO toDto(CustomerProto proto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "customername", source = "customername")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "membershipLevel", source = "membershipLevel")
    CustomerProto toProto(CustomerDTO dto);

    // gRPC → DTO enum
    @ValueMappings({
            @ValueMapping(source = "UNRECOGNIZED", target = "BASIC")
    })
    MembershipLevel map(com.scheduler.customermanagement.grpc.base.MembershipLevel grpcLevel);

    // DTO → gRPC enum
    @ValueMappings({
            @ValueMapping(source = "BASIC",   target = "BASIC"),
            @ValueMapping(source = "PLUS",    target = "PLUS"),
            @ValueMapping(source = "PREMIUM", target = "PREMIUM")
    })
    com.scheduler.customermanagement.grpc.base.MembershipLevel map(MembershipLevel dtoLevel);
}
