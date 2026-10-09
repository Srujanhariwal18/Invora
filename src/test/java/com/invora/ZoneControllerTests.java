package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.ZoneRequest;
import com.invora.entity.Warehouse;
import com.invora.entity.Zone;
import com.invora.repository.WarehouseRepository;
import com.invora.repository.ZoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ZoneControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    private static final UUID SEED_WAREHOUSE_ID = UUID.fromString("e0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_ZONE_A_ID = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/zones - Success creates zone with HTTP 201")
    void testCreateZone_Success() throws Exception {
        ZoneRequest request = new ZoneRequest(
                SEED_WAREHOUSE_ID,
                "Zone C - Cold Storage",
                "ZC"
        );

        mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.warehouseId").value(SEED_WAREHOUSE_ID.toString()))
                .andExpect(jsonPath("$.warehouseCode").value("WH-MAIN"))
                .andExpect(jsonPath("$.name").value("Zone C - Cold Storage"))
                .andExpect(jsonPath("$.code").value("ZC"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/zones - Non-existent warehouse returns HTTP 404 Not Found")
    void testCreateZone_NonExistentWarehouse_Returns404() throws Exception {
        ZoneRequest request = new ZoneRequest(
                NON_EXISTENT_ID,
                "Zone Invalid",
                "ZX"
        );

        mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/zones - Duplicate code in same warehouse returns HTTP 409 Conflict")
    void testCreateZone_DuplicateCodeInWarehouse_Returns409() throws Exception {
        // ZA already exists in SEED_WAREHOUSE_ID
        ZoneRequest request = new ZoneRequest(
                SEED_WAREHOUSE_ID,
                "Duplicate Zone",
                "ZA"
        );

        mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/zones - Blank name or code returns HTTP 400 Bad Request")
    void testCreateZone_BlankFields_Returns400() throws Exception {
        ZoneRequest request = new ZoneRequest(
                SEED_WAREHOUSE_ID,
                "  ",
                ""
        );

        mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    @DisplayName("GET /api/zones - Lists all zones with HTTP 200")
    void testGetAllZones_Returns200() throws Exception {
        mockMvc.perform(get("/api/zones")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].code").value(hasItem("ZA")));
    }

    @Test
    @DisplayName("GET /api/zones/{id} - Existing zone returns HTTP 200")
    void testGetZoneById_Success() throws Exception {
        mockMvc.perform(get("/api/zones/{id}", SEED_ZONE_A_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_ZONE_A_ID.toString()))
                .andExpect(jsonPath("$.code").value("ZA"))
                .andExpect(jsonPath("$.warehouseCode").value("WH-MAIN"));
    }

    @Test
    @DisplayName("GET /api/zones/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetZoneById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/zones/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/zones/{zoneId}/racks - Returns racks belonging to zone with HTTP 200")
    void testGetRacksByZoneId_Success() throws Exception {
        mockMvc.perform(get("/api/zones/{zoneId}/racks", SEED_ZONE_A_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].code").value(hasItem("RA1")));
    }

    @Test
    @DisplayName("GET /api/zones/{zoneId}/racks - Non-existent zone returns HTTP 404 Not Found")
    void testGetRacksByZoneId_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/zones/{zoneId}/racks", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/zones/{id} - Updates zone details with HTTP 200")
    void testUpdateZone_Success() throws Exception {
        ZoneRequest updateRequest = new ZoneRequest(
                SEED_WAREHOUSE_ID,
                "Zone A - Upgraded Heavy Machinery",
                "ZA"
        );

        mockMvc.perform(put("/api/zones/{id}", SEED_ZONE_A_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_ZONE_A_ID.toString()))
                .andExpect(jsonPath("$.name").value("Zone A - Upgraded Heavy Machinery"));
    }

    @Test
    @DisplayName("PUT /api/zones/{id} - Duplicate code in warehouse returns HTTP 409 Conflict")
    void testUpdateZone_DuplicateCodeInWarehouse_Returns409() throws Exception {
        Warehouse wh = warehouseRepository.findById(SEED_WAREHOUSE_ID).orElseThrow();
        Zone newZone = zoneRepository.save(new Zone(wh, "Zone Delta", "ZD"));

        // Attempt to rename to "ZA" which already exists in SEED_WAREHOUSE_ID
        ZoneRequest duplicateRequest = new ZoneRequest(
                SEED_WAREHOUSE_ID,
                "Conflict Name",
                "ZA"
        );

        mockMvc.perform(put("/api/zones/{id}", newZone.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/zones/{id} - Non-existent zone returns HTTP 404 Not Found")
    void testUpdateZone_NotFound_Returns404() throws Exception {
        ZoneRequest request = new ZoneRequest(SEED_WAREHOUSE_ID, "Valid Zone", "ZX9");

        mockMvc.perform(put("/api/zones/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/zones/{id} - Zone with existing racks returns HTTP 409 Conflict")
    void testDeleteZone_WithExistingRacks_Returns409() throws Exception {
        // SEED_ZONE_A_ID has racks RA1, RA2 in seed data
        mockMvc.perform(delete("/api/zones/{id}", SEED_ZONE_A_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("contains existing racks")));
    }

    @Test
    @DisplayName("DELETE /api/zones/{id} - Safe empty zone deletion returns HTTP 204 No Content")
    void testDeleteZone_Empty_Returns204() throws Exception {
        Warehouse wh = warehouseRepository.findById(SEED_WAREHOUSE_ID).orElseThrow();
        Zone emptyZone = zoneRepository.save(new Zone(wh, "Empty Zone", "Z-EMPTY"));

        mockMvc.perform(delete("/api/zones/{id}", emptyZone.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/zones/{id}", emptyZone.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/zones/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testDeleteZone_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/zones/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
