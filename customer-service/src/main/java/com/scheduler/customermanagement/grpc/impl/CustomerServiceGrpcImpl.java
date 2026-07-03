package com.scheduler.customermanagement.grpc.impl;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.MinimumRequirementDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.customermanagement.grpc.base.*;
import com.scheduler.customermanagement.services.CustomerService;
import static com.scheduler.commoncode.grpc.JwtGrpcServerInterceptor.CUSTOMER_ID_CTX_KEY;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Collections;

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
        CustomerProto.Builder builder = CustomerProto.newBuilder()
                .setId(dto.getId())
                .setCustomername(dto.getCustomername())
                .setEmail(dto.getEmail())
                .setActive(dto.isActive())
                .setMembershipLevel(mapEnumToGrpc(dto.getMembershipLevel()));
        if (dto.getSchedulingPreference() != null) {
            builder.setSchedulingPreference(toProto(dto.getSchedulingPreference()));
        }
        return builder.build();
    }

    private SchedulingPreferenceProto toProto(SchedulingPreferenceDTO dto) {
        SchedulingPreferenceProto.Builder builder = SchedulingPreferenceProto.newBuilder()
                .setId(dto.getId() != null ? dto.getId() : 0)
                .setPrimaryPriority(value(dto.getPrimaryPriority()))
                .addAllCategoryPriorityOrder(dto.getCategoryPriorityOrder() != null ? dto.getCategoryPriorityOrder() : Collections.emptyList())
                .addAllFixedCommitmentCategories(dto.getFixedCommitmentCategories() != null ? dto.getFixedCommitmentCategories() : Collections.emptySet())
                .setWorkFlexibility(value(dto.getWorkFlexibility()))
                .addAllHealthConstraints(dto.getHealthConstraints() != null ? dto.getHealthConstraints() : Collections.emptySet())
                .setAllocationMode(value(dto.getAllocationMode()))
                .setPlannedHoursPerDayMinutes(dto.getPlannedHoursPerDayMinutes() != null ? dto.getPlannedHoursPerDayMinutes() : 0)
                .setFixedTimeBudgetMode(value(dto.getFixedTimeBudgetMode()))
                .setPauseMinutes(dto.getPauseMinutes() != null ? dto.getPauseMinutes() : 5)
                .setPauseOverloadBehavior(value(dto.getPauseOverloadBehavior()))
                .setPlanningFullness(value(dto.getPlanningFullness()))
                .addAllOverloadReductionOrder(dto.getOverloadReductionOrder() != null ? dto.getOverloadReductionOrder() : Collections.emptyList())
                .setTemporaryMode(value(dto.getTemporaryMode()))
                .setTemporaryUntil(dto.getTemporaryUntil() != null ? dto.getTemporaryUntil().toString() : "");

        for (Map.Entry<String, Integer> entry : (dto.getTaskCountTargets() != null ? dto.getTaskCountTargets() : Collections.<String, Integer>emptyMap()).entrySet()) {
            builder.addTaskCountTargets(CategoryIntTargetProto.newBuilder()
                    .setCategory(entry.getKey())
                    .setValue(entry.getValue() != null ? entry.getValue() : 0)
                    .build());
        }
        for (Map.Entry<String, Integer> entry : (dto.getTimeBudgetTargets() != null ? dto.getTimeBudgetTargets() : Collections.<String, Integer>emptyMap()).entrySet()) {
            builder.addTimeBudgetTargets(CategoryIntTargetProto.newBuilder()
                    .setCategory(entry.getKey())
                    .setValue(entry.getValue() != null ? entry.getValue() : 0)
                    .build());
        }
        for (Map.Entry<String, Boolean> entry : (dto.getFixedTimeCountsByCategory() != null ? dto.getFixedTimeCountsByCategory() : Collections.<String, Boolean>emptyMap()).entrySet()) {
            builder.addFixedTimeCountsByCategory(CategoryBooleanSettingProto.newBuilder()
                    .setCategory(entry.getKey())
                    .setValue(Boolean.TRUE.equals(entry.getValue()))
                    .build());
        }
        for (MinimumRequirementDTO minimum : dto.getMinimumRequirements() != null ? dto.getMinimumRequirements() : Collections.<MinimumRequirementDTO>emptyList()) {
            builder.addMinimumRequirements(MinimumRequirementProto.newBuilder()
                    .setCategory(value(minimum.getCategory()))
                    .setType(value(minimum.getType()))
                    .setAmount(minimum.getAmount() != null ? minimum.getAmount() : 0)
                    .setPeriod(value(minimum.getPeriod()))
                    .build());
        }
        return builder.build();
    }

    private String value(String value) {
        return value != null ? value : "";
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
