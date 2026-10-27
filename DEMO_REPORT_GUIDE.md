# HƯỚNG DẪN BÁO CÁO & KỊCH BẢN DEMO DỰ ÁN LUXORA MAISON
> **Dành cho buổi thuyết trình / bảo vệ đồ án**
> **Mục tiêu:** Demo mượt mà trong 5 – 7 phút, tự tin, không lỗi và nêu bật các điểm kỹ thuật chuyên sâu.

---

## 1. THÔNG TIN KHỞI ĐỘNG HỆ THỐNG TRƯỚC KHI BÁO CÁO

Bạn có thể chọn **1 trong 2 cách** khởi động dưới đây tùy theo nhu cầu:

---

### CÁCH 1 (Khuyên dùng khi báo cáo — 1 Lệnh chạy toàn bộ bằng Docker)
Toàn bộ CSDL, Redis, AWS LocalStack, Backend và Frontend đều chạy khép kín trong Docker container:
1. Đảm bảo cổng `3000` và `8080` không bị chiếm bởi terminal dev nào đang mở trên máy (tắt `npm run dev` hoặc `mvn` nếu có).
2. Mở terminal tại thư mục gốc `e:\LUXORA`:
   ```bash
   docker compose up -d
   ```
3. Sau khi chạy, kiểm tra:
   - Frontend: `http://localhost:3000`
   - Backend API: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

### CÁCH 2 (Chạy Hạ tầng Docker + Chạy Code trực tiếp trên máy để xem Log/Debug)
Nếu bạn muốn chạy Backend và Frontend ở 2 terminal ngoài để nhìn log nhảy trực tiếp:

#### Bước 1: Khởi động 3 dịch vụ hạ tầng trong Docker
Mở terminal tại thư mục gốc `e:\LUXORA`:
```bash
docker compose up -d sqlserver redis localstack
```
*(Chỉ bật 3 container nền, không bật container backend và frontend trong Docker để tránh bị trùng cổng 8080 và 3000).*

#### Bước 2: Khởi động Backend Spring Boot
Mở terminal thứ hai tại `e:\LUXORA\backend`:
```bash
mvn spring-boot:run
```
*Backend chạy tại: `http://localhost:8080`. Kiểm tra Swagger: `http://localhost:8080/swagger-ui/index.html`.*

#### Bước 3: Khởi động Frontend Next.js
Mở terminal thứ ba tại `e:\LUXORA\frontend`:
```bash
npm run dev
```
*Giao diện mở tại: `http://localhost:3000`.*

---

## 2. BẢNG TÀI KHOẢN & DỮ LIỆU DEMO

| Đối tượng | Email / Tài khoản | Mật khẩu chính xác | Mục đích sử dụng |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@luxora.com` | `Admin123456!` | Demo Dashboard doanh thu, Quản lý Banners, Flash Sale, Vận đơn GHN, Duyệt đánh giá |
| **Khách hàng VIP (User)** | `customer@luxora.com` | `Password123!` | Demo bắt buộc đăng nhập mua hàng, tích điểm Loyalty, thanh toán VNPay, lịch sử đơn |

*(Lưu ý: Hệ thống yêu cầu bắt buộc đăng nhập để mua hàng nhằm bảo lưu quyền lợi bảo hành 24 tháng, tích lũy điểm thưởng VIP và bảo mật giao dịch).*

### Dữ liệu Test VNPay Sandbox (Cổng thanh toán thực tế)
- **Ngân hàng:** Chọn **NCB**
- **Số thẻ:** `9704198526191432198`
- **Tên chủ thẻ:** `NGUYEN VAN A`
- **Ngày phát hành:** `07/15`
- **Mã OTP xác thực:** `123456`

### Mã Voucher khuyến mãi sẵn có
- `WELCOME50`: Giảm ngay **$50** cho đơn từ $200.
- `LUXORA10`: Giảm **10%** tổng giá trị giỏ hàng.

---

## 3. KỊCH BẢN THUYẾT TRÌNH & DEMO TỪNG BƯỚC (5 - 7 PHÚT)

### Phần 1: Giới thiệu tổng quan & Giao diện Thượng lưu (1 phút)
1. Mở trang chủ `http://localhost:3000`:
   - *"Kính thưa thầy cô/hội đồng, nhóm em xây dựng hệ thống thương mại điện tử hàng xa xỉ LUXORA Maison theo chuẩn kiến trúc Modular Monolith hiện đại."*
   - Chỉ vào giao diện: Tone màu **Cashmere Beige & Alabaster White** tinh tế, kết hợp điểm nhấn vàng Champagne Gold và font chữ serif Cormorant Garamond.
