create table orders (
    id uniqueidentifier not null primary key,
    user_id uniqueidentifier not null references users (id),
    status varchar(40) not null,
    recipient_name varchar(160) not null,
    phone varchar(32) not null,
    address_line1 varchar(240) not null,
    address_line2 varchar(240),
    city varchar(120) not null,
    province varchar(120) not null,
    country varchar(120) not null,
    postal_code varchar(24),
    subtotal decimal(12, 2) not null,
    shipping_fee decimal(12, 2) not null,
    tax decimal(12, 2) not null,
    discount decimal(12, 2) not null,
    grand_total decimal(12, 2) not null,
    currency varchar(3) not null,
    created_at datetimeoffset(7) not null default sysdatetimeoffset()
);

create table order_items (
    id uniqueidentifier not null primary key,
    order_id uniqueidentifier not null references orders (id) on delete cascade,
    product_id uniqueidentifier not null,
    variant_id uniqueidentifier not null,
    product_slug varchar(220) not null,
    product_name varchar(180) not null,
    variant_name varchar(180),
    sku varchar(80) not null,
    color varchar(80),
    storage varchar(80),
    image_url varchar(500),
    unit_price decimal(12, 2) not null,
    quantity int not null,
    line_total decimal(12, 2) not null,
    created_at datetimeoffset(7) not null default sysdatetimeoffset(),
    constraint ck_order_items_quantity_positive check (quantity > 0)
);

create index idx_orders_user_created_at on orders (user_id, created_at);
create index idx_order_items_order on order_items (order_id);
create index idx_order_items_variant on order_items (variant_id);