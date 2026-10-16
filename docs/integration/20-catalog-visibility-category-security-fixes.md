# Catalog visibility and category authorization fixes

2026-10-15; branch `feature/android-backend-integration`, base HEAD `8f9dded`. Uncommitted, focused implementation of the two approved audit findings. This report supersedes documents17–19 **only for the source fixes described here**; those documents retain historical runtime evidence. The running8081 process was not restarted and has not loaded this patch.

## Behavior implemented

- Canonical `ACTIVE` means approved. Public main/category/search behavior remains approved-only. Product detail now requires ACTIVE or authenticated seller/admin permission; unauthorized pending/rejected/suspended detail and image metadata return404.
- Both seller catalog routes use the same filtered service. A farmer/supplier can retrieve their own pending products using their existing authenticated seller endpoint. Administrators can review any seller. Anonymous or unrelated callers receive ACTIVE products only. Caller identity comes from the security context and current database account/role through ResourceAuthorization, never the requested seller ID alone.
- Product-image metadata inherits the product-detail check. Database image bytes also check product-image links and product visibility; unlinked product-folder uploads fail closed. The local filesystem interceptor checks linked product visibility, rejects alternate path spellings and resolves real filesystem names so Windows case/short-name aliases cannot skip the link check. No image data was moved or modified.
- Database/local images use `Cache-Control: no-store` instead of public long-term caching. This avoids new shared-cache exposure when an image's product becomes nonpublic; it cannot erase copies already cached or downloaded.
- Public GET/HEAD and POST routes are separate arrays passed to method-aware matchers. Category mutations require ADMIN at the filter chain and actual service proxy. Public category reads and OPTIONS requests remain available.
- Removed implicit category creation from product create/update: an existing category name still resolves; an unknown name returns400 instead of silently creating a category. Numeric category-ID Android requests are unchanged. Administrators create categories through the guarded category API.

No product status value, creation status, approval transition, stock filter, sold-out behavior or approved-edit policy changed. No unlisting was implemented. Unlisting and sold-out requirements remain Daniel's handover work, outside this patch. Orders, payments/M-Pesa and all migrations were untouched.

## API compatibility and important limitation

Product routes, response envelopes, Long IDs, money/quantity fields and multipart `image` upload contracts are unchanged. The current Android authenticated seller-list/detail/image-metadata requests retain pending access. Public pending detail/image reads now intentionally return404. Category mutation returns401 to anonymous callers and403 to authenticated non-admins.

**Pending image bytes now require an authenticated owner/admin request.** Their existing same-origin image URL still works with the owner's access-token Authorization header; approved image bytes remain public. Android currently uses a token-free image client, so pending image display/reconciliation through that client requires a small coordinated Android change (same-origin-only authentication), or a separately reviewed private-image delivery design. No Android files were authorized or changed in this pass. Never send bearer tokens to Cloudinary/external hosts. Do not claim the current Android image loader passes after this security tightening.

External public Cloudinary URLs are outside these backend routes; API checks cannot revoke externally hosted public assets or previously cached copies. A private/signed provider strategy and cache invalidation are still needed before claiming universal asset confidentiality. Non-product image policies were not redesigned. The local/database delivery changes protect canonical product-linked assets and product-folder orphans; this is not a security review of every inspection/KYC image route.

## Exact files changed by this pass

| File | Purpose |
|---|---|
| src/main/java/com/kilivana/backend/security/ProductVisibility.java (new) | Shared approved-or-owner/admin read predicate and image-link checks |
| src/main/java/com/kilivana/backend/ecommerce/service/ProductService.java | Guard details, filter seller results, prevent implicit category creation |
| src/main/java/com/kilivana/backend/ecommerce/repository/ProductImageRepository.java | Find all asset links by public ID for visibility checks |
| src/main/java/com/kilivana/backend/common/controller/StoredImageController.java | Check product image visibility and use no-store caching |
| src/main/java/com/kilivana/backend/config/LocalStorageWebConfig.java | Guard local product assets and canonical paths; no-store caching |
| src/main/java/com/kilivana/backend/config/SecurityConfig.java | Method-aware public matchers and admin category mutations; preserve OPTIONS |
| src/main/java/com/kilivana/backend/ecommerce/service/CategoryService.java | Admin checks on create/update/status/delete service methods |
| src/test/java/com/kilivana/backend/security/CatalogVisibilityTest.java (new) | Real filter/controller/service visibility, seller aliases, image bytes, category method and preflight regressions |
| src/test/java/com/kilivana/backend/security/LocalProductImageVisibilityTest.java (new) | Local asset denial, no-store, path normalization and real-file aliases |
| src/test/java/com/kilivana/backend/security/ServiceAuthorizationTest.java | Real security proxies/identity predicates: category lifecycle, pending seller/admin reads, no implicit categories |
| src/test/java/com/kilivana/backend/ecommerce/service/ProductStockSafetyTest.java | Constructor fixture updated for visibility dependency; reservation/moderation assertions retained |
| docs/integration/20-catalog-visibility-category-security-fixes.md (new) | This report |
| docs/integration/README.md | Append this current implementation checkpoint, preserving earlier uncommitted content |

All other pre-existing modified/untracked work was preserved, including V16–V18, historical audit documents and local tooling. No staging, branch changes, commits, pushes or PR creation.

## Tests and verification

JDK17, Maven offline. Initial invocation stopped before compilation because inherited JAVA_HOME was invalid; corrected only in the test process. Initial focused run:54 passed. The first broader run found two new local-path expectations failing because Spring normalizes repeated slashes; added raw-path validation, then extended coverage to real Windows filesystem aliases and category OPTIONS handling.

**Final command:** `mvnw.cmd -o "-Dtest=!OptionalFilterQueryTest" test`

**Final result:314 tests passed,0 failures,0 errors,0 skipped; BUILD SUCCESS.** This includes35 additional security/regression cases over the previous279 database-free cases, plus existing authentication, account-state, farmer profile, stock, pricing and authorization tests. CatalogVisibilityTest exercises actual MockMvc controllers, SecurityConfig and ProductService/ProductVisibility with repository mocks. ServiceAuthorizationTest exercises actual method-security proxies and ResourceAuthorization with mocked persistent accounts. Test status changes exist only in memory, never PostgreSQL.

OptionalFilterQueryTest was excluded completely to avoid connecting to or inserting into the existing database. No SQL, seeders, database migrations or live create/update/approval requests ran. These changes add a derived repository query, not schema requirements. All seven approved migration/helper manifest entries still match their checksums. `git diff --check` passed. Test logs are ignored under target/ and should not be included in a PR.

Android tests and current physical-device image behavior were not rerun: Android was unchanged, and the private-byte authentication requirement needs a companion client change. No actual website/admin frontend regression or live deployment smoke test is claimed. Existing8081 process remains on its prior runtime; source tests do not establish that it has these fixes.

## Handover readiness

**Ready for focused code/handover review, not a deployment or complete farmer-device compatibility claim.** Review the13 files above alongside preserved pre-existing work. Coordinate authenticated pending-image loading in Android/web/admin, arrange a reviewed runtime reload and GET-only owner/admin/unrelated/anonymous smoke checks, and address external/cached asset exposure before public deployment. The existing uncommitted migration/evidence file curation from document19 is still required before a commit/push.

Approved-product editing/reapproval remains a separate unresolved team decision. Safe unlisting and sold-out functionality remain documented handover requirements; no implementation was added. No new Phase2 feature development is included. Stopped for review.