2. Trình diễn **Hero Banner Carousel**:
   - Tự động chuyển slide lookbook với ảnh độ phân giải cao, nút điều hướng thủ công và thanh tiến trình thời gian thực.
3. Cuộn nhẹ xuống mục **Flash Sale Event**:
   - Trình diễn đồng hồ đếm ngược giờ/phút/giây thời gian thực (`FlashSaleCountdown`), thanh tiến độ hiển thị số lượng sản phẩm đã bán và giới hạn quota.

### Phần 2: Luồng Khách hàng — Bắt buộc Đăng nhập, Voucher & Thanh toán VNPay (2 phút)
1. Bấm vào 1 sản phẩm bất kỳ -> Chọn biến thể -> Bấm **"Thêm vào túi mua sắm"** (Hệ thống có phản hồi toast mượt mà).
2. Vào giỏ hàng `/cart`:
   - Trình diễn nút: Nếu chưa đăng nhập, nút chuyển thành **"Đăng nhập để đặt hàng"** kèm lưu ý bảo vệ quyền lợi hội viên.
   - Bấm đăng nhập -> Nhập `customer@luxora.com` / `Password123!` -> Hệ thống tự động hợp nhất giỏ hàng (`Merge anonymous cart via X-Cart-Id`) và chuyển thẳng tới `/checkout`.
3. Tại trang `/checkout`:
   - Trình diễn tính năng **Tự động gợi ý & Áp dụng Voucher tốt nhất (`autoApplyBestVoucher`)**: Hệ thống tự động chọn voucher mang lại mức giảm cao nhất cho khách hàng.
   - Nhập thông tin giao hàng (địa chỉ, số điện thoại).
4. Chọn phương thức thanh toán **Cổng VNPay Sandbox**:
   - Bấm **"Xác nhận thanh toán"**: Hệ thống ký mã băm bảo mật **HMAC-SHA512** và chuyển hướng sang cổng thanh toán VNPay chính thức.
   - Nhập số thẻ test NCB -> Bấm tiếp tục -> Nhập OTP `123456`.
   - Cổng VNPay xác nhận thành công và tự động chuyển về trang `/orders/{id}` với trạng thái đơn hàng cập nhật thành **PAID (Đã thanh toán)**.
5. Nêu điểm kỹ thuật: *"Hệ thống sử dụng cơ chế xử lý IPN Webhook hai chiều với Khóa bi quan (Pessimistic Locking) để đảm bảo tính toàn vẹn số tiền và chống gian lận đơn hàng."*
6. Đồng thời, hệ thống tự động kích hoạt **AWS SES** gửi email xác nhận hóa đơn điện tử về hòm thư của khách hàng.

### Phần 3: Luồng Khách hàng — Điểm thưởng Hội viên VIP (1 phút)
1. Bấm vào tài khoản `/account`:
   - Trình diễn thẻ hội viên **LUXORA Rewards**: Hạng thẻ (Silver, Gold, Platinum), hệ số nhân điểm (1.0x, 1.25x, 1.5x) và thanh tiến trình thăng hạng.
   - Khách hàng có thể dùng điểm tích lũy để trừ trực tiếp tiền mặt trong lần mua tiếp theo (100 điểm = $1.00).

### Phần 4: Luồng Quản trị viên Admin & Báo cáo Doanh thu (2 phút)
1. Đăng nhập tài khoản `admin@luxora.com`.
2. Mở trang quản trị `/admin`:
   - **Executive BI Dashboard**: Xem các chỉ số tài chính thời gian thực: Tổng doanh thu, Doanh thu hôm nay, Doanh thu tháng, Giá trị đơn hàng trung bình (AOV), Tỷ lệ hoàn thành đơn hàng.
   - **Biểu đồ SVG tương tác (Time-Series Chart)**: Chuyển đổi linh hoạt giữa 7 Ngày, 30 Ngày, 12 Tháng; di chuột xem chi tiết doanh thu từng mốc mà không phụ thuộc vào thư viện ngoài nặng nề.
   - **Thanh tiến độ Fulfillment**: Phân bổ trực quan đơn hàng theo trạng thái (Pending, Paid, Processing, Shipped, Delivered).
