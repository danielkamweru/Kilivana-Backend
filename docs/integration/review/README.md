> Update 2026-10-15: the upstream code reconciliation is now recorded in [12-phase1-upstream-reconciliation.md](12-phase1-upstream-reconciliation.md). That report supersedes the historical upstream-conflict and ACTIVE-only warnings below. Live merged-build verification is blocked by the local schema; no migrations or pushes were performed.

# Phase 1 review guide

This review covers the tested canonical backend feature branch, based on `a55b56e` and the earlier `16e3292` main baseline. It preserves Daniel's backend and existing canonical contracts while closing focused authorization/pricing gaps. Android farmer integration has live device evidence, but **Android client code is in the separate FarmApp working tree and is not included in this backend PR**.

## Review order and scope

1. Existing `a55b56e`: disable insecure password reset; historical source/tests/report remain traceable, without rewriting the commit.
2. Access-account validation and recovery enumeration fix: current account must be ACTIVE and token role must match; login/refresh token payloads unchanged.
3. Service ownership/admin guards, catalog pricing, row-locked stock and fail-closed checkout. Guards and inventory are grouped because their tests and ResourceAuthorization service span the same service files; no artificial partially secured intermediate inventory commit.
4. Shareable audit and implementation evidence in this directory. Original developer documents, local SQL proposals, launcher/export scripts, credential files and build/runtime artifacts are excluded from the new review commits. Original working-tree files are preserved.

Documents:

- [Feature parity matrix](06-backend-feature-parity-matrix.md)
- [Android integration audit](07-android-integration-audit.md)
- [Inspection and final administrator verification](08-farmer-inspection-verification-audit.md)
- [Migration plan](09-backend-migration-plan.md)
- [Business decisions and risks](10-open-decisions-technical-risks.md)
- [Phase 1 implementation report and exact changed files](11-phase1-security-implementation-report.md)
- [Historical farmer demo verification](05-local-demo-verification.md)

These copies omit developer workstation/device identifiers and local Git-status listings. The feature matrix omits its generated endpoint inventory for readability. Historical reports describe the code and environment tested then, not current upstream functionality. In particular audit statements about missing payment/tracking capabilities must be revalidated against new upstream work.

## Significant API behavior changes

- Method-security denials return403 in the canonical envelope; protected anonymous requests remain401. Alternate controller aliases cannot establish ownership using a supplied user ID.
- Inspection creation/global listing/deletion is administrator-only. Single records/results/evidence require assigned inspector or admin. This is not the complete report-submission/final-review workflow.
- Whole orders/payments are buyer/admin scoped to avoid leaking mixed-seller lines. Seller-safe projections remain unavailable. Payment mutations/refunds and dispute resolution are administrator-only; own cart/address/notification/private-account operations are scoped.
- Direct order entry is administrator-only until delivery-price authority and real buyer checkout are agreed. Checkout returns503 instead of claiming an order was created. Catalog prices are authoritative, supplied price must match, quantities/minimum/stock/totals are validated, and relevant stock writes lock rows.
- Product status changes are administrator-only under the existing moderation authority. New listings remain pending; status values and visibility filters are unchanged. **Individual product approval is PENDING TEAM DECISION.** No verified-farmer publication/purchasing rule is introduced.
- Existing logistics side effects require assigned-driver/admin permissions. No new logistics workflow is implemented.
- Access requests require ACTIVE account state and current role. **Compatibility warning:** PENDING_VERIFICATION accounts are also rejected by the new ACTIVE-only check, while existing login/refresh still permit that status. This discrepancy needs explicit review before rollout; it does not affect the tested ACTIVE farmer registration flow. No deployment readiness is claimed.
- Forgot-password no longer reveals account existence; reset remains disabled. The preserved generic message does not establish email delivery. Refresh rotation/revocation and server logout are not implemented; Android sign-out remains local only.

## Validation

On the completed feature tree: 247 backend tests passed (0 failures/errors/skips; database-dependent OptionalFilterQueryTest excluded), 138 Android unit tests passed, and physical-device farmer registration/login/profile/categories/listings/pending creation/edit/two-image upload and retrieval passed. A separate process restored the encrypted session and refreshed against the actual local canonical backend. Hibernate validation startup and read-only PostgreSQL identity/count/rolled-back row-lock contention checks passed. No schema migration was required by these feature changes. Actual web/admin frontend regressions and full concurrent checkout/payment-provider integration were not performed.

These are prior Phase 1 results verified from the existing reports during review preparation; no new device accounts or database initialization are needed simply to prepare Git commits. Changes to upstream after that baseline are not covered by these results.

## Upstream reconciliation blocker

During preparation, origin/main advanced nine commits to `af453d3`. The feature branch did not exist remotely and had no upstream. Remote is `https://github.com/danielkamweru/Kilivana-Backend.git` (Daniel's repository, not a fork).

Upstream includes inspection/product DTO/entity changes, admin services, request logging/30-minute access expiry, REST/WebSocket tracking, Daraja code and migrations V14/V15. A non-mutating merge-tree check of the existing password-reset commit already conflicts in AuthService.java. The complete candidate must be checked as well; reconciliation must preserve both new upstream capabilities and the reset security guard. No merge/rebase or upstream migration has been executed.

No merge-ready or current-main test claim is made. Resolve/review the upstream integration in a separately authorized step and rerun tests before merging the PR. Do not blindly apply upstream schema changes to the existing local database. Daniel's real web/admin contracts, new WebSocket authentication paths and Daraja authorization must also be reviewed when integrating upstream.

## Remaining Phase 2 work

Agree durable refresh-session rotation/logout/replay policy and separately reviewed storage migration; private inspection/KYC evidence retrieval; ACTIVE/PENDING_VERIFICATION access policy; authoritative farmer/farm inspection target, submitted report and final admin review history; safe seller order/payment projections, delivery quote and real checkout. Preserve product moderation pending team decision. Original backend retirement and production rollout are not part of this PR.

Push/PR creation/merge require explicit user approval. No force push or direct main push is proposed. Phase 2 should start on a separate development branch only after the Phase 1 review checkpoint and upstream reconciliation decision.
