package com.scheduler.taskmanagement.grpc;

import com.scheduler.taskmanagement.mappers.TaskMapper;
import com.scheduler.taskmanagement.services.TaskService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
