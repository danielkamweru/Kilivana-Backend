# Delivery workflow API contract

This contract extends the existing `/api/v1` API. Existing routes remain compatible;
new routes use authenticated identity from the JWT and return the standard `ApiResponse`
envelope.

## Identifiers and lifecycle

- `orderId` identifies the commercial order.
- `deliveryJobId` identifies seller-specific fulfilment work for an order.
- `tripId` identifies one driver execution attempt. A job may have multiple historical
  trips; no more than one trip for a job is active at a time.
- New GPS routes address a trip; the server resolves its linked delivery job and checks
  the authenticated driver. Legacy job-based GPS routes remain supported.
- GPS fixes never change the delivery-job status. Starting a trip, confirming pickup,
  and completing delivery are distinct business actions.
- Job status uses the existing wire values: `pending_assignment`, `assigned`,
  `accepted`, `en_route_to_pickup`, `arrived_at_pickup`, `picked_up`, `in_transit`,
  `arrived_at_destination`, `delivered`, `cancelled`, and `failed`.

## Pickup stations

| Method and path | Authentication / ownership | Request | Success |
|---|---|---|---|
| `POST /api/v1/admin/pickup-stations` | ADMIN | Station name, address, region, latitude, longitude and optional availability/contact fields | `201`, canonical station response |
| `GET /api/v1/admin/pickup-stations` | ADMIN | None | `200`, station list |
| `GET /api/v1/admin/pickup-stations/{id}` | ADMIN | None | `200`, station |
| `PUT /api/v1/admin/pickup-stations/{id}` | ADMIN | Same validated station fields | `200`, updated station |
| `DELETE /api/v1/admin/pickup-stations/{id}` | ADMIN | None | `200`, deactivated station |
| `GET /api/v1/pickup-stations/nearest?lat={lat}&lng={lng}&radiusKm={km}&limit={n}` | Authenticated | Valid coordinates; radius `> 0` and at most 100 km; limit 1–100 | `200`, nearest available stations in radius, possibly empty |

Latitude and longitude are required when an admin creates or updates a station. Send
decimal WGS84 coordinates; latitude must be between -90 and 90, and longitude between
-180 and 180. For example:

```json
{
  "name": "Kilivana Westlands Pickup Point",
  "address": "Soin Arcade, Westlands, Nairobi, Kenya",
  "latitude": -1.2653443,
  "longitude": 36.8039078,
  "region": "Nairobi",
  "isAvailable": true
}
```

Existing available, region, detail, availability, and `/my-stations` routes remain
supported. A saved station is only a buyer preference. Checkout resolves a selected
station by ID and uses its canonical database address and coordinates.

`GET /api/v1/pickup-stations/available` includes each station's latitude and longitude,
so clients can use browser geolocation to calculate/sort distances locally and build
external map links without a separate map provider or nearest-station request. The
`/nearest` route remains available when server-side distance filtering is preferred.

## Seller dispatch locations

Farmer farm creation and updates at `/api/v1/farmers/me/farms` do not require latitude
or longitude; provide both only when known. Farmers can create products and receive
orders without entering coordinates. If coordinates are omitted at order time, an admin
must add valid farm coordinates through `PUT /api/v1/admin/farms/{id}` before dispatch.
Dispatch uses the order's coordinate snapshot when present and falls back to the farm's
current address and coordinates when the order was placed without coordinates. Dispatch
still requires valid coordinates so map routes have a real origin; no coordinates are
invented from a text address.

Supplier warehouses use
the following routes; the authenticated supplier owns all resources, with no owner ID
accepted from the request:

| Method and path | Authentication / ownership | Request | Success |
|---|---|---|---|
| `POST /api/v1/suppliers/me/warehouses` | SUPPLIER | Name, address, county, coordinates and optional contact/hours/default fields | `201`, warehouse |
| `GET /api/v1/suppliers/me/warehouses` | SUPPLIER | None | `200`, owned warehouses |
| `GET /api/v1/suppliers/me/warehouses/{id}` | SUPPLIER, own warehouse | None | `200`, warehouse |
| `PUT /api/v1/suppliers/me/warehouses/{id}` | SUPPLIER, own warehouse | Valid warehouse fields | `200`, warehouse |
| `DELETE /api/v1/suppliers/me/warehouses/{id}` | SUPPLIER, own warehouse | None | `200`, deactivated warehouse |
| `PUT /api/v1/suppliers/me/warehouses/{id}/default` | SUPPLIER, own warehouse | None | `200`, warehouse |

Product requests must specify `farmId` for a farmer or `warehouseId` for a supplier.
The service validates seller type, ownership, active status and address before accepting
the listing. Supplier warehouse coordinates remain required for orders. Existing
products without a dispatch location need a seller/admin to select one before they can
be ordered and dispatched.

## Checkout, orders and dispatch

`POST /api/v1/checkout/delivery-quote` continues to accept legacy manual
`pickupLocation` values. New pickup-station checkout supplies `deliveryMethod:
"PICKUP_STATION"` and `pickupStationId`; the response includes that ID and the
canonical station details. `DOOR_DELIVERY` continues to use a buyer-owned `addressId`.

`POST /api/v1/checkout/order` retains its existing quote-based request and response.
The station selection is bound to the authenticated buyer's server-side quote.
Door-delivery quotes require a buyer-owned saved address, but the address may omit
latitude and longitude so buyers can place orders without entering map coordinates.
Seller farms may also omit coordinates at order time; admins must add farm coordinates
before dispatch. Supplier warehouses still require coordinates when the order is
created. Door-delivery dispatch requires destination coordinates to build a map-ready
delivery job, so add or update the buyer address before dispatch if it was saved without
coordinates.