3. Mở mục **Đơn hàng `/admin/orders`**:
   - Chọn đơn hàng vừa thanh toán -> Chuyển trạng thái sang `PROCESSING`.
   - Trình diễn **Tạo vận đơn tự động Giao Hàng Nhanh (GHN)**: Bấm tạo vận đơn GHN, hệ thống sinh mã vận đơn thật và tạo link tra cứu trực tiếp trên cổng GHN (`https://donhang.ghn.vn/?order_code=...`).
4. Mở mục **Quản lý Banner `/admin/banners`** hoặc **Flash Sale `/admin/flash-sales`**:
   - Trình diễn khả năng bật/tắt banner sự kiện, điều chỉnh vị trí hiển thị và quản lý chiến dịch khuyến mại tức thời.
5. Mở mục **Quản lý Sản phẩm `/admin/products/{id}` (Phân quyền Admin & Đồng bộ ảnh thời gian thực)**:
   - Trình diễn khu vực **Hình ảnh Sản phẩm (Product Media Gallery)** với huy hiệu bảo mật *"Chỉ Quản trị viên (Admin) mới có quyền Thêm, Sửa, Xóa ảnh sản phẩm"*.
   - Có thumbnail preview trực quan (64x64px), chế độ sửa trực tiếp (Inline Edit) đổi URL ảnh, chú thích (Alt text), và thứ tự hiển thị (Display Order), hoặc bấm Xóa ảnh.
   - Mở tab người dùng bình thường (`/shop` hoặc trang chi tiết sản phẩm `/products/[slug]`): Ảnh mới cập nhật đồng bộ ngay lập tức không bị lưu cache cũ.
   - Nhấn mạnh điểm kỹ thuật & bảo mật: *"Hệ thống kiểm soát phân quyền nghiêm ngặt với Spring Security `hasRole('ADMIN')` trên toàn bộ endpoint `/api/v1/admin/products/{id}/images/**`. Người dùng thường hoặc khách vãng lai nếu cố tình gọi API sửa/xóa sẽ bị chặn đứng với mã lỗi HTTP 403 Forbidden hoặc 401 Unauthorized."*

---

## 4. BẢNG TỔNG HỢP CÔNG NGHỆ (DÙNG ĐỂ NÓI KHI THẦY CÔ HỎI)

| Thành phần | Công nghệ sử dụng | Điểm nổi bật & Lý do lựa chọn |
| :--- | :--- | :--- |
| **Backend** | Spring Boot 4.0, Java 21 | Modular Monolith phân tách domain rõ ràng (Catalog, Order, Payment, Shipping, Loyalty). |
| **Frontend** | Next.js 16 (App Router), React 19, TypeScript | Server Components tối ưu SEO, Vanilla CSS kiến trúc Haute Couture không cồng kềnh. |
| **Cơ sở dữ liệu** | Microsoft SQL Server 2022 | RDBMS doanh nghiệp mạnh mẽ, toàn vẹn dữ liệu giao dịch tài chính. |
| **Migration** | Liquibase (`17 changesets XML`) | Quản lý phiên bản CSDL chặt chẽ, hỗ trợ cả SQL Server thực tế lẫn H2 in-memory test. |
| **Bộ nhớ đệm** | Redis 8 Alpine | Lưu trữ giỏ hàng vãng lai & xác thực với TTL tự hủy, tốc độ đọc ghi milli-giây. |
| **Thanh toán** | VNPay Gateway Sandbox | Ký bảo mật chuẩn HMAC-SHA512, cơ chế IPN Webhook Server-to-Server. |
| **Vận chuyển** | GHN (Giao Hàng Nhanh) Open API v2 | Tích hợp Provider Pattern (`ShippingProvider`), tạo vận đơn & webhook tự động. |
| **Điện toán đám mây** | AWS SDK v2 (S3, SES, SQS, SNS) + LocalStack | Giả lập môi trường Cloud AWS chuẩn doanh nghiệp, upload ảnh S3 presigned URL, email SES. |
| **Kiểm thử** | JUnit 5, Mockito, AssertJ | **231 automated tests** bao phủ toàn bộ các luồng nghiệp vụ cốt lõi. |

