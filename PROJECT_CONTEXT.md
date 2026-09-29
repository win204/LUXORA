# LUXORA Project Context

## 1. Stack

- Backend: Java 21 target, Spring Boot 4.0.0, Maven.
- Backend libraries: Spring WebMVC, Spring Data JPA, Validation, Spring Data Redis, Spring Security, Actuator.
- Database: Microsoft SQL Server 2022 in Docker for local development.
- JDBC/Flyway: Microsoft SQL Server JDBC driver, Flyway Core, Spring Boot Flyway integration, Flyway SQL Server module.
- Frontend: Next.js 16.3.0, React 19.1, TypeScript 5.9, App Router.
- Frontend tooling: ESLint 9, `eslint-config-next` 16.
- Cache: Redis 8 Alpine in Docker.
- API docs: springdoc OpenAPI starter WebMVC UI 3.0.3.
- Testing: JUnit 5/Spring Boot test, Spring WebMVC test, AssertJ/Mockito via Spring Boot test, H2 for backend tests.
- Local orchestration: Docker Compose.

## 2. Architecture

- Repository root contains `backend/`, `frontend/`, `docker-compose.yml`, env examples, README, and this context file.
- Backend is a modular monolith under base package `com.luxora.commerce`.
- Backend domains currently include `catalog`, `inventory`, `cart`, `auth`, `user`, `checkout`, `order`, `order.shipment`, `payment`, `refund`, `returning`, `admin`, and `promotion`.
- APIs use DTOs at boundaries; JPA entities are not exposed directly.
- Frontend App Router pages live in `frontend/app`.
- Frontend shared components live in `frontend/components`.
- Frontend API clients and types live in `frontend/lib`.
- SQL Server stores catalog, inventory, user/auth, order, payment, refund, return/RMA, shipment, and admin-managed metadata.
- Redis stores anonymous and authenticated cart state.

## 3. Infrastructure

- Docker Compose services:
  - `sqlserver`: SQL Server 2022, host port `1433`, volume `sqlserver-data`.
  - `redis`: Redis 8 Alpine, host port `6379`, volume `redis-data`.
  - `backend`: Spring Boot app, host port `8080`.
  - `frontend`: Next.js app, host port `3000`.
- Jenkins CI/CD service (`docker-compose.jenkins.yml`):
  - `jenkins`: Jenkins LTS (JDK 21, Maven 3.9, Node.js 20, Docker CLI), host port `8088`, agent port `50000`, volume `jenkins-data`, mounted `/var/run/docker.sock`.
- Docker internal addresses:
  - Backend to SQL Server: `sqlserver:1433`.
  - Backend to Redis: `redis:6379`.
  - Frontend server-side API base URL: `http://backend:8080`.
- Browser/host addresses:
  - Frontend: `http://localhost:3000`.
  - Backend health: `http://localhost:8080/actuator/health`.
  - Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
  - OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
  - Jenkins UI: `http://localhost:8088`.
- CORS is origin-restricted, environment-driven, and credential-enabled for `/api/v1/**`.
- Do not record passwords or secrets in this file.

## 4. Database

