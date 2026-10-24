# Prompt Log

## PA-12 - Quản lý người dùng

Implement a paginated Vietnamese Manager/Admin user-management page with search by name/email, role and status filters, a user detail page, and account block/unblock actions with a required reason. Enforce management rules in the service: managers cannot view or manage Admin/Manager accounts, users cannot block themselves, and only Admin can manage Manager accounts. Reuse existing User status, block reason, CurrentUser, and authentication behavior. Do not modify User, SecurityConfig, UserDetailsServiceImpl, OrderService, or dependencies.

## PG-03 - Dashboard layout and role sidebars

Create a responsive Thymeleaf dashboard layout with role-specific sidebars for Vendor, Shipper, Manager, and Admin; add role demo pages and endpoints, reusing the existing breadcrumb and toast fragments. Do not change dependencies, security configuration, WebSocket code, or other unrelated files.

## PG-05 - Error pages

Build friendly, responsive Vietnamese 403, 404, and 500 error pages using the shared Thymeleaf layout, inline SVG illustrations, and a dedicated Midnight Rare stylesheet. Render only fixed HTTP codes and safe copy; never expose model-provided exception details or trace IDs.

## PG-06 - Cloudinary upload service and dev test page

Add the Cloudinary SDK and environment-backed configuration, implement validated image/video uploads with magic-byte checks and safe failures, and provide a temporary `/dev/upload` page/controller for manual testing. Do not alter security configuration, WebSocket code, or unrelated backend modules.

## PG-07 - Shared frontend JavaScript utilities

Create a vanilla JavaScript utility module for CSRF-aware JSON fetch, Bootstrap toast and confirmation modal, safe image preview validation, and button loading states. Add a temporary local-profile demo page/controller and the CSRF meta tags required by the shared layout contract.

## PU-12b - Cart controllers and page shell

Read AGENTS.md, SECURITY_SPEC.md, and CONTRACTS.md section 6. Add only the REST cart controller delegating to CartService, the web cart page controller without loading cart data into the model, a minimal cart template using the existing main layout, and MockMvc tests for authorization, validation, CSRF, response shape, and not-found errors. Keep existing SecurityConfig and mock CartService in controller tests.

## PU-12c - Cart page interactions

Reuse common.js fetch/CSRF, toast, and confirmation helpers; build the cart view in cart.js with safe DOM APIs, shop grouping, selection, quantity updates, removal, summaries, loading/empty/error states, and cart:updated events. Add a short add-to-cart JavaScript integration example to CONTRACTS.md. Keep the markup Bootstrap-based and do not add API endpoints or long custom CSS.

## PG-05 - Error pages

Build friendly, responsive Vietnamese 403, 404, and 500 error pages using the shared Thymeleaf layout, inline SVG illustrations, and a dedicated Midnight Rare stylesheet. Render only fixed HTTP codes and safe copy; never expose model-provided exception details or trace IDs.

## PG-06 - Cloudinary upload service and dev test page

Add the Cloudinary SDK and environment-backed configuration, implement validated image/video uploads with magic-byte checks and safe failures, and provide a temporary `/dev/upload` page/controller for manual testing. Do not alter security configuration, WebSocket code, or unrelated backend modules.

## PG-07 - Shared frontend JavaScript utilities

Create a vanilla JavaScript utility module for CSRF-aware JSON fetch, Bootstrap toast and confirmation modal, safe image preview validation, and button loading states. Add a temporary local-profile demo page/controller and the CSRF meta tags required by the shared layout contract.

## PA-11 - Card catalog REST API

Expose the existing Card Catalog service through REST endpoints: allow Vendors to search active cards for listings, and allow Managers/Admins to search, view, create, update, and hide/show cards. Reuse the service's Cloudinary upload and replacement cleanup behavior; do not add a frontend or change shared entities.


## PA-18 - Quản lý nhà vận chuyển

Implement Admin-only shipping carrier CRUD under `/admin/shipping-carriers`, preserving the checkout `ShippingService` contract. Validate and check duplicate carrier name/code, retain fee-threshold calculation, hide carriers referenced by orders and delete unused ones, add Thymeleaf list/form pages and focused service tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. Since `ShippingCarrier` has no `logoPublicId`, document the blocker and do not upload Cloudinary logos until the schema can safely persist the public ID.

## PA-17 - Quản lý hoa hồng

