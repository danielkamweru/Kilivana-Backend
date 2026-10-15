# Kilivana — handover checklist, endpoint inventory and proposed PR

2026-10-15. Read with [final parity audit](17-final-functionality-parity-handover.md) and [agreed approval policy / gaps](18-product-approval-decision-and-verification.md). Documentation only; no commit, push or PR created. No Phase2 implementation started.

## Migration completion checklist

- [x] Use Daniel's backend as canonical; Android farmer transport defaults to8081 and canonical envelopes/Long IDs.
- [x] Adapt farmer registration/login/profile, categories, own products, create/edit, images and refresh without copying entire modules.
- [x] Retain Phase1 ownership/account/token protections, authoritative order pricing, locking and fail-closed checkout.
- [x] Reconcile upstream in merge8f9dded; no rewrite of Daniel's commits. Upstream inspection/product/payment/logistics/tracking improvements retained.
- [x] Apply reviewed V16–V18 locally after isolated rehearsal, backup and preservation verification; Hibernate validate passes.
- [x] Record mandatory farmer-product admin approval as AGREED; leave pending records intact.
- [ ] Fix or explicitly assign the approval visibility and method/category authorization gaps before public use (document18).
- [ ] Decide transfer or explicit deferral for safe unlisting, saved products, contact inbox, secure session/recovery capabilities, seller-projected orders, inspection/report/visit capabilities and event notifications. Do not describe deferred work as migrated.
- [ ] Agree edit-after-approval and withdrawal/restore policies; farmer verification remains a distinct workflow.
- [ ] Review/deliver Android's separate uncommitted changes; preserve all three repositories' existing work.
- [ ] Connect phone and verify current runtime login, existing data/images, create/edit and real process-restart session restoration.
- [ ] Inspect actual web/admin API consumers and test compatibility; their current functionality was not verified here.
- [ ] Curate untracked migration/evidence documents, review secrets/PII, run final diff checks and refresh remote comparison before committing.
- [ ] Obtain explicit commit/push/PR instructions. Current task authorizes none.

## Android-required canonical endpoints

Base `/api/v1/`; exact current bindings are A `app/src/main/java/com/example/farmapp/network/canonical/CanonicalBackendApi.kt`. Numeric IDs are Long; response bodies use CanonicalApiResponse. Login uses email; canonical pending product wire value is `pending`. No reference UUID conversion or fractional stock rounding.

| Method and route | Current consumer / requirement | Evidence and caveat |
|---|---|---|
| POST auth/register | Farmer signup | Prior HTTP smoke passed; user then profile flow is not atomic |
| POST auth/login | SignIn/Login, AuthRepository | Prior HTTP smoke passed |
| POST auth/refresh | RefreshCoordinator through CanonicalAuthService | Prior HTTP refresh passed; no durable replay/logout revocation |
| GET auth/me | Session identity/profile | Prior HTTP smoke passed; current Android restart not device-tested |
| POST profiles/farmers/{id} | Farmer registration farm profile | Own/admin only; prior HTTP smoke passed |
| PUT users/me | Farmer account/profile editor | Updates account name/email/phone; do not mistake this for mapping every farm-specific field |
| GET categories/type/FARMER | Crop creation category picker |11 active entries verified |
| GET products/seller/{id} | Farmer dashboard/My Crops | Own pending needed; current public overexposure must be fixed without losing owner access |
| GET products/{id} | Crop detail/edit reload | Prior HTTP smoke passed; current anonymous pending exposure confirmed |
| POST products | CropPublisher | JSON name/description/unit/price/stockQty/minimumOrderQty/categoryId; moderation preserved |
| PUT products/{id} | Crop editing | Integer total stock must retain reserved stock; money precision preserved |
| GET products/{id}/images | Image reconciliation/display | Prior smoke passed; pending metadata currently public |
| POST products/{id}/images | Multi-photo publisher | Multipart field `image`, sortOrder/isPrimary query parameters; uncertain uploads not automatically retried |
| GET /api/v1/images/{publicId}, local /uploads or allowed Cloudinary HTTPS | Separate token-free image transport | Prior byte preservation/upload retrieval passed; privacy fix requires coordinated image client contract |

Current A CanonicalFarmService explicitly disables unlist, farmer orders and legacy farmer-roster calls. Do not add DELETE products to the Android contract: C physically deletes. BuyerScreen imports mockProducts/mockOrders; SupplierScreen mockSupplyItems; InspectorScreen mockInspectionRecords. Their local saved/order/message interactions do not prove any backend integration. Canonical product/profile adapters are the real farmer connection; demos remain preserved. No safe seller-order endpoint currently fills the disabled farmer feed.

## Prioritized owner handover

