# LUXORA

Production-oriented e-commerce foundation using a modular monolith architecture.

## Stack

- Backend: Java 21, Spring Boot 4, Maven
- Frontend: Next.js 16, TypeScript
- Data: SQL Server, Redis, Liquibase
- Cloud / Messaging: AWS SDK v2 (S3, SES, SQS, SNS) & LocalStack
- Local infra: Docker Compose

## Structure

- `backend/` - Spring Boot application under `com.luxora.commerce`
- `frontend/` - Next.js App Router application
- `docker-compose.yml` - local SQL Server, Redis, LocalStack, backend, and frontend services

## Local Setup

Copy `.env.example` to `.env` and adjust values as needed.
Anonymous carts are stored in Redis with a local development TTL of 24 hours (`CART_TTL=PT24H`).

```bash
docker compose up --build
```

Backend health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

## Payment Providers

- **Mock Payment**: For local development and test automation (`provider: "MOCK"`).
- **VNPay Sandbox**: Direct integration with VNPay payment gateway sandbox (`provider: "VNPAY"`).
  - Configurable via environment variables: `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, and `VNPAY_RETURN_URL`.
  - IPN webhook endpoint: `GET /api/v1/payments/vnpay-ipn`.
  - Browser return URL: `GET /api/v1/payments/vnpay-return`.

## Shipping Providers

- **Mock Shipping**: For local development and test automation (`provider: "MOCK"`).
- **GHN Sandbox (Giao Hàng Nhanh)**: Direct integration with GHN Open API v2 (`provider: "GHN"`).
  - Configurable via environment variables: `GHN_API_URL`, `GHN_TOKEN`, `GHN_SHOP_ID`, `GHN_FROM_DISTRICT_ID`, `GHN_FROM_WARD_CODE`, `GHN_WEBHOOK_TOKEN`.
  - Automated waybill creation in Admin Order details.
  - Tracking webhook endpoint: `POST /api/v1/shipments/ghn-webhook`.
  - Public tracking portal link: `https://donhang.ghn.vn/?order_code={trackingNumber}`.

## AWS Cloud Services & LocalStack

- **Amazon S3**: File storage for product images and customer review attachments.
  - Endpoint: `POST /api/v1/media/presigned-upload-url` (presigned client-side upload).
- **Amazon SES**: Automated transactional email system with luxury-themed responsive HTML templates:
  - **Order Confirmation & Tax Invoice**: Automatically dispatched upon order commit for both authenticated users and guests, rendering an itemized receipt, variant breakdown, promotions/loyalty deductions, and delivery address.
  - **Shipment & Waybill Tracking**: Dispatched upon transition to `SHIPPED` via `ShipmentService.createShipment`, providing carrier details, monospace waybill number, and live carrier tracking portal URL (`https://donhang.ghn.vn/?order_code=...` for GHN).
  - **Loyalty Tier Ascension**: Automatically sent when customer crosses tier thresholds (`GOLD`, `PLATINUM`), showcasing new earning multipliers and complimentary shipping perks.
  - **Admin Diagnostic Testing**: `POST /api/v1/admin/emails/test` endpoint allowing admins to verify SES delivery for all notification templates.
  - **Non-blocking Resilience**: Gracefully handles offline LocalStack or mock credentials with warning logs, ensuring commerce transactions never fail due to email transmission issues.
- **Amazon SQS & SNS**: Asynchronous event publishing (`CommerceEventPublisher`) for commerce domain events.
- **LocalStack**: Local emulation running on port `4566` in Docker Compose with zero cloud costs.

## Database Migrations (Liquibase)

- Managed via `spring-boot-starter-liquibase` with master changelog `db/changelog/db.changelog-master.xml`.
- 17 modular XML changesets located in `backend/src/main/resources/db/changelog/changesets/` (`001` through `017`) supporting SQL Server and H2 in-memory test databases.

## Event Banners & Voucher Auto-Apply on Checkout

- **Homepage Hero Carousel**: Interactive luxury banner carousel (`HeroBannerCarousel.tsx`) featuring auto-advance, custom visual vignettes, metallic gold typography, custom CTA links, manual chevron controls, and dot indicators.
- **Admin Banner Management**: Dedicated portal (`/admin/banners`) allowing administrators to create, edit, toggle active status, preview images, and delete event banners.
- **Server-Authoritative Voucher Recommendations**: `GET /api/v1/promotions/available?subtotal=...` evaluates active vouchers, computes eligibility, estimated savings, remaining spend needed to unlock, and highlights the best deal.
- **Checkout Auto-Apply**: Seamless 1-click apply and `autoApplyBestVoucher` option in `CheckoutView.tsx` automatically selecting the highest-value promotion for the customer's cart.