`POST /api/v1/admin/orders/{orderId}/dispatch` creates/reuses one job per seller and
dispatch origin, using the selected products' active farm/warehouse origins and an
immutable destination snapshot. It is idempotent by `(orderId, sellerId, origin)`.
Orders with several sellers or origins yield several delivery jobs. No job is created
for unpaid/pending orders. Existing
`POST /api/v1/admin/orders/{orderId}/assign` remains for legacy single-seller dispatch;
it must not merge different pickup origins.

## Dispatcher and driver

| Method and path | Authentication / ownership | Request | Success / transition |
|---|---|---|---|
| `GET /api/v1/logistics/jobs` | ADMIN or DRIVER; driver sees only own assigned jobs | None | `200`, all jobs for admin or own jobs for driver |
| `GET /api/v1/logistics/jobs/status/{status}` | ADMIN | Status in path | `200`, jobs |
| `PUT /api/v1/logistics/jobs/{deliveryJobId}/assign?driverId={id}` | ADMIN | Driver ID | `200`, job `pending_assignment → assigned` |
| `POST /api/v1/trips/jobs/{deliveryJobId}/accept` | Assigned DRIVER | None; identity from JWT | `200`, creates and returns an active trip including `tripId` and `deliveryJobId` |
| `POST /api/v1/logistics/jobs/{deliveryJobId}/reject?reason={reason}` | Assigned DRIVER | Optional reason; identity from JWT | `200`, job returned to dispatch |
| `GET /api/v1/trips/mine` | DRIVER | None | `200`, caller's active trips with `tripId` identifying each trip and its linked `deliveryJobId` |
| `GET /api/v1/trips/current` | DRIVER | None | `200`, current active trip's job fields plus `tripId` |
| `GET /api/v1/trips/{tripId}` | Assigned DRIVER | None | `200`, own trip including `tripId` and `deliveryJobId` |
| `POST /api/v1/trips/{tripId}/tracking/start` | Assigned DRIVER | None | `200`, assigned/accepted job advances to `en_route_to_pickup` |
| `POST /api/v1/trips/{tripId}/status?status={nextStatus}` | Assigned DRIVER | Next status; `picked_up` from en-route records `arrived_at_pickup` and `picked_up` in order | `200`, linked job/trip advanced |
| `POST /api/v1/trips/{tripId}/location` | Active trip's DRIVER | Latitude, longitude and optional telemetry/timestamp | `200`, fix accepted; no status transition |
| `POST /api/v1/trips/{tripId}/otp/verify?otp={code}` | Active trip's DRIVER | Buyer handover code | `200`, code verified |
| `POST /api/v1/trips/{tripId}/proof-photo` | Active trip's DRIVER | `multipart/form-data`, required image part named `image`; JPEG, PNG, GIF or WebP, max 10 MB | `201`, `{ "success": true, "data": { "url": "..." } }` |
| `POST /api/v1/trips/{tripId}/proof?otp={code}` | Active trip's DRIVER | Proof recipient/signature/photo fields; code required unless already verified | `200`, linked job/trip completed once |
| `GET /api/v1/buyer/orders/{orderId}/delivery` | Owning BUYER | None | `200`, own order delivery summary |
| `GET /api/v1/seller/orders/{orderId}/deliveries` | Seller with an order line | None | `200`, only that seller's delivery jobs |

Dispatch requires an existing active DRIVER account, a verified driver profile, an
available driver, and no active assignment. The existing job state machine continues
to validate all transitions. Delivery completion requires the existing hashed,
attempt-limited buyer code and proof of delivery. Delivery codes do not expire, are
consumed after successful verification, and remain protected by the configured
incorrect-attempt limit and lockout.

The proof-photo upload is authenticated and scoped to the assigned driver's trip. It is
available only while the job is `in_transit` or `arrived_at_destination`. The uploaded
image is stored using the configured image-storage provider and the response URL is meant
to be sent as `photoUrl` in the JSON proof request; device-local URIs such as Android
`content://` values are rejected by proof submission. Upload errors use the standard API
error envelope: `400` for unsupported/missing files, the 10 MB limit, or an inactive job;
`403` for another driver's trip; and `404` for an unknown trip/job. Configure
`STORAGE_PROVIDER=database` (the default) for durable database-backed storage and set
`PUBLIC_BACKEND_URL` so returned image URLs are absolute and usable by other clients.

## Schema rollout

`db/migrations/V24__delivery_workflow_foundation.sql` defines the persisted warehouse,
dispatch-snapshot, job, trip, GPS-link and delivery-event schema.
`db/migrations/V25__backfill_dispatch_locations.sql` assigns existing products to an
active default supplier warehouse or their sole active location when unambiguous, then
backfills missing order-item snapshots from eligible product locations. It intentionally
leaves products without a safe location choice unchanged. The application does not
currently configure Flyway, so deployments must run these migrations through the
existing database rollout process; `ddl-auto=update` is not a production migration
strategy. Validate the migration against the deployed PostgreSQL version and data before
rollout.

## Errors and compatibility

Responses continue to use the existing global exception and `ApiResponse` handling:
400 for invalid input or transitions, 401 for missing authentication, 403 for a
role/ownership failure, 404 for missing resources, 409 for conflicting or duplicate
operations, and 429 for delivery-code lockout. Existing logistics, proof, tracking,
trip-alias, and saved-station routes are retained; no route is removed in this change.
Legacy tracking routes use the delivery-job ID as their path identifier; new trip routes
use a distinct trip ID and persist it with each GPS fix and delivery event.

The committed OpenAPI JSON is a generated snapshot and does not currently list all
source routes. Runtime Swagger is generated by Springdoc; regenerate the snapshot when
the project has an established snapshot-generation command.
