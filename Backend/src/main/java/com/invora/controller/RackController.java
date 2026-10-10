package com.invora.controller;

import com.invora.dto.RackRequest;
import com.invora.dto.RackResponse;
import com.invora.service.RackService;
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
@RequestMapping("/api/racks")
public class RackController {

    private final RackService rackService;

    public RackController(RackService rackService) {
        this.rackService = rackService;
    }

    @PostMapping
    public ResponseEntity<RackResponse> createRack(@Valid @RequestBody RackRequest request) {
        RackResponse response = rackService.createRack(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RackResponse>> getAllRacks() {
        return ResponseEntity.ok(rackService.getAllRacks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RackResponse> getRackById(@PathVariable UUID id) {
        return ResponseEntity.ok(rackService.getRackById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RackResponse> updateRack(
            @PathVariable UUID id,
            @Valid @RequestBody RackRequest request) {
        return ResponseEntity.ok(rackService.updateRack(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteRack(@PathVariable UUID id) {
        rackService.deleteRack(id);
        return ResponseEntity.noContent().build();
    }
}
