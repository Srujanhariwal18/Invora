package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.CategoryRequest;
import com.invora.entity.Category;
import com.invora.repository.CategoryRepository;
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
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin_demo", roles = {"ADMIN"})
class CategoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final UUID SEED_RAW_MATERIALS_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/categories - Success creates category with HTTP 201")
    void testCreateCategory_Success() throws Exception {
        CategoryRequest request = new CategoryRequest("Electronics", "Electronic parts and sensors");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic parts and sensors"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/categories - Duplicate category name returns HTTP 409 Conflict")
    void testCreateCategory_DuplicateName_Returns409() throws Exception {
        CategoryRequest request = new CategoryRequest("Raw Materials", "Duplicate raw materials");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/categories - Blank name returns HTTP 400 Bad Request")
    void testCreateCategory_BlankName_Returns400() throws Exception {
        CategoryRequest request = new CategoryRequest("   ", "Description only");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("GET /api/categories - Lists all categories with HTTP 200")
    void testGetAllCategories_Returns200() throws Exception {
        mockMvc.perform(get("/api/categories")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[*].name").value(hasItem("Raw Materials")));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Existing category returns HTTP 200")
    void testGetCategoryById_Success() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", SEED_RAW_MATERIALS_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_RAW_MATERIALS_ID.toString()))
                .andExpect(jsonPath("$.name").value("Raw Materials"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetCategoryById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - Updates category with HTTP 200")
    void testUpdateCategory_Success() throws Exception {
        Category newCat = categoryRepository.save(new Category("Temporary Category", "To be updated"));

        CategoryRequest updateRequest = new CategoryRequest("Updated Category Name", "Updated description");

        mockMvc.perform(put("/api/categories/{id}", newCat.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newCat.getId().toString()))
                .andExpect(jsonPath("$.name").value("Updated Category Name"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - Duplicate name on update returns HTTP 409 Conflict")
    void testUpdateCategory_DuplicateName_Returns409() throws Exception {
        Category newCat = categoryRepository.save(new Category("Distinct Cat", "Distinct description"));

        // Attempt to rename to an existing seed category name "Raw Materials"
        CategoryRequest duplicateRequest = new CategoryRequest("Raw Materials", "Conflict name");

        mockMvc.perform(put("/api/categories/{id}", newCat.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testUpdateCategory_NotFound_Returns404() throws Exception {
        CategoryRequest request = new CategoryRequest("Valid Name", "Valid description");

        mockMvc.perform(put("/api/categories/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Safe empty category deletion returns HTTP 204 No Content")
    void testDeleteCategory_Safe_Returns204() throws Exception {
        Category safeCat = categoryRepository.save(new Category("Empty Unused Category", "No products assigned"));

        mockMvc.perform(delete("/api/categories/{id}", safeCat.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{id}", safeCat.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Category referenced by products returns HTTP 409 Conflict")
    void testDeleteCategory_ReferencedByProduct_Returns409() throws Exception {
        // SEED_RAW_MATERIALS_ID is referenced by several seed products (e.g., SKU-BOLT-M10)
        mockMvc.perform(delete("/api/categories/{id}", SEED_RAW_MATERIALS_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("referenced by existing products")));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testDeleteCategory_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
