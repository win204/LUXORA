# LUXORA Project Context

## 1. Stack

- Backend: Java 21 target, Spring Boot 4.0.0, Maven.
- Backend libraries: Spring WebMVC, Spring Data JPA, Validation, Spring Data Redis, Spring Security, Actuator, Liquibase (`spring-boot-starter-liquibase`), AWS SDK v2 (`s3`, `ses`, `sqs`, `sns`).
- Database: Microsoft SQL Server 2022 in Docker for local development.
- Migration / Liquibase: Liquibase starter, master changelog `classpath:db/changelog/db.changelog-master.xml` with 17 modular XML changesets.
- Cloud / AWS Emulation: AWS SDK v2 with LocalStack in Docker Compose (port `4566`) for S3, SES, SQS, SNS.
- Frontend: Next.js 16.3.0, React 19.1, TypeScript 5.9, App Router.
- Frontend tooling: ESLint 9, `eslint-config-next` 16.
- Cache: Redis 8 Alpine in Docker.
- API docs: springdoc OpenAPI starter WebMVC UI 3.0.3.
- Testing: JUnit 5/Spring Boot test, Spring WebMVC test, AssertJ/Mockito via Spring Boot test, H2 for backend tests (236 automated tests passing).
- Local orchestration: Docker Compose.

## 2. Architecture

- Repository root contains `backend/`, `frontend/`, `docker-compose.yml`, env examples, README, and this context file.
- Backend is a modular monolith under base package `com.luxora.commerce`.
- Backend domains currently include `catalog`, `inventory`, `cart`, `auth`, `user`, `checkout`, `order`, `order.shipment`, `payment`, `refund`, `returning`, `admin`, `promotion`, `banner`, and `aws` (`aws.config`, `aws.s3`, `aws.ses`, `aws.event`).
- APIs use DTOs at boundaries; JPA entities are not exposed directly.
- Frontend App Router pages live in `frontend/app`.
- Frontend shared components live in `frontend/components`.
- Frontend API clients and types live in `frontend/lib`.
- SQL Server stores catalog, inventory, user/auth, order, payment, refund, return/RMA, shipment, flash sales, loyalty, banners, and admin-managed metadata.
- Redis stores anonymous and authenticated cart state.
- LocalStack / AWS provides cloud storage (S3), transactional email (SES), and message queuing/pub-sub (SQS/SNS).

## 3. Infrastructure

- Docker Compose services:
  - `sqlserver`: SQL Server 2022, host port `1433`, volume `sqlserver-data`.
  - `redis`: Redis 8 Alpine, host port `6379`, volume `redis-data`.
  - `localstack`: LocalStack (S3, SES, SQS, SNS), host port `4566`, volume `localstack-data`.
  - `backend`: Spring Boot app, host port `8080`.
  - `frontend`: Next.js app, host port `3000`.
- Jenkins CI/CD service (`docker-compose.jenkins.yml`):
  - `jenkins`: Jenkins LTS (JDK 21, Maven 3.9, Node.js 20, Docker CLI), host port `8088`, agent port `50000`, volume `jenkins-data`, mounted `/var/run/docker.sock`.
- Docker internal addresses:
  - Backend to SQL Server: `sqlserver:1433`.
  - Backend to Redis: `redis:6379`.
  - Backend to LocalStack: `http://localstack:4566`.
  - Frontend server-side API base URL: `http://backend:8080`.
- Browser/host addresses:
  - Frontend: `http://localhost:3000`.
  - Backend health: `http://localhost:8080/actuator/health`.
  - LocalStack endpoint: `http://localhost:4566`.
  - Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
  - OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
  - Jenkins UI: `http://localhost:8088`.
- CORS is origin-restricted, environment-driven, and credential-enabled for `/api/v1/**`.
- Do not record passwords or secrets in this file.

## 4. Database

- Database name: `luxora`.
- Liquibase owns schema migrations; Hibernate uses `spring.jpa.hibernate.ddl-auto=validate`.
- Liquibase master changelog: `classpath:db/changelog/db.changelog-master.xml`.
- Changesets (`backend/src/main/resources/db/changelog/changesets/`):
  - `001-catalog-foundation.xml`: SQL Server brands, categories, products, images, specs, variants, inventory.
  - `002-auth-user-foundation.xml`: users, roles, user_roles, refresh_tokens, default roles.
  - `003-align-auth-timestamps.xml`: timestamp alignment.
  - `004-order-foundation.xml`: orders and order_items snapshots.
  - `005-payment-foundation.xml`: payment attempts.
  - `006-order-status-history.xml`: order status transition history.
  - `007-refund-foundation.xml`: refund attempts (with dbms-specific filtered indexes for SQL Server and H2).
  - `008-shipment-foundation.xml`: shipment tracking foundation.
  - `009-return-rma-foundation.xml`: return requests, return items, and return status history.
  - `010-return-shipment-foundation.xml`: return shipment label and tracking foundation.
  - `011-admin-order-operations.xml`: admin operational notes and order queue index.
  - `012-promotion-foundation.xml`: promotions and order promotions snapshots.
  - `013-guest-checkout-foundation.xml`: guest checkout nullable user_id, recipient email index, and unique guest_access_token index.
  - `014-product-reviews-foundation.xml`: customer reviews (1-5 stars, verified purchase badge, status moderation) and review images on AWS S3.
  - `015-loyalty-points-foundation.xml`: loyalty accounts, tier status (SILVER, GOLD, PLATINUM), loyalty transactions audit log, and order points deduction/discount.
  - `016-flash-sale-foundation.xml`: flash sales and flash sale items with quota, sold counter, and countdown scheduling.
  - `017-banner-foundation.xml`: event banners with title, subtitle, image_url, link_url, cta_text, display_order, active flag, date validity ranges, and indexed active ordering.
