create table payments (
    id uniqueidentifier not null primary key,
    order_id uniqueidentifier not null references orders (id),
    provider varchar(40) not null,
    provider_reference varchar(120) not null unique,
    amount decimal(12, 2) not null,
    currency varchar(3) not null,
    status varchar(40) not null,
    created_at datetimeoffset(7) not null default sysdatetimeoffset(),
    updated_at datetimeoffset(7) not null default sysdatetimeoffset(),
    constraint ck_payments_amount_non_negative check (amount >= 0)
);

create index idx_payments_order_created_at on payments (order_id, created_at);
create index idx_payments_order_status on payments (order_id, status);