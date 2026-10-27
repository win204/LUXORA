-- ====================================================================================================
-- LUXORA COMMERCE - COMPREHENSIVE SEED DATA SCRIPT (SQL Server)
-- ====================================================================================================
-- Usage: Run this script directly in SQL Server Management Studio (SSMS) against database [luxora].
-- This script is 100% idempotent (safe to run multiple times without duplicating data).
-- ====================================================================================================

USE [luxora];
GO

SET NOCOUNT ON;

-- ----------------------------------------------------------------------------------------------------
-- 1. ROLES
-- ----------------------------------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM [roles] WHERE [name] = 'ROLE_ADMIN')
    INSERT INTO [roles] ([id], [name], [created_at], [updated_at])
    VALUES ('B4C5F08C-A672-4B6A-92FB-8A80D87E6001', 'ROLE_ADMIN', SYSUTCDATETIME(), SYSUTCDATETIME());

IF NOT EXISTS (SELECT 1 FROM [roles] WHERE [name] = 'ROLE_USER')
    INSERT INTO [roles] ([id], [name], [created_at], [updated_at])
    VALUES ('C5D6A19D-B783-4C7B-A3AC-9B91E98F7002', 'ROLE_USER', SYSUTCDATETIME(), SYSUTCDATETIME());

-- ----------------------------------------------------------------------------------------------------
-- 2. DEFAULT USERS
-- Admin: admin@luxora.com / Admin123456!
-- Customer: customer@luxora.com / Password123!
-- ----------------------------------------------------------------------------------------------------
DECLARE @AdminRoleId UNIQUEIDENTIFIER = (SELECT [id] FROM [roles] WHERE [name] = 'ROLE_ADMIN');
DECLARE @UserRoleId UNIQUEIDENTIFIER = (SELECT [id] FROM [roles] WHERE [name] = 'ROLE_USER');
DECLARE @AdminUserId UNIQUEIDENTIFIER = 'A1B2C3D4-E5F6-4A7B-8C9D-0E1F2A3B4C5D';
DECLARE @CustUserId UNIQUEIDENTIFIER = 'F1E2D3C4-B5A6-4F7E-8D9C-0B1A2F3E4D5C';

