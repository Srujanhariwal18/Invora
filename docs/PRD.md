# PRODUCT REQUIREMENTS DOCUMENT (PRD)

# Invora - Warehouse Inventory Stock IN/OUT & Threshold Alert System

## 1. Project Information

**Project Title:** Invora - Warehouse Inventory Stock IN/OUT & Threshold Alert System  
**Project Name:** Invora  
**Subject:** Java Programming  
**Project Type:** Java-based Web Application  
**Primary Technology:** Java  
**Recommended Backend:** Java Spring Boot  
**Database:** PostgreSQL (Supabase-compatible)  
**Frontend:** HTML, CSS, JavaScript, Bootstrap/Tailwind-style custom CSS  
**Optional 3D:** Three.js on the frontend  

---

# 2. Executive Summary

**Invora** is a Java-based warehouse inventory management web application designed to digitally manage warehouse products, stock movements, rack locations, and inventory thresholds.

The system allows warehouse staff to:

- Add incoming stock (Stock IN)
- Remove outgoing stock (Stock OUT)
- Transfer stock between racks
- Monitor current inventory
- Configure minimum stock thresholds
- Receive low-stock and critical-stock alerts
- View stock movement history
- Analyze warehouse utilization
- Predict approximate stock depletion
- Maintain an audit trail

The project demonstrates Java programming concepts through:

- Object-oriented programming
- Classes and objects
- Encapsulation
- Inheritance where appropriate
- Interfaces
- Exception handling
- Collections
- JDBC/JPA database connectivity
- REST APIs
- CRUD operations
- Authentication
- Validation
- Multithreading/scheduled tasks where useful

---

# 3. Problem Statement

Traditional small-scale warehouse inventory systems often depend on spreadsheets or manual registers.

This creates problems such as:

- Incorrect stock counts
- Delayed stock updates
- Unnoticed low inventory
- Duplicate entries
- Difficulty tracking stock movement
- Lack of centralized records
- Difficulty identifying fast-moving products
- No clear audit history

**Invora** solves these problems through a centralized Java-based inventory management platform.

---

# 4. Proposed Solution

Invora provides a digital warehouse inventory management platform where stock is represented digitally.

Every product has:

- SKU
- Name
- Category
- Supplier
- Quantity
- Threshold
- Warehouse
- Rack
- Unit price

Every stock movement creates a transaction.

The system automatically evaluates inventory status after each transaction.

Example:

```text
Current Stock: 82
Threshold: 100

Status: CRITICAL
```

The dashboard immediately reflects the change.

---

# 5. Goals

## Primary Goals

1. Manage warehouse inventory digitally.
2. Implement Stock IN and Stock OUT.
3. Prevent negative inventory.
4. Automatically generate threshold alerts.
5. Maintain transaction history.
6. Provide inventory analytics.
7. Demonstrate Java OOP and backend development.
8. Provide a professional user interface.
9. Provide a visual warehouse representation.
10. Maintain secure and structured database records.

## Secondary Goals

- Stock transfer
- Supplier management
- Role-based access
- Audit logs
- Stockout estimation
- Warehouse utilization
- Search and filtering

---

# 6. Target Users

## Admin

Can:
- Manage users
- Add products
- Manage warehouses
- Configure thresholds
- View reports
- Manage inventory
- View audit logs

## Warehouse Manager

Can:
- View inventory
- Perform stock IN/OUT
- Transfer stock
- View alerts
- View analytics

## Warehouse Staff

Can:
- Perform stock IN
- Perform stock OUT
- Search products
- View rack information

## Viewer

Can:
- View inventory
- View dashboard
- View analytics
- *Cannot modify data*

---

# 7. Core Functional Requirements

## FR-01 Authentication

Users must log in using:
- Username / email
- Password

System validates credentials before granting access.

---

## FR-02 Product Management

Admin can:
- Add product
- Edit product
- Delete / deactivate product
- Search product
- View product details

Product fields:

```text
Product ID
SKU
Product Name
Category
Supplier
Unit
Unit Price
Threshold
Reorder Quantity
Description
Status
```

---

## FR-03 Warehouse Management

Admin can create:
- Warehouses
- Zones
- Racks

Example:

```text
Warehouse A
 ├── Zone A
 │    ├── Rack A-01
 │    ├── Rack A-02
 │    └── Rack A-03
 │
 └── Zone B
      ├── Rack B-01
      └── Rack B-02
```

---

## FR-04 Stock IN

User enters:

```text
Product
Quantity
Supplier
Warehouse
Rack
Batch
Reference
Date
```

System:
1. Validates quantity
2. Finds inventory record
3. Increases stock
4. Creates transaction
5. Records user
6. Records timestamp
7. Updates alert status
8. Updates dashboard

Formula:

```text
newStock = currentStock + quantity
```

---

## FR-05 Stock OUT

User enters:

```text
Product
Quantity
Destination
Warehouse
Rack
Reference
Reason
```

System checks available quantity.

If:

```text
requestedQuantity > currentStock
```

Reject transaction.

Otherwise:

```text
newStock = currentStock - quantity
```

Then:
- Create transaction
- Update inventory
- Check threshold
- Create alert if necessary
- Create audit record

---

# 8. Stock Transfer

Allow users to transfer inventory:

```text
Warehouse A
Rack A-01
      ↓
Warehouse A
Rack B-02
```

System must:
1. Validate source quantity
2. Subtract from source
3. Add to destination
4. Create transfer record
5. Create audit record

