package com.scheduler.routing.controllers;

import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.commoncode.dto.AddressDTO;
import com.scheduler.routing.models.Address;
import com.scheduler.routing.repositories.AddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/routing/addresses")
public class AddressController {

    private final AddressRepository addressRepository;
    private final JwtUtil jwtUtil;

    public AddressController(AddressRepository addressRepository, JwtUtil jwtUtil) {
        this.addressRepository = addressRepository;
        this.jwtUtil = jwtUtil;
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractCustomerId(token);
    }

    private MembershipLevel extractMembershipLevel(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String membershipLevel = jwtUtil.extractMembershipLevel(token);
        if (membershipLevel == null) {
            return MembershipLevel.BASIC;
        }
        return MembershipLevel.valueOf(membershipLevel);
    }

    // GET /routing/addresses
    @GetMapping
    public ResponseEntity<List<AddressDTO>> listAddresses(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        List<AddressDTO> list = addressRepository.findByCustomerId(cid).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    // GET /routing/addresses/{id}  (optional but handy)
    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> getAddress(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        return addressRepository.findById(id)
                .filter(a -> cid.equals(a.getCustomerId()))
                .map(a -> ResponseEntity.ok(toDto(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /routing/addresses
    @PostMapping
    public ResponseEntity<AddressDTO> createAddress(
            @RequestBody AddressDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        enforceAddressLimit(cid, extractMembershipLevel(authHeader));

        Address entity = new Address();
        entity.setAddressLine(dto.getAddressLine());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setCustomerId(cid);

        Address saved = addressRepository.save(entity);
        return ResponseEntity.status(201).body(toDto(saved));
    }

    // DELETE /routing/addresses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        int deleted = addressRepository.deleteByIdAndCustomerId(id, cid);
        return deleted > 0
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private AddressDTO toDto(Address a) {
        return new AddressDTO(
                a.getId(),
                a.getAddressLine(),
                a.getLatitude(),
                a.getLongitude()
        );
    }

    private void enforceAddressLimit(Long customerId, MembershipLevel membershipLevel) {
        int maxAddresses = switch (membershipLevel) {
            case BASIC -> 1;
            case PLUS -> 5;
            case PREMIUM -> Integer.MAX_VALUE;
        };

        if (addressRepository.countByCustomerId(customerId) >= maxAddresses) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Address limit reached for " + membershipLevel + " membership"
            );
        }
    }
}
