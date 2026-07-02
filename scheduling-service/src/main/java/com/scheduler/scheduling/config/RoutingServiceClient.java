//package com.scheduler.scheduling.config;
//
//import com.scheduler.routing.grpc.DistanceMatrixProto;
//import com.scheduler.routing.grpc.DistanceMatrixRequest;
//import com.scheduler.routing.grpc.RoutingServiceGrpc;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RoutingServiceClient {
//
//    private final RoutingServiceGrpc.RoutingServiceBlockingStub stub;
//
//    public RoutingServiceClient(RoutingServiceGrpc.RoutingServiceBlockingStub stub) {
//        this.stub = stub;
//    }
//
//    public DistanceMatrixProto getDistanceMatrix(long customerId) {
//        return stub.getDistanceMatrix(
//                DistanceMatrixRequest.newBuilder()
//                        .setCustomerId(customerId)
//                        .build()
//        );
//    }
//}
