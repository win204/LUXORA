create table brands (
    id uniqueidentifier not null primary key,
    name varchar(120) not null unique,
    slug varchar(140) not null unique,
    active bit not null default 1
);

create table categories (
    id uniqueidentifier not null primary key,
    name varchar(120) not null unique,
    slug varchar(140) not null unique,
    active bit not null default 1
);

create table products (
    id uniqueidentifier not null primary key,
    name varchar(180) not null,
    slug varchar(220) not null unique,
    subtitle varchar(240),
    description varchar(4000) not null,
    brand_id uniqueidentifier not null references brands (id),
    category_id uniqueidentifier not null references categories (id),
    active bit not null default 1
);

create table product_images (
    id uniqueidentifier not null primary key,
    product_id uniqueidentifier not null references products (id) on delete cascade,
    url varchar(500) not null,
    alt_text varchar(180),
    display_order int not null
);

create table product_specifications (
    id uniqueidentifier not null primary key,
    product_id uniqueidentifier not null references products (id) on delete cascade,
    name varchar(120) not null,
    spec_value varchar(500) not null,
    display_order int not null
);

create table product_variants (
    id uniqueidentifier not null primary key,
    product_id uniqueidentifier not null references products (id) on delete cascade,
    sku varchar(80) not null unique,
    color varchar(80),
    storage varchar(80),
    price decimal(12, 2) not null,
    active bit not null default 1
);

create table inventory_items (
    id uniqueidentifier not null primary key,
    variant_id uniqueidentifier not null unique references product_variants (id) on delete cascade,
    quantity_available int not null default 0
);

create index idx_products_active_slug on products (active, slug);
create index idx_products_brand on products (brand_id);
create index idx_products_category on products (category_id);
create index idx_product_variants_product_price on product_variants (product_id, price);
create index idx_inventory_items_variant on inventory_items (variant_id);