- Important tables:
  - Catalog/inventory: `brands`, `categories`, `products`, `product_images`, `product_specifications`, `product_variants`, `inventory_items`.
  - Flash sales: `flash_sales`, `flash_sale_items`.
  - Banners: `banners`.
  - Loyalty: `loyalty_accounts`, `loyalty_transactions`.
  - Auth/user: `users`, `roles`, `user_roles`, `refresh_tokens`.
  - Order/payment/fulfillment/returns: `orders`, `order_items`, `order_status_history`, `payments`, `refunds`, `shipments`, `returns`, `return_items`, `return_status_history`, `return_shipments`.
- Important constraints/relationships:
  - Product belongs to brand/category; product slug is unique.
  - ProductVariant is the purchasable SKU; SKU is unique.
  - Inventory has one row per variant.
  - Orders belong to users or guests (with non-null customer_email and secure guest_access_token) and contain immutable order item and shipping address snapshots; admin notes are nullable, capped at 500 characters, and operational only.
  - Payments and refunds belong to orders; refunds also reference payments.
  - Shipment belongs to one order; `shipments.order_id` is unique.
  - Shipment carrier/tracking number are required; `delivered_at` is nullable.
  - Returns belong to an order/user and contain immutable return item snapshots that reference order items.
  - Return status history records RMA transitions; refunds may reference a return.
  - ReturnShipment belongs to one return; `return_shipments.return_id` and `tracking_number` are unique.
  - Refresh tokens are stored hashed, not raw.
- Local catalog seed is profile-gated with `local` profile and `luxora.seed.catalog.enabled`.
- Seeded local products: `AeroPhone X1`, `Studio One`, `Vision Slate`.

## 5. Implemented Features

- Catalog API with brands, categories, products, variants, images, specifications, inventory availability, filters, pagination, and detail by slug.
- Local/dev catalog seed data.
- Centralized JSON API error handling.
- Swagger/OpenAPI UI with Bearer JWT support.
- Premium storefront with homepage, shop, product detail, variant selection, cart, checkout, orders, account, and admin pages.
- Authentication with register, login, HttpOnly refresh cookie rotation, logout, current user, profile update, and password change.
- Anonymous and authenticated Redis-backed carts with TTL and anonymous-cart merge on login via `X-Cart-Id`.
- Authenticated checkout preview with validated shipping address and server-authoritative totals.
- Authenticated order creation from cart, stock decrement, immutable snapshots, cart clear after commit, owner-only detail/history, and pending-order cancellation.
- Local mock payment provider with payment attempts and `PENDING -> PAID` on successful payment.
- VNPay Sandbox Payment Gateway Integration: secure payment URL generation with HMAC-SHA512 checksum, bank code selection, IPN webhook handling with pessimistic locking, amount verification against order grand total, idempotent order transition `PENDING -> PAID`, return URL signature verification, and customer return handling for both authenticated users and guests.
- Admin product read/write foundation protected by `ROLE_ADMIN`.
- Admin Order Operations Hardening protected by `ROLE_ADMIN`: filtered order queue, safe sorting, standalone fulfillment notes, detail, pending cancellation, controlled status transitions, and status history.
- Mock refund-and-cancel for `PAID`/`PROCESSING` orders with deterministic success/failure and restock after successful refund.
- Shipping/Fulfillment Tracking Foundation: one shipment per order, admin shipment creation/update, customer/admin shipment visibility, and delivered timestamp handling.
- External Shipping Carrier Integration (GHN - Giao Hàng Nhanh Sandbox): Provider pattern abstraction (`ShippingProvider`, `MockShippingProvider`, `GhnShippingProvider`), admin 1-click automated waybill generation with tracking portal URL, and webhook handler (`POST /api/v1/shipments/ghn-webhook`) for automated delivery status updates and transition to `DELIVERED`.
- Return/RMA Foundation for delivered orders: customer item-level return requests, admin approval/rejection/receive/refund, status history, receive-time restock, and mock refund retry.
- Return Shipment Tracking Foundation: admin-generated mock return labels, customer mark-shipped, shipment visibility, and receive-time returned-package timestamping.
- Admin Return/RMA Operations Hardening: server-side return queue filtering, safe sorting, compact status views, and standalone operational notes.
- Promotion Foundation: admin-managed percentage/fixed coupons, server-authoritative checkout discount calculation, locked order redemption, and immutable order discount snapshots.
- Guest Checkout Foundation: server-authoritative guest preview, guest order placement from anonymous Redis cart with mandatory recipient email, stock decrement with pessimistic locking, promotion usage, anonymous cart clearance, and cryptographically secure guest access token for authorization of order detail, payment attempts, and pending cancellation without an account.
- Liquibase Migration System: Fully replaced Flyway with Liquibase (`spring-boot-starter-liquibase`), master changelog, and 14 modular XML changesets (`001` through `014`) supporting both SQL Server (production/local) and H2 (in-memory tests).
- Product Reviews & Ratings Foundation: customer 1-5 star ratings, review comments, multi-photo attachments uploaded directly to AWS S3 via presigned URLs, automatic verified purchase verification for delivered orders, real-time rating summary calculations (average rating and 1-5 star breakdown distribution), and full admin moderation queue (`PENDING`, `APPROVED`, `REJECTED`).
- AWS Cloud Suite Foundation (with LocalStack emulation):
  - Amazon S3: `S3StorageService` supporting file uploads, downloads, deletions, and presigned upload URLs (`POST /api/v1/media/presigned-upload-url`).
  - Amazon SES: `EmailService` and `AwsSesEmailService` for transactional emails (order confirmation, shipment notification).
  - Amazon SQS & SNS: `CommerceEventPublisher` and `AwsCommerceEventPublisher` for event-driven asynchronous processing (e.g. `order.created`, `order.paid`, `shipment.created`).

