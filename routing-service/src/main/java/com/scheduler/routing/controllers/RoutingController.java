package com.scheduler.routing.controllers;

import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.routing.dto.DistanceMatrix;
import com.scheduler.routing.models.Address;
import com.scheduler.routing.repositories.AddressRepository;
import com.scheduler.routing.services.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routing")
public class RoutingController {

    private static final Logger log = LoggerFactory.getLogger(RoutingController.class);

    private final AddressRepository addressRepository;
    private final RoutingService routingService;
    private final JwtUtil jwtUtil;

    public RoutingController(AddressRepository addressRepository,
                             RoutingService routingService,
                             JwtUtil jwtUtil) {
        this.addressRepository = addressRepository;
        this.routingService = routingService;
        this.jwtUtil = jwtUtil;
    }

    private Long extractCustomerId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header: {}", authHeader);
            return null;
        }
        try {
            String token = authHeader.substring("Bearer ".length());
            return jwtUtil.extractCustomerId(token);
        } catch (Exception e) {
            log.error("Failed to extract customer id from JWT", e);
            return null;
        }
    }

    @GetMapping("/distance-matrix")
    public ResponseEntity<DistanceMatrix> getDistanceMatrix(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        if (cid == null) {
            return ResponseEntity.status(401).build();
        }

        List<Address> addresses = addressRepository.findByCustomerId(cid);
        if (addresses.isEmpty()) {
            // returns empty matrix, frontend can handle it
            return ResponseEntity.ok(new DistanceMatrix(List.of(), new double[0][0]));
        }

        DistanceMatrix matrix = routingService.buildDistanceMatrix(addresses);
        return ResponseEntity.ok(matrix);
    }
}
