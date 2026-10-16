# Optional demo data — archived, never automatic

`short_distance_order.sql` preserves Daniel's optional short-distance order fixture from upstream `4679d49`. It was moved from `db/migrations/V16__seed_short_distance_order.sql` to avoid colliding with the reviewed V16 schema repair. Only trailing whitespace was normalized; no SQL statement or fixture value was changed.

No configured Flyway/Liquibase runner, Render command or startup hook executes this file. Its relocation does not change the inspected deployment commands. This is not a schema migration and must not be added to startup, CI, deployment or the reviewed transactional migration driver.

**Do not execute this archived fixture as-is.** It uses fixed IDs, dummy authentication/OTP data, pre-approved product/user states and legacy column names that do not match current canonical mappings. Any future runnable demo must be reviewed separately and confined to a disposable database. It must not approve existing products or modify the shared development database.

The reviewed V16–V18 schema files remain byte-for-byte unchanged. Historical migrations must not be replayed blindly. No SQL was run during reconciliation.
