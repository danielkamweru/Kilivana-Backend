# Farmer product and Admin farm API changes

Branch: `feature/supplier-api-updates`; baseline: `bf61030`. All URLs below are relative to the backend origin. No new endpoints or request DTOs were added. Existing controllers and storage providers are reused. The Android and Admin Portal sources were not modified. No Admin Portal source was available in the sibling project inventory, so UI behavior is not claimed as verified.

## Authentication, wire format and errors

Send `Authorization: Bearer <accessToken>` for mutations and owner/private reads. The authenticated principal, not a submitted seller/user ID, establishes identity. FARMER/SUPPLIER owners and ADMIN may modify products; INSPECTOR, BUYER, DRIVER and unrelated sellers may not. ADMIN moderation remains separate. Farm self-service requires FARMER, including pending-verification accounts permitted by the existing account policy.

Product enum responses remain lowercase: `active`, `pending`, `rejected`, `suspended`, with additive `unlisted`, `archived`, `deleted`. Query parameters use Java enum constants: `ACTIVE`, `PENDING_APPROVAL`, `UNLISTED`, `ARCHIVED`; `pending` is the JSON spelling, not a guaranteed query-param spelling. The existing Admin PATCH parses uppercase enum names from its JSON map.

All product responses retain `ApiResponse<ProductResponse>` and existing image response fields. Errors retain the current envelope: `success:false`, `message`, `data:null`, `timestamp`, `error:{code,details}`. Invalid lifecycle/file/foreign-image requests return 400; unauthenticated mutations 401; unauthorized ownership/role 403; missing/deleted owner product or missing image 404; upload I/O failure 500. Servlet multipart limit violations return 413. No response DTO fields were removed.

## Existing endpoints changed or affected

