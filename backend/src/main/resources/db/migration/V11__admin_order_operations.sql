alter table orders add admin_note varchar(500);
GO
alter table orders add updated_at datetimeoffset(7);
GO
update orders set updated_at = created_at where updated_at is null;
GO
alter table orders alter column updated_at datetimeoffset(7) not null;
GO
create index idx_orders_status_created_at on orders (status, created_at desc);