| Priority | Work | Transfer versus future development | Acceptance / tests |
|---|---|---|---|
| P0 | Pending catalog/asset privacy and method-specific public matchers/category write authorization | Focused current canonical security/approved-policy correction | Anonymous/unrelated denial; owner/admin pending access; admin-only approval; no new automatic approval; farmer regressions |
| P1 | Safe unlisting or explicit accepted deferral | Real R farmer capability currently disabled in A; requires status/restore decision | Retain records/history; owner scope; approved/public and pending/withdrawn behavior tested |
| P1 | Curated review branch and Android companion handover | Existing completed work, not new modules | V16–V18 hashes/evidence, negative auth and farmer tests, no private artifacts, phone smoke, review actual consumers |
| P2 | Saved products and contact inbox, if retained | Real R-only capabilities; C Long entities/endpoints/migrations needed | Unique save relation and ownership; approved-only visibility; admin inbox + submission anti-abuse |
| P2 | Refresh revocation/rotation, logout, password recovery/rate limiting | Transfer capabilities, redesign safely for C; Android coordinated contract step | Replay/expiry/revoke transaction tests; secure reset delivery; generic errors; no token leaks |
| P2 | Seller-projected orders and notification event integration | Real R capability but incompatible aggregates / delivery risks | Mixed seller privacy, no unauthorized whole-order changes, durable idempotent notifications |
| P2 | Inspection checklist/report/visit evidence pieces | Transfer useful R data/workflow concepts, retain C target model and authorization | Assigned inspector only; immutable submitted evidence; admin final review references report; no automatic farmer verification |
| Later | Full buyer checkout, real payments/refunds, delivery pricing, deployment, completed inspection administration, remaining client experiences | Daniel-owned completion/new development; original stubs do not count as working features to migrate | Domain agreement, provider sandbox/callback security, contention/idempotency tests, cross-client end-to-end tests |

Badges and richer metadata are optional retained-feature decisions, not prerequisites for the currently working farmer slice. Peer messaging is new development, not a completed R feature. Approval after editing and farmer-verification eligibility still require agreement. This list does not authorize any implementation.

## Working-tree review inventory

C branch/HEAD above are unchanged. Remote origin is Daniel's `https://github.com/danielkamweru/Kilivana-Backend.git`, not a fork. Comparison with locally recorded origin/main shows49 changed committed files, including security/tests and sanitized historical review docs. No fetch was performed in this audit; remote freshness/conflict status must be checked again before a later push. No conflict resolution, staging or branch change was attempted.

Recommended review candidates, all currently untracked unless noted:

| Files | Treatment before a commit |
|---|---|
| db/migrations/V16__reconciled_tracking_schema.sql; V17__reconciled_product_payment_fields.sql; V18__reconciled_payment_checks_order_item_fk.sql | Include reviewed forward schema repair with exact successful rehearsal hashes. Do not execute blindly against other environments or modify approved bodies for this audit |
| docs/integration/schema-review/preflight.sql; apply-reviewed-schema.psql; preservation-query.sql; verify.sql; SHA256SUMS.txt | Include as reviewed local migration tooling/evidence, explicitly document baseline-specific guards and transaction driver. Not a universal deployment procedure or a reason to rerun historical migrations |
| docs/integration/13-local-schema-migration-plan.md;14-isolated-migration-rehearsal.md;15-isolated-migration-rehearsal-results.md;16-local-migration-backend-verification.md | Relevant evidence. Review for machine paths/private evidence locations before sharing; prefer sanitized review copies, preserving originals. Keep backup archives/fixture credentials/logs private |
| docs/integration/17-final-functionality-parity-handover.md;18-product-approval-decision-and-verification.md;19-handover-checklist-endpoints-pr-summary.md | New current audit/decision/PR draft; include after user review |
| docs/integration/README.md (already modified before audit) | Preserve its existing changes; reconcile stale token-design checkpoint with current docs in a separate small docs edit, not a blanket overwrite. Current decision is authoritative in17/18 |
| docs/integration/02-token-lifecycle.md | Historical unfinished token design; review for superseded assumptions and choose explicit inclusion/deferral |
| docs/integration/03-local-schema-proposal.sql;03-local-startup-plan.md;04-local-categories-proposal.sql | Original local initialization artifacts; preserve locally, normally exclude from final PR or label archival. Not migrations to reapply to existing data |
| docs/integration/05-local-demo-verification.md;06-backend-feature-parity-matrix.md;07-android-integration-audit.md;08-farmer-inspection-verification-audit.md;09-backend-migration-plan.md;10-open-decisions-technical-risks.md;11-phase1-security-implementation-report.md | Local originals have sanitized committed counterparts under docs/integration/review/. Avoid duplicate/stale review copies and superseded policy claims; preserve originals |
| scripts/Start-LocalCanonical.ps1; scripts/GenerateLocalSchema.java | Existing untracked tooling; launcher is workstation-specific. Review portability/private-path handling before deciding inclusion; do not include merely because untracked |

