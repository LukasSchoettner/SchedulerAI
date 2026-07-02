package com.scheduler.routing.services;

import com.scheduler.routing.dto.DistanceMatrix;
import com.scheduler.routing.models.Address;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoutingService {

    private static final double DEFAULT_SPEED_KMH = 40.0;

    public DistanceMatrix buildDistanceMatrix(List<Address> addresses) {
        int n = addresses.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            Address origin = addresses.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0.0;
                } else {
                    Address dest = addresses.get(j);
                    matrix[i][j] = estimateTravelTimeMinutes(origin, dest);
                }
            }
        }
        return new DistanceMatrix(addresses, matrix);
    }

    private double estimateTravelTimeMinutes(Address origin, Address dest) {
        if (origin.getLatitude() == null || origin.getLongitude() == null
                || dest.getLatitude() == null || dest.getLongitude() == null) {
            return Double.POSITIVE_INFINITY;
        }
        double distanceKm = haversineKm(
                origin.getLatitude(), origin.getLongitude(),
                dest.getLatitude(), dest.getLongitude()
        );
        return (distanceKm / DEFAULT_SPEED_KMH) * 60.0;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) *
                                Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLon / 2) *
                                Math.sin(dLon / 2);
        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