| HTTP / URL | Controller -> service/repository | Request | Success response / status | Authorization and integration impact |
|---|---|---|---|---|
| PUT `/api/v1/products/{id}/status?status=UNLISTED` | ProductController -> ProductService.updateSellerProductStatus | No body; required ProductStatus query | ApiResponse<ProductResponse>, 200 | Owner FARMER/SUPPLIER or ADMIN; seller state machine below; enables Unlist/Archive/Submit for review actions |
| DELETE `/api/v1/products/{id}` | ProductController -> ProductService.deleteProduct | No body | ApiResponse<Void>, 200 | Owner seller or ADMIN; requires ARCHIVED and zero reserved stock; soft deletion; repeat owner request 404 |
| PUT `/api/v1/products/{id}` multipart | ProductController -> ProductService.updateProductWithImages | `product` JSON ProductRequest; optional repeated `images` binary parts | ApiResponse<ProductResponse>, 200 | Owner seller or ADMIN; supplied images append and existing images are preserved; explicit removal/replacement uses image-ID routes |
| PUT `/api/v1/products/{id}` JSON | ProductController -> ProductService.updateProduct | ProductRequest JSON | ApiResponse<ProductResponse>, 200 | Added service-boundary ownership and terminal-deletion guard; no image changes |
| POST `/api/v1/products` multipart | ProductController -> ProductService.createProductWithImages | `product` ProductRequest; optional `images` | ApiResponse<ProductResponse>, 201 | Authenticated seller; same payload; invalid files/count and failed uploads roll back product and image metadata |
| POST `/api/v1/products/{id}/images` | ProductController -> ProductService.uploadProductImage | multipart `image`; optional `sortOrder`, `isPrimary` query | ApiResponse<ProductImageResponse>, 201 | Owner seller or ADMIN; locked product, count/type/size/signature validation |
| PUT `/api/v1/products/{productId}/images/{imageId}` | ProductController -> ProductService.updateProductImage | multipart `image`; optional `sortOrder`, `isPrimary` query | ApiResponse<ProductImageResponse>, 200 | Owner seller or ADMIN; verifies image belongs to product; upload succeeds before old asset is removed |
| DELETE `/api/v1/products/{productId}/images/{imageId}` | ProductController -> ProductService.deleteProductImage | No body | ApiResponse<Void>, 200 | Owner seller or ADMIN; only server-stored publicId selects storage asset; wrong product 400; repeat 404 |
| PUT `/api/v1/products/{productId}/images/{imageId}/primary` | ProductController -> ProductService.setPrimaryImage | No body | ApiResponse<Void>, 200 | Owner seller or ADMIN; service boundary and product lock added |
| PUT `/api/v1/products/{productId}/images/reorder` | ProductController -> ProductService.reorderImages | JSON array of image IDs, e.g. `[8,7]` | ApiResponse<Void>, 200 | Owner seller or ADMIN; partial orders remain supported; duplicate or foreign IDs now reject with 400 |
| GET `/api/v1/products/seller/{sellerId}` and `/api/v1/sellers/{sellerId}/products` | ProductController / CatalogCompatibilityController -> ProductService.getProductsBySeller | No body | ApiResponse<List<ProductResponse>>, 200 | Owner/Admin sees nondeleted management states; others see ACTIVE only. Compatibility seller route still requires authentication; canonical product seller GET remains public |
| GET `/api/v1/products`, `/api/v1/products/search`, `/api/v1/products/category/{categoryId}` | ProductController -> ProductService / ProductRepository | Existing filters | List or Page<ProductResponse> in envelope, 200 | Public; ACTIVE-only predicates already existed and exclude new nonpublic states |
| GET `/api/v1/products/{id}`, `/api/v1/products/{id}/images` | ProductController -> ProductService.getProductById / ProductVisibility | No body | ProductResponse or List<ProductImageResponse>, 200 | Public ACTIVE; nonpublic owner/Admin access; DELETED hidden even from direct owner detail (404) |
| GET `/api/v1/images/{publicId}`, `/uploads/**` | StoredImageController / LocalStorageWebConfig -> ProductVisibility | No body | Image bytes, 200 | Existing linked-product visibility applies; supply bearer header for owner nonpublic images; deleted assets removed/hidden |
| GET `/api/v1/admin/products` | AdminProductController -> ProductService.getFilteredProducts / ProductRepository | Existing filters | ApiResponse<Page<ProductResponse>>, 200 | ADMIN; hides DELETED by default; explicit `status=DELETED` allows retained-row inspection |
| PATCH `/api/v1/admin/products/{id}/status` | AdminProductController -> ProductService.updateProductStatusWithReason | `{"status":"ACTIVE","reason":"Approved"}` | ApiResponse<ProductResponse>, 200 | ADMIN only; cannot revive a DELETED product or use status=DELETED to bypass deletion cleanup; current ACTIVE/reject/suspend workflow retained |
| GET `/api/v1/admin/farms` | FarmController -> FarmRepository.searchFarms -> searchFarmsByStatusName | Optional farmerId, county, status, search; Pageable | ApiResponse<{items,page,size,totalItems,totalPages}>, 200 | ADMIN; native status parameter now binds enum NAME; existing FarmResponse includes name and farmerId |
| POST/GET `/api/v1/farmers/me/farms`; GET/PUT `/api/v1/farmers/me/farms/{id}` | FarmerFarmController -> FarmRepository / FarmImageRepository | POST/PUT FarmRequest; GET no body | ApiResponse<FarmResponse>, 201 POST / 200 PUT or detail; list is List<FarmResponse> | FARMER role now enforced at class boundary; existing ownership checks remain; no fake farm creation on account approval |

No new endpoints. All affected requests also retain framework validation/unsupported-media/method error handling. Admin product statistics currently count retained rows in total; live/pending metrics remain status-filtered. This report does not redefine that existing total metric.

## Seller product lifecycle

