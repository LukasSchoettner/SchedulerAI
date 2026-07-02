package com.scheduler.customermanagement.grpc.impl;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.customermanagement.grpc.base.*;
import com.scheduler.customermanagement.services.CustomerService;
import static com.scheduler.commoncode.grpc.JwtGrpcServerInterceptor.CUSTOMER_ID_CTX_KEY;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class CustomerServiceGrpcImpl extends CustomerServiceGrpc.CustomerServiceImplBase {

    @Autowired
    private CustomerService service;

    @Override
    public void getCustomerById(CustomerRequest req, StreamObserver<CustomerProto> obs) {
        Long callerId = CUSTOMER_ID_CTX_KEY.get();
        service.getCustomerById(req.getId())
                .ifPresentOrElse(
                        dto -> {
                            obs.onNext(buildProto(dto));
                            obs.onCompleted();
                        },
                        () -> obs.onError(new RuntimeException("Not found"))
                );
    }

    @Override
    public void createCustomer(CustomerCreate req, StreamObserver<CustomerProto> obs) {
        CustomerDTO dto = new CustomerDTO(
                null,
                req.getCustomername(),
                req.getEmail(),
                req.getPassword(),
                true,
                null, // default active = true
                MembershipLevel.BASIC // default membership
        );
        CustomerDTO created = service.createCustomer(dto);
        obs.onNext(buildProto(created));
        obs.onCompleted();
    }

    @Override
    public void updateCustomer(CustomerUpdate req, StreamObserver<CustomerProto> obs) {
        MembershipLevel level = mapGrpcLevel(req.getMembershipLevel());

        CustomerDTO dto = new CustomerDTO(
                req.getId(),
                req.getCustomername(),
                req.getEmail(),
                req.getPassword(), // now comes from the new message
                req.getActive(),
                null,
                level
        );

        service.updateCustomer(req.getId(), dto)
                .ifPresentOrElse(
                        updated -> {
                            obs.onNext(buildProto(updated));
                            obs.onCompleted();
                        },
                        () -> obs.onError(new RuntimeException("Not found"))
                );
    }

    @Override
    public void deleteCustomer(CustomerRequest req, StreamObserver<CustomerDeleteResponse> obs) {
        boolean success = service.deleteCustomer(req.getId());
        obs.onNext(CustomerDeleteResponse.newBuilder().setSuccess(success).build());
        obs.onCompleted();
    }

    private CustomerProto buildProto(CustomerDTO dto) {
        return CustomerProto.newBuilder()
                .setId(dto.getId())
                .setCustomername(dto.getCustomername())
                .setEmail(dto.getEmail())
                .setActive(dto.isActive())
                .setMembershipLevel(mapEnumToGrpc(dto.getMembershipLevel()))
                .build();
    }

    private MembershipLevel mapGrpcLevel(com.scheduler.customermanagement.grpc.base.MembershipLevel grpcLevel) {
        return switch (grpcLevel) {
            case PLUS -> MembershipLevel.PLUS;
            case PREMIUM -> MembershipLevel.PREMIUM;
            case BASIC, UNRECOGNIZED -> MembershipLevel.BASIC;
        };
    }

    private com.scheduler.customermanagement.grpc.base.MembershipLevel mapEnumToGrpc(MembershipLevel level) {
        if (level == null) {
            return com.scheduler.customermanagement.grpc.base.MembershipLevel.BASIC; // or add UNSPECIFIED to proto
        }

        return switch (level) {
            case BASIC -> com.scheduler.customermanagement.grpc.base.MembershipLevel.BASIC;
            case PLUS -> com.scheduler.customermanagement.grpc.base.MembershipLevel.PLUS;
            case PREMIUM -> com.scheduler.customermanagement.grpc.base.MembershipLevel.PREMIUM;
        };
    }

}