IF NOT EXISTS (SELECT 1 FROM [users] WHERE [email] = 'admin@luxora.com')
BEGIN
    INSERT INTO [users] ([id], [email], [password_hash], [first_name], [last_name], [enabled], [created_at], [updated_at])
    VALUES (@AdminUserId, 'admin@luxora.com', '$2a$10$1gmE/E2yEaIwUGyHM5kC6.1QBlzdyPd2evGG7D2vfZB1N2aOv1hkm', 'Luxora', 'Administrator', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    IF NOT EXISTS (SELECT 1 FROM [user_roles] WHERE [user_id] = @AdminUserId AND [role_id] = @AdminRoleId)
        INSERT INTO [user_roles] ([user_id], [role_id]) VALUES (@AdminUserId, @AdminRoleId);

    IF NOT EXISTS (SELECT 1 FROM [user_roles] WHERE [user_id] = @AdminUserId AND [role_id] = @UserRoleId)
        INSERT INTO [user_roles] ([user_id], [role_id]) VALUES (@AdminUserId, @UserRoleId);
END

IF NOT EXISTS (SELECT 1 FROM [users] WHERE [email] = 'customer@luxora.com')
BEGIN
    INSERT INTO [users] ([id], [email], [password_hash], [first_name], [last_name], [enabled], [created_at], [updated_at])
    VALUES (@CustUserId, 'customer@luxora.com', '$2a$10$sr90AY5mcbraZ/I4U7vCr.gqyA/Zxol5PqFerFUgwzxiDk9WVL0/6', 'Alex', 'Morgan', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    IF NOT EXISTS (SELECT 1 FROM [user_roles] WHERE [user_id] = @CustUserId AND [role_id] = @UserRoleId)
        INSERT INTO [user_roles] ([user_id], [role_id]) VALUES (@CustUserId, @UserRoleId);
END

-- ----------------------------------------------------------------------------------------------------
-- 3. PROMOTIONS
-- ----------------------------------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM [promotions] WHERE [code] = 'LUXORA10')
    INSERT INTO [promotions] ([id], [code], [name], [description], [type], [value], [minimum_order_amount], [maximum_discount_amount], [starts_at], [ends_at], [usage_limit], [usage_count], [active], [created_at], [updated_at])
    VALUES ('E1F2A3B4-5C6D-4E7F-8A9B-0C1D2E3F4A5B', 'LUXORA10', N'Giảm 10% cho đơn hàng đầu tiên', N'Áp dụng giảm 10% tối đa $100 cho đơn hàng từ $200', 'PERCENTAGE', 10.00, 200.00, 100.00, SYSUTCDATETIME(), DATEADD(year, 1, SYSUTCDATETIME()), 500, 0, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

IF NOT EXISTS (SELECT 1 FROM [promotions] WHERE [code] = 'WELCOME50')
    INSERT INTO [promotions] ([id], [code], [name], [description], [type], [value], [minimum_order_amount], [maximum_discount_amount], [starts_at], [ends_at], [usage_limit], [usage_count], [active], [created_at], [updated_at])
    VALUES ('F2A3B4C5-6D7E-4F8A-9B0C-1D2E3F4A5B6C', 'WELCOME50', N'Giảm ngay $50 cho đơn hàng cao cấp', N'Áp dụng giảm trực tiếp $50 cho đơn hàng từ $300', 'FIXED_AMOUNT', 50.00, 300.00, 50.00, SYSUTCDATETIME(), DATEADD(year, 1, SYSUTCDATETIME()), 200, 0, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ----------------------------------------------------------------------------------------------------
-- 4. BRANDS
-- ----------------------------------------------------------------------------------------------------
DECLARE @BrandApple UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000001';
DECLARE @BrandSamsung UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000002';
DECLARE @BrandSony UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000003';
DECLARE @BrandBose UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000004';
DECLARE @BrandAurora UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000005';
DECLARE @BrandAtelier UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000006';
DECLARE @BrandLumen UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000007';
DECLARE @BrandKeychron UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000008';
DECLARE @BrandDyson UNIQUEIDENTIFIER = '10000000-0000-0000-0000-000000000009';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'apple')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandApple, 'Apple', 'apple', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandApple = [id] FROM [brands] WHERE [slug] = 'apple';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'samsung')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandSamsung, 'Samsung', 'samsung', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandSamsung = [id] FROM [brands] WHERE [slug] = 'samsung';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'sony')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandSony, 'Sony', 'sony', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandSony = [id] FROM [brands] WHERE [slug] = 'sony';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'bose')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandBose, 'Bose', 'bose', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandBose = [id] FROM [brands] WHERE [slug] = 'bose';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'aurora-devices')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandAurora, 'Aurora Devices', 'aurora-devices', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandAurora = [id] FROM [brands] WHERE [slug] = 'aurora-devices';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'atelier-sound')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandAtelier, 'Atelier Sound', 'atelier-sound', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandAtelier = [id] FROM [brands] WHERE [slug] = 'atelier-sound';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'lumen-works')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandLumen, 'Lumen Works', 'lumen-works', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandLumen = [id] FROM [brands] WHERE [slug] = 'lumen-works';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'keychron')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandKeychron, 'Keychron', 'keychron', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandKeychron = [id] FROM [brands] WHERE [slug] = 'keychron';

IF NOT EXISTS (SELECT 1 FROM [brands] WHERE [slug] = 'dyson')
    INSERT INTO [brands] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@BrandDyson, 'Dyson', 'dyson', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @BrandDyson = [id] FROM [brands] WHERE [slug] = 'dyson';

-- ----------------------------------------------------------------------------------------------------
-- 5. CATEGORIES
-- ----------------------------------------------------------------------------------------------------
DECLARE @CatPhones UNIQUEIDENTIFIER = '20000000-0000-0000-0000-000000000001';
DECLARE @CatLaptops UNIQUEIDENTIFIER = '20000000-0000-0000-0000-000000000002';
DECLARE @CatAudio UNIQUEIDENTIFIER = '20000000-0000-0000-0000-000000000003';
DECLARE @CatWearables UNIQUEIDENTIFIER = '20000000-0000-0000-0000-000000000004';
DECLARE @CatTablets UNIQUEIDENTIFIER = '20000000-0000-0000-0000-000000000005';
DECLARE @CatAccessories UNIQUEIDENTIFIER = '20000000-0000-0000-0000-000000000006';

