package com.scheduler.routing.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The full address string as entered by the customer
    @Column(nullable = false)
    private String addressLine;

    // Geocoded coordinates
    private Double latitude;
    private Double longitude;

    // If you link to a customer (Plus: up to 5, Premium: unlimited)
    private Long customerId;

    public Address(String addressLine) {
        this.addressLine = addressLine;
    }
}

