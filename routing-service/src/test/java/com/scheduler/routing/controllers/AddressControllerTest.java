package com.scheduler.routing.controllers;

import com.scheduler.commoncode.dto.AddressDTO;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.routing.models.Address;
import com.scheduler.routing.repositories.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private static final String AUTH = "Bearer token";

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AddressController controller;

    @Test
    void createAddressAllowsPlusCustomerBelowLimit() {
        Address saved = new Address();
        saved.setId(7L);
        saved.setAddressLine("Main Street");
        saved.setLatitude(52.5);
        saved.setLongitude(13.4);
        saved.setCustomerId(42L);

        when(jwtUtil.extractCustomerId("token")).thenReturn(42L);
        when(jwtUtil.extractMembershipLevel("token")).thenReturn("PLUS");
        when(addressRepository.countByCustomerId(42L)).thenReturn(4L);
        when(addressRepository.save(org.mockito.ArgumentMatchers.any(Address.class))).thenReturn(saved);

        var response = controller.createAddress(
                new AddressDTO(null, "Main Street", 52.5, 13.4),
                AUTH
        );

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(7L);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(42L);
    }

    @Test
    void createAddressRejectsPlusCustomerAtLimit() {
        when(jwtUtil.extractCustomerId("token")).thenReturn(42L);
        when(jwtUtil.extractMembershipLevel("token")).thenReturn("PLUS");
        when(addressRepository.countByCustomerId(42L)).thenReturn(5L);

        assertThatThrownBy(() -> controller.createAddress(
                new AddressDTO(null, "Main Street", 52.5, 13.4),
                AUTH
        )).isInstanceOf(ResponseStatusException.class);

        verify(addressRepository, never()).save(org.mockito.ArgumentMatchers.any(Address.class));
    }

    @Test
    void createAddressTreatsMissingMembershipAsBasic() {
        when(jwtUtil.extractCustomerId("token")).thenReturn(42L);
        when(jwtUtil.extractMembershipLevel("token")).thenReturn(null);
        when(addressRepository.countByCustomerId(42L)).thenReturn(1L);

        assertThatThrownBy(() -> controller.createAddress(
                new AddressDTO(null, "Second Address", 52.5, 13.4),
                AUTH
        )).isInstanceOf(ResponseStatusException.class);
    }
}
