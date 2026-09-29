create table refunds (
    id uniqueidentifier not null primary key,
    order_id uniqueidentifier not null,
    payment_id uniqueidentifier not null,
    provider varchar(40) not null,
    provider_reference varchar(120) not null,
    amount decimal(12, 2) not null,
    currency varchar(3) not null,
    status varchar(40) not null,
    reason varchar(500),
    created_at datetimeoffset(7) not null,
    updated_at datetimeoffset(7) not null,
    constraint fk_refunds_order foreign key (order_id) references orders(id),
    constraint fk_refunds_payment foreign key (payment_id) references payments(id),
    constraint ux_refunds_provider_reference unique (provider_reference)
);

create index idx_refunds_order_created_at on refunds(order_id, created_at);
create index idx_refunds_payment on refunds(payment_id);
create unique index ux_refunds_order_succeeded on refunds(order_id) where status = 'SUCCEEDED';