No blanket `git add .`. Existing A modifications include canonical adapters/models/tests, farmer/auth UI/repositories, Gradle and local IDE files; R also has pre-existing product/image/security/test/Docker changes. Neither repository was modified in this audit. Their uncommitted changes are not included by pushing C and must not be discarded. IDE files are not part of the backend review.

Private/excluded artifacts: credentials CLIXML, JWT/session data, pg_dump archives/large objects, live fixtures, user data, workstation logs, target/ runtime helpers, generated builds and Android APKs. No secrets were printed. This inventory is not a certification that every existing untracked document/tool is sanitized; a final content and staged-diff review remains mandatory after selecting files. Existing migration checksums must remain consistent with approved evidence.

## Test evidence and remaining checks

Report16 documents279 non-database backend cases plus5 read-only PostgreSQL search cases =284 passed;138 Android unit tests passed;37-entity Hibernate validation; registration/login/profile/categories, creation/editing, two-image upload/retrieval, original image fingerprints, refresh, forbidden owner escalation and cross-profile denial. All71 migration preservation entries matched, then original rows/images were checked again after the authorized smoke additions. No original product was approved.

Current audit added only anonymous GET checks: health200, public main list0 records, pending detail200, pending seller catalog200, image list200 with2 entries. These confirm a bug, not successful approval enforcement. No new database writes/test records. No tests were rerun because this audit changes documentation only.

Outstanding verification: positive admin approve/reject with isolated fixtures; visibility matrix including actual image bytes/providers; category-write negative tests; real phone current-runtime smoke; actual web/admin consumers; safe concurrency checkout/provider tests when those features are implemented. Historical physical-device evidence is not a current-runtime pass. No complete production readiness claim.

## Proposed PR title

Integrate canonical Android farmer contracts, Phase1 security safeguards and validated schema repair

## Proposed PR description

Android farmer development now uses the canonical backend's existing registration, profile, catalog and multi-image APIs. This branch adds focused ownership/account/token enforcement, protects catalog pricing and stock updates, and preserves Daniel's newer upstream changes through merge8f9dded. Reviewed V16–V18 repair the local schema for the merged entities; rehearsal and local preservation/Hibernate evidence accompany the scripts. Android adapter changes require a separate companion review.

Authorization changes restrict user-specific inspection/order/payment/dispute/cart/address/notification records and privileged status operations. Pending farmer accounts remain permitted where intended. Buyer checkout stays503 and direct order creation remains admin-only until safe quoting/checkout exists; no fake successful production checkout was introduced. Canonical DTO/envelope/Long/integer-stock contracts remain the source of truth.

**Agreed product policy:** all farmer submissions require admin approval before public visibility. Creation remains pending and owner escalation is forbidden. **Known blocker:** public detail/seller/image paths still expose pending listings; public endpoint method matchers/category mutations also need correction. Do not present this branch as enforcing complete catalog privacy or ready for public launch. Edit-after-approval is unresolved; no pending records were approved and no new transition policy was invented.

Validation:284 backend cases,138 Android unit cases, Hibernate validation and actual local farmer HTTP smoke passed as recorded in report16. Current GET-only audit reproduces the pending visibility gap. Physical phone verification against the reconciled runtime and actual web/admin clients remain outstanding. Refresh tokens remain stateless without durable logout/replay revocation. Seller-safe order feeds, safe unlisting, complete inspection/admin review, checkout/provider operations and remaining transfer-or-defer items are explicit follow-up work, not silently completed.

## Proposed reviewer notes and decision gate

Review security/account changes and mixed-seller pricing/locking separately from schema tooling and historical documentation. Preserve upstream af453d3 ancestry; inspect merge semantics rather than blindly reapply R modules. Validate migration baseline and production deployment conventions separately from the already migrated local database. Confirm Android image/session compatibility before changes to those contracts. Accept or reassign each missing original capability explicitly.

**Branch readiness:** ready for an honest scope-limited handover discussion and PR-content preparation; not yet a curated commit/push, completed parity claim or public deployment. Before committing: approve file selection/sanitization and dispositions of discovered gaps; before public use: fix approval visibility and authorization issues. Before pushing: refresh remote state, inspect complete selected diff, ensure checks, report exact outgoing commits and obtain explicit approval. No push command is executed by this task. A later proposed command, after those checks, is `git push origin feature/android-backend-integration` (never main or force-push).

The contributor can return to local Android farmer development now using the verified canonical core. Public buyer browsing, removal and farmer order features need stable safe contracts first; future backend features can be owned by Daniel with the above backlog accepted. No requirement to build the whole marketplace is implied by this handover.
