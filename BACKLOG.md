# Stock API - Feature Backlog

This document tracks features and improvements that have been identified through the codebase and test specifications but are not yet implemented.

## Priority Levels
- 🔴 **Critical**: Core functionality mentioned in contracts/specs but missing
- 🟠 **High**: Important features that impact user experience or API completeness
- 🟡 **Medium**: Enhancements that add value but system functions without them
- 🟢 **Low**: Nice-to-have improvements and documentation

---

## 🔴 Critical Priority

### 1. Authentication & Authorization System
**Status**: Not Implemented  
**Effort**: Large (3-5 days)

**Description**:
All API endpoint specifications reference bearer token authentication and permission-based authorization, but no implementation exists.

**Requirements**:
- Implement bearer token authentication middleware in Ktor
- Add authentication interceptor to validate tokens on protected endpoints
- Implement `401 Unauthorized` response when token is missing/invalid
- Implement `403 Forbidden` response when token lacks required permissions
- Define permission model for different operations (read, write, admin)
- Add authentication context to endpoint handlers

**Evidence**:
- All endpoint specs show: `Authorization: Bearer abc123xyz`
- OpenAPI specs include `bearerAuth` security scheme
- Background steps commented out: `Given the API is authenticated with a valid bearer token`
- Commented scenarios in multiple features (GetStock line 259, Move line 256)

**Files to Modify**:
- `src/main/kotlin/org/darren/stock/ktor/Application.kt`
- All endpoint files (`Sale.kt`, `Move.kt`, `Delivery.kt`, `GetStock.kt`)
- New files: `Authentication.kt`, `AuthorizationInterceptor.kt`

**Test Coverage**:
- Uncomment and implement authentication scenarios in:
  - `GetStock/2. Get Stock Contract.feature` (line 256-273)
  - `Move/2. Movement Contract.feature` (line 253-274)
  - Background sections in multiple features

---

### 2. Idempotency for State-Changing Operations
**Status**: Partially Implemented (requestId captured but not enforced)  
**Effort**: Medium (2-3 days)

**Description**:
The `requestId` parameter is documented and captured in Sale, Delivery, and Movement endpoints but duplicate requests are not detected or handled.

**Requirements**:
- Implement idempotency key tracking (in-memory cache or database)
- Return existing response for duplicate `requestId` instead of re-processing
- Define TTL for idempotency keys (e.g., 24 hours)
- Return appropriate status code for duplicate requests (200 with existing response)
- Handle race conditions for concurrent duplicate requests

**Evidence**:
- Sale Contract feature (line 282-297): Commented scenario for duplicate `requestId`
- Delivery Contract documentation: "The `requestId` ensures that duplicate submissions of the same request are handled gracefully"
- AdminOverride feature (line 56): Commented duplicate stock count scenario

**Files to Modify**:
- `src/main/kotlin/org/darren/stock/domain/` - Add idempotency tracking
- `src/main/kotlin/org/darren/stock/ktor/Sale.kt`
- `src/main/kotlin/org/darren/stock/ktor/Delivery.kt`
- `src/main/kotlin/org/darren/stock/ktor/Move.kt`
- `src/main/kotlin/org/darren/stock/ktor/StockCount.kt`

**Test Coverage**:
- Uncomment and implement scenarios:
  - `Sales/2. Sale Contract.feature` (line 282-297)
  - `Counts/AdminOverride.feature` (line 56-80)

---

## 🟠 High Priority

### 3. Insufficient Stock Validation for Sales
**Status**: Not Implemented  
**Effort**: Small (1 day)

**Description**:
Sales can currently be recorded even when insufficient stock exists. The API should validate available quantity before allowing a sale.

**Requirements**:
- Check `quantity` at location before processing sale
- Return `400 Bad Request` with `{"status": "InsufficientStock"}` when quantity < sale amount
- Consider `pendingAdjustment` in validation (optional based on business rules)
- Leave stock level unchanged when validation fails

**Evidence**:
- Sale Contract feature (line 299-315): Commented scenario "Fail to record a sale due to insufficient stock"
- OpenAPI spec defines `InsufficientStock` as valid 400 response status

**Files to Modify**:
- `src/main/kotlin/org/darren/stock/ktor/Sale.kt`
- `src/main/kotlin/org/darren/stock/domain/` - Add validation logic

**Test Coverage**:
- Uncomment scenario in `Sales/2. Sale Contract.feature` (line 299-315)

---

### 4. Product Existence Validation
**Status**: Not Implemented  
**Effort**: Small (1 day)

**Description**:
Endpoints don't validate that a product exists before performing operations on it.

**Requirements**:
- Add product existence check before operations
- Return `404 Not Found` with `{"status": "ProductNotFound"}` for invalid products
- Implement product registry/catalog (or stub for testing)

**Evidence**:
- GetStock Contract feature (line 224-240): Commented scenario "Fail to retrieve stock level due to invalid product"
- OpenAPI specs define `ProductNotFound` as valid 404 response status

**Files to Modify**:
- `src/main/kotlin/org/darren/stock/domain/` - Add product validation
- All endpoints that reference productId

**Test Coverage**:
- Uncomment scenario in `GetStock/2. Get Stock Contract.feature` (line 224-240)

---

## 🟡 Medium Priority

### 5. Delivery Input Validation
**Status**: Partially Implemented  
**Effort**: Small (1 day)

