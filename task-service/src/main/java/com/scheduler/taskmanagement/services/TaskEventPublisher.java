package com.scheduler.taskmanagement.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.taskmanagement.models.Task;

@Service
public class TaskEventPublisher {

    private static final String TOPIC = "task-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void publishTaskEvent(Task task, String eventType) {
        try {
            // You can create an event object or just send the task JSON along with an event type
            String message = objectMapper.writeValueAsString(task);
            kafkaTemplate.send(TOPIC, eventType, message);
        } catch (JsonProcessingException e) {
            // Handle exception: log or rethrow
            e.printStackTrace();
        }
    }
}