## Flash Sales & Real-Time Countdown Timer

- **Time-Boxed Campaigns**: Scheduled promotional windows with start/end timestamps, automated status progression (`UPCOMING`, `ACTIVE`, `ENDED`, `CANCELLED`).
- **Quota & Sold Allocation**: Pre-allocated inventory stock per flash sale item; real-time tracking of units sold and visual progress bars (`XX% Claimed`).
- **Automated Checkout Pricing**: Cart and checkout preview automatically apply flash sale prices when quota is available, without requiring manual coupon entry.
- **Pessimistic Quota Reservation**: Atomic increment of claimed quantities upon order creation prevents overselling flash sale allocations.
- **Live Client Ticker**: Luxury numeric countdown widget (`HH : MM : SS`) updating in real-time, refreshing instantly when sales conclude.
- **Admin Management**: Dedicated management portal (`/admin/flash-sales`) to launch campaigns, monitor quotas, and cancel active sales.

## Loyalty Points & Membership Tiers (LUXORA Rewards)

- **Tier Progression**: `SILVER` (1.0x points, 0 pts), `GOLD` (1.25x points, 500 lifetime pts), `PLATINUM` (1.5x points + Free Shipping, 2,000 lifetime pts).
- **Point Value & Redemption**: 100 points = $1.00 USD. Point redemption is directly applied at checkout and capped at 50% of the order subtotal.
- **Automated Lifecycle**: Points awarded automatically upon order delivery (`DELIVERED`), deducted atomically at checkout with pessimistic locking, and restored upon order cancellation.
- **Account Dashboard**: Rich loyalty card in `/account` showing current points, dollar value, tier progress bar, and transaction history.
- **Admin Management**: Endpoints for inspecting customer points balance and adjusting points with audit logs.

## Executive Intelligence & Revenue Analytics Dashboard

- **Financial KPIs**: Gross revenue (excluding cancelled orders), today revenue, month-to-date revenue, Average Order Value (AOV), and order fulfillment completion rate.
- **Interactive SVG Time-Series Chart**: Real-time trajectory with period switching (7 Days, 30 Days, 12 Months), coordinate mapping, subtle gradient area fill, and interactive hover tooltips with zero 3rd-party dependencies.
- **Fulfillment Pipeline**: Visual proportional segmented status bar (`Delivered`, `Shipped`, `Processing`, `Paid`, `Pending`, `Cancelled`).
- **Catalog Performance**: Top best-selling products ranked by units sold and gross revenue contribution.
- **Customer & Retention Analytics**: Total customer volume, monthly new customer growth, and breakdown across LUXORA loyalty membership tiers (SILVER, GOLD, PLATINUM).

## Product Reviews & Ratings (with AWS S3 Upload)

- **Customer Reviews**: Star ratings (1-5 stars), review comments, and verified purchase verification for customers with delivered orders.
- **Media Upload**: Direct photo attachments uploaded to AWS S3 via presigned upload URLs.
- **Real-time Statistics**: Average rating score and star rating breakdown distribution (1-5 stars).
- **Admin Moderation**: Admin moderation queue (`/admin/reviews`) with status filtering (`ALL`, `PENDING`, `APPROVED`, `REJECTED`) and approval actions.

## Default Accounts & Seed Data

### Default Test Accounts
- **Admin**: `admin@luxora.com` / `Admin123456!` (Role: `ROLE_ADMIN`, `ROLE_USER`)
- **Customer**: `customer@luxora.com` / `Password123!` (Role: `ROLE_USER`)

### Sample Promotions
- `LUXORA10`: 10% discount for orders from $200 (max $100 off).
- `WELCOME50`: $50 direct discount for orders from $300.

### Database Seeding Options
1. **Automatic Seeding (Spring Boot Local Profile)**:
   When running the backend with `-Dspring.profiles.active=local`, `CatalogSeedDataLoader`, `ExtendedCatalogSeedDataLoader`, and `UserSeedDataLoader` run automatically on startup to populate 20+ products, categories, default accounts, and promotions.
2. **Manual Seeding via SSMS (SQL Server)**:
   Open [backend/src/main/resources/db/seed_data.sql](file:///e:/LUXORA/backend/src/main/resources/db/seed_data.sql) in SQL Server Management Studio (SSMS) and execute it against database `luxora`. The script is completely idempotent.

## Testing & Quality

Run backend test suite:

```bash
cd backend
mvn test
```

Run frontend lint and build:

```bash
cd frontend
npm run lint
npm run build
```
