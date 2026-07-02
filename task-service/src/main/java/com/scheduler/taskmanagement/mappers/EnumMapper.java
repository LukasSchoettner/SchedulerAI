package com.scheduler.taskmanagement.mappers;

import com.scheduler.commoncode.enums.TaskType;

public class EnumMapper {
    public static TaskType toDomainTaskType(
            com.scheduler.taskmanagement.grpc.TaskType grpcType)
    {
        if (grpcType == null) {
            return null; // or throw an exception
        }
        return switch (grpcType) {
            case FIXED -> TaskType.FIXED;
            case FLEXIBLE -> TaskType.FLEXIBLE;
            case PROJECT -> TaskType.PROJECT;
            default -> throw new IllegalArgumentException("Unknown gRPC type: " + grpcType);
        };
    }

    public static com.scheduler.taskmanagement.grpc.TaskType toGrpcTaskType(
            TaskType domainType)
    {
        if (domainType == null) {
            return com.scheduler.taskmanagement.grpc.TaskType.FIXED; // or some default
        }
        return switch (domainType) {
            case FIXED -> com.scheduler.taskmanagement.grpc.TaskType.FIXED;
            case FLEXIBLE -> com.scheduler.taskmanagement.grpc.TaskType.FLEXIBLE;
            case PROJECT -> com.scheduler.taskmanagement.grpc.TaskType.PROJECT;
        };
    }

    // do the same approach for TaskStatus and TaskNature
}
