package com.scheduler.scheduling.services;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.mappers.TaskDtoMapper;
import com.scheduler.taskmanagement.grpc.TaskProto;
import com.scheduler.taskmanagement.grpc.TaskStatus;
import com.scheduler.taskmanagement.grpc.TaskType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class TaskDtoLocationMappingTest {

    private final TaskDtoMapper mapper = Mappers.getMapper(TaskDtoMapper.class);

    @Test
    void flexibleTaskProtoPreservesPositiveAddressIdAndText() {
        TaskProto proto = TaskProto.newBuilder()
                .setId(44L)
                .setTitle("Gym")
                .setType(TaskType.FLEXIBLE)
                .setStatus(TaskStatus.PENDING)
                .setPriority(3)
                .setAddressId(12L)
                .setAddressText("Gym address")
                .build();

        FlexibleTaskDTO dto = (FlexibleTaskDTO) mapper.toTaskDTO(proto);

        assertThat(dto.getAddressId()).isEqualTo(12L);
        assertThat(dto.getAddressText()).isEqualTo("Gym address");
    }

    @Test
    void fixedTaskProtoPreservesPositiveAddressIdAndText() {
        TaskProto proto = TaskProto.newBuilder()
                .setId(45L)
                .setTitle("Doctor")
                .setType(TaskType.FIXED)
                .setStatus(TaskStatus.PENDING)
                .setPriority(4)
                .setAddressId(34L)
                .setAddressText("Doctor address")
                .build();

        FixedTaskDTO dto = (FixedTaskDTO) mapper.toTaskDTO(proto);

        assertThat(dto.getAddressId()).isEqualTo(34L);
        assertThat(dto.getAddressText()).isEqualTo("Doctor address");
    }

    @Test
    void protoDefaultZeroAddressIdBecomesNullForScheduling() {
        TaskProto proto = TaskProto.newBuilder()
                .setId(46L)
                .setTitle("Reading")
                .setType(TaskType.FLEXIBLE)
                .setStatus(TaskStatus.PENDING)
                .setPriority(2)
                .build();

        FlexibleTaskDTO dto = (FlexibleTaskDTO) mapper.toTaskDTO(proto);

        assertThat(dto.getAddressId()).isNull();
        assertThat(dto.getAddressText()).isBlank();
    }
}
