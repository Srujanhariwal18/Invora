package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.WarehouseRequest;
import com.invora.entity.Warehouse;
import com.invora.repository.WarehouseRepository;
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
class WarehouseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private static final UUID SEED_WAREHOUSE_ID = UUID.fromString("e0000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/warehouses - Success creates warehouse with HTTP 201")
    void testCreateWarehouse_Success() throws Exception {
        WarehouseRequest request = new WarehouseRequest(
                "East Coast Logistics Hub",
                "WH-EAST",
                "100 Port Road, Elizabeth, NJ",
                25000
        );

        mockMvc.perform(post("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("East Coast Logistics Hub"))
                .andExpect(jsonPath("$.code").value("WH-EAST"))
                .andExpect(jsonPath("$.address").value("100 Port Road, Elizabeth, NJ"))
                .andExpect(jsonPath("$.capacity").value(25000))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/warehouses - Duplicate warehouse code returns HTTP 409 Conflict")
    void testCreateWarehouse_DuplicateCode_Returns409() throws Exception {
        // WH-MAIN is in seed data
        WarehouseRequest request = new WarehouseRequest(
                "Duplicate Main Hub",
                "WH-MAIN",
                "Some Address",
                10000
        );

        mockMvc.perform(post("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/warehouses - Blank name or code returns HTTP 400 Bad Request")
    void testCreateWarehouse_BlankFields_Returns400() throws Exception {
        WarehouseRequest request = new WarehouseRequest(
                "   ",
                "",
                "Valid Address",
                5000
        );

        mockMvc.perform(post("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    @DisplayName("POST /api/warehouses - Negative capacity returns HTTP 400 Bad Request")
    void testCreateWarehouse_NegativeCapacity_Returns400() throws Exception {
        WarehouseRequest request = new WarehouseRequest(
                "Negative Capacity Warehouse",
                "WH-NEG",
                "Address",
                -50
        );

        mockMvc.perform(post("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.capacity").exists());
    }

    @Test
    @DisplayName("GET /api/warehouses - Lists all warehouses with HTTP 200")
    void testGetAllWarehouses_Returns200() throws Exception {
        mockMvc.perform(get("/api/warehouses")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[*].code").value(hasItem("WH-MAIN")));
    }

    @Test
    @DisplayName("GET /api/warehouses/{id} - Existing warehouse returns HTTP 200")
    void testGetWarehouseById_Success() throws Exception {
        mockMvc.perform(get("/api/warehouses/{id}", SEED_WAREHOUSE_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_WAREHOUSE_ID.toString()))
                .andExpect(jsonPath("$.code").value("WH-MAIN"));
    }

    @Test
    @DisplayName("GET /api/warehouses/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetWarehouseById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/warehouses/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/zones - Returns zones belonging to warehouse")
    void testGetZonesByWarehouseId_Success() throws Exception {
        mockMvc.perform(get("/api/warehouses/{warehouseId}/zones", SEED_WAREHOUSE_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].code").value(hasItem("ZA")));
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/zones - Non-existent warehouse returns HTTP 404")
    void testGetZonesByWarehouseId_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/warehouses/{warehouseId}/zones", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/warehouses/{id} - Updates warehouse with HTTP 200")
    void testUpdateWarehouse_Success() throws Exception {
        WarehouseRequest updateRequest = new WarehouseRequest(
                "Main Central Distribution Center - Renamed",
                "WH-MAIN",
                "500 Logistics Way, Bay 12, Level 2",
                20000
        );

        mockMvc.perform(put("/api/warehouses/{id}", SEED_WAREHOUSE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_WAREHOUSE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Main Central Distribution Center - Renamed"))
                .andExpect(jsonPath("$.capacity").value(20000));
    }

    @Test
    @DisplayName("PUT /api/warehouses/{id} - Duplicate code on update returns HTTP 409 Conflict")
    void testUpdateWarehouse_DuplicateCode_Returns409() throws Exception {
        Warehouse secondary = warehouseRepository.save(new Warehouse("Secondary Hub", "WH-SEC", "Address", 5000));

        // Attempting to rename secondary to "WH-MAIN"
        WarehouseRequest conflictRequest = new WarehouseRequest(
                "Conflict Warehouse",
                "WH-MAIN",
                "Address",
                5000
        );

        mockMvc.perform(put("/api/warehouses/{id}", secondary.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conflictRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/warehouses/{id} - Non-existent warehouse returns HTTP 404 Not Found")
    void testUpdateWarehouse_NotFound_Returns404() throws Exception {
        WarehouseRequest request = new WarehouseRequest("Valid Name", "WH-NEW-99", "Address", 1000);

        mockMvc.perform(put("/api/warehouses/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/warehouses/{id} - Warehouse with existing zones returns HTTP 409 Conflict")
    void testDeleteWarehouse_WithExistingZones_Returns409() throws Exception {
        // SEED_WAREHOUSE_ID contains zones ZA and ZB in seed data
        mockMvc.perform(delete("/api/warehouses/{id}", SEED_WAREHOUSE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("contains existing zones")));
    }

    @Test
    @DisplayName("DELETE /api/warehouses/{id} - Unreferenced empty warehouse returns HTTP 204 No Content")
    void testDeleteWarehouse_Empty_Returns204() throws Exception {
        Warehouse emptyWarehouse = warehouseRepository.save(new Warehouse("Empty Depo", "WH-EMPTY", "Nowhere", 0));

        mockMvc.perform(delete("/api/warehouses/{id}", emptyWarehouse.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/warehouses/{id}", emptyWarehouse.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/warehouses/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testDeleteWarehouse_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/warehouses/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