## 6. API

### Catalog

- `GET /api/v1/products`: List active products with filters and pagination.
- `GET /api/v1/products/{slug}`: Get product detail by slug.
- `GET /api/v1/categories`: List active categories.
- `GET /api/v1/brands`: List active brands.

### Media & Cloud Storage (AWS S3)

- `POST /api/v1/media/presigned-upload-url`: Request secure presigned S3 upload URL for product media or review attachments.

### Product Reviews & Ratings

- `GET /api/v1/products/{slug}/reviews`: Paginated approved reviews for a product (public).
- `GET /api/v1/products/{slug}/reviews/summary`: Rating statistics including average score and star distribution (public).
- `POST /api/v1/products/{id}/reviews`: Submit or update review with 1-5 star rating, comment, and AWS S3 photos (authenticated).
- `GET /api/v1/products/{id}/reviews/me`: Return authenticated user's review for a product.
- `GET /api/v1/admin/reviews`: Admin review moderation queue filtered by status (admin only).
- `PATCH /api/v1/admin/reviews/{id}/status`: Admin approve or reject review (admin only).
- `DELETE /api/v1/admin/reviews/{id}`: Admin delete review (admin only).

### Auth/User

- `POST /api/v1/auth/register`: Register, set refresh cookie, return access token/user.
- `POST /api/v1/auth/login`: Login, optionally merge `X-Cart-Id`, set refresh cookie, return access token/user.
- `POST /api/v1/auth/refresh`: Rotate refresh cookie and return new access token/user.
- `POST /api/v1/auth/logout`: Revoke refresh token and clear cookie.
- `GET /api/v1/users/me`: Return authenticated profile.
- `PATCH /api/v1/users/me`: Update first/last name.
- `POST /api/v1/users/me/password`: Change password and revoke refresh sessions.

### Cart

- `GET /api/v1/cart`: Get authenticated or anonymous cart.
- `POST /api/v1/cart/items`: Add product variant to cart.
- `PATCH /api/v1/cart/items/{itemId}`: Update cart quantity.
- `DELETE /api/v1/cart/items/{itemId}`: Remove cart item.
- `DELETE /api/v1/cart`: Clear cart.

### Checkout/Order/Payment

- `POST /api/v1/checkout/preview`: Return validated server-authoritative checkout preview for authenticated user or guest (via `X-Cart-Id` header and validated email); does not create order/reserve stock/process payment.
- `POST /api/v1/orders`: Create pending order from authenticated user cart or guest cart via `X-Cart-Id` header and recipient email; returns order with `guestAccessToken` for guest checkout.
- `GET /api/v1/orders`: Paginated owner-only order history.
- `GET /api/v1/orders/{id}`: Safe order detail for owner or guest (authorized via `X-Guest-Access-Token` header or `?token=` parameter), including shipment info when present.
- `POST /api/v1/orders/{id}/cancel`: Idempotent pending-order cancellation with restock for owner or guest (authorized via token).
- `POST /api/v1/orders/{id}/payments`: Create payment attempt from persisted order total/currency for owner or guest (authorized via token); accepts optional `provider` (`MOCK` or `VNPAY`), `bankCode`, and `returnUrl`. For VNPay, generates signed sandbox redirect URL in `paymentUrl`.
- `GET /api/v1/orders/{id}/payments/latest`: Return latest payment attempt for owner or guest (authorized via token).
- `GET /api/v1/payments/vnpay-ipn`: Server-to-server IPN webhook endpoint from VNPay; validates HMAC-SHA512 checksum, verifies order grand total, idempotently updates payment and transitions order `PENDING -> PAID`, returns `{ "RspCode": "00", "Message": "Confirm Success" }` (publicly accessible).
- `GET /api/v1/payments/vnpay-return`: Browser return redirect endpoint from VNPay; validates HMAC-SHA512 checksum and returns verification status (publicly accessible).

