package com.invora;

import com.invora.dto.StockInRequest;
import com.invora.dto.StockOutRequest;
import com.invora.dto.StockTransferRequest;
import com.invora.entity.Category;
import com.invora.entity.Inventory;
import com.invora.entity.Product;
import com.invora.entity.Rack;
import com.invora.entity.Supplier;
import com.invora.entity.Warehouse;
import com.invora.entity.Zone;
import com.invora.exception.ConflictException;
import com.invora.repository.CategoryRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.RackRepository;
import com.invora.repository.StockTransactionRepository;
import com.invora.repository.SupplierRepository;
import com.invora.repository.WarehouseRepository;
import com.invora.repository.ZoneRepository;
import com.invora.service.StockTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.security.test.context.support.WithMockUser(username = "admin_demo", roles = {"ADMIN"})
class StockMovementConcurrencyAndRollbackTests {

    @Autowired
    private StockTransactionService stockTransactionService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private RackRepository rackRepository;

    @SpyBean
    private StockTransactionRepository stockTransactionRepository;

    private static final UUID SEED_PRODUCT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A2_ID = UUID.fromString("e2000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("Atomic Rollback: Simulated failure when saving StockTransaction rolls back inventory changes during Stock IN")
    void testAtomicRollback_StockIn_RollsBackInventoryUpdate() {
        Inventory initialInv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
        int initialBalance = initialInv.getQuantity();
        long initialTxCount = stockTransactionRepository.count();

        // Spy to throw exception on stockTransactionRepository.save
        doThrow(new RuntimeException("Simulated DB failure saving transaction log"))
                .when(stockTransactionRepository).save(any());

        StockInRequest request = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                50,
                "PO-ROLLBACK-TEST",
                "Testing atomic rollback",
                null
        );

        try {
            assertThatThrownBy(() -> stockTransactionService.processStockIn(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Simulated DB failure");

            // Verify inventory balance did NOT change
            Inventory afterInv = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
            assertThat(afterInv.getQuantity())
                    .as("Inventory quantity must remain unchanged due to atomic transaction rollback")
                    .isEqualTo(initialBalance);

            // Verify transaction was not logged
            assertThat(stockTransactionRepository.count()).isEqualTo(initialTxCount);
        } finally {
            reset(stockTransactionRepository);
        }
    }

    @Test
    @DisplayName("Atomic Rollback: Simulated failure when saving StockTransaction rolls back source and destination inventory changes during Transfer")
    void testAtomicRollback_StockTransfer_RollsBackBothRacks() {
        Inventory srcBefore = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
        int initialSrcBalance = srcBefore.getQuantity();

        Inventory dstBefore = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A2_ID).orElse(null);
        int initialDstBalance = dstBefore == null ? 0 : dstBefore.getQuantity();
        long initialTxCount = stockTransactionRepository.count();

        // Spy to throw exception on save
        doThrow(new RuntimeException("Simulated DB failure saving transfer log"))
                .when(stockTransactionRepository).save(any());

        StockTransferRequest request = new StockTransferRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                SEED_RACK_A2_ID,
                25,
                "TR-ROLLBACK-TEST",
                "Testing transfer rollback",
                null
        );

        try {
            assertThatThrownBy(() -> stockTransactionService.processStockTransfer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Simulated DB failure");

            // Verify neither rack balance was modified
            Inventory srcAfter = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A1_ID).orElseThrow();
            assertThat(srcAfter.getQuantity())
                    .as("Source rack balance must not change on rolled back transfer")
                    .isEqualTo(initialSrcBalance);

            Inventory dstAfter = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_ID, SEED_RACK_A2_ID).orElse(null);
            int dstAfterBalance = dstAfter == null ? 0 : dstAfter.getQuantity();
            assertThat(dstAfterBalance)
                    .as("Destination rack balance must not change on rolled back transfer")
                    .isEqualTo(initialDstBalance);

            assertThat(stockTransactionRepository.count()).isEqualTo(initialTxCount);
        } finally {
            reset(stockTransactionRepository);
        }
    }

    @Test
    @DisplayName("Concurrency: Concurrent stock-out requests never oversell available inventory")
    void testConcurrentStockOut_DoesNotOversell() throws InterruptedException, ExecutionException {
        // Create an isolated product and rack with known starting stock to avoid cross-test contamination
        Category cat = categoryRepository.findAll().getFirst();
        Supplier sup = supplierRepository.findAll().getFirst();
        Warehouse wh = warehouseRepository.findAll().getFirst();
        Zone zone = zoneRepository.findByWarehouseId(wh.getId()).getFirst();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Rack testRack = rackRepository.save(new Rack(zone, "Test Rack " + uniqueSuffix, "CRK-" + uniqueSuffix, 500));
        Product testProduct = productRepository.save(new Product(
                "SKU-CONC-" + uniqueSuffix,
                "Concurrent Test Widget",
                cat,
                sup,
                "PIECE",
                new BigDecimal("19.99"),
                5,
                10,
                "Description"
        ));

        // Initial inventory = 100 units
        int initialStock = 100;
        inventoryRepository.save(new Inventory(testProduct, testRack, initialStock));

        // 10 concurrent threads, each requesting 15 units (Total requested = 150 units > 100 available)
        int threadCount = 10;
        int requestQuantity = 15;
        // Exactly 6 requests can succeed: 6 * 15 = 90 (10 units remain). The 7th+ must fail.
        int expectedSuccessCount = initialStock / requestQuantity; // 6

        ExecutorService executor = new org.springframework.security.concurrent.DelegatingSecurityContextExecutorService(
                Executors.newFixedThreadPool(threadCount)
        );
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger conflictCounter = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCounter = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // ensure all threads fire at once
                    StockOutRequest req = new StockOutRequest(
                            testProduct.getId(),
                            testRack.getId(),
                            requestQuantity,
                            "CONC-REQ-" + index,
                            "Concurrent stock out test",
                            null
                    );
                    stockTransactionService.processStockOut(req);
                    successCounter.incrementAndGet();
                } catch (ConflictException ce) {
                    // Expected when stock is exhausted
                    conflictCounter.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedErrorCounter.incrementAndGet();
                }
            }));
        }

        // Release latch to start all threads concurrently
        startLatch.countDown();

        // Wait for all threads to complete
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Fetch final balance from DB
        Inventory finalInv = inventoryRepository.findByProductIdAndRackId(testProduct.getId(), testRack.getId()).orElseThrow();

        // Integrity assertions:
        // 1. Stock must NEVER be negative
        assertThat(finalInv.getQuantity())
                .as("Final stock must be >= 0 (no overselling)")
                .isGreaterThanOrEqualTo(0);

        // 2. Exactly expectedSuccessCount operations should have succeeded
        assertThat(successCounter.get())
                .as("Successful stock out operations count")
                .isEqualTo(expectedSuccessCount);

        // 3. The rest must have been rejected as ConflictException
        assertThat(conflictCounter.get())
                .as("Conflict rejections due to insufficient stock")
                .isEqualTo(threadCount - expectedSuccessCount);

        // 4. No unexpected errors (like deadlocks or data corruption)
        assertThat(unexpectedErrorCounter.get())
                .as("Unexpected errors during concurrent operations")
                .isZero();

        // 5. Final balance must match: initialStock - (successCounter * requestQuantity)
        int expectedFinalBalance = initialStock - (expectedSuccessCount * requestQuantity);
        assertThat(finalInv.getQuantity())
                .as("Final stock in database must accurately reflect the sum of all successful transactions")
                .isEqualTo(expectedFinalBalance);
    }
}
