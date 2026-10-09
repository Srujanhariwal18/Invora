package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.SupplierRequest;
import com.invora.entity.Supplier;
import com.invora.repository.SupplierRepository;
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
class SupplierControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    private static final UUID SEED_TITAN_SUPPLIER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/suppliers - Success creates supplier with HTTP 201")
    void testCreateSupplier_Success() throws Exception {
        SupplierRequest request = new SupplierRequest(
                "Global Fasteners Ltd",
                "Alice Walker",
                "alice@globalfasteners.com",
                "+1-555-9876",
                "77 Enterprise Blvd, Zone 5"
        );

        mockMvc.perform(post("/api/suppliers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Global Fasteners Ltd"))
                .andExpect(jsonPath("$.contactPerson").value("Alice Walker"))
                .andExpect(jsonPath("$.email").value("alice@globalfasteners.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-9876"))
                .andExpect(jsonPath("$.address").value("77 Enterprise Blvd, Zone 5"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/suppliers - Success creates supplier with null optional fields")
    void testCreateSupplier_WithOptionalFieldsNull() throws Exception {
        SupplierRequest request = new SupplierRequest(
                "Minimalist Supplier",
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/suppliers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Minimalist Supplier"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/suppliers - Blank name returns HTTP 400 Bad Request")
    void testCreateSupplier_BlankName_Returns400() throws Exception {
        SupplierRequest request = new SupplierRequest(
                "   ",
                "Contact Person",
                "contact@example.com",
                "+1-555-0000",
                "Address"
        );

        mockMvc.perform(post("/api/suppliers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("POST /api/suppliers - Invalid email format returns HTTP 400 Bad Request")
    void testCreateSupplier_InvalidEmail_Returns400() throws Exception {
        SupplierRequest request = new SupplierRequest(
                "Acme Supplies",
                "John Doe",
                "not-a-valid-email-format",
                "+1-555-1111",
                "123 Main St"
        );

        mockMvc.perform(post("/api/suppliers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("GET /api/suppliers - Lists all suppliers with HTTP 200")
    void testGetAllSuppliers_Returns200() throws Exception {
        mockMvc.perform(get("/api/suppliers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].name").value(hasItem("Titan Hardware & Logistics")));
    }

    @Test
    @DisplayName("GET /api/suppliers/{id} - Existing supplier returns HTTP 200")
    void testGetSupplierById_Success() throws Exception {
        mockMvc.perform(get("/api/suppliers/{id}", SEED_TITAN_SUPPLIER_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_TITAN_SUPPLIER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Titan Hardware & Logistics"))
                .andExpect(jsonPath("$.contactPerson").value("Robert Vance"));
    }

    @Test
    @DisplayName("GET /api/suppliers/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetSupplierById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/suppliers/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/suppliers/{id} - Updates supplier details with HTTP 200")
    void testUpdateSupplier_Success() throws Exception {
        SupplierRequest updateRequest = new SupplierRequest(
                "Titan Hardware & Logistics International",
                "Robert Vance Jr.",
                "rvance@titanhw.com",
                "+1-555-0198",
                "100 Industrial Parkway, Sector 4, Suite 200"
        );

        mockMvc.perform(put("/api/suppliers/{id}", SEED_TITAN_SUPPLIER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_TITAN_SUPPLIER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Titan Hardware & Logistics International"))
                .andExpect(jsonPath("$.contactPerson").value("Robert Vance Jr."))
                .andExpect(jsonPath("$.email").value("rvance@titanhw.com"));
    }

    @Test
    @DisplayName("PUT /api/suppliers/{id} - Non-existent supplier returns HTTP 404 Not Found")
    void testUpdateSupplier_NotFound_Returns404() throws Exception {
        SupplierRequest request = new SupplierRequest(
                "Valid Supplier",
                "Contact",
                "valid@example.com",
                "+1-555-2222",
                "Address"
        );

        mockMvc.perform(put("/api/suppliers/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/suppliers/search?name={name} - Returns matching suppliers with HTTP 200")
    void testSearchSuppliersByName_Success() throws Exception {
        mockMvc.perform(get("/api/suppliers/search")
                .param("name", "titan")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].name").value("Titan Hardware & Logistics"));
    }

    @Test
    @DisplayName("DELETE /api/suppliers/{id} - Supplier referenced by products returns HTTP 409 Conflict")
    void testDeleteSupplier_ReferencedByProduct_Returns409() throws Exception {
        // SEED_TITAN_SUPPLIER_ID supplies SKU-BOLT-M10, SKU-FLANGE-04, etc.
        mockMvc.perform(delete("/api/suppliers/{id}", SEED_TITAN_SUPPLIER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("referenced by existing products")));
    }

    @Test
    @DisplayName("DELETE /api/suppliers/{id} - Safe unreferenced supplier deletion returns HTTP 204 No Content")
    void testDeleteSupplier_Unreferenced_Returns204() throws Exception {
        Supplier unreferenced = supplierRepository.save(
                new Supplier("Unreferenced Supplier Co", "Sam", "sam@unreferenced.local", null, null)
        );

        mockMvc.perform(delete("/api/suppliers/{id}", unreferenced.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/suppliers/{id}", unreferenced.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/suppliers/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testDeleteSupplier_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/suppliers/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
