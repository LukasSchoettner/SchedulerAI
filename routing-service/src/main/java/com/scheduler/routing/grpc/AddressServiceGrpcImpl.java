//// routing-service/src/main/java/com/scheduler/routing/grpc/AddressServiceGrpcImpl.java
//package com.scheduler.routing.grpc;
//
//import com.scheduler.routing.grpc.AddressServiceGrpc;
//import com.scheduler.routing.grpc.AddressesRequest;
//import com.scheduler.routing.grpc.AddressesResponse;
//import io.grpc.stub.StreamObserver;
//import net.devh.boot.grpc.server.service.GrpcService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@GrpcService
//public class AddressServiceGrpcImpl extends AddressServiceGrpc.AddressServiceImplBase {
//
//    private static final Logger log = LoggerFactory.getLogger(AddressServiceGrpcImpl.class);
//
//    @Override
//    public void listAddresses(AddressesRequest request,
//                              StreamObserver<AddressesResponse> responseObserver) {
//
//        log.info("Received listAddresses gRPC call for customerId={}", request.getCustomerId());
//
//        // TODO: query DB, map entities to proto, fill response
//        AddressesResponse response = AddressesResponse.newBuilder()
//                .build();
//
//        responseObserver.onNext(response);
//        responseObserver.onCompleted();
//    }
//}
