package com.scheduler.customermanagement.mapper;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.customermanagement.grpc.base.CustomerProto;
import com.scheduler.customermanagement.models.Customer;

public class CustomerMapper {

    public static CustomerDTO toDto(Customer customer) {
        if (customer == null) return null;

        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setCustomername(customer.getCustomerName());
        dto.setEmail(customer.getEmail());
        dto.setActive(customer.isActive());
        dto.setMembershipLevel(customer.getMembershipLevel());
        return dto;
    }

    public static Customer toEntity(CustomerDTO dto) {
        if (dto == null) return null;

        Customer customer = new Customer();
        customer.setCustomerName(dto.getCustomername());
        customer.setEmail(dto.getEmail());
        customer.setActive(dto.isActive());
        customer.setMembershipLevel(dto.getMembershipLevel());
        return customer;
    }

    public static Customer toEntity(CustomerProto proto) {
        if (proto == null) return null;

        Customer customer = new Customer();
        customer.setId(proto.getId());
        customer.setCustomerName(proto.getCustomername());
        customer.setEmail(proto.getEmail());
        customer.setActive(proto.getActive());
        //customer.setMembershipLevel(proto.);
        return customer;
    }

    public static CustomerDTO toDto(CustomerProto proto) {
        if (proto == null) return null;

        CustomerDTO dto = new CustomerDTO();
        dto.setId(proto.getId());
        dto.setCustomername(proto.getCustomername());
        dto.setEmail(proto.getEmail());
        dto.setActive(proto.getActive());
        return dto;
    }

    public static CustomerProto toProto(CustomerDTO dto) {
        if (dto == null) return null;

        return CustomerProto.newBuilder()
                .setId(dto.getId() != null ? dto.getId() : 0)
                .setCustomername(dto.getCustomername())
                .setEmail(dto.getEmail())
                .setActive(dto.isActive())
                .build();
    }
}