Implement Admin-only commission management under `/admin/commissions`: preserve `CommissionService.getRate(Long shopId)`, prefer a shop override, otherwise use the latest platform rate and fall back to 5.00%; append a new `CommissionSetting` row for each platform-rate change; allow Admins to set or clear a shop-specific override. Validate rates from 0% to 100%, enforce Admin authorization in controller and service, add paginated Vietnamese Thymeleaf pages, and write focused service tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. `CommissionSetting` has no `effective_from`, so document the blocker and make platform changes immediate until the schema is approved.

## PA-13 - Tạo tài khoản Manager và Shipper

Implement Admin-only Manager account creation and Manager/Admin Shipper account creation/listing under `/admin/**` and `/manager/**`. Generate a 12-character SecureRandom temporary password, store only its encoded form, send it synchronously by email so a send failure rolls back the transaction, and require a password change at first login. Show Shipper status and count orders in `SHIPPING`; do not modify shared entities, SecurityConfig, security package, OrderService, dependencies, or unrelated tests. `User` has no region field, so document the blocker and do not accept/discard region input.
## PA-14 - Duyệt và tạm ngừng cửa hàng

Implement Manager/Admin shop review under `/manager/shops`: search/filter/paginate shop applications, view shop/owner details, approve or reject pending applications, suspend approved shops, and reactivate suspended shops. Approval adds `ROLE_VENDOR` without removing `ROLE_USER`; rejection stores a required reason; send the owner a `SHOP_APPROVAL` notification. Enforce Manager/Admin authorization in the controller and service, validate form input, map entities to DTOs, and add focused service and MVC tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. No public product listing service/controller exists yet; preserve approval-status checks in cart and document that public listing filtering must be wired when that module is added.

## PU-11a - Address service and DTO

Implement authenticated-user address listing, CRUD, default selection and checkout lookup. Extend the existing repositories with user-row locking and ownership-scoped queries, normalize validated address fields, enforce the per-user limit and a single default address, and add DTO mapping plus Mockito and validation tests. Do not create a duplicate controller or overwrite existing address code.

## PU-11b - Address controllers and pages

Implement web and read-only REST controllers for authenticated address management, reuse the existing profile navigation and safe return URL resolver, add Bootstrap Thymeleaf address list/form pages and delete confirmation, preserve the existing security rules, and add MockMvc coverage for validation, redirects, CSRF, authorization, 404 ownership handling, and must-change-password behavior.

## PU-13a - Checkout DTOs and money allocation

Add largest-remainder allocation for whole-VND proportional splits, checkout preview request/response DTOs with validation and non-null response lists, and deterministic unit tests including 2,000 seeded property-style cases. Reuse existing checkout-related types and utilities without creating duplicate services or DTOs.

## PU-13b - Checkout preview calculation

Implement a read-only checkout preview service that uses the existing cart, address, coupon and shipping service contracts, rounds unit prices before multiplication, applies shop and platform discounts in order, allocates platform discounts proportionally, and reports blocking issues. Add focused Mockito tests for calculation, service-call counts, validation scenarios and money invariants, then document the shared checkout money rules in CONTRACTS.md.

## PU-14a - Checkout preview endpoint and page shell

Add the checkout preview REST endpoint and checkout page shell without replacing existing code. Restrict `/api/checkout/**` to `ROLE_USER`, document the URL rule, reject duplicate shop ids in the preview service, and add MockMvc/service tests for authorization, CSRF, validation, idempotency token generation, and error handling.

## PU-14b - Checkout preview interface

Complete the checkout page with address selection, per-shop carrier and coupon controls, payment selection, summary totals, loading and retry states, stale-request cancellation, safe DOM rendering, and the shared CSRF/API/toast helpers. Keep order submission as a PU-15 placeholder and do not add or call a write API.

## PA-15 - Quản lý sản phẩm theo shop

Implement Manager/Admin product moderation under `/manager/products`: filter products by shop, type and status, search by product/shop name, and paginate results. Allow removal of a product only with a required reason, persist `ProductStatus.BANNED` and `banReason`, restore a banned product to `ACTIVE`, and notify the shop owner through `NotificationService`. Enforce role authorization in both controller and service, map entities to DTOs, and add focused service and MVC tests. Do not modify shared entities, SecurityConfig, OrderService, security package, dependencies, or unrelated tests. No public product catalog exists yet; retain cart availability checks and document that catalog filtering for banned products must be wired when that module is added.

