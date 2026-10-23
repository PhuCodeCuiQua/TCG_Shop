# Prompt Log

## PA-12 - Quản lý người dùng

Implement a paginated Vietnamese Manager/Admin user-management page with search by name/email, role and status filters, a user detail page, and account block/unblock actions with a required reason. Enforce management rules in the service: managers cannot view or manage Admin/Manager accounts, users cannot block themselves, and only Admin can manage Manager accounts. Reuse existing User status, block reason, CurrentUser, and authentication behavior. Do not modify User, SecurityConfig, UserDetailsServiceImpl, OrderService, or dependencies.

## PG-03 - Dashboard layout and role sidebars

Create a responsive Thymeleaf dashboard layout with role-specific sidebars for Vendor, Shipper, Manager, and Admin; add role demo pages and endpoints, reusing the existing breadcrumb and toast fragments. Do not change dependencies, security configuration, WebSocket code, or other unrelated files.
<<<<<<< Updated upstream

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
=======

## PG-05 - Error pages

Build friendly, responsive Vietnamese 403, 404, and 500 error pages using the shared Thymeleaf layout, inline SVG illustrations, and a dedicated Midnight Rare stylesheet. Render only fixed HTTP codes and safe copy; never expose model-provided exception details or trace IDs.

## PG-06 - Cloudinary upload service and dev test page

Add the Cloudinary SDK and environment-backed configuration, implement validated image/video uploads with magic-byte checks and safe failures, and provide a temporary `/dev/upload` page/controller for manual testing. Do not alter security configuration, WebSocket code, or unrelated backend modules.

## PG-07 - Shared frontend JavaScript utilities

Create a vanilla JavaScript utility module for CSRF-aware JSON fetch, Bootstrap toast and confirmation modal, safe image preview validation, and button loading states. Add a temporary local-profile demo page/controller and the CSRF meta tags required by the shared layout contract.

## PA-11 - Card catalog REST API

Expose the existing Card Catalog service through REST endpoints: allow Vendors to search active cards for listings, and allow Managers/Admins to search, view, create, update, and hide/show cards. Reuse the service's Cloudinary upload and replacement cleanup behavior; do not add a frontend or change shared entities.

>>>>>>> Stashed changes

## PA-18 - Quản lý nhà vận chuyển

Implement Admin-only shipping carrier CRUD under `/admin/shipping-carriers`, preserving the checkout `ShippingService` contract. Validate and check duplicate carrier name/code, retain fee-threshold calculation, hide carriers referenced by orders and delete unused ones, add Thymeleaf list/form pages and focused service tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. Since `ShippingCarrier` has no `logoPublicId`, document the blocker and do not upload Cloudinary logos until the schema can safely persist the public ID.
