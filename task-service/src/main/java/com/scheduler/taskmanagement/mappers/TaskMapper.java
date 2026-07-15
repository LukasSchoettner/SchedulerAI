package com.scheduler.taskmanagement.mappers;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.mappers.DateMapper;
import com.scheduler.taskmanagement.grpc.TaskCreate;
import com.scheduler.taskmanagement.grpc.TaskProto;
import com.scheduler.taskmanagement.models.FixedTask;
import com.scheduler.taskmanagement.models.FlexibleTask;
import com.scheduler.taskmanagement.models.ProjectTask;
import com.scheduler.taskmanagement.models.Task;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = DateMapper.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TaskMapper {

    //
    // 1) DTO → gRPC-Proto
    //
    default TaskProto toTaskProto(TaskDTO dto) {
        if (dto == null) return TaskProto.getDefaultInstance();
        if (dto instanceof FixedTaskDTO)    return toTaskProto((FixedTaskDTO) dto);
        if (dto instanceof FlexibleTaskDTO) return toTaskProto((FlexibleTaskDTO) dto);
        if (dto instanceof ProjectTaskDTO)  return toTaskProto((ProjectTaskDTO) dto);
        throw new IllegalArgumentException("Unknown DTO subtype: " + dto.getClass());
    }

    @Mapping(target="id",               source="id")
    @Mapping(target="title",            source="title")
    @Mapping(target="priority",         source="priority")
    @Mapping(target="dueDate",          source="dueDate",            qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="reminderDate",     source="reminderDate",       qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="startDateTime",    source="startDateTime",      qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="endDateTime",      source="endDateTime",        qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="recurrencePattern",source="recurrencePattern")
    @Mapping(target="description",      source="description")
    @Mapping(target="category",         source="category")
    @Mapping(target="type",             expression="java(com.scheduler.taskmanagement.grpc.TaskType.valueOf(dto.getType().name()))")
    @Mapping(target="status",           expression="java(com.scheduler.taskmanagement.grpc.TaskStatus.valueOf(dto.getStatus().name()))")
    // NEW:
    @Mapping(target="addressId",        source="addressId")
    @Mapping(target="addressText",      source="addressText")
    TaskProto toTaskProto(FixedTaskDTO dto);

    @Mapping(target="id",                         source="id")
    @Mapping(target="title",                      source="title")
    @Mapping(target="priority",                   source="priority")
    @Mapping(target="dueDate",                    source="dueDate",            qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="reminderDate",               source="reminderDate",       qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="earliestStartDateTime",      source="earliestStartDateTime",qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="latestEndDateTime",          source="latestEndDateTime",  qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="estimatedDuration",          source="estimatedDuration")
    @Mapping(target="bufferTime",                 source="bufferTime")
    @Mapping(target="minimalBlockSize",           source="minimalBlockSize")
    @Mapping(target="maximalBlockSize",           source="maximalBlockSize")
    @Mapping(target="canBeSeparated",             source="canBeSeparated")
    @Mapping(target="progressive",                source="progressive")
    @Mapping(target="taskNature",                 expression="java(com.scheduler.taskmanagement.grpc.TaskNature.valueOf(dto.getTaskNature().name()))")
    @Mapping(target="recurrencePattern",          source="recurrencePattern")
    @Mapping(target="description",                source="description")
    @Mapping(target="category",                   source="category")
    @Mapping(target="type",                       expression="java(com.scheduler.taskmanagement.grpc.TaskType.valueOf(dto.getType().name()))")
    @Mapping(target="status",                     expression="java(com.scheduler.taskmanagement.grpc.TaskStatus.valueOf(dto.getStatus().name()))")
    // NEW:
    @Mapping(target="addressId",                  source="addressId")
    @Mapping(target="addressText",                source="addressText")
    TaskProto toTaskProto(FlexibleTaskDTO dto);

    @Mapping(target="id",               source="id")
    @Mapping(target="title",            source="title")
    @Mapping(target="priority",         source="priority")
    @Mapping(target="dueDate",          source="dueDate",            qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="reminderDate",     source="reminderDate",       qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="recurrencePattern",source="recurrencePattern")
    @Mapping(target="description",      source="description")
    @Mapping(target="category",         source="category")
    //@Mapping(target="subTasksList",     source="subTasks")
    @Mapping(target="type",             expression="java(com.scheduler.taskmanagement.grpc.TaskType.PROJECT)")
    @Mapping(target="status",           expression="java(com.scheduler.taskmanagement.grpc.TaskStatus.valueOf(dto.getStatus().name()))")
    // NEW:
    @Mapping(target="addressId",        source="addressId")
    @Mapping(target="addressText",      source="addressText")
    TaskProto toTaskProto(ProjectTaskDTO dto);


    //
    // 2) Create-Proto → DTO
    //
    @Mapping(target="id",                   ignore=true)
    @Mapping(target="title",                source="title")
    @Mapping(target="priority",             source="priority")
    @Mapping(target="dueDate",              source="dueDate",            qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="reminderDate",         source="reminderDate",       qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="startDateTime",        source="startDateTime",      qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="endDateTime",          source="endDateTime",        qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="recurrencePattern",    source="recurrencePattern")
    @Mapping(target="description",          source="description")
    @Mapping(target="category",             source="category")
    @Mapping(target="type",                 source="type")
    @Mapping(target="status",               source="status")
    // NEW:
    @Mapping(target="addressId",            source="addressId", qualifiedByName="positiveAddressId")
    @Mapping(target="addressText",          source="addressText")
    FixedTaskDTO toFixedTaskDTO(TaskCreate proto);

    @Mapping(target="id",                     ignore=true)
    @Mapping(target="title",                  source="title")
    @Mapping(target="priority",               source="priority")
    @Mapping(target="dueDate",                source="dueDate",              qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="reminderDate",           source="reminderDate",         qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="earliestStartDateTime",  source="earliestStartDateTime",qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="latestEndDateTime",      source="latestEndDateTime",    qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="estimatedDuration",      source="estimatedDuration")
    @Mapping(target="bufferTime",             source="bufferTime")
    @Mapping(target="minimalBlockSize",       source="minimalBlockSize")
    @Mapping(target="maximalBlockSize",       source="maximalBlockSize")
    @Mapping(target="canBeSeparated",         source="canBeSeparated")
    @Mapping(target="progressive",            source="progressive")
    @Mapping(target="taskNature",             source="taskNature")
    @Mapping(target="recurrencePattern",      source="recurrencePattern")
    @Mapping(target="description",            source="description")
    @Mapping(target="category",               source="category")
    @Mapping(target="type",                   source="type")
    @Mapping(target="status",                 source="status")
    // NEW:
    @Mapping(target="addressId",              source="addressId", qualifiedByName="positiveAddressId")
    @Mapping(target="addressText",            source="addressText")
    FlexibleTaskDTO toFlexibleTaskDTO(TaskCreate proto);

    @Mapping(target="id",                   ignore=true)
    @Mapping(target="title",                source="title")
    @Mapping(target="priority",             source="priority")
    @Mapping(target="dueDate",              source="dueDate",            qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="reminderDate",         source="reminderDate",       qualifiedByName="timestampToLocalDateTime")
    //@Mapping(target="subTasks",             source="subTasksList")
    @Mapping(target="recurrencePattern",    source="recurrencePattern")
    @Mapping(target="description",          source="description")
    @Mapping(target="category",             source="category")
    @Mapping(target="type",                 source="type")
    @Mapping(target="status",               source="status")
    // NEW:
    @Mapping(target="addressId",            source="addressId", qualifiedByName="positiveAddressId")
    @Mapping(target="addressText",          source="addressText")
    ProjectTaskDTO toProjectTaskDTO(TaskCreate proto);

    default TaskDTO toTaskDTO(TaskCreate proto) {
        return switch(proto.getType()) {
            case FIXED    -> toFixedTaskDTO(proto);
            case FLEXIBLE -> toFlexibleTaskDTO(proto);
            case PROJECT  -> toProjectTaskDTO(proto);
            default       -> throw new IllegalArgumentException("Unknown create type: " + proto.getType());
        };
    }


    //
    // 3) Domain → gRPC-Proto
    //
    default TaskProto toTaskProto(Task task) {
        if (task instanceof FixedTask)    return toTaskProto((FixedTask) task);
        if (task instanceof FlexibleTask) return toTaskProto((FlexibleTask) task);
        if (task instanceof ProjectTask)  return toTaskProto((ProjectTask) task);
        return TaskProto.getDefaultInstance();
    }

    @Mapping(target="id",               source="id")
    @Mapping(target="title",            source="title")
    @Mapping(target="priority",         source="priority")
    @Mapping(target="dueDate",          source="dueDate",            qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="reminderDate",     source="reminderDate",       qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="startDateTime",    source="startDateTime",      qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="endDateTime",      source="endDateTime",        qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="recurrencePattern",source="recurrencePattern")
    @Mapping(target="description",      source="description")
    @Mapping(target="category",         source="category")
    @Mapping(target="type",             expression="java(com.scheduler.taskmanagement.grpc.TaskType.valueOf(task.getType().name()))")
    @Mapping(target="status",           expression="java(com.scheduler.taskmanagement.grpc.TaskStatus.valueOf(task.getStatus().name()))")
    // NEW:
    @Mapping(target="addressId",        source="addressId")
    @Mapping(target="addressText",      source="addressText")
    TaskProto toTaskProto(FixedTask task);

    @Mapping(target="id",                         source="id")
    @Mapping(target="title",                      source="title")
    @Mapping(target="priority",                   source="priority")
    @Mapping(target="dueDate",                    source="dueDate",            qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="reminderDate",               source="reminderDate",       qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="earliestStartDateTime",      source="earliestStartDateTime",qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="latestEndDateTime",          source="latestEndDateTime", qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="estimatedDuration",          source="estimatedDuration")
    @Mapping(target="bufferTime",                 source="bufferTime")
    @Mapping(target="minimalBlockSize",           source="minimalBlockSize")
    @Mapping(target="maximalBlockSize",           source="maximalBlockSize")
    @Mapping(target="canBeSeparated",             source="canBeSeparated")
    @Mapping(target="progressive",                source="progressive")
    @Mapping(target="taskNature",                 expression="java(com.scheduler.taskmanagement.grpc.TaskNature.valueOf(task.getTaskNature().name()))")
    @Mapping(target="recurrencePattern",          source="recurrencePattern")
    @Mapping(target="description",                source="description")
    @Mapping(target="category",                   source="category")
    @Mapping(target="type",                       expression="java(com.scheduler.taskmanagement.grpc.TaskType.valueOf(task.getType().name()))")
    @Mapping(target="status",                     expression="java(com.scheduler.taskmanagement.grpc.TaskStatus.valueOf(task.getStatus().name()))")
    // NEW:
    @Mapping(target="addressId",                  source="addressId")
    @Mapping(target="addressText",                source="addressText")
    TaskProto toTaskProto(FlexibleTask task);

    @Mapping(target="id",               source="id")
    @Mapping(target="title",            source="title")
    @Mapping(target="priority",         source="priority")
    @Mapping(target="dueDate",          source="dueDate",            qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="reminderDate",     source="reminderDate",       qualifiedByName="localDateTimeToTimestamp")
    @Mapping(target="recurrencePattern",source="recurrencePattern")
    @Mapping(target="description",      source="description")
    @Mapping(target="category",         source="category")
    //@Mapping(target="subTasksList",     source="subTasks")
    @Mapping(target="type",             expression="java(com.scheduler.taskmanagement.grpc.TaskType.PROJECT)")
    @Mapping(target="status",           expression="java(com.scheduler.taskmanagement.grpc.TaskStatus.valueOf(task.getStatus().name()))")
    // NEW:
    @Mapping(target="addressId",        source="addressId")
    @Mapping(target="addressText",      source="addressText")
    TaskProto toTaskProto(ProjectTask task);


    //
    // 4) gRPC-Proto → DTO
    //
    @Mapping(target="id",                 source="id")
    @Mapping(target="title",              source="title")
    @Mapping(target="priority",           source="priority")
    @Mapping(target="dueDate",            source="dueDate", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="reminderDate",       source="reminderDate", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="startDateTime",      source="startDateTime", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="endDateTime",        source="endDateTime", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="recurrencePattern",  source="recurrencePattern")
    @Mapping(target="description",        source="description")
    @Mapping(target="category",           source="category")
    @Mapping(target="type",               expression="java(toDomainTaskType(proto.getType()))")
    @Mapping(target="status",             expression="java(toDomainTaskStatus(proto.getStatus()))")
    // NEW:
    @Mapping(target="addressId",          source="addressId", qualifiedByName="positiveAddressId")
    @Mapping(target="addressText",        source="addressText")
    FixedTaskDTO toFixedTaskDTO(TaskProto proto);

    @Mapping(target="id",                         source="id")
    @Mapping(target="title",                      source="title")
    @Mapping(target="priority",                   source="priority")
    @Mapping(target="dueDate",                    source="dueDate", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="reminderDate",               source="reminderDate", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="earliestStartDateTime",      source="earliestStartDateTime", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="latestEndDateTime",          source="latestEndDateTime",   qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="estimatedDuration",          source="estimatedDuration")
    @Mapping(target="bufferTime",                 source="bufferTime")
    @Mapping(target="minimalBlockSize",           source="minimalBlockSize")
    @Mapping(target="maximalBlockSize",           source="maximalBlockSize")
    @Mapping(target="canBeSeparated",             source="canBeSeparated")
    @Mapping(target="progressive",                source="progressive")
    @Mapping(target="taskNature",                 expression="java(toDomainTaskNature(proto.getTaskNature()))")
    @Mapping(target="recurrencePattern",          source="recurrencePattern")
    @Mapping(target="description",                source="description")
    @Mapping(target="category",                   source="category")
    @Mapping(target="type",                       expression="java(toDomainTaskType(proto.getType()))")
    @Mapping(target="status",                     expression="java(toDomainTaskStatus(proto.getStatus()))")
    // NEW:
    @Mapping(target="addressId",                  source="addressId", qualifiedByName="positiveAddressId")
    @Mapping(target="addressText",                source="addressText")
    FlexibleTaskDTO toFlexibleTaskDTO(TaskProto proto);

    @Mapping(target="id",                 source="id")
    @Mapping(target="title",              source="title")
    @Mapping(target="priority",           source="priority")
    @Mapping(target="dueDate",            source="dueDate", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="reminderDate",       source="reminderDate", qualifiedByName="timestampToLocalDateTime")
    @Mapping(target="recurrencePattern",  source="recurrencePattern")
    @Mapping(target="description",        source="description")
    @Mapping(target="category",           source="category")
    //@Mapping(target="subTasks",           source="subTasksList")
    @Mapping(target="type",               expression="java(toDomainTaskType(proto.getType()))")
    @Mapping(target="status",             expression="java(toDomainTaskStatus(proto.getStatus()))")
    // NEW:
    @Mapping(target="addressId",          source="addressId", qualifiedByName="positiveAddressId")
    @Mapping(target="addressText",        source="addressText")
    ProjectTaskDTO toProjectTaskDTO(TaskProto proto);

    default TaskDTO toTaskDTO(TaskProto proto) {
        return switch(proto.getType()) {
            case FIXED    -> toFixedTaskDTO(proto);
            case FLEXIBLE -> toFlexibleTaskDTO(proto);
            case PROJECT  -> toProjectTaskDTO(proto);
            default       -> throw new IllegalArgumentException("Unknown proto type: " + proto.getType());
        };
    }

    //
    // Helpers: enum fallbacks
    //
    @ValueMappings({
            @ValueMapping(source="UNRECOGNIZED", target = MappingConstants.NULL)
    })
    TaskType toDomainTaskType(com.scheduler.taskmanagement.grpc.TaskType grpcType);

    @ValueMappings({
            @ValueMapping(source="TASK_STATUS_UNSPECIFIED", target=MappingConstants.NULL),
            @ValueMapping(source="UNRECOGNIZED",            target=MappingConstants.NULL)
    })
    TaskStatus toDomainTaskStatus(com.scheduler.taskmanagement.grpc.TaskStatus grpcStatus);

    @ValueMappings({
            @ValueMapping(source="UNRECOGNIZED", target=MappingConstants.NULL)
    })
    TaskNature toDomainTaskNature(com.scheduler.taskmanagement.grpc.TaskNature grpcNature);

    @Named("positiveAddressId")
    default Long positiveAddressId(long addressId) {
        return addressId > 0 ? addressId : null;
    }
}
