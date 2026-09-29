create table promotions (
 id uniqueidentifier not null primary key,
 code varchar(64) not null unique,
 name varchar(160) not null,
 description varchar(500) null,
 type varchar(32) not null,
 value decimal(12,2) not null,
 minimum_order_amount decimal(12,2) null,
 maximum_discount_amount decimal(12,2) null,
 starts_at datetimeoffset(7) not null,
 ends_at datetimeoffset(7) not null,
 usage_limit int null,
 usage_count int not null default 0,
 active bit not null default 1,
 created_at datetimeoffset(7) not null,
 updated_at datetimeoffset(7) not null,
 constraint ck_promotions_type check (type in ('PERCENTAGE','FIXED_AMOUNT')),
 constraint ck_promotions_window check (ends_at > starts_at),
 constraint ck_promotions_value check (value > 0),
 constraint ck_promotions_usage check (usage_count >= 0),
 constraint ck_promotions_usage_limit check (usage_limit is null or usage_limit > 0)
);
create index idx_promotions_active_dates on promotions (active, starts_at, ends_at);
GO
alter table orders add promotion_code varchar(64) null;
GO
alter table orders add promotion_name varchar(160) null;
GO
alter table orders add promotion_type varchar(32) null;
GO
alter table orders add promotion_value decimal(12,2) null;
GO
alter table orders add promotion_discount_amount decimal(12,2) null;