| Current state | Requested state | Result |
|---|---|---|
| ACTIVE | UNLISTED | Hide from public marketplace; preserve all product data and images |
| ACTIVE / UNLISTED / PENDING_APPROVAL / REJECTED | ARCHIVED | Retain record for management; allow subsequent deletion if no reservations |
| UNLISTED / ARCHIVED | PENDING_APPROVAL | Submit for Admin review; remains private |
| Any seller state | ACTIVE / SUSPENDED / REJECTED / DELETED | Rejected (400); seller cannot approve, moderate, or bypass deletion |
| SUSPENDED | Any seller transition | Rejected (400); Admin resolves moderation first |
| DELETED | Any mutation or owner detail read | Terminal; 404 |

Same-state seller transitions are invalid (400). Re-listing never directly returns a seller product to ACTIVE. ADMIN retains its existing generic status route behavior as well as the dedicated Admin PATCH. Admin status setters reject DELETED as a destination and cannot resurrect deleted rows. Existing product edits do not change approval status: whether editing an ACTIVE product requires reapproval remains the documented pre-existing policy decision.

Examples (no request body):

```http
PUT /api/v1/products/42/status?status=UNLISTED
Authorization: Bearer <farmer-access-token>

PUT /api/v1/products/42/status?status=ARCHIVED
Authorization: Bearer <farmer-access-token>

PUT /api/v1/products/42/status?status=PENDING_APPROVAL
Authorization: Bearer <farmer-access-token>
```

Exact representative ProductResponse envelope after unlisting (fixture values only; timestamps/IDs vary):

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 42,
    "sellerId": 4,
    "sellerType": "farmer",
    "categoryId": 1,
    "name": "Tomatoes",
    "description": "Fresh tomatoes",
    "unit": "kg",
    "price": 120.0,
    "stockQty": 10,
    "reservedQty": 0,
    "soldQty": 2,
    "minimumOrderQty": 1,
    "status": "unlisted",
    "qualityGrade": null,
    "harvestDate": null,
    "lowStockThreshold": null,
    "farmId": null,
    "statusReason": null,
    "statusChangedAt": "2026-10-21T12:00:00",
    "statusChangedBy": 4,
    "verifiedAt": null,
    "verifiedBy": null,
    "verifiedQty": null,
    "lastInspectionId": null,
    "createdAt": "2026-10-20T12:00:00",
    "updatedAt": "2026-10-21T12:00:00",
    "images": [
      {
        "id": 7,
        "url": "/api/v1/images/100",
        "publicId": "100",
        "assetId": null,
        "sortOrder": 0,
        "isPrimary": true,
        "createdAt": "2026-10-20T12:00:00"
      }
    ],
    "sellerName": "Example Farmer",
    "sellerPhone": "0700000000",
    "sellerCounty": "Kiambu",
    "categoryName": "Vegetables",
    "farmName": null,
    "farmCertified": false,
    "isLive": false
  },
  "timestamp": "2026-10-21T12:00:00",
  "error": null
}
```

Product deletion request and response:

```http
DELETE /api/v1/products/42
Authorization: Bearer <farmer-access-token>
```

```json
{
  "success": true,
  "message": "Product deleted successfully",
  "data": null,
  "timestamp": "2026-10-21T12:00:00",
  "error": null
}
```

A published product returns 400 until archived. Reservations must be resolved before deletion. Historical order lines retain productId, productName and price snapshots; payments continue to reference their orders. No order, payment, inspection, cart or product-history row is hard-deleted. There is no review entity/API in this baseline; retaining product IDs avoids dangling references for external historical consumers. No restore or purge endpoint was added.

## Android product-image workflow

1. Load owner product detail or GET `/api/v1/products/42/images`. Keep image record IDs; `publicId` and URL are display/storage metadata, not deletion input.
2. To add, POST `/api/v1/products/42/images?sortOrder=1&isPrimary=false` with multipart part `image`. The response contains the new ProductImageResponse.
3. To replace, PUT `/api/v1/products/42/images/7` with multipart `image`; optional sortOrder/isPrimary. The existing image record ID remains, while URL/publicId change.
4. To remove, DELETE `/api/v1/products/42/images/7`. Only that product's image can be removed. Never send a URL to delete an asset.
5. To save product fields, JSON PUT the existing ProductRequest, or multipart PUT part `product` with additional `images`. Omitted images remain. Empty file parts reject; do not send placeholder binaries. Refresh product detail after saves to render the final image collection.
6. Primary selection and reorder routes remain available. Client can select a remaining primary after deleting the previous primary. Partial reorder arrays remain supported; unknown/duplicate IDs reject.

Example JSON ProductRequest (also the `product` multipart part with `Content-Type: application/json`):

```json
{"categoryId":1,"name":"Tomatoes","description":"Fresh tomatoes","unit":"kg","price":120.00,"stockQty":10,"minimumOrderQty":1}
```

Add-image example:

```http
POST /api/v1/products/42/images?sortOrder=1&isPrimary=false
Authorization: Bearer <farmer-access-token>
Content-Type: multipart/form-data; boundary=example