## PG-10 - Đăng ký shop

Implement the seller shop registration flow for regular users at `/seller-registration` using the existing `Shop` entity, `ShopStatus`, repository, `CloudinaryService`, shared main layout, and `common.js`. Support new applications and resubmission after `REJECTED`, display `SUSPENDED` as a locked shop, and do not modify shared entities, enums, `SecurityConfig`, approval workflows, WebSocket code, dependencies, or unrelated files.

## PG-11 - Quản lý hồ sơ và trang công khai của shop

Add Vendor shop profile editing at `/vendor/shop/manage` for logo, banner, description, phone, and pickup address, reusing the existing Shop schema and CloudinaryService with safe image replacement cleanup. Add a public approved shop page at `/shops/{id}` with paginated active products and ProductType filtering. Keep ProductRepository, ShopRepository, shared entities, SecurityConfig, PA-14, WebSocket code, and dependencies unchanged.

## PG-12 - Danh sách sản phẩm Vendor

Implement the Vendor product list at `/vendor/products` with name search, ProductType/status filters, and pagination. Resolve the authenticated user with CurrentUser, find their Shop, and query only that shop through a dedicated repository projection. Map query results to VendorProductResponse and render a responsive Midnight Rare dashboard table with status badges, empty state, shared pagination, and coming-soon toasts for create/edit/delete placeholders. Keep Product, ProductRepository, ShopRepository, SecurityConfig, dependencies, and unrelated files unchanged.

## UI - Connect implemented navigation routes

Review the shared main/dashboard layouts and role sidebars against the implemented web controllers and SECURITY_SPEC URL map. Route implemented links to their real pages, mark unavailable destinations as coming soon with a shared toast, and leave unrelated controllers, entities, and security configuration unchanged.

## PG-13 - Đăng bán Single Card

Implement the Vendor single-card listing flow at `/vendor/products/new/single`. Reuse the existing CardService catalog search, approved Vendor shop, shared Product/Card entities and enums, and CloudinaryService. Provide a responsive Midnight Rare card search and listing form with validation, image preview/loading, and a shop-scoped duplicate variant check that directs the Vendor to update existing stock. Keep shared entities, repositories, SecurityConfig, dependencies, and unrelated files unchanged.

## PG-14 - Đăng bán hàng niêm phong và phụ kiện

Implement Vendor product creation at `/vendor/products/new/other` for BOOSTER_BOX, BOOSTER_PACK, BUNDLE, and SUPPLY. Reuse the existing Product, Shop, ProductType, CardLanguage, repositories, SeriesMapper, CurrentUser, and CloudinaryService; enforce Vendor ownership and APPROVED shop status, validate fields, and render a responsive Midnight Rare conditional form with image preview/loading. Since Product has no accessory-category column, persist the selected supply category as a line in the product description. Do not modify shared entities, SecurityConfig, dependencies, or unrelated files.

## PG-15 - Chá»‰nh sá»­a, áº©n vÃ  xÃ³a má»m sáº£n pháº©m Vendor

Add an ownership-checked Vendor product edit form at `/vendor/products/{id}/edit`, supporting price, non-negative stock, description, and optional Cloudinary image replacement with old image cleanup after commit. Add CSRF-protected AJAX status toggle and soft-delete endpoints; soft delete changes status to INACTIVE and blocks when the product has orders in processing states. Use the existing Product `@Version` for optimistic locking, return a retry message on conflicts, use a dedicated repository for order checks, and leave Product, shared repositories, SecurityConfig, and dependencies unchanged.

## PG-16 - Khuyến mãi của shop

Implement Vendor shop-promotion list/create/edit/toggle/soft-delete under `/vendor/promotions`, with 1–90% validation, date-order validation, product selection scoped to the authenticated approved shop, ownership checks, and rejection of overlapping active promotion periods for the same product. Implement the existing shared `PromotionService` signatures (`getEffectivePrice(Long)` and `getEffectivePrices(Collection<Long>)`) to combine the best applicable shop discount and platform discount; use bulk effective prices on the Vendor product list. Preserve shared entities, `PromotionRepository`, SecurityConfig, dependencies, and other members' files.

## PG-17 - Mã giảm giá của shop và CouponService