### Admin

- `GET /api/v1/admin/dashboard`: Operational counts.
- `GET /api/v1/admin/products`: Paginated admin product summaries.
- `POST /api/v1/admin/products`: Create product.
- `GET /api/v1/admin/products/{id}`: Admin product detail.
- `PATCH /api/v1/admin/products/{id}`: Update product.
- `POST /api/v1/admin/products/{id}/variants`: Add variant/SKU with inventory.
- `PATCH /api/v1/admin/variants/{id}`: Update variant.
- `PATCH /api/v1/admin/inventory/{variantId}`: Update inventory quantity.
- `POST /api/v1/admin/products/{id}/images`: Add image URL.
- `DELETE /api/v1/admin/products/{id}/images/{imageId}`: Remove image.
- `POST /api/v1/admin/products/{id}/specifications`: Add specification.
- `DELETE /api/v1/admin/products/{id}/specifications/{specId}`: Remove specification.
- `GET /api/v1/admin/orders`: Paginated admin order queue with status, order, customer, tracking, date, and safe sort filters.
- `GET /api/v1/admin/orders/{id}`: Safe admin order detail, including payment/refund/shipment/history and operational note.
- `PATCH /api/v1/admin/orders/{id}/note`: Set or clear a standalone trimmed fulfillment note without changing status.
- `GET /api/v1/admin/promotions`: Paginated promotion list with active/search filters.
- `GET/POST/PATCH /api/v1/admin/promotions/{id}`: Admin promotion detail/create/update operations.
- `POST /api/v1/admin/orders/{id}/cancel`: Admin idempotent pending-order cancellation with restock.
- `POST /api/v1/admin/orders/{id}/refund-and-cancel`: Mock refund and cancel for paid/processing orders.
- `PATCH /api/v1/admin/orders/{id}/status`: Controlled admin transition; currently `PAID -> PROCESSING` and `SHIPPED -> DELIVERED`.
- `POST /api/v1/admin/orders/{id}/shipment`: Create shipment and transition `PROCESSING -> SHIPPED`; supports automated waybill generation via `provider: "GHN"` or `"MOCK"`, or manual `carrier` and `trackingNumber`.
- `POST /api/v1/shipments/ghn-webhook`: Carrier webhook endpoint receiving order status updates from GHN Open API v2 (verifies `X-GHN-Token`, updates `shipments.delivered_at`, and transitions order `SHIPPED -> DELIVERED`, publicly accessible).
- `PATCH /api/v1/admin/orders/{id}/shipment`: Update carrier/tracking while order is `SHIPPED`.
- `GET /api/v1/admin/returns`: Paginated admin return queue with status, order, customer email, tracking, date, and safe sort filters.
- `PATCH /api/v1/admin/returns/{id}/note`: Set or clear a standalone trimmed operational note without changing RMA status.
- `GET /api/v1/admin/returns/{id}`: Admin return detail with items and status history.
- `POST /api/v1/admin/returns/{id}/approve`: Approve requested quantities.
- `POST /api/v1/admin/returns/{id}/reject`: Reject a requested return.
- `POST /api/v1/admin/returns/{id}/receive`: Mark approved quantities received and restock once; requires return shipment metadata.
- `POST /api/v1/admin/returns/{id}/shipping-label`: Generate or return an idempotent mock return label for approved returns.
- `POST /api/v1/admin/returns/{id}/mark-received`: Shipment-aware receive endpoint; stores received timestamp and restocks once.
- `POST /api/v1/admin/returns/{id}/refund`: Mock refund received return quantities from order item snapshots.
- `GET /api/v1/admin/analytics/overview`: High-level business intelligence KPIs (total gross revenue excluding cancelled orders, today revenue, this month revenue, AOV, completed orders count, completion rate, customer count, new customer count, orders by status breakdown map, customers by loyalty tier map). Requires `ROLE_ADMIN`.
- `GET /api/v1/admin/analytics/timeline?period={7d|30d|12m}`: Zero-filled date/month timeline of revenue and order volume for executive charting. Requires `ROLE_ADMIN`.
- `GET /api/v1/admin/analytics/top-products?limit={n}`: Ranked best-selling products by quantity sold and revenue. Requires `ROLE_ADMIN`.
- `GET /api/v1/flash-sales/active`: Fetch current active flash sale campaign with items, discount percentage, claimed quota percentage, and countdown seconds remaining.
- `GET /api/v1/flash-sales/{id}`: Fetch flash sale by ID.
- `GET /api/v1/admin/flash-sales`: List all flash sale campaigns for admin inspection. Requires `ROLE_ADMIN`.
- `GET /api/v1/admin/flash-sales/{id}`: Get flash sale campaign details. Requires `ROLE_ADMIN`.
- `POST /api/v1/admin/flash-sales`: Create a new flash sale campaign with allocated quota and flash pricing. Requires `ROLE_ADMIN`.
- `POST /api/v1/admin/flash-sales/{id}/cancel`: Cancel an active or upcoming flash sale campaign. Requires `ROLE_ADMIN`.

