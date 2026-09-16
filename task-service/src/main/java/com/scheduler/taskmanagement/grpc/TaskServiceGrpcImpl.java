// src/main/java/com/scheduler/taskmanagement/grpc/TaskServiceGrpcImpl.java
package com.scheduler.taskmanagement.grpc;

import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.grpc.JwtGrpcServerInterceptor;
import com.scheduler.taskmanagement.mappers.TaskMapper;
import com.scheduler.taskmanagement.services.TaskService;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@GrpcService
public class TaskServiceGrpcImpl extends TaskServiceGrpc.TaskServiceImplBase {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskServiceGrpcImpl(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    private Long currentCustomerId() {
        return JwtGrpcServerInterceptor.CUSTOMER_ID_CTX_KEY.get();
    }

    private long requireCurrentCustomerId() {
        Long cid = currentCustomerId();
        if (cid == null) {
            throw Status.UNAUTHENTICATED
                    .withDescription("Missing customer id in gRPC context")
                    .asRuntimeException();
        }
        return cid;
    }

    @Override
    public void listTasksForCustomer(ListTasksRequest req, StreamObserver<TaskProto> obs) {
        long cid = req.getCustomerId() > 0 ? req.getCustomerId() : requireCurrentCustomerId();
        taskService
                .listTasksForCustomer(cid)
                .forEach(dto -> obs.onNext(taskMapper.toTaskProto(dto)));
        obs.onCompleted();
    }

    @Override
    public void createTask(TaskCreate req, StreamObserver<TaskProto> obs) {
        long cid = req.getCustomerId() > 0 ? req.getCustomerId() : requireCurrentCustomerId();
        TaskDTO dto = taskMapper.toTaskDTO(req);
        TaskDTO saved = taskService.createTask(dto, cid);
        obs.onNext(taskMapper.toTaskProto(saved));
        obs.onCompleted();
    }

    @Override
    public void updateTaskStatus(UpdateTaskStatusRequest req, StreamObserver<TaskProto> obs) {
        long cid = req.getCustomerId() > 0 ? req.getCustomerId() : requireCurrentCustomerId();
        com.scheduler.commoncode.enums.TaskStatus status =
                com.scheduler.commoncode.enums.TaskStatus.valueOf(req.getStatus().name());
        TaskDTO updated = taskService.updateStatus(req.getId(), cid, status);
        obs.onNext(taskMapper.toTaskProto(updated));
        obs.onCompleted();
    }

    @Override
    public void updateFlexibleTaskRemainingDuration(UpdateFlexibleTaskRemainingDurationRequest req, StreamObserver<TaskProto> obs) {
        long cid = req.getCustomerId() > 0 ? req.getCustomerId() : requireCurrentCustomerId();
        try {
            TaskDTO updated = taskService.updateFlexibleTaskRemainingDuration(
                    req.getTaskId(),
                    cid,
                    req.getRemainingMinutes(),
                    req.hasEarliestStartDateTime() ? timestampToLocalDateTime(req.getEarliestStartDateTime()) : null,
                    req.hasDueDate() ? timestampToLocalDateTime(req.getDueDate()) : null
            );
            obs.onNext(taskMapper.toTaskProto(updated));
            obs.onCompleted();
        } catch (ResponseStatusException e) {
            obs.onError(toGrpcStatus(e).withDescription(e.getReason()).asRuntimeException());
        }
    }

    @Override
    public void updateTask(TaskProto req, StreamObserver<TaskProto> obs) {
        long cid = requireCurrentCustomerId();
        TaskDTO dto = taskMapper.toTaskDTO(req);
        taskService
                .updateTask(req.getId(), dto, cid)
                .ifPresentOrElse(
                        updated -> {
                            obs.onNext(taskMapper.toTaskProto(updated));
                            obs.onCompleted();
                        },
                        () -> obs.onError(Status.NOT_FOUND
                                .withDescription("Task not found or not yours")
                                .asRuntimeException())
                );
    }

    @Override
    public void getTaskById(TaskRequest req, StreamObserver<TaskProto> obs) {
        long cid = requireCurrentCustomerId();
        taskService
                .getTaskById(req.getId(), cid)
                .ifPresentOrElse(
                        dto -> {
                            obs.onNext(taskMapper.toTaskProto(dto));
                            obs.onCompleted();
                        },
                        () -> obs.onError(Status.NOT_FOUND
                                .withDescription("Task not found")
                                .asRuntimeException())
                );
    }

    @Override
    public void deleteTask(TaskRequest req, StreamObserver<TaskDeleteResponse> obs) {
        long cid = requireCurrentCustomerId();
        boolean ok = taskService.deleteTask(req.getId(), cid);
        obs.onNext(TaskDeleteResponse.newBuilder().setSuccess(ok).build());
        obs.onCompleted();
    }

    private Status toGrpcStatus(ResponseStatusException exception) {
        return switch (exception.getStatusCode().value()) {
            case 400 -> Status.INVALID_ARGUMENT;
            case 403 -> Status.PERMISSION_DENIED;
            case 404 -> Status.NOT_FOUND;
            default -> Status.UNKNOWN;
        };
    }

    private LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
        return LocalDateTime.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos(), ZoneOffset.UTC);
    }
}