Add Vendor shop-coupon list/create/edit/toggle/deactivate under `/vendor/coupons`, with uppercase unique codes, discount/date/usage validation, ownership checks, and Midnight Rare Thymeleaf pages. Implement the existing shared `CouponService` signatures without changing the contract: invalid coupons return `CouponApplyResponse(valid=false, message=...)`, percentage discounts round down and respect the maximum, and successful order placement records coupon usage and increments usage under a coupon row lock. Preserve shared entities, enums, repositories, SecurityConfig, dependencies, and other members' files; infer coupon scope from `Coupon.shop` as defined in `ENTITY_SPEC.md`.

## UI - TCGplayer-style home page

Replace the current role-action landing page with a responsive TCG marketplace homepage using `layout/main.html`, mock game/product/set data in `HomeController`, local original SVG artwork, Best Sellers and Latest Sets sections, and a detailed shared footer. Preserve role-based quick links and existing homepage shortcut search; do not add dependencies, modify entities/security, or rely on external image URLs.

## UI - Shop registration routing and Midnight Rare pages

Route users without a shop to the seller registration form and all existing shop applications to their status page; provide a rejected-only resubmission URL. Show the correct seller action in the shared header based on shop registration state, redirect authenticated roles to their selected dashboards while preserving safe return URLs and mandatory password changes, and provide a Shipper dashboard using its role sidebar. Restyle the registration and status pages with the shared main layout and Midnight Rare theme. Keep SecurityConfig, entities, dependencies, and unrelated worktree changes untouched; synchronize the redirect table in SECURITY_SPEC.md and add focused tests.

## PA-30 - Trang chủ Guest và User

Wire the TCG homepage to active games and public products from the database. Show at most 12 active products from approved, non-deleted shops with more than 10 sales, ordered by sales descending; add authenticated User sections for their recent viewed products and highly-rated product recommendations. Reuse the existing ViewedProduct data without changing the PG-27 recording contract, preserve the banner and existing layout, and add focused repository, service, and MVC tests.

## PA-31 - Trang theo danh mục

Add a public catalog navigation flow from active Games to their active Series and from each Series to a paginated list of active marketplace products, including singles linked through Cards and sealed products linked directly to Series. Include breadcrumbs, pagination, empty states, approved-shop and soft-delete/status filtering, and a visible Game & Series menu link. Reuse existing Game/Series catalog services and DTOs, do not modify entities or security rules, and add repository, service, and MVC tests.

## PA-32 + PA-33 - Tìm kiếm nâng cao và Top 20

Add a public `/products` discovery page combining validated keyword, Game, Series, rarity, product type, language, condition, and non-negative price range filters with stable URL-preserving pagination and sorting. Use JPA Specification for public product eligibility and dynamic filters. Group SINGLE offers by Card and show the lowest matching price and distinct approved sellers. Add four ranked Top 20 tabs (newest, best-selling, top-rated, favorites), with top-rated entries requiring at least five reviews. Keep public product/shop/card visibility rules, reuse existing catalog services, do not change shared entities, dependencies, or security rules, and cover combined filtering, card aggregation, review threshold, Top 20 caps, URL pagination, and validation in focused tests.

## PG-18 - Vendor orders

Add a paginated Vendor order list with status tabs and order-code search, plus an ownership-checked order detail page containing recipient, payment, snapshot item, total, and status timeline data. Add a CSRF-protected AJAX action endpoint that allows a Vendor to confirm or reject only NEW orders, requires a rejection note, and delegates every state transition to the shared `OrderService.changeStatus(Long, OrderStatus, String)` contract. Keep shared entities, `OrderRepository`, `OrderService`, SecurityConfig, and common.js unchanged; scope every order query to the shop owned by `CurrentUser`.

## PG-19 - Thông báo đơn mới cho Vendor

Listen for each committed `OrderCreatedEvent` and send an `ORDER_NEW` notification to its `shopOwnerId` through the existing `NotificationService` contract. Add a dashboard bell with unread count, recent notification dropdown, and SockJS/STOMP subscription to `/user/queue/notifications`; preserve shared entities, order flow, security configuration, and dependencies.

## PG-20 - Xử lý yêu cầu trả hàng của Vendor

Add a paginated Vendor return-request list scoped to the authenticated shop, with proof-image preview and AJAX accept/reject actions. Delegate all order transitions to `OrderService.changeStatus`, validate the rejection reason, and preserve the shared return entities, repositories, security configuration, and order workflow.