### Platform

- `GET /actuator/health`: Backend health.
- `GET /v3/api-docs`: OpenAPI JSON.
- `GET /swagger-ui/index.html`: Swagger UI.

## 7. Frontend

- Routes:
  - `/`, `/shop`, `/products/[slug]`.
  - `/cart`, `/checkout`, `/orders/[id]`.
  - `/login`, `/register`, `/account`, `/account/orders`, `/account/returns`, `/account/returns/[id]`.
  - `/admin`, `/admin/flash-sales`, `/admin/products`, `/admin/products/new`, `/admin/products/[id]`, `/admin/orders`, `/admin/orders/[id]`, `/admin/returns`, `/admin/returns/[id]`, `/admin/reviews`.
- `/orders/[id]` shows order details, payment action for pending orders (supporting both instant Mock payment and redirect to VNPay Sandbox gateway), return URL response processing (`vnp_ResponseCode`), pending cancellation, customer-visible shipment tracking, and return request form when eligible; supports both authenticated users and guests with access token.
- `/admin/orders` provides server-driven status views, customer/order/tracking/date filters, safe sort controls, pagination, and compact operational summaries.
- `/admin/orders/[id]` shows customer/order/payment/refund/status history, a separately saved operational note, pending cancellation, refund-and-cancel, shipment form for processing orders, shipment edit for shipped orders, and delivered timestamp for delivered orders.
- `/account/returns` and `/account/returns/[id]` show customer return history/detail, eligible cancellation, mock return label/tracking, and mark-shipped action.
- `/admin/returns` provides status views, customer/order/tracking/date filters, controlled sort, pagination, and compact operational links.
- `/checkout` supports both authenticated users and guests (with validated email input); accepts an optional coupon code, flash sale automated pricing, and renders backend-authoritative discount/totals.
- `/admin` renders the Executive Business Intelligence & Revenue Dashboard (`AdminDashboardView`) featuring gross/today/month revenue KPIs, average order value, order completion rate, customer growth, interactive SVG revenue & order volume time series (7d/30d/12m) with hover data inspection, order fulfillment segmented pipeline bar, best-selling products list, and loyalty tier membership breakdown.
- `/admin/flash-sales` provides flash sale campaign list, launch modal, quota tracking, and cancellation controls (`AdminFlashSalesView`).
- Important shared components: `Navbar`, `Footer`, `Container`, `Button`, `ProductCard`, `ProductGrid`, `Price`, `EmptyState`, `ErrorState`, `Skeleton`, `ProductVariantSelector`, `CartView`, `CheckoutView`, `OrderDetailsView`, `AdminShell`, `AdminDashboardView`, `AdminFlashSalesView`, `FlashSaleSection`, `FlashSaleCountdown`, `AdminOrderDetailsView`, `AdminProductForm`, `ReturnRequestPanel`, `AccountReturnsView`, `AccountReturnDetailsView`, `AdminReturnsView`, `AdminReturnDetailsView`.
- Frontend does not store refresh tokens in localStorage, sessionStorage, or readable cookies.
- Access token lives in runtime auth state and is restored through `POST /api/v1/auth/refresh`.
- Anonymous cart id remains in localStorage and is sent as `X-Cart-Id` during checkout and login.
- Guest access token is stored in `sessionStorage` upon order creation and attached as `X-Guest-Access-Token` or query parameter `token` for subsequent guest order view/payment/cancellation.

## 8. Tests
- Backend integration/unit coverage exists for catalog, auth, profile/password, cart, checkout preview, order creation/history/cancellation, mock and VNPay payment, admin products/orders/analytics, flash sales, refunds, shipment fulfillment, returns/RMA, Liquibase migrations, and AWS suite (S3, SES, SQS/SNS).
- Flash sale tests (`FlashSaleApiIntegrationTests`) cover 401/403 authorization, active sale discovery with countdown calculation, checkout preview flash pricing application, order creation atomic sold quota increment, and campaign cancellation.
- Shipment tests cover creation from `PROCESSING`, transition to `SHIPPED`, history creation, invalid status, validation, duplicate shipment, tracking update while `SHIPPED`, admin authorization, customer shipment visibility, ownership isolation, `DELIVERED` setting `deliveredAt`, and delivery rejection without shipment.
- Return/RMA tests cover eligibility, ownership, return window, invalid/over quantities, partial returns, approve/reject/customer cancel, mock label generation, customer mark-shipped, receive shipment requirement, receive restock, refund failure/retry/success, authorization, idempotency around label/shipped/receive/refund, and admin queue status/customer/order/tracking/date filters, pagination, sorting, and notes.
- Admin analytics tests (`AdminAnalyticsApiIntegrationTests`) cover 401 unauthenticated, 403 non-admin, gross revenue calculation excluding cancelled orders, zero-filled 7-day timeline trajectory, and top-selling products rank.
- Admin order tests cover status/customer/order/tracking/date filters, pagination, constrained sorting, summary metadata, note save/clear, and endpoint authorization.
- Guest checkout tests cover server-authoritative guest preview, guest order placement from anonymous Redis cart with mandatory recipient email, stock decrement with pessimistic locking, promotion usage, anonymous cart clearance, guest token access authorization for order detail, payment attempts, and pending cancellation.
- VNPay payment tests cover URL generation with sorted query params and HMAC-SHA512 checksum, valid IPN webhook with idempotent order `PAID` transition, invalid IPN checksum rejection, order total amount verification, tamper detection, and return signature validation.
- Liquibase tests cover changelog verification and all 32 changesets execution on startup.
- AWS suite tests cover S3 storage operations, presigned upload URLs, SES transactional email dispatch, Admin SES test endpoint, and SQS/SNS commerce event publishing.
- Latest backend status on 2026-10-21 after AWS SES Email Notifications: `mvn test` passed (**231 tests, 0 failures, 0 errors, 0 skipped**).
- Latest frontend status on 2026-10-21: `npm run lint` passed (0 errors, 0 warnings); 18 static/dynamic routes.
- Latest Docker/runtime status on 2026-08-21 after Admin Order Operations Hardening: SQL Server and Redis healthy, backend/frontend running, health/Swagger/admin order pages returned 200.

