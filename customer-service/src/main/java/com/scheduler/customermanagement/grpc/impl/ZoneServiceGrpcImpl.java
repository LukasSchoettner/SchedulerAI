package com.scheduler.customermanagement.grpc.impl;

import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.customermanagement.grpc.base.*;
import com.scheduler.customermanagement.models.ZoneConfiguration;
import com.scheduler.customermanagement.models.ZoneDefinition;
import com.scheduler.customermanagement.repositories.ZoneConfigurationRepository;
import com.scheduler.customermanagement.repositories.ZoneDefinitionRepository;
import com.scheduler.customermanagement.services.ZoneService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GrpcService
public class ZoneServiceGrpcImpl extends ZoneServiceGrpc.ZoneServiceImplBase {

    @Autowired
    private ZoneConfigurationRepository zoneConfigurationRepository;
    @Autowired
    private ZoneDefinitionRepository zoneDefinitionRepository;
    @Autowired
    private ZoneService zoneService;

    @Override
    public void createZoneConfiguration(ZoneConfigCreateRequest request, StreamObserver<ZoneConfigurationProto> responseObserver) {
        try {
            ZoneConfigurationDTO dto = zoneService.createZoneConfig(
                    request.getCustomerId(),
                    request.getName(),
                    request.getActive(),
                    LocalTime.parse(request.getStartTime()),
                    LocalTime.parse(request.getEndTime())
            );
            ZoneConfigurationProto response = buildZoneConfigurationProto(dto);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getZoneConfigurationById(ZoneConfigRequest request, StreamObserver<ZoneConfigurationProto> responseObserver) {
        try {
            var entity = zoneConfigurationRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("ZoneConfiguration not found"));
            ZoneConfigurationProto response = buildZoneConfigurationProto(entity);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getAllZoneConfigsForCustomer(GetAllConfigsRequest request, StreamObserver<ZoneConfigList> responseObserver) {
        try {
            List<ZoneConfigurationProto> protoList = zoneConfigurationRepository.findAllByCustomerId(request.getCustomerId()).stream()
                    .map(this::buildZoneConfigurationProto)
                    .collect(Collectors.toList());
            ZoneConfigList response = ZoneConfigList.newBuilder().addAllConfigs(protoList).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void addZoneDefinition(ZoneDefinitionCreateRequest request, StreamObserver<ZoneConfigurationProto> responseObserver) {
        try {
            var config = zoneConfigurationRepository.findById(request.getZoneConfigId())
                    .orElseThrow(() -> new RuntimeException("ZoneConfiguration not found"));

            ZoneDefinition definition = getZoneDefinition(request, config.getId());
            zoneDefinitionRepository.save(definition);

            ZoneConfigurationProto response = buildZoneConfigurationProto(config);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private static ZoneDefinition getZoneDefinition(ZoneDefinitionCreateRequest request, Long zoneConfigId) {
        ZoneDefinition definition = new ZoneDefinition();
        definition.setTitle(request.getTitle());
        definition.setDayMask(request.getDayMask());
        definition.setStartTime(LocalTime.parse(request.getStartTime()));
        definition.setEndTime(LocalTime.parse(request.getEndTime()));
        definition.setAllowedCategories(new HashSet<>(request.getAllowedCategoriesList()));
        definition.setExcludedCategories(new HashSet<>(request.getExcludedCategoriesList()));
        definition.setPriorityOverrideThreshold(normalizePriorityOverrideThreshold(request.getPriorityOverrideThreshold()));
        definition.setPrimaryCategory(blankToNull(request.getPrimaryCategory()));
        definition.setSecondaryCategories(new LinkedHashSet<>(request.getSecondaryCategoriesList()));
        definition.setBehaviorMode(normalizeBehaviorMode(request.getBehaviorMode()));
        deriveAllowedCategories(definition);
        definition.setZoneConfigId(zoneConfigId);
        return definition;
    }

    @Override
    public void deleteZoneConfiguration(ZoneConfigRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            if (zoneConfigurationRepository.existsById(request.getId())) {
                zoneConfigurationRepository.deleteById(request.getId());
            }
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getActiveConfigByCustomer(ActiveConfigRequest req,
                                          StreamObserver<ZoneConfigurationProto> obs) {
        try {
            ZoneConfigurationDTO dto = zoneService.getActiveConfig(req.getCustomerId());
            if (dto == null) {
                obs.onError(Status.NOT_FOUND.asRuntimeException());
                return;
            }
            obs.onNext(buildZoneConfigurationProto(dto));
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(Status.INTERNAL
                    .withDescription("Error fetching active config: "+e.getMessage())
                    .asRuntimeException());
        }
    }

    private ZoneConfigurationProto buildZoneConfigurationProto(ZoneConfiguration config) {
        ZoneConfigurationProto.Builder builder = ZoneConfigurationProto.newBuilder()
                .setId(config.getId())
                .setName(config.getName())
                .setActive(config.isActive())
                .setCustomerId(config.getCustomerId())
                .setStartTime(config.getStartTime().toString())
                .setEndTime(config.getEndTime().toString());

        for (ZoneDefinition def : config.getZones()) {
            builder.addZones(buildZoneDefinitionProto(def));
        }
        return builder.build();
    }

    private ZoneConfigurationProto buildZoneConfigurationProto(ZoneConfigurationDTO dto) {
        ZoneConfigurationProto.Builder builder = ZoneConfigurationProto.newBuilder()
                .setId(dto.getId())
                .setName(dto.getName())
                .setActive(dto.isActive())
                .setCustomerId(dto.getCustomerId())
                .setStartTime(dto.getStartTime().toString())
                .setEndTime(dto.getEndTime().toString());

        if (dto.getZones() != null) {
            for (var def : dto.getZones()) {
                builder.addZones(
                        ZoneDefinitionProto.newBuilder()
                                .setId(def.getId())
                                .setTitle(def.getTitle())
                                .setDayMask(def.getDayMask())
                                .setStartTime(def.getStartTime().toString())
                                .setEndTime(def.getEndTime().toString())
                                .addAllAllowedCategories(def.getAllowedCategories())
                                .addAllExcludedCategories(def.getExcludedCategories())
                                .setPriorityOverrideThreshold(protoPriorityOverrideThreshold(def.getPriorityOverrideThreshold()))
                                .setPrimaryCategory(def.getPrimaryCategory() != null ? def.getPrimaryCategory() : "")
                                .addAllSecondaryCategories(def.getSecondaryCategories() != null ? def.getSecondaryCategories() : Set.of())
                                .setBehaviorMode(def.getBehaviorMode() != null ? def.getBehaviorMode() : "STRICT")
                                .setZoneConfigId(dto.getId())
                                .build()
                );
            }
        }

        return builder.build();
    }

    private ZoneDefinitionProto buildZoneDefinitionProto(ZoneDefinition def) {
        ZoneDefinitionProto.Builder builder = ZoneDefinitionProto.newBuilder()
                .setId(def.getId())
                .setTitle(def.getTitle())
                .setDayMask(def.getDayMask())
                .setStartTime(def.getStartTime().toString())
                .setEndTime(def.getEndTime().toString())
                .setPriorityOverrideThreshold(protoPriorityOverrideThreshold(def.getPriorityOverrideThreshold()))
                .setZoneConfigId(def.getZoneConfigId());

        if (def.getAllowedCategories() != null) {
            builder.addAllAllowedCategories(def.getAllowedCategories());
        }
        if (def.getExcludedCategories() != null) {
            builder.addAllExcludedCategories(def.getExcludedCategories());
        }
        if (def.getPrimaryCategory() != null) {
            builder.setPrimaryCategory(def.getPrimaryCategory());
        }
        if (def.getSecondaryCategories() != null) {
            builder.addAllSecondaryCategories(def.getSecondaryCategories());
        }
        builder.setBehaviorMode(def.getBehaviorMode() != null ? def.getBehaviorMode() : "STRICT");
        return builder.build();
    }

    private static Integer normalizePriorityOverrideThreshold(Integer threshold) {
        return threshold != null && threshold > 0 ? threshold : null;
    }

    private static int protoPriorityOverrideThreshold(Integer threshold) {
        return threshold != null && threshold > 0 ? threshold : 0;
    }

    private static String normalizeBehaviorMode(String behaviorMode) {
        if ("PREFERRED".equalsIgnoreCase(behaviorMode)) {
            return "PREFERRED";
        }
        return "STRICT";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void deriveAllowedCategories(ZoneDefinition definition) {
        Set<String> derived = new LinkedHashSet<>();
        if (definition.getPrimaryCategory() != null) {
            derived.add(definition.getPrimaryCategory());
        }
        if (definition.getSecondaryCategories() != null) {
            derived.addAll(definition.getSecondaryCategories());
        }
        if (!derived.isEmpty()) {
            definition.setAllowedCategories(derived);
        }
    }

    @PostConstruct
    public void debugStartup() {
        System.out.println("⚠️ ZoneServiceGrpcImpl has been initialized!");
    }
}
