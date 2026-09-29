create table shipments (
    id uniqueidentifier not null primary key,
    order_id uniqueidentifier not null,
    carrier varchar(120) not null,
    tracking_number varchar(120) not null,
    shipped_at datetimeoffset(7) not null,
    delivered_at datetimeoffset(7),
    created_at datetimeoffset(7) not null,
    updated_at datetimeoffset(7) not null,
    constraint fk_shipments_order foreign key (order_id) references orders(id),
    constraint ux_shipments_order unique (order_id)
);

create index idx_shipments_tracking_number on shipments(tracking_number);
