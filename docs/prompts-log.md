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

## PA-15 - Quản lý sản phẩm theo shop

Implement Manager/Admin product moderation under `/manager/products`: filter products by shop, type and status, search by product/shop name, and paginate results. Allow removal of a product only with a required reason, persist `ProductStatus.BANNED` and `banReason`, restore a banned product to `ACTIVE`, and notify the shop owner through `NotificationService`. Enforce role authorization in both controller and service, map entities to DTOs, and add focused service and MVC tests. Do not modify shared entities, SecurityConfig, OrderService, security package, dependencies, or unrelated tests. No public product catalog exists yet; retain cart availability checks and document that catalog filtering for banned products must be wired when that module is added.

## PG-10 - Đăng ký shop

Implement the seller shop registration flow for regular users at `/seller-registration` using the existing `Shop` entity, `ShopStatus`, repository, `CloudinaryService`, shared main layout, and `common.js`. Support new applications and resubmission after `REJECTED`, display `SUSPENDED` as a locked shop, and do not modify shared entities, enums, `SecurityConfig`, approval workflows, WebSocket code, dependencies, or unrelated files.
