# Android/backend integration handoff

## Phase 1A — Password Reset Security + Daniel Handoff

Status: implementation compiled and database-free validation passed on 2026-10-08.
Focused suite: 15 passed; additional regressions: 130 passed. Across both runs:
145 passed, 0 failures, 0 errors, 0 skipped. **Approved by Daniel**, as reported
by the user at Phase 1B kickoff. Locally committed as
`a55b56e1a6f55b7a3ac6ec6bec18178cbde7c96e`. Deployment and merge are not asserted.
The Phase 1A report preserves its historical pending-approval checkpoint.

The Maven blocker was offline mode with an uncached parent POM, followed by
sandbox write restrictions on the Maven cache. An approved online run resolved
dependencies; the additional regression run then passed offline. Exact commands
and limits are recorded in the security handoff below. Only these documentation
files were edited during validation; the prepared implementation was preserved.

Verified starting branch: `feature/android-backend-integration`; baseline HEAD: `16e3292`;
starting working tree: clean. Git verification uses a per-command `safe.directory`
override; global Git configuration is unchanged.

See [password-reset security and Daniel handoff](01-password-reset-security.md) for
the vulnerability, response contract, changed files, validation, compatibility,
rollback and review requirements. This phase disables unsafe recovery; it does
not implement a replacement recovery flow.

No database connection, migration, schema change, JWT architecture change,
push or modification of `main` is authorized or required for this phase.
The final checkpoint authorizes one local commit containing only the six Phase 1A
source, test and documentation files; that checkpoint is complete.

Coordinate the Android recovery screen behavior before any deployment.
No handoff message has been sent.

## Phase 1B — Token lifecycle security audit and design

Status: **design in progress / awaiting review; implementation pending**.
See [token lifecycle audit and proposed architecture](02-token-lifecycle.md).
This audit starts from `a55b56e` on the feature branch with a clean working tree.
Only documentation changes are authorized here; no Java/configuration changes,
database connections, migrations, backend startup, commits or pushes. Phase 1B
is not approved. Daniel's policy and client-rollout decisions are required before
implementation.

## Current handover checkpoint — 2026-10-15

The sections above and earlier reports retain their historical checkpoints.
For the current reconciled implementation and remaining scope, read:

- [Final functionality parity and handover audit](17-final-functionality-parity-handover.md).
- [Agreed farmer-product approval policy and verification gaps](18-product-approval-decision-and-verification.md).
- [Handover checklist, Android endpoints and proposed PR summary](19-handover-checklist-endpoints-pr-summary.md).
- [Completed local migration and backend verification](review/16-local-migration-backend-verification.md).

**The team now requires administrator approval of every farmer-submitted product
before public visibility.** Earlier PENDING TEAM DECISION statements about whether
approval is required are superseded. Editing an already approved product remains
an open decision. Public pending-product exposure is a known unresolved gap;
this audit did not alter product records or application behavior. No commit,
push or PR is authorized by this checkpoint.

## Focused catalog security patch — 2026-10-15

See [catalog visibility and category authorization fixes](20-catalog-visibility-category-security-fixes.md)
for the uncommitted source corrections to the two final-audit findings and314 passing
database-free tests. Existing reports retain historical checkpoints. The running8081
process has not loaded this patch. Pending image byte reads now require owner/admin
authentication; Android's token-free image transport needs a coordinated follow-up.
No PostgreSQL data, migrations or product statuses changed. Unlisting and sold-out
functionality remain Daniel's handover requirements, outside this patch.