- Database name: `luxora`.
- Flyway owns schema migrations; Hibernate uses `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway location: `classpath:db/migration`.
- Current migrations:
  - `V1__init.sql`: SQL Server catalog/inventory schema.
  - `V2__auth_user_foundation.sql`: users, roles, user_roles, refresh_tokens, default roles.
  - `V3__align_auth_timestamps_with_sql_server.sql`: SQL Server timestamp alignment.
  - `V4__order_foundation.sql`: orders and order_items snapshots.
  - `V5__payment_foundation.sql`: mock payment attempts.
  - `V6__order_status_history.sql`: order status transition history.
  - `V7__refund_foundation.sql`: mock refund attempts.
  - `V8__shipment_foundation.sql`: one shipment per order with carrier/tracking timestamps.
  - `V9__return_rma_foundation.sql`: delivered-order return/RMA requests, item quantities, status history, and refund association.
  - `V10__return_shipment_foundation.sql`: one return shipment per return with mock label/tracking metadata.
  - `V11__admin_order_operations.sql`: order operational notes, update timestamps, and queue index.
  - `V12__promotion_foundation.sql`: promotions plus immutable order promotion snapshots.
- Important tables:
  - Catalog/inventory: `brands`, `categories`, `products`, `product_images`, `product_specifications`, `product_variants`, `inventory_items`.
  - Auth/user: `users`, `roles`, `user_roles`, `refresh_tokens`.
  - Order/payment/fulfillment/returns: `orders`, `order_items`, `order_status_history`, `payments`, `refunds`, `shipments`, `returns`, `return_items`, `return_status_history`, `return_shipments`.
- Important constraints/relationships:
  - Product belongs to brand/category; product slug is unique.
  - ProductVariant is the purchasable SKU; SKU is unique.
  - Inventory has one row per variant.
  - Orders belong to users and contain immutable order item and shipping address snapshots; admin notes are nullable, capped at 500 characters, and operational only.
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
- Admin product read/write foundation protected by `ROLE_ADMIN`.
- Admin Order Operations Hardening protected by `ROLE_ADMIN`: filtered order queue, safe sorting, standalone fulfillment notes, detail, pending cancellation, controlled status transitions, and status history.
- Mock refund-and-cancel for `PAID`/`PROCESSING` orders with deterministic success/failure and restock after successful refund.
- Shipping/Fulfillment Tracking Foundation: one shipment per order, admin shipment creation/update, customer/admin shipment visibility, and delivered timestamp handling.
- Return/RMA Foundation for delivered orders: customer item-level return requests, admin approval/rejection/receive/refund, status history, receive-time restock, and mock refund retry.
- Return Shipment Tracking Foundation: admin-generated mock return labels, customer mark-shipped, shipment visibility, and receive-time returned-package timestamping.
- Admin Return/RMA Operations Hardening: server-side return queue filtering, safe sorting, compact status views, and standalone operational notes.
- Promotion Foundation: admin-managed percentage/fixed coupons, server-authoritative checkout discount calculation, locked order redemption, and immutable order discount snapshots.

## 6. API

### Catalog

- `GET /api/v1/products`: List active products with filters and pagination.
- `GET /api/v1/products/{slug}`: Get product detail by slug.
- `GET /api/v1/categories`: List active categories.
- `GET /api/v1/brands`: List active brands.

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

- `POST /api/v1/checkout/preview`: Return validated server-authoritative checkout preview; does not create order/reserve stock/process payment.
- `POST /api/v1/orders`: Create pending order from authenticated cart.
- `GET /api/v1/orders`: Paginated owner-only order history.
- `GET /api/v1/orders/{id}`: Owner-only safe order detail, including shipment info when present.
- `POST /api/v1/orders/{id}/cancel`: Owner-only idempotent pending-order cancellation with restock.
- `POST /api/v1/orders/{id}/payments`: Create mock payment attempt from persisted order total/currency.
- `GET /api/v1/orders/{id}/payments/latest`: Return latest owner-only payment attempt.

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
- `POST /api/v1/admin/orders/{id}/shipment`: Create shipment and transition `PROCESSING -> SHIPPED`.
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

### Platform

- `GET /actuator/health`: Backend health.
- `GET /v3/api-docs`: OpenAPI JSON.
- `GET /swagger-ui/index.html`: Swagger UI.

## 7. Frontend

- Routes:
  - `/`, `/shop`, `/products/[slug]`.
  - `/cart`, `/checkout`, `/orders/[id]`.
  - `/login`, `/register`, `/account`, `/account/orders`, `/account/returns`, `/account/returns/[id]`.
  - `/admin`, `/admin/products`, `/admin/products/new`, `/admin/products/[id]`, `/admin/orders`, `/admin/orders/[id]`, `/admin/returns`, `/admin/returns/[id]`.
- `/orders/[id]` shows order details, payment action for pending orders, pending cancellation, customer-visible shipment tracking, and return request form when eligible.
- `/admin/orders` provides server-driven status views, customer/order/tracking/date filters, safe sort controls, pagination, and compact operational summaries.
- `/admin/orders/[id]` shows customer/order/payment/refund/status history, a separately saved operational note, pending cancellation, refund-and-cancel, shipment form for processing orders, shipment edit for shipped orders, and delivered timestamp for delivered orders.
- `/account/returns` and `/account/returns/[id]` show customer return history/detail, eligible cancellation, mock return label/tracking, and mark-shipped action.
- `/admin/returns` provides status views, customer/order/tracking/date filters, controlled sort, pagination, and compact operational links.
- `/checkout` accepts an optional coupon code and renders backend-authoritative discount/totals.
- `/admin/promotions`, `/admin/promotions/new`, and `/admin/promotions/[id]` provide promotion list/create/edit workflows.
- `/admin/returns/[id]` shows a separately saved operational note alongside approve, reject, mock label generation, shipment-aware receive, and mock refund actions.
- Important shared components: `Navbar`, `Footer`, `Container`, `Button`, `ProductCard`, `ProductGrid`, `Price`, `EmptyState`, `ErrorState`, `Skeleton`, `ProductVariantSelector`, `CartView`, `CheckoutView`, `OrderDetailsView`, `AdminShell`, `AdminOrderDetailsView`, `AdminProductForm`, `ReturnRequestPanel`, `AccountReturnsView`, `AccountReturnDetailsView`, `AdminReturnsView`, `AdminReturnDetailsView`.
- Frontend does not store refresh tokens in localStorage, sessionStorage, or readable cookies.
- Access token lives in runtime auth state and is restored through `POST /api/v1/auth/refresh`.
- Anonymous cart id remains in localStorage and is sent as `X-Cart-Id` during login.

## 8. Tests

- Backend integration/unit coverage exists for catalog, auth, profile/password, cart, checkout preview, order creation/history/cancellation, payment, admin products/orders, refunds, shipment fulfillment, and returns/RMA.
- Shipment tests cover creation from `PROCESSING`, transition to `SHIPPED`, history creation, invalid status, validation, duplicate shipment, tracking update while `SHIPPED`, admin authorization, customer shipment visibility, ownership isolation, `DELIVERED` setting `deliveredAt`, and delivery rejection without shipment.
- Return/RMA tests cover eligibility, ownership, return window, invalid/over quantities, partial returns, approve/reject/customer cancel, mock label generation, customer mark-shipped, receive shipment requirement, receive restock, refund failure/retry/success, authorization, idempotency around label/shipped/receive/refund, and admin queue status/customer/order/tracking/date filters, pagination, sorting, and notes.
- Admin order tests cover status/customer/order/tracking/date filters, pagination, constrained sorting, summary metadata, note save/clear, and endpoint authorization.
- Latest backend status on 2026-08-21 after Admin Order Operations Hardening: `mvn test` passed; `mvn package` passed.
- Latest frontend status on 2026-08-21 after Admin Order Operations Hardening: `npm run lint` passed; `NEXT_DIST_DIR=.next-codex npm run build` passed.
- Latest Docker/runtime status on 2026-08-21 after Admin Order Operations Hardening: SQL Server and Redis healthy, backend/frontend running, Flyway applied v11, health/Swagger/admin order pages returned 200; all six order status filters, customer/order/tracking/date filters, safe sorting, pagination, note save/clear, and admin authorization were verified.

## 9. Important Decisions

- Preserve modular monolith architecture; do not split into microservices yet.
- Flyway is the only schema manager; keep Hibernate `ddl-auto=validate`.
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

## 10. Not Implemented Yet

- External payment gateway integration.
- External shipping provider integration, real carrier labels, and tracking webhooks.
- Admin payment/refund history list beyond current order detail summaries.
- Loyalty points, gift cards, and promotion scheduling.
- Guest checkout.
- OAuth.
- Kubernetes manifests.
- Full Prometheus/Grafana/OpenTelemetry observability stack.

## 11. Known Issues / Technical Debt

- Git commands may report dubious ownership for `E:/LUXORA`; use a one-off safe.directory override when needed.
- A stale local `frontend/.next` directory has Windows permission/lock issues; verified builds use `.next-codex`, which is ignored.
- Cart persistence is Redis-only; authenticated carts expire by Redis TTL.
- Runtime verification has created local development users/orders/payments/refunds/shipments/returns/return shipments/status history and promotions.
- Promotion Foundation needs dedicated automated coverage for all validation and concurrency cases; existing backend regression tests pass.

## 12. Next Task

- Guest checkout foundation, preserving server-authoritative cart, inventory, promotion, and order pricing rules.

## Future Maintenance Rule

- At the end of every future development task, update this file only if architecture, APIs, database, features, infrastructure, known issues, or next task changed.
- Future agents should read this file first and inspect only files directly relevant to the requested task.