---

# 9. Threshold Alert System

Each product has a configurable threshold.

Status:
- `NORMAL`
- `LOW`
- `WARNING`
- `CRITICAL`
- `OUT_OF_STOCK`

Suggested calculation:

```text
stock > threshold × 1.5
NORMAL

stock <= threshold × 1.5
LOW

stock <= threshold
WARNING

stock <= threshold × 0.5
CRITICAL

stock == 0
OUT_OF_STOCK
```

The threshold rules should be implemented in a dedicated Java service rather than scattered throughout controllers.

---

# 10. Alert Management

Alert contains:

```text
Alert ID
Product
Current Stock
Threshold
Severity
Message
Created At
Status
```

Alert statuses:
- `ACTIVE`
- `ACKNOWLEDGED`
- `RESOLVED`

Users can acknowledge or resolve alerts according to their role.

---

# 11. Inventory Analytics

Dashboard should display:
- Total products
- Total stock units
- Stock IN today
- Stock OUT today
- Low-stock products
- Critical alerts
- Warehouse utilization
- Top moving products

Charts:
- Inventory flow
- Stock movement
- Category distribution
- Stock health
- Daily IN/OUT

---

# 12. Stockout Estimation

Calculate approximate days until stockout:

```text
averageDailyOutflow = totalOutflow / numberOfDays
```

Then:

```text
estimatedDays = currentStock / averageDailyOutflow
```

If outflow is zero:

```text
estimatedDays = N/A
```

*This is an estimate, not a machine-learning prediction.*

---

# 13. Audit Log

Record important actions:
- `LOGIN`
- `PRODUCT_CREATED`
- `PRODUCT_UPDATED`
- `STOCK_IN`
- `STOCK_OUT`
- `STOCK_TRANSFER`
- `THRESHOLD_UPDATED`
- `ALERT_ACKNOWLEDGED`
- `ALERT_RESOLVED`
- `USER_CREATED`

Each record should contain:

```text
User
Action
Entity
Entity ID
Timestamp
Description
```

---

# 14. User Interface Requirements

The interface should have a premium warehouse-control-center style.

Main pages:
- Dashboard
- Warehouse
- Inventory
- Stock IN
- Stock OUT
- Transfers
- Alerts
- Analytics
- Products
- Suppliers
- Activity
- Users
- Settings

The dashboard should contain a 3D warehouse visualization where practical.

---

# 15. 3D Warehouse Visualization

The 3D interface is a frontend enhancement.

It should show:
- Racks
- Zones
- Warehouse floor
- Stock status
- Highlighted critical racks

Color coding:
- `GREEN` = Healthy
- `YELLOW` = Low
- `ORANGE` = Warning
- `RED` = Critical
- `GRAY` = Inactive

Clicking a rack should display:

```text
Rack A-04

Capacity: 1500
Current Stock: 1284
Utilization: 85.6%
Status: HEALTHY
```

The core Java inventory system must continue working even if 3D rendering is unavailable.

---

# 16. Non-Functional Requirements

## Performance
- Dashboard should load quickly
- Database queries should be optimized
- Pagination for large inventory
- Avoid unnecessary API calls

## Security
- Password hashing
- Role-based authorization
- Server-side validation
- Prepared/JPA queries
- No database credentials in frontend

## Reliability
- Stock updates must be transactional
- Stock IN/OUT must not partially complete

## Usability
- The application should be understandable to a first-time user

---

# 17. Java Concepts Demonstrated

The project should clearly demonstrate Java programming concepts.

### Classes
- `Product`
- `Inventory`
- `Warehouse`
- `Rack`
- `Supplier`
- `User`
- `StockTransaction`
- `Alert`
- `AuditLog`

### Encapsulation
Use:
- `private` fields
- `public` getters/setters

### Inheritance
Possible hierarchy:
```text
User
 ├── Admin
 ├── Manager
 └── Staff
```
*Prefer role-based design if inheritance is unnecessary.*

### Interfaces
Example:
```java
public interface InventoryService {
    void stockIn(...);
    void stockOut(...);
    void transferStock(...);
}
```

### Exception Handling
Custom exceptions:
- `InsufficientStockException`
- `ProductNotFoundException`
- `InvalidQuantityException`
- `DuplicateSkuException`
- `UnauthorizedOperationException`

### Collections
Use:
- `List`
- `Set`
- `Map`  
where appropriate.

### Enums
- `StockTransactionType`
- `AlertSeverity`
- `AlertStatus`
- `UserRole`

### Database
Use Spring Data JPA / Hibernate.

### REST
Expose APIs through Spring Boot controllers.

---

# 18. Success Criteria

The project is successful when:
- Users can authenticate
- Products can be created
- Inventory can be viewed
- Stock IN works
- Stock OUT works
- Negative stock is prevented
- Stock transfers work
- Thresholds automatically generate alerts
- Transaction history is stored
- Dashboard reflects changes
- Analytics are available
- Java backend handles business logic
- PostgreSQL stores persistent data
- The application has a polished UI
- 3D visualization works or gracefully falls back

---

# 19. Future Scope

Possible future improvements:
- Barcode scanning
- QR code scanning
- Email notifications
- WhatsApp/SMS alerts
- Supplier purchase orders
- Advanced demand forecasting
- Mobile application
- IoT warehouse sensors
- RFID integration
- AI-based demand prediction
- Automated warehouse robots
