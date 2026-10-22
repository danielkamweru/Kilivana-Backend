# Backend API audit

Baseline: bf61030, feature/supplier-api-updates. Initial findings were recorded before code changes. The final inventory below includes implementation updates. Subsequent isolated PostgreSQL validation is recorded in BACKEND_IMPLEMENTATION_REPORT.md; no frontend or live database was used.

## Security and domain findings

- Stateless bearer access JWT; refresh tokens rejected as access credentials; current account state checked. SecurityConfig enables method security. ADMIN paths require ADMIN; other routes require authentication unless explicitly public. Service predicates add ownership/participant checks.
- FARMER and SUPPLIER are sellers; INSPECTOR is not a product owner privilege. Original ownership helper used the supplied userId and isStaff (ADMIN only), returning 400; final service boundaries require the authenticated owner seller or ADMIN.
- Product status endpoint exists but delegates to ADMIN-only updateProductStatus. No unlisted/archived/deleted state exists. Public queries select ACTIVE; owner seller queries include nonpublic records.
- Original deleteProduct hard-deletes any state and image bytes. OrderItem, CartItem, inspections and historical identifiers reference products; retain product rows with terminal soft deletion.
- Image CRUD, primary and ordering already exist. Multipart product PUT deletes the entire image collection when files are supplied. Providers accept JPEG/PNG/GIF/WebP up to 10 MiB, but no product image count cap or transaction-aware local cleanup exists.
- Registration creates an account, profile creation creates farmer_profiles only, explicit POST /api/v1/farmers/me/farms creates farms with authenticated farmerId. FarmerRegistrationRequest is a DTO without a mapped registration consumer. Approval sets user verification, does not create farms. Never synthesize farms from personal names.
- Admin farm list already returns name and farmerId for existing associated farms regardless of account verification. Farm status is separate from account approval. Native FarmStatus binding needs explicit enum name, matching the existing product repository workaround. Existing page result uses one-based page metadata while Spring Pageable input is zero-based.
- No Admin Portal source found among sibling project directories. UI behavior has not been verified. A farmName in farmer_profiles alone does not establish a valid farm association.
- AdminFarmerService duplicates controller roster/approval logic and is not injected by AdminFarmerController. Admin product county filter is acknowledged but unused. These pre-existing gaps are recorded, not silently redefined.
- pom.xml targets Java 17 although requested runtime is Java 21. Preserve source compatibility; validate with available JDK.

- Farmer farm endpoints originally required authentication and checked ownership but did not require FARMER; class-level role enforcement is added.
- AuthService comments claim INSPECTOR cannot self-register, but actual isStaff blocks ADMIN only. This existing policy discrepancy is recorded for separate review.
- Product edit reapproval policy remains unchanged, as required by the existing documented open decision.

## Endpoint inventory

This final-source inventory follows the pre-change findings above. Every row is an implemented handler. Aliases and JSON/multipart overloads have separate rows. Request and response columns retain declared DTO types. Test names are source evidence; executed results are recorded in BACKEND_IMPLEMENTATION_REPORT.md. URL authorization applies together with listed service/inline checks.

### AddressController

