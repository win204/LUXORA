create table order_status_history (
    id uniqueidentifier not null primary key,
    order_id uniqueidentifier not null references orders (id) on delete cascade,
    from_status varchar(40) not null,
    to_status varchar(40) not null,
    changed_at datetimeoffset(7) not null default sysdatetimeoffset(),
    changed_by_user_id uniqueidentifier references users (id)
);

create index idx_order_status_history_order_changed_at on order_status_history (order_id, changed_at);
create index idx_order_status_history_changed_by on order_status_history (changed_by_user_id);