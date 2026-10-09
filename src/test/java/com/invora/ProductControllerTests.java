package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.ProductRequest;
import com.invora.entity.Product;
import com.invora.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private static final UUID SEED_CATEGORY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_SUPPLIER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_PRODUCT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/products - Success creates product with HTTP 201")
    void testCreateProduct_Success() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-TEST-001",
                "High Strength Steel Rod",
                SEED_CATEGORY_ID,
                SEED_SUPPLIER_ID,
                "PIECE",
                new BigDecimal("49.99"),
                10,
                25,
                "Carbon steel rod 12mm"
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("SKU-TEST-001"))
                .andExpect(jsonPath("$.name").value("High Strength Steel Rod"))
                .andExpect(jsonPath("$.categoryId").value(SEED_CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.categoryName").value("Raw Materials"))
                .andExpect(jsonPath("$.supplierId").value(SEED_SUPPLIER_ID.toString()))
                .andExpect(jsonPath("$.supplierName").value("Titan Hardware & Logistics"))
                .andExpect(jsonPath("$.unitPrice").value(49.99))
                .andExpect(jsonPath("$.threshold").value(10))
                .andExpect(jsonPath("$.reorderQuantity").value(25))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("POST /api/products - Duplicate SKU returns HTTP 409 Conflict")
    void testCreateProduct_DuplicateSku_Returns409() throws Exception {
        // SKU-BOLT-M10 is in seed data
        ProductRequest request = new ProductRequest(
                "SKU-BOLT-M10",
                "Duplicate Bolt",
                SEED_CATEGORY_ID,
                null,
                "PIECE",
                new BigDecimal("10.00"),
                5,
                10,
                "Duplicate item"
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/products - Non-existent category ID returns HTTP 404 Not Found")
    void testCreateProduct_NonExistentCategory_Returns404() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-NEW-002",
                "Product with Bad Category",
                NON_EXISTENT_ID,
                null,
                "PIECE",
                new BigDecimal("15.00"),
                5,
                10,
                "Invalid category"
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/products - Non-existent supplier ID returns HTTP 404 Not Found")
    void testCreateProduct_NonExistentSupplier_Returns404() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-NEW-003",
                "Product with Bad Supplier",
                SEED_CATEGORY_ID,
                NON_EXISTENT_ID,
                "PIECE",
                new BigDecimal("15.00"),
                5,
                10,
                "Invalid supplier"
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/products - Negative price returns HTTP 400 Bad Request")
    void testCreateProduct_NegativePrice_Returns400() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-NEW-004",
                "Negative Price Item",
                SEED_CATEGORY_ID,
                null,
                "PIECE",
                new BigDecimal("-5.00"),
                5,
                10,
                "Negative price"
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.unitPrice").exists());
    }

    @Test
    @DisplayName("POST /api/products - Negative threshold and reorder quantity return HTTP 400")
    void testCreateProduct_NegativeThreshold_Returns400() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-NEW-005",
                "Negative Threshold Item",
                SEED_CATEGORY_ID,
                null,
                "PIECE",
                new BigDecimal("10.00"),
                -1,
                -5,
                "Negative values"
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.threshold").exists())
                .andExpect(jsonPath("$.validationErrors.reorderQuantity").exists());
    }

    @Test
    @DisplayName("POST /api/products - Missing required fields returns HTTP 400")
    void testCreateProduct_MissingFields_Returns400() throws Exception {
        ProductRequest request = new ProductRequest(
                "",
                "   ",
                null,
                null,
                null,
                null,
                0,
                0,
                null
        );

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists())
                .andExpect(jsonPath("$.validationErrors.unitPrice").exists());
    }

    @Test
    @DisplayName("GET /api/products - Lists all products with HTTP 200")
    void testGetAllProducts_Returns200() throws Exception {
        mockMvc.perform(get("/api/products")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(8)))
                .andExpect(jsonPath("$[*].sku").value(hasItem("SKU-BOLT-M10")));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Existing product returns HTTP 200")
    void testGetProductById_Success() throws Exception {
        mockMvc.perform(get("/api/products/{id}", SEED_PRODUCT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-BOLT-M10"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetProductById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/products/sku/{sku} - Existing SKU returns HTTP 200")
    void testGetProductBySku_Success() throws Exception {
        mockMvc.perform(get("/api/products/sku/{sku}", "SKU-BOLT-M10")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-BOLT-M10"))
                .andExpect(jsonPath("$.name").value(org.hamcrest.Matchers.containsString("Bolt")));
    }

    @Test
    @DisplayName("GET /api/products/sku/{sku} - Non-existent SKU returns HTTP 404 Not Found")
    void testGetProductBySku_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/products/sku/{sku}", "NON-EXISTENT-SKU")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/products/search?name={name} - Returns matching products")
    void testSearchProductsByName_Success() throws Exception {
        mockMvc.perform(get("/api/products/search")
                .param("name", "valve")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-VALVE-BALL02"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Updates product details with HTTP 200")
    void testUpdateProduct_Success() throws Exception {
        ProductRequest updateRequest = new ProductRequest(
                "SKU-BOLT-M10", // keeping original SKU
                "Updated Industrial Hex Bolt M10 (Pack of 100)",
                SEED_CATEGORY_ID,
                SEED_SUPPLIER_ID,
                "BOX",
                new BigDecimal("28.90"),
                30,
                60,
                "Updated bolt packaging"
        );

        mockMvc.perform(put("/api/products/{id}", SEED_PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Updated Industrial Hex Bolt M10 (Pack of 100)"))
                .andExpect(jsonPath("$.unitPrice").value(28.90))
                .andExpect(jsonPath("$.threshold").value(30))
                .andExpect(jsonPath("$.reorderQuantity").value(60));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Updating to duplicate SKU of another product returns HTTP 409 Conflict")
    void testUpdateProduct_DuplicateSku_Returns409() throws Exception {
        // Attempting to change SEED_PRODUCT_ID's SKU to "SKU-WIRE-CU50" which belongs to product 2
        ProductRequest conflictRequest = new ProductRequest(
                "SKU-WIRE-CU50",
                "Conflict Bolt",
                SEED_CATEGORY_ID,
                null,
                "PIECE",
                new BigDecimal("20.00"),
                10,
                20,
                "Conflict attempt"
        );

        mockMvc.perform(put("/api/products/{id}", SEED_PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conflictRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Non-existent product ID returns HTTP 404 Not Found")
    void testUpdateProduct_NotFound_Returns404() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-VALID-NEW",
                "Valid Product",
                SEED_CATEGORY_ID,
                null,
                "PIECE",
                new BigDecimal("10.00"),
                5,
                10,
                "Description"
        );

        mockMvc.perform(put("/api/products/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/deactivate - Deactivates product without physical deletion")
    void testDeactivateProduct_Success() throws Exception {
        mockMvc.perform(patch("/api/products/{id}/deactivate", SEED_PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.active").value(false));

        // Verify product still exists in DB
        Product product = productRepository.findById(SEED_PRODUCT_ID).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/deactivate - Non-existent ID returns HTTP 404 Not Found")
    void testDeactivateProduct_NotFound_Returns404() throws Exception {
        mockMvc.perform(patch("/api/products/{id}/deactivate", NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
