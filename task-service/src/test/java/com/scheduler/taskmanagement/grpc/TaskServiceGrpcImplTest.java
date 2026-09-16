package com.scheduler.taskmanagement.grpc;

import com.scheduler.taskmanagement.mappers.TaskMapper;
import com.scheduler.taskmanagement.services.TaskService;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceGrpcImplTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final TaskServiceGrpcImpl grpcService = new TaskServiceGrpcImpl(taskService, taskMapper);

    @Test
    void listTasksForCustomerUsesRequestedCustomerIdForInternalSchedulerCalls() {
        when(taskService.listTasksForCustomer(123L)).thenReturn(List.of());
        @SuppressWarnings("unchecked")
        StreamObserver<TaskProto> observer = mock(StreamObserver.class);

        grpcService.listTasksForCustomer(
                ListTasksRequest.newBuilder().setCustomerId(123L).build(),
                observer
        );

        verify(taskService).listTasksForCustomer(123L);
        verify(observer).onCompleted();
    }

    @Test
    void updateFlexibleTaskRemainingDurationUsesRequestedCustomerId() {
        FlexibleTaskDTO updated = new FlexibleTaskDTO();
        updated.setId(44L);
        updated.setType(com.scheduler.commoncode.enums.TaskType.FLEXIBLE);
        updated.setStatus(com.scheduler.commoncode.enums.TaskStatus.PENDING);
        updated.setTitle("Study");
        updated.setEstimatedDuration(25);
        updated.setDueDate(LocalDateTime.of(2026, 7, 17, 23, 59));
        when(taskService.updateFlexibleTaskRemainingDuration(eq(44L), eq(123L), eq(25), eq(null), eq(null)))
                .thenReturn(updated);
        TaskProto response = TaskProto.newBuilder()
                .setId(44L)
                .setType(TaskType.FLEXIBLE)
                .setStatus(TaskStatus.PENDING)
                .setEstimatedDuration(25)
                .build();
        when(taskMapper.toTaskProto((TaskDTO) updated)).thenReturn(response);
        @SuppressWarnings("unchecked")
        StreamObserver<TaskProto> observer = mock(StreamObserver.class);

        grpcService.updateFlexibleTaskRemainingDuration(
                UpdateFlexibleTaskRemainingDurationRequest.newBuilder()
                        .setTaskId(44L)
                        .setCustomerId(123L)
                        .setRemainingMinutes(25)
                        .build(),
                observer
        );

        verify(taskService).updateFlexibleTaskRemainingDuration(44L, 123L, 25, null, null);
        verify(observer).onNext(response);
        verify(observer).onCompleted();
    }
}