## 9. Important Decisions

- Preserve modular monolith architecture; do not split into microservices yet.
- Liquibase is the single database schema manager (`spring-boot-starter-liquibase`); keep Hibernate `ddl-auto=validate`.
- Changesets are XML-based, strictly numbered, and partitioned with `dbms` preconditions where SQL Server and H2 diverge (e.g. filtered indexes).
- AWS SDK v2 is integrated for S3, SES, SQS, and SNS, backed by LocalStack in local development and Docker Compose. S3 presigned URLs avoid routing large media payloads through backend memory..
- Use DTOs and validation; never expose JPA entities through APIs.
- Backend owns price, subtotal, shipping fee, tax, discount, and grand total calculations.
- ProductVariant is the purchasable SKU; cart and order items reference variants.
- Inventory quantity is internal; public APIs expose availability rather than raw stock.
- Checkout preview validates stock but does not reserve/decrement stock.
- Order creation decrements inventory using pessimistic locking and immutable snapshots.
- Payment uses persisted order total/currency and does not modify stock or cart.
- Refund uses persisted payment/order amount/currency and restocks only after successful mock refund-and-cancel.
- Pending cancellation is idempotent and restocks exactly once.
- `PROCESSING -> SHIPPED` requires shipment creation; `SHIPPED -> DELIVERED` requires existing shipment and sets `deliveredAt`.
- Returns use dedicated status, not `OrderStatus`; order remains `DELIVERED` through return/RMA.
- Return eligibility requires owner, `DELIVERED`, shipment `deliveredAt`, and `luxora.returns.window` currently defaulting to `P14D`.
- Return requests and approvals do not restock or refund; `APPROVED -> RECEIVED` restocks received quantities exactly once.
- Return refunds are allowed only from `RECEIVED`, use immutable order item unit prices, and retry after mock failure without duplicate successful refunds.
- Return shipment metadata is one shipment per return for now; label generation is deterministic/idempotent and does not change ReturnStatus.
- Approved returns must have return shipment metadata before admin receive; customer mark-shipped and admin receive timestamps are idempotent.
- Admin RMA queue filtering is server-side. Sorting is intentionally constrained to `requestedAt` or `updatedAt` with `asc`/`desc`; summary relation data is fetched for the selected page to avoid N+1 query behavior.
- Operational notes reuse `returns.admin_note`, are capped at 500 characters, trim whitespace, clear when blank, and update independently of lifecycle transitions.
- Admin order queue filtering is server-side. Sorting is intentionally constrained to `createdAt` or `updatedAt` with `asc`/`desc`; summary relation data is fetched for the selected page to avoid N+1 query behavior.
- Operational order notes use `orders.admin_note`, are capped at 500 characters, trim whitespace, clear when blank, update `updatedAt`, and never change order status.
- Shipment metadata is one shipment per order for now; no external shipping provider is integrated.
- Admin APIs are backend-enforced with `ROLE_ADMIN`; frontend role checks are UX only.
- Refresh tokens are stored hashed in SQL Server and sent only as HttpOnly cookies.
- Redis cart TTL is configurable with `luxora.cart.ttl` and currently defaults to `PT24H`.
- Docker-to-Docker calls use service names; browser calls use host-accessible URLs.
- Keep production schema migrations separate from local/dev seed data.
- Avoid hardcoded secrets.
- Promotion redemption is recalculated from the cart on preview/order creation; order creation locks the promotion before incrementing usage, and payment always uses persisted discounted grand total.
- Guest checkout uses anonymous Redis cart (`anon:<cartId>`), server-authoritative calculations, email validation, and a 64-character hex guest access token. Token is required for guest order read, payment, and cancel operations. Anonymous cart is cleared upon order creation.
- VNPay Sandbox integration uses HMAC-SHA512 checksum signing over alphabetically sorted query parameters with URL encoding according to VNPay specifications. Amount is server-authoritative from `order.getGrandTotal()` multiplied by 100 as integer VND without cents. Concurrency on IPN webhooks is handled with pessimistic write locking on `Order` and payment state checks before transition to `PAID`. Webhook and return endpoints are publicly accessible via Spring Security.
- Shipping provider abstraction decouples order lifecycle from carrier implementations (`ShippingProvider` interface with `MockShippingProvider` as `@Primary` and `GhnShippingProvider`). GHN integration connects via `RestClient` to GHN Shiip Open API v2 (`/v2/shipping-order/create`), generates tracking code, resolves tracking portal URL (`https://donhang.ghn.vn/?order_code=...`), and handles webhook status transitions safely (`POST /api/v1/shipments/ghn-webhook`).
- Seed data architecture isolates baseline seed data (`CatalogSeedDataLoader` for integration tests with 3 products) from extended rich catalog data (`ExtendedCatalogSeedDataLoader` with 19 products, active under `local` profile with `luxora.seed.catalog.extended=true`). `UserSeedDataLoader` seeds default users (`admin@luxora.com`, `customer@luxora.com`) and promotions (`LUXORA10`, `WELCOME50`). Standalone idempotent SQL script `backend/src/main/resources/db/seed_data.sql` enables direct seeding in SQL Server Management Studio (SSMS).
- Loyalty Points & Tiers System:
  - Liquibase changeset `015-loyalty-points-foundation.xml` manages `loyalty_accounts` and `loyalty_transactions` with indexed user and tier columns, and adds `loyalty_points_redeemed` and `loyalty_discount` to `orders`.
  - Membership tiers: SILVER (1.0x multiplier, threshold 0), GOLD (1.25x multiplier, threshold 500 lifetime points), PLATINUM (1.5x multiplier + free shipping, threshold 2,000 lifetime points).
  - Point conversion rate: 100 points = $1.00 USD. Redemption discount is capped at 50% of the order subtotal.
  - Lifecycle integration: points are deducted at order creation via pessimistic locking (`findByUserIdForUpdate`), restored automatically upon pending order cancellation, and awarded upon order reaching `DELIVERED` (idempotently verified via `existsByAccountIdAndOrderIdAndType`).
  - International payment gateways (Stripe, PayPal) are intentionally skipped per user instruction.
