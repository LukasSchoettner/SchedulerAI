//package com.scheduler.scheduling.config;
//
//import com.scheduler.customermanagement.grpc.base.ZoneServiceGrpc;
//import com.scheduler.routing.grpc.RoutingServiceGrpc;
//import com.scheduler.taskmanagement.grpc.TaskServiceGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class GrpcClientConfig {
//
//    @Value("${grpc.task-service-address:task-service:9090}")
//    private String taskServiceTarget;
//
//    @Value("${grpc.zone-service-address:customer-service:9090}")
//    private String zoneServiceTarget;
//
//    @Value("${grpc.routing-service-address:routing-service:9090}")
//    private String routingServiceTarget;
//
//    // ---- TaskService ----
//    @Bean(destroyMethod = "shutdownNow")
//    public ManagedChannel taskServiceChannel() {
//        return ManagedChannelBuilder
//                .forTarget(taskServiceTarget)   // "task-service:9090" inside Docker network
//                .usePlaintext()
//                .build();
//    }
//
//    @Bean
//    public TaskServiceGrpc.TaskServiceBlockingStub taskServiceStub(ManagedChannel taskServiceChannel) {
//        return TaskServiceGrpc.newBlockingStub(taskServiceChannel);
//    }
//
//    // ---- ZoneService (Customer-Service) ----
//    @Bean(destroyMethod = "shutdownNow")
//    public ManagedChannel zoneServiceChannel() {
//        return ManagedChannelBuilder
//                .forTarget(zoneServiceTarget)   // "customer-service:9090"
//                .usePlaintext()
//                .build();
//    }
//
//    @Bean
//    public ZoneServiceGrpc.ZoneServiceBlockingStub zoneServiceStub(ManagedChannel zoneServiceChannel) {
//        return ZoneServiceGrpc.newBlockingStub(zoneServiceChannel);
//    }
//
//    // ---- RoutingService ----
//    @Bean(destroyMethod = "shutdownNow")
//    public ManagedChannel routingServiceChannel() {
//        return ManagedChannelBuilder
//                .forTarget(routingServiceTarget)   // "routing-service:9090"
//                .usePlaintext()
//                .build();
//    }
//
//    @Bean
//    public RoutingServiceGrpc.RoutingServiceBlockingStub routingServiceStub(
//            ManagedChannel routingServiceChannel
//    ) {
//        return RoutingServiceGrpc.newBlockingStub(routingServiceChannel);
//    }
//}