---

## 5. TOP 5 CÂU HỎI THƯỜNG GẶP CỦA HỘI ĐỒNG & CÁCH TRẢ LỜI MẪU

### Câu hỏi 1: "Làm thế nào để hệ thống xử lý khi có 2 khách hàng cùng mua món hàng cuối cùng lúc Flash Sale?"
> **Trả lời:**
> *"Hệ thống của em sử dụng cơ chế **Khóa bi quan ghi (Pessimistic Write Locking - `SELECT ... WITH (UPDLOCK)`)** ở tầng JPA/Database khi trừ số lượng tồn kho `inventory_items` và chỉ tiêu Flash Sale `flash_sale_items`. Giao dịch thứ nhất giữ khóa và trừ thành công, giao dịch thứ hai bị chặn lại cho đến khi kiểm tra thấy tồn kho đã bằng 0 và trả về lỗi `Out of Stock` một cách nhất quán, tuyệt đối không xảy ra hiện tượng bán vượt (Overselling)."*

### Câu hỏi 2: "Tại sao không dùng Microservices mà lại dùng Modular Monolith?"
> **Trả lời:**
> *"Dự án áp dụng kiến trúc **Modular Monolith** vì đây là mô hình tối ưu nhất cho giai đoạn này: vừa phân chia ranh giới nghiệp vụ (Boundaries) rõ ràng theo từng package domain độc lập, vừa tránh được độ trễ mạng (Network latency), chi phí hạ tầng và tính phức tạp của phân tán dữ liệu (Distributed Transactions/2PC). Khi cần mở rộng trong tương lai, từng module hoàn toàn có thể tách thành microservice riêng lẻ một cách dễ dàng."*

### Câu hỏi 3: "Làm sao đảm bảo hacker không thể tự chỉnh sửa số tiền thanh toán khi gửi sang VNPay?"
> **Trả lời:**
> *"Mọi số liệu đơn hàng và tổng thanh toán đều do **Server backend tính toán và lưu trong CSDL**, Frontend hoàn toàn không thể can thiệp. Khi tạo URL sang VNPay, backend tự sinh chuỗi băm bảo mật HMAC-SHA512 với `vnp_HashSecret`. Khi VNPay gửi IPN Webhook phản hồi về, backend kiểm tra lại chữ ký băm và đối soát chính xác số tiền `vnp_Amount` với `grandTotal` của đơn hàng trong database trước khi cập nhật đơn sang `PAID`."*

### Câu hỏi 4: "Khách hàng mua hàng mà không tạo tài khoản (Guest Checkout) thì theo dõi đơn hàng bằng cách nào?"
> **Trả lời:**
> *"Hệ thống hỗ trợ tính năng **Guest Checkout**. Khi khách vãng lai đặt hàng, hệ thống sinh ra một **Guest Access Token** ngẫu nhiên 64-kí tự mật mã (Cryptographically Secure Hex Token) lưu cùng đơn hàng và trả về client. Khách hàng sử dụng token này để tra cứu trạng thái đơn, thanh toán VNPay và yêu cầu hủy đơn mà không cần phải đăng ký tài khoản."*

### Câu hỏi 5: "Quản lý migration CSDL bằng công cụ nào và có lợi ích gì?"
> **Trả lời:**
> *"Hệ thống sử dụng **Liquibase** với 17 Changeset XML độc lập được quản lý phiên bản qua Git. Liquibase cho phép triển khai CSDL tự động đồng nhất trên mọi môi trường (SQL Server khi chạy Docker/Production và H2 in-memory khi chạy Unit/Integration Test) mà không phụ thuộc vào `ddl-auto` của Hibernate, đảm bảo an toàn tuyệt đối cho dữ liệu doanh nghiệp."*

---

*Chúc bạn có buổi báo cáo thành công rực rỡ và đạt điểm số tối đa!*
