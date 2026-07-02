package com.scheduler.routing.services;

import com.scheduler.routing.models.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

//@Service
//public class GeocodingService {
//
//    @Value("${google.api.key}")
//    private String googleApiKey;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public void geocodeAddress(Address address) {
//        // E.g. call the Google Geocoding API:
//        // "https://maps.googleapis.com/maps/api/geocode/json?address=" + addressLine + "&key=" + googleApiKey
//        // For brevity, let's do a mock approach:
//        // In real code, parse the JSON, extract lat/lon, store in address.setLatitude(...), address.setLongitude(...).
//
//        // This is just a placeholder or pseudo-code:
//
//        String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
//                + address.getAddressLine().replace(" ", "+")
//                + "&key=" + googleApiKey;
//
//        // ... make the REST call ...
//        // parse response, set address lat/long
//
//        // For demonstration, let's just set some dummy coords:
//        address.setLatitude(40.7128);
//        address.setLongitude(-74.0060);
//    }
//}