- Admin Analytics & Revenue Dashboard:
  - Revenue calculation boundaries: only active/paid qualifying order statuses (`PAID`, `PROCESSING`, `SHIPPED`, `DELIVERED`) contribute toward revenue, today revenue, month revenue, and average order value (AOV). `CANCELLED` and `PENDING` orders are strictly excluded from revenue aggregates.
  - Interactive SVG time-series: zero-external-dependency SVG area charting with linear gradient fill and coordinate math ensures zero hydration mismatches and rapid rendering across 7-day, 30-day, and 12-month periods.
  - Timeline time-series: server generates a continuous zero-filled chronological date/month sequence so graphs always show complete trends even when days have zero sales.
- Flash Sales & Real-Time Countdown Timer:
  - Liquibase changeset `016-flash-sale-foundation.xml` manages `flash_sales` and `flash_sale_items` with indexed status/time ranges and allocated vs sold quotas.
  - Pricing integration: `CheckoutCalculationService` automatically identifies active flash sales for cart variants/products with remaining quota (`sold_quantity < stock_quantity`), substituting the flash sale price as authoritative unit price.
  - Concurrency & Quota: `OrderService` atomically increments `sold_quantity` via pessimistic locking when orders are placed.
  - Real-time client countdown ticker (`FlashSaleCountdown`) computes hours/minutes/seconds remaining, triggering immediate refresh when sale expires.
  - Frontend components: `FlashSaleSection` with luxury gold & emerald aesthetic, "% Claimed" progress bar, and admin management view `/admin/flash-sales`.
- AWS SES Automated Transactional Email Notifications:
  - Luxury Responsive Templates: Custom emerald (`#112623`) and warm gold (`#c5a059`) HTML layouts with clean typography, inline CSS compatibility, and mobile responsiveness.
  - Order Confirmation & Tax Invoice: Dispatched on transaction commit (`afterCommit`) in `OrderService.createOrder` for both authenticated and guest checkouts, rendering itemized line items, variants, promotional/loyalty discounts, tax, shipping, and shipping address.
  - Shipment & Tracking Dispatch: Dispatched in `ShipmentService.createShipment` when order transitions to `SHIPPED`, including carrier name, monospace waybill tracking number, and live tracking portal link (`https://donhang.ghn.vn/?order_code=...` for GHN).
  - Loyalty Tier Ascension: Automatically dispatched in `LoyaltyServiceImpl.awardPointsOnDelivery` and `LoyaltyServiceImpl.adjustPoints` when lifetime points cross tier boundaries (e.g., promoted to `GOLD` or `PLATINUM`), highlighting new earning multipliers and free shipping privileges.
  - Admin Diagnostic Endpoint: `POST /api/v1/admin/emails/test` allowing administrators to trigger and verify SES delivery across all template types (`ORDER_CONFIRMATION`, `SHIPMENT`, `TIER_UPGRADE`, `GENERIC`).
  - Resilient Fault Tolerance: SES client network or credential exceptions are caught and logged as warnings without disrupting commerce flows or database transactions.
