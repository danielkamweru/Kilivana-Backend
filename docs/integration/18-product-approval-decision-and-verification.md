# Farmer product approval — agreed decision and verification

2026-10-15, C HEAD `8f9dded`. **AGREED: every farmer-submitted product requires administrator approval before public visibility.** This replaces the historical PENDING TEAM DECISION statements in earlier integration reports, including report16. Historical reports and approved migration checksums remain unchanged; they describe their original checkpoints. No products, statuses, approval transitions or database rows were changed in this audit.

Farmer verification and product approval are separate decisions. An inspector's finding is not administrator approval of either a farmer or a product. Whether editing an already approved product requires reapproval remains **PENDING TEAM DECISION**.

## Verification results

| Required behavior | Code and observed evidence | Result |
|---|---|---|
| New farmer product starts PENDING_APPROVAL | ProductService.createProduct sets the enum explicitly. Report16 actual create/edit fixture stayed pending. ProductStatus serializes it as lowercase `pending`, not `PENDING_APPROVAL`. | Implemented and previously HTTP-tested |
| Public list excludes pending | ProductService.getAllProducts uses ACTIVE plus positive stock; current anonymous GET /api/v1/products returned200 with0 records while pending listings exist | Implemented; current positive filtering result, not an approved-product positive fixture |
| Public category/search excludes pending | getProductsByCategory filters ACTIVE; ProductRepository.searchProducts SQL requires ACTIVE. Earlier PostgreSQL search tests passed | Source verified; no new category/search fixture mutation or approval test |
| Public detail hides pending | GET /api/v1/products/{id} -> getProductById -> unfiltered findById | **FAIL: current anonymous GET200 returned status `pending`** |
| Public seller catalog hides pending | GET /api/v1/products/seller/{sellerId} -> getProductsBySeller -> unfiltered findBySellerId | **FAIL: current anonymous GET200 returned the pending fixture** |
| Compatibility seller route is scoped | CatalogCompatibilityController /api/v1/sellers/{sellerId}/products calls same unfiltered service; authentication fallback does not establish seller ownership | Source-confirmed exposure to unrelated authenticated users; not separately exercised live |
| Farmer can retrieve own pending | Same seller/detail route currently permits it; report16 authenticated refresh/listing read passed | Working, but currently overbroad. Fix must preserve owner/admin pending access and existing Android wire contract |
| Only admin changes approval status | ProductService.updateProductStatus and updateProductStatusWithReason use @PreAuthorize admin; /api/v1/admin namespace ADMIN-only; UserRole.isStaff means ADMIN only | Source verified; prior own-product escalation403. Positive admin approve/reject not exercised during this audit because it would mutate records |
| Pending image metadata is hidden publicly | GET /api/v1/products/{id}/images does not enforce product visibility | **FAIL: current anonymous GET200 returned2 image entries** |
| Pending asset bytes are private | StoredImageController /api/v1/images/{publicId} retrieves by numeric ID without checking linked product/status/owner and adds public30-day caching; /uploads/** is public; Cloudinary URLs may be public too | Source-confirmed gap; report16 anonymous retrieval of original pending images succeeded. A product DTO filter alone cannot fix asset access |

Current GET-only check also returned200 from /api/v1/health. No new records or uploads were created. Fixture IDs and response payloads containing user data are omitted. Existing pending products were left untouched.

## Additional authorization finding

SecurityConfig.PUBLIC_ENDPOINTS interleaves strings such as `"GET", "/api/v1/categories/**"`, but passes the entire String[] to `.requestMatchers(PUBLIC_ENDPOINTS).permitAll()`. This overload matches paths; it does not pair an HTTP method with the next path. Consequently public path wildcards apply to writes too. CategoryController POST/PUT/status/DELETE and CategoryService have no equivalent method guards, so category mutations lack the intended authorization. Product methods have some independent ownership/admin guards; do not assume the same defect permits every product operation.

This is verified by source, not an anonymous destructive request. A future fix must use HttpMethod matchers and explicit mutation roles, then test anonymous, buyer, unrelated farmer, owner, assigned inspector and admin behavior. Review other wildcarded reference-data paths too. No writes were attempted to prove this issue.

## Proposed focused correction, not implemented

1. Apply one visibility predicate to detail, lists, seller aliases and image metadata: public only approved ACTIVE; owner/admin can read their authorized pending records. Preserve current owner seller-list envelope so Android continues displaying pending listings. Add /mine only if necessary and coordinated; do not substitute the general order feed.
2. Decide an asset strategy for DB/local/Cloudinary: authenticated owner/admin delivery for pending/private evidence, signed short-lived URLs or private storage, approved public assets, appropriate cache invalidation. Existing Android image transport intentionally sends no bearer token; coordinate any private-image contract change with Android and web/admin clients. Never send bearer tokens to external image hosts.
3. Restrict public matchers by HTTP method and guard category mutations. Keep legitimate admin capabilities and existing status values.
4. Add endpoint tests proving anonymous/unrelated users cannot retrieve pending detail/catalog/image metadata/bytes; owner/admin still can; admins alone approve/reject; newly approved listings become public; rejected/suspended do not. Assert no accidental pending-product autoapproval and retain negative escalation tests. Run against isolated fixtures, not existing user products.

## Decisions still required

- Approved product edits: which fields/images trigger reapproval; whether old approved content stays public during review; stock/price-only edits; audit trail and versioning. Current update retains ACTIVE and publishes changes immediately. Do not silently change this before agreement.
- Safe farmer withdrawal/restoration: distinguish owner withdrawal from admin suspension; never restore directly to approved visibility by accident.
- Relationship between farmer verification and listing/purchasing eligibility. Current seller authorization allows ACTIVE or PENDING_VERIFICATION accounts, product creation is pending, and administrator order creation requires ACTIVE products without an independent seller-verification gate. Buyer checkout is disabled. Thus unverified farmers can create pending listings; existing admin approval can make a product ACTIVE without a farmer-verification prerequisite. This does not establish that production checkout for such farmers works. No new eligibility rule is inferred from the product-approval decision.

These are release/security blockers for public catalog exposure, not a demand to implement all future backend modules before an honest review PR. Daniel must receive this gap explicitly. Farmer demo work can continue locally while remediation and ownership are agreed.
