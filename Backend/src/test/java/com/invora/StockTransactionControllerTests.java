package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.StockInRequest;
import com.invora.dto.StockOutRequest;
import com.invora.dto.StockTransferRequest;
import com.invora.entity.Inventory;
import com.invora.repository.InventoryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.security.test.context.support.WithMockUser(username = "admin_demo", roles = {"ADMIN"})
class StockTransactionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    private static final UUID SEED_PRODUCT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A2_ID = UUID.fromString("e2000000-0000-0000-0000-000000000002");
    private static final UUID SEED_TX_ID = UUID.fromString("f1000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("POST /api/stock/transactions/in - Increases existing inventory balance")
    void testStockIn_ExistingInventory_Success() throws Exception {
        StockInRequest request = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                50,
                "PO-2026-999",
                "Restocking bolts",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.transactionType").value("STOCK_IN"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.destinationRackCode").value("RA1"))
                .andExpect(jsonPath("$.balanceAfter").value(200));

        // Verify updated inventory in database
        Inventory inv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
        assertThat(inv.getQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("POST /api/stock/transactions/in - Creates new inventory record in empty rack")
    void testStockIn_EmptyRack_CreatesInventory_Success() throws Exception {
        // Product 1 initially has no stock in Rack A2
        assertThat(inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A2_ID)).isEmpty();

        StockInRequest request = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A2_ID,
                40,
                "PO-2026-100",
                "New location stocking",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("STOCK_IN"))
                .andExpect(jsonPath("$.quantity").value(40))
                .andExpect(jsonPath("$.destinationRackCode").value("RA2"))
                .andExpect(jsonPath("$.balanceAfter").value(40));

        Inventory inv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A2_ID).orElseThrow();
        assertThat(inv.getQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("POST /api/stock/transactions/in - Non-positive quantity returns HTTP 400 Bad Request")
    void testStockIn_NonPositiveQuantity_Returns400() throws Exception {
        StockInRequest zeroRequest = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                0,
                "REF-0",
                "Zero quantity",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zeroRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/stock/transactions/in - Non-existent product returns HTTP 404 Not Found")
    void testStockIn_NonExistentProduct_Returns404() throws Exception {
        StockInRequest request = new StockInRequest(
                NON_EXISTENT_ID,
                SEED_RACK_A1_ID,
                10,
                "REF",
                null,
                null
        );

        mockMvc.perform(post("/api/stock/transactions/in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/stock/transactions/in - Non-existent rack returns HTTP 404 Not Found")
    void testStockIn_NonExistentRack_Returns404() throws Exception {
        StockInRequest request = new StockInRequest(
                SEED_PRODUCT_ID,
                NON_EXISTENT_ID,
                10,
                "REF",
                null,
                null
        );

        mockMvc.perform(post("/api/stock/transactions/in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/stock/transactions/out - Issues stock successfully when sufficient")
    void testStockOut_SufficientStock_Success() throws Exception {
        // Seed inventory is 150
        StockOutRequest request = new StockOutRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                50,
                "SO-2026-001",
                "Customer order fulfillment",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("STOCK_OUT"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.sourceRackCode").value("RA1"))
                .andExpect(jsonPath("$.balanceAfter").value(100));

        Inventory inv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
        assertThat(inv.getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("POST /api/stock/transactions/out - Insufficient stock returns HTTP 409 Conflict")
    void testStockOut_InsufficientStock_Returns409() throws Exception {
        // Available is 150, request 500
        StockOutRequest request = new StockOutRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                500,
                "SO-OVERDRAFT",
                "Too much stock requested",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        // Stock must remain unchanged
        Inventory inv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
        assertThat(inv.getQuantity()).isEqualTo(150);
    }

    @Test
    @DisplayName("POST /api/stock/transactions/out - Stock out from empty rack returns HTTP 409 Conflict")
    void testStockOut_EmptyRack_Returns409() throws Exception {
        StockOutRequest request = new StockOutRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A2_ID,
                10,
                "SO-EMPTY",
                null,
                null
        );

        mockMvc.perform(post("/api/stock/transactions/out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/stock/transactions/transfer - Transfers stock between different racks")
    void testStockTransfer_Success() throws Exception {
        StockTransferRequest request = new StockTransferRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                SEED_RACK_A2_ID,
                30,
                "TR-2026-001",
                "Inter-rack relocation",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.quantity").value(30))
                .andExpect(jsonPath("$.sourceRackCode").value("RA1"))
                .andExpect(jsonPath("$.destinationRackCode").value("RA2"))
                .andExpect(jsonPath("$.balanceAfter").value(120));

        // Source decremented: 150 - 30 = 120
        Inventory srcInv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
        assertThat(srcInv.getQuantity()).isEqualTo(120);

        // Destination incremented: 0 + 30 = 30
        Inventory dstInv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A2_ID).orElseThrow();
        assertThat(dstInv.getQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("POST /api/stock/transactions/transfer - Identical source and destination returns HTTP 409")
    void testStockTransfer_SameRack_Returns409() throws Exception {
        StockTransferRequest request = new StockTransferRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                SEED_RACK_A1_ID, // Identical rack
                10,
                "TR-SAME",
                "Loopback transfer",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be the same")));
    }

    @Test
    @DisplayName("POST /api/stock/transactions/transfer - Insufficient source stock returns HTTP 409")
    void testStockTransfer_InsufficientStock_Returns409() throws Exception {
        StockTransferRequest request = new StockTransferRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                SEED_RACK_A2_ID,
                9999, // Exceeds available 150
                "TR-EXCEED",
                null,
                null
        );

        mockMvc.perform(post("/api/stock/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/stock/transactions - Lists all transactions with HTTP 200")
    void testGetAllTransactions_Returns200() throws Exception {
        mockMvc.perform(get("/api/stock/transactions")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("GET /api/stock/transactions/{id} - Existing transaction returns HTTP 200")
    void testGetTransactionById_Success() throws Exception {
        mockMvc.perform(get("/api/stock/transactions/{id}", SEED_TX_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_TX_ID.toString()))
                .andExpect(jsonPath("$.transactionType").value("STOCK_IN"))
                .andExpect(jsonPath("$.quantity").value(150))
                .andExpect(jsonPath("$.reference").value("PO-2026-001"));
    }

    @Test
    @DisplayName("GET /api/stock/transactions/{id} - Non-existent ID returns HTTP 404 Not Found")
    void testGetTransactionById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/stock/transactions/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/stock/transactions/product/{productId} - Returns product history in descending order")
    void testGetTransactionsByProductId_Success() throws Exception {
        mockMvc.perform(get("/api/stock/transactions/product/{productId}", SEED_PRODUCT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].productSku").value("SKU-BOLT-M10"));
    }

    @Test
    @DisplayName("GET /api/stock/transactions/product/{productId} - Non-existent product returns HTTP 404")
    void testGetTransactionsByProductId_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/stock/transactions/product/{productId}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
