package com.scheduler.taskmanagement.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfiguration {
    @Bean
    public Config hazelCastConfig() {
        return new Config()
                .setInstanceName("hazelcast-instance")
                .addMapConfig(new MapConfig()
                        .setName("tasks")
                        .setTimeToLiveSeconds(3600));
    }
}
