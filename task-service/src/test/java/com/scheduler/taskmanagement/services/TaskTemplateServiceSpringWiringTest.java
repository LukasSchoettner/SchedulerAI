package com.scheduler.taskmanagement.services;

import com.scheduler.taskmanagement.repositories.TaskTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TaskTemplateServiceSpringWiringTest {

    @Test
    void springCanConstructTaskTemplateServiceWithProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TaskTemplateRepository.class, () -> mock(TaskTemplateRepository.class));
            context.registerBean(TaskService.class, () -> mock(TaskService.class));
            context.registerBean(TaskTemplateService.class);

            context.refresh();

            assertThat(context.getBean(TaskTemplateService.class)).isNotNull();
        }
    }
}