--example
Content-Disposition: form-data; name="image"; filename="tomatoes.png"
Content-Type: image/png

<PNG binary bytes>
--example--
```

Image response (replace returns 200 with the same shape; upload returns 201):

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 7,
    "url": "/api/v1/images/100",
    "publicId": "100",
    "assetId": null,
    "sortOrder": 0,
    "isPrimary": true,
    "createdAt": "2026-10-20T12:00:00"
  },
  "timestamp": "2026-10-21T12:00:00",
  "error": null
}
```

Validation: maximum 10 attached images per product; JPEG, PNG, GIF, WebP only; nonempty file, allowed MIME and matching file signature; at most 10 MiB per image. This checks signatures, not a full malware/content scan. Existing servlet configuration also caps the entire multipart request at 10 MB, so upload large images individually or reduce size. Existing products above the count cap may replace/remove images, but cannot add until below the cap.

Product locks serialize image count and mutation operations. Checked IOException now rolls back the entire product transaction, including fields and image links. PostgreSQL stored image bytes share the transaction. Local new uploads are removed on rollback; removal/replacement of old local files occurs after commit; partially copied local files are deleted on I/O failure. The existing local provider logs filesystem cleanup errors. Abrupt process failure or OS deletion denial can still leave a local orphan; a durable cleanup queue/reconciliation job is not introduced in this change. Production storage remains the PostgreSQL provider.

Each image HTTP operation commits independently. There is no new single atomic endpoint for a staged mixed remove/replace/add batch. An image operation failure leaves its original data intact; earlier successful independent requests remain saved. Android must track per-operation success, retry deliberately and refresh the final list. The multipart product edit transaction is atomic for its fields and appended files.

## Admin Farms visibility and onboarding

The actual model is account -> optional farmer_profiles -> explicit farms. A profile farmName does not create a Farm. Account approval modifies user verification status only. The list reads farms directly; it intentionally includes associated farms for pending and approved Farmers. FarmStatus ACTIVE/FALLOW/INACTIVE describes farm use, not Farmer verification. This policy is preserved and tested before/after user verification.

Create a real farm through the existing Farmer route after collecting real farm data:

```http
POST /api/v1/farmers/me/farms
Authorization: Bearer <farmer-access-token>
Content-Type: application/json

{"name":"Green Valley Farm","county":"Kiambu","subCounty":"Thika","sizeAcres":2.5,"ownershipType":"OWNED","description":"Vegetable farming"}
```

The backend assigns farmerId from the token and status ACTIVE. It returns 201 with FarmResponse. Repeated POST intentionally creates another farm (a Farmer may own several); onboarding must fetch existing farms and resume/edit instead of replaying creation blindly. Approval and listing never create or duplicate farms.

