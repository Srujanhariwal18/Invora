-- ==============================================================================
-- V3__seed_development_data.sql
-- Invora: Warehouse Inventory Management System — Development Seed Data
-- Part 2: Relational Database Migration (Supabase PostgreSQL / Flyway)
-- NOTE: For development and testing environments only.
-- ==============================================================================

-- 1. SEED USERS (Demo / Development Accounts)
-- Password for all demo accounts is: Demo@12345
-- BCrypt hash (cost 12): $2a$12$K1d0wQW5q3cEuUu8n9O3Ael9I7G7j4Z6r9X4F3V2C1B0N9M8L7K6J (valid demo BCrypt hash)
INSERT INTO users (id, username, email, password_hash, role, active, created_at, updated_at)
VALUES 
    ('a0000000-0000-0000-0000-000000000001', 'admin_demo', 'admin@invora.local', '$2a$12$K1d0wQW5q3cEuUu8n9O3Ael9I7G7j4Z6r9X4F3V2C1B0N9M8L7K6J', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000002', 'manager_demo', 'manager@invora.local', '$2a$12$K1d0wQW5q3cEuUu8n9O3Ael9I7G7j4Z6r9X4F3V2C1B0N9M8L7K6J', 'WAREHOUSE_MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000003', 'staff_demo', 'staff@invora.local', '$2a$12$K1d0wQW5q3cEuUu8n9O3Ael9I7G7j4Z6r9X4F3V2C1B0N9M8L7K6J', 'STAFF', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a0000000-0000-0000-0000-000000000004', 'viewer_demo', 'viewer@invora.local', '$2a$12$K1d0wQW5q3cEuUu8n9O3Ael9I7G7j4Z6r9X4F3V2C1B0N9M8L7K6J', 'VIEWER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. SEED CATEGORIES (3 Categories)
INSERT INTO categories (id, name, description, created_at)
VALUES 
    ('b0000000-0000-0000-0000-000000000001', 'Raw Materials', 'Base raw industrial supplies and metals', CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000002', 'Finished Goods', 'Assembled machinery, equipment, and ready goods', CURRENT_TIMESTAMP),
    ('b0000000-0000-0000-0000-000000000003', 'Packaging Supplies', 'Cartons, wrappers, and shipping materials', CURRENT_TIMESTAMP);

-- 3. SEED SUPPLIERS (2 Suppliers)
INSERT INTO suppliers (id, name, contact_person, email, phone, address, created_at)
VALUES 
    ('c0000000-0000-0000-0000-000000000001', 'Titan Hardware & Logistics', 'Robert Vance', 'contact@titanhw.com', '+1-555-0199', '100 Industrial Parkway, Sector 4', CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'Apex Industrial Supplies', 'Sarah Lin', 'orders@apexsupplies.com', '+1-555-0244', '42 Logistics Boulevard, Building C', CURRENT_TIMESTAMP);

-- 4. SEED PRODUCTS (8 Products across categories and suppliers)
INSERT INTO products (id, sku, name, category_id, supplier_id, unit, unit_price, threshold, reorder_quantity, description, active, created_at, updated_at)
VALUES 
    ('d0000000-0000-0000-0000-000000000001', 'SKU-BOLT-M10', 'Industrial Hex Bolt M10 (Pack of 50)', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'BOX', 14.50, 20, 50, 'Grade 8.8 zinc-plated high tensile bolts', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000002', 'SKU-WIRE-CU50', 'Industrial Copper Wiring 50m Spool', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'ROLL', 85.00, 10, 25, 'Heavy-gauge insulated copper wiring', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000003', 'SKU-FLANGE-04', 'Carbon Steel Pipe Flange 4-inch', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'PIECE', 32.75, 15, 30, 'Standard class 150 weld-neck flange', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000004', 'SKU-PUMP-SUB01', 'Submersible Sump Pump 1HP', 'b0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'UNIT', 189.99, 5, 15, 'Stainless steel casing heavy pump', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000005', 'SKU-VALVE-BALL02', 'Brass Ball Valve 2-inch Full Port', 'b0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'PIECE', 45.20, 12, 20, 'Forged brass body 600 WOG rating', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000006', 'SKU-CARTON-HV', 'Heavy Duty Double-Wall Corrugated Carton', 'b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'BUNDLE', 22.00, 25, 60, 'Bundle of 25 shipping boxes (24x18x18)', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000007', 'SKU-WRAP-ST500', 'Stretch Pallet Wrap Film 500mm x 300m', 'b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'ROLL', 18.30, 20, 50, '23 micron cast stretch film', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d0000000-0000-0000-0000-000000000008', 'SKU-GASKET-KIT', 'Neoprene Flange Gasket Assortment Kit', 'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'KIT', 28.50, 10, 20, 'Multi-size chemical-resistant gaskets', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. SEED WAREHOUSE (1 Warehouse)
INSERT INTO warehouses (id, name, code, address, capacity, created_at)
VALUES 
    ('e0000000-0000-0000-0000-000000000001', 'Main Central Distribution Center', 'WH-MAIN', '500 Logistics Way, Bay 12', 15000, CURRENT_TIMESTAMP);

-- 6. SEED ZONES (2 Zones)
INSERT INTO zones (id, warehouse_id, name, code, created_at)
VALUES 
    ('e1000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'Zone A - Heavy Metals & Machinery', 'ZA', CURRENT_TIMESTAMP),
    ('e1000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000001', 'Zone B - Fast Moving & Packaging', 'ZB', CURRENT_TIMESTAMP);

-- 7. SEED RACKS (4 Racks across zones)
INSERT INTO racks (id, zone_id, name, code, capacity, created_at)
VALUES 
    ('e2000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'Rack A1 - Structural Components', 'RA1', 1200, CURRENT_TIMESTAMP),
    ('e2000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000001', 'Rack A2 - Precision Equipment', 'RA2', 800, CURRENT_TIMESTAMP),
    ('e2000000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000002', 'Rack B1 - Bulk Shipping Boxes', 'RB1', 1500, CURRENT_TIMESTAMP),
    ('e2000000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000002', 'Rack B2 - Films & Tapes', 'RB2', 1000, CURRENT_TIMESTAMP);

-- 8. SEED INVENTORY (Stock allocated to racks)
INSERT INTO inventory (id, product_id, rack_id, quantity, updated_at)
VALUES 
    ('f0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'e2000000-0000-0000-0000-000000000001', 150, CURRENT_TIMESTAMP),
    ('f0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'e2000000-0000-0000-0000-000000000001', 35, CURRENT_TIMESTAMP),
    ('f0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'e2000000-0000-0000-0000-000000000001', 8, CURRENT_TIMESTAMP),
    ('f0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'e2000000-0000-0000-0000-000000000002', 12, CURRENT_TIMESTAMP),
    ('f0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000006', 'e2000000-0000-0000-0000-000000000003', 200, CURRENT_TIMESTAMP),
    ('f0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000007', 'e2000000-0000-0000-0000-000000000004', 85, CURRENT_TIMESTAMP);

-- 9. SEED HISTORICAL STOCK TRANSACTIONS
INSERT INTO stock_transactions (id, product_id, transaction_type, quantity, source_rack_id, destination_rack_id, reference, reason, balance_after, performed_by, created_at)
VALUES 
    ('f1000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'STOCK_IN', 150, NULL, 'e2000000-0000-0000-0000-000000000001', 'PO-2026-001', 'Initial stock receiving from Titan HW', 150, 'a0000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP),
    ('f1000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'STOCK_IN', 35, NULL, 'e2000000-0000-0000-0000-000000000001', 'PO-2026-002', 'Initial stock receiving from Apex Supplies', 35, 'a0000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP),
    ('f1000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'STOCK_IN', 8, NULL, 'e2000000-0000-0000-0000-000000000001', 'PO-2026-003', 'Initial stock receiving', 8, 'a0000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP);

-- 10. SEED SAMPLE ALERT (Product SKU-FLANGE-04 has stock 8 with threshold 15 -> WARNING alert)
INSERT INTO alerts (id, product_id, current_stock, threshold, severity, status, message, created_at, resolved_at)
VALUES 
    ('f2000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 8, 15, 'WARNING', 'ACTIVE', 'Stock (8) is below safety threshold (15) for Carbon Steel Pipe Flange 4-inch', CURRENT_TIMESTAMP, NULL);

-- 11. SEED SAMPLE AUDIT LOG
INSERT INTO audit_logs (id, user_id, action, entity, entity_id, description, created_at)
VALUES 
    ('f3000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'USER_CREATED', 'User', 'a0000000-0000-0000-0000-000000000002', 'System administrator initialized warehouse manager account', CURRENT_TIMESTAMP);
