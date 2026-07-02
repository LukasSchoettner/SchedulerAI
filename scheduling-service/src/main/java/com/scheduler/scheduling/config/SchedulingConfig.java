package com.scheduler.scheduling.config;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import com.scheduler.scheduling.scheduler.FixedTaskScheduler;
import com.scheduler.scheduling.scheduler.GenericFlexibleTaskScheduler;
import com.scheduler.scheduling.scheduler.ProjectTaskScheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SchedulingConfig {

    @Bean
    public SchedulingStrategy<FixedTaskDTO> fixedScheduler() {
        return new FixedTaskScheduler();
    }

    @Bean
    public SchedulingStrategy<FlexibleTaskDTO> flexibleScheduler(
            GenericFlexibleTaskScheduler gen
    ) {
        return gen;
    }

    @Bean
    public SchedulingStrategy<ProjectTaskDTO> projectScheduler() {
        return new ProjectTaskScheduler();
    }

    @Bean
    public Map<Class<?>, SchedulingStrategy<?>> strategyMap(
            @Qualifier("fixedScheduler") SchedulingStrategy<FixedTaskDTO> fixed,
            @Qualifier("flexibleScheduler") SchedulingStrategy<FlexibleTaskDTO> flex,
            @Qualifier("projectScheduler") SchedulingStrategy<ProjectTaskDTO> proj
    ) {
        return Map.of(
                FixedTaskDTO.class,    fixed,
                FlexibleTaskDTO.class, flex,
                ProjectTaskDTO.class,  proj
        );
    }
}