```http
GET /api/v1/admin/farms?farmerId=4&status=ACTIVE&search=Green&page=0&size=20&sort=id,asc
Authorization: Bearer <admin-access-token>
```

```json
{"success":true,"message":"Success","data":{"items":[{"id":9,"farmerId":4,"name":"Green Valley Farm","county":"Kiambu","subCounty":"Thika","address":null,"latitude":null,"longitude":null,"sizeAcres":2.5,"ownershipType":"OWNED","status":"ACTIVE","description":null,"images":[],"crops":null,"createdAt":"2026-10-21T12:00:00"}],"page":1,"size":20,"totalItems":1,"totalPages":1},"timestamp":"2026-10-21T12:00:00","error":null}
```

Farm list DTO is intentionally a summary; the detail route contains fuller fields/images. Input page is zero-based Spring Pageable; response page metadata is one-based, as before. Search covers name/county/subCounty case-insensitively. County is currently an exact match. Farm status filter uses enum names, bound as text to native SQL. FarmerId scopes both result and count. Pagination and status/search combinations were tested on disposable PostgreSQL.

No live records were inspected, so this identifies source-level causes rather than asserting the state of a particular production Farmer. If an associated valid farm exists, it already satisfies the roster query without account-approval filtering; the status binding defect could break filtered requests. If only farmer_profiles exists, the explicit farm step is missing. If an existing farm points at the wrong user, an association repair needs verified ownership evidence.

Safe repair strategy: obtain a separately authorized read-only report of accounts, farmer_profiles and farms; classify missing farms versus incorrect associations; verify actual farm name, county, size and ownership with the Farmer; use the existing authenticated creation/edit flow or a reviewed targeted repair keyed by immutable IDs. Recheck for an existing associated farm before creation. Do not infer farm data from personal names, fuzzy-match owners, run blind bulk backfills or alter production data from this task. No repair migration was automatically generated.

## Migration and rollout

`db/migrations/V19__seller_product_lifecycle.sql` expands products_status_check to retain the four existing states plus UNLISTED/ARCHIVED/DELETED. No data rewrite, destructive row operation or new column. Existing migrations are manual; this project does not have Flyway enabled. Review the target schema and execute V19 separately before deploying this code. The DDL was exercised in a fresh disposable PostgreSQL fixture transaction, not on any live database. Do not replay V11, which rewrites unfamiliar statuses. Do not run the old guarded V16-V18 review scripts as part of this change.

No farm migration is required for the query fix. Association repair remains evidence-driven and separately reviewed. Do not downgrade the status constraint once new states exist; rollback requires coordinated application/client handling, not coercing rows into ACTIVE.

Android work still required: add the unlist/archive/delete/resubmit actions; recognize additive status strings; preserve existing image IDs; call explicit image deletion/replacement; attach bearer headers for private image loads; handle image-count validation and multi-request failures. Public marketplace requests need no new route. Supplier payloads/roles remain supported, with the same seller lifecycle policy.

Admin Portal work still required: verify it consumes `data.items`, `name`, `farmerId`, one-based result metadata and zero-based request page correctly; recognize new product statuses; use uppercase existing Admin PATCH status values; represent pending approval separately from farm ACTIVE state; guide profile-only Farmers through real farm creation. No frontend behavior was tested or changed. Inspector endpoints were not modified.

Compatibility considerations: status values are additive and require exhaustive client enum updates; old multipart edits that expected replacement must switch to explicit image ID removal/replacement (preserving files is the required corrected behavior); nonarchived deletion now rejects and deletion is terminal soft deletion; image count and signature checks are stricter; unauthorized product mutation now returns 403; Farmer farm self-service now rejects other roles. No DTO shape or endpoint URL was removed. ProductRequest farmId/qualityGrade/harvestDate/lowStockThreshold fields are pre-existing ignored-write gaps, and ProductResponse farmName is still a pre-existing placeholder; this task does not invent associations or certification semantics.
