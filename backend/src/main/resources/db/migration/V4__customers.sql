-- Customer accounts, saved addresses and the link from bags and orders to an account.

create table customers (
    id            bigint generated always as identity primary key,
    name          text        not null,
    phone         text        not null unique check (phone ~ '^01[3-9][0-9]{8}$'),
    email         text,
    password_hash text        not null,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create table addresses (
    id          bigint generated always as identity primary key,
    customer_id bigint      not null references customers (id) on delete cascade,
    label       text        not null default 'Home',
    name        text        not null,
    phone       text        not null check (phone ~ '^01[3-9][0-9]{8}$'),
    address     text        not null,
    area_id     bigint      not null references delivery_areas (id),
    note        text,
    is_default  boolean     not null default false,
    created_at  timestamptz not null default now()
);
create index addresses_customer on addresses (customer_id);
-- At most one default address per customer.
create unique index addresses_one_default on addresses (customer_id) where is_default;

-- A signed-in customer has one bag, shared by every device they sign in on.
alter table carts add constraint carts_customer_fk foreign key (customer_id) references customers (id) on delete cascade;
create unique index carts_one_per_customer on carts (customer_id) where customer_id is not null;

alter table orders add constraint orders_customer_fk foreign key (customer_id) references customers (id) on delete set null;
create index orders_customer on orders (customer_id, created_at desc) where customer_id is not null;
