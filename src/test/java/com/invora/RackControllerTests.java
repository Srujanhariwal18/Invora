package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.RackRequest;
import com.invora.entity.Rack;
import com.invora.entity.Zone;
import com.invora.repository.RackRepository;
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
@org.springframework.security.test.context.support.WithMockUser(username = "admin_demo", roles = {"ADMIN"})
class RackControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private RackRepository rackRepository;

    private static final UUID SEED_ZONE_A_ID = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/racks - Success creates rack with HTTP 201")
    void testCreateRack_Success() throws Exception {
        RackRequest request = new RackRequest(
                SEED_ZONE_A_ID,
                "Rack A3 - Heavy Pallets",
                "RA3",
                2500
        );

        mockMvc.perform(post("/api/racks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.zoneId").value(SEED_ZONE_A_ID.toString()))
                .andExpect(jsonPath("$.zoneCode").value("ZA"))
                .andExpect(jsonPath("$.name").value("Rack A3 - Heavy Pallets"))
                .andExpect(jsonPath("$.code").value("RA3"))
                .andExpect(jsonPath("$.capacity").value(2500))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/racks - Non-existent zone returns HTTP 404 Not Found")
    void testCreateRack_NonExistentZone_Returns404() throws Exception {
        RackRequest request = new RackRequest(
                NON_EXISTENT_ID,
                "Rack Invalid",
                "RX",
                1000
        );

        mockMvc.perform(post("/api/racks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/racks - Duplicate code within same zone returns HTTP 409 Conflict")
    void testCreateRack_DuplicateCodeInZone_Returns409() throws Exception {
        // RA1 already exists in SEED_ZONE_A_ID
        RackRequest request = new RackRequest(
                SEED_ZONE_A_ID,
                "Duplicate Rack",
                "RA1",
                1000
        );

        mockMvc.perform(post("/api/racks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/racks - Negative capacity returns HTTP 400 Bad Request")
    void testCreateRack_NegativeCapacity_Returns400() throws Exception {
        RackRequest request = new RackRequest(
                SEED_ZONE_A_ID,
                "Negative Capacity Rack",
                "RA_NEG",
                -100
        );

        mockMvc.perform(post("/api/racks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.capacity").exists());
    }

    @Test
    @DisplayName("POST /api/racks - Blank name or code returns HTTP 400 Bad Request")
    void testCreateRack_BlankFields_Returns400() throws Exception {
        RackRequest request = new RackRequest(
                SEED_ZONE_A_ID,
                "   ",
                "",
                500
        );

        mockMvc.perform(post("/api/racks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    @DisplayName("GET /api/racks - Lists all racks with HTTP 200")
    void testGetAllRacks_Returns200() throws Exception {
        mockMvc.perform(get("/api/racks")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$[*].code").value(hasItem("RA1")));
    }

    @Test
    @DisplayName("GET /api/racks/{id} - Existing rack returns HTTP 200")
    void testGetRackById_Success() throws Exception {
        mockMvc.perform(get("/api/racks/{id}", SEED_RACK_A1_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_RACK_A1_ID.toString()))
                .andExpect(jsonPath("$.code").value("RA1"))
                .andExpect(jsonPath("$.zoneCode").value("ZA"))
                .andExpect(jsonPath("$.warehouseCode").value("WH-MAIN"));
    }

    @Test
    @DisplayName("GET /api/racks/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetRackById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/racks/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/racks/{id} - Updates rack details with HTTP 200")
    void testUpdateRack_Success() throws Exception {
        RackRequest updateRequest = new RackRequest(
                SEED_ZONE_A_ID,
                "Rack A1 - Structural Heavy Components",
                "RA1",
                1600
        );

        mockMvc.perform(put("/api/racks/{id}", SEED_RACK_A1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_RACK_A1_ID.toString()))
                .andExpect(jsonPath("$.name").value("Rack A1 - Structural Heavy Components"))
                .andExpect(jsonPath("$.capacity").value(1600));
    }

    @Test
    @DisplayName("PUT /api/racks/{id} - Duplicate code in zone returns HTTP 409 Conflict")
    void testUpdateRack_DuplicateCodeInZone_Returns409() throws Exception {
        Zone zone = zoneRepository.findById(SEED_ZONE_A_ID).orElseThrow();
        Rack newRack = rackRepository.save(new Rack(zone, "Temporary Rack", "RA_TEMP", 500));

        // Attempting to rename to "RA1" which already exists in SEED_ZONE_A_ID
        RackRequest duplicateRequest = new RackRequest(
                SEED_ZONE_A_ID,
                "Conflict Rack",
                "RA1",
                500
        );

        mockMvc.perform(put("/api/racks/{id}", newRack.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/racks/{id} - Non-existent rack returns HTTP 404 Not Found")
    void testUpdateRack_NotFound_Returns404() throws Exception {
        RackRequest request = new RackRequest(SEED_ZONE_A_ID, "Valid Rack", "RX9", 100);

        mockMvc.perform(put("/api/racks/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/racks/{id} - Rack with active inventory returns HTTP 409 Conflict")
    void testDeleteRack_WithActiveInventory_Returns409() throws Exception {
        // SEED_RACK_A1_ID holds stock in seed inventory
        mockMvc.perform(delete("/api/racks/{id}", SEED_RACK_A1_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inventory")));
    }

    @Test
    @DisplayName("DELETE /api/racks/{id} - Safe unreferenced rack deletion returns HTTP 204 No Content")
    void testDeleteRack_Empty_Returns204() throws Exception {
        Zone zone = zoneRepository.findById(SEED_ZONE_A_ID).orElseThrow();
        Rack emptyRack = rackRepository.save(new Rack(zone, "Unused Rack", "R-EMPTY", 100));

        mockMvc.perform(delete("/api/racks/{id}", emptyRack.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/racks/{id}", emptyRack.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/racks/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testDeleteRack_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/racks/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