IF NOT EXISTS (SELECT 1 FROM [categories] WHERE [slug] = 'phones')
    INSERT INTO [categories] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@CatPhones, 'Phones', 'phones', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @CatPhones = [id] FROM [categories] WHERE [slug] = 'phones';

IF NOT EXISTS (SELECT 1 FROM [categories] WHERE [slug] = 'laptops')
    INSERT INTO [categories] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@CatLaptops, 'Laptops', 'laptops', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @CatLaptops = [id] FROM [categories] WHERE [slug] = 'laptops';

IF NOT EXISTS (SELECT 1 FROM [categories] WHERE [slug] = 'audio')
    INSERT INTO [categories] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@CatAudio, 'Audio', 'audio', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @CatAudio = [id] FROM [categories] WHERE [slug] = 'audio';

IF NOT EXISTS (SELECT 1 FROM [categories] WHERE [slug] = 'wearables')
    INSERT INTO [categories] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@CatWearables, 'Wearables', 'wearables', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @CatWearables = [id] FROM [categories] WHERE [slug] = 'wearables';

IF NOT EXISTS (SELECT 1 FROM [categories] WHERE [slug] = 'tablets')
    INSERT INTO [categories] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@CatTablets, 'Tablets', 'tablets', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @CatTablets = [id] FROM [categories] WHERE [slug] = 'tablets';

IF NOT EXISTS (SELECT 1 FROM [categories] WHERE [slug] = 'accessories')
    INSERT INTO [categories] ([id], [name], [slug], [created_at], [updated_at]) VALUES (@CatAccessories, 'Accessories', 'accessories', SYSUTCDATETIME(), SYSUTCDATETIME());
ELSE SELECT @CatAccessories = [id] FROM [categories] WHERE [slug] = 'accessories';

