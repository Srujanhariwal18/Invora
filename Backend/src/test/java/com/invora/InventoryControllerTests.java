package com.invora;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.security.test.context.support.WithMockUser(username = "admin_demo", roles = {"ADMIN"})
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private static final UUID SEED_INVENTORY_ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_PRODUCT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("GET /api/inventory - Lists all inventory balances with HTTP 200")
    void testGetAllInventory_Returns200() throws Exception {
        mockMvc.perform(get("/api/inventory")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].productSku").exists())
                .andExpect(jsonPath("$[0].rackCode").exists())
                .andExpect(jsonPath("$[0].warehouseCode").exists());
    }

    @Test
    @DisplayName("GET /api/inventory/{id} - Existing inventory returns HTTP 200")
    void testGetInventoryById_Success() throws Exception {
        mockMvc.perform(get("/api/inventory/{id}", SEED_INVENTORY_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_INVENTORY_ID.toString()))
                .andExpect(jsonPath("$.productId").value(SEED_PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.productSku").value("SKU-BOLT-M10"))
                .andExpect(jsonPath("$.rackCode").value("RA1"))
                .andExpect(jsonPath("$.quantity").value(150));
    }

    @Test
    @DisplayName("GET /api/inventory/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetInventoryById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/inventory/product/{productId} - Returns inventory for product across racks")
    void testGetInventoryByProductId_Success() throws Exception {
        mockMvc.perform(get("/api/inventory/product/{productId}", SEED_PRODUCT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].productSku").value("SKU-BOLT-M10"));
    }

    @Test
    @DisplayName("GET /api/inventory/product/{productId} - Non-existent product returns HTTP 404 Not Found")
    void testGetInventoryByProductId_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/product/{productId}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/inventory/rack/{rackId} - Returns inventory items in rack")
    void testGetInventoryByRackId_Success() throws Exception {
        mockMvc.perform(get("/api/inventory/rack/{rackId}", SEED_RACK_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].rackCode").value("RA1"));
    }

    @Test
    @DisplayName("GET /api/inventory/rack/{rackId} - Non-existent rack returns HTTP 404 Not Found")
    void testGetInventoryByRackId_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/inventory/rack/{rackId}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
