package com.invora.controller;

import com.invora.dto.RackResponse;
import com.invora.dto.ZoneRequest;
import com.invora.dto.ZoneResponse;
import com.invora.service.RackService;
import com.invora.service.ZoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;
    private final RackService rackService;

    public ZoneController(ZoneService zoneService, RackService rackService) {
        this.zoneService = zoneService;
        this.rackService = rackService;
    }

    @PostMapping
    public ResponseEntity<ZoneResponse> createZone(@Valid @RequestBody ZoneRequest request) {
        ZoneResponse response = zoneService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ZoneResponse>> getAllZones() {
        return ResponseEntity.ok(zoneService.getAllZones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoneResponse> getZoneById(@PathVariable UUID id) {
        return ResponseEntity.ok(zoneService.getZoneById(id));
    }

    @GetMapping("/{zoneId}/racks")
    public ResponseEntity<List<RackResponse>> getRacksByZoneId(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(rackService.getRacksByZoneId(zoneId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZoneResponse> updateZone(
            @PathVariable UUID id,
            @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.updateZone(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteZone(@PathVariable UUID id) {
        zoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }
}
