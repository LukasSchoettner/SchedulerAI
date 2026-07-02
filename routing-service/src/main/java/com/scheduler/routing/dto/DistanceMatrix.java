package com.scheduler.routing.dto;

import com.scheduler.routing.models.Address;
import lombok.Data;

import java.util.List;

// Not a JPA entity, just a container
@Data
public class DistanceMatrix {
    private List<Address> addresses;  // The list of addresses in row/col order
    private double[][] distances;     // distances[i][j] = distance/time from addresses[i] to addresses[j]

    public DistanceMatrix(List<Address> addresses, double[][] distances) {
        this.addresses = addresses;
        this.distances = distances;
    }
}
