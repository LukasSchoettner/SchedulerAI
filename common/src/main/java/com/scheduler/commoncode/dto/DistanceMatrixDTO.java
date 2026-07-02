package com.scheduler.commoncode.dto;

import lombok.Data;

import java.util.List;

@Data
public class DistanceMatrixDTO {

    private List<AddressDTO> addresses;
    private double[][] distances;

    @Data
    public static class AddressDTO {
        private Long id;
        private String addressLine;
        private Double latitude;
        private Double longitude;
        private Long customerId;
    }
}

