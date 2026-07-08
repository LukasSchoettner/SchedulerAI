package com.scheduler.scheduling.services;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.mappers.TaskDtoMapper;
import com.scheduler.commoncode.mappers.CustomerDtoMapper;
import com.scheduler.commoncode.mappers.ZoneConfigDtoMapperCom;
import com.scheduler.routing.grpc.DistanceMatrixProto;
import com.scheduler.routing.grpc.RoutingServiceGrpc;
import com.scheduler.scheduling.models.Schedule;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.SchedulerRunResult;
import com.scheduler.scheduling.scheduler.MasterScheduler;
import com.scheduler.taskmanagement.grpc.ListTasksRequest;
import com.scheduler.taskmanagement.grpc.TaskServiceGrpc;
import com.scheduler.customermanagement.grpc.base.CustomerRequest;
import com.scheduler.customermanagement.grpc.base.CustomerProto;
import com.scheduler.customermanagement.grpc.base.CustomerServiceGrpc;
import com.scheduler.customermanagement.grpc.base.ActiveConfigRequest;
import com.scheduler.customermanagement.grpc.base.ZoneConfigurationProto;
import com.scheduler.customermanagement.grpc.base.ZoneServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

@Service
public class TaskSchedulerService {

    @GrpcClient("task-service")
    private TaskServiceGrpc.TaskServiceBlockingStub taskStub;

    @GrpcClient("customer-service")
    private CustomerServiceGrpc.CustomerServiceBlockingStub customerStub;

    @GrpcClient("zone-service")
    private ZoneServiceGrpc.ZoneServiceBlockingStub zoneStub;

    @GrpcClient("routing-service")
    private RoutingServiceGrpc.RoutingServiceBlockingStub routingStub;

    private final TaskDtoMapper taskDtoMapper;
    private final CustomerDtoMapper customerDtoMapper;
    private final ZoneConfigDtoMapperCom zoneConfigDtoMapperCom;
    private final MasterScheduler masterScheduler;

    public TaskSchedulerService(
            TaskDtoMapper taskDtoMapper,
            CustomerDtoMapper customerDtoMapper,
            ZoneConfigDtoMapperCom zoneConfigDtoMapperCom,
            MasterScheduler masterScheduler
    ) {
        this.taskDtoMapper = taskDtoMapper;
        this.customerDtoMapper = customerDtoMapper;
        this.zoneConfigDtoMapperCom = zoneConfigDtoMapperCom;
        this.masterScheduler = masterScheduler;
    }

    public Schedule scheduleTasksForCustomer(Long customerId) {
        return scheduleTasksForCustomer(customerId, List.of());
    }

    public Schedule scheduleTasksForCustomer(Long customerId, Collection<Long> excludedTaskIds) {
        return scheduleTasksForCustomer(customerId, excludedTaskIds, null);
    }

    public Schedule scheduleTasksForCustomer(Long customerId, Collection<Long> excludedTaskIds, LocalDateTime flexibleStartAfter) {
        return scheduleTasksForCustomer(customerId, excludedTaskIds, flexibleStartAfter, Map.of());
    }

    public Schedule scheduleTasksForCustomer(
            Long customerId,
            Collection<Long> excludedTaskIds,
            LocalDateTime flexibleStartAfter,
            Map<Long, Integer> durationOverrides
    ) {
        // customer
        CustomerProto custProto = customerStub.getCustomerById(
                CustomerRequest.newBuilder().setId(customerId).build()
        );
        CustomerDTO customerDto = customerDtoMapper.toDto(custProto);

        // tasks
        List<TaskDTO> fetchedTasks = new ArrayList<>();
        taskStub.listTasksForCustomer(
                ListTasksRequest.newBuilder().setCustomerId(customerId).build()
        ).forEachRemaining(proto ->
                fetchedTasks.add(taskDtoMapper.toTaskDTO(proto))
        );

        List<TaskDTO> tasks = fetchedTasks;
        Set<Long> excluded = new HashSet<>(excludedTaskIds != null ? excludedTaskIds : List.of());
        if (!excluded.isEmpty()) {
            tasks = tasks.stream()
                    .filter(task -> task.getId() == null || !excluded.contains(task.getId()))
                    .toList();
        }
        if (durationOverrides != null && !durationOverrides.isEmpty()) {
            tasks.stream()
                    .filter(task -> task instanceof com.scheduler.commoncode.dto.FlexibleTaskDTO)
                    .filter(task -> task.getId() != null && durationOverrides.containsKey(task.getId()))
                    .map(com.scheduler.commoncode.dto.FlexibleTaskDTO.class::cast)
                    .forEach(task -> task.setEstimatedDuration(Math.max(5, durationOverrides.get(task.getId()))));
        }

        // zone config
        ZoneConfigurationDTO zoneConfigDto = null;
        try {
            ZoneConfigurationProto zcProto = zoneStub.getActiveConfigByCustomer(
                    ActiveConfigRequest.newBuilder().setCustomerId(customerId).build()
            );
            zoneConfigDto = zoneConfigDtoMapperCom.toDto(zcProto);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                System.out.println("No active zone config for customer " + customerId);
            } else {
                throw e;
            }
        }
        customerDto.setZoneConfiguration(zoneConfigDto);

        // routing – make this safe / optional
        DistanceMatrixProto dm = null;
        try {
            dm = routingStub.getDistanceMatrix(
                    com.scheduler.routing.grpc.DistanceMatrixRequest.newBuilder()
                            .setCustomerId(customerId)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            System.err.println(
                    "Routing call failed for customer " + customerId
                            + " - status: " + e.getStatus()
                            + " - message: " + e.getMessage()
            );
            // You can decide here whether to rethrow for non-auth errors:
            // if (e.getStatus().getCode() != Status.Code.UNAVAILABLE &&
            //     e.getStatus().getCode() != Status.Code.UNAUTHENTICATED &&
            //     e.getStatus().getCode() != Status.Code.NOT_FOUND) {
            //     throw e;
            // }
        }

        SchedulerRunResult schedulerResult = masterScheduler.scheduleTasksWithReliability(
                customerDto,
                tasks,
                dm,
                flexibleStartAfter
        );

        Schedule schedule = new Schedule();
        schedule.setScheduledTasks(schedulerResult.getScheduledTasks());
        schedule.setUnscheduledTasks(schedulerResult.getUnscheduledTasks());
        schedule.setExplanations(schedulerResult.getExplanations());
        return schedule;
    }
}
