# Android Integration Audit

> Review copy: historical evidence from the tested feature branch based on `a55b56e`, not an audit of current upstream `main`. Workstation paths/device identifiers are omitted. Original local documents are preserved outside this commit scope. See [review guide](README.md).

Audit date: 2026-10-14. Source roots and evidence definitions: [06-backend-feature-parity-matrix.md](06-backend-feature-parity-matrix.md). No Android files changed during this audit.

## Active transport and sessions

`FarmApp/app/build.gradle.kts:7` defaults to `http://127.0.0.1:8081/api/v1/`; USE_CANONICAL_BACKEND defaults true. `-Pkilivana.canonicalBackend=false` selects legacy Retrofit contracts; it does **not** automatically select the original server URL. Both flag and base URL must be checked for fallback builds. Mock auth is separately selectable; the isolated `.canonicaldemo` package is a development install suffix, not a second backend/database.

`data/remote/ApiClient.kt` is the only Retrofit construction found in Android main source. It chooses CanonicalAuthService and CanonicalFarmService or the dormant original ApiService/FarmApiService declarations. Auth uses a plain client; protected farmer requests use AuthTokenInterceptor/RefreshCoordinator; image downloads use a separate bearer-free client. Same-origin API/uploads paths and HTTPS res.cloudinary.com URLs are allowed; redirects and connection retries disabled. Build-specific HTTPS/host setup needs review before anything beyond USB-local development.

`network/EncryptedSessionPersistence.kt`, `TokenManager.kt`, `repository/AuthRepository.kt`, `network/AuthValidation.kt`, `AuthTokenInterceptor.kt` preserve encrypted session storage, origin marker validation and single-flight refresh. Canonical refresh expiry is not supplied; app attempts server refresh without inventing a deadline. Canonical sign-out only clears local storage. Protected GET/HEAD may refresh/retry; canonical mutations do not automatically retry after401. An expired access token can therefore cause a mutation to fail until a safe read/login restores access; this needs explicit UX acceptance, not silent mutation retry.

## Screen and repository call matrix

All paths below begin at `app/src/main/java/com/example/farmapp/`.

