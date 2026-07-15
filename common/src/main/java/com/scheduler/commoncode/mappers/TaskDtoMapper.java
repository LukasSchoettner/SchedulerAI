package com.scheduler.commoncode.mappers;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.taskmanagement.grpc.TaskCreate;
import com.scheduler.taskmanagement.grpc.TaskProto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

@Mapper(componentModel = "spring", uses = DateMapper.class)
public interface TaskDtoMapper {

    // --- TaskCreate → DTO ------------------------------------------------

    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "title",                source = "title")
    @Mapping(target = "priority",             source = "priority")
    @Mapping(target = "dueDate",              source = "dueDate",            qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "reminderDate",         source = "reminderDate",       qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "startDateTime",        source = "startDateTime",      qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "endDateTime",          source = "endDateTime",        qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "recurrencePattern",    source = "recurrencePattern")
    @Mapping(target = "description",          source = "description")
    @Mapping(target = "category",             source = "category")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "type",                 expression = "java(TaskType.valueOf(create.getType().name()))")
    // NEW:
    @Mapping(target = "addressId",            source = "addressId", qualifiedByName = "positiveAddressId")
    @Mapping(target = "addressText",          source = "addressText")
    FixedTaskDTO toFixedTaskDTO(TaskCreate create);

    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "title",                source = "title")
    @Mapping(target = "priority",             source = "priority")
    @Mapping(target = "dueDate",              source = "dueDate",                qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "reminderDate",         source = "reminderDate",           qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "earliestStartDateTime",source = "earliestStartDateTime",  qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "latestEndDateTime",    source = "latestEndDateTime",      qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "estimatedDuration",    source = "estimatedDuration")
    @Mapping(target = "bufferTime",           source = "bufferTime")
    @Mapping(target = "minimalBlockSize",     source = "minimalBlockSize")
    @Mapping(target = "maximalBlockSize",     source = "maximalBlockSize")
    @Mapping(target = "canBeSeparated",       source = "canBeSeparated")
    @Mapping(target = "progressive",          source = "progressive")
    @Mapping(target = "taskNature",           source = "taskNature")
    @Mapping(target = "recurrencePattern",    source = "recurrencePattern")
    @Mapping(target = "description",          source = "description")
    @Mapping(target = "category",             source = "category")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "type",                 expression = "java(TaskType.valueOf(create.getType().name()))")
    // NEW:
    @Mapping(target = "addressId",            source = "addressId", qualifiedByName = "positiveAddressId")
    @Mapping(target = "addressText",          source = "addressText")
    FlexibleTaskDTO toFlexibleTaskDTO(TaskCreate create);

    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "title",                source = "title")
    @Mapping(target = "priority",             source = "priority")
    @Mapping(target = "dueDate",              source = "dueDate",            qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "reminderDate",         source = "reminderDate",       qualifiedByName = "timestampToLocalDateTime")
    //@Mapping(target = "subTasks",             source = "subTasksList")
    @Mapping(target = "recurrencePattern",    source = "recurrencePattern")
    @Mapping(target = "description",          source = "description")
    @Mapping(target = "category",             source = "category")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "type",                 expression = "java(TaskType.valueOf(create.getType().name()))")
    // NEW:
    @Mapping(target = "addressId",            source = "addressId", qualifiedByName = "positiveAddressId")
    @Mapping(target = "addressText",          source = "addressText")
    ProjectTaskDTO toProjectTaskDTO(TaskCreate create);

    default TaskDTO toTaskDTO(TaskCreate create) {
        return switch (create.getType()) {
            case FIXED    -> toFixedTaskDTO(create);
            case FLEXIBLE -> toFlexibleTaskDTO(create);
            case PROJECT  -> toProjectTaskDTO(create);
            default       -> throw new IllegalArgumentException("Unknown type: " + create.getType());
        };
    }


    // --- TaskProto → DTO -------------------------------------------------

    @Mapping(target = "id",                   source = "id")
    @Mapping(target = "title",                source = "title")
    @Mapping(target = "priority",             source = "priority")
    @Mapping(target = "dueDate",              source = "dueDate",            qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "reminderDate",         source = "reminderDate",       qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "startDateTime",        source = "startDateTime",      qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "endDateTime",          source = "endDateTime",        qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "recurrencePattern",    source = "recurrencePattern")
    @Mapping(target = "description",          source = "description")
    @Mapping(target = "category",             source = "category")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "type",                 expression = "java(TaskType.valueOf(proto.getType().name()))")
    // NEW:
    @Mapping(target = "addressId",            source = "addressId", qualifiedByName = "positiveAddressId")
    @Mapping(target = "addressText",          source = "addressText")
    FixedTaskDTO toFixedTaskDTO(TaskProto proto);

    @Mapping(target = "id",                   source = "id")
    @Mapping(target = "title",                source = "title")
    @Mapping(target = "priority",             source = "priority")
    @Mapping(target = "dueDate",              source = "dueDate",                qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "reminderDate",         source = "reminderDate",           qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "earliestStartDateTime",source = "earliestStartDateTime",  qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "latestEndDateTime",    source = "latestEndDateTime",      qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "estimatedDuration",    source = "estimatedDuration")
    @Mapping(target = "bufferTime",           source = "bufferTime")
    @Mapping(target = "minimalBlockSize",     source = "minimalBlockSize")
    @Mapping(target = "maximalBlockSize",     source = "maximalBlockSize")
    @Mapping(target = "canBeSeparated",       source = "canBeSeparated")
    @Mapping(target = "progressive",          source = "progressive")
    @Mapping(target = "taskNature",           source = "taskNature")
    @Mapping(target = "recurrencePattern",    source = "recurrencePattern")
    @Mapping(target = "description",          source = "description")
    @Mapping(target = "category",             source = "category")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "type",                 expression = "java(TaskType.valueOf(proto.getType().name()))")
    // NEW:
    @Mapping(target = "addressId",            source = "addressId", qualifiedByName = "positiveAddressId")
    @Mapping(target = "addressText",          source = "addressText")
    FlexibleTaskDTO toFlexibleTaskDTO(TaskProto proto);

    @Mapping(target = "id",                   source = "id")
    @Mapping(target = "title",                source = "title")
    @Mapping(target = "priority",             source = "priority")
    @Mapping(target = "dueDate",              source = "dueDate",            qualifiedByName = "timestampToLocalDateTime")
    @Mapping(target = "reminderDate",         source = "reminderDate",       qualifiedByName = "timestampToLocalDateTime")
    //@Mapping(target = "subTasks",             source = "subTasksList")
    @Mapping(target = "recurrencePattern",    source = "recurrencePattern")
    @Mapping(target = "description",          source = "description")
    @Mapping(target = "category",             source = "category")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "type",                 expression = "java(TaskType.valueOf(proto.getType().name()))")
    // NEW:
    @Mapping(target = "addressId",            source = "addressId", qualifiedByName = "positiveAddressId")
    @Mapping(target = "addressText",          source = "addressText")
    ProjectTaskDTO toProjectTaskDTO(TaskProto proto);

    default TaskDTO toTaskDTO(TaskProto proto) {
        return switch (proto.getType()) {
            case FIXED    -> toFixedTaskDTO(proto);
            case FLEXIBLE -> toFlexibleTaskDTO(proto);
            case PROJECT  -> toProjectTaskDTO(proto);
            default       -> throw new IllegalArgumentException("Unknown type: " + proto.getType());
        };
    }


    // --- Enum conversions with defaults -----------------------------------

    @ValueMappings({
            @ValueMapping(source = "UNRECOGNIZED", target = "FIXED")
    })
    TaskType toTaskType(com.scheduler.taskmanagement.grpc.TaskType grpcType);

    @ValueMappings({
            @ValueMapping(source = "TASK_STATUS_UNSPECIFIED", target = "PENDING"),
            @ValueMapping(source = "UNRECOGNIZED",            target = "PENDING")
    })
    TaskStatus toTaskStatus(com.scheduler.taskmanagement.grpc.TaskStatus grpcStatus);

    @ValueMappings({
            @ValueMapping(source = "UNRECOGNIZED", target = "FIXED_ESTIMATE")
    })
    TaskNature toTaskNature(com.scheduler.taskmanagement.grpc.TaskNature grpcNature);

    @Named("positiveAddressId")
    default Long positiveAddressId(long addressId) {
        return addressId > 0 ? addressId : null;
    }
}