- Event Banners & Voucher Codes Auto-Apply on Checkout:
  - Liquibase changeset `017-banner-foundation.xml` manages the `banners` table (`id`, `title`, `subtitle`, `image_url`, `link_url`, `cta_text`, `display_order`, `active`, `starts_at`, `ends_at`, `created_at`, `updated_at`) with indexed active display ordering.
  - Public & Admin API Endpoints: `GET /api/v1/banners` (active banners within current date window) and `/api/v1/admin/banners/**` (full CRUD, status toggling, and pagination guarded by `ROLE_ADMIN`).
  - Intelligent Voucher Recommendations: `GET /api/v1/promotions/available?subtotal=...` evaluates all active promotion codes against order subtotal, calculating `eligible`, potential `discountEstimate`, `amountNeeded` to unlock, and flags the single highest-value voucher as `isBestDeal`.
  - Server-Authoritative Auto-Apply: `CheckoutPreviewRequest` and `createOrder` support `autoApplyBestVoucher`. If no specific code is provided and `autoApplyBestVoucher=true`, the system automatically resolves the highest-saving eligible voucher.
  - Homepage Hero Carousel (`HeroBannerCarousel.tsx`): Luxury responsive showcase with auto-advance, vignette overlays, metallic gold badge accents, custom CTA links, manual chevron navigation, and dot indicators.
  - Admin Banner Management (`AdminBannersView.tsx` at `/admin/banners`): Interactive portal with instant live image preview, modal for creating and editing banners, one-click active status toggle, and delete confirmation.
  - Checkout Voucher Selection (`CheckoutView.tsx`): Dedicated "Ưu đãi & Voucher LUXORA" section with "Tự động chọn ưu đãi tốt nhất" toggle, best deal gold badges, one-click code apply chips, and unlock requirement notices.
- Product Image Management & Admin-Only Security:
  - Full CRUD operations: `POST /api/v1/admin/products/{id}/images` (add image), `PATCH /api/v1/admin/products/{id}/images/{imageId}` (update url, alt text, display order), `DELETE /api/v1/admin/products/{id}/images/{imageId}` (remove image).
  - Security & RBAC: Spring Security strictly enforces `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`. Non-admin customers or unauthenticated users receive HTTP 403 Forbidden / 401 Unauthorized if attempting any mutation.
  - Storefront Real-Time Synchronization: Customer-facing product cards on `/` and `/shop` render `product.imageUrl` with smooth zoom effects, and `/products/[slug]` fetches fresh product details (`force-dynamic`). When an admin adds, edits, or deletes product images, the storefront reflects the update immediately without stale caching.
  - Interactive Admin Image UI (`AdminProductForm.tsx`): Provides visual thumbnails (64x64px), inline editing mode for URL, alt text, and display order, one-click image deletion, new image addition, and "Admin Privileges Only" security badge.
- Mandatory Member Login for Checkout: Storefront checkout flow enforces active user authentication. Unauthenticated shoppers viewing cart/checkout are guarded and routed to login/register with redirect back to checkout and automatic anonymous cart merge via X-Cart-Id.
- Luxury Aesthetic & Presentation Readiness:
  - Refactored storefront theme to Haute Couture Cashmere Beige (`#f7f5f0`) & Alabaster White (`#ffffff`) with champagne gold accents (`#b38a38`) and Cormorant Garamond serif headings.
  - Created comprehensive presentation & demo walkthrough guide (`DEMO_REPORT_GUIDE.md`) with 5-7 minute step-by-step presentation script, demo accounts, VNPay Sandbox test card details, architecture review, and top 5 defense Q&A.

## 10. Not Implemented Yet

- Additional shipping carrier providers (e.g. GHTK, ViettelPost, FedEx) and production courier contracts.
- International payment gateways (Stripe, PayPal) [skipped per user request].
- Gift cards and automated promotion scheduling.
- OAuth2 social login (Google, GitHub) [deferred by user request].
- Kubernetes manifests.
- Full Prometheus/Grafana/OpenTelemetry observability stack.

## 11. Known Issues / Technical Debt

- Git commands may report dubious ownership for `E:/LUXORA`; use a one-off safe.directory override when needed.
- A stale local `frontend/.next` directory has Windows permission/lock issues; verified builds use `.next-codex`, which is ignored.
- Cart persistence is Redis-only; authenticated carts expire by Redis TTL.
- Runtime verification has created local development users/orders/payments/refunds/shipments/returns/return shipments/status history and promotions.
- Promotion Foundation needs dedicated automated coverage for all validation and concurrency cases; existing backend regression tests pass.

## 12. Next Task

- Option 1 (Recommended): Thực hiện diễn tập Demo thử theo file `DEMO_REPORT_GUIDE.md` trước khi báo cáo.
- Option 2: Tích hợp xuất hóa đơn PDF điện tử chuẩn VAT (PDF Invoice download for orders).
- Option 3: Tối ưu hóa hiệu năng & Cache nâng cao với Redis.

## Future Maintenance Rule

- At the end of every future development task, update this file only if architecture, APIs, database, features, infrastructure, known issues, or next task changed.
- Future agents should read this file first and inspect only files directly relevant to the requested task.

