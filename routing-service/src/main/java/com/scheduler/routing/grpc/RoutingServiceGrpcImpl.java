// routing-service/src/main/java/com/scheduler/routing/grpc/RoutingServiceGrpcImpl.java
package com.scheduler.routing.grpc;

import com.scheduler.routing.models.Address;
import com.scheduler.routing.repositories.AddressRepository;
import com.scheduler.routing.services.RoutingService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@GrpcService
public class RoutingServiceGrpcImpl extends RoutingServiceGrpc.RoutingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(RoutingServiceGrpcImpl.class);

    private final AddressRepository addressRepository;
    private final RoutingService routingService;

    public RoutingServiceGrpcImpl(AddressRepository addressRepository,
                                  RoutingService routingService) {
        this.addressRepository = addressRepository;
        this.routingService = routingService;
    }

    @Override
    public void getDistanceMatrix(DistanceMatrixRequest request,
                                  StreamObserver<DistanceMatrixProto> responseObserver) {

        long customerId = request.getCustomerId();
        log.info("GetDistanceMatrix called for customerId={}", customerId);

        // 1) Load addresses for customer
        List<Address> addresses = addressRepository.findByCustomerId(customerId);

        // 2) Build in-memory distance matrix (your level-0 implementation)
        var dm = routingService.buildDistanceMatrix(addresses);

        // 3) Map to proto
        DistanceMatrixProto.Builder protoBuilder = DistanceMatrixProto.newBuilder();

        // addresses
        for (Address addr : dm.getAddresses()) {
            protoBuilder.addAddresses(
                    AddressProto.newBuilder()
                            .setId(addr.getId())
                            .setAddressLine(addr.getAddressLine())
                            .setLatitude(addr.getLatitude() != null ? addr.getLatitude() : 0.0)
                            .setLongitude(addr.getLongitude() != null ? addr.getLongitude() : 0.0)
                            .setCustomerId(addr.getCustomerId() != null ? addr.getCustomerId() : 0L)
                            .build()
            );
        }

        // rows (matrix)
        double[][] matrix = dm.getDistances();
        for (double[] row : matrix) {
            DistanceMatrixProto.Row.Builder rowBuilder = DistanceMatrixProto.Row.newBuilder();
            for (double value : row) {
                rowBuilder.addValue(value);
            }
            protoBuilder.addRows(rowBuilder.build());
        }

        DistanceMatrixProto response = protoBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
