package com.scheduler.taskmanagement.mappers;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.taskmanagement.grpc.TaskCreate;
import com.scheduler.taskmanagement.grpc.TaskStatus;
import com.scheduler.taskmanagement.grpc.TaskType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperLocationTest {

    private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

    @Test
    void taskCreateDefaultZeroAddressIdBecomesNull() {
        TaskCreate create = TaskCreate.newBuilder()
                .setTitle("Quick task")
                .setType(TaskType.FLEXIBLE)
                .setStatus(TaskStatus.PENDING)
                .setPriority(3)
                .build();

        FlexibleTaskDTO dto = (FlexibleTaskDTO) mapper.toTaskDTO(create);

        assertThat(dto.getAddressId()).isNull();
    }

    @Test
    void taskCreatePositiveAddressIdIsPreserved() {
        TaskCreate create = TaskCreate.newBuilder()
                .setTitle("Gym")
                .setType(TaskType.FLEXIBLE)
                .setStatus(TaskStatus.PENDING)
                .setPriority(3)
                .setAddressId(99L)
                .setAddressText("Gym address")
                .build();

        FlexibleTaskDTO dto = (FlexibleTaskDTO) mapper.toTaskDTO(create);

        assertThat(dto.getAddressId()).isEqualTo(99L);
        assertThat(dto.getAddressText()).isEqualTo("Gym address");
    }
}