| Screen/workflow | Call chain and current source | Backend state / test evidence |
|---|---|---|
| AuthWelcome/SignUp/Login/SignIn | LoginScreen→AuthViewModel→AuthRepository→ApiClient.authService→network/canonical/CanonicalAuthService→CanonicalBackendApi POST auth/register/login, POST profiles/farmers/{id} | Real farmer registration/login tested. Signup adapter explicitly restricts live registration to FARMER. Other signup forms still exist but do not establish live onboarding. Partial registration account/profile failure needs recovery UI; do not register account twice. |
| Auth recovery | ui/screens/auth/PasswordRecoveryScreen.kt | Local OTP/reset preview, explicit notice; no recovery API call. Canonical secure reset deliberately disabled. |
| Role navigation | ui/navigation/AppNavigation.kt and AuthDestination.kt | Farmer→FarmerShell; buyer/supplier→local screens; inspector only if session verificationStatus VERIFIED, else AwaitingVerificationScreen. Client routing is not server authorization. DRIVER/ADMIN unsupported. Canonical inspector registration currently NOT_REQUIRED, not accepted by VERIFIED navigation. |
| Farmer home/listings/detail refresh | FarmerShell.kt→repository/ProductRepository.kt→CanonicalFarmService.myProducts→GET products/seller/{id}; detail GET products/{id} | Real authenticated farmer UI, current seller filtered/checked locally; backend public route itself not private. Physical-device listing reads tested. Canonical lowercase pending mapped to Android PENDING. |
| Add Crop/review/publish | AddCropScreen/CropCategoryDropdown→ProductRepository→CanonicalFarmService categories/create→GET categories/type/FARMER, POST products | Real active categories, integer stock and exact prices; pending moderation displayed. Live creation tested. No approval bypass API called. Names/category/unit/size validation need cross-client contract cases. |
| Edit Crop | CropDetailScreen/CropSaveController→ProductRepository.update→CanonicalFarmService.update→GET product then PUT products/{id} | Real edit tested; observed reserved count added to entered available quantity. Race between read/save remains; no server atomicity added. Threshold field explicitly unsupported. |
| Photos | CropPhotoController/CropPhotoStorage/CropDraft/CropPublisher→ProductRepository.upload→CanonicalFarmService→POST products/{id}/images with image, sortOrder,isPrimary; ProductImage.kt→ApiClient.imageContent | Two database-provider PNG uploads and binary retrieval live tested. Local photo drafts retained, uncertain checkpoint prevents blind retry. Canonical filename/type/size unknown, no fabricated metadata. No UI image replace/delete/reorder integration; providers local/Cloudinary not tested live. |
| Farmer profile/account phone | FarmerProfileScreen→FarmerProfileViewModel→FarmerProfileRepository→CanonicalFarmService.farmerProfile/updateFarmerPhone→GET auth/me, PUT users/me | Live account read/phone update tested. **Farm profile fields not loaded from GET profiles/farmers/{id}**; registration creates profile but UI does not edit farmName/location/details. Avoid describing this as complete farm-profile parity. |
| Product removal | CropDetailScreen backendLimited; CanonicalFarmService.unlist rejects | Disabled because canonical DELETE physically deletes; original soft HIDDEN semantics lost in live mode deliberately, not removed from demo/reference source. |
| Farmer incoming/crop orders | FarmerOrdersRefreshController/ProductRepository orders; CanonicalFarmService rejects; FarmerScreen backendLimited notices | Disabled/unavailable; never uses canonical general order feeds to simulate seller isolation. Original GET orders/mine remains declared only for legacy mode. |
| Farmer supplies, map/tracking/dashboard extras | FarmerSuppliesScreen/FarmerMapScreen/FarmerTrackingScreen/FarmerDashboardScreen, DemoFarmerShell and demo/DemoRepository | Demo/local state; availability notices in live home for disconnected orders/supplies. No canonical logistics/supplier purchasing APIs in CanonicalBackendApi. A map UI/marker is not backend live tracking. |
| Buyer browsing/cart/checkout/orders | ui/screens/BuyerScreen.kt, CheckoutBottomSheet.kt, OrderTrackingDialog.kt; data/model/Product.kt, Order.kt | Hardcoded products/categories/mockOrders and remember state; no ApiClient/repository networking in BuyerScreen. Checkout does not exercise real payment/order. Preserve source and label as demo until integration approved. |
| Supplier experience | ui/screens/SupplierScreen.kt, data/model/SupplyItem.kt | Mock/local lists and actions; no canonical supplier API repository found. Canonical login can select role UI, but role screen data is not live. |
| Inspector assessment/evidence | ui/screens/InspectorScreen.kt, data/model/InspectionRecord.kt | Sample records, remember checklist/notes/decision state; photo toggles are not file upload. No canonical inspection/evidence/report API interface/repository. UI recommendation is not admin farmer verification. |
| Legacy farmer CRUD | repository/FarmerRepository.kt, viewmodel/FarmerViewModel.kt→FarmApiService farmers/list/search/create/update/delete | Retained legacy declarations; CanonicalFarmService unsupported methods explicit failure. Not live farmer-profile implementation. No standalone reference-URL network client found. |
| Nonfarmer profile modal | ui/components/ProfileModal.kt via AppNavigation | Session/local account display, no complete buyer/supplier/inspector backend profile read/update integration. |

## Contract and lost-capability findings

Canonical DTO files under `data/model/canonical/` and `network/canonical/` preserve Long IDs exactly using strict integer decoding, BigDecimal money, explicit lower enum mappings and reject fractional/overflow stock. R UUID strings remain in domain APIs as strings for compatibility; they cannot be numerically converted. R sector maps explicitly to C seller type, not string substitution throughout the app.

Original phone/email login, atomic signup+role profile, known refresh expiry, server logout, fractional stock, low-stock threshold, soft unlisting and own seller orders are not fully available in canonical mode. Preserving original code does not mean these operations remain usable against C. Android currently handles several limits safely (explicit unavailable/validation/local logout), while missing role workflows remain local demos.

Comments in CanonicalJson.kt and ProductImage.kt still describe earlier transport wiring; actual ApiClient factory uses canonical Gson and bearer-free image transport. Treat executable wiring, not stale comments, as evidence. A future cleanup is documentation-only unless separately approved.

## Tested versus unverified

Previous tests recorded in [05](05-local-demo-verification.md): 138 unit tests, final debug APK/release Kotlin compile and physical canonical request tests. Device tests at `app/src/androidTest/java/com/example/farmapp/network/CanonicalBackendDeviceTest.kt` cover registration/profile/phone/categories/pending product/two images/edit and separate-process persistence+refresh. They do not cover every Compose click, real buyer/supplier/inspector workflows, order checkout, moderation, admin frontend, concurrent stock, secure logout or payment providers.

No new tests were run in this audit because mutation tests could create data and test execution is unnecessary for documentation-only changes. Retirement of R requires all intended original live features integrated or explicitly deferred with team acceptance, fallback builds reviewed, existing data addressed, and cross-client tests. See [09](09-backend-migration-plan.md).
