create table returns (
    id uniqueidentifier not null primary key,
    order_id uniqueidentifier not null,
    user_id uniqueidentifier not null,
    status varchar(40) not null,
    customer_note varchar(500),
    admin_note varchar(500),
    requested_at datetimeoffset(7) not null,
    approved_at datetimeoffset(7),
    rejected_at datetimeoffset(7),
    received_at datetimeoffset(7),
    refunded_at datetimeoffset(7),
    cancelled_at datetimeoffset(7),
    created_at datetimeoffset(7) not null,
    updated_at datetimeoffset(7) not null,
    constraint fk_returns_order foreign key (order_id) references orders(id),
    constraint fk_returns_user foreign key (user_id) references users(id),
    constraint ck_returns_status check (status in ('REQUESTED','APPROVED','REJECTED','CANCELLED','RECEIVED','REFUNDED'))
);

create table return_items (
    id uniqueidentifier not null primary key,
    return_id uniqueidentifier not null,
    order_item_id uniqueidentifier not null,
    requested_quantity int not null,
    approved_quantity int not null,
    received_quantity int not null,
    reason varchar(500) not null,
    product_name varchar(180) not null,
    sku varchar(80) not null,
    unit_price decimal(12,2) not null,
    created_at datetimeoffset(7) not null,
    constraint fk_return_items_return foreign key (return_id) references returns(id),
    constraint fk_return_items_order_item foreign key (order_item_id) references order_items(id),
    constraint ck_return_items_requested_quantity check (requested_quantity > 0),
    constraint ck_return_items_approved_quantity check (approved_quantity >= 0),
    constraint ck_return_items_received_quantity check (received_quantity >= 0)
);

create table return_status_history (
    id uniqueidentifier not null primary key,
    return_id uniqueidentifier not null,
    from_status varchar(40) not null,
    to_status varchar(40) not null,
    changed_at datetimeoffset(7) not null,
    changed_by_user_id uniqueidentifier,
    constraint fk_return_status_history_return foreign key (return_id) references returns(id),
    constraint fk_return_status_history_user foreign key (changed_by_user_id) references users(id),
    constraint ck_return_status_history_from_status check (from_status in ('REQUESTED','APPROVED','REJECTED','CANCELLED','RECEIVED','REFUNDED')),
    constraint ck_return_status_history_to_status check (to_status in ('REQUESTED','APPROVED','REJECTED','CANCELLED','RECEIVED','REFUNDED'))
);

alter table refunds add return_id uniqueidentifier;
alter table refunds add constraint fk_refunds_return foreign key (return_id) references returns(id);
GO

create index idx_returns_user_requested_at on returns(user_id, requested_at desc);
create index idx_returns_order on returns(order_id);
create index idx_return_items_order_item on return_items(order_item_id);
create index idx_return_status_history_return on return_status_history(return_id, changed_at);
create unique index ux_refunds_return_succeeded on refunds(return_id) where return_id is not null and status = 'SUCCEEDED';
