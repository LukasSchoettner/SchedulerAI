package com.scheduler.commoncode.mappers;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.MinimumRequirementDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.customermanagement.grpc.base.*;
import org.mapstruct.Mapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CustomerDtoMapper {

    default CustomerDTO toDto(CustomerProto proto) {
        if (proto == null) return null;
        CustomerDTO dto = new CustomerDTO();
        dto.setId(proto.getId());
        dto.setCustomername(proto.getCustomername());
        dto.setEmail(proto.getEmail());
        dto.setActive(proto.getActive());
        dto.setMembershipLevel(map(proto.getMembershipLevel()));
        if (proto.hasSchedulingPreference()) {
            dto.setSchedulingPreference(toDto(proto.getSchedulingPreference()));
        }
        return dto;
    }

    default SchedulingPreferenceDTO toDto(SchedulingPreferenceProto proto) {
        SchedulingPreferenceDTO dto = new SchedulingPreferenceDTO();
        dto.setId(proto.getId() > 0 ? proto.getId() : null);
        dto.setPrimaryPriority(blankToNull(proto.getPrimaryPriority()));
        dto.setCategoryPriorityOrder(proto.getCategoryPriorityOrderList().isEmpty()
                ? defaultCategoryPriorityOrder()
                : proto.getCategoryPriorityOrderList());
        dto.setFixedCommitmentCategories(new HashSet<>(proto.getFixedCommitmentCategoriesList()));
        dto.setWorkFlexibility(blankToNull(proto.getWorkFlexibility()));
        dto.setHealthConstraints(new HashSet<>(proto.getHealthConstraintsList()));
        dto.setAllocationMode(blankToNull(proto.getAllocationMode()));
        dto.setTaskCountTargets(proto.getTaskCountTargetsList().stream()
                .collect(Collectors.toMap(CategoryIntTargetProto::getCategory, CategoryIntTargetProto::getValue)));
        dto.setPlannedHoursPerDayMinutes(proto.getPlannedHoursPerDayMinutes() > 0 ? proto.getPlannedHoursPerDayMinutes() : null);
        dto.setTimeBudgetTargets(proto.getTimeBudgetTargetsList().stream()
                .collect(Collectors.toMap(CategoryIntTargetProto::getCategory, CategoryIntTargetProto::getValue)));
        dto.setFixedTimeBudgetMode(blankToNull(proto.getFixedTimeBudgetMode()));
        dto.setFixedTimeCountsByCategory(proto.getFixedTimeCountsByCategoryList().stream()
                .collect(Collectors.toMap(CategoryBooleanSettingProto::getCategory, CategoryBooleanSettingProto::getValue)));
        dto.setMinimumRequirements(proto.getMinimumRequirementsList().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        dto.setPauseMinutes(proto.getPauseMinutes() > 0 ? proto.getPauseMinutes() : 5);
        dto.setPauseOverloadBehavior(blankToNull(proto.getPauseOverloadBehavior()));
        dto.setPlanningFullness(blankToNull(proto.getPlanningFullness()));
        dto.setOverloadReductionOrder(proto.getOverloadReductionOrderList());
        dto.setTemporaryMode(blankToNull(proto.getTemporaryMode()));
        if (!proto.getTemporaryUntil().isBlank()) {
            dto.setTemporaryUntil(LocalDate.parse(proto.getTemporaryUntil()));
        }
        return dto;
    }

    default MinimumRequirementDTO toDto(MinimumRequirementProto proto) {
        return new MinimumRequirementDTO(
                blankToNull(proto.getCategory()),
                blankToNull(proto.getType()),
                proto.getAmount() > 0 ? proto.getAmount() : null,
                blankToNull(proto.getPeriod())
        );
    }

    default CustomerProto toProto(CustomerDTO dto) {
        CustomerProto.Builder builder = CustomerProto.newBuilder()
                .setId(dto.getId() != null ? dto.getId() : 0)
                .setCustomername(dto.getCustomername() != null ? dto.getCustomername() : "")
                .setEmail(dto.getEmail() != null ? dto.getEmail() : "")
                .setActive(dto.isActive())
                .setMembershipLevel(map(dto.getMembershipLevel()));
        return builder.build();
    }

    default MembershipLevel map(com.scheduler.customermanagement.grpc.base.MembershipLevel grpcLevel) {
        return switch (grpcLevel) {
            case PLUS -> MembershipLevel.PLUS;
            case PREMIUM -> MembershipLevel.PREMIUM;
            case BASIC, UNRECOGNIZED -> MembershipLevel.BASIC;
        };
    }

    default com.scheduler.customermanagement.grpc.base.MembershipLevel map(MembershipLevel dtoLevel) {
        if (dtoLevel == null) return com.scheduler.customermanagement.grpc.base.MembershipLevel.BASIC;
        return switch (dtoLevel) {
            case PLUS -> com.scheduler.customermanagement.grpc.base.MembershipLevel.PLUS;
            case PREMIUM -> com.scheduler.customermanagement.grpc.base.MembershipLevel.PREMIUM;
            case BASIC -> com.scheduler.customermanagement.grpc.base.MembershipLevel.BASIC;
        };
    }

    default String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    default java.util.List<String> defaultCategoryPriorityOrder() {
        return java.util.List.of("Work", "Duty", "Health", "Social", "Sport", "Leisure");
    }
}
