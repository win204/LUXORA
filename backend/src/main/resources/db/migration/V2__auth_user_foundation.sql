create table roles (
    id uniqueidentifier not null primary key,
    name varchar(40) not null unique
);

create table users (
    id uniqueidentifier not null primary key,
    email varchar(254) not null unique,
    password_hash varchar(100) not null,
    first_name varchar(120) not null,
    last_name varchar(120) not null,
    enabled bit not null default 1,
    created_at datetime2 not null default sysutcdatetime(),
    updated_at datetime2 not null default sysutcdatetime()
);

create table user_roles (
    user_id uniqueidentifier not null references users (id) on delete cascade,
    role_id uniqueidentifier not null references roles (id),
    primary key (user_id, role_id)
);

create table refresh_tokens (
    id uniqueidentifier not null primary key,
    user_id uniqueidentifier not null references users (id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at datetime2 not null,
    revoked_at datetime2,
    replaced_by_token_hash varchar(64),
    created_at datetime2 not null default sysutcdatetime()
);

insert into roles (id, name)
select newid(), 'ROLE_USER'
where not exists (select 1 from roles where name = 'ROLE_USER');

insert into roles (id, name)
select newid(), 'ROLE_ADMIN'
where not exists (select 1 from roles where name = 'ROLE_ADMIN');

create index idx_users_email on users (email);
create index idx_refresh_tokens_user on refresh_tokens (user_id);
create index idx_refresh_tokens_hash on refresh_tokens (token_hash);
