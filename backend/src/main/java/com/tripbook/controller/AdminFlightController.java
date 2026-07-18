package com.tripbook.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripbook.dto.FlightDetailResponse;
import com.tripbook.dto.FlightRequest;
import com.tripbook.service.FlightService;

import jakarta.validation.Valid;

/** Authorization is enforced at the URL level in SecurityConfig (/api/admin/** -> ROLE_ADMIN), not here. */
@RestController
@RequestMapping("/api/admin/flights")
public class AdminFlightController {

    private final FlightService flightService;

    public AdminFlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    public ResponseEntity<FlightDetailResponse> create(@Valid @RequestBody FlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(request));
    }

    @PutMapping("/{id}")
    public FlightDetailResponse update(@PathVariable Long id, @Valid @RequestBody FlightRequest request) {
        return flightService.updateFlight(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}