-- ----------------------------------------------------------------------------------------------------
-- 6. PRODUCTS HELPER PROCEDURE OR BLOCK
-- ----------------------------------------------------------------------------------------------------
-- Product 1: iPhone 16 Pro Max
DECLARE @P_IPhone UNIQUEIDENTIFIER = '30000000-0000-0000-0000-000000000002';
IF NOT EXISTS (SELECT 1 FROM [products] WHERE [slug] = 'iphone-16-pro-max')
BEGIN
    INSERT INTO [products] ([id], [name], [slug], [short_description], [description], [brand_id], [category_id], [active], [created_at], [updated_at])
    VALUES (@P_IPhone, 'iPhone 16 Pro Max', 'iphone-16-pro-max', 'Titanium excellence with A18 Pro chip, 48MP Fusion camera, and Camera Control.', 'Crafted in Grade 5 titanium with an expansive 6.9-inch Super Retina XDR display, ProMotion technology, and next-generation battery longevity.', @BrandApple, @CatPhones, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_IPhone, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569', 'iPhone 16 Pro Max Desert Titanium', 1, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (NEWID(), @P_IPhone, 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5', 'iPhone camera lens detail', 2, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_specifications] ([id], [product_id], [name], [value], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_IPhone, 'Display', '6.9 inch Super Retina XDR OLED, 120Hz ProMotion', 1, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (NEWID(), @P_IPhone, 'Processor', 'Apple A18 Pro chip with 6-core GPU', 2, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (NEWID(), @P_IPhone, 'Camera', '48MP Fusion + 48MP Ultra Wide + 12MP 5x Telephoto', 3, SYSUTCDATETIME(), SYSUTCDATETIME());

    DECLARE @V_IPhone1 UNIQUEIDENTIFIER = NEWID();
    DECLARE @V_IPhone2 UNIQUEIDENTIFIER = NEWID();
    INSERT INTO [product_variants] ([id], [product_id], [sku], [color], [storage], [price], [active], [created_at], [updated_at])
    VALUES (@V_IPhone1, @P_IPhone, 'APL-16PM-DTI-256', 'Desert Titanium', '256GB', 1199.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (@V_IPhone2, @P_IPhone, 'APL-16PM-NTI-512', 'Natural Titanium', '512GB', 1399.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [inventory_items] ([id], [variant_id], [quantity_available], [created_at], [updated_at])
    VALUES (NEWID(), @V_IPhone1, 25, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (NEWID(), @V_IPhone2, 15, SYSUTCDATETIME(), SYSUTCDATETIME());
END

-- Product 2: Galaxy S25 Ultra
DECLARE @P_S25 UNIQUEIDENTIFIER = '30000000-0000-0000-0000-000000000003';
IF NOT EXISTS (SELECT 1 FROM [products] WHERE [slug] = 'galaxy-s25-ultra')
BEGIN
    INSERT INTO [products] ([id], [name], [slug], [short_description], [description], [brand_id], [category_id], [active], [created_at], [updated_at])
    VALUES (@P_S25, 'Galaxy S25 Ultra', 'galaxy-s25-ultra', 'Galaxy AI powerhouse with built-in S Pen, 200MP camera, and Snapdragon 8 Elite.', 'Redesigned rounded titanium corners for superior comfort, anti-reflective flat display, and real-time on-device multilingual translation.', @BrandSamsung, @CatPhones, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_S25, 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf', 'Galaxy S25 Ultra frame', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_specifications] ([id], [product_id], [name], [value], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_S25, 'Display', '6.8 inch Dynamic AMOLED 2X, 1-120Hz, 2600 nits', 1, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (NEWID(), @P_S25, 'Processor', 'Qualcomm Snapdragon 8 Elite for Galaxy', 2, SYSUTCDATETIME(), SYSUTCDATETIME());

    DECLARE @V_S25_1 UNIQUEIDENTIFIER = NEWID();
    INSERT INTO [product_variants] ([id], [product_id], [sku], [color], [storage], [price], [active], [created_at], [updated_at])
    VALUES (@V_S25_1, @P_S25, 'SAM-S25U-TGR-256', 'Titanium Gray', '256GB', 1299.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [inventory_items] ([id], [variant_id], [quantity_available], [created_at], [updated_at])
    VALUES (NEWID(), @V_S25_1, 20, SYSUTCDATETIME(), SYSUTCDATETIME());
END

-- Product 3: MacBook Pro 16 M4 Max
DECLARE @P_MBP UNIQUEIDENTIFIER = '30000000-0000-0000-0000-000000000004';
IF NOT EXISTS (SELECT 1 FROM [products] WHERE [slug] = 'macbook-pro-16-m4')
BEGIN
    INSERT INTO [products] ([id], [name], [slug], [short_description], [description], [brand_id], [category_id], [active], [created_at], [updated_at])
    VALUES (@P_MBP, 'MacBook Pro 16 M4 Max', 'macbook-pro-16-m4', 'Extreme pro performance with M4 Max, Liquid Retina XDR display, and 24-hour battery.', 'Engineered for demanding creative workflows with 16-core CPU, 40-core GPU, and 140W fast charging.', @BrandApple, @CatLaptops, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_MBP, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8', 'MacBook Pro 16 Space Black', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_specifications] ([id], [product_id], [name], [value], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_MBP, 'Display', '16.2 inch Liquid Retina XDR, 1600 nits, 120Hz', 1, SYSUTCDATETIME(), SYSUTCDATETIME()),
           (NEWID(), @P_MBP, 'Processor', 'Apple M4 Max with 16-core CPU, 40-core GPU', 2, SYSUTCDATETIME(), SYSUTCDATETIME());

    DECLARE @V_MBP1 UNIQUEIDENTIFIER = NEWID();
    INSERT INTO [product_variants] ([id], [product_id], [sku], [color], [storage], [price], [active], [created_at], [updated_at])
    VALUES (@V_MBP1, @P_MBP, 'APL-MBP16-SBK-1TB', 'Space Black', '36GB / 1TB SSD', 2499.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [inventory_items] ([id], [variant_id], [quantity_available], [created_at], [updated_at])
    VALUES (NEWID(), @V_MBP1, 10, SYSUTCDATETIME(), SYSUTCDATETIME());
END

-- Product 4: Sony WH-1000XM5
DECLARE @P_XM5 UNIQUEIDENTIFIER = '30000000-0000-0000-0000-000000000005';
IF NOT EXISTS (SELECT 1 FROM [products] WHERE [slug] = 'sony-wh-1000xm5')
BEGIN
    INSERT INTO [products] ([id], [name], [slug], [short_description], [description], [brand_id], [category_id], [active], [created_at], [updated_at])
    VALUES (@P_XM5, 'Sony WH-1000XM5', 'sony-wh-1000xm5', 'Industry-leading noise canceling headphones with dual processors and 8 microphones.', 'Auto NC Optimizer adjusts noise canceling automatically based on wearing conditions and environment with LDAC Hi-Res support.', @BrandSony, @CatAudio, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_XM5, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b', 'Sony WH-1000XM5', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    DECLARE @V_XM5_1 UNIQUEIDENTIFIER = NEWID();
    INSERT INTO [product_variants] ([id], [product_id], [sku], [color], [storage], [price], [active], [created_at], [updated_at])
    VALUES (@V_XM5_1, @P_XM5, 'SNY-XM5-BLK', 'Midnight Black', 'Standard', 399.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [inventory_items] ([id], [variant_id], [quantity_available], [created_at], [updated_at])
    VALUES (NEWID(), @V_XM5_1, 30, SYSUTCDATETIME(), SYSUTCDATETIME());
END

-- Product 5: Apple Watch Ultra 2
DECLARE @P_AWU UNIQUEIDENTIFIER = '30000000-0000-0000-0000-000000000006';
IF NOT EXISTS (SELECT 1 FROM [products] WHERE [slug] = 'apple-watch-ultra-2')
BEGIN
    INSERT INTO [products] ([id], [name], [slug], [short_description], [description], [brand_id], [category_id], [active], [created_at], [updated_at])
    VALUES (@P_AWU, 'Apple Watch Ultra 2', 'apple-watch-ultra-2', 'Rugged titanium sports watch with precision dual-frequency GPS and 3000 nits display.', 'Designed for endurance athletes, outdoor adventurers, and water sports with 100m water resistance.', @BrandApple, @CatWearables, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_AWU, 'https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1', 'Apple Watch Ultra 2', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    DECLARE @V_AWU1 UNIQUEIDENTIFIER = NEWID();
    INSERT INTO [product_variants] ([id], [product_id], [sku], [color], [storage], [price], [active], [created_at], [updated_at])
    VALUES (@V_AWU1, @P_AWU, 'APL-AWU2-ALP', 'Titanium Case', 'Alpine Loop Blue', 799.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [inventory_items] ([id], [variant_id], [quantity_available], [created_at], [updated_at])
    VALUES (NEWID(), @V_AWU1, 15, SYSUTCDATETIME(), SYSUTCDATETIME());
END

-- Product 6: Keychron Q1 Pro
DECLARE @P_KEY UNIQUEIDENTIFIER = '30000000-0000-0000-0000-000000000007';
IF NOT EXISTS (SELECT 1 FROM [products] WHERE [slug] = 'keychron-q1-pro')
BEGIN
    INSERT INTO [products] ([id], [name], [slug], [short_description], [description], [brand_id], [category_id], [active], [created_at], [updated_at])
    VALUES (@P_KEY, 'Keychron Q1 Pro Wireless Custom', 'keychron-q1-pro', 'Full-metal QMK/VIA wireless mechanical keyboard with double-gasket design.', 'Crafted with 6063 CNC aluminum body, hot-swappable switches, South-facing RGB, and rotary knob.', @BrandKeychron, @CatAccessories, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
    VALUES (NEWID(), @P_KEY, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3', 'Keychron Q1 Pro', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    DECLARE @V_KEY1 UNIQUEIDENTIFIER = NEWID();
    INSERT INTO [product_variants] ([id], [product_id], [sku], [color], [storage], [price], [active], [created_at], [updated_at])
    VALUES (@V_KEY1, @P_KEY, 'KEY-Q1P-RED', 'Carbon Black', 'K Pro Red', 199.00, 1, SYSUTCDATETIME(), SYSUTCDATETIME());

    INSERT INTO [inventory_items] ([id], [variant_id], [quantity_available], [created_at], [updated_at])
    VALUES (NEWID(), @V_KEY1, 22, SYSUTCDATETIME(), SYSUTCDATETIME());
END


-- ----------------------------------------------------------------------------------------------------
-- 5. SAMPLE REVIEWS
-- ----------------------------------------------------------------------------------------------------
DECLARE @CustomerUserId UNIQUEIDENTIFIER = (SELECT [id] FROM [users] WHERE [email] = 'customer@luxora.com');
DECLARE @AeroPhoneId UNIQUEIDENTIFIER = (SELECT [id] FROM [products] WHERE [slug] = 'aerophone-x1');

IF @CustomerUserId IS NOT NULL AND @AeroPhoneId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM [reviews] WHERE [product_id] = @AeroPhoneId AND [user_id] = @CustomerUserId)
    BEGIN
        DECLARE @SampleReviewId UNIQUEIDENTIFIER = NEWID();
        INSERT INTO [reviews] ([id], [product_id], [user_id], [rating], [title], [comment], [status], [is_verified_purchase], [created_at], [updated_at])
        VALUES (@SampleReviewId, @AeroPhoneId, @CustomerUserId, 5, 'Exceptional flagship performance!', 'The display clarity and battery life exceeded all my expectations. Camera low-light mode is stunning.', 'APPROVED', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

        INSERT INTO [review_images] ([id], [review_id], [image_url], [display_order], [created_at])
        VALUES (NEWID(), @SampleReviewId, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9', 0, SYSUTCDATETIME());
    END
END

-- Ensure all existing products have at least one image if not present
INSERT INTO [product_images] ([id], [product_id], [url], [alt_text], [display_order], [created_at], [updated_at])
SELECT NEWID(), p.[id], 
       CASE p.[slug]
           WHEN 'aerophone-x1' THEN 'https://images.unsplash.com/photo-1598327105666-5b89351aff97'
           WHEN 'studio-one' THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e'
           WHEN 'vision-slate' THEN 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0'
           WHEN 'iphone-16-pro-max' THEN 'https://images.unsplash.com/photo-1695048133142-1a20484d2569'
           WHEN 'galaxy-s25-ultra' THEN 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf'
           WHEN 'pixel-9-pro' THEN 'https://images.unsplash.com/photo-1598327105666-5b89351aff97'
           WHEN 'macbook-pro-16-m4' THEN 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8'
           WHEN 'dell-xps-15-oled' THEN 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45'
           WHEN 'thinkpad-x1-carbon-gen12' THEN 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed'
           WHEN 'rog-zephyrus-g16' THEN 'https://images.unsplash.com/photo-1603302576837-37561b2e2302'
           WHEN 'sony-wh-1000xm5' THEN 'https://images.unsplash.com/photo-1546435770-a3e426bf472b'
           WHEN 'airpods-max' THEN 'https://images.unsplash.com/photo-1583394838336-acd977736f90'
           WHEN 'bose-qc-ultra' THEN 'https://images.unsplash.com/photo-1572536147248-ac59a8abfa4b'
           WHEN 'devialet-mania' THEN 'https://images.unsplash.com/photo-1545454675-3531b543be5d'
           WHEN 'apple-watch-ultra-2' THEN 'https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1'
           WHEN 'galaxy-watch-ultra' THEN 'https://images.unsplash.com/photo-1523275335684-37898b6baf30'
           WHEN 'garmin-fenix-8' THEN 'https://images.unsplash.com/photo-1509042239860-f550ce710b93'
           WHEN 'ipad-pro-13-m4' THEN 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0'
           WHEN 'galaxy-tab-s10-ultra' THEN 'https://images.unsplash.com/photo-1561154464-82e9adf32764'
           WHEN 'keychron-q1-pro' THEN 'https://images.unsplash.com/photo-1587829741301-dc798b83add3'
           WHEN 'anker-prime-charging-stand' THEN 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0'
           WHEN 'dyson-solarcycle-morph' THEN 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c'
           ELSE 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e'
       END,
       p.[name],
       1,
       SYSUTCDATETIME(),
       SYSUTCDATETIME()
FROM [products] p
WHERE NOT EXISTS (SELECT 1 FROM [product_images] pi WHERE pi.[product_id] = p.[id]);

PRINT 'LUXORA Seed Data applied successfully.';
GO