**Description**:
Delivery endpoint needs enhanced validation for timestamps, quantities, and other input values.

**Requirements**:
- Validate `deliveredAt` timestamp format and range (not in future)
- Validate product quantities are positive
- Validate supplier reference format if provided
- Return `400 Bad Request` with `invalidValues` array

**Evidence**:
- Delivery Contract feature (line 296): Commented scenario "Fail to record a delivery due to invalid values"

**Files to Modify**:
- `src/main/kotlin/org/darren/stock/ktor/Delivery.kt`

**Test Coverage**:
- Uncomment and implement scenario in `Deliveries/2. Delivery Contract.feature` (line 296)

---

### 6. Supplier Verification System
**Status**: Not Implemented  
**Effort**: Medium (2 days)

**Description**:
Delivery documentation mentions supplier must be registered, but no validation exists.

**Requirements**:
- Implement supplier registry (in-memory or database)
- Validate `supplierId` exists before accepting delivery
- Return appropriate error for unregistered suppliers
- Add supplier management endpoints (optional)

**Evidence**:
- Delivery Contract documentation: "The `supplierId` must correspond to a registered external supplier"

**Files to Modify**:
- `src/main/kotlin/org/darren/stock/domain/` - Add supplier domain
- `src/main/kotlin/org/darren/stock/ktor/Delivery.kt`

**Test Coverage**:
- Create new scenarios for invalid supplier validation

---

### 7. Error Response Standardization
**Status**: Inconsistent  
**Effort**: Small (1 day)

**Description**:
Different scenarios show different error response formats. Need to standardize on the current pattern.

**Current Patterns**:
- ✅ `{"status": "LocationNotFound"}` - Current standard
- ✅ `{"missingFields": ["field1"]}` - Current standard
- ✅ `{"invalidValues": ["field1"]}` - Current standard
- ❌ `{"status": "error", "message": "..."}` - Old pattern in commented scenarios

**Requirements**:
- Document error response standards in copilot-instructions.md
- Update all commented scenarios to use consistent format
- Ensure new endpoints follow pattern

**Files to Review**:
- All commented scenarios with error responses
- `.github/copilot-instructions.md` - add error response guidelines

---

## 🟢 Low Priority

### 8. API Documentation & Getting Started
**Status**: Missing  
**Effort**: Medium (1-2 days)

**Description**:
No README.md or getting-started guide exists. Living documentation is generated but lacks overview.

**Requirements**:
- Create README.md with:
  - Project overview
  - Prerequisites (Kotlin, JVM version)
  - Build instructions
  - Running the server
  - Running tests
  - API overview
- Add architecture documentation
- Document development workflow

**Files to Create**:
- `README.md`
- `docs/ARCHITECTURE.md` (optional)
- `docs/DEVELOPMENT.md` (optional)

---

### 9. Missing Request Payload Validation
**Status**: Partially Implemented  
**Effort**: Small (1 day)

**Description**:
One commented scenario shows validation when requestId is missing entirely.

**Evidence**:
- Sale Contract feature (line 319-333): "Fail to record a sale due to invalid request payload"

**Note**: This may already be handled by existing `MissingFieldsDTO` logic. Needs verification.

**Action Required**:
- Verify existing behavior
- Uncomment scenario and confirm test passes
- If not passing, implement missing validation

---

## Implementation Notes

### Recommended Implementation Order

1. **Authentication & Authorization** - Foundational for security
2. **Idempotency** - Critical for production reliability
3. **Insufficient Stock Validation** - Prevents data integrity issues
4. **Product Existence Validation** - Completes basic validation layer
5. **Error Response Standardization** - Clean up before adding more features
6. **Delivery Input Validation** - Complete delivery endpoint
7. **Supplier Verification** - Enhanced delivery validation
8. **API Documentation** - Before external release
9. **Missing Request Payload** - Verify if already complete

### Testing Strategy

For each feature:
1. Uncomment relevant test scenarios
2. Run tests to confirm they fail appropriately
3. Implement feature
4. Run tests to confirm they pass
5. Run full test suite to ensure no regressions
6. Commit with reference to backlog item

### Documentation Updates

When implementing each feature:
- Update `.github/copilot-instructions.md` if patterns change
- Update OpenAPI specs if contracts change
- Add inline code documentation for complex logic
- Update this backlog with completion status

---

## Completed Features

_This section will track features as they move from backlog to completion._

### ✅ Stock Management Core
- Location hierarchy (tracked/untracked)
- Stock movements between locations
- Sales recording
- Deliveries from suppliers
- Stock counts with admin override
- Pending adjustments tracking
- Child location aggregation
- Get stock levels with optional children

### ✅ Error Handling
- LocationNotFoundException
- LocationNotTrackedException  
- BadRequestException
- MissingFieldsDTO validation
- InvalidValuesDTO validation
- StatusPages exception mapping
- Redirect to tracked parent (303)

### ✅ Code Quality
- Detekt static analysis (0 issues)
- Spotless code formatting
- SOLID principles refactoring
- Chain of Responsibility for exceptions
- Koin dependency injection
- Thread-safe concurrent operations
- Comprehensive BDD test coverage

---

**Last Updated**: 2025-12-13  
**Total Backlog Items**: 9  
**Critical**: 2 | **High**: 2 | **Medium**: 3 | **Low**: 2