Source: `src/main/java/com/kilivana/backend/admin/controller/AddressController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/users/{userId}/addresses` | `getAddressesByUser( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<AddressResponse>>>` | AddressService.getAddressesByUser | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/{userId}/addresses` | `getAddressesByUser( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<AddressResponse>>>` | AddressService.getAddressesByUser | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/users/addresses` | `getOwnAddresses(@AuthenticationPrincipal Long userId)` -> `ResponseEntity<ApiResponse<List<AddressResponse>>>` | AddressService.getAddressesByUser | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/addresses` | `getOwnAddresses(@AuthenticationPrincipal Long userId)` -> `ResponseEntity<ApiResponse<List<AddressResponse>>>` | AddressService.getAddressesByUser | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/users/{userId}/addresses` | `createAddress( @Parameter(description = "User id", example = "1") @PathVariable Long userId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.createAddress | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| POST | `/api/v1/{userId}/addresses` | `createAddress( @Parameter(description = "User id", example = "1") @PathVariable Long userId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.createAddress | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| POST | `/api/v1/users/addresses` | `createOwnAddress( @AuthenticationPrincipal Long userId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.createAddress | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/addresses` | `createOwnAddress( @AuthenticationPrincipal Long userId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.createAddress | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/users/{userId}/addresses/{addressId}` | `updateAddress( @Parameter(description = "User id", example = "1") @PathVariable Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.updateAddress | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| PUT | `/api/v1/{userId}/addresses/{addressId}` | `updateAddress( @Parameter(description = "User id", example = "1") @PathVariable Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.updateAddress | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| PUT | `/api/v1/users/addresses/{addressId}` | `updateOwnAddress( @AuthenticationPrincipal Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.updateAddress | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/addresses/{addressId}` | `updateOwnAddress( @AuthenticationPrincipal Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId, @Valid @RequestBody AddressRequest request)` -> `ResponseEntity<ApiResponse<AddressResponse>>` | AddressService.updateAddress | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/users/{userId}/addresses/{addressId}` | `deleteAddress( @Parameter(description = "User id", example = "1") @PathVariable Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId)` -> `ResponseEntity<ApiResponse<Void>>` | AddressService.deleteAddress | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| DELETE | `/api/v1/{userId}/addresses/{addressId}` | `deleteAddress( @Parameter(description = "User id", example = "1") @PathVariable Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId)` -> `ResponseEntity<ApiResponse<Void>>` | AddressService.deleteAddress | authenticated; @resourceAuthorization.self(#userId) | ServiceAuthorizationTest.java | implemented; existing route |
| DELETE | `/api/v1/users/addresses/{addressId}` | `deleteOwnAddress( @AuthenticationPrincipal Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId)` -> `ResponseEntity<ApiResponse<Void>>` | AddressService.deleteAddress | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/addresses/{addressId}` | `deleteOwnAddress( @AuthenticationPrincipal Long userId, @Parameter(description = "Address id", example = "1") @PathVariable Long addressId)` -> `ResponseEntity<ApiResponse<Void>>` | AddressService.deleteAddress | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |

### AdminBuyerController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminBuyerController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/buyers` | `listBuyers()` -> `ResponseEntity<ApiResponse<List<AdminBuyerResponse>>>` | AdminBuyerService.listBuyers | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/buyers/{userId}/suspend` | `suspendBuyer( @PathVariable Long userId, @RequestParam String reason)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.suspendUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/buyers/{userId}/unsuspend` | `unsuspendBuyer(@PathVariable Long userId)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.activateUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/buyers/{userId}` | `updateBuyerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody BuyerProfileRequest request)` -> `ResponseEntity<ApiResponse<BuyerProfileResponse>>` | ProfileService.updateBuyerProfile | ADMIN; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |

### AdminDriverController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminDriverController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/admin/drivers` | `registerDriver( @Valid @RequestBody DriverRegistrationRequest request)` -> `ResponseEntity<ApiResponse<AdminDriverResponse>>` | AdminDriverService.registerDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/drivers/{userId}` | `deleteDriver(@PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | AdminDriverService.deleteDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/drivers/{userId}` | `updateDriver( @PathVariable Long userId, @Valid @RequestBody DriverRegistrationRequest request)` -> `ResponseEntity<ApiResponse<AdminDriverResponse>>` | AdminDriverService.updateDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/drivers/{userId}/suspend` | `suspendDriver( @PathVariable Long userId, @RequestParam String reason)` -> `ResponseEntity<ApiResponse<AdminDriverResponse>>` | AdminDriverService.suspendDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/drivers/{userId}/unsuspend` | `unsuspendDriver( @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<AdminDriverResponse>>` | AdminDriverService.unsuspendDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/drivers` | `listDrivers()` -> `ResponseEntity<ApiResponse<List<AdminDriverResponse>>>` | AdminDriverService.listDrivers | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/drivers/{userId}` | `getDriver(@PathVariable Long userId)` -> `ResponseEntity<ApiResponse<AdminDriverResponse>>` | AdminDriverService.findDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### AdminFarmerController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminFarmerController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| PUT | `/api/v1/admin/farmers/{userId}/suspend` | `suspendFarmer( @PathVariable("userId") Long userId, @RequestParam String reason)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.suspendUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/farmers/{userId}/unsuspend` | `unsuspendFarmer(@PathVariable("userId") Long userId)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.activateUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/farmers` | `listFarmers( @RequestParam(required = false) String search, @RequestParam(required = false) String county, @RequestParam(required = false) UserStatus accountStatus, @RequestParam(required = false) VerificationStatus verificationStatus, Pageable pageable)` -> `ResponseEntity<ApiResponse<Map<String, Object>>>` | UserRepository.findByRole | ADMIN; URL rule; resource checks, if any, are inline | AdminFarmerControllerTest.java | implemented; existing route |
| GET | `/api/v1/admin/farmers/stats` | `getStats()` -> `ResponseEntity<ApiResponse<Map<String, Object>>>` | UserRepository.findByRole | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/farmers/{id}` | `getFarmer(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<FarmerResponse>>` | UserRepository.findByIdAndRole | ADMIN; URL rule; resource checks, if any, are inline | AdminFarmerControllerTest.java | implemented; existing route |
| PUT | `/api/v1/admin/farmers/{id}` | `updateFarmerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable("id") Long userId, @Valid @RequestBody FarmerProfileRequest request)` -> `ResponseEntity<ApiResponse<FarmerProfileResponse>>` | ProfileService.updateFarmerProfile | ADMIN; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/farmers/{id}/approve` | `approveFarmer(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<FarmerResponse>>` | UserRepository.findByIdAndRole; UserRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/farmers/{id}/reject` | `rejectFarmer( @PathVariable Long id, @RequestParam String reason)` -> `ResponseEntity<ApiResponse<FarmerResponse>>` | UserRepository.findByIdAndRole; UserRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### AdminInspectorController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminInspectorController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| PUT | `/api/v1/admin/inspectors/{userId}/suspend` | `suspendInspector( @PathVariable Long userId, @RequestParam String reason)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.suspendUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/inspectors/{userId}/unsuspend` | `unsuspendInspector(@PathVariable Long userId)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.activateUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/inspectors/{userId}` | `updateInspectorProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody InspectorProfileRequest request)` -> `ResponseEntity<ApiResponse<InspectorProfileResponse>>` | ProfileService.updateInspectorProfile | ADMIN; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |

### AdminOperationsController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminOperationsController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/dashboard/stats` | `dashboardStats()` -> `ResponseEntity<ApiResponse<DashboardStatsResponse>>` | DashboardService.getStats | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/dashboard` | `dashboard()` -> `ResponseEntity<ApiResponse<Map<String, Long>>>` | LogisticsJobRepository.count; OrderRepository.count; PaymentRepository.count | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/orders` | `orders( @RequestParam(required = false) Long buyerId, @RequestParam(required = false) String status, Pageable pageable)` -> `ResponseEntity<ApiResponse<Page<OrderResponse>>>` | OrderService.searchOrders | ADMIN; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/orders/{orderId}/assign` | `assignOrderToDriver( @PathVariable Long orderId, @RequestParam Long driverId, @RequestParam(required = false) String pickupAddress, @RequestParam(required = false) String destinationAddress)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | OrderService.assignOrderToDriver | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/logistics/jobs` | `logisticsJobs()` -> `ResponseEntity<ApiResponse<List<LogisticsJob>>>` | LogisticsService.getAllJobs | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/payments` | `payments()` -> `ResponseEntity<ApiResponse<List<PaymentResponse>>>` | PaymentService.getAllPayments | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/reports` | `reports()` -> `ResponseEntity<ApiResponse<Map<String, Long>>>` | LogisticsService.getAllJobs; OrderService.searchOrders; PaymentService.getAllPayments | ADMIN; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |

### AdminProductController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminProductController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/products` | `getAllProducts( @Parameter(description = "Filter by product status") @RequestParam(required = false) ProductStatus status, @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId, @Parameter(description = "Filter by seller ID") @RequestParam(required = false) Long sellerId, @Parameter(description = "Filter by seller county") @RequestParam(required = false) String county, @Parameter(description = "Search in product name and description") @RequestParam(required = false) String search, @Parameter(description = "Pagination and sorting parameters") Pageable pageable)` -> `ResponseEntity<ApiResponse<Page<ProductResponse>>>` | ProductService.getFilteredProducts | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/products/stats` | `getProductStats()` -> `ResponseEntity<ApiResponse<Map<String, Object>>>` | ProductService.getProductStats | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PATCH | `/api/v1/admin/products/{id}/status` | `updateProductStatus( @Parameter(description = "Authenticated admin user ID") @AuthenticationPrincipal Long adminId, @Parameter(description = "Product ID") @PathVariable Long id, @RequestBody Map<String, Object> statusUpdate)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.updateProductStatusWithReason | ADMIN; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |

### AdminSupplierController

Source: `src/main/java/com/kilivana/backend/admin/controller/AdminSupplierController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/admin/suppliers` | `registerSupplier( @Valid @RequestBody SupplierRegistrationRequest request)` -> `ResponseEntity<ApiResponse<AdminSupplierResponse>>` | AdminSupplierService.registerSupplier | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/suppliers` | `listSuppliers()` -> `ResponseEntity<ApiResponse<List<AdminSupplierResponse>>>` | AdminSupplierService.listSuppliers | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/suppliers/{userId}` | `getSupplier( @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<AdminSupplierResponse>>` | AdminSupplierService.getSupplier | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/suppliers/{userId}` | `updateSupplier( @PathVariable Long userId, @Valid @RequestBody SupplierRegistrationRequest request)` -> `ResponseEntity<ApiResponse<AdminSupplierResponse>>` | AdminSupplierService.updateSupplier | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/suppliers/{userId}` | `deleteSupplier(@PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | AdminSupplierService.deleteSupplier | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/suppliers/{userId}/suspend` | `suspendSupplier( @PathVariable Long userId, @RequestParam String reason)` -> `ResponseEntity<ApiResponse<AdminSupplierResponse>>` | AdminSupplierService.suspendSupplier | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/suppliers/{userId}/unsuspend` | `unsuspendSupplier( @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<AdminSupplierResponse>>` | AdminSupplierService.unsuspendSupplier | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### AuditLogController

Source: `src/main/java/com/kilivana/backend/admin/controller/AuditLogController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/audit-logs/{id}` | `getAuditLogById(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<AuditLogResponse>>` | AuditLogService.getAuditLogById | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/audit-logs/actor/{actorId}` | `getAuditLogsByActor( @PathVariable Long actorId)` -> `ResponseEntity<ApiResponse<List<AuditLogResponse>>>` | AuditLogService.getAuditLogsByActor | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/audit-logs/entity/{entityType}/{entityId}` | `getAuditLogsByEntity( @PathVariable String entityType, @PathVariable Long entityId)` -> `ResponseEntity<ApiResponse<List<AuditLogResponse>>>` | AuditLogService.getAuditLogsByEntity | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/audit-logs` | `searchAuditLogs( @RequestParam(required = false) Long actorId, @RequestParam(required = false) String entityType, @RequestParam(required = false) String action, @RequestParam(required = false) LocalDateTime startDate, @RequestParam(required = false) LocalDateTime endDate, @RequestParam(required = false) Pageable pageable)` -> `ResponseEntity<ApiResponse<Page<AuditLogResponse>>>` | AuditLogService.searchAuditLogs | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### InspectionController

Source: `src/main/java/com/kilivana/backend/admin/controller/InspectionController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/admin/inspections` | `createInspection(@RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.createInspection | ADMIN; @resourceAuthorization.admin() | ServiceAuthorizationTest.java | implemented; existing route |
| POST | `/api/v1/inspections` | `createInspection(@RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.createInspection | authenticated; @resourceAuthorization.admin() | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/admin/inspections/{id}` | `getInspectionById( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.getInspectionById | ADMIN; @resourceAuthorization.inspection(#id) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/inspections/{id}` | `getInspectionById( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.getInspectionById | authenticated; @resourceAuthorization.inspection(#id) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/admin/inspections` | `getAllInspections(Pageable pageable)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getAllInspections | ADMIN; URL rule; resource checks, if any, are inline | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/inspections` | `getAllInspections(Pageable pageable)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getAllInspections | authenticated; URL rule; resource checks, if any, are inline | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/admin/inspections/inspector/{inspectorId}` | `getInspectionsByInspector( @Parameter(description = "Inspector user id", example = "1") @PathVariable Long inspectorId)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getInspectionsByInspector | ADMIN; @resourceAuthorization.inspector(#inspectorId) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/inspections/inspector/{inspectorId}` | `getInspectionsByInspector( @Parameter(description = "Inspector user id", example = "1") @PathVariable Long inspectorId)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getInspectionsByInspector | authenticated; @resourceAuthorization.inspector(#inspectorId) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/admin/inspections/status/{status}` | `getInspectionsByStatus( @Parameter(description = "Inspection status", example = "PENDING") @PathVariable InspectionStatus status)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getInspectionsByStatus | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/inspections/status/{status}` | `getInspectionsByStatus( @Parameter(description = "Inspection status", example = "PENDING") @PathVariable InspectionStatus status)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getInspectionsByStatus | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/inspections/target/{targetType}/{targetId}` | `getInspectionsByTarget( @Parameter(description = "Type of the inspected target, e.g. FARM or PRODUCT") @PathVariable String targetType, @Parameter(description = "Id of the inspected target", example = "1") @PathVariable Long targetId)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getInspectionsByTarget | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/inspections/target/{targetType}/{targetId}` | `getInspectionsByTarget( @Parameter(description = "Type of the inspected target, e.g. FARM or PRODUCT") @PathVariable String targetType, @Parameter(description = "Id of the inspected target", example = "1") @PathVariable Long targetId)` -> `ResponseEntity<ApiResponse<List<InspectionResponse>>>` | InspectionService.getInspectionsByTarget | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/inspections/{id}/status` | `updateInspectionStatus( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "New status", example = "IN_PROGRESS") @RequestParam InspectionStatus status)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.updateInspectionStatus | ADMIN; @resourceAuthorization.inspection(#id) | ServiceAuthorizationTest.java | implemented; existing route |
| PUT | `/api/v1/inspections/{id}/status` | `updateInspectionStatus( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "New status", example = "IN_PROGRESS") @RequestParam InspectionStatus status)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.updateInspectionStatus | authenticated; @resourceAuthorization.inspection(#id) | ServiceAuthorizationTest.java | implemented; existing route |
| PUT | `/api/v1/admin/inspections/{id}/result` | `updateInspectionResult( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "Inspection result", example = "PASS") @RequestParam String result, @RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.updateInspectionResult | ADMIN; @resourceAuthorization.inspection(#id) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/inspections/{id}/result` | `updateInspectionResult( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "Inspection result", example = "PASS") @RequestParam String result, @RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | InspectionService.updateInspectionResult | authenticated; @resourceAuthorization.inspection(#id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/inspections/{id}/result` | `submitInspectionResult( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "Inspection result", example = "PASS") @RequestParam String result, @RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | inline | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/inspections/{id}/result` | `submitInspectionResult( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "Inspection result", example = "PASS") @RequestParam String result, @RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/inspections/{id}/evidence` | `submitEvidence( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | inline | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/inspections/{id}/evidence` | `submitEvidence( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @RequestBody InspectionRequest request)` -> `ResponseEntity<ApiResponse<InspectionResponse>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/inspections/{id}/evidence/images` | `uploadEvidenceImage( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @RequestPart("image") MultipartFile image, @Parameter(description = "Mark the image as the primary evidence photo", example = "true") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | InspectionService.uploadEvidenceImage | ADMIN; @resourceAuthorization.inspection(#inspectionId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/inspections/{id}/evidence/images` | `uploadEvidenceImage( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @RequestPart("image") MultipartFile image, @Parameter(description = "Mark the image as the primary evidence photo", example = "true") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | InspectionService.uploadEvidenceImage | authenticated; @resourceAuthorization.inspection(#inspectionId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/inspections/{id}/evidence/images` | `listEvidenceImages( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | InspectionService.listEvidenceImages | ADMIN; @resourceAuthorization.inspection(#inspectionId) | ServiceAuthorizationTest.java | implemented; existing route |
| GET | `/api/v1/inspections/{id}/evidence/images` | `listEvidenceImages( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | InspectionService.listEvidenceImages | authenticated; @resourceAuthorization.inspection(#inspectionId) | ServiceAuthorizationTest.java | implemented; existing route |
| DELETE | `/api/v1/admin/inspections/{id}/evidence/images/{imageId}` | `deleteEvidenceImage( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "Evidence image id", example = "1") @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | InspectionService.deleteEvidenceImage | ADMIN; @resourceAuthorization.inspection(#inspectionId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/inspections/{id}/evidence/images/{imageId}` | `deleteEvidenceImage( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id, @Parameter(description = "Evidence image id", example = "1") @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | InspectionService.deleteEvidenceImage | authenticated; @resourceAuthorization.inspection(#inspectionId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/inspections/{id}` | `deleteInspection( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | InspectionService.deleteInspection | ADMIN; URL rule; resource checks, if any, are inline | ServiceAuthorizationTest.java | implemented; existing route |
| DELETE | `/api/v1/inspections/{id}` | `deleteInspection( @Parameter(description = "Inspection id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | InspectionService.deleteInspection | authenticated; URL rule; resource checks, if any, are inline | ServiceAuthorizationTest.java | implemented; existing route |

### NotificationController

Source: `src/main/java/com/kilivana/backend/admin/controller/NotificationController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/admin/notifications` | `createNotification(@RequestBody Notification notification)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.createNotification | ADMIN; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/notifications` | `createNotification(@RequestBody Notification notification)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.createNotification | authenticated; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/notifications/{id}` | `getNotificationById( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.getNotificationById | ADMIN; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/notifications/{id}` | `getNotificationById( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.getNotificationById | authenticated; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/notifications` | `getAllNotifications( @Parameter(description = "Filter to unread notifications only", example = "true") @RequestParam(required = false) Boolean unread)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | NotificationService.getAllNotifications | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/notifications` | `getAllNotifications( @Parameter(description = "Filter to unread notifications only", example = "true") @RequestParam(required = false) Boolean unread)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | NotificationService.getAllNotifications | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/notifications/user/{userId}` | `getNotificationsByUserId( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | NotificationService.getNotificationsByUserId | ADMIN; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/notifications/user/{userId}` | `getNotificationsByUserId( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | NotificationService.getNotificationsByUserId | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/notifications/user/{userId}/unread` | `getUnreadNotifications( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | NotificationService.getUnreadNotifications | ADMIN; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/notifications/user/{userId}/unread` | `getUnreadNotifications( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | NotificationService.getUnreadNotifications | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/notifications/{id}/read` | `markAsRead( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.markAsRead | ADMIN; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/notifications/{id}/read` | `markAsRead( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.markAsRead | authenticated; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/notifications/{id}/read` | `markAsReadPost( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | inline | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/notifications/{id}/read` | `markAsReadPost( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/notifications/user/{userId}/read-all` | `markAllAsRead( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.markAllAsRead | ADMIN; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/notifications/user/{userId}/read-all` | `markAllAsRead( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.markAllAsRead | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/notifications/read-all` | `markOwnNotificationsAsRead(@AuthenticationPrincipal Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.markAllAsRead | ADMIN; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/notifications/read-all` | `markOwnNotificationsAsRead(@AuthenticationPrincipal Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.markAllAsRead | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/notifications/user/{userId}/read-all` | `markAllAsReadPost( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | inline | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/notifications/user/{userId}/read-all` | `markAllAsReadPost( @Parameter(description = "User id", example = "1") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/notifications/{id}` | `deleteNotification( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.deleteNotification | ADMIN; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/notifications/{id}` | `deleteNotification( @Parameter(description = "Notification id", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.deleteNotification | authenticated; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |

### ProfileController

Source: `src/main/java/com/kilivana/backend/admin/controller/ProfileController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/profiles/farmers/{userId}` | `getFarmerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<FarmerProfileResponse>>` | ProfileService.getFarmerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/farmers/{userId}` | `createFarmerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody FarmerProfileRequest request)` -> `ResponseEntity<ApiResponse<FarmerProfileResponse>>` | ProfileService.createFarmerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/farmers/{userId}` | `updateFarmerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody FarmerProfileRequest request)` -> `ResponseEntity<ApiResponse<FarmerProfileResponse>>` | ProfileService.updateFarmerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| DELETE | `/api/v1/profiles/farmers/{userId}` | `deleteFarmerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteFarmerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin | ProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/farmers/{userId}/images` | `uploadFarmerImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @RequestPart("image") MultipartFile image, @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.uploadFarmerProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| GET | `/api/v1/profiles/farmers/{userId}/images` | `getFarmerImages( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.getFarmerProfileImages | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/farmers/{userId}/images/{imageId}/primary` | `setPrimaryFarmerImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.setPrimaryFarmerProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| DELETE | `/api/v1/profiles/farmers/{userId}/images/{imageId}` | `deleteFarmerImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteFarmerProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| GET | `/api/v1/profiles/buyers/{userId}` | `getBuyerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<BuyerProfileResponse>>` | ProfileService.getBuyerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/profiles/buyers/{userId}` | `createBuyerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody BuyerProfileRequest request)` -> `ResponseEntity<ApiResponse<BuyerProfileResponse>>` | ProfileService.createBuyerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/buyers/{userId}` | `updateBuyerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody BuyerProfileRequest request)` -> `ResponseEntity<ApiResponse<BuyerProfileResponse>>` | ProfileService.updateBuyerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/profiles/buyers/{userId}` | `deleteBuyerProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteBuyerProfile | authenticated; ProfileService.ensureOwnershipOrAdmin | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/profiles/inspectors/{userId}` | `getInspectorProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<InspectorProfileResponse>>` | ProfileService.getInspectorProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/inspectors/{userId}` | `createInspectorProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody InspectorProfileRequest request)` -> `ResponseEntity<ApiResponse<InspectorProfileResponse>>` | ProfileService.createInspectorProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/inspectors/{userId}` | `updateInspectorProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody InspectorProfileRequest request)` -> `ResponseEntity<ApiResponse<InspectorProfileResponse>>` | ProfileService.updateInspectorProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/profiles/inspectors/{userId}` | `deleteInspectorProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteInspectorProfile | authenticated; ProfileService.ensureOwnershipOrAdmin | ProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/inspectors/{userId}/images` | `uploadInspectorImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @RequestPart("image") MultipartFile image, @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.uploadInspectorProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| GET | `/api/v1/profiles/inspectors/{userId}/images` | `getInspectorImages( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.getInspectorProfileImages | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/profiles/inspectors/{userId}/images/{imageId}/primary` | `setPrimaryInspectorImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.setPrimaryInspectorProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| DELETE | `/api/v1/profiles/inspectors/{userId}/images/{imageId}` | `deleteInspectorImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteInspectorProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| GET | `/api/v1/profiles/suppliers/{userId}` | `getSupplierProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<SupplierProfileResponse>>` | ProfileService.getSupplierProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/suppliers/{userId}` | `createSupplierProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody SupplierProfileRequest request)` -> `ResponseEntity<ApiResponse<SupplierProfileResponse>>` | ProfileService.createSupplierProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/suppliers/{userId}` | `updateSupplierProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody SupplierProfileRequest request)` -> `ResponseEntity<ApiResponse<SupplierProfileResponse>>` | ProfileService.updateSupplierProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/profiles/suppliers/{userId}` | `deleteSupplierProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteSupplierProfile | authenticated; ProfileService.ensureOwnershipOrAdmin | ProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/suppliers/{userId}/images` | `uploadSupplierImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @RequestPart("image") MultipartFile image, @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.uploadSupplierProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| GET | `/api/v1/profiles/suppliers/{userId}/images` | `getSupplierImages( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.getSupplierProfileImages | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/suppliers/{userId}/images/{imageId}/primary` | `setPrimarySupplierImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.setPrimarySupplierProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/profiles/suppliers/{userId}/images/{imageId}` | `deleteSupplierImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteSupplierProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | ProfileControllerTest.java | implemented; existing route |

### UserController

Source: `src/main/java/com/kilivana/backend/admin/controller/UserController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/admin/users` | `createUser(@RequestBody UserRegistrationRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.createUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/users/{id}` | `getUserById( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.getUserById | ADMIN; @resourceAuthorization.self(#id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/users` | `getAllUsers(Pageable pageable)` -> `ResponseEntity<ApiResponse<List<UserResponse>>>` | UserService.getAllUsers | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/users/role/{role}` | `getUsersByRole( @Parameter(description = "User role to filter by", example = "ADMIN") @PathVariable UserRole role)` -> `ResponseEntity<ApiResponse<List<UserResponse>>>` | UserService.getUsersByRole | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/users/status/{status}` | `getUsersByStatus( @Parameter(description = "User status to filter by", example = "ACTIVE") @PathVariable UserStatus status)` -> `ResponseEntity<ApiResponse<List<UserResponse>>>` | UserService.getUsersByStatus | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/users/{id}` | `updateUser( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id, @RequestBody UserRegistrationRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.updateUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/users/{id}/status` | `updateUserStatus( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id, @Parameter(description = "New status to assign", example = "SUSPENDED") @RequestParam UserStatus status)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.updateUserStatus | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PATCH | `/api/v1/admin/users/{id}/status` | `patchUserStatus( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id, @Parameter(description = "New status to assign", example = "SUSPENDED") @RequestParam UserStatus status)` -> `ResponseEntity<ApiResponse<UserResponse>>` | inline | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/users/{id}` | `deleteUser( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | UserService.deleteUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/users/{id}/suspend` | `suspendUser( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id, @Parameter(description = "Reason for suspension", example = "Violation of terms of service") @RequestParam String reason)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.suspendUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/users/{id}/activate` | `activateUser( @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.activateUser | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### UserProfileController

Source: `src/main/java/com/kilivana/backend/admin/controller/UserProfileController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/users` | `register(@Valid @RequestBody UserRegistrationRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | AuthService.register | public registration; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/users` | `register(@Valid @RequestBody UserRegistrationRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | AuthService.register | public registration; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/users/{id}` | `getUserProfile(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.getUserById | authenticated; @resourceAuthorization.self(#id) | no direct handler test identified | implemented; existing route |
| GET | `/api/users/{id}` | `getUserProfile(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.getUserById | authenticated; @resourceAuthorization.self(#id) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/users/me` | `updateOwnProfile( @AuthenticationPrincipal Long userId, @Valid @RequestBody UserProfileUpdateRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.updateUser | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/users/me` | `updateOwnProfile( @AuthenticationPrincipal Long userId, @Valid @RequestBody UserProfileUpdateRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | UserService.updateUser | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### AuthController

Source: `src/main/java/com/kilivana/backend/common/controller/AuthController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/auth/register` | `register(@Valid @RequestBody UserRegistrationRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | AuthService.register | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/auth/register` | `register(@Valid @RequestBody UserRegistrationRequest request)` -> `ResponseEntity<ApiResponse<UserResponse>>` | AuthService.register | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/v1/auth/login` | `login(@Valid @RequestBody AuthLoginRequest request)` -> `ResponseEntity<ApiResponse<AuthTokenResponse>>` | AuthService.login | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/auth/login` | `login(@Valid @RequestBody AuthLoginRequest request)` -> `ResponseEntity<ApiResponse<AuthTokenResponse>>` | AuthService.login | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/v1/auth/logout` | `logout()` -> `ResponseEntity<ApiResponse<Void>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/auth/logout` | `logout()` -> `ResponseEntity<ApiResponse<Void>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/auth/refresh` | `refresh(@RequestBody RefreshTokenRequest request)` -> `ResponseEntity<ApiResponse<AuthTokenResponse>>` | AuthService.refreshToken | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/auth/refresh` | `refresh(@RequestBody RefreshTokenRequest request)` -> `ResponseEntity<ApiResponse<AuthTokenResponse>>` | AuthService.refreshToken | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/v1/auth/forgot-password` | `forgotPassword(@RequestParam String email)` -> `ResponseEntity<ApiResponse<String>>` | AuthService.forgotPassword | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/auth/forgot-password` | `forgotPassword(@RequestParam String email)` -> `ResponseEntity<ApiResponse<String>>` | AuthService.forgotPassword | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/auth/reset-password` | `resetPassword(@Valid @RequestBody PasswordResetRequest request)` -> `ResponseEntity<ApiResponse<String>>` | AuthService.resetPassword | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| POST | `/api/auth/reset-password` | `resetPassword(@Valid @RequestBody PasswordResetRequest request)` -> `ResponseEntity<ApiResponse<String>>` | AuthService.resetPassword | public auth route; credential/token validation; URL rule; resource checks, if any, are inline | AuthControllerTest.java | implemented; existing route |
| GET | `/api/v1/auth/me` | `getCurrentUser()` -> `ResponseEntity<ApiResponse<UserResponse>>` | AuthService.getCurrentUser | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/auth/me` | `getCurrentUser()` -> `ResponseEntity<ApiResponse<UserResponse>>` | AuthService.getCurrentUser | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### HealthController

Source: `src/main/java/com/kilivana/backend/common/controller/HealthController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/` | `health()` -> `ResponseEntity<String>` | inline | public GET; URL rule; resource checks, if any, are inline | RequestLoggingFilterTest.java, HealthControllerTest.java | implemented; existing route |
| GET | `/api/v1/health` | `apiHealth()` -> `ResponseEntity<Map<String, Object>>` | inline | public GET; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/favicon.ico` | `favicon()` -> `ResponseEntity<Void>` | inline | public GET; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### PublicFarmerController

Source: `src/main/java/com/kilivana/backend/common/controller/PublicFarmerController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/users/farmers/{farmerId}` | `getPublicFarmerProfile( @Parameter(description = "Farmer ID", example = "23") @PathVariable Long farmerId)` -> `ResponseEntity<ApiResponse<PublicFarmerProfileResponse>>` | UserRepository.findByIdAndRole | public; visibility checks where listed; URL rule; resource checks, if any, are inline | PublicFarmerControllerTest.java | implemented; existing route |

### RegionController

Source: `src/main/java/com/kilivana/backend/common/controller/RegionController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/regions` | `listCounties()` -> `ResponseEntity<ApiResponse<List<String>>>` | inline | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/regions/currency` | `currency()` -> `ResponseEntity<ApiResponse<Map<String, String>>>` | inline | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### StoredImageController

Source: `src/main/java/com/kilivana/backend/common/controller/StoredImageController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/images/{publicId}` | `read(@PathVariable String publicId)` -> `ResponseEntity<byte[]>` | StoredImageRepository.findById; com.kilivana.backend.security.ProductVisibility.requireImageReadable | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### CartCompatibilityController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/CartCompatibilityController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/cart` | `getCart(@AuthenticationPrincipal Long buyerId)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartService.getCartByBuyer | authenticated; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/cart/items` | `addItem( @AuthenticationPrincipal Long buyerId, @RequestBody CartItemRequest request)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartService.addItemToCart | authenticated; @resourceAuthorization.buyer(#buyerId); CartService.orderableProduct | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/cart/items/{itemId}` | `updateItem( @Parameter(description = "ID of the cart item to update") @PathVariable Long itemId, @Parameter(description = "New quantity for the cart item") @RequestParam Integer quantity)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartItemRepository.findById; CartService.updateCartItem | authenticated; @resourceAuthorization.cart(#cartId); CartService.orderableProduct | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/cart/items/{itemId}` | `removeItem(@Parameter(description = "ID of the cart item to remove") @PathVariable Long itemId)` -> `ResponseEntity<ApiResponse<Void>>` | CartItemRepository.findById; CartService.removeCartItem | authenticated; @resourceAuthorization.cart(#cartId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/checkout/validate` | `validateCheckout(@AuthenticationPrincipal Long buyerId)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartService.getCartByBuyer | authenticated; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/checkout` | `checkout(@AuthenticationPrincipal Long buyerId)` -> `ResponseEntity<ApiResponse<Void>>` | CartService.checkout; CartService.getCartByBuyer | authenticated; @resourceAuthorization.buyer(#buyerId); @resourceAuthorization.cart(#cartId) | no direct handler test identified | implemented; existing route |

### CartController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/CartController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/carts/buyer/{buyerId}` | `getCartByBuyer(@Parameter(description = "Buyer identifier") @PathVariable Long buyerId)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartService.getCartByBuyer | authenticated; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/carts/buyer/{buyerId}/items` | `addItemToCart(@Parameter(description = "Buyer identifier") @PathVariable Long buyerId, @Parameter(description = "Cart item to add") @RequestBody CartItemRequest request)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartService.addItemToCart | authenticated; @resourceAuthorization.buyer(#buyerId); CartService.orderableProduct | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/carts/{cartId}/items/{productId}` | `updateCartItem( @Parameter(description = "Cart identifier") @PathVariable Long cartId, @Parameter(description = "Product identifier") @PathVariable Long productId, @Parameter(description = "New quantity for the cart item") @RequestParam Integer quantity)` -> `ResponseEntity<ApiResponse<CartResponse>>` | CartService.updateCartItem | authenticated; @resourceAuthorization.cart(#cartId); CartService.orderableProduct | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/carts/{cartId}/items/{productId}` | `removeCartItem( @Parameter(description = "Cart identifier") @PathVariable Long cartId, @Parameter(description = "Product identifier") @PathVariable Long productId)` -> `ResponseEntity<ApiResponse<Void>>` | CartService.removeCartItem | authenticated; @resourceAuthorization.cart(#cartId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/carts/{cartId}/checkout` | `checkout(@Parameter(description = "Cart identifier") @PathVariable Long cartId)` -> `ResponseEntity<ApiResponse<Void>>` | CartService.checkout | authenticated; @resourceAuthorization.cart(#cartId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/carts/{cartId}` | `clearCart(@Parameter(description = "Cart identifier") @PathVariable Long cartId)` -> `ResponseEntity<ApiResponse<Void>>` | CartService.clearCart | authenticated; @resourceAuthorization.cart(#cartId) | no direct handler test identified | implemented; existing route |

### CatalogCompatibilityController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/CatalogCompatibilityController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/sellers/{sellerId}/products` | `getSellerProducts( @Parameter(description = "ID of the seller") @PathVariable Long sellerId)` -> `ResponseEntity<ApiResponse<List<ProductResponse>>>` | ProductService.getProductsBySeller | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### CategoryController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/CategoryController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/categories` | `createCategory(@Parameter(description = "Category creation details") @RequestBody CategoryRequest request)` -> `ResponseEntity<ApiResponse<CategoryResponse>>` | CategoryService.createCategory | authenticated; @resourceAuthorization.admin() | CatalogVisibilityTest.java | implemented; existing route |
| GET | `/api/v1/categories/{id}` | `getCategoryById(@Parameter(description = "Category identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<CategoryResponse>>` | CategoryService.getCategoryById | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/categories` | `getAllCategories(@Parameter(description = "Pagination and sorting parameters") Pageable pageable)` -> `ResponseEntity<ApiResponse<List<CategoryResponse>>>` | CategoryService.getAllCategories | public; visibility checks where listed; URL rule; resource checks, if any, are inline | CatalogVisibilityTest.java | implemented; existing route |
| GET | `/api/v1/categories/type/{type}` | `getCategoriesByType(@Parameter(description = "Seller type to filter categories by") @PathVariable SellerType type)` -> `ResponseEntity<ApiResponse<List<CategoryResponse>>>` | CategoryService.getCategoriesByType | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/categories/active` | `getActiveCategories()` -> `ResponseEntity<ApiResponse<List<CategoryResponse>>>` | CategoryService.getActiveCategories | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/categories/{id}` | `updateCategory( @Parameter(description = "Category identifier") @PathVariable Long id, @Parameter(description = "Updated category details") @RequestBody CategoryRequest request)` -> `ResponseEntity<ApiResponse<CategoryResponse>>` | CategoryService.updateCategory | authenticated; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/categories/{id}/status` | `updateCategoryStatus( @Parameter(description = "Category identifier") @PathVariable Long id, @Parameter(description = "New active status") @RequestParam Boolean active)` -> `ResponseEntity<ApiResponse<CategoryResponse>>` | CategoryService.updateCategoryStatus | authenticated; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/categories/{id}` | `deleteCategory(@Parameter(description = "Category identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | CategoryService.deleteCategory | authenticated; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |

### CropTypeController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/CropTypeController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/crop-types` | `listCropTypes()` -> `ResponseEntity<ApiResponse<List<CropTypeDto>>>` | CropTypeRepository.findByActiveTrueOrderByName | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### DarajaCallbackController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/DarajaCallbackController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/payments/daraja/callback` | `callback( @RequestBody DarajaCallbackRequest callback, HttpServletRequest request)` -> `ResponseEntity<ApiResponse<DarajaCallbackResponse>>` | DarajaCallbackService.processCallback | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### DisputeController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/DisputeController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/disputes` | `createDispute(@Parameter(description = "Dispute creation details") @RequestBody DisputeRequest request)` -> `ResponseEntity<ApiResponse<DisputeResponse>>` | DisputeService.createDispute | authenticated; @resourceAuthorization.self(#request.raisedBy) and @resourceAuthorization.orderParticipant(#request.orderId) and (@resourceAuthorization.admin() or #request.status == null or #request.status.name() == 'OPEN') | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/disputes/{id}` | `getDisputeById(@Parameter(description = "Dispute identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<DisputeResponse>>` | DisputeService.getDisputeById | authenticated; @resourceAuthorization.dispute(#id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/disputes/order/{orderId}` | `getDisputesByOrder(@Parameter(description = "Order identifier") @PathVariable Long orderId)` -> `ResponseEntity<ApiResponse<List<DisputeResponse>>>` | DisputeService.getDisputesByOrder | authenticated; @resourceAuthorization.order(#orderId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/disputes/user/{userId}` | `getDisputesByUser(@Parameter(description = "User identifier") @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<DisputeResponse>>>` | DisputeService.getDisputesByUser | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/disputes/{id}/status` | `updateDisputeStatus( @Parameter(description = "Dispute identifier") @PathVariable Long id, @Parameter(description = "New dispute status") @RequestParam DisputeStatus status)` -> `ResponseEntity<ApiResponse<DisputeResponse>>` | DisputeService.updateDisputeStatus | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/disputes/{id}/resolution` | `resolveDispute( @Parameter(description = "Dispute identifier") @PathVariable Long id, @Parameter(description = "Dispute resolution details") @RequestBody DisputeResolutionRequest request)` -> `ResponseEntity<ApiResponse<DisputeResponse>>` | DisputeService.resolveDispute | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### EcommerceNotificationController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/EcommerceNotificationController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/ecommerce/notifications/orders/{orderId}` | `sendOrderNotification( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long orderId, @RequestParam EcommerceNotificationType type, @RequestParam(required = false) String title, @RequestParam(required = false) String message)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | EcommerceNotificationService.sendCustomNotification; EcommerceNotificationService.sendOrderNotification | authenticated; @resourceAuthorization.admin() | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/ecommerce/notifications/payments/{paymentId}` | `sendPaymentNotification( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long paymentId, @RequestParam EcommerceNotificationType type, @RequestParam(required = false) String title, @RequestParam(required = false) String message)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | EcommerceNotificationService.sendCustomNotification; EcommerceNotificationService.sendPaymentNotification | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/ecommerce/notifications/disputes/{disputeId}` | `sendDisputeNotification( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long disputeId, @RequestParam EcommerceNotificationType type, @RequestParam(required = false) String title, @RequestParam(required = false) String message)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | EcommerceNotificationService.sendCustomNotification; EcommerceNotificationService.sendDisputeNotification | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/ecommerce/notifications/inventory/{productId}` | `sendInventoryNotification( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long productId, @RequestParam String productName, @RequestParam EcommerceNotificationType type)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | EcommerceNotificationService.sendInventoryNotification | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/ecommerce/notifications` | `getMyNotifications( @AuthenticationPrincipal Long authenticatedUserId, @RequestParam(value = "unread", required = false) Boolean unread)` -> `ResponseEntity<ApiResponse<List<NotificationResponse>>>` | EcommerceNotificationService.getEcommerceNotifications; EcommerceNotificationService.getUnreadEcommerceNotifications | authenticated; @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/ecommerce/notifications/{id}` | `getNotificationById(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.getNotificationById | authenticated; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/ecommerce/notifications/{id}/read` | `markAsRead(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<NotificationResponse>>` | NotificationService.markAsRead | authenticated; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/ecommerce/notifications/read-all` | `markAllAsRead(@AuthenticationPrincipal Long authenticatedUserId)` -> `ResponseEntity<ApiResponse<Void>>` | EcommerceNotificationService.getUnreadEcommerceNotifications; NotificationService.markAsRead | authenticated; @resourceAuthorization.notification(#id); @resourceAuthorization.self(#userId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/ecommerce/notifications/{id}` | `deleteNotification(@PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | NotificationService.deleteNotification | authenticated; @resourceAuthorization.notification(#id) | no direct handler test identified | implemented; existing route |

### OrderController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/OrderController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/orders` | `createOrder(@Parameter(description = "Order creation details") @Valid @RequestBody OrderRequest request)` -> `ResponseEntity<ApiResponse<OrderResponse>>` | OrderService.createOrder; OrderService.toResponse | authenticated; @resourceAuthorization.admin(); @resourceAuthorization.order(#order.id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/orders/{id}` | `getOrderById(@Parameter(description = "Order identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<OrderResponse>>` | OrderService.getOrderById; OrderService.toResponse | authenticated; @resourceAuthorization.order(#id); @resourceAuthorization.order(#order.id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/orders` | `getOrders( @Parameter(description = "Filter orders by buyer ID") @RequestParam(required = false) Long buyerId, @Parameter(description = "Filter orders by status") @RequestParam(required = false) String status, @Parameter(description = "Pagination and sorting parameters") Pageable pageable)` -> `ResponseEntity<ApiResponse<Page<OrderResponse>>>` | OrderService.searchOrders | authenticated; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/orders/buyer/{buyerId}` | `getOrdersByBuyer(@Parameter(description = "Buyer identifier") @PathVariable Long buyerId)` -> `ResponseEntity<ApiResponse<List<OrderResponse>>>` | OrderService.getOrdersByBuyer | authenticated; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/orders/search` | `searchOrders( @Parameter(description = "Filter orders by buyer ID") @RequestParam(required = false) Long buyerId, @Parameter(description = "Filter orders by status") @RequestParam(required = false) String status, @Parameter(description = "Pagination and sorting parameters") Pageable pageable)` -> `ResponseEntity<ApiResponse<Page<OrderResponse>>>` | OrderService.searchOrders | authenticated; @resourceAuthorization.buyer(#buyerId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/orders/{id}/status` | `updateOrderStatus( @Parameter(description = "Order identifier") @PathVariable Long id, @Parameter(description = "New order status value") @RequestParam String status)` -> `ResponseEntity<ApiResponse<OrderResponse>>` | OrderService.toResponse; OrderService.updateOrderStatus | authenticated; @resourceAuthorization.order(#order.id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/orders/{id}/cancel` | `cancelOrder( @Parameter(description = "Order identifier") @PathVariable Long id, @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason)` -> `ResponseEntity<ApiResponse<OrderResponse>>` | OrderService.cancelOrder; OrderService.toResponse | authenticated; @resourceAuthorization.order(#order.id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/orders/{id}/confirm-receipt` | `confirmReceipt(@Parameter(description = "Order identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<OrderResponse>>` | OrderService.confirmReceipt; OrderService.toResponse | authenticated; @resourceAuthorization.order(#order.id); @resourceAuthorization.order(#orderId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/orders/{id}/status` | `postOrderStatus( @Parameter(description = "Order identifier") @PathVariable Long id, @Parameter(description = "New order status value") @RequestParam String status)` -> `ResponseEntity<ApiResponse<OrderResponse>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/orders/{id}/timeline` | `getOrderTimeline(@Parameter(description = "Order identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<List<OrderEvent>>>` | OrderService.getOrderTimeline | authenticated; @resourceAuthorization.order(#orderId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/orders/{id}` | `deleteOrder(@Parameter(description = "Order identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | OrderService.deleteOrder | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### PaymentController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/PaymentController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/payments` | `createPayment(@Parameter(description = "Payment creation details") @RequestBody PaymentRequest request)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | PaymentService.createPayment; PaymentService.mapToResponse | authenticated; @resourceAuthorization.admin(); @resourceAuthorization.payment(#payment.id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/payments/initiate` | `initiatePayment(@Parameter(description = "Payment initiation details") @RequestBody PaymentRequest request)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/payments/webhook` | `paymentWebhook(@Parameter(description = "Webhook notification payload") @RequestBody PaymentRequest request)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/payments/{id}` | `getPaymentById(@Parameter(description = "Payment identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | PaymentService.getPaymentById; PaymentService.mapToResponse | authenticated; @resourceAuthorization.payment(#id); @resourceAuthorization.payment(#payment.id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/payments/order/{orderId}` | `getPaymentsByOrder(@Parameter(description = "Order identifier") @PathVariable Long orderId)` -> `ResponseEntity<ApiResponse<List<PaymentResponse>>>` | PaymentService.getPaymentsByOrder | authenticated; @resourceAuthorization.order(#orderId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/payments/{id}/status` | `updatePaymentStatus( @Parameter(description = "Payment identifier") @PathVariable Long id, @Parameter(description = "New payment status value") @RequestParam String status)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | PaymentService.mapToResponse; PaymentService.updatePaymentStatus | authenticated; @resourceAuthorization.payment(#payment.id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/payments/{id}/refund` | `refundPayment(@Parameter(description = "Payment identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | PaymentService.mapToResponse; PaymentService.updatePaymentStatus | authenticated; @resourceAuthorization.payment(#payment.id) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/payments/stk-push/farmer` | `initiateStkPushFarmer( @Parameter(description = "STK Push initiation details") @RequestBody StkPushInitiateRequest request)` -> `ResponseEntity<ApiResponse<StkPushInitiateResponse>>` | PaymentService.initiateStkPush | authenticated; @resourceAuthorization.sellerPayment(#userId, #userRole, #request.orderId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/payments/stk-push/supplier` | `initiateStkPushSupplier( @Parameter(description = "STK Push initiation details") @RequestBody StkPushInitiateRequest request)` -> `ResponseEntity<ApiResponse<StkPushInitiateResponse>>` | PaymentService.initiateStkPush | authenticated; @resourceAuthorization.sellerPayment(#userId, #userRole, #request.orderId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/payments/{id}/status` | `getPaymentStatus( @Parameter(description = "Payment identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<PaymentResponse>>` | PaymentService.getPaymentById; PaymentService.mapToResponse | authenticated; @resourceAuthorization.payment(#id); @resourceAuthorization.payment(#payment.id) | no direct handler test identified | implemented; existing route |

### ProductController

Source: `src/main/java/com/kilivana/backend/ecommerce/controller/ProductController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/products` | `createProduct( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product details (multipart part named \"product\")") @Valid @RequestPart("product") ProductRequest request, @Parameter(description = "Product images (multipart part named \"images\")") @RequestPart(value = "images", required = false) List<MultipartFile> images)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.createProduct; ProductService.createProductWithImages | authenticated; @resourceAuthorization.seller(#userId) | CatalogVisibilityTest.java | implemented; existing route |
| POST | `/api/v1/products` | `createProductJson( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product details") @Valid @RequestBody ProductRequest request)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.createProduct | authenticated; @resourceAuthorization.seller(#userId) | CatalogVisibilityTest.java | implemented; existing route |
| GET | `/api/v1/products/categories` | `listCategories()` -> `ResponseEntity<ApiResponse<List<com.kilivana.backend.ecommerce.dto.CategoryResponse>>>` | CategoryService.getAllCategories | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/products` | `getAllProducts()` -> `ResponseEntity<ApiResponse<List<ProductResponse>>>` | ProductService.getAllProducts | public; visibility checks where listed; URL rule; resource checks, if any, are inline | CatalogVisibilityTest.java | implemented; existing route |
| GET | `/api/v1/products/{id}` | `getProductById(@Parameter(description = "Product identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.getProductById | public; visibility checks where listed; ProductService.requireReadable | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/products/seller/{sellerId}` | `getProductsBySeller(@Parameter(description = "Seller identifier") @PathVariable Long sellerId)` -> `ResponseEntity<ApiResponse<List<ProductResponse>>>` | ProductService.getProductsBySeller | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/products/category/{categoryId}` | `getProductsByCategory(@Parameter(description = "Category identifier") @PathVariable Long categoryId)` -> `ResponseEntity<ApiResponse<List<ProductResponse>>>` | ProductService.getProductsByCategory | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/products/search` | `searchProducts( @Parameter(description = "Filter by product name (partial match)") @RequestParam(required = false) String name, @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId, @Parameter(description = "Filter by seller type") @RequestParam(required = false) String sellerType, @Parameter(description = "Pagination and sorting parameters") Pageable pageable)` -> `ResponseEntity<ApiResponse<Page<ProductResponse>>>` | ProductService.searchProducts | public; visibility checks where listed; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/products/{id}` | `updateProduct( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long id, @Parameter(description = "Updated product details (multipart part named \"product\")") @Valid @RequestPart("product") ProductRequest request, @Parameter(description = "Additional or replacement images (multipart part named \"images\")") @RequestPart(value = "images", required = false) List<MultipartFile> images)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.ensureProductOwnership; ProductService.updateProduct; ProductService.updateProductWithImages | authenticated; @productVisibility.canModifyProduct(#id); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/products/{id}` | `updateProductJson( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long id, @Parameter(description = "Updated product details") @Valid @RequestBody ProductRequest request)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.ensureProductOwnership; ProductService.updateProduct | authenticated; @productVisibility.canModifyProduct(#id); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/products/{id}/status` | `updateProductStatus( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long id, @Parameter(description = "New product status") @RequestParam ProductStatus status)` -> `ResponseEntity<ApiResponse<ProductResponse>>` | ProductService.ensureProductOwnership; ProductService.updateSellerProductStatus | authenticated; @productVisibility.canModifyProduct(#id); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/products/{id}` | `deleteProduct( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | ProductService.deleteProduct; ProductService.ensureProductOwnership | authenticated; @productVisibility.canModifyProduct(#id); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/products/{id}/images` | `uploadProductImage( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long id, @Parameter(description = "Image file to upload") @RequestPart("image") MultipartFile image, @Parameter(description = "Display order of the image") @RequestParam(value = "sortOrder", required = false) Integer sortOrder, @Parameter(description = "Whether this image should be the primary image") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<ProductImageResponse>>` | ProductService.ensureProductOwnership; ProductService.uploadProductImage | authenticated; @productVisibility.canModifyProduct(#productId); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/products/{id}/images` | `getProductImages(@Parameter(description = "Product identifier") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<List<ProductImageResponse>>>` | ProductService.getProductById | public; visibility checks where listed; ProductService.requireReadable | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/products/{productId}/images/{imageId}` | `updateProductImage( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long productId, @Parameter(description = "Image identifier") @PathVariable Long imageId, @Parameter(description = "Updated image file") @RequestPart("image") MultipartFile image, @Parameter(description = "Display order of the image") @RequestParam(value = "sortOrder", required = false) Integer sortOrder, @Parameter(description = "Whether this image should be the primary image") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<ProductImageResponse>>` | ProductService.ensureProductOwnership; ProductService.updateProductImage | authenticated; @productVisibility.canModifyProduct(#productId); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/products/{productId}/images/{imageId}/primary` | `setPrimaryImage( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long productId, @Parameter(description = "Image identifier") @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProductService.ensureProductOwnership; ProductService.setPrimaryImage | authenticated; @productVisibility.canModifyProduct(#productId); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/products/{productId}/images/reorder` | `reorderImages( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long productId, @Parameter(description = "Image IDs in the desired order") @RequestBody List<Long> imageIdsInOrder)` -> `ResponseEntity<ApiResponse<Void>>` | ProductService.ensureProductOwnership; ProductService.reorderImages | authenticated; @productVisibility.canModifyProduct(#productId); ensureProductOwnership | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/products/{productId}/images/{imageId}` | `deleteProductImage( @Parameter(description = "Authenticated seller or buyer identifier") @AuthenticationPrincipal Long userId, @Parameter(description = "Product identifier") @PathVariable Long productId, @Parameter(description = "Image identifier") @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProductService.deleteProductImage; ProductService.ensureProductOwnership | authenticated; @productVisibility.canModifyProduct(#productId); ensureProductOwnership | no direct handler test identified | implemented; existing route |

### CropController

Source: `src/main/java/com/kilivana/backend/farm/controller/CropController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/crops` | `listCrops( @RequestParam(required = false) Long farmerId, @RequestParam(required = false) Long farmId, @RequestParam(required = false) Long cropTypeId, @RequestParam(required = false) String category, @RequestParam(required = false) CropStatus status, @RequestParam(required = false) String search, Pageable pageable)` -> `ResponseEntity<ApiResponse<Map<String, Object>>>` | CropRepository.searchCrops | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/crops/summary` | `getCropSummary()` -> `ResponseEntity<ApiResponse<List<CropSummaryDto>>>` | CropRepository.findAll; CropTypeRepository.findAll | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/crops/types` | `getCropTypes()` -> `ResponseEntity<ApiResponse<List<CropTypeDto>>>` | CropTypeRepository.findByActiveTrueOrderByName | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/crops/{id}` | `updateCrop( @Parameter(description = "ID of the crop to update") @PathVariable Long id, @RequestBody CropResponse request)` -> `ResponseEntity<ApiResponse<CropResponse>>` | CropRepository.findById; CropRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/crops/{id}/status` | `updateCropStatus( @Parameter(description = "ID of the crop to update") @PathVariable Long id, @Parameter(description = "New status for the crop") @RequestParam CropStatus status)` -> `ResponseEntity<ApiResponse<CropResponse>>` | CropRepository.findById; CropRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/crops/{id}` | `deleteCrop(@Parameter(description = "ID of the crop to delete") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | CropRepository.deleteById; CropRepository.findById | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### FarmController

Source: `src/main/java/com/kilivana/backend/farm/controller/FarmController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/farms` | `listFarms( @RequestParam(required = false) Long farmerId, @RequestParam(required = false) String county, @RequestParam(required = false) FarmStatus status, @RequestParam(required = false) String search, Pageable pageable)` -> `ResponseEntity<ApiResponse<Map<String, Object>>>` | FarmRepository.searchFarms | ADMIN; URL rule; resource checks, if any, are inline | AdminFarmVisibilityTest.java | implemented; existing route |
| GET | `/api/v1/admin/farms/{id}` | `getFarm(@Parameter(description = "ID of the farm") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<FarmResponse>>` | FarmRepository.findById | ADMIN; URL rule; resource checks, if any, are inline | AdminFarmVisibilityTest.java | implemented; existing route |
| PUT | `/api/v1/admin/farms/{id}` | `updateFarm( @Parameter(description = "ID of the farm to update") @PathVariable Long id, @RequestBody FarmResponse request)` -> `ResponseEntity<ApiResponse<FarmResponse>>` | FarmRepository.findById; FarmRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/admin/farms/{id}/status` | `updateFarmStatus( @Parameter(description = "ID of the farm to update") @PathVariable Long id, @Parameter(description = "New status for the farm") @RequestParam FarmStatus status)` -> `ResponseEntity<ApiResponse<FarmResponse>>` | FarmRepository.findById; FarmRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/admin/farms/{id}` | `deleteFarm(@Parameter(description = "ID of the farm to delete") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | FarmRepository.delete; FarmRepository.findById | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### FarmerFarmController

Source: `src/main/java/com/kilivana/backend/farm/controller/FarmerFarmController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/farmers/me/farms` | `createFarm( @AuthenticationPrincipal Long authenticatedUserId, @Valid @RequestBody FarmRequest request)` -> `ResponseEntity<ApiResponse<FarmResponse>>` | FarmRepository.save | FARMER; hasRole('FARMER') | FarmerFarmControllerTest.java | implemented; existing route |
| GET | `/api/v1/farmers/me/farms` | `listMyFarms( @AuthenticationPrincipal Long authenticatedUserId)` -> `ResponseEntity<ApiResponse<List<FarmResponse>>>` | FarmRepository.findByFarmerId | FARMER; hasRole('FARMER') | FarmerFarmControllerTest.java | implemented; existing route |
| GET | `/api/v1/farmers/me/farms/{id}` | `getMyFarm( @AuthenticationPrincipal Long authenticatedUserId, @Parameter(description = "ID of the farm") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<FarmResponse>>` | FarmRepository.findById | FARMER; farm.farmerId equals authenticated principal; hasRole('FARMER') | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/farmers/me/farms/{id}` | `updateMyFarm( @AuthenticationPrincipal Long authenticatedUserId, @Parameter(description = "ID of the farm to update") @PathVariable Long id, @Valid @RequestBody FarmRequest request)` -> `ResponseEntity<ApiResponse<FarmResponse>>` | FarmRepository.findById; FarmRepository.save | FARMER; farm.farmerId equals authenticated principal; hasRole('FARMER') | FarmerFarmControllerTest.java | implemented; existing route |

### KycController

Source: `src/main/java/com/kilivana/backend/farm/controller/KycController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/kyc` | `listDocuments()` -> `ResponseEntity<ApiResponse<List<KycDocumentResponse>>>` | KycDocumentRepository.findAll | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/admin/kyc/pending` | `listPendingDocuments()` -> `ResponseEntity<ApiResponse<List<KycDocumentResponse>>>` | KycDocumentRepository.findByDocumentStatus | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/kyc/{id}/approve` | `approveDocument( @Parameter(description = "ID of the KYC document to approve") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<KycDocumentResponse>>` | KycDocumentRepository.findById; KycDocumentRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/admin/kyc/{id}/reject` | `rejectDocument( @Parameter(description = "ID of the KYC document to reject") @PathVariable Long id, @Parameter(description = "Reason for rejecting the KYC document") @RequestParam String reason)` -> `ResponseEntity<ApiResponse<KycDocumentResponse>>` | KycDocumentRepository.findById; KycDocumentRepository.save | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### DriverLocationController

Source: `src/main/java/com/kilivana/backend/logistics/controller/DriverLocationController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/tracking/{jobId}/location` | `submitLocation( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId, @Valid @RequestBody LocationUpdateRequest request)` -> `ResponseEntity<ApiResponse<LocationUpdateResponse>>` | DriverLocationService.submitLocation | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/tracking/{jobId}/location` | `getLocation( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId)` -> `ResponseEntity<ApiResponse<DriverLocationResponse>>` | TrackingAuthorizationService.ensureCanTrack; DriverLocationService.getLatest | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/tracking/{jobId}/status` | `getStatus( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId)` -> `ResponseEntity<ApiResponse<DriverLocationResponse>>` | TrackingAuthorizationService.ensureCanTrack; DriverLocationService.getLatest | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### DriverProfileController

Source: `src/main/java/com/kilivana/backend/logistics/controller/DriverProfileController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/profiles/drivers/{userId}` | `getDriverProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<DriverProfileResponse>>` | ProfileService.getDriverProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | DriverProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/drivers/{userId}` | `createDriverProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody DriverProfileRequest request)` -> `ResponseEntity<ApiResponse<DriverProfileResponse>>` | ProfileService.createDriverProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | DriverProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/drivers/{userId}` | `updateDriverProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @Valid @RequestBody DriverProfileRequest request)` -> `ResponseEntity<ApiResponse<DriverProfileResponse>>` | ProfileService.updateDriverProfile | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | DriverProfileControllerTest.java | implemented; existing route |
| DELETE | `/api/v1/profiles/drivers/{userId}` | `deleteDriverProfile( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteDriverProfile | authenticated; ProfileService.ensureOwnershipOrAdmin | DriverProfileControllerTest.java | implemented; existing route |
| POST | `/api/v1/profiles/drivers/{userId}/images` | `uploadDriverImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @RequestPart("image") MultipartFile image, @RequestParam(value = "isPrimary", required = false) Boolean isPrimary)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.uploadDriverProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | DriverProfileControllerTest.java | implemented; existing route |
| GET | `/api/v1/profiles/drivers/{userId}/images` | `getDriverImages( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId)` -> `ResponseEntity<ApiResponse<List<ImageResponse>>>` | ProfileService.getDriverProfileImages | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | DriverProfileControllerTest.java | implemented; existing route |
| PUT | `/api/v1/profiles/drivers/{userId}/images/{imageId}/primary` | `setPrimaryDriverImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.setPrimaryDriverProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/profiles/drivers/{userId}/images/{imageId}` | `deleteDriverImage( @AuthenticationPrincipal Long authenticatedUserId, @PathVariable Long userId, @PathVariable Long imageId)` -> `ResponseEntity<ApiResponse<Void>>` | ProfileService.deleteDriverProfileImage | authenticated; ProfileService.ensureOwnershipOrAdmin; ProfileService.ensureRole | DriverProfileControllerTest.java | implemented; existing route |

### LogisticsCompatibilityController

Source: `src/main/java/com/kilivana/backend/logistics/controller/LogisticsCompatibilityController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/logistics/jobs/{jobId}/tracking` | `getTracking( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId)` -> `ResponseEntity<ApiResponse<List<TrackingEvent>>>` | LogisticsService.getTrackingHistory | authenticated; @resourceAuthorization.jobRead(#jobId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/logistics/jobs/{jobId}/proof-of-delivery` | `submitProof( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId, @RequestBody ProofOfDelivery proof, @Parameter(description = "Six-digit delivery handover code sent to the buyer; required while the job still has an unverified delivery code", example = "123456") @RequestParam(required = false) String otp)` -> `ResponseEntity<ApiResponse<ProofOfDelivery>>` | LogisticsService.createProofOfDelivery | authenticated; @resourceAuthorization.jobWrite(#proof.logisticsJobId) | no direct handler test identified | implemented; existing route |

### LogisticsJobController

Source: `src/main/java/com/kilivana/backend/logistics/controller/LogisticsJobController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/logistics/jobs` | `createJob(@RequestBody LogisticsJob job)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.createJob | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/jobs/{id}` | `getJobById( @Parameter(description = "ID of the delivery job") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.getJobById | authenticated; @resourceAuthorization.jobRead(#id) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/jobs` | `getAllJobs(Pageable pageable)` -> `ResponseEntity<ApiResponse<List<LogisticsJob>>>` | LogisticsService.getAllJobs | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/jobs/driver/{driverId}` | `getJobsByDriver( @Parameter(description = "ID of the driver") @PathVariable Long driverId)` -> `ResponseEntity<ApiResponse<List<LogisticsJob>>>` | LogisticsService.getJobsByDriver | authenticated; @resourceAuthorization.driver(#driverId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/jobs/order/{orderId}` | `getJobsByOrder( @Parameter(description = "ID of the order") @PathVariable Long orderId)` -> `ResponseEntity<ApiResponse<List<LogisticsJob>>>` | LogisticsService.getJobsByOrder | authenticated; @resourceAuthorization.order(#orderId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/jobs/status/{status}` | `getJobsByStatus( @Parameter(description = "Delivery status to filter by, e.g. picked_up or in_transit", example = "in_transit") @PathVariable String status)` -> `ResponseEntity<ApiResponse<List<LogisticsJob>>>` | LogisticsService.getJobsByStatus | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/logistics/jobs/{id}/assign` | `assignDriver( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "ID of the driver to assign") @RequestParam Long driverId)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.assignDriver | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/logistics/jobs/{id}/assign` | `assignDriverPost( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "ID of the driver to assign") @RequestParam Long driverId)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/logistics/jobs/{id}/accept` | `acceptJob( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "ID of the driver accepting the job") @RequestParam Long driverId)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.acceptJob | authenticated; @resourceAuthorization.jobWrite(#jobId) and @resourceAuthorization.driver(#driverId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/logistics/jobs/{id}/accept` | `acceptJobPost( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "ID of the driver accepting the job") @RequestParam Long driverId)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/logistics/jobs/{id}/status` | `updateJobStatus( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "Target delivery status, e.g. picked_up or delivered", example = "delivered") @RequestParam String status)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.updateJobStatus | authenticated; @resourceAuthorization.jobWrite(#jobId) | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/logistics/jobs/{id}/status` | `updateJobStatusPost( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "Target delivery status, e.g. picked_up or delivered", example = "delivered") @RequestParam String status)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | inline | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/logistics/jobs/{id}/otp/verify` | `verifyDeliveryOtp( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @Parameter(description = "Six-digit delivery handover code sent to the buyer", example = "123456") @RequestParam String otp)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.verifyDeliveryOtp | authenticated; @resourceAuthorization.jobWrite(#jobId) | no direct handler test identified | implemented; existing route |
| PUT | `/api/v1/logistics/jobs/{id}/cancel` | `cancelJob( @Parameter(description = "ID of the delivery job") @PathVariable Long id, @RequestBody String cancellationReason)` -> `ResponseEntity<ApiResponse<LogisticsJob>>` | LogisticsService.cancelJob | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/logistics/jobs/{id}` | `deleteJob( @Parameter(description = "ID of the delivery job") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | LogisticsService.deleteJob | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### ProofOfDeliveryController

Source: `src/main/java/com/kilivana/backend/logistics/controller/ProofOfDeliveryController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/logistics/proof-of-delivery` | `createProofOfDelivery( @RequestBody ProofOfDelivery proof, @Parameter(description = "Six-digit delivery handover code sent to the buyer; required while the job still has an unverified delivery code", example = "123456") @RequestParam(required = false) String otp)` -> `ResponseEntity<ApiResponse<ProofOfDelivery>>` | LogisticsService.createProofOfDelivery | authenticated; @resourceAuthorization.jobWrite(#proof.logisticsJobId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/proof-of-delivery/{id}` | `getProofById( @Parameter(description = "ID of the proof of delivery") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<ProofOfDelivery>>` | LogisticsService.getProofById | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/proof-of-delivery/job/{jobId}` | `getProofByJobId( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId)` -> `ResponseEntity<ApiResponse<ProofOfDelivery>>` | LogisticsService.getProofByJobId | authenticated; @resourceAuthorization.jobRead(#jobId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/proof-of-delivery/driver/{driverId}` | `getProofsByDriver( @Parameter(description = "ID of the driver") @PathVariable Long driverId)` -> `ResponseEntity<ApiResponse<List<ProofOfDelivery>>>` | LogisticsService.getProofsByDriver | authenticated; @resourceAuthorization.driver(#driverId) | no direct handler test identified | implemented; existing route |
| DELETE | `/api/v1/logistics/proof-of-delivery/{id}` | `deleteProof( @Parameter(description = "ID of the proof of delivery") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | LogisticsService.deleteProof | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

### TrackingEventController

Source: `src/main/java/com/kilivana/backend/logistics/controller/TrackingEventController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/logistics/tracking-events` | `createTrackingEvent(@RequestBody TrackingEvent event)` -> `ResponseEntity<ApiResponse<TrackingEvent>>` | LogisticsService.addTrackingEvent | authenticated; @resourceAuthorization.jobWrite(#jobId) and @resourceAuthorization.driver(#driverId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/tracking-events/{id}` | `getTrackingEventById( @Parameter(description = "ID of the tracking event") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<TrackingEvent>>` | LogisticsService.getTrackingEventById | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/tracking-events/job/{jobId}` | `getTrackingHistory( @Parameter(description = "ID of the delivery job") @PathVariable Long jobId)` -> `ResponseEntity<ApiResponse<List<TrackingEvent>>>` | LogisticsService.getTrackingHistory | authenticated; @resourceAuthorization.jobRead(#jobId) | no direct handler test identified | implemented; existing route |
| GET | `/api/v1/logistics/tracking-events/driver/{driverId}` | `getTrackingEventsByDriver( @Parameter(description = "ID of the driver") @PathVariable Long driverId)` -> `ResponseEntity<ApiResponse<List<TrackingEvent>>>` | LogisticsService.getTrackingEventsByDriver | authenticated; @resourceAuthorization.driver(#driverId) | no direct handler test identified | implemented; existing route |

### TripsController

Source: `src/main/java/com/kilivana/backend/logistics/controller/TripsController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| GET | `/api/v1/trips/active` | `getActiveTrips()` -> `ResponseEntity<ApiResponse<List<LogisticsJob>>>` | DriverLocationService.getActiveStates; LogisticsService.getAllJobs | authenticated; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |
| POST | `/api/v1/trips/{id}/tracking/start` | `startTracking( @Parameter(description = "ID of the trip to start tracking") @PathVariable Long id)` -> `ResponseEntity<ApiResponse<Void>>` | DriverLocationService.getActiveStates; LogisticsService.acceptJob; LogisticsService.getJobById | authenticated; @resourceAuthorization.jobRead(#id); @resourceAuthorization.jobWrite(#jobId) and @resourceAuthorization.driver(#driverId) | no direct handler test identified | implemented; existing route |

### MailAdminController

Source: `src/main/java/com/kilivana/backend/mail/MailAdminController.java`

| HTTP | Endpoint | Handler / request -> response | Service/repository | Role / checks | Test source evidence | Implementation |
|---|---|---|---|---|---|---|
| POST | `/api/v1/admin/mail/test` | `sendTestEmail( @RequestBody TestEmailRequest request)` -> `ResponseEntity<ApiResponse<Map<String, Object>>>` | SendGridMailService.send; SendGridProperties.getFromEmail; SendGridProperties.isConfigured | ADMIN; URL rule; resource checks, if any, are inline | no direct handler test identified | implemented; existing route |

## Additional implementation gaps

- ProductRequest accepts farmId, qualityGrade, harvestDate and lowStockThreshold, but existing create/update mapping does not persist them; response farmName remains a TODO. No inferred farm association or certification policy added.
- Generic farm update/delete and other unrelated Admin APIs retain existing semantics; a static inventory is not a penetration-test certification of all 311 mappings.
- See FARMER_PRODUCT_ADMIN_FARM_API_CHANGES.md for the final lifecycle, image transaction behavior, all affected endpoints and frontend work.

## Structure, schema, tests and documentation reviewed

All Java sources and SQL migration files were read for structural inventory. No environment/credential file read. Migration files are manually managed, including guarded review-only V16–V18; no Flyway dependency in pom. A new lifecycle migration must be reviewed/applied separately before deployment; never replay old data-normalizing migrations. Swagger uses springdoc annotations and global bearer scheme. Compatibility controllers intentionally alias carts, seller products, logistics and auth paths; multipart/JSON overloads share paths by consumes. WebSocket handlers are not REST APIs.

## Read/reuse manifest

- `db/demo/short_distance_order.sql` — API/domain/schema inspection
- `db/migrations/V10__order_status_placed.sql` — API/domain/schema inspection
- `db/migrations/V11__align_order_and_product_states.sql` — API/domain/schema inspection
- `db/migrations/V12__order_reference_and_line_details.sql` — API/domain/schema inspection
- `db/migrations/V13__inspector_specialization.sql` — API/domain/schema inspection
- `db/migrations/V14__driver_location_tracking.sql` — API/domain/schema inspection
- `db/migrations/V15__daraja_payment_fields.sql` — API/domain/schema inspection
- `db/migrations/V16__reconciled_tracking_schema.sql` — API/domain/schema inspection
- `db/migrations/V17__reconciled_product_payment_fields.sql` — API/domain/schema inspection
- `db/migrations/V18__reconciled_payment_checks_order_item_fk.sql` — API/domain/schema inspection
- `db/migrations/V2__add_super_admin_role.sql` — API/domain/schema inspection
- `db/migrations/V3__proof_of_delivery_optional_fields.sql` — API/domain/schema inspection
- `db/migrations/V4__one_proof_of_delivery_per_job.sql` — API/domain/schema inspection
- `db/migrations/V5__backfill_reference_codes.sql` — API/domain/schema inspection
- `db/migrations/V6__unique_email_case_insensitive.sql` — API/domain/schema inspection
- `db/migrations/V7__driver_profile_admin_fields.sql` — API/domain/schema inspection
- `db/migrations/V8__single_admin_role.sql` — API/domain/schema inspection
- `db/migrations/V9__kenyan_payment_methods.sql` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AddressController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminBuyerController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminDriverController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminFarmerController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminInspectorController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminOperationsController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminProductController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AdminSupplierController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/AuditLogController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/InspectionController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/NotificationController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/ProfileController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/UserController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/controller/UserProfileController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/AddressRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/AddressResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/AdminBuyerResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/AdminDriverResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/AdminSupplierResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/AuditLogResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/BuyerProfileRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/BuyerProfileResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/DashboardStatsResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/DriverCapacityFormatter.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/DriverProfileRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/DriverProfileResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/DriverRegistrationRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/FarmerProfileRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/FarmerProfileResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/InspectionRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/InspectionResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/InspectorProfileRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/InspectorProfileResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/NotificationResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/SupplierProfileRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/SupplierProfileResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/SupplierRegistrationRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/UserProfileUpdateRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/UserRegistrationRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/dto/UserResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/Address.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/AuditLog.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/BuyerProfile.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/DriverProfile.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/DriverProfileImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/FarmerProfile.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/FarmerProfileImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/Inspection.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/InspectionEvidenceImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/InspectorProfile.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/InspectorProfileImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/Notification.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/SupplierProfile.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/SupplierProfileImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/entity/User.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/AddressRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/AuditLogRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/BuyerProfileRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/DriverProfileImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/DriverProfileRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/FarmerProfileImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/FarmerProfileRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/InspectionEvidenceImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/InspectionRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/InspectorProfileImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/InspectorProfileRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/NotificationRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/SupplierProfileImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/SupplierProfileRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/repository/UserRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/AddressService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/AdminBuyerService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/AdminDriverService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/AdminSupplierService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/AuditLogService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/DashboardService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/InspectionService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/NotificationService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/ProfileService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/admin/service/UserService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/controller/AuthController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/controller/HealthController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/controller/PublicFarmerController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/controller/RegionController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/controller/StoredImageController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/dto/ApiResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/dto/AuthLoginRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/dto/AuthTokenResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/dto/ImageResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/dto/PasswordResetRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/dto/RefreshTokenRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/entity/BaseEntity.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/entity/BaseImageEntity.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/entity/PasswordResetToken.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/entity/StoredImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/CropCategory.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/CropStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/DeliveryStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/DisputeStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/DocumentStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/DocumentType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/DriverStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/EnumNameMatcher.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/FarmStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/InspectionResult.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/InspectionStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/KycStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/OrderStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/OwnershipType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/PaymentMethod.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/PaymentStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/ProductStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/QualityGrade.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/SellerType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/UserRole.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/UserStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/VehicleType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/enums/VerificationStatus.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/BadRequestException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/ConflictException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/ForbiddenException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/GlobalExceptionHandler.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/ResourceNotFoundException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/ServiceUnavailableException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/TooManyRequestsException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/exception/UnauthorizedException.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/repository/ImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/repository/PasswordResetTokenRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/repository/StoredImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/AuthService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/DatabaseImageStorage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/DatabaseImageStorageProvider.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/ImageStorage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/KenyaCounty.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/KenyanIdType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/LocalImageStorage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/common/service/UserReferenceCodeGenerator.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/CorsConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/DarajaProperties.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/DatabaseUrlEnvironmentPostProcessor.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/DevelopmentDataSeeder.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/DotenvEnvironmentPostProcessor.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/IntegrationReadinessLogger.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/LocalStorageWebConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/OpenApiConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/RequestLoggingFilter.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/SecurityConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/SeedDataRunner.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/config/WebClientConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/dto/DarajaCallbackRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/dto/DarajaCallbackResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/dto/PublicFarmerProfileResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/dto/StkPushInitiateRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/dto/StkPushInitiateResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/dto/StkPushResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/CartCompatibilityController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/CartController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/CatalogCompatibilityController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/CategoryController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/CropTypeController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/DarajaCallbackController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/DisputeController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/EcommerceNotificationController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/OrderController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/PaymentController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/controller/ProductController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/CartItemRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/CartResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/CategoryRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/CategoryResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/DisputeRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/DisputeResolutionRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/DisputeResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderItemRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderItemResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/OrderResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/PaymentRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/PaymentResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/ProductImageResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/ProductRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/dto/ProductResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/Cart.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/CartItem.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/Category.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/CategoryImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/Dispute.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/Order.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/OrderEvent.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/OrderItem.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/Payment.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/Product.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/entity/ProductImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/enums/EcommerceNotificationType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/CartItemRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/CartRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/CategoryImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/CategoryRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/DisputeRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderEventRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderItemRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/OrderRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/PaymentRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/ProductImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/repository/ProductRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/CartService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/CategoryService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/DisputeService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/EcommerceNotificationService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/OrderService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/PaymentService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/ecommerce/service/ProductService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/controller/CropController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/controller/FarmController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/controller/FarmerFarmController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/controller/KycController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/CropResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/CropSummaryDto.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/CropTypeDto.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/FarmerListItem.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/FarmerRegistrationRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/FarmerResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/FarmImageResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/FarmRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/FarmResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/dto/KycDocumentResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/entity/Crop.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/entity/CropType.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/entity/Farm.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/entity/FarmImage.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/entity/KycDocument.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/repository/CropRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/repository/CropTypeRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/repository/FarmImageRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/repository/FarmRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/repository/KycDocumentRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/farm/service/AdminFarmerService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/KilivanaBackendApplication.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/config/TrackingConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/config/TrackingProperties.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/config/WebSocketConfig.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/DriverLocationController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/DriverProfileController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/LogisticsCompatibilityController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/LogisticsJobController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/ProofOfDeliveryController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/TrackingEventController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/TrackingWebSocketController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/controller/TripsController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/dto/DriverLocationResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/dto/LocationEvent.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/dto/LocationUpdateRequest.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/dto/LocationUpdateResponse.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/dto/TrackingStoppedEvent.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/entity/DriverLatestLocation.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/entity/DriverLocation.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/entity/LogisticsJob.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/entity/ProofOfDelivery.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/entity/TrackingEvent.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/repository/DriverLatestLocationRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/repository/DriverLocationRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/repository/LogisticsJobRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/repository/ProofOfDeliveryRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/repository/TrackingEventRepository.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/DriverLocationService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/LocationRateLimiter.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/LocationValidator.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/LogisticsService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/TrackingAuthorizationService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/TrackingBroadcastService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/TrackingChannelInterceptor.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/logistics/service/TrackingStopService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/mail/DeliveryOtpNotifier.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/mail/DriverOtpEmailTemplate.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/mail/MailAdminController.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/mail/SendGridBootstrapValidator.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/mail/SendGridMailService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/mail/SendGridProperties.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/AccountAccessValidator.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/CurrentUser.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/JwtAuthenticationFilter.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/JwtHandshakeInterceptor.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/JwtProperties.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/JwtService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/ProductVisibility.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/ResourceAuthorization.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/security/RestAuthenticationErrorHandler.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/service/DarajaCallbackService.java` — API/domain/schema inspection
- `src/main/java/com/kilivana/backend/service/DarajaService.java` — API/domain/schema inspection
- `src/test/java/com/kilivana/backend/admin/controller/AdminFarmerControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/admin/controller/ProfileControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/admin/dto/DriverCapacityFormatterTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/admin/dto/DriverEnumParsingTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/admin/service/UserServiceUsernameRegionTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/controller/AuthControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/controller/HealthControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/controller/PublicFarmerControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/enums/EnumWireFormatTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/enums/PaymentMethodTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/service/AuthServiceEmailNormalizationTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/service/KenyaLocaleTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/service/PasswordResetFlowTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/common/service/PasswordResetSecurityTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/config/BootstrapConfigTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/config/DatabaseUrlEnvironmentPostProcessorTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/config/RequestLoggingFilterTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/ecommerce/service/DisputeServiceTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/ecommerce/service/OrderServiceTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/ecommerce/service/PaymentCallbackTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/ecommerce/service/PaymentServiceTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/ecommerce/service/ProductStockSafetyTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/farm/controller/FarmerFarmControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/logistics/controller/DriverProfileControllerTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/logistics/service/DeliveryOtpFlowTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/logistics/service/LocationValidatorTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/logistics/service/LogisticsServiceTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/logistics/service/TrackingChannelInterceptorTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/logistics/service/TrackingDestinationTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/mail/SendGridMailServiceTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/repository/OptionalFilterQueryTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/security/AccountAccessValidatorTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/security/CatalogVisibilityTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/security/JwtAccountEnforcementTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/security/JwtHandshakeAccountTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/security/LocalProductImageVisibilityTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/service/DarajaServiceTest.java` — test coverage inspected
- `src/test/java/com/kilivana/backend/support/JwtTestSupport.java` — test coverage inspected

## Framework and non-REST surfaces

Spring MVC also supplies HEAD for GET mappings and OPTIONS; SecurityConfig explicitly permits preflight. Actuator health/info, Swagger UI, and springdoc `/v3/api-docs` (and configured aliases) are framework endpoints, not additional application controllers. TrackingWebSocketController uses STOMP message mappings; WebSocket handshake paths are public but per-subscription/job authorization is handled by the tracking authorization components. These are not HTTP product mutation routes.

## Final validation pointer

The implementation report contains executed Maven commands and per-suite counts, including five tests against a new disposable PostgreSQL 16 database. No production migration or frontend validation occurred. Original pre-change findings above are historical; the final integration contract describes what was implemented.
