create table return_shipments (
    id uniqueidentifier not null primary key,
    return_id uniqueidentifier not null,
    carrier varchar(80) not null,
    tracking_number varchar(120) not null,
    mock_label_reference varchar(180) not null,
    shipped_at datetimeoffset(7),
    received_at datetimeoffset(7),
    created_at datetimeoffset(7) not null,
    updated_at datetimeoffset(7) not null,
    constraint fk_return_shipments_return foreign key (return_id) references returns(id),
    constraint ux_return_shipments_return unique (return_id),
    constraint ux_return_shipments_tracking unique (tracking_number)
);

create index idx_return_shipments_return on return_shipments(return_id);