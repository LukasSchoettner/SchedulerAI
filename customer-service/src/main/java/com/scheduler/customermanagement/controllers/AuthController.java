package com.scheduler.customermanagement.controllers;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.customermanagement.dto.AuthResponse;
import com.scheduler.customermanagement.dto.LoginRequest;
import com.scheduler.customermanagement.services.AuthService;
import com.scheduler.customermanagement.services.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    public AuthController(AuthService authService, CustomerService customerService) {
        this.authService = authService;
        this.customerService = customerService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        System.out.println(req);
        String token = authService.authenticate(req.getCustomername(), req.getPassword());
        if (token != null) {
            return ResponseEntity.ok(new AuthResponse(token));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody CustomerDTO dto) {
        CustomerDTO created = customerService.createCustomer(dto);
        String token = authService.authenticate(created.getCustomername(), dto.getPassword());

        if (token != null) {
            return ResponseEntity.ok(new AuthResponse(token));